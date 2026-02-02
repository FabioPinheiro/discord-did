package fmgp.did.discord

import fmgp.did.DIDSubject
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoWalletConfig

import fmgp.crypto.*
import fmgp.crypto.OKPPrivateKey.*
import fmgp.crypto.OKPPrivateKeyWithKid.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.method.peer.*
import fmgp.did.method.prism.*

import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

object ConfigDiscord {

  def blockfrostConfig: BlockfrostConfig = ???
  def walletConfig: CardanoWalletConfig = ???

  def didPrism: DIDSubject = ???
}

import CurveConfig.given

case class ApplicationConfig(
    did: DIDSubject,
    keyStore: KeyStore,
    endpoint: String,
    port: Int,
    discordToken: String,
) {
  def applicationAgent = ApplicationAgent(id = did, keyStore = keyStore)
  // val agentLayer: ZLayer[Any, Nothing, Agent] =
  //   ZLayer(ApplicationAgent.make(id = did, keyStore = keyStore))
}

object ApplicationConfig {

  val configAgent = {
    Config
      .string("did")
      .mapOrFail(str =>
        DIDSubject.either(str) match
          case Left(value)  => Left(Config.Error.InvalidData(Chunk("did"), "Fail to parse the DID: " + value.error))
          case Right(value) => Right(value)
      ) ++
      Config
        .Sequence(
          Config.Fallback[PrivateKeyWithKid](Config.derived[OKPPrivateKeyWithKid], Config.derived[ECPrivateKeyWithKid])
        )
        .map(keys => KeyStore(keys.toSet))
        .nested("keyStore") ++
      Config
        .string("endpoint")
        .nested("service") ++
      Config
        .int("port")
        .nested("service") ++
      Config
        .string("token")
        .nested("discord")
  }.map((did, keyStore, endpoint, port, discordToken) =>
    ApplicationConfig(
      did = did,
      keyStore = keyStore,
      endpoint = endpoint,
      port = port,
      discordToken = discordToken
    )
  )
}
