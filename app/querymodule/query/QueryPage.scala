package querymodule.query

import play.api.libs.json.{JsNull, JsObject, Json}

import scala.util.Try
import QueryConstants.{CURRENT, END, LIMIT, NEXT, PAGE, PAGES, PREVIOUS, RECORDS, SHOWING, START, TOTAL}

/** OPTIONAL: parse page parameters from query_module.query string
 *  GET /path?page.limit=25&page.current=1
 *
 *  Will default to 500 records per page, currently on first page
 *
 *  @param querystring page parameters
 */
case class QueryPage(querystring: Map[String, Seq[String]]) extends QueryParser {
  /** Page limit reference
   *  /...?page.limit=25
   */
  val limit: Int = parse(LIMIT, isValid).getOrElse(500)

  /** Current page reference
   *  /...?page.current=1
   */
  val current: Int = parse(CURRENT, isValid).getOrElse(1)

  /** Page offset reference
   *  Calculated from current and limit values
   */
  val offset: Int = (current - 1) * limit

  /** Standard JSON output for date object in HTTP response
   */
  def json: JsObject = Json.obj(
    LIMIT -> limit,
    CURRENT -> current
  )

  /** Standard JSON output for date object in HTTP response,
   *  with additional calculated fields used for pagination
   */
  def jsonWithTotals(count: Int, total: Int): JsObject = {
    val start: Int = if (count > 0) offset + 1 else 0
    val end: Int = if (count > 0) offset + count else 0
    val pages: Int = math.ceil(total.toFloat / limit).toInt
    val next: Option[Int] = if ((pages > 1) && (current < pages)) Some(current + 1) else None
    val previous: Option[Int] = if ((pages > 1) && (current > 1)) Some(current - 1) else None

    Json.obj(
      PAGE -> (json ++ Json.obj(
        NEXT -> next,
        PREVIOUS -> previous,
        PAGES -> pages,
        RECORDS -> count,
        SHOWING -> (if (pages == 0) JsNull else Json.obj(START -> start, END -> end)),
        TOTAL -> total
      ))
    )
  }

  /** Check if date is valid. Must be current date or earlier.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean = Try(p.toInt).isSuccess && p.toInt > 0

  /** Parse page current and limit values from standard querystring
   *
   *  @param param      desired parameter: current or limit
   *  @param constraint validation constraint
   *  @return page current or limit value or illegal argument exception
   */
  def parse(param: String, constraint: String => Boolean): Option[Int] =
    querystring
      .get(s"$PAGE.$param")
      .map(
        _.headOption
          .collect {
            case s if constraint(s) => s.toInt
          }
      ).map(_.getOrElse(invalid(s"$PAGE $param")))
}
