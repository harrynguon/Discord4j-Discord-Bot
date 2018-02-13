package discord.main;

import discord.api.CommandProcessor;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class BotListener {

    public static String prefix = "=";

    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getContent().toLowerCase().startsWith(prefix)) {
            CommandProcessor.processCommand(event.getMessage(), prefix);
        }
    }

}
