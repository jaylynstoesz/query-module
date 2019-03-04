package querymodule.exporter

import play.api.libs.json.{JsObject, Json}

trait Exportable {
  /** JSON output of the extended class (required)
   */
  def json: JsObject

  /** JSON output of the totals row version of the extended class (optional)
   */
  def jsonTotal: JsObject = Json.obj()

  /** CSV output of the extended class (required)
   *  Use list instead of a Map to preserve order
   */
  def csv: List[(String, Any)]

  /** CSV headers
   */
  def csvHeaders: List[String] = csv.map(_._1)

  /** CSV values
   *
   *  @param na "N/A" value
   */
  def csvValues(na: String = "-"): List[String] =
    csv.map(_._2).map {
      case None         => na
      case v: Some[Any] => v.map(_.toString).getOrElse(na)
      case v            => v.toString
    }
}
