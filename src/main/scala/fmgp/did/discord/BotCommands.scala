package fmgp.did.discord

import zio.*
import zio.json.*
import zio.http.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.EmbedBuilder
import net.glxn.qrgen.javase.QRCode
import net.glxn.qrgen.core.image.ImageType
import fmgp.did.comm.protocol.auth.*
import fmgp.did.comm.protocol.trustping2.*
import fmgp.did.comm.*
import fmgp.did.method.peer.*
import fmgp.did.*

object BotCommands {

  def handleSayCommand(event: SlashCommandInteractionEvent): Unit = {
    val content = event.getOption("content", _.getAsString)
    event.reply(content).queue()
  }

  private def sendLoginEmbed(
      event: SlashCommandInteractionEvent,
      secret: Int,
      qrData: String,
      title: String = "🔐 Login with Your DID Wallet"
  ): Unit = {
    val oobUrl = s"https://discord.fmgp.app/login/$secret"
    val stream = QRCode.from(qrData).to(ImageType.PNG).stream()
    val file: FileUpload = FileUpload.fromData(stream.toByteArray(), "qrcode.png")
    val embed = new EmbedBuilder()
    embed.setTitle(title)
    embed.setDescription("To login using DIDComm protocol with your DID wallet:")
    embed.addField("**Option 1:** Click the link to open your wallet", oobUrl, false)
    embed.addField("**Option 2:** Scan the QR code with your wallet app", "See QR code below ⬇️", false)
    embed.setImage("attachment://qrcode.png")
    event
      .replyEmbeds(embed.build())
      .setEphemeral(true)
      .addFiles(file)
      .queue()
  }

  def handleLoginCommand(event: SlashCommandInteractionEvent, agent: Agent, DB: Seq[User]): (Seq[User], Unit) = {
    val userId = event.getUser().getId()
    var updatedDB = DB

    DB.find(_.id == userId) match {
      case Some(User(id, secret, true)) =>
        event.reply("✅ You are already logged in (with user $id)!").setEphemeral(true).queue()

      case Some(User(id, secret, false)) =>
        sendLoginEmbed(event, secret, s"Secret $secret", "🔐 Complete Your Login")

      case None =>
        val program = Operations
          .sign(AuthRequest(from = agent.id.asFROM).toPlaintextMessage)
          .provideSomeLayer(Operations.layerOperations ++ DidPeerResolver.layer)
          .provideEnvironment(ZEnvironment(agent))

        Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.run(program)
        } match {
          case Exit.Failure(cause) =>
            println(s"ERROR: ${cause.prettyPrint}")
            event.reply(s"ERROR: ${cause.prettyPrint}").queue()

          case Exit.Success(sMsg) =>
            val secret = scala.util.Random.apply().nextInt()
            updatedDB = DB :+ User(userId, secret, false)
            sendLoginEmbed(event, secret, sMsg.toJson)
        }
    }

    (updatedDB, ())
  }

  def handleInfoCommand(event: SlashCommandInteractionEvent, agent: Agent): Unit = {
    val embed = new EmbedBuilder()
    embed.setTitle("🤖 DID AUTH Bot")
    embed.setDescription("This bot associates the Discord User with a DID")
    embed.addField("DID of the Discord Agent", agent.id.string, false)
    // embed.addField("Relay Endpoint (HTTPS)", "https://relay.fmgp.app", false)
    // embed.addField("Relay Endpoint (WebSocket)", "wss://relay.fmgp.app/ws", false)
    event.replyEmbeds(embed.build()).setEphemeral(true).queue()
  }

  def handleDbCommand(event: SlashCommandInteractionEvent, DB: Seq[User]): Unit = {
    event.reply(DB.map(_.toString()).mkString(">>>>>>>>>>>>\n", "\n", "\n<<<<<<<<<<<<\n")).queue()
  }

  def handleLeaveCommand(event: SlashCommandInteractionEvent): Unit = {
    println("LEAVE:")
    event
      .reply("I'm leaving the server now!")
      .setEphemeral(true)
      .flatMap(m => event.getGuild().leave())
      .queue()
  }

  def handleTrustPingCommand(event: SlashCommandInteractionEvent, agent: Agent): Unit = {
    val didString = event.getOption("did", _.getAsString)

    // Parse the DID string
    DIDSubject.either(didString) match {
      case Left(error) =>
        event.reply(s"❌ Invalid DID: ${error.error}").setEphemeral(true).queue()

      case Right(targetDid) =>
        // Create a Trust Ping message with requested response
        val trustPing = TrustPingWithRequestedResponse(
          id = MsgID(),
          from = agent.id.asFROM,
          to = targetDid.asTO
        )

        val program = for {
          // Convert to PlaintextMessage and sign
          signedMsg <- Operations.sign(trustPing.toPlaintextMessage)
          // TODO: Send the message via the agent's mediator/relay
          _ <- ZIO.log(s"Trust Ping sent to ${targetDid.string}")
        } yield signedMsg

        Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.run(
            program
              .provideSomeLayer(Operations.layerOperations ++ DidPeerResolver.layer)
              .provideEnvironment(ZEnvironment(agent))
          )
        } match {
          case Exit.Failure(cause) =>
            println(s"ERROR: ${cause.prettyPrint}")
            event.reply(s"❌ Failed to send Trust Ping: ${cause.prettyPrint}").setEphemeral(true).queue()

          case Exit.Success(signedMsg) =>
            val embed = new EmbedBuilder()
            embed.setTitle("🏓 Trust Ping Sent")
            embed.setDescription(s"Trust Ping message sent to DID")
            embed.addField("Target DID", targetDid.string, false)
            embed.addField("Message ID", trustPing.id.value, false)
            embed.addField("Status", "Waiting for response...", false)
            event.replyEmbeds(embed.build()).setEphemeral(true).queue()
        }
    }
  }

  def handleResolveDIDCommand(
      event: SlashCommandInteractionEvent,
      makeResolver: ZIO[Client & Scope, Nothing, Resolver]
  ): Unit = {
    val didString = event.getOption("did", _.getAsString)

    // Parse the DID string
    DIDSubject.either(didString) match {
      case Left(error) =>
        event.reply(s"❌ Invalid DID: ${error.error}").setEphemeral(true).queue()

      case Right(did) =>
        val program = for {
          resolver <- makeResolver
          didDocument <- resolver.didDocument(did.asTO)
        } yield didDocument

        Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.run(
            program.provide(Client.default, Scope.default)
          )
        } match {
          case Exit.Failure(cause) =>
            println(s"ERROR: ${cause.prettyPrint}")
            event.reply(s"❌ Failed to resolve DID: ${cause.prettyPrint}").setEphemeral(true).queue()

          case Exit.Success(didDocument) =>
            val embed = new EmbedBuilder()
            embed.setTitle("🔍 DID Document")
            embed.setDescription(s"Resolved DID Document")
            embed.addField("DID", did.string, false)

            // Add verification methods
            val verificationMethods = didDocument.verificationMethod.map(_.size).getOrElse(0)
            embed.addField("Verification Methods", s"$verificationMethods method(s)", true)

            // Add services
            val services = didDocument.service.map(_.size).getOrElse(0)
            embed.addField("Services", s"$services service(s)", true)

            // Add authentication methods
            val authMethods = didDocument.authentication
              .map {
                case e: VerificationMethod => 1
                case e                     => e.asInstanceOf[Seq[VerificationMethod]].size
              }
              .getOrElse(0)
            embed.addField("Authentication", s"$authMethods method(s)", true)

            // Create JSON file attachment with the full DID Document
            val didDocJson = didDocument.toJsonPretty
            val jsonFile: FileUpload = FileUpload.fromData(
              didDocJson.getBytes(java.nio.charset.StandardCharsets.UTF_8),
              s"did-document-${didDocument.didSubject.string}.json"
            )
            embed.addField("DID Document", "See attached JSON file ⬇️", false)

            event
              .replyEmbeds(embed.build())
              .setEphemeral(true)
              .addFiles(jsonFile)
              .queue()
        }
    }
  }
}
