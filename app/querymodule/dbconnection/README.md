# Connecting to a Database

### Overview

This set of tools is used for:
* [making a JDBC connections](#connect-using-jdbc)
* [listing, counting, and streaming results of SQL queries](#execute-sql-queries)
* [connecting to a Spark cluster](#connect-to-spark)
* [executing common Spqrk SQL commands](#execute-spark-commands)

Please familiarize yourself with the [Spark SQL documentation](https://spark.apache.org/docs/2.2.0/sql-programming-guide.html) before using Spark components.

### Usage

#### Import the models you need

In your code, import the models you need from the library:
```
import querymodule.dbconnection.{ ... }
```

#### Connect using jdbc

1) Configure connections using the Play `db.default` configuration
file: `/conf/application.conf`. See [Play documentation](https://www.playframework.com/documentation/2.6.x/ScalaDatabase) for details.

You should have three config files:
* `conf/application.conf` (development, __always points to a stage db!__)
* `conf/stage/application.conf` (stage environment, __always points to a stage db!__)
* `conf/prod/application.conf` (production)

2) Create a DB connection that is an instance of `JdbcConnection`.
_Note: class will throw an error if configuration is missing,
or if pointed at a database without "stage" in the url in development config._

```
package connections

import javax.inject.Inject
import play.api.db.Database
import play.api.{Configuration, Environment}
import querymodule.dbconnection.JdbcConnection

/** Connection to Broadway RDS instance
 *
 *  @param database      db.default connection
 *  @param configuration global configuration
 *  @param environment   global environment
 */
class BroadwayConnection @Inject() (
    database: Database 
    // to use non-default db: @NamedDatabase("alternativedb") database: Database
)(
    implicit
    configuration: Configuration,
    environment:   Environment
) extends JdbcConnection(database)
```

3) Pass connection instance to classes that need them, and execute queries from it:

```
case class ReportRow(id: Long, name: String, revenue: Double)

class SampleReport @Inject() {
        implicit cxn:   BroadwayConnection,
        ec:             ExecutionContext
} {
      import SampleReport._

      val parser: RowParser[ReportRow] = ...

      def run(implicit queryRequest: QueryRequest): List[ReportRow] = {
            cxn.list(query, parser)
      }
}
```

#### Execute SQL queries

Various common functions are available for retrieving records in the `JdbcMaterializer` trait, which is extended by `JdbcConnection`:
* `count`: fetch number of records in a set of query results
* `stream`: get a `Stream` of query results in batches - use whenever the result set could be very large and eat up a lot of memory or CPU
* `list`: get a `List` of query results - only use if results are being immediately transformed and result set won't get too large
* `select`: get a `Stream` of ID's in batches
* `find`: get an `Option[T]` of a query result - statement should return a single result or 0 results

A custom `execute` function is also available for wrapping queries that may not fit into any of the functions above.

"Temp" table handling is available via the `tempTableProcess` function, which follows this pattern.
__MAKE SURE YOU ARE POINTED AT THE CORRECT DATABASE BEFORE EXECUTING THESE QUERIES!__

```
DROP TABLE IF EXISTS `table_tmp`;
CREATE TABLE `table_tmp` LIKE `table`;

// Populate temp table

RENAME TABLE `table` TO `table_old`, `table_tmp` TO `table`;
DROP TABLE `table_old`;
```

#### Connect to Spark

1) Configure connections in a new `spark` configuration in `/conf/application.conf`.
See [Play documentation](https://www.playframework.com/documentation/2.6.x/ScalaDatabase) for details.

Define the following configurations, tweaking each of them to optimize for you job's needs:
```
spark {
  # master = "local[*]"
  master = "spark://ec2-00-00-00-000.us-west-2.compute.amazonaws.com:7077"
  enabled = false
  initialDelay = 0
  cadence = 60
  driverMemory = "1g"
  executorCores = "1"
  executorMemory = "1g"
  maxCores = 8
  shufflePartitions = 8
}
```

You should have three config files:
* `conf/application.conf` (development, `spark.master` config always points to `local[*]` - connecting to the production Spark cluster will fail)
* `conf/stage/application.conf` (stage environment, `spark.master` config can point to production Spark cluster)
* `conf/prod/application.conf` (production)

2) Import `SparkConnection` wherever it's needed. You can import it directly, rather than instantiating multiple
instances (you can only have one Spark connection per JVM)
_Note: class will throw an error if configuration is missing._

```
package tasks

class ETLScheduler @Inject() (
    actorSystem:                   ActorSystem,
    @Named("etl-actor") someActor: ActorRef
)(
    implicit
    executionContext:   ExecutionContext,
    configuration:      Configuration,
    sparkConnection:    SparkConnection,
    jdbcConnection:     JdbcConnectionInstance,
    environment:        Environment
) {
    //...
}
```

3) Initialize a Spark Session and execute jobs within it:

```
class SparkJob @Inject()(
    implicit
    executionContext:   ExecutionContext,
    configuration:      Configuration,
    environment:        Environment,
    sparkConnection:    SparkConnection
) {
    val title: String = "My Spark Job"

     def run() = {
         import play.api.Logger

         if (sparkConnection.enabled) {
               val startTime = LocalDateTime.now()

               val sparkSession: SparkSession = sparkConnection.start(title)

               try {
                 process(sparkSession)
                 sparkSession.close()

                 Logger.info(s"Completed $title")
               } catch {
                 case e: Exception =>
                   sparkSession.close()

                   val endTime = LocalDateTime.now()
                   val afterMS: Int = endTime.getMillisOfDay - startTime.getMillisOfDay

                   Logger.info(s"Failed $title after $afterMs milliseconds")
               }
         }
     }

     def process(sparkSession: SparkSession): Any = {
        //... Run Spark job(s)
     }
}
```

__Note: make sure you close your Spark Session when you're done with it! You must close the session in
order to release the custer resources you're using.__

#### Execute Spark commands

Various common functions are available for reading, analyzing, formatting,
and writing records in the `SparkMaterializer` trait, which is extended by `SparkConnection`:
* initializing a Spark session
* reading parquet files
* reading CSV files
* querying SQL tables
* writing parquet files in S3
* writing rows to an SQL database via JDBC
* filtering DataFrames
* joining DataFrames
* formatting DataFrames in the proper data type and with the correct column headers
* saving DataFrames as temporary views and retrieving them later

Please see code documentation and Spark Sql official documentation for details on how to implement these
components properly.

### Important Links

Full Scaladocs (_TODO_)


