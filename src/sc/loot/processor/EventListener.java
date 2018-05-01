package sc.loot.processor;

import sc.loot.util.Constants;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

public class EventListener {

    private final IDiscordClient client;

    public EventListener(IDiscordClient client) {
        this.client = client;
    }

    /**
     * Whenever a bot handler sends a command, the bot will process the command and the whole message
     * will determine what the bot does. Or otherwise if a message is sent in the #authentication
     * channel.
     * @param event
     */
    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        // command
        if (event.getMessage().getContent().toLowerCase().startsWith(Constants.PREFIX)) {
            CommandProcessor.processCommand(event.getMessage(), Constants.PREFIX, client);
        } else if (event.getChannel().getLongID() == Constants.AUTHENTICATION_CHANNEL_ID) {
            CommandProcessor.processAuthentication(event);
        }
    }

    /**
     * When a user reacts to a message, the bot will ensure that the reaction is an item that is
     * found in the post. This will now allow users to randomly do any random reactions.
     * @param event
     */
    @EventSubscriber
    public void onUserReaction(ReactionAddEvent event) {
        // TODO: once finished, check to see if the channel is in #read_this_first
        if (event.getChannel().getLongID() == Constants.SC_LOOT_CHANNEL_ID) {
            CommandProcessor.processReaction(event.getReaction(), event.getUser(), event.getMessage());
        }
    }

    /**
     * When a user joins the server, the user will get PMed with the Welcome Message by the bot
     * @param event
     */
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        IUser user = event.getUser();
        user.addRole(event.getGuild().getRoleByID(Constants.NEW_USER_ROLE_ID));

        try {
            String welcomeMessage = Constants.WELCOME_MESSAGE
                    .replaceFirst("#REPLACE_THIS",
                            event.getGuild()
                                    .getChannelByID(Constants.READ_THIS_FIRST_CHANNEL_ID) // #read_this_first channel
                                    .toString());
            user.getOrCreatePMChannel().sendMessage(welcomeMessage);
        } catch (DiscordException e) {
            System.out.println(e.getErrorMessage());
        }

        // Uncomment this if we want the to-be-authenticated members to be able to read the
        // message history
/*        // Wait a bit to ensure the user can read the message as they are not allowed to read
        // the message history
        try {
            Thread.sleep(3000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        event.getGuild().getChannelByID(Constants.AUTHENTICATION_CHANNEL_ID).sendMessage(
                "Hi " + event.getUser().mention()
                        + "! To be granted access for reading messages on this server"
                        + ", please enter your IGN for **StarBreak** (*case sensitive*) starting " +
                        "with `" +
                        Constants.MY_IGN_PREFIX + "` ...");
    }

    /**
     * User has PMed the bot requesting to gain access to the server (since they're a new member)
     * @param event
     */
    @EventSubscriber
    public void onUserPMBot(MessageReceivedEvent event) {
        if (event.getChannel().isPrivate()) {
            IChannel scLootLogChannel = client.getGuildByID(Constants.SC_LOOT_GUILD_ID)
                    .getChannelByID(Constants.SC_LOOT_LOG_ID);
            scLootLogChannel.sendMessage(event.getAuthor() + " has just sent a PM saying: `" +
                    event.getMessage().toString() + "`");
        }
    }

}
