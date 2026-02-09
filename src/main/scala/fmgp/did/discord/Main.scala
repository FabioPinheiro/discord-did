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
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.components.container.*
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import net.dv8tion.jda.api.EmbedBuilder

import java.nio.charset.StandardCharsets
import scala.compiletime.ops.boolean
import fmgp.did.Agent
import fmgp.did.framework.TransportFactoryImp

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import fmgp.did.comm.*

//Discord example https://github.com/bbarker/diz/blob/main/src/main/scala/Main.scala
object DiscordDID extends ZIOAppDefault {

  def botDiscordProgram =
    for {
      agent <- ZIO.service[ApplicationAgent]
      config <- ZIO.service[ApplicationConfig]
      jda <- ZIO.succeed {
        JDABuilder
          .createLight(
            config.discordToken,
            Seq().asJava
          ) // EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
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
    } yield jda

  def botAgentProgram(port: Int) = for {
    _ <- ZIO.log(s"Bot Agent Program Started (on port $port)")

    // a = ??? : ZLayer[Any, Nothing, fmgp.did.framework.Operator]
    // b = ??? : ZLayer[Any, Nothing, fmgp.did.comm.Operations]

    myServer <- Server
      .serve((BotDidAgent.didCommApp ++ DIDCommRoutes.app) @@ (Middleware.cors))
      // .serve((BotDidAgent.didCommApp) @@ (Middleware.cors))
      .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(Server.defaultWithPort(port))
      .debug
      .fork

    // _ <- myServer.join *> ZIO.log(s"Bot Agent Program End")
    // _ <- ZIO.log(s"*" * 100)

  } yield (myServer)

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath())
  //   Runtime.removeDefaultLoggers >>> SLF4J.slf4j(mediatorColorFormat)

  override def run =
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

      applicationConfig <- ZIO
        .config(ApplicationConfig.configAgent)
        .provideLayer(ZLayer.succeed(configs))

      _ <- ZIO.log(s"Application started")
      _ <- ZIO.log(s"Application Config: ${applicationConfig}")
      agent = applicationConfig.applicationAgent
      transportFactory = Client.default >>> TransportFactoryImp.layer
      botAgent = BotDidAgent(applicationConfig.did, applicationConfig.keyStore)
      botAgentLayer = ZLayer(ZIO.succeed(botAgent))

      myServer <- botAgentProgram(applicationConfig.port)
        .provideSomeLayer(Scope.default >>> ((botAgentLayer ++ transportFactory) >>> OperatorImp.layer))
        .provideSomeLayer(Operations.layerOperations ++ botAgentLayer)

      jda <- botDiscordProgram
        .provideEnvironment(
          ZEnvironment(agent) ++ ZEnvironment(applicationConfig)
        )

      // ### Shutdown ###
      _ <- Console.readLine("Enter to Shutdown")

      // Discord Bot
      _ = jda.shutdown()
      // HTTP Server
      exitServer <- myServer.interrupt.delay(2.seconds) *> ZIO.log(s"Server stop")

      _ <- ZIO.debug(s"Await Shutdown")
      _ = jda.awaitShutdown()
      _ <- myServer.join
      _ <- ZIO.debug(s"Application Ended")
    } yield ()
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
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe
        .run {
          ZIO.log(s"COMMAND /${event.getName()}") *> {
            event.getName() match {
              case "say" =>
                ZIO.succeed(handleSayCommand(event))

              case "login" =>
                val (updatedDB, _) = handleLoginCommand(event, agent, DB)
                DB = updatedDB
                ZIO.unit

              case "info" =>
                ZIO.succeed(handleInfoCommand(event, agent))

              case "db" =>
                ZIO.succeed(handleDbCommand(event, DB))

              case "trust-ping" =>
                ZIO.succeed(handleTrustPingCommand(event, agent))

              case "resolve-did" =>
                handleResolveDIDCommand(event)
                  .provideSomeLayer(Utils.resolverLayer ++ Operations.layerOperations)

              case "leave" =>
                ZIO.succeed(handleLeaveCommand(event))

              case any =>
                ZIO.succeed(println(s"ANY: $any"))
            }

          }
        }
    }
  }
}
