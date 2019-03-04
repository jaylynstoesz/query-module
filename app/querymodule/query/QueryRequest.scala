package querymodule.query

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import QueryConstants._
import com.github.tototoshi.csv._
import org.joda.time.LocalDate

object QueryRequest {
  def empty = QueryRequest(dateDefault)

  def dateDefault: Map[String, Seq[String]] = {
    import querymodule.exporter.ExportFormatting.dateFriendly

    val today = dateFriendly(new LocalDate)

    Map(
      s"$DATE.$START" -> Seq(today),
      s"$DATE.$END" -> Seq(today)
    )
  }

}

/** Standard parser, container, and formatter for all incoming queries
 *
 *  @param queryString querystring parameters extracted from an HTTP request
 */
case class QueryRequest(queryString: Map[String, Seq[String]]) {
  lazy val cacheId: String = this.hashCode().toString

  /** Parse and format date parameters
   *
   *  Defaults to today's date if parameters are not explicitly passed
   */
  lazy val dateAttr: QueryDate = {
    val qs = QueryRequest.dateDefault ++ queryString

    QueryDate(qs.filter(q => isA(DATE, q._1)))
  }

  /** Parse and format dimension parameter
   */
  lazy val dimensionAttr: QueryDimension = QueryDimension(queryString.filter(q => isA(DIMENSION, q._1)))

  /** Parse and format download parameter
   */
  lazy val downloadAttr: QueryDownload = QueryDownload(queryString.filter(q => isA(DOWNLOAD, q._1)))

  /** Parse and format filter parameters
   */
  lazy val filtersAttr: Seq[QueryFilter] =
    queryString
      .collect {
        case q if isFilter(q._1) => QueryFilter(q)
      }.toSeq

  /** Parse and format order parameters
   */
  lazy val orderAttr: QueryOrder = QueryOrder(queryString.filter(q => isA(ORDER, q._1)))

  /** Parse and format page parameters
   */
  lazy val pageAttr: QueryPage = QueryPage(queryString.filter(q => isA(PAGE, q._1)))

  /** Shortcut to paginate attribute
   *  Do not paginate if downloading result set
   */
  def paginate: Boolean = !downloadAttr.download

  /** Shortcut to download attribute
   */
  def download: Boolean = downloadAttr.download

  /** Shortcut to dimension attribute
   */
  def dimension: Option[String] = dimensionAttr.dimension

  /** Append this object to request as an attribute (used for executing queries)
   *
   *  @param request HTTP request
   *  @tparam A implicit type
   *  @return modified request
   */
  def withParameters[A](request: Request[A]): Request[A] =
    request.addAttr(Query, this)

  /** Must have at least one filter
   *
   *  @param default default filter, i.e. "company_id IS NOT NULL"
   *  @return SQL filter
   */
  def filterClause(default: String): String = {
    s"$default" + (if (filtersAttr.nonEmpty) {
      s" AND ${filtersAttr.map(_.clause).mkString(" AND ")}"
    } else {
      ""
    })
  }

  /** Output standard response for a reporting query_module.query
   *
   *  @param queryResponse standard QueryResponse instance
   *  @return standard JSON response with dimension, date, order, filters, page, totals, and records values
   */
  def standardJsonResponse(
    title:         String        = "Report",
    queryResponse: QueryResponse
  ): JsObject = {
    import querymodule.exporter.ExportFormatting.concat

    concat(
      Json.obj(TITLE -> title),
      dimensionAttr.json,
      downloadAttr.json,
      dateAttr.json,
      orderAttr.json,
      Json.obj(FILTERS -> filtersAttr.flatMap(_.json)),
      pageAttr.jsonWithTotals(queryResponse.records.size, queryResponse.total),
      Json.obj(WARNINGS -> queryResponse.warnings),
      Json.obj(TOTALS -> queryResponse.totals.map(_.jsonTotal)),
      Json.obj(RECORDS -> queryResponse.records.map(_.json))
    )
  }

  /** Output standard response for a reporting query_module.query
   *
   *  @param queryResponse standard QueryResponse instance
   *  @return standard CSV response with dimension, date, order, filters, page, totals, and records values
   */
  def standardCSVResponse(
    title:         String        = "Report",
    queryResponse: QueryResponse
  ): File = {
    val file = new File(s"/tmp/${title.replaceAllLiterally(" ", "-")}.csv")
    val writer = CSVWriter.open(file)

    if (queryResponse.warnings.nonEmpty) {
      queryResponse.warnings.foreach { warning =>
        writer.writeRow(Array(s"WARNING! $warning"))
      }
    }

    val headers: Seq[String] = queryResponse.records.headOption.map(_.csvHeaders).getOrElse(Seq("No records"))
    writer.writeRow(headers)

    queryResponse.records.foreach { record =>
      writer.writeRow(record.csvValues())
    }

    writer.close()

    file
  }

  /** Check if query_module.query parameter is a filter, i.e. it is not any of the other parameter types
   *
   *  @param p parameter (key)
   *  @return boolean is a filter
   */
  private[this] def isFilter(p: String): Boolean =
    !isA(DATE, p) && !isA(DIMENSION, p) && !isA(DOWNLOAD, p) && !isA(ORDER, p) && !isA(PAGE, p)

  /** Check if query_module.query parameter is of a certain type, i.e. it has a supported prefix
   *
   *  @param prefix date, dimension, order, or page
   *  @param p      parameter (key)
   *  @return boolean is a parameter of the given type
   */
  private[this] def isA(prefix: String, p: String): Boolean = p.startsWith(s"$prefix")
}
