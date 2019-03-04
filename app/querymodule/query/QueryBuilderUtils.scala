package querymodule.query

trait QueryBuilderUtils {
  import querymodule.exporter.ExportFormatting._
  /** Builds a HAVING clause in MySQL given a standard query and a set of valid filters
   *  on calculated fields
   *  Tosses out invalid filters, applies valid ones
   *
   *  @param queryRequest standard query
   *  @param validFilters valid filter names
   *  @return MySQL HAVING clause
   */
  def havingClause(queryRequest: QueryRequest, validFilters: List[String]): String = {
    val filters = queryRequest.filtersAttr.filter(f => isValidFilter(f, validFilters))

    if (filters.nonEmpty) {
      "having " + filters.map(_.clause).mkString("\nAND ")
    } else {
      ""
    }
  }

  /** Builds an LIMIT clause in MySQL given a standard query given a standard query
   *
   *  @param queryRequest standard query
   *  @return MySQL LIMIT clause
   */
  def limitClause(queryRequest: QueryRequest): String = {
    s"limit ${queryRequest.pageAttr.offset}, ${queryRequest.pageAttr.limit}"
  }

  /** Builds an ORDER BY clause in MySQL given a standard query given a standard query
   *
   *  @param queryRequest standard query
   *  @return MySQL ORDER BY clause
   */
  def orderClause(queryRequest: QueryRequest, validOrders: List[String]): String = {
    import querymodule.exporter.ExportFormatting.toSnakeCase

    if (isValidOrder(queryRequest.orderAttr, validOrders)) {
      val by = queryRequest.orderAttr.by.collect {
        case b if b.contains("status.created_on") => toSnakeCase(b.replaceAllLiterally("status.created_on", "id"))
        case b                                    => toSnakeCase(b)
      }.getOrElse("null")
      val dir = {
        queryRequest.orderAttr.dir.getOrElse("")
      }
      s"order by $by $dir"
    } else {
      "order by null"
    }
  }

  /** Builds a WHERE clause in MySQL given a standard query and a set of valid filters
   *  on NON-calculated fields
   *  Tosses out invalid filters, applies valid ones
   *
   *  @param queryRequest standard query
   *  @param validFilters valid filter names
   *  @return MySQL WHERE clause
   */
  def whereClause(queryRequest: QueryRequest, validFilters: List[String]): String = {
    val filters = queryRequest.filtersAttr.filter(f => isValidFilter(f, validFilters))

    if (filters.nonEmpty) {
      "where " + filters.map(_.clause).mkString("\nAND ")
    } else {
      ""
    }
  }

  /** Maps dimension from dot, etc. notation to snake_case given a standard query and a set of valid dimensions
   *
   *  @param queryRequest    standard query
   *  @param validDimensions valid dimensions
   *  @return snake_case dimension
   */
  def mapDimension(queryRequest: QueryRequest, validDimensions: List[String]): String = {
    import querymodule.exporter.ExportFormatting.toSnakeCase

    val dimension = toSnakeCase(queryRequest.dimensionAttr.dimension.getOrElse(""))

    val dimensionsWithSuffix = validDimensions.map(v => s"${v}_id")

    dimension match {
      case d if validDimensions.contains(d) => s"${d}_id"
      case d if validDimensions.contains(s"${d}_id") => s"${d}_id"
      case d if dimensionsWithSuffix.contains(d) => d
      case d if dimensionsWithSuffix.contains(s"${d}_id") => s"${d}_id"
      case e => throw new IllegalArgumentException(s"Invalid dimension '$e'")
    }
  }

  /** List of columns available for filtering
   *  Congruent with query column headers by default
   */
  def validOrders: List[String] = columns.map(_._2)

  /** List of columns available for filtering
   *  Congruent with query column headers by default
   */
  def validFilters: List[String] = columns.map(_._2)

  /** Build SELECT clause for row data query
   *  ([schema.selection] -> [snake_case_column_header])
   *
   *  @note Column headers should be a snake_case transformation of the JSON payload
   *  e.g. record_attribute_id => record.attribute.id =>
   *  {{{
   *  {
   *   "record": {
   *       "attribute": {
   *         "id": 12345
   *         ...
   *         }
   *       ...
   *       }
   *    ...
   *    }
   *  }
   *  }}}
   */
  def columns: List[(String, String)]

}
