# Query Parsing

### Overview

This set of tools is used for:
* [parsing query parameters from a request into a standard format](#parse-query-strings)
* [query validation and business logic utilities](#custom-utilities)
* [easily composing SQL queries with clause helpers](#compose-sql-queries)

### Standard formatting

Available standard query parameters are the following:

| Parameter           | Required | Behavior                               | Example                       | Default | Notes |
| :------------------:|:--------:|:---------------------------------------|:-----------------------------:| :-----: | :-----|
| `download`          |   No     | Returns JSON if `false`, CSV if `true` | `&download=true`              | `false` |
| `page.limit`        |   No     | Max records per page                   | `&page.limit=25`              |  500    | Thrown out if `download` = `true`
| `page.current`      |   No     | Current page                           | `&page.current=1`             |  1      | Thrown out if `download` = `true`
| `date.start`        |   Yes    | Start date for report                  | `&date.start=2018-11-13`      |         | Must be equal to or before `date.end`
| `date.end`          |   Yes    | End date for report                    | `&date.end=2018-11-14`        |         | Must be equal to or after `date.start` _and_ today's date (UTC)
| `dimension`         |   *      | Groups records by specified dimension  | `&dimension=line_item`        |         | Varies by application
| `order.by`          |   No     | Orders records by specified column     | `&order.by=status.created_on` |  `null` | Valid values should match output payload structure
| `order.dir`         |   No     | Orders records in specified direction  | `&order.dir=desc`             |  `asc`  |

Query filters can be passed as normal query string key/value pairs.
Valid values should match output payload structure as dot notation. For example:

Given this response JSON:
```
{
    "network" : {
        "id" : 123,
        "name": "ABC Co.",
        "status": {
            "active": true,
            "created_on": "2018-11-12"
        }
    }
}
```

The following are valid columns:
* `network.id`
* `network.name`
* `network.status.active`
* `network.status.created_on`

Numeric values can have case-insensitive operators applied to them in the format: `/...?metric=value(operator)`. Available operators:
* `GT`  => `>`
* `GTE` => `>=`
* `LT`  => `<`
* `LTE` => `<=`
* `NE`  => `!=`

For example: `/...?revenue=100(gte)` maps to `where revenue is greater than or equal to 100` or:

```
SELECT * FROM table WHERE SUM(revenue) >= 100;
```

String matching can be requested by placing `*` at the beginning of the filter value.
_Note: in the resulting query, the string will be split on space characters and each word will be
searched for separately._

For example: `/...?name=*Candy Crush*` maps to `where name is like "Candy" and name is like "Crush"` or:
```
SELECT * FROM table WHERE (name LIKE '%Candy%' AND name LIKE '%Crush%');
```

### Usage

#### Import the models you need

In your code, import the models you need from the library:
```
import querymodule.query.{ ... }
```

#### Parse query strings

1) Inject `QueryAction` into your controller as a dependency:

```
@Singleton
class SampleController @Inject() (
    cc:          ControllerComponents,
    query:    QueryAction
)(
    //...
) extends AbstractController(cc) {
    //...
}
```

2) Parse the query through an action instance and append the result to the request:
```
  def sampleResource: EssentialAction = {

      query.async { implicit request =>
            // ...
      }
  }
```

3) Access the parsed query parameters through the request:
```
   import querymodule.query.QueryRequest
   import querymodule.query.QueryConstants._

   val queryRequest: QueryRequest = {
       request
            .attrs
            .get(QueryConstants.Query)
            .getOrElse {
                throw MalformedQueryException("Could not get Query object from Request.")
            }
   }
```

#### Compose SQL queries

1) Instantiate a class that extends `QueryBuilderUtils`. The compiler will tell you
what values are required:

```
object SampleReport extends QueryBuilderUtils {

    def columns: List[(String, String)] = {
        List(
            "network.id" -> "network_id",
            "network.name" -> "network_name"
        )
    }

    //...
}

```

2) Build your query, with or without using the helpers defined in the `QueryBuilderUtils` trait:
```
object SampleReport extends QueryBuilderUtils {
    //...

    def query(implicit queryRequest: QueryRequest): String = {
            s"""
               |SELECT
               |  $selectSql
               |FROM line_item
               |  $joinSql
               |WHERE line_item.deleted = 0
               |ORDER BY NULL
             """.stripMargin
    }
}
```

3) Execute the query in a dependency-injected instance of the report using a [JdbcConnection](/app/dbconnection) instance:
```
case class ReportRow(id: Long, name: String, revenue: Double)

class SampleReport @Inject() {
        implicit cxn:   JdbcConnectionInstance,
        ec:             ExecutionContext
} {
  import SampleReport._

  val parser: RowParser[ReportRow] = ...

  def run(implicit queryRequest: QueryRequest): List[ReportRow] = {
        cxn.list(query, parser)
  }
}
```

#### Custom utilities

Use the QueryRequest attribute on the Request instance however you see fit in order to build your query
and implement business logic.

_Note: how you compose your queries is entirely up to you. Optimize and override the base classes as needed._

__Example A (simple): Check for presence of a name filter__
```
  /** Check to see if query is filtered by name
   *
   *  @param queryRequest standard query request
   */
  def filterByName(implicit queryRequest: QueryRequest): Boolean = {
    queryRequest.filtersAttr.exists(_.col == "name")
  }
```


__Example B (medium): Check that incoming query is valid__
```
/** Validate that a query is requesting a valid dimension
  *
  * @param queryRequest standard query request
  * @return Exception if validation fails, QueryRequest if validation passes
  */
  def validation(queryRequest: QueryRequest): Either[InvalidQueryException, QueryRequest] = {
    val dimension: String = queryRequest.dimension.getOrElse("invalid")

    val validDimensions: List[String] = List("network", "company", "line_item")

    if (!validDimensions.contains(dimension)) {
      Left(
        InvalidQueryException(
          message = s"Dimension ${dimension.getOrElse("[unknown]")} is unavailable for this report."
        )
      )
    } else {
      Right(queryRequest)
    }
  }
```

__Example C (complex): Modify the default WHERE clause builder to handle certain filters differently__
```
  /** Override filter mapping for building parameters of WHERE clause for row data query
   *  Accounts for special use cases:
   *   - optimizing filtering on user.id (AM filtering)
   *   - filtering on the presence of a KPI passed by an API integration
   *
   *  @param queryRequest standard query request
   *  @return parameters SQL WHERE clause
   */
  override def whereClause(queryRequest: QueryRequest, validFilters: List[String]): String = {
    val filters = queryRequest.filtersAttr.filter(f => isValidFilter(f, validFilters))

    if (filters.nonEmpty) {
      "AND " + filters.collect {
        case f if f.col == "user.id" =>
          s"""
             |network.id IN (
             |  SELECT id FROM network WHERE ${f.clause}
             |)
           """.stripMargin
        case f if f.col == "kpi" =>
          val not = if (f.containsValue("true")) "NOT" else ""
          s"line_item.kpi IS $not NULL"
        case f =>
          f.copy(
            (mapping.getOrElse(f.col, f.col), f.querystring._2)
          ).clauseUnmapped
      }.mkString("\nAND ")
    } else {
      ""
    }
  }
```

#### Important Links

Full Scaladocs (_TODO_)


