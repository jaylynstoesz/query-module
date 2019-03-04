package querymodule.dbconnection

import javax.inject.{Inject, Singleton}
import org.apache.spark.sql.SparkSession
import play.api.{Configuration, Environment}
import querymodule.config.ConfigCommon._

object SparkConnection extends SparkMaterializer {
  val SPARKVERSION = "2.2.0"
  val HADOOPVERSION = "2.7.2"
  val FASTERXMLVERSION = "2.9.0"
  val AWSS3AVERSION = "1.7.4"
  val JODAVERSION = "2.10"
  val MYSQLCONNECTORAVERSION = "6.0.6"

  val packages = List(
    s"file:///opt/demand-insights/lib/org.apache.spark.spark-sql_2.11-$SPARKVERSION.jar",
    s"file:///opt/demand-insights/lib/org.apache.spark.spark-core_2.11-$SPARKVERSION.jar",
    s"file:///opt/demand-insights/lib/org.apache.hadoop.hadoop-aws-$HADOOPVERSION.jar",
    s"file:///opt/demand-insights/lib/org.apache.hadoop.hadoop-common-$HADOOPVERSION.jar",
    s"file:///opt/demand-insights/lib/com.amazonaws.aws-java-sdk-$AWSS3AVERSION.jar",
    s"file:///opt/demand-insights/lib/mysql.mysql-connector-java-$MYSQLCONNECTORAVERSION.jar"
  )
}

@Singleton
class SparkConnection @Inject() (
    implicit
    configuration: Configuration,
    environment:   Environment
) {
  import SparkConnection._

  val configPath: String = "spark"

  val enabled: Boolean = getConfigBool("enabled", configPath)
  val initialDelay: Int = getConfigInt("initialDelay", configPath)
  val cadence: Double = getConfigInt("cadence", configPath)
  val driverMemory: String = getConfigStr("driverMemory", configPath)
  val executorCores: Int = getConfigInt("executorCores", configPath)
  val executorMemory: String = getConfigStr("executorMemory", configPath)
  val maxCores: Int = getConfigInt("maxCores", configPath)
  val shufflePartitions: Int = getConfigInt("shufflePartitions", configPath)
  val master: String = getConfigStr("master", configPath)

  /** Start a new Spark session
   *  @note only one session can run at a time in the same JVM.
   */
  def start(title: String): SparkSession = {
    initializeSparkSession(title, master, driverMemory, maxCores, executorCores, executorMemory, shufflePartitions)
  }
}
