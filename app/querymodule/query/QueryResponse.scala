package querymodule.query

import play.api.Configuration
import querymodule.config.ConfigCommon._
import querymodule.exporter.Exportable

/** Output standard response for a reporting query_module.query
 *
 *  @param records       records retrieved from report and converted to JSON
 *  @param totals        totals retrieved from report and converted to JSON
 *  @param total         total number of records in non-paginated series
 *  @param configPath    configuration path for a report
 *  @param configuration global Configuration instance
 */
case class QueryResponse(
    queryRequest: QueryRequest       = QueryRequest.empty,
    records:      Stream[Exportable] = Stream.empty[Exportable],
    totals:       Option[Exportable] = None,
    total:        Int                = 0,
    configPath:   String             = "query"
)(implicit configuration: Configuration) {
  /** Max records configuration for response
   */
  private[this] val maxRecords: Int = getConfigInt("maxRecords", configPath, Some(total))

  /** Warnings from issues encountered while building report
   */
  var warnings: Array[String] = {
    if (maxRecords < total && queryRequest.download) {
      Array(maxRecordsWarning(maxRecords))
    } else {
      Array.empty
    }
  }

  /** Add a warning to the list of warnings to be displayed with response
   *
   *  @param warning error message
   */
  def addWarning(warning: String): Unit = {
    warnings = warnings ++ Array(warning)
  }

  /** Helper for displaying that N rows exceeds max number of rows allowed
   *
   *  @param n Max number of rows
   */
  private[this] def maxRecordsWarning(n: Int): String = {
    s"The result set has $total rows, greater number than the maximum allowed ($n). Some data has been lost."
  }

}
