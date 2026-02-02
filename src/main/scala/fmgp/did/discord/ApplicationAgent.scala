package fmgp.did.discord

import fmgp.did.*
import fmgp.crypto.*
import zio.*

case class ApplicationAgent(
    override val id: DID,
    override val keyStore: KeyStore,
) extends Agent { def keys: Seq[PrivateKey] = keyStore.keys.toSeq }

object ApplicationAgent {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, ApplicationAgent] =
    ZIO.succeed(ApplicationAgent(id, keyStore))
}
