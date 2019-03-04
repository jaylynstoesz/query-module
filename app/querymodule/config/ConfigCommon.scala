package querymodule.config

import play.api.db.Database
import play.api.{Configuration, Environment, Logger, Mode}

import scala.util.Try

object ConfigCommon {

  import querymodule.config.ConfigErrors._

  /** Get DB config
   *
   *  @param key           config key
   *  @param configPath    config path
   *  @param configuration config object
   *  @return string config parameter
   */
  def getConfigStr(
    key:        String,
    configPath: String,
    default:    Option[String] = None
  )(implicit configuration: Configuration): String = {
    getConfig[String](
      configuration.get[String](s"$configPath.$key"),
      key,
      configPath
    )
  }

  /** Get DB config
   *
   *  @param key           config key
   *  @param configPath    config key
   *  @param configuration config object
   *  @return integer config parameter
   */
  def getConfigInt(
    key:        String,
    configPath: String,
    default:    Option[Int] = None
  )(implicit configuration: Configuration): Int = {
    getConfig[Int](configuration.get[Int](s"$configPath.$key"), key, configPath)
  }

  /** Get DB config
   *
   *  @param key           config key
   *  @param configPath    config key
   *  @param configuration config object
   *  @return boolean config parameter
   */
  def getConfigBool(
    key:        String,
    configPath: String,
    default:    Option[Boolean] = None
  )(implicit configuration: Configuration): Boolean = {
    getConfig[Boolean](configuration.get[Boolean](s"$configPath.$key"), key, configPath)
  }

  /** Get DB config
   *
   *  @param key           config key
   *  @param configPath    config path
   *  @param configuration config object
   *  @return string config parameter
   */
  def getConfigListStr(
    key:        String,
    configPath: String,
    default:    Seq[String] = Nil
  )(implicit configuration: Configuration): Seq[String] = {
    getConfig[Seq[String]](
      configuration.get[Seq[String]](s"$configPath.$key"),
      key,
      configPath
    )
  }

  /** Get DB config
   *
   *  @param key           config key
   *  @param configPath    config path
   *  @param configuration config object
   *  @return string config parameter
   */
  def getConfigListInt(
    key:        String,
    configPath: String,
    default:    Seq[Int] = Nil
  )(implicit configuration: Configuration): Seq[Int] = {
    getConfig[Seq[Int]](
      configuration.get[Seq[Int]](s"$configPath.$key"),
      key,
      configPath
    )
  }

  /** Utility wrapper for DB config getter
   *
   *  @param fn         get DB config
   *  @param key        config key
   *  @param configPath config path
   *  @param default    default value
   *  @tparam A config Type
   *  @return config parameter or a MissingConfigException
   */
  private[this] def getConfig[A](
    fn:         => A,
    key:        String,
    configPath: String,
    default:    Option[A] = None
  ): A = {
    Try {
      fn
    } getOrElse {
      default.getOrElse {
        throw MissingConfigException()(s"$configPath.$key")
      }
    }
  }

  /** Output start/stop in console
   *
   *  @param isStart is beginning of process
   *  @param message process description
   */
  def logProgress(isStart: Boolean = true, message: String): Unit = {
    Logger.info(s"${if (isStart) "Starting" else "Finished"} $message")
  }

  /** Check application mode (prod vs dev) against db connection string
   *
   *  @param environment global environment
   *  @param database    database connection
   */
  def checkEnv(database: Database)(implicit environment: Environment): Unit = {
    if (environment.mode != Mode.Prod && !database.url.contains("localhost")) {
      throw DangerousEnvironmentException()
    }
  }
}
