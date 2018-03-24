package sc.loot.processor;

import sc.loot.main.Main;
import sc.loot.util.Constants;
import sc.loot.util.SCLootScheduler;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EventListener {

    public static boolean scOpenTracker;

    private final IDiscordClient client;
    private final ScheduledExecutorService scOpenCounterScheduler = Executors.newScheduledThreadPool(1);

    public EventListener(IDiscordClient client) {
        this.client = client;
        scOpenTracker = true;
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
        /*// sc loot post
        else if (scOpenTracker && event.getMessage()
                .getChannel()
                .getName()
                .equals(Constants.SC_LOOT_CHANNEL_NAME)) {
            scOpenTracker = false;
            scOpenCounterScheduler.schedule(SCLootScheduler::countSC, 4, TimeUnit.HOURS);
        }*/
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

        user.getOrCreatePMChannel().sendMessage(welcomeMessage);

        // add the new user role
        user.addRole(event.getGuild()
                .getRolesByName(Constants.NEW_USER_ROLE_NAME)
                .get(0));

        // instruct them to send a pm telling their ign
        user.getOrCreatePMChannel().sendMessage("Please enter your IGN for StarBreak, " +
                "starting with `" + Constants.MY_IGN_PREFIX + "`...");

    }

    /**
     * User has PMed the bot requesting to gain access to the server (since they're a new member)
     * @param event
     */
    @EventSubscriber
    public void onUserPMBot(MessageReceivedEvent event) {
        if (event.getChannel().isPrivate()) { // user pm
            if (event.getMessage().getContent().toLowerCase().contains(Constants.MY_IGN_PREFIX
                    .toLowerCase())) {
                client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                        .getChannelByID(Constants.SC_LOOT_LOG_ID)
                        .sendMessage(event.getAuthor() + ": " + event.getMessage().toString());
            }
        }
        event.getAuthor()
                .removeRole(client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                        .getRolesByName(Constants.NEW_USER_ROLE_NAME)
                        .get(0));
    }

}
