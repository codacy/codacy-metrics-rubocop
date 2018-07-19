package codacy.metrics

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import better.files.File
import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.{CommandResult, CommandRunner}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.matching.Regex
import scala.util.{Failure, Properties, Success, Try}

object Rubocop extends MetricsTool {

  private val configFileContent =
    """AllCops:
      |  DisplayCopNames: false
      |  UseCache: false
      |Metrics/CyclomaticComplexity:
      |  Max: 1
    """.stripMargin

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    language match {
      case Some(lang) if lang != Languages.Ruby =>
        Failure(new Exception(s"Rubocop only supports Ruby. Provided language: $lang"))
      case _ =>
        calculateComplexity(source.path, files)
    }
  }

  private def calculateComplexity(directory: String, maybeFiles: Option[Set[Source.File]]): Try[List[FileMetrics]] = {
    val cmd = command(directory, maybeFiles)

    CommandRunner.exec(cmd, Option(File(directory).toJava)).toTry.flatMap {

      case resultFromTool if resultFromTool.exitCode < 2 =>
        parseResult(resultFromTool.stdout.mkString("\n")).recoverWith {
          case e =>
            val errorMessage = exceptionMsg(resultFromTool, Some(e))
            Failure(new Exception(errorMessage))
        }

      case resultFromTool =>
        val errorMessage = exceptionMsg(resultFromTool, None)
        Failure(new Exception(errorMessage))
    }
  }

  private def exceptionMsg(resultFromTool: CommandResult, throwable: Option[Throwable]) = {
    s"""
       |Rubocop exited with code ${resultFromTool.exitCode}
       |exception message: ${throwable.map(_.getMessage).getOrElse("(no exception thrown)")}
       |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
       |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
      """.stripMargin
  }

  private def command(directory: String, maybeFiles: Option[Set[Source.File]]): List[String] = {
    val directoryAbsolutePath = File(directory).path.toAbsolutePath.toString

    val filesCmd = maybeFiles.map(_.map(_.path)).getOrElse(Set(directoryAbsolutePath))

    val configPath = createConfigFile.map { configFile =>
      List("-c", configFile.toAbsolutePath.toString)
    }.getOrElse(List.empty)

    List("rubocop", "--force-exclusion", "-f", "json", "--only", "Metrics/CyclomaticComplexity") ++ configPath ++ filesCmd
  }

  private def parseResult(resultFromTool: String): Try[List[FileMetrics]] = {
    Try {
      Json.parse(resultFromTool)
    }.flatMap { json =>
      json.validate[RubocopResult] match {
        case JsSuccess(rubocopResult, _) =>
          val files = rubocopResult.files.getOrElse(List.empty)
          val fileMetricsList = files.map(fileMetrics(_))

          Success(fileMetricsList)

        case JsError(err) =>
          Failure(new Throwable(Json.stringify(JsError.toJson(err))))
      }
    }
  }

  private def parseComplexityMessage(message: String): Option[Int] = {
    // http://www.rubydoc.info/github/bbatsov/rubocop/Rubocop/Cop/Style/CyclomaticComplexity
    val complexityRegex: Regex =
      """Cyclomatic complexity for .*? is too high\. \[(\d+)\/\d+\]""".r
    message match {
      case complexityRegex(n) => Try(n.toInt).toOption
      case _                  => None
    }
  }

  private def fileMetrics(rubocopFiles: RubocopFile): FileMetrics = {
    val lineComplexities: Set[LineComplexity] = rubocopFiles.offenses
      .getOrElse(List.empty)
      .flatMap(r => parseComplexityMessage(r.message.value).map(v => LineComplexity(r.location.line, v)))(
        collection.breakOut)

    val complexity = (lineComplexities.map(_.value) + 0).max

    FileMetrics(rubocopFiles.path.value, complexity = Some(complexity), None, None, None, None, lineComplexities)
  }

  private def createConfigFile: Option[Path] = {
    val prefix = "config"
    val suffix = ".yml"

    Try {
      Files.write(
        Files.createTempFile(prefix, suffix),
        configFileContent.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE)
    }.toOption
  }

}
