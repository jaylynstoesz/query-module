package querymodule.dbconnection

import java.util.Properties

import javax.inject.Inject
import play.api.db.Database
import play.api.{Configuration, Environment}
import querymodule.config.ConfigCommon.{checkEnv, getConfigStr}

class JdbcConnection @Inject() (
    database: Database
)(
    implicit
    configuration: Configuration,
    environment:   Environment
) extends Properties with JdbcMaterializer {

  /** Override with true if connecting to non-local instance is permitted in development */
  def allowProductionInDevelopment: Boolean = false

  if (!allowProductionInDevelopment) checkEnv(database)

  implicit def cxn: JdbcConnection = this

  val db: Database = this.database

  val name: String = database.name

  val connection: String = this.url

  configure(this)

  def url(implicit configuration: Configuration): String = getConfigStr("url", configPath)

  def configPath: String = s"db.$name"

  private[this] def configure(properties: Properties)(implicit configuration: Configuration): Unit = {
    properties.setProperty("driver", getConfigStr("driver", configPath))
    properties.setProperty("user", getConfigStr("username", configPath))
    properties.setProperty("password", getConfigStr("password", configPath))
    properties.setProperty("sslMode", "disable")
  }
}
