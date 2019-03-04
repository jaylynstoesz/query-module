package querymodule.query

import play.api.libs.json.{JsObject, Json}
import QueryConstants.DIMENSION

/** OPTIONAL: parse dimension parameter from query_module.query string
 *  GET /path?dimension=model
 *
 *  @param querystring dimension parameter
 */
case class QueryDimension(querystring: Map[String, Seq[String]]) extends QueryParser {
  /** Dimension reference
   *  /...?dimension=record_type
   */
  val dimension: Option[String] = parse(DIMENSION, isValid)

  /** Standard JSON output for date object in HTTP response
   */
  val json: JsObject = Json.obj(DIMENSION -> dimension)

  /** Check if dimension is valid. Must be a non-empty string.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean = p.nonEmpty

  /** Parse dimension value from standard querystring
   *
   *  @param param      desired parameter: dimension
   *  @param constraint validation constraint
   *  @return dimension or illegal argument exception
   */
  def parse(param: String, constraint: String => Boolean): Option[String] = {
    querystring
      .get(param)
      .map(_.headOption)
      .map(_.getOrElse(invalid(param)))
  }
}
