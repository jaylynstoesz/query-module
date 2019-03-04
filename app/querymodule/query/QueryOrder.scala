package querymodule.query

import play.api.libs.json.{JsObject, Json}
import QueryConstants.{ASC, BY, DESC, DIR, ORDER}

/** OPTIONAL: parse order parameters from query_module.query string
 *  GET /path?order.by=conversions&order.dir=desc
 *
 *  @param querystring order parameters
 */
case class QueryOrder(querystring: Map[String, Seq[String]]) extends QueryParser {

  /** Order by reference
   *  /...?order.by=record.name
   */
  val by: Option[String] = parse(BY, isValid)

  /** Order dir reference
   *  /...?order.dir=asc
   */
  val dir: Option[String] = parse(DIR, isValidDir)

  /** Standard JSON output for date object in HTTP response
   */
  val json: JsObject = Json.obj(ORDER -> Json.obj(BY -> by, DIR -> dir))

  /** Check if order by is valid. Must be either a case-insensitive version of ASC or DESC
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValidDir(p: String): Boolean = List(ASC, DESC).contains(p.toUpperCase)

  /** Check if order by is valid. Must be a non-empty string.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean = p.nonEmpty

  /** Parse order by and dir values from standard querystring
   *
   *  @param param      desired parameter: by or dir
   *  @param constraint validation constraint
   *  @return order by value or illegal argument exception
   */
  def parse(param: String, constraint: String => Boolean): Option[String] = {
    querystring
      .get(s"$ORDER.$param")
      .map(_.headOption.filter(constraint))
      .map(_.getOrElse(invalid(s"$ORDER $param")))
  }
}
