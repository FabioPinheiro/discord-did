package lace.poh

// Verification domain models
object VCModels {
  case class VerificationRecord(
      discordUserId: String,
      holderDid: String,
      verifiedAt: Long, // Unix timestamp for simplicity
      credentialType: String = "ProofOfHumanity"
  )

  // Stub verification result types (will be replaced with real SD-JWT verification later)
  sealed trait VerificationResult
  case class VerificationSuccess(holderDid: String, issuerDid: String) extends VerificationResult
  case class VerificationFailure(reason: String) extends VerificationResult
}
