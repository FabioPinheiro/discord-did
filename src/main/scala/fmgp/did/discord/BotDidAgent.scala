package fmgp.did.discord

import fmgp.crypto.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.oobinvitation.OOBInvitation
import fmgp.did.comm.protocol.reportproblem2.ProblemReport
import io.netty.handler.codec.http.HttpHeaderNames
// import org.hyperledger.identus.mediator.*
// import org.hyperledger.identus.mediator.db.*
// import org.hyperledger.identus.mediator.protocols.*
// import reactivemongo.api.bson.{*, given}
import zio.*
import zio.http.*
import zio.http.*
import zio.http.Header.AccessControlAllowMethods
import zio.http.Header.AccessControlAllowOrigin
import zio.http.Header.HeaderType
import zio.json.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Try

case class BotDidAgent(
    override val id: DID,
    override val keyStore: KeyStore, // Should we make it lazy with ZIO
) extends Agent { def keys: Seq[PrivateKey] = keyStore.keys.toSeq }

object BotDidAgent {

  def didCommApp = didCommAppAux @@ TraceIdMiddleware.addTraceId

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, BotDidAgent] = ZIO.succeed(BotDidAgent(id, keyStore))

  def didCommAppAux = {
    Routes(
      Method.GET / "headers" -> handler { (req: Request) =>
        val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
        for {
          _ <- ZIO.log("GET /health")
          text = "HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress
          ret <- ZIO.succeed(Response.text(text)).debug
        } yield (ret)
      },
      Method.GET / "health" -> handler { (req: Request) =>
        for {
          _ <- ZIO.log("GET /health")
          ret <- ZIO.succeed(Response.ok)
        } yield (ret)
      },
      // Method.GET / "version" -> handler { (req: Request) => ZIO.succeed(Response.text(MediatorBuildInfo.version)) },
      Method.GET / "did" -> handler { (req: Request) =>
        for {
          agent <- ZIO.service[BotDidAgent]
          _ <- ZIO.log("GET /did")
          ret <- ZIO.succeed(Response.text(agent.id.string))
        } yield (ret)
      },
    )
  }.sandbox

}
