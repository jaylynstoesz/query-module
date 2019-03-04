package querymodule.query

import javax.inject.Inject
import play.api.mvc._
import QueryErrors.MalformedQueryException

import scala.concurrent.{ExecutionContext, Future}

/** Action builder for standard incoming query_module.query requests
 *  Parses querystring and appends a QueryRequest instance to the request as an attribute
 */
class QueryAction @Inject() (
    parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    block {
      try {
        QueryRequest(request.queryString).withParameters(request)
      } catch {
        case e: Exception =>
          throw MalformedQueryException(cause = e.getCause)
      }
    }
  }
}
