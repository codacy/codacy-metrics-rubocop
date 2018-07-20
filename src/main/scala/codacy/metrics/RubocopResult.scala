package codacy.metrics

import play.api.libs.json.{Format, JsString, JsValue, Json}

final case class RubocopLocation(line: Int, column: JsValue, length: JsValue)

final case class RubocopOffense(message: JsString, location: RubocopLocation)

final case class RubocopFile(path: JsString, offenses: Option[List[RubocopOffense]])

final case class RubocopResult(files: Option[List[RubocopFile]])

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
