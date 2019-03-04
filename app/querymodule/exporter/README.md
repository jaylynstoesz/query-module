# Exporting Results

### Overview

This set of tools is used for:
* [handling ingestion and validation of query requests](#process-query-requests)
* [handling exportation of query results](#process-query-results)
* [miscellaneous common formatting functions](#common-formatting-utils)


### Usage

#### Import the models you need

In your code, import the models you need from the library:
```
import querymodule.exporter.{ ... }
```

#### Process query requests

1) Inject `Exporter` into your controller as a dependency. _Note: using the Exporter in conjunction with
the `QueryAction` component is recommended._

```
@Singleton
class SampleController @Inject() (
    cc:        ControllerComponents,
    query:     QueryAction,
    exporter:  Exporter
)(
    //...
) extends AbstractController(cc) {
    //...
}
```

2) Validate query string and retrieve from request using the `withRequest()` function:
```
  def sampleResource: EssentialAction = {

  val queryRequest: QueryRequest = exporter.withRequest

  //...

  }
```

3) Use the `handle()` function to wrap `QueryRequest` processing:
```
  def sampleResource: EssentialAction = {
      import exporter.withRequest

      query.async { implicit request =>
          def constraints(queryRequest: QueryRequest): Either[Exception, QueryRequest] = //...

          def response: Future[QueryResponse] = //...

          val title: String = "Sample Report Title"

          exporter.handle(
            constraints,
            () => response, // NOTE: exporter will not execute function until constraints are validated
            title
          )
      }
  }
```

#### Process query results

1) Pass `&download=true` in the query string to request a CSV download, otherwise the default response
is a JSON payload.

```
GET /sample-resource?company.id=1234&download=true
```

2) Create a case class to contain the final, properly formatted data from your query, and
extend the `Exportable` trait. Define the JSON and CSV outputs in the case class.

_Note: name column selections as the snake_case versions of your case class parameter names to use the
anorm `Macro.namedParser[T]` function. Otherwise, see the [Play documentation](https://www.playframework.com/documentation/2.6.x/ScalaAnorm) for parsing instructions._

```
object ReportRow {
    import anorm.{Macro, RowParser}
    import anorm.Macro.ColumnNaming

    val parser: RowParser[ReportRow] = Macro.namedParser[ReportRow](ColumnNaming.SnakeCase)
}

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsObject, Json}
import querymodule.exporter.Exportable

case class ReportRow(
    id: Long,
    name: String,
    statusActive: Boolean,
    statusCreatedOn: DateTime,
    statusUpdatedOn: DateTime
) extends Exportable {
    def json: JsObject = //...

    def csv: List[(String, Any)] = //...
}

```

3) Import helpers from `ExportFormatting` to make standard formatting a little easier for common values.

_Note: your naming conventions should be consistent between case class parameter names and JSON structure._

```
case class ReportRow(
    id: Long,
    name: String,
    statusActive: Boolean,
    statusCreatedOn: DateTime,
    statusUpdatedOn: DateTime
) extends Exportable {
    import querymodule.exporter.ExportFormatting.dateTimeFriendly

    private def createdOn: String = dateTimeFriendly(statusCreatedOn)
    private def updatedOn: String = dateTimeFriendly(statusUpdatedOn)

    def json: JsObject = {
        Json.obj(
            "id" -> id,
            "name" -> name,
            "status" -> Json.obj(
                "active" -> statusActive,
                "created_on" -> createdOn,
                "updated_on" -> updatedOn
            )
        )
    }

    def csv: List[(String, Any)] = {
        List(
            "ID" -> id,
            "Name" -> name,
            "Active" -> statusActive,
            "Created On" -> createdOn,
            "Updated On" -> updatedOn
        )
    }
}
```

#### Common formatting utils

1) Import the `ExportFormatting` or `ExportUtils` functions you need:
```
import querymodule.exporter.ExportFormatting.initials
```

2) Use 'em
```
import querymodule.exporter.ExportFormatting.initials

def userInitials(name: String): String = initials(name)
```

_Note: please feel free to add common formatting functions to this list! General conventions
should be standard across different API's._


### Important Links

Full Scaladocs (_TODO_)


