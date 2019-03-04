package querymodule.query

import org.joda.time.LocalDate
import play.api.libs.json.{JsObject, Json}
import QueryConstants.{DATE, END, START}

/** REQUIRED: parse date parameters from query_module.query string
 *  GET /path?date.start=2018-07-19&date.end=2018-07-25
 *
 *  @param querystring date parameters
 */
case class QueryDate(querystring: Map[String, Seq[String]]) extends QueryParser {
  /** Start date reference
   *  /...?date.start=2018-08-17
   */
  val start: LocalDate = parse(START, isValid)

  /** End date reference
   *  Should be after start date
   *  /...?date.end=2018-09-17
   */
  val end: LocalDate = parse(END, isValid)

  /** Standard JSON output for date object in HTTP response
   */
  val json: JsObject = Json.obj(DATE -> Json.obj(START -> start.toString("yyyy-MM-dd"), END -> end.toString("yyyy-MM-dd")))

  /** Check if date is valid. Must be current date or earlier.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean = {
    val d = LocalDate.parse(p)
    val now = LocalDate.now()
    d.isEqual(now) || d.isBefore(now)
  }

  /** Parse date start and end values from standard querystring
   *
   *  @param param      desired parameter: start or end
   *  @param constraint validation constraint
   *  @return local date or illegal argument exception
   */
  def parse(param: String, constraint: String => Boolean): LocalDate = {
    querystring
      .get(s"$DATE.$param")
      .flatMap(_.headOption.filter(constraint))
      .map(LocalDate.parse)
      .getOrElse(invalid(s"$DATE.$param"))
  }
}
