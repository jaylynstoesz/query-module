package querymodule.query

import play.api.libs.json.JsObject

/** Base class for query_module.query parser case classes
 */
trait QueryParser {

  /** Standard JSON output for date object in HTTP response
   */
  def json: JsObject

  /** Check if dimension is valid. Must be a non-empty string.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValid(p: String): Boolean

  /** Throw an exception if parameter is invalid
   *
   *  @param param desired parameter: dimension
   *  @return exception with failure detail
   */
  def invalid(param: String) = {
    throw new IllegalArgumentException(s"invalid or missing $param")
  }
}
