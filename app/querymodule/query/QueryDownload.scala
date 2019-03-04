package querymodule.query

import play.api.libs.json.{JsObject, Json}
import QueryConstants.DOWNLOAD

/** OPTIONAL: parse download parameter from query_module.query string
 *  GET /path?download=true
 *
 *  @param querystring download parameter
 */
case class QueryDownload(querystring: Map[String, Seq[String]]) extends QueryParser {
  /** Download reference
   *  /...?download=true
   */
  val download: Boolean = parse(DOWNLOAD, isValid)

  /** Standard JSON output for date object in HTTP response
   */
  val json: JsObject = Json.obj(DOWNLOAD -> download)

  /** Check if dimension is valid. Must be a case-insensitive version of
   *  "true" or "false".
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean = Array("true", "false").contains(p.toLowerCase)

  /** Parse download value from standard querystring
   *
   *  @param param      desired parameter: download
   *  @param constraint validation constraint
   *  @return boolean string or illegal argument exception
   */
  private[this] def parse(param: String, constraint: String => Boolean): Boolean =
    querystring
      .get(param)
      .map(_.headOption.filter(isValid))
      .map(_.getOrElse(invalid(s"$param parameter")))
      .contains("true")
}
