package querymodule.query

import play.api.libs.typedmap.TypedKey

/** Constants for Query-related models
 */
object QueryConstants {
  val ASC = "ASC"
  val BY = "by"
  val COL = "col"
  val CURRENT = "current"
  val DATE = "date"
  val DESC = "DESC"
  val DIMENSION = "dimension"
  val DOWNLOAD = "download"
  val DIR = "dir"
  val END = "end"
  val FILTERS = "filters"
  val GT = "GT"
  val GTE = "GTE"
  val JSON = "json"
  val LIKE = "LIKE"
  val LIMIT = "limit"
  val LT = "LT"
  val LTE = "LTE"
  val NE = "NE"
  val NEXT = "next"
  val OP = "op"
  val ORDER = "order"
  val PAGE = "page"
  val PAGES = "pages"
  val PREVIOUS = "previous"
  val QUERY = "querymodule/query"
  val RECORDS = "records"
  val SHOWING = "showing"
  val START = "start"
  val TITLE = "title"
  val TOTAL = "total"
  val TOTALS = "totals"
  val VALUE = "value"
  val WARNINGS = "warnings"

  val Date: TypedKey[QueryDate] = TypedKey.apply(DATE)
  val Dimension: TypedKey[QueryDimension] = TypedKey.apply(DIMENSION)
  val Filters: TypedKey[Seq[QueryFilter]] = TypedKey.apply(FILTERS)
  val Order: TypedKey[QueryOrder] = TypedKey.apply(ORDER)
  val Page: TypedKey[QueryPage] = TypedKey.apply(PAGE)
  val Query: TypedKey[QueryRequest] = TypedKey.apply(QUERY)
}
