package fmgp.did.discord

import zio.*
import zio.json.*
import zio.http.*
import scala.jdk.CollectionConverters.*

import fmgp.crypto.*
import fmgp.did.method.peer.*
import fmgp.crypto.OKPPrivateKeyWithKid
import fmgp.did.discord.BotCommands.*

import net.dv8tion.jda.api.JDABuilder
import java.util.EnumSet
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.components.container.*

import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.filedisplay.FileDisplay
import net.dv8tion.jda.api.utils.FileUpload
import java.nio.charset.StandardCharsets
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.glxn.qrgen.javase.QRCode
import net.dv8tion.jda.api.EmbedBuilder
import scala.compiletime.ops.boolean
import net.glxn.qrgen.core.image.ImageType
import fmgp.did.Agent

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

// @main def hello(): Unit = {
//   println("Hello world!")
//   val qrCode = QrCode.encodeText("https://www.scala-lang.org/", Ecc.LOW)

//   qrCode.foreach { line =>
//     println(line.map(x => if (x) "\u2588" else " ").mkString)
//   }
// }

//Discord example https://github.com/bbarker/diz/blob/main/src/main/scala/Main.scala
object DiscordDID extends ZIOAppDefault {
  val token = "MTE3MDQ0MjMyOTg4MzE2ODc4OA.GSVRDi.IMZNQo1TgcF7n5jG3YV7TH8OqaWZbisN-0r4YU" // DID
  // val token = "ODEwNjAwNTk5OTg0NjY4NzEz.Gn7Qu_.qvz0_FqLRhiQIEW0npnaHdMvonmyzI3Ayqgqu8" // GH3

  val bot =
    for {
      _ <- Console.printLine( // https://patorjk.com/software/taag/#p=display&f=ANSI+Shadow&t=AUTH-BOT
        """ █████╗ ██╗   ██╗████████╗██╗  ██╗      ██████╗  ██████╗ ████████╗
          |██╔══██╗██║   ██║╚══██╔══╝██║  ██║      ██╔══██╗██╔═══██╗╚══██╔══╝
          |███████║██║   ██║   ██║   ███████║█████╗██████╔╝██║   ██║   ██║   
          |██╔══██║██║   ██║   ██║   ██╔══██║╚════╝██╔══██╗██║   ██║   ██║   
          |██║  ██║╚██████╔╝   ██║   ██║  ██║      ██████╔╝╚██████╔╝   ██║   
          |╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝      ╚═════╝  ╚═════╝    ╚═╝   
          |
          |Visit: https://github.com/FabioPinheiro/discord-did""".stripMargin
      )
      configs = ConfigProvider.fromResourcePath()

      applicationConfig <- ZIO.config(ApplicationConfig.config.nested("identity")).provideLayer(ZLayer.succeed(configs))
      _ <- ZIO.debug(s"Application started")
      _ <- ZIO.log(s"Application Config: ${applicationConfig}")
      agent = applicationConfig.applicationAgent

      jda <- ZIO.succeed {
        JDABuilder
          .createLight(token, Seq().asJava) // EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
          .addEventListeners(new SlashCommandListener(agent))
          .addEventListeners(new MessageReceiveListener())
          .build()
      }
      commandListUpdateAction <- ZIO.succeed { jda.updateCommands() }
      commands <- ZIO.succeed {
        // Add all your commands on this action instance
        commandListUpdateAction.addCommands(
          Commands
            .slash("say", "Makes the bot say what you tell it to")
            .addOption(OptionType.STRING, "content", "What the bot should say", true), // Accepting a user input
          Commands.slash("login", "Login with DID"),
          // Commands.slash("verify", "Verify your Proof of Humanity credential"),
          // Commands
          //   .slash("submit-vc", "Submit VC for verification (temporary)")
          //   .addOption(OptionType.STRING, "credential", "JWT VC string", true),
          // Commands.slash("verify-status", "Check your verification status"),
          Commands.slash("info", "Show information about the Verifier agent"),
          Commands
            .slash("trust-ping", "Send a Trust Ping to a DID")
            .addOption(OptionType.STRING, "did", "The DID to ping", true),
          Commands
            .slash("resolve-did", "Resolve a DID to its DID Document")
            .addOption(OptionType.STRING, "did", "The DID to resolve", true),
          Commands.slash("db", "SHOW DB"), // TODO REMOVE
          Commands
            .slash("leave", "Makes the bot leave the server")
            .setContexts(InteractionContextType.GUILD) // this doesn't make sense in DMs
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED) // only admins should be able to use this command.
        )
        commandListUpdateAction.complete().asScala
      }
      _ <- Console.readLine("Enter to Shutdown")
      _ = jda.shutdown()
      _ <- ZIO.debug(s"Await Shutdown")
      _ = jda.awaitShutdown()
      _ <- ZIO.debug(s"Application Ended")
    } yield ()

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath())
  //   Runtime.removeDefaultLoggers >>> SLF4J.slf4j(mediatorColorFormat)

  override def run = bot // .provide(AppConfig.layer, DBConfig.layer, ServerConfig.layer)
}

class MessageReceiveListener extends ListenerAdapter {
  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    println(s"[${event.getChannel()}] ${event.getAuthor()}: ${event.getMessage().getContentDisplay()}");
  }
}

class SlashCommandListener(
    agent: Agent
) extends ListenerAdapter {

  import fmgp.crypto.error.*
  import fmgp.did.*
  import fmgp.did.comm.*
  import fmgp.did.comm.protocol.auth.*

  def resolverLayer = ZLayer.fromZIO(makeResolver)
  def makeResolver: ZIO[Client & Scope, Nothing, Resolver] = for {
    // uniresolver <- Uniresolver.make()
    _ <- ZIO.log("Make MultiFallbackResolver")
    multiResolver = MultiFallbackResolver(
      // HardcodeResolver.default,
      DidPeerResolver.default,
      // uniresolver,
    )
  } yield multiResolver

  var DB = Seq.empty[User]

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    event.getName() match {
      case "say" =>
        handleSayCommand(event)

      case "login" =>
        val (updatedDB, _) = handleLoginCommand(event, agent, DB)
        DB = updatedDB

      case "info" =>
        handleInfoCommand(event, agent)

      case "db" =>
        handleDbCommand(event, DB)

      case "trust-ping" =>
        handleTrustPingCommand(event, agent)

      case "resolve-did" =>
        handleResolveDIDCommand(event, makeResolver)

      case "leave" =>
        handleLeaveCommand(event)

      case any =>
        println(s"ANY: $any")
    }
  }
}
