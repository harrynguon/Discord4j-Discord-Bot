package sc.loot.main;

import sc.loot.api.CommandProcessor;
import sc.loot.api.ConfigHandler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;

public class BotListener {

    /** Force JVM to create new String reference */
    public static String prefix = new String("!");

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
        Object obj = ConfigHandler.getProperty(event.getGuild(), "welcome");
        if (obj != null) {
            String message = ((String) obj).replaceAll("@user", event.getUser().mention());
            event.getUser().getOrCreatePMChannel().sendMessage(message);
//            event.getGuild().getChannelsByName("sc_loot_bot").get(0)
//                    .sendMessage("I have sent a welcome message to " + event.getUser().mention());
        }
    }

}
