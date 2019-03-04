package querymodule.exporter

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsObject, Json}
import querymodule.query.{QueryFilter, QueryOrder, QueryRequest}

/** Utilities for JSON responses
 */
object ExportFormatting {
  val delimiters: Array[String] = Array(".", "_", "-")

  /** Append a list of JSON objects
   *
   *  @param objects objects to concatenate
   *  @return final object
   */
  def concat(objects: JsObject*): JsObject = {
    objects.reduce((a, b) => a ++ b)
  }

  /** Utility for converting strings to camelCase
   *
   *  @param str string to be converted
   *  @return camel case string
   */
  def toCamelCase(str: String): String = {
    val letters = str.split("")

    var output: String = ""

    def change(iterator: Array[String]): String = {
      if (iterator.nonEmpty) {
        if (!delimiters.contains(iterator.head)) {
          output += iterator.head
          change(iterator.tail)
        } else {
          output += iterator.tail.head.toUpperCase()
          change(iterator.drop(2))
        }
      } else {
        output
      }
    }

    change(letters)
  }

  /** Utility for converting strings to kebab-case
   *
   *  @param str string to be converted
   *  @return snake case string
   */
  def toKebabCase(str: String): String = toSnakeCase(str, delimiter = "-")

  /** Utility for converting strings to snake_case
   *
   *  @param str       string to be converted
   *  @param delimiter desired delimiter
   *  @return snake case string
   */
  def toSnakeCase(str: String, delimiter: String = "_"): String = {
    val letters = str.split("")

    var output: String = ""

    def change(iterator: Array[String]): String = {
      if (iterator.nonEmpty) {
        if (iterator.head == delimiter) {
          output += iterator.head.toLowerCase
          change(iterator.tail)
        } else if (delimiters.contains(iterator.head)) {
          output += delimiter
          change(iterator.tail)
        } else if (iterator.head == iterator.head.toUpperCase) {
          output += delimiter
          change(iterator.tail)
        } else {
          output += iterator.head
          change(iterator.tail)
        }
      } else {
        output
      }
    }

    change(letters)
  }

  /** Utility for converting strings to dot.notation
   *
   *  @param str string to be converted
   *  @return snake case string
   */
  def toDotNotation(str: String): String = toSnakeCase(str, delimiter = ".")

  /** Get acronym for a name or phrase (i.e. initials)
   *
   *  @param str string to be converted
   *  @return acronym (Foo Bar -> FB)
   */
  def acronym(str: String): String = {
    str.split(" ").map(_.take(1)).mkString("")
  }

  /** Standardized error messaging for a JSON payload
   *
   *  @param code   HTTP response code
   *  @param errors list of JSON objects with errors
   *  @return standard JSON object with error messaging
   */
  def standardErrorList(
    code:   Int,
    errors: List[JsObject] = Nil
  ): Right[Nothing, JsObject] = {
    Right(
      Json.obj(
        "status" -> code,
        "errors" -> errors
      )
    )
  }

  /** Standardized error messaging for a JSON payload
   *
   *  @param code  HTTP response code
   *  @param error error message
   *  @return standard JSON object with error messaging
   */
  def standardError(
    code:  Int,
    error: String
  ): JsObject = {
    Json.obj(
      "status" -> code,
      "error" -> error
    )
  }

  /** Format a datetime in a readable format
   *  @param datetime timestamp
   *  @return standard format datetime
   */
  def dateTimeFriendly(datetime: DateTime): String = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    formatter.print(datetime)
  }

  /** Format a date in a readable format
   *  @param date timestamp
   *  @return standard format date
   */
  def dateFriendly(date: LocalDate): String = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    formatter.print(date)
  }

  /** Parse user initials from user name
   *  @param userName user.full_name
   *  @return "Steve Abrams" => "SA"
   */
  def initials(userName: String): String = {
    userName.split(" ").map(_.take(1)).mkString("")
  }

  /** Determine whether a filter parameter in the standard case is valid,
   *  given a list of valid filters
   *
   *  @param queryFilter standard query parameter
   *  @param valid       list of valid filters
   *  @return filter is valid
   */
  def isValidFilter(queryFilter: QueryFilter, valid: List[String] = Nil): Boolean = {
    if (valid.nonEmpty) {
      valid.contains(toSnakeCase(queryFilter.col))
    } else {
      true
    }
  }

  /** Determine whether a filter parameter in the standard case is valid,
   *  given a list of valid orders
   *
   *  @param queryOrder standard query parameter
   *  @param valid      list of valid orders
   *  @return order is valid
   */
  def isValidOrder(queryOrder: QueryOrder, valid: List[String]): Boolean = {
    queryOrder.by.exists(b => valid.contains(toSnakeCase(b)))
  }

  /** Get date interval from a date range
   *
   *  @param queryRequest standard query
   *  @return day series: 0 = today so far, 1 = yesterday, 7 = last 7 days, etc.
   */
  def dateInterval(queryRequest: QueryRequest): Int = {
    val start = queryRequest.dateAttr.start
    val end = queryRequest.dateAttr.end

    if (start.equals(end)) {
      val today: LocalDate = new LocalDate()
      if (today.equals(end)) 0 else 1
    } else {
      val s = start.getDayOfYear
      val e = end.getDayOfYear

      if (s > e) {
        e + 365 - s + 1
      } else {
        e - s + 1
      }
    }
  }

  /** Map a date range to a standard series (0, 1, 7, 30, or 90 days)
   *  @param queryRequest
   *  @return
   */
  def mapStandardSeries(queryRequest: QueryRequest): Int = {
    val series = dateInterval(queryRequest)
    val validSeries = Array(0, 1, 7, 30, 90)

    if (validSeries.contains(series)) {
      series
    } else {
      throw new IllegalArgumentException(
        s"Invalid date range of $series days. Please choose from range of ${validSeries.mkString(", ")} days"
      )
    }
  }
}
