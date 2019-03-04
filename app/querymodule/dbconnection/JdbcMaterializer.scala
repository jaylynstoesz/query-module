package querymodule.dbconnection

import anorm.SqlParser.scalar
import anorm.{NamedParameter, ResultSetParser, RowParser, SQL, SqlQuery}
import querymodule.dbconnection.DbConnectionErrors.FailedJdbcException
import querymodule.query.QueryRequest

/** Utility for executing and parsing SQL queries with Anorm
 */
trait JdbcMaterializer {
  /** Get single integer value (count) from an SQL query, if paginating result set
   *
   *  @param statement       SQL query
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param queryRequest    standard query request
   *  @param cxn             database connection
   *  @return N rows
   */
  def count(
    statement:       String,
    namedParameters: NamedParameter*
  )(implicit queryRequest: QueryRequest, cxn: JdbcConnection): Int = {
    execute(SQL(statement), scalar[Int].single, namedParameters: _*)
  }

  /** Interpolate query with named parameters, execute, and parse
   *
   *  @param statement       SQL query
   *  @param parser          ResultSetParser
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param cxn             database connection
   *  @tparam T return type (inferred from ResultSetParser)
   *  @return parsed result set
   */
  def execute[T](
    statement:       SqlQuery,
    parser:          ResultSetParser[T],
    namedParameters: NamedParameter*
  )(implicit cxn: JdbcConnection): T = {
    try {
      cxn.db.withConnection { implicit connection => statement.on(namedParameters: _*).as(parser) }
    } catch {
      case e: Exception =>
        throw FailedJdbcException()(cxn.db.name, statement)
    }
  }

  /** Get list of ID's from an SQL query
   *
   *  @param statement       SQL query
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param cxn             database connection
   *  @return list of ID's
   */
  def select(
    statement:       String,
    count:           Int,
    maxRecords:      Int,
    namedParameters: NamedParameter*
  )(implicit queryRequest: QueryRequest, cxn: JdbcConnection): Stream[Long] = {
    stream(
      statement = statement,
      count = count,
      batchSize = 10000,
      maxRecords = maxRecords,
      parser = scalar[Long],
      namedParameters = namedParameters: _*
    )
  }

  /** Get stream of results from an SQL query
   *  Useful for very large result sets, like CSV downloads
   *
   *  @param statement       SQL query. Must contain `LIMIT %s, %s` clause
   *  @param count           total number of records in unpaginated result set
   *  @param batchSize       number of records in a "batch", determined using memory footprint and query latency
   *  @param maxRecords      maximum number of records allowed, configurable in application.conf
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param cxn             database connection
   *  @return results
   */
  def stream[T](
    statement:       String,
    count:           Int,
    batchSize:       Int,
    maxRecords:      Int,
    parser:          RowParser[T],
    namedParameters: NamedParameter*
  )(implicit queryRequest: QueryRequest, cxn: JdbcConnection): Stream[T] = {
    val stUpper = statement.toUpperCase

    if (!stUpper.contains("LIMIT")) {
      throw new IllegalArgumentException("Missing `LIMIT [OFFSET], [N]` clause for stream query")
    }

    val range = {
      val rg = 0 to Math.ceil(count / batchSize).toInt
      val ordered = stUpper.contains("ORDER") && !stUpper.contains("ORDER BY NULL")
      /* If results don't need to be in order, run queries in parallel */
      if (ordered) rg else rg.par
    }

    var results = Stream.empty[T]

    range.foreach { i =>
      val offset = i * batchSize

      if (offset < maxRecords) {
        val records = execute(SQL(statement.format(offset, batchSize)), parser.*, namedParameters: _*)
        results = results.append(records.toStream)
      }
    }

    results
  }

  /** Get single row value from an SQL query, if it exists
   *
   *  @param statement       SQL query
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param queryRequest    standard query request
   *  @param cxn             database connection
   *  @return one row, if it exists
   */
  def find[T](
    statement: String, parser: RowParser[T],
    namedParameters: NamedParameter*
  )(implicit queryRequest: QueryRequest, cxn: JdbcConnection): Option[T] = {
    if (!queryRequest.download) {
      execute(SQL(statement), parser.singleOpt, namedParameters: _*)
    } else None
  }

  /** Get list of results from an SQL query
   *
   *  @param statement       SQL query
   *  @param namedParameters set of parameters for query, ex: "ids" -> List(123, 234, 345)
   *  @param cxn             database connection
   *  @return results
   */
  def list[T](
    statement:       String,
    parser:          RowParser[T],
    namedParameters: NamedParameter*
  )(implicit queryRequest: QueryRequest, cxn: JdbcConnection): List[T] = {
    execute(SQL(statement), parser.*, namedParameters: _*)
  }

  /** Copy a table temporarily, populate it, name-swap it for the permanent table, and drop old table
   *
   *  @note explicitly pass db connection to avoid mixups
   *  @param cxn     current database connection
   *  @param table   db table being written to
   *  @param process series of functions to execute during ETL
   */
  def tempTableProcess(
    table:   String,
    process: () => Any
  )(implicit cxn: JdbcConnection): Boolean = {
    createTempTable(table)

    process()

    renameTables(table)
    removeTable(table)
  }

  /** Create table to populate with updated insights data
   *  Will be name-swapped with current table and old table will be dropped
   */
  def createTempTable(table: String)(implicit cxn: JdbcConnection): Boolean = {
    cxn.db.withConnection { implicit connection =>
      SQL(s"DROP TABLE IF EXISTS ${table}_tmp").execute()
      SQL(s"CREATE TABLE ${table}_tmp LIKE $table").execute()
    }
  }

  /** Swap existing and temporary table names after
   *  populating temp table with processed insights records
   */
  def renameTables(table: String)(implicit cxn: JdbcConnection): Boolean = {
    cxn.db.withConnection { implicit connection =>
      SQL(s"RENAME TABLE $table TO ${table}_old, ${table}_tmp TO $table").execute()
    }
  }

  /** Drop old insights table after temp table with new data has been made permanent
   */
  def removeTable(table: String)(implicit cxn: JdbcConnection): Boolean = {
    cxn.db.withConnection { implicit connection =>
      SQL(s"DROP TABLE ${table}_old").execute()
    }
  }
}
