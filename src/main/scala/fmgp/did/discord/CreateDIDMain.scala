package fmgp.did.discord

import zio.*
import fmgp.did.discord.ConfigDiscord
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.crypto.*
import _root_.proto.prism
import _root_.proto.prism.PrismEvent
import _root_.proto.prism.SignedPrismEvent
import _root_.proto.prism.ProtoCreateDID
import com.google.protobuf.ByteString

//@main
def CreateDIDMain = {
  // 1.
  val txHash = runProgram(
    programCreateDID(
      ConfigDiscord.blockfrostConfig,
      ConfigDiscord.walletConfig
    )
  )
  println(s"✓ Create DID txHash: ${txHash.hex}") // 0e458f7a091f1cfed05ede0a29c11ef17305162ea1782d7cb165e0380a72ee0c
  // https://github.com/FabioPinheiro/prism-vdr/commit/7689f0d098183720d8ede331dbbd5bad47e82206
  runWithPrismState(for {
    prismState <- ZIO.service[fmgp.did.method.prism.PrismState]
    _ = prismState.getSSI(ConfigDiscord.didPrism)
  } yield ())
}

def programCreateDID(
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
) = {
  val pkMaster = ConfigDiscord.walletConfig.secp256k1PrivateKey(0, 0)

  val (tmpDIDPrism, signedPrismEvent) = createDIDForDIDComm(
    masterKeys = Seq(("master1", pkMaster)),
    authentication = Seq(("encry1", KeyGenerator.unsafeEd25519)), // Encrypting Messages
    keyAgreement = Seq() // Signing Messages
  )
  assert(tmpDIDPrism.string == ConfigDiscord.didPrism.string)
  for {
    txHash <- PrismChainServiceImpl(bfConfig, wallet).commitAndPush(
      Seq(signedPrismEvent),
      Some("DID from PrismForDiscord")
    )
    _ <- ZIO.log(s"DID '${tmpDIDPrism.string}' created in txHash '${txHash.hex}'")
  } yield (txHash)
}

def runWithPrismState[E, A](program: ZIO[PrismState, E, A]) = {
  import fmgp.did.method.prism.mongo.AsyncDriverResource
  val layer =
    AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(Secrets.mongoDBConnection)
  runProgram(program.provideLayer(layer))
}

/** Execute a ZIO program synchronously
  *
  * Helper method to run ZIO effects in a synchronous context.
  *
  * @param program
  *   The ZIO program to execute
  * @return
  *   The result of the program execution
  */
def runProgram[E, A](program: ZIO[Any, E, A]): A =
  Unsafe.unsafe { implicit unsafe =>
    Runtime.default.unsafe.run(program).getOrThrowFiberFailure()
  }

def createDIDForDIDComm(
    masterKeys: Seq[(String, Secp256k1PrivateKey)],
    authentication: Seq[(String, OKPPrivateKey)], // OPK with Curve Ed25519 (Encrypting Messages)
    keyAgreement: Seq[(String, OKPPrivateKey)], // OPK with Curve X25519 (Signing Messages)
): (DIDPrism, SignedPrismEvent) = {
  def op = PrismEvent(
    event = PrismEvent.Event.CreateDid(
      value = ProtoCreateDID(
        didData = Some(
          ProtoCreateDID.DIDCreationData(
            publicKeys = Seq.empty ++
              masterKeys.map { (keyName, pk) =>
                prism.PublicKey(
                  id = keyName,
                  usage = prism.KeyUsage.MASTER_KEY,
                  keyData = pk.compressedEcKeyData
                )
              } ++
              authentication.map { (keyName, pk) =>
                val pubKey = pk.toPublicKey
                prism.PublicKey(
                  id = keyName,
                  usage = prism.KeyUsage.AUTHENTICATION_KEY,
                  keyData = _root_.proto.prism.PublicKey.KeyData.CompressedEcKeyData(
                    value = _root_.proto.prism.CompressedECKeyData(
                      curve = pubKey.crv.name,
                      data = com.google.protobuf.ByteString.copyFrom(
                        fmgp.util.Base64.fromBase64(pubKey.x).decode
                      )
                    )
                  )
                )
              } ++
              keyAgreement.map { (keyName, pk) =>
                val pubKey = pk.toPublicKey
                prism.PublicKey(
                  id = keyName,
                  usage = prism.KeyUsage.KEY_AGREEMENT_KEY,
                  keyData = _root_.proto.prism.PublicKey.KeyData.CompressedEcKeyData(
                    value = _root_.proto.prism.CompressedECKeyData(
                      curve = pubKey.crv.name,
                      data = com.google.protobuf.ByteString.copyFrom(
                        fmgp.util.Base64.fromBase64(pubKey.x).decode
                      )
                    )
                  )
                )
              },
            services = Seq.empty[_root_.proto.prism.Service],
            context = Seq.empty[String]
          )
        )
      )
    )
  )
  def signedPrismCreateEventDID = SignedPrismEvent(
    signedWith = masterKeys.head._1,
    signature = ByteString.copyFrom(masterKeys.head._2.sign(op.toByteArray)),
    event = Some(op)
  )
  import fmgp.did.method.prism.proto.didPrism
  def didPrism: DIDPrism = op.didPrism.getOrElse(???)
  (didPrism, signedPrismCreateEventDID)
}
