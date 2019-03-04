package query

import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.scalatestplus.play._
import querymodule.query.{QueryDate, QueryRequest}

@RunWith(classOf[JUnitRunner])
class QueryDateSpec extends PlaySpec {
  val start = "2018-11-04"
  val end = "2018-12-03"

  val queryString = Map(
    "date.start" -> Seq(start),
    "date.end" -> Seq(end)
  )

  val queryRequest = QueryRequest(queryString)

  "Query parser" must {
    "correctly parse QueryDate from query string" in {
      queryRequest.dateAttr mustEqual QueryDate(queryString)
    }
  }

  "Date parser" must {
    "correctly parse start date" in {
      queryRequest.dateAttr.start mustEqual LocalDate.parse(start)
    }

    "correctly parse end date" in {
      queryRequest.dateAttr.end mustEqual LocalDate.parse(end)
    }
  }
}
