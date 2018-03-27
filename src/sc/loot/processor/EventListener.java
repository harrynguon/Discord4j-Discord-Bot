package sc.loot.processor;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import sc.loot.main.Main;
import sc.loot.util.Constants;
import sc.loot.util.SCLootScheduler;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final IDiscordClient client;

    public EventListener(IDiscordClient client) {
        this.client = client;
    }

    /**
     * Whenever a bot handler sends a command, the bot will process the command and the whole message
     * will determine what the bot does.
     * @param event
     */
    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        // command
        if (event.getMessage().getContent().toLowerCase().startsWith(Constants.PREFIX)) {
            CommandProcessor.processCommand(event.getMessage(), Constants.PREFIX, client);
        }
    }

    /**
     * When a user reacts to a message, the bot will assign the user a role of the chosen colour.
     * @param event
     */
    @EventSubscriber
    public void onUserReactToColorMessage(ReactionEvent event) {
        // TODO: once finished, check to see if the channel is in #read_this_first
        // CommandProcessor.processReactionToMessage(event.getReaction(), event.getUser());
    }

    /**
     * When a user joins the server, the user will get PMed with the Welcome Message by the bot
     * @param event
     */
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        String welcomeMessage = Constants.WELCOME_MESSAGE
                .replaceFirst("#REPLACE_THIS", event.getGuild()
                        .getChannelByID(Constants.READ_THIS_FIRST_CHANNEL_ID) // #read_this_first channel
                        .toString());
        IUser user = event.getUser();

        try {
            user.getOrCreatePMChannel().sendMessage(welcomeMessage);
            // add the new user role
            user.addRole(event.getGuild()
                    .getRoleByID(Constants.NEW_USER_ROLE_ID));

            // instruct them to send a pm telling their ign
            user.getOrCreatePMChannel().sendMessage("In order for you to read messages on this server," +
                    " please enter your IGN for StarBreak, " +
                    "starting with `" + Constants.MY_IGN_PREFIX + "`...");

        } catch (DiscordException e) {
            System.out.println(e.getErrorMessage());
            System.out.println("The user does not have " +
                    "direct messages from server members enabled, or the role doesn't exist.");
        }

    }

    /**
     * User has PMed the bot requesting to gain access to the server (since they're a new member)
     * @param event
     */
    @EventSubscriber
    public void onUserPMBot(MessageReceivedEvent event) {
        if (event.getChannel().isPrivate() && // user pm
                event.getAuthor().hasRole(client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                        .getRoleByID(Constants.NEW_USER_ROLE_ID))) {

            IChannel scLootLogChannel = client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                    .getChannelByID(Constants.SC_LOOT_LOG_ID);

            if (event.getMessage().getContent().toLowerCase().contains(Constants.MY_IGN_PREFIX
                    .toLowerCase()) &&
                    // check to see if they typed more than just "my ign is"
                    event.getMessage().getContent().length() > Constants.MY_IGN_PREFIX.length() + 1) {

                event.getAuthor()
                        .removeRole(client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                                .getRoleByID(Constants.NEW_USER_ROLE_ID));

                try {
                    event.getAuthor().getOrCreatePMChannel().sendMessage("Thank you. " +
                            "You have now been given permission to read messages on this server. " +
                            "If you cannot send messages due to not having a phone-verified " +
                            "account, please send a message to the server owner or one of the " +
                            "moderators.");
                } catch (DiscordException e) {
                    System.out.println(e.getErrorMessage());
                    System.out.println("The user does not have " +
                            "direct messages from server members enabled.");
                }

                scLootLogChannel.sendMessage(event.getAuthor() + " has just sent a PM saying: `" +
                        event.getMessage().toString() + "`");

                // calculate account age
                scLootLogChannel.sendMessage(event.getAuthor() + "has just been given permission" +
                        " to read messages on this server.");
            } else {
                // doesn't contain "my ign is...", so just send their message to the log channel
                scLootLogChannel.sendMessage(event.getAuthor() + " has just sent a PM saying: `" +
                        event.getMessage().toString() + "`");
            }
        }
    }

}
