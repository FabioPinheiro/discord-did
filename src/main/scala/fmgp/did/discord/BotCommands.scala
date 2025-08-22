package fmgp.did.discord

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
import fmgp.did.discord.VCModels.*

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
}
