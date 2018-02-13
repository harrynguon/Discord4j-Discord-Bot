package discord.main;

import discord.api.CommandProcessor;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;

public class BotListener {

    /** Force JVM to create new String reference */
    public static String prefix = new String("=");

    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getContent().toLowerCase().startsWith(prefix)) {
            CommandProcessor.processCommand(event.getMessage(), prefix);
        }
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event) {
        System.out.println("test");
    }

}
