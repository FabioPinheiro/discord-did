package lace.poh

import zio.*
import zio.json.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.EmbedBuilder
import net.glxn.qrgen.javase.QRCode
import net.glxn.qrgen.core.image.ImageType
import fmgp.did.comm.protocol.auth.*
import fmgp.did.comm.*
import fmgp.did.method.peer.*
import fmgp.did.*
import lace.poh.VCModels.*

object BotCommandsPoH {

  // Stub verification function (TODO: Replace with real SD-JWT verification using eudi-lib-jvm-sdjwt-kt)
  def stubVerifyCredential(credential: String): VerificationResult = {
    // For now, accept any credential that looks like JSON and contains "ProofOfHumanity" or "test"
    if (credential.contains("ProofOfHumanity") || credential.contains("test")) {
      VerificationSuccess(
        holderDid = "did:peer:stub-holder-" + java.lang.System.currentTimeMillis(),
        issuerDid = "did:peer:stub-issuer"
      )
    } else {
      VerificationFailure("Invalid credential format (stub validation)")
    }
  }

  def handleVerifyCommand(event: SlashCommandInteractionEvent, VerifiedDB: Seq[VerificationRecord]): Unit = {
    val userId = event.getUser().getId()

    VerifiedDB.find(_.discordUserId == userId) match {
      case Some(record) =>
        val verifiedDate = new java.util.Date(record.verifiedAt * 1000)
        event
          .reply(
            s"✅ You are already verified!\n" +
              s"Holder DID: ${record.holderDid}\n" +
              s"Verified at: $verifiedDate"
          )
          .setEphemeral(true)
          .queue()

      case None =>
        val presentationRequest = s"""{
          "type": "VerificationRequest",
          "credentialType": "ProofOfHumanity",
          "instructions": "Use /submit-vc command with your credential"
        }"""

        val stream = QRCode.from(presentationRequest).to(ImageType.PNG).stream()
        val file: FileUpload = FileUpload.fromData(stream.toByteArray(), "verify-qr.png")
        val embed = new EmbedBuilder()
        embed.setImage("attachment://verify-qr.png")
        embed.addField(
          "Verify Proof of Humanity",
          "Scan QR code with wallet or use `/submit-vc` command",
          false
        )
        event
          .replyEmbeds(embed.build())
          .setEphemeral(true)
          .addFiles(file)
          .queue()
    }
  }

  def handleSubmitVcCommand(
      event: SlashCommandInteractionEvent,
      VerifiedDB: Seq[VerificationRecord]
  ): (Seq[VerificationRecord], Unit) = {
    val userId = event.getUser().getId()
    val credential = event.getOption("credential", _.getAsString)
    var updatedVerifiedDB = VerifiedDB

    VerifiedDB.find(_.discordUserId == userId) match {
      case Some(_) =>
        event.reply("✅ Already verified!").setEphemeral(true).queue()

      case None =>
        val verificationResult = stubVerifyCredential(credential)

        verificationResult match {
          case VerificationSuccess(holderDid, issuerDid) =>
            val record = VerificationRecord(
              discordUserId = userId,
              holderDid = holderDid,
              verifiedAt = java.lang.System.currentTimeMillis() / 1000
            )
            updatedVerifiedDB = VerifiedDB :+ record

            event
              .reply(
                s"✅ Verification successful!\n" +
                  s"Holder DID: $holderDid\n" +
                  s"Issuer DID: $issuerDid\n\n" +
                  s"Note: Role assignment will be added in Phase 4"
              )
              .setEphemeral(true)
              .queue()

          case VerificationFailure(reason) =>
            event.reply(s"❌ Verification failed: $reason").setEphemeral(true).queue()
        }
    }

    (updatedVerifiedDB, ())
  }

  def handleVerifyStatusCommand(event: SlashCommandInteractionEvent, VerifiedDB: Seq[VerificationRecord]): Unit = {
    val userId = event.getUser().getId()

    VerifiedDB.find(_.discordUserId == userId) match {
      case Some(record) =>
        val verifiedDate = new java.util.Date(record.verifiedAt * 1000)
        val embed = new EmbedBuilder()
        embed.addField("Status", "✅ Verified", false)
        embed.addField("Holder DID", record.holderDid, false)
        embed.addField("Credential Type", record.credentialType, false)
        embed.addField("Verified At", verifiedDate.toString, false)
        event.replyEmbeds(embed.build()).setEphemeral(true).queue()

      case None =>
        event
          .reply(
            """❌ Not verified yet.
              | 
              |Use `/verify` to start the verification process.""".stripMargin
          )
          .setEphemeral(true)
          .queue()
    }
  }
}
