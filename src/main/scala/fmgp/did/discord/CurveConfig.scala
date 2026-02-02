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
import CurveConfig.given

object CurveConfig {

  // Config for OKPCurve union type
  given okpCurveConfig: Config[OKPCurve] =
    Config.string.mapOrFail:
      case "X25519"  => Right(Curve.X25519)
      case "Ed25519" => Right(Curve.Ed25519)
      case other => Left(Config.Error.InvalidData(message = s"Invalid OKP curve: $other. Expected X25519 or Ed25519"))

  // Config for ECCurve union type
  given ecCurveConfig: Config[ECCurve] =
    Config.string.mapOrFail:
      case "P-256"     => Right(Curve.`P-256`)
      case "P-384"     => Right(Curve.`P-384`)
      case "P-521"     => Right(Curve.`P-521`)
      case "secp256k1" => Right(Curve.secp256k1)
      case other =>
        Left(
          Config.Error.InvalidData(message = s"Invalid EC curve: $other. Expected P-256, P-384, P-521, or secp256k1")
        )
}
