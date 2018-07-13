package codacy.metrics

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import better.files.File
import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.CommandRunner
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.matching.Regex
import scala.util.{Failure, Properties, Success, Try}

object Rubocop extends MetricsTool {

  private lazy val resultFilePath: Path = Paths.get(Properties.tmpDir, "rubocop-result.json")
  //TODO: do we need the gemfile.lock??
  private val filesToIgnore: Set[String] = Set("Gemfile.lock").map(_.toLowerCase())

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

  //TODO: refactor!!!!
  private def calculateComplexity(directory: String, maybeFiles: Option[Set[Source.File]]) = {
    val directoryAbsolutePath = File(directory).path.toAbsolutePath.toString
    val filesCmd = maybeFiles.map(_.map(_.path)).getOrElse(List(directoryAbsolutePath)).collect {
      case file if !filesToIgnore.contains(file.toLowerCase()) =>
        file.toString
    }
    val configPath = getConfigFile.map { configFile =>
      List("-c", configFile.toAbsolutePath.toString)
    }.getOrElse(List.empty)

    //TODO: do we need all these args?
    val cmd = List(
      "rubocop",
      "--force-exclusion",
      "-f",
      "json",
      "-o",
      resultFilePath.toAbsolutePath.toString,
      "--only",
      "Metrics/CyclomaticComplexity") ++ configPath ++ filesCmd
    CommandRunner.exec(cmd, Option(File(directory).toJava)) match {

      case Right(resultFromTool) if resultFromTool.exitCode < 2 =>
        parseResult(resultFilePath.toString) match {
          case s @ Success(_) => s
          case Failure(e) =>
            val msg =
              s"""
                 |Rubocop exited with code ${resultFromTool.exitCode}
                 |message: ${e.getMessage}
                 |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
                 |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
                """.stripMargin
            Failure(new Exception(msg))
        }

      case Right(resultFromTool) =>
        val msg =
          s"""
             |Rubocop exited with code ${resultFromTool.exitCode}
             |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
             |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
                """.stripMargin
        Failure(new Exception(msg))

      case Left(e) =>
        Failure(e)
    }
  }

  private[this] def parseResult(filename: String): Try[List[FileMetrics]] = {
    Try {
      val resultFromTool = File(filename).contentAsString
      println(resultFromTool)
      Json.parse(resultFromTool)
    }.flatMap { json =>
      json.validate[RubocopResult] match {
        case JsSuccess(rubocopResult, _) =>
          Success(rubocopResult.files.getOrElse(List.empty).map { file =>
            ruboFileToResult(file)
          })
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

  private def ruboFileToResult(rubocopFiles: RubocopFiles): FileMetrics = {
    val lineComplexities: Set[LineComplexity] = rubocopFiles.offenses
      .getOrElse(List.empty)
      .flatMap(r => parseComplexityMessage(r.message.value).map(v => LineComplexity(r.location.line, v)))(
        collection.breakOut)
    val complexity = Some((lineComplexities.map(_.value) + 0).max)

    FileMetrics(rubocopFiles.path.value, complexity, None, None, None, None, lineComplexities)
  }

  private def getConfigFile: Option[Path] = {

    val ymlConfiguration =
      s"""
         |AllCops:
         |  DisplayCopNames: false
         |  UseCache: false
         |Metrics/CyclomaticComplexity:
         |  Max: 1
      """.stripMargin

    fileForConfig(ymlConfiguration).toOption
  }

  private[this] def fileForConfig(config: String) = tmpFile(config.toString)

  private[this] def tmpFile(content: String, prefix: String = "config", suffix: String = ".yml"): Try[Path] = {
    Try {
      Files.write(
        Files.createTempFile(prefix, suffix),
        content.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE)
    }
  }

}
