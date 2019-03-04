package querymodule.query

object QueryErrors {

  /** For use when an error occurs because parsing a query_module.query string failed
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class MalformedQueryException(
      private val message: String    = "Could not parse Query attribute from request context.",
      private val cause:   Throwable = None.orNull
  ) extends Exception(message, cause)

  /** For use when an error occurs because parsing a query_module.query string failed
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class InvalidQueryException(
      private val message: String    = "One or more values in the querystring is invalid.",
      private val cause:   Throwable = None.orNull
  ) extends Exception(message, cause)
}
