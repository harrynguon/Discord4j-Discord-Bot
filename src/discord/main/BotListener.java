package discord.main;

import discord.api.CommandProcessor;
import discord.api.ConfigHandler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;

import java.util.Optional;

public class BotListener {

    /** Force JVM to create new String reference */
    public static String prefix = new String("=");

    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getContent().toLowerCase().startsWith(prefix)) {
            CommandProcessor.processCommand(event.getMessage(), prefix);
        }
    }

    /**
     * Triggered whenever the bot is added to a server.
     * This method makes sure the guild has its own property file
     * @param event
     */
    @EventSubscriber
    public void onBotJoin(GuildCreateEvent event) {
        ConfigHandler.addGuild(event.getGuild());
    }

    /**
     * Triggered when a user joins the server to which the bot is in.
     * Whenever a user joins the guild, checks to see if the server has a welcome message
     * and if it does, it will replace the @user (if defined in the message) and mention the user
     * @param event
     */
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        Optional<Object> obj = Optional.of(ConfigHandler.getProperty(event.getGuild(), "welcome"));
        if (obj.isPresent()) {
            String message = ((String) obj.get()).replaceAll("@user", event.getUser().mention());
            try {
                event.getGuild().getDefaultChannel().sendMessage(message);
            } catch (NullPointerException e) {
                // send the message to the very first channel instead if the default channel
                // does not exist
                event.getGuild().getChannels().get(0).sendMessage(message);
            }
        }
    }


}
