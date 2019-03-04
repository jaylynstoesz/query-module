package querymodule.exporter

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.mvc.{Result, _}
import querymodule.exporter.ExportFormatting.standardError
import querymodule.query.QueryErrors.MalformedQueryException
import querymodule.query.{QueryConstants, QueryRequest, QueryResponse}

import scala.concurrent.{ExecutionContext, Future}

/** Controller utility for executing actions and returning results in standard format
 *
 *  @param cc            Play controller API
 *  @param exec          Play global execution context
 *  @param configuration Play global configuration
 */
class Exporter @Inject() (
    cc: ControllerComponents
)(
    implicit
    exec:          ExecutionContext,
    configuration: Configuration
) extends AbstractController(cc) {
  /** Verify query_module.query is within constraints, execute, and handle response
   *
   *  @param constraints function to validate and normalize the standard query_module.query request
   *  @param response    report response
   *  @param title       report title
   *  @param request     HTTP request
   *  @return HTTP result
   */
  def handle(
    constraints: QueryRequest => Either[Exception, QueryRequest],
    response:    => () => Future[QueryResponse],
    title:       String
  )(implicit request: Request[AnyContent]): Future[Result] = {
    constraints(withRequest).fold(
      error => Future.successful(BadRequest(error.getMessage)),
      queryRequest => {
        execute(response().map(res => Left(title -> res)))(queryRequest)
      }
    )
  }

  /** Retrieve standard query_module.query attributes that have been attached to the HTTP request
   *
   *  @param request HTTP request
   *  @return standard query_module.query request or throw an error
   */
  def withRequest(implicit request: Request[AnyContent]): QueryRequest = {
    request.attrs.get(QueryConstants.Query).getOrElse {
      throw MalformedQueryException("Could not get Query object from Request.")
    }
  }

  /** Asynchronously execute an action and return a standard response with a title
   *
   *  @param action       action to be executed
   *  @param queryRequest QueryRequest http request wrapper
   *  @return future result
   */
  def execute(action: Future[Any])(implicit queryRequest: QueryRequest): Future[Result] = {
    action
      .mapTo[Either[(String, QueryResponse), JsObject]]
      .map { res =>
        res.fold(
          standardResponse => send(queryRequest, standardResponse._2, standardResponse._1),
          customResponse => Ok(customResponse)
        )
      } recover {
        case e => sendError(e)
      }
  }

  /** Either return a JSON or CSV output depending on request parameters
   *  ?download=false or not specified => JSON
   *  ?download=true => CSV
   *  Use `Accepted 202` code instead of `OK 200` for CSV downloads for caching purposes
   *
   *  @param queryRequest  QueryRequest http request wrapper
   *  @param queryResponse result of an action
   *  @param title         report title
   *  @return either JSON or CSV
   */
  def send(queryRequest: QueryRequest, queryResponse: QueryResponse, title: String): Result = {
    if (queryRequest.download) {
      Accepted.sendFile(queryRequest.standardCSVResponse(title, queryResponse))
    } else {
      Ok(queryRequest.standardJsonResponse(title, queryResponse))
    }
  }

  /** Send a standardized error message
   *
   *  @param exception throwable exception
   *  @return standard error response
   */
  def sendError(exception: Throwable): Result = {

    exception match {
      case e: akka.pattern.AskTimeoutException => GatewayTimeout(standardError(GATEWAY_TIMEOUT, e.getMessage))
      case e: IllegalArgumentException         => BadRequest(standardError(BAD_REQUEST, e.getMessage))
      case e =>
        e.printStackTrace()
        InternalServerError(standardError(INTERNAL_SERVER_ERROR, e.getMessage))
    }
  }
}
