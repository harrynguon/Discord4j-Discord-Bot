package sc.loot.main;

import sc.loot.processor.CommandProcessor;
import sc.loot.util.Constants;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;

public class BotListener {

    private IDiscordClient client;

    public BotListener(IDiscordClient client) {
        this.client = client;
    }

    /**
     * Whenever a bot handler sends a command, the bot will process the command and the whole message
     * will determine what the bot does.
     * @param event
     */
    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
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
        CommandProcessor.processReactionToMessage(event.getReaction(), event.getUser());
    }

    /**
     * When a user joins the server, the user will get PMed with the Welcome Message by the bot
     * @param event
     */
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        event.getUser().getOrCreatePMChannel().sendMessage(Constants.WELCOME_MESSAGE);
    }

}
