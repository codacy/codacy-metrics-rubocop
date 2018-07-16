package codacy.metrics

import play.api.libs.json.{Format, JsString, JsValue, Json}

//TODO: do we really need all of this? can't we just parse the message we need from json to get the complexity?

case class RubocopLocation(line: Int, column: JsValue, length: JsValue)

case class RubocopMetadata(rubocop_version: JsValue,
                           ruby_engine: JsValue,
                           ruby_version: JsValue,
                           ruby_patchlevel: JsValue,
                           ruby_platform: JsValue)

case class RubocopOffense(severity: JsValue,
                          message: JsString,
                          cop_name: JsString,
                          corrected: JsValue,
                          location: RubocopLocation)

case class RubocopFile(path: JsString, offenses: Option[List[RubocopOffense]])

case class RubocopSummary(offense_count: JsValue, target_file_count: JsValue, inspected_file_count: JsValue)

case class RubocopResult(metadata: RubocopMetadata, files: Option[List[RubocopFile]], summary: RubocopSummary)

object RubocopResult {
  implicit val RLocation: Format[RubocopLocation] = {
    Json.format[RubocopLocation]
  }

  implicit val RMetaData: Format[RubocopMetadata] = {
    Json.format[RubocopMetadata]
  }

  implicit val ROffenses: Format[RubocopOffense] = {
    Json.format[RubocopOffense]
  }

  implicit val RFiles: Format[RubocopFile] = {
    Json.format[RubocopFile]
  }

  implicit val RSummary: Format[RubocopSummary] = {
    Json.format[RubocopSummary]
  }

  implicit val RResult: Format[RubocopResult] = {
    Json.format[RubocopResult]
  }
}
