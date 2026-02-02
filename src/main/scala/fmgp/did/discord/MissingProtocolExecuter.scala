package fmgp.did.discord

import zio.ZIO

import fmgp.did.*
import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.protocol.Reply
import fmgp.did.comm.protocol.ProtocolExecuter
import fmgp.did.comm.protocol.reportproblem2.*

object MissingProtocolExecuter extends ProtocolExecuter[Agent, Nothing] {

  override def supportedPIURI = Seq()
  override def program(plaintextMessage: PlaintextMessage) =
    ZIO
      .service[Agent]
      .map(agent =>
        Reply(
          ProblemReport(
            to = plaintextMessage.from.map(_.asTO).toSet,
            from = agent.id,
            pthid = plaintextMessage.id,
            ack = None,
            code = ProblemCode.ErroFail("msg", "unsupported"),
            comment = None,
            args = None,
            escalate_to = Some("email"),
          ).toPlaintextMessage
        )
      )
}
