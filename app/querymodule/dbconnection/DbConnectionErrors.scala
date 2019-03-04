package querymodule.dbconnection

import anorm.SqlQuery

object DbConnectionErrors {

  /** For use when an error occurs because a Spark session is already running in the JVM
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class ExistingSparkSessionException(
      private val message: String    = "Spark Session already running",
      private val cause:   Throwable = None.orNull
  ) extends Exception(message, cause)

  /** For use when an error occurs because application failed to make a Jdbc connection
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class FailedJdbcException(
      private val message: String    = "Failed to connect to s% to execute query: %s",
      private val cause:   Throwable = None.orNull
  )(name: String, query: SqlQuery) extends Exception(message.format(name, query.toString().take(1000)), cause)
}
