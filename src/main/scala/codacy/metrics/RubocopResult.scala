package codacy.metrics

import play.api.libs.json.{Format, JsString, JsValue, Json}

case class RubocopLocation(line: Int, column: JsValue, length: JsValue)

case class RubocopOffense(message: JsString,
                          location: RubocopLocation)

case class RubocopFile(path: JsString, offenses: Option[List[RubocopOffense]])

case class RubocopResult(files: Option[List[RubocopFile]])

object RubocopResult {
  implicit val RLocation: Format[RubocopLocation] = {
    Json.format[RubocopLocation]
  }

  implicit val ROffenses: Format[RubocopOffense] = {
    Json.format[RubocopOffense]
  }

  implicit val RFiles: Format[RubocopFile] = {
    Json.format[RubocopFile]
  }

  implicit val RResult: Format[RubocopResult] = {
    Json.format[RubocopResult]
  }
}
