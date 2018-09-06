package codacy.metrics

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.metrics.{FileMetrics, LineComplexity}
import org.specs2.mutable.Specification

class RubocopSpec extends Specification {

  val targetDir = "src/test/resources"

  val expectedFileMetric1 =
    FileMetrics("codacy/metrics/something.rb", Some(10), None, None, None, None, Set(LineComplexity(1, 10)))

  val expectedFileMetric2 =
    FileMetrics(
      "codacy/metrics/graph.rb",
      Some(15),
      None,
      None,
      None,
      None,
      Set(
        LineComplexity(184, 3),
        LineComplexity(61, 15),
        LineComplexity(211, 3),
        LineComplexity(161, 5),
        LineComplexity(223, 2),
        LineComplexity(198, 3)))

  "Robocop" should {
    "get metrics" in {
      "all files within a directory" in {

        val expectedFileMetrics = List(expectedFileMetric1, expectedFileMetric2)

        val fileMetricsMap =
          Rubocop(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap.get should containTheSameElementsAs(expectedFileMetrics)
      }

      "specific files" in {

        val expectedFileMetrics = List(expectedFileMetric2)

        val fileMetricsMap = Rubocop(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(expectedFileMetric2.filename))),
          options = Map.empty)

        fileMetricsMap.get should containTheSameElementsAs(expectedFileMetrics)
      }
    }
  }
}
