import zio.*
import net.dv8tion.jda.api.JDABuilder
import java.util.EnumSet
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import collection.JavaConverters._
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions

//https://github.com/bbarker/diz/blob/main/src/main/scala/Main.scala
object DiscordDID extends ZIOAppDefault {
  val token = "MTE3MDQ0MjMyOTg4MzE2ODc4OA.GSVRDi.IMZNQo1TgcF7n5jG3YV7TH8OqaWZbisN-0r4YU" // DID
  // val token = "ODEwNjAwNTk5OTg0NjY4NzEz.Gn7Qu_.qvz0_FqLRhiQIEW0npnaHdMvonmyzI3Ayqgqu8" // GH3

  val bot =
    for {
      // c <- ZIO.service[AppConfig]
      _ <- ZIO.debug(s"Application started")
      jda <- ZIO.succeed {
        val jda = JDABuilder
          .createLight(token, Seq().asJava)
          // .createLight(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
          .addEventListeners(new SlashCommandListener())
          // .addEventListeners(new MessageReceiveListener())
          .build()

        val commands = jda.updateCommands()
        // }
        // _ <- ZIO.succeed {

        // Add all your commands on this action instance
        commands.addCommands(
          Commands
            .slash("say", "Makes the bot say what you tell it to")
            .addOption(OptionType.STRING, "content", "What the bot should say", true), // Accepting a user input
          Commands
            .slash("test", "Makes the bot say what you tell it to")
            .addOption(OptionType.STRING, "content", "What the bot should say", true), // Accepting a user input
          Commands
            .slash("leave", "Makes the bot leave the server")
            .setContexts(InteractionContextType.GUILD) // this doesn't make sense in DMs
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED) // only admins should be able to use this command.
        )
        commands.queue(e => {
          e.asScala.foreach { c =>
            println(s"c: ${c.getName()}")
          }
          println("queue")
        })
        commands.complete()
        println("!!!!")
        jda

      }

      // Then finally send your commands to discord using the API

      _ <- Console.readLine("Enter to end")
      _ = jda.shutdown()
      _ <- ZIO.debug(s"awaitShutdown")
      _ = jda.awaitShutdown()
      _ <- ZIO.debug(s"Application Ended")
    } yield ()

  def run = bot // .provide(AppConfig.layer, DBConfig.layer, ServerConfig.layer)
}

class MessageReceiveListener extends ListenerAdapter {
  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    println(s"[${event.getChannel()}] ${event.getAuthor()}: ${event.getMessage().getContentDisplay()}");
  }
}

class SlashCommandListener extends ListenerAdapter {
  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    event.getName() match {
      case "say" => {
        println("SAY:")
        val content = event.getOption("content", _.getAsString);
        event.reply(content).queue();
      };
      case "test" => {
        val content = event.getOption("content", _.getAsString);
        event.reply(content).queue();
      };
      case "leave" => {
        println("LEAVE:")
        event
          .reply("I'm leaving the server now!")
          .setEphemeral(true) // this message is only visible to the command user
          .flatMap(m => event.getGuild().leave()) // append a follow-up action using flatMap
          .queue(); // enqueue both actions to run in sequence (send message -> leave guild)
      };
      case any => println(s"ANY: $any")
    }
  }
}
