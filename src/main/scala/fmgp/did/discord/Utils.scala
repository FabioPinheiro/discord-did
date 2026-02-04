package fmgp.did.discord

import zio.*
import zio.http.*
import fmgp.did.*
import fmgp.did.method.prism.*
import fmgp.did.method.peer.*

object Utils {
  def resolverLayer =
    (((Client.default ++ Scope.default >>> HttpUtils.layer) >>> DIDPrismResolver.layerDIDPrismResolver(
      s"https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"
    )) >>> ZLayer.fromZIO(makeResolver))

  def makeResolver: ZIO[DIDPrismResolver, Nothing, Resolver] = {
    for {
      // uniresolver <- Uniresolver.make()
      didPrismResolver <- ZIO.service[DIDPrismResolver]
      _ <- ZIO.log("Make MultiFallbackResolver")
      multiResolver = MultiFallbackResolver(
        // HardcodeResolver.default,
        DidPeerResolver.default,
        didPrismResolver
        // uniresolver,
      )
    } yield multiResolver
  }
}
