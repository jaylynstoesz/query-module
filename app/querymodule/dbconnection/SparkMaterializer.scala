package querymodule.dbconnection

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Column, DataFrame, SaveMode, SparkSession}
import play.api.{Configuration, Environment, Mode}
import querymodule.dbconnection.DbConnectionErrors.ExistingSparkSessionException
import querymodule.query.QueryFilter

trait SparkMaterializer {
  /** Initialize a Spark session
   *
   *  @param title             Application title - must be unique while running, visible in Spark UI
   *  @param master            cluster address
   *  @param driverMemory      driver memory
   *  @param maxCores          max number of cores for this application
   *  @param executorCores     number of cores per executor
   *  @param executorMemory    memory allocation per executor (NOT per core)
   *  @param shufflePartitions number of partitions allowed during a shuffle (default 200)
   *  @return Spark session
   */
  def initializeSparkSession(
    title:             String,
    master:            String,
    driverMemory:      String,
    maxCores:          Int,
    executorCores:     Int,
    executorMemory:    String,
    shufflePartitions: Int
  )(implicit environment: Environment): SparkSession = {

    try {
      val pkg = if (environment.mode == Mode.Prod) SparkConnection.packages.mkString(",") else ""

      SparkSession
        .builder()
        .appName(title)
        .master(master)
        .config("spark.driver.memory", driverMemory)
        .config("spark.cores.max", maxCores)
        .config("spark.executor.cores", executorCores)
        .config("spark.executor.memory", executorMemory)
        .config("spark.jars", pkg)
        .config("spark.sql.shuffle.partitions", shufflePartitions.toString)
        .getOrCreate()
    } catch {
      case e: Exception =>
        throw ExistingSparkSessionException()
    }
  }

  /** Filter records from a dataframe given a set of filters
   *
   *  @param df      Spark dataframe
   *  @param filters standard query filters
   *  @return filtered dataframe
   */
  def filterRows(df: DataFrame, filters: Seq[QueryFilter]): DataFrame = {
    if (filters.nonEmpty) {
      filterRows(df.filter(filters.head.clause), filters.tail)
    } else {
      df
    }
  }

  /** Filter records with null values in specified columns from a dataframe
   *
   *  @param df   Spark dataframe
   *  @param cols non-nullable columns
   *  @return filtered dataframe
   */
  def filterNulls(df: DataFrame, cols: List[String]): DataFrame = {
    if (cols.nonEmpty) {
      val col = cols.head
      filterNulls(
        df.filter(s"$col IS NOT NULL AND $col > 0"),
        cols.tail
      )
    } else {
      df
    }
  }

  /** Read a table that has been loaded into Spark
   *
   *  @param name  table name
   *  @param spark existing Spark session
   *  @return table as dataframe
   */
  def readRows(name: String)(implicit spark: SparkSession): DataFrame = {
    spark
      .read
      .table(name)
  }

  /** Map a list of strings to Columns without an alias in Spark
   *
   *  @param cols  list of column names
   *  @param spark Spark session
   *  @return list of Columns
   */
  def strToColumn(cols: List[String])(implicit spark: SparkSession): List[Column] = {
    import spark.implicits._

    cols.map(d => $"$d")
  }

  /** Map a list of strings to Columns with an alias in Spark
   *
   *  @param cols  list of column names with corresponding aliases
   *  @param spark Spark session
   *  @return list of aliased Columns
   */
  def mapToColumn(cols: Map[String, String])(implicit spark: SparkSession): List[Column] = {
    import spark.implicits._

    cols
      .map(d => $"${d._1}".alias(s"${d._2}"))
      .toList
  }

  /** Join two dataframes in Spark
   *
   *  @param datasetA dataframe A
   *  @param datasetB dataframe B
   *  @param joinType Type of join to perform. Default `inner`. Must be one of:
   *                 `inner`, `cross`, `outer`, `full`, `full_outer`, `left`, `left_outer`,
   *                 `right`, `right_outer`, `left_semi`, `left_anti`.
   *  @return joined dataframe
   */
  def joinDataframes(datasetA: DataFrame, datasetB: DataFrame, joinType: String = "inner"): DataFrame = {
    datasetA
      .join(
        datasetB,
        datasetB.columns.intersect(datasetA.columns),
        joinType
      )
  }

  /** Write a dataframe to a table in MySQL
   *
   *  @param dataframe     contructed dataframe
   *  @param table         name of the table
   *  @param cxn           DB connection
   *  @param configuration global configuration
   */
  def writeJDBC(
    dataframe: DataFrame,
    table:     String,
    cxn:       JdbcConnection
  )(implicit configuration: Configuration): Unit = {
    dataframe
      .write
      .mode(SaveMode.Append)
      .jdbc(cxn.url, s"${table}_tmp", cxn)
  }

  def writeParquet(
    dataframe: DataFrame,
    dest:      String
  )(implicit environment: Environment): Unit = {
    dataframe
      .write
      .mode(SaveMode.Overwrite)
      .parquet(s"$dest/${environment.mode.toString}")
  }

  /** Save a dataframe to Spark cluster memory
   *
   *  @param dataframe constructed dataframe
   *  @param viewName  name of dataframe
   *  @return loaded dataframe
   */
  def save(dataframe: DataFrame, viewName: String)(implicit environment: Environment): Unit = {
    dataframe.createOrReplaceTempView(viewName)
  }

  /** Retrieve saved dataframe from memory
   *
   *  @param viewName     name of dataframe
   *  @param sparkSession Spark session
   *  @return loaded dataframe
   */
  def readSaved(viewName: String)(implicit sparkSession: SparkSession): DataFrame = {
    sparkSession.table(viewName)
  }

  /** Read JDBC records into a dataframe
   *
   *  @param properties JDBC connection properties
   *  @param table      or query being read
   *  @example "line_item"
   *  @example ((SELECT * FROM network) AS networks)
   *  @param sparkSession Spark session
   *  @return loaded dataframe
   */
  def readJDBC(
    properties: JdbcConnection,
    table:      String
  )(implicit sparkSession: SparkSession, configuration: Configuration): DataFrame = {
    val url: String = properties.url
    val driver: String = properties.getProperty("driver")
    val user: String = properties.getProperty("user")
    val password: String = properties.getProperty("password")

    sparkSession
      .read
      .format("jdbc")
      .option("url", url)
      .option("driver", driver)
      .option("user", user)
      .option("password", password)
      .option("sslMode", "disable")
      .option("dbtable", table)
      .load()
  }

  /** Read CSV files into a dataframe
   *
   *  @param source       path to files
   *  @param schema       typsesafe data structure
   *  @param sparkSession Spark session
   *  @return loaded dataframe
   */
  def readCSV(
    source: String,
    schema: StructType
  )(implicit sparkSession: SparkSession): DataFrame = {
    val data = sparkSession
      .read
      .format("csv")
      .option("delimiter", "\t")
      .option("nullValue", "\\N")
      .option("header", "false")
      .load(source)
      .toDF(schema.fieldNames: _*)

    formatSchema(schema, data)
  }

  /** add column names and formats to a raw DataFrame
   *
   *  @param schema desired schema
   *  @param df     raw data
   *  @param spark  existing Spark session
   *  @return formatted data
   */
  def formatSchema(
    schema: StructType,
    df:     DataFrame
  )(implicit spark: SparkSession): DataFrame = {
    import spark.implicits._

    df.select(
      schema
        .map { c =>
          $"${c.name}".cast(c.dataType).alias(c.name)
        }: _*
    )
  }

  /** Read Parquet files into a dataframe
   *
   *  @param source       path to files
   *  @param inferSchema  infer schema from files (for those that have previously been formatted)
   *  @param sparkSession Spark session
   *  @return loaded dataframe
   */
  def readParquet(
    source:      String,
    inferSchema: Boolean = true
  )(implicit sparkSession: SparkSession, environment: Environment): DataFrame = {
    sparkSession
      .read
      .option("inferSchema", inferSchema.toString)
      .parquet(s"$source/${environment.mode.toString}")
  }
}
