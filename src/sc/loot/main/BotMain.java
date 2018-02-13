package sc.loot.main;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;

/**
 * To run this discord bot, paste in your token ID in the program arguments.
 */
public class BotMain {

    public static IDiscordClient createClient(String token, boolean login) {
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token);
        try {
            if (login) {
                return clientBuilder.login();
            } else {
                return clientBuilder.build();
            }
        } catch (DiscordException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        final IDiscordClient bot = createClient(args[0], true);
        EventDispatcher dis = bot.getDispatcher();
        dis.registerListener(new BotListener());
    }
}
