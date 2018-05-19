package sc.loot.main;

import sc.loot.processor.EventListener;
import sc.loot.util.Constants;
import sc.loot.util.SCLootScheduler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static Instant scheduledReportTime;
    public static Optional<IDiscordClient> bot = Optional.empty();



    public static IDiscordClient createClient(String token, boolean login) {
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token)
                .setPresence(StatusType.ONLINE, ActivityType.PLAYING, "Welcome to SC Loot.");
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
        try {
            bot = Optional.of(createClient(args[0], true));
            IDiscordClient discordClient = bot.get();
            EventDispatcher dis = discordClient.getDispatcher();
            EventListener eventListener = new EventListener(discordClient);
            dis.registerListener(eventListener);
            // Schedules the weekly/monthly reports
            // The initial delay is in minutes
            int initialDelay = 1395;
            int repeatCycle = 1440;
            scheduledReportTime = Instant.now().plus(12, ChronoUnit.HOURS).plus(initialDelay, ChronoUnit.MINUTES);
            System.out.println("The initial delay before the scheduler runs is: " + initialDelay + "" +
                    " minutes.");
            System.out.println("The scheduler will run at " + scheduledReportTime + " and repeat" +
                    " every 24 hours.");
            scheduler.scheduleAtFixedRate(SCLootScheduler::automatedReport,
                    initialDelay,
                    repeatCycle,
                    TimeUnit.MINUTES
            );
        } catch (DiscordException e) {
            bot.get().getGuildByID(Constants.SC_LOOT_GUILD_ID).getChannelByID
                    (413975567931670529L).sendMessage("`"+e.getErrorMessage()+"`");
        } catch (Exception e) {
            bot.get().getGuildByID(Constants.SC_LOOT_GUILD_ID).getChannelByID
                    (413975567931670529L).sendMessage("`"+e.getMessage()+"`");
        }
    }
}
