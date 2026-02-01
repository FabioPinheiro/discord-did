package fmgp.did.discord

import zio.*
import zio.http.*

object TraceIdMiddleware {

  private val NAME_TRACE_ID = "requestid"

  private def requestId(req: Request) =
    for {
      value <- req.headers
        .find(h => h.headerName.equalsIgnoreCase("x-request-id")) match
        case Some(header) => ZIO.succeed(header.renderedValue)
        case None         => Random.nextUUID.map(_.toString)
    } yield (LogAnnotation(NAME_TRACE_ID, value))

  def addTraceId[R] = {
    HandlerAspect.interceptHandler[R, Unit](
      Handler.fromFunctionZIO[Request] { request =>
        ZIO.scoped {
          requestId(request).map(_ => (request, ()))
        }
      }
    )(Handler.identity)
  }

}
