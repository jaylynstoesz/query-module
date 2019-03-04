package querymodule.config

object ConfigErrors {

  /** For use when an error occurs because a parameter is missing for a given configuration path
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class MissingConfigException(
      private val message: String    = "Missing config path '%s'.",
      private val cause:   Throwable = None.orNull
  )(configPath: String) extends Exception(message.format(configPath), cause)

  /** For use when config is not pointing at a stage DB in development mode
   *
   *  @param message friendly error message
   *  @param cause   implicit cause
   */
  final case class DangerousEnvironmentException(
      private val message: String    = s"DANGER: you are pointed at a production database!! Please triple-check config settings!",
      private val cause:   Throwable = None.orNull
  ) extends Exception(message, cause)
}
