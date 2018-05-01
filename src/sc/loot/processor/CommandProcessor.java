package sc.loot.processor;

import sc.loot.main.Main;
import sc.loot.util.Constants;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MessageHistory;

import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandProcessor {

    /**
     * Process all input
     *
     * @param message
     * @param prefix
     */
    public static void processCommand(IMessage message, String prefix, IDiscordClient client) {
        // discord user sender
        IUser sender = message.getAuthor();
        // discord channel
        IChannel channel = message.getChannel();
        // discord server
        IGuild guild = message.getGuild();

        // this only checks for the first occurrence of the role name. if the role does not exist,
        // then it exits, or if the sender does not have the role, then it exits.
        IRole botAuth = guild.getRoleByID(Constants.BOT_AUTH_ROLE_ID);
        if (!sender.hasRole(botAuth)) {
            return;
        }

        // process imessage into string array with args and remove the prefix, then check
        // the corresponding command below
        String[] command = message.getContent().replaceFirst(prefix, "").split(" ");
        // !help or !HeLP; doesn't matter
        command[0] = command[0].toLowerCase();

        switch (command[0]) {
            case "setwelcome":
                if (command.length <= 1) {
                    sendInvalidArgumentMessage("setwelcome", channel, prefix);
                    return;
                }
                channel.sendMessage("To setwelcome, please message Rebuked");
                return;
            case "warn":
                if (command.length <= 2) {
                    sendInvalidArgumentMessage("warn", channel, prefix);
                    return;
                }
                Optional<IUser> __user = getUser(command[1], channel, guild);
                if (__user.isPresent()) {
                    IUser user = __user.get();
                    String warningMessage = createString(command, 2);
                    try {
                        user.getOrCreatePMChannel().sendMessage("You have been warned for: `" +
                                warningMessage + "`");
                        // send to channel that the warn function was called
                        channel.sendMessage(user.mention() + " has been warned for: " + "`" +
                                warningMessage + "`");
                    } catch (DiscordException e) {
                        System.out.println(e.getErrorMessage());
                        System.out.println("The user does not have " +
                                "direct messages from server members enabled.");
                    }

                }
                return;
            // post a message to #board_of_punishments, pm the user informing they were banned,
            // and then ban the user.
            case "ban":
                if (command.length <= 2) {
                    sendInvalidArgumentMessage("ban", channel, prefix);
                }
                Optional<IUser> userToBan = getUser(command[1], channel, guild);
                if (userToBan.isPresent()) {
                    IUser user = userToBan.get();
                    Optional<Message.Attachment> attachment =
                            message.getAttachments().isEmpty() ? Optional.empty() :
                                    Optional.of(message.getAttachments()
                                            .get(0));
                    String banMessage = createString(command, 2);
                    // TODO: create embed message to beautify the banning message
                    IMessage banMsg = new MessageBuilder(client)
                            .withContent(user.mention() + " has been banned for: `" + banMessage +
                                    "`\n")
                            .appendContent(attachment.isPresent() ? attachment.get().getUrl() : "")
                            .withChannel(message.getChannel())
                            .build();
                    try {
                        user.getOrCreatePMChannel()
                                .sendMessage("You have been banned from " +
                                        "the SC Loot Discord server for: `" + banMessage + "`");
                    } catch (DiscordException e) {
                        System.out.println(e.getErrorMessage());
                        System.out.println("The user does not have " +
                                "direct messages from server members enabled.");
                    }
                    guild.banUser(user);
                }
                return;
            case "weeklyreport":
//                createReport(client, Constants.WEEKLY_REPORT_CHANNEL_ID, Constants.WEEKLY);
                return;
            case "monthlyreport":
//                createReport(client, Constants.MONTHLY_REPORT_CHANNEL_ID, Constants.MONTHLY);
                return;
            case "help":
                channel.sendMessage(Constants.HELP_MESSAGE);
                return;
            case "changestatus":
                if (command.length < 2) {
                    return;
                }
                String newStatus = createString(command, 1);
                client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, newStatus);
                channel.sendMessage("My online status has been changed to: `" + newStatus + "`");
                return;
            case "getroleid":
                if (command.length < 2) {
                    return;
                }
                String roleName = createString(command, 1);
                List<IRole> role = guild.getRolesByName(roleName);
                if (role.size() != 0) {
                    channel.sendMessage("There was `" + role.size() + "` role that was retrieved." +
                            " The role ID for `" + roleName + "` is: `" +
                            role.get(0).getLongID() + "`");
                }
                return;
            case "addmemberrolestoallusers":
                for (int i = 0; i < guild.getUsers().size(); i++) {
                    IUser u = guild.getUsers().get(i);
                    if (!u.hasRole(guild.getRoleByID(Constants.MEMBER_ROLE_ID))) {
                        u.addRole(guild.getRoleByID(Constants.MEMBER_ROLE_ID));
                    }
                    if (i % 10 == 0) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return;
            case "testweeklyreport":
                createReport(client, Constants.TEST_CHANNEL_ID, Constants.WEEKLY);
                return;
            case "testmonthlyreport":
                createReport(client, Constants.TEST_CHANNEL_ID, Constants.MONTHLY);
                return;
            case "shutdown":
                guild.getChannelByID(Constants.SC_LOOT_BOT_CHANNEL_ID).sendMessage("The bot is shutting " +
                        "down. This was initiated by " + message.getAuthor().mention() + ".") ;
                System.exit(0);
                return;
            default:
                sendInvalidArgumentMessage("invalidcommand", channel, prefix);
                return;
        }

    }

    /**
     * Concatenate the message contents into a String seperated by spaces.
     *
     * @param command
     * @param startingIndex
     * @return
     */
    private static String createString(String[] command, int startingIndex) {
        StringBuilder message = new StringBuilder();
        for (int i = startingIndex; i < command.length; i++) {
            if (i == command.length - 1) {
                message.append(command[i]);
            } else {
                message.append(command[i] + " ");
            }
        }
        return message.toString();
    }

    /**
     * Creates the report and submits it to #weekly_report or #monthly_report, depending on its type
     *
     * @param client
     */
    public static void createReport(IDiscordClient client, long channelId, String reportType) {
        System.out.println("--- Data printed from the CommandProcessor.class ---");
        String weekOrMonth = reportType.equals(Constants.WEEKLY) ? "week" : "month";
        int maxReactionSubmissions = reportType.equals(Constants.WEEKLY) ? 5 : 15;

        IGuild guild = client.getGuildByID(Constants.SC_LOOT_GUILD_ID);
        IChannel channel = guild.getChannelByID(channelId);

        Map<String, Integer> itemCount = createHashTable();
        // 1-n => Portal number
        // -1  => Last
        // -2  => Unkown, Extra Info
        Map<Integer, Integer> portalNumberCount = new HashMap<>();
        Set<IMessage> messagesForReactionPost = new HashSet<>();

        // -------- Date time calculations ---------- /
        // UTC+12
        final Instant currentTime = Instant.now().plus(12, ChronoUnit.HOURS);
        final LocalDateTime currentTimeLDT = LocalDateTime.ofInstant(currentTime, ZoneOffset.ofHours(0));
        int numDaysInTheMonth = currentTimeLDT.toLocalDate().lengthOfMonth() - 1; // otherwise it
        // will cut into the last day of the previous month, but we want the start of this month
        final Instant currentTimeMinusOneMonth = LocalDateTime.ofInstant(
                currentTime.minus(Period.ofDays(numDaysInTheMonth)),
                ZoneId.of("UTC")
        )
                .toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.ofHours(0));

        LocalDate currentTimeMinusTimePeriodForDisplay =
                reportType.equals(Constants.WEEKLY) ?
                        currentTimeLDT.toLocalDate().minusDays(7) :
                        currentTimeLDT.minusDays(numDaysInTheMonth).toLocalDate();

        System.out.println("The current date and time is: " + currentTime);
        System.out.println("The current date and time minus one month is: " + currentTimeMinusOneMonth);

        // process data given within the time range
        final IMessage[] messages = getiMessages(reportType, guild, currentTime,
                currentTimeMinusOneMonth)
                .asArray();

        Predicate<IMessage> withinTheTimePeriod =
                reportType.equals(Constants.WEEKLY) ?
                        m -> m.getTimestamp().isAfter(currentTime.minus(Period.ofDays(7))) :
                        m -> m.getTimestamp().isAfter(currentTimeMinusOneMonth);

        long totalMessages = getTotalMessages(messages, withinTheTimePeriod);

        // process each message
        processEachMessage(itemCount, portalNumberCount, messages, withinTheTimePeriod,
                messagesForReactionPost);

        // custom comparator that stores messages by their most-highest reaction count
        Comparator<IMessage> mostReactions = Comparator.comparingInt(
                m -> m.getReactions()
                        .stream()
                        .max(Comparator.comparingInt(IReaction::getCount)).get()
                        .getCount()
        );

        // a list of the top N messages by their highest single reaction count, sorted in descending
        // order (from highest to lowest)
        List<IMessage> topReactionMessages = messagesForReactionPost.stream()
                .sorted(mostReactions.reversed())
                .limit(maxReactionSubmissions)
                .collect(Collectors.toList());

        System.out.println("Data has been processed for the " + reportType + " report");

        /////------- START BUILDING ALL EMBEDDED MESSAGES AFTER PROCESSING DATA -------////

        EmbedBuilder report1 = new EmbedBuilder();
        EmbedBuilder report2 = new EmbedBuilder();
        report1.withTitle(reportType + " Item Drop Count Report from __" +
                currentTimeMinusTimePeriodForDisplay + "__ to __" +
                currentTimeLDT.toLocalDate() + "__ with a total number of `" +
                totalMessages + "` submissions.");
        report2.withTitle("cont.");

        Color color = getRandomColor();

        // start building the drop count table for the weekly/monthly report
        buildDropCounts(guild, itemCount, report1, report2);

        // section for portal number count submissions
        EmbedBuilder portalCounts = buildPortalCounts(weekOrMonth, portalNumberCount, color);

        // MOST REACTION STATISTICS MESSAGE //
        EmbedBuilder statistics1 = new EmbedBuilder();
        statistics1.withTitle("Extras");
        statistics1.appendField("__Reactions__",
                "Top " + maxReactionSubmissions + " distinct reactions from different " +
                        "submissions during this " + weekOrMonth + ".", true);

        EmbedBuilder statistics2 = new EmbedBuilder();
        statistics2.withTitle("Extras");
        statistics2.appendField("__Reactions continued__",
                "Top " + maxReactionSubmissions + " distinct reactions from different " +
                        "submissions during this " + weekOrMonth + ".", true);

        // append all top reaction messages to the statistics embed messages
        addTopReactionMsgs(topReactionMessages, statistics1, statistics2);

        report1.withColor(color);
        report2.withColor(color);
        statistics1.withColor(color);
        statistics2.withColor(color);

        channel.sendMessage(report1.build());
        channel.sendMessage(report2.build());
        channel.sendMessage(portalCounts.build());
        channel.sendMessage(statistics1.build());
        if (reportType.equals(Constants.MONTHLY)) {
            channel.sendMessage(statistics2.build());
        }

        System.out.println("Messages have been sent to the " + reportType + " report channel.");

        // send a log to #sc_loot_bot
        IChannel scLootBotChannel = guild.getChannelByID(Constants.SC_LOOT_BOT_CHANNEL_ID);

        if (channelId != Constants.TEST_CHANNEL_ID) {
            new MessageBuilder(client).withChannel(scLootBotChannel)
                    .withContent("`" + reportType + " report` has just been initiated. The " +
                            "current time is: `"
                            + currentTime + "`.")
                    .build();
        }

        System.out.println("A log has just been sent.");
        System.out.println("------");
    }

    private static Color getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        return new Color(r, g, b);
    }

    private static void addTopReactionMsgs(List<IMessage> topReactionMessages,
                                           EmbedBuilder statistics1, EmbedBuilder statistics2) {
        for (int i = 0; i < topReactionMessages.size(); i++) {
            IReaction reaction = topReactionMessages.get(i)
                    .getReactions()
                    .stream()
                    .max(Comparator.comparingInt(IReaction::getCount)).get();
            ReactionEmoji emoji = reaction.getEmoji();
            int noReactions = reaction.getCount();

            String message = "" + topReactionMessages.get(i) +
                    " \n*which has **" + noReactions +
                    "** <:" + emoji.getName() + ":" + emoji.getLongID() + ">" + " reactions.*";

            //add each post as a field to the post.
            int subNumber = i + 1;
            if (i < 10) {
                statistics1.appendField("Submission #" + subNumber + ":", message, true);
            } else {
                statistics2.appendField("Submission #" + subNumber + ":", message, true);
            }
        }
    }

    private static EmbedBuilder buildPortalCounts(String weekOrMonth,
                                                  Map<Integer, Integer> portalNumberCount,
                                                  Color color) {
        EmbedBuilder portalCounts = new EmbedBuilder();
        portalCounts.withTitle("Extras");
        portalCounts.appendField("__Portal numbers__",
                "The count of all the portal numbers that were submitted during this "
                        + weekOrMonth + ".",
                true);

        portalNumberCount.entrySet()
                .stream()
                .forEachOrdered(entry -> {
                    int k = entry.getKey();
                    int value = entry.getValue();
                    if (k >= 1) {
                        portalCounts.appendField("Portal #" + k + ":", Integer.toString(value),
                                true);
                    }
                });
        if (portalNumberCount.containsKey(-1)) {
            portalCounts.appendField("Last Portal:",
                    Integer.toString(portalNumberCount.get(-1)),
                    true);
        }
        if (portalNumberCount.containsKey(-2)) {
            portalCounts.appendField("Other (unknown, extra info, etc.):",
                    Integer.toString(portalNumberCount.get(-2)),
                    true);
        }

        portalCounts.withColor(color);
        return portalCounts;
    }

    private static void buildDropCounts(IGuild guild, Map<String, Integer> itemCount,
                                        EmbedBuilder builder1, EmbedBuilder builder2) {
        itemCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String k = entry.getKey();
                    int v = entry.getValue();
                    if (v > 0) {
                        if (builder1.getFieldCount() < 25) {
                            builder1.appendField(
                                    "<:" + k + ":" + guild.getEmojiByName(k).getLongID() + ">",
                                    "`Drop Count: " + v + "`", true);
                        } else {
                            builder2.appendField(
                                    "<:" + k + ":" + guild.getEmojiByName(k).getLongID() + ">",
                                    "`Drop Count: " + v + "`", true);
                        }
                    }
                });
    }

    private static void processEachMessage(Map<String, Integer> itemCount,
                                           Map<Integer, Integer> portalNumberCount,
                                           IMessage[] messages, Predicate<IMessage> withinTimePeriod,
                                           Set<IMessage> messagesForReactionPost) {
        Stream.of(messages)
                .filter(withinTimePeriod)
                .forEach(m -> {
                    processRawMessage(m, itemCount);
                    processPortalNumber(m.getContent(), portalNumberCount);
                    if (m.getReactions().size() != 0) {
                        messagesForReactionPost.add(m);
                    }
                });
    }

    private static MessageHistory getiMessages(String reportType, IGuild guild, Instant currentTime,
                                               Instant currentTimeMinusOneMonth) {
        return reportType.equals(Constants.WEEKLY) ?
                guild.getChannelByID(Constants.SC_LOOT_CHANNEL_ID)
                        .getMessageHistoryTo(currentTime.minus(Period.ofDays(7))) :
                guild.getChannelByID(Constants.SC_LOOT_CHANNEL_ID)
                        .getMessageHistoryTo(currentTimeMinusOneMonth);
    }

    private static long getTotalMessages(IMessage[] messages,
                                         Predicate<IMessage> withinTheTimePeriod) {
        return Stream.of(messages)
                .filter(withinTheTimePeriod)
                .count();
    }

    /**
     * Finds the IUser from the input of the command (command[1]).
     *
     * @param userName
     * @param channel
     * @param guild
     * @return
     */
    private static Optional<IUser> getUser(String userName, IChannel channel, IGuild guild) {
        userName = userName.replaceAll("[<>@!]", "");
        for (char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
            if (userName.toUpperCase().indexOf(alphabet) >= 0) {
                channel.sendMessage(userName + " Is not a valid user!");
                return Optional.empty();
            }
        }
        return Optional.of(guild.getUserByID(Long.parseLong(userName)));
    }

    /**
     * The bot will send a message with why the user has sent an invalid command
     *
     * @param type
     * @param channel
     * @param prefix
     */
    private static void sendInvalidArgumentMessage(String type, IChannel channel, String prefix) {
        switch (type) {
            case "setwelcome":
                channel.sendMessage("Please enter valid arguments!" +
                        " `[" + prefix + "setwelcome <String[]:message>]`");
                break;
            case "setrolecolour":
                channel.sendMessage("Please enter valid arguments!" +
                        " `[" + prefix + "setrolecolour <role> <colour>]`");
                break;
            case "kick":
                channel.sendMessage("Please enter valid arguments!" +
                        " `[" + prefix + "kick [@user]`");
                break;
            case "warn":
                channel.sendMessage("Please enter valid arguments!" +
                        " `[" + prefix + "warn [@user <message>]`");
                break;
            case "ban":
                break;
            case "invalidcommand":
                channel.sendMessage("Please enter a valid command!" +
                        " Type `" + prefix + "help` to view the available commands");
                break;
        }
    }

    /**
     * Collect segments of the message and update item Count Hash table
     *
     * @param message
     * @param itemCount
     */
    private static void processRawMessage(IMessage message, Map<String, Integer> itemCount) {
        String content = message.getContent();
        Scanner scan = new Scanner(content);
        String previous = "";
        while (scan.hasNext()) {
            String segment = scan.next().toLowerCase();
            String finalPrevious = previous;
            Stream.of(Constants.ITEMS)
                    .filter(item -> segment.contains(item))
                    .forEach(item -> {
                        // check if the item is an implant. if so, check if the previous word is
                        // "inf"
                        if (Constants.IMPLANTS.contains(item)) {
                            if (!finalPrevious.contains("inf")) {
                                itemCount.put(item, itemCount.get(item) + 1);
                            }
                        } else {
                            itemCount.put(item, itemCount.get(item) + 1);
                        }
                    });
            previous = segment;
        }
        scan.close();
    }

    /**
     * (Int,Int)
     * @param message
     * @param portalCountMap
     */
    private static void processPortalNumber(String message, Map<Integer, Integer> portalCountMap) {
        String portalNumber = message.split(" ")[0];
        char c = portalNumber.charAt(0);
        if (Character.isDigit(c)) {
            int portNum = Character.getNumericValue(c);
            incrementCount(portNum, portalCountMap);
        } else {
            if (portalNumber.toLowerCase().contains("last")) {
                incrementCount(-1, portalCountMap);
            } else {
                // unknown, extra info
                incrementCount(-2, portalCountMap);
            }
        }
    }

    /**
     * 1-n => Portal number
     * -1  => Last
     * -2  => Unkown, Extra Info
     * @param portNum
     * @param portalCountMap
     */
    private static void incrementCount(int portNum, Map<Integer, Integer> portalCountMap) {
        if (portalCountMap.containsKey(portNum)) {
            portalCountMap.put(portNum, portalCountMap.get(portNum) + 1);
        } else {
            portalCountMap.put(portNum, 1);
        }
    }

    /**
     * Initialises the map of items, each with a count of 0 for the weekly/monthly report
     *
     * @return
     */
    private static Map<String, Integer> createHashTable() {
        Map<String, Integer> itemCount = new HashMap<>();
        Stream.of(Constants.ITEMS).forEach(item -> itemCount.put(item, 0));
        return itemCount;
    }

    /**
     * Processes the authentication of a user in the #AUTHENTICATION channel.
     * @param event
     */
    public static void processAuthentication(MessageReceivedEvent event) {
        IGuild guild = event.getGuild();
        IChannel scLootLogChannel = guild.getChannelByID(Constants.SC_LOOT_LOG_ID);
        if (event.getAuthor().hasRole(guild.getRoleByID(Constants.NEW_USER_ROLE_ID))) {

            String[] input = event.getMessage().getContent().split(" ");

            if (event.getMessage().getContent().toLowerCase().contains(Constants.MY_IGN_PREFIX
                    .toLowerCase()) &&
                    // check to see if they typed more than just "my ign is"
                    input.length > 3) {

                String username = input[3];
                username = username.replace(".", "");
                boolean validUsername = checkUsername(username);

                if (validUsername) {
                    try {
                        event.getAuthor().removeRole(guild.getRoleByID(Constants.NEW_USER_ROLE_ID));
                        event.getAuthor().addRole(guild.getRoleByID(Constants.MEMBER_ROLE_ID));
                        sendAuthenticatedPM(event);
//                        deleteChannelLog(guild.getChannelByID(Constants.AUTHENTICATION_CHANNEL_ID));
                    } catch (DiscordException e) {
                        System.out.println(e.getErrorMessage());
                    }
                    scLootLogChannel.sendMessage(event.getAuthor() + " has just been given " +
                            "permission to read messages on this server. Their username is `" +
                            username + "`.");
                } else {
                    scLootLogChannel.sendMessage(event.getAuthor() + " has tried to authenticate" +
                            " themself with an invalid username which was `" + username + "`.");
                }
            }
        }
    }

    /**
     * Deletes the authentication channel history and copies it over to the authentication log
     * channel.
     * @param authenticationChannel
    private static void deleteChannelLog(IChannel authenticationChannel) {
        IGuild guild = authenticationChannel.getGuild();
        IChannel authenticationLogChannel =
                guild.getChannelByID(Constants.AUTHENTICATION_LOG_CHANNEL_ID);
        List<IMessage> messages = authenticationChannel.getFullMessageHistory().bulkDelete();
        for (IMessage message : messages) {
            if (message.getAuthor().hasRole(guild.getRoleByID(Constants.SC_LOOT_BOT_ROLE_ID))) {
                authenticationLogChannel.sendMessage(message.getContent());
            } else {
                authenticationLogChannel.sendMessage(message.getAuthor().mention() + ": " + message
                        .getContent());
            }
        }
    }*/

    /**
     * Scans the all time leaderboard website and checks to see if the user entered a valid username
     * @param username
     * @return
     */
    private static boolean checkUsername(String username) {
        InputStream is = null;
        try {
            URL url = new URL(Constants.ALL_TIME_LEADERBOARDS_WEBSITE);
            URLConnection con = url.openConnection();
            is = con.getInputStream();
        } catch (Exception e) {
            Main.bot.get().getGuildByID(Constants.SC_LOOT_GUILD_ID).getChannelByID(Constants
                    .SC_LOOT_BOT_CHANNEL_ID).sendMessage("Unable to process data from the All " +
                    "Time Leaderboards website at: `" + Constants.ALL_TIME_LEADERBOARDS_WEBSITE + "`");
            return false;
        }
        Scanner scan = new Scanner(is);
        boolean isActive = false;
        while (scan.hasNext()) {
            String playerEntry = Stream.of(scan.nextLine().split(" "))
                    .filter(s -> !s.contains("."))
                    .collect(Collectors.joining())
                    .replace(" ", "");
            if (!isActive && playerEntry.equals("PlayerPoints")) {
                isActive = true;
                continue;
            }
            if (isActive) {
                if (playerEntry.equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void sendAuthenticatedPM(MessageReceivedEvent event) throws DiscordException {
        event.getAuthor().getOrCreatePMChannel().sendMessage("Thank you. " +
                "You have now been given permission to read messages on this server. " +
                "If you cannot send messages due to not having a phone-verified " +
                "account, please send a message to the server owner or one of the " +
                "moderators.");

    }


    public static void processReaction(IReaction reaction, IUser user, IMessage message) {
        if (!message.getContent().contains(reaction.getEmoji().getName())) {
            // :velites: => getEmoji().getName() => velites
            message.removeReaction(user, reaction);
        }
    }
}
