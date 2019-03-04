package querymodule.query

import play.api.libs.json.{JsObject, Json}
import QueryConstants.{COL, GT, GTE, LT, LTE, NE, OP, VALUE, LIKE}
import querymodule.exporter.ExportFormatting.toSnakeCase

import scala.util.Try

/** OPTIONAL: parse filter parameters from query_module.query string
 *  GET /path?record.id=123&metric=500(gte)
 *
 *  @param querystring filter parameters
 */
case class QueryFilter(querystring: (String, Seq[String])) {
  /** Filter column reference
   *  Given /...?record.id=123 then col = record.id
   */
  val col: String = querystring._1

  /** Filter value(s) reference
   *  Given /...?record.id=123 then values = [123] OR
   *  given /...?record.id=123&record.id=456 then values = [123, 456]
   */
  val values: Seq[(String, Option[String])] = parse

  /** Standard JSON output for date object in HTTP response
   */
  val json: Seq[JsObject] = values.map(v => Json.obj(COL -> col, VALUE -> v._1, OP -> v._2))

  /** Build an SQL OR clause from list of conditions
   */
  def clause: String = s"(${conditions().mkString(" OR ")})"

  /** Build an SQL OR clause from list of conditions
   *  without normalizing column in snake_case
   */
  def clauseUnmapped: String = s"(${conditions(unmapped = true).mkString(" OR ")})"

  /** Build an SQL WHERE clause using a value and its operator
   *
   *  @param unmapped if true, use column exactly the way it was passed.
   *                 Otherwise, normalize to standard snake_case.
   */
  def conditions(unmapped: Boolean = false): Seq[String] = {
    if (col.contains("name")) {
      values
        .map(v => splitSubstr(v._1))
        .map { v =>
          v.map { u =>
            s"${if (unmapped) col else toSnakeCase(col)} $LIKE '${u.replaceAllLiterally("*", "%")}'"
          }.mkString(" AND ")
        }
    } else {
      values map { v =>
        val operator = v._2.map(opMapping).getOrElse("=")

        val value: Any =
          Try(v._1.toLong)
            .getOrElse(
              Try(v._1.toDouble)
                .getOrElse(
                  Try(v._1.toBoolean)
                    .getOrElse(s"'${v._1}'".replaceAllLiterally("*", "%"))
                )
            )

        s"${if (unmapped) col else toSnakeCase(col)} $operator $value"
      }
    }
  }

  def splitSubstr(v: String) = {
    v.split(" ")
      .map { part => s"*$part*" }
  }

  /** Map an operator parameter in the querystring to its counterpart in SQL
   *  @param op operator. Must be a case-insensitive version of
   *           "gt", "gte", "lt", "lte", or "ne".
   *           "eq" or "=" is implied (not passed)
   */
  def opMapping(op: String): String = {
    op.toUpperCase match {
      case GT   => ">"
      case GTE  => ">="
      case LT   => "<"
      case LTE  => "<="
      case NE   => "!="
      case LIKE => LIKE
      case _    => throw new IllegalArgumentException(s"invalid $OP parameter $op")
    }
  }

  /** Parse filter column, operator, and values from standard querystring
   *
   *  @return filter or illegal argument exception
   */
  def parse: Seq[(String, Option[String])] = {
    val s: Seq[String] = querystring._2
    s.map { p =>
      if (p.startsWith("*")) {
        if (isValidCol(p)) (p, None) else invalidValues(p)
      } else {
        val parts = p.split("\\(")
        val v: String = parts.head
        val op: Option[String] = parts.tail.headOption.map(t => if (t.endsWith(")")) t.dropRight(1) else t)

        if (isValidCol(v) && op.forall(isValidOp)) {
          (v, op)
        } else {
          invalidValues(p)
        }
      }
    }
  }

  /** Check if operator is valid. Must be a case-insensitive version of
   *           "gt", "gte", "lt", "lte", or "ne".
   *           "eq" or "=" is implied (not passed)
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValidOp(p: String): Boolean = Array(GT, GTE, LT, LTE, NE, LIKE).contains(p.toUpperCase)

  /** Check if filter column is valid. Must be a non-empty string.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValidCol(p: String): Boolean = p.nonEmpty

  /** Check if filter value is valid. Must be a non-empty string.
   *
   *  @param p query_module.query parameter value
   *  @return whether value is valid
   */
  def isValidValue(p: String): Boolean = p.nonEmpty

  /** Check if a value is present in a set of filters
   *
   *  @param p desired query_module.query parameter value
   *  @return whether a value is present
   */
  def containsValue(p: String): Boolean = {
    values.exists(_._1 == p)
  }

  /** Check if a value is the only one present in a set of filters
   *
   *  @param p desired query_module.query parameter value
   *  @return whether a value is the only one present
   */
  def containsExactValue(p: String): Boolean = {
    containsValue(p) && values.forall(_._1 == p)
  }

  /** Throw an exception if one or more columns is invalid
   *
   *  @param param column
   *  @return exception with failure detail
   */
  def invalidValues(param: String) = {
    throw new IllegalArgumentException(s"invalid filter $param")
  }
}
