package sc.loot.main;

import sc.loot.processor.EventListener;
import sc.loot.util.SCLootScheduler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static Optional<IDiscordClient> bot = Optional.empty();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static IDiscordClient createClient(String token, boolean login) {
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token)
                .setPresence(StatusType.ONLINE, ActivityType.PLAYING, "Perf SC Boosters..");
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
        bot = Optional.of(createClient(args[0], true));
        IDiscordClient discordClient = bot.get();
        EventDispatcher dis = discordClient.getDispatcher();
        EventListener eventListener = new EventListener(discordClient);
        dis.registerListener(eventListener);
        scheduler.scheduleAtFixedRate(SCLootScheduler::weeklyReport, 1, 1440, TimeUnit.MINUTES);
    }
}
