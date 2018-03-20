package sc.loot.processor;

import sc.loot.util.Constants;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MessageHistory;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CommandProcessor {

    /**
     * Used for finding the maximum emoji count of a message
     */
    private static class ReactionMaxHelper {
        private int maxNumReaction = 0;
        private Optional<IReaction> reaction = Optional.empty();
        private Optional<IMessage> maxReactionMessage = Optional.empty();
    }

    /**
     * Process all input
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
        List<IRole> userRoles = guild.getRolesByName(Constants.BOT_AUTH_NAME);
        if (userRoles.isEmpty() || !sender.hasRole(userRoles.get(0))) {
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
                    user.getOrCreatePMChannel().sendMessage("You have been warned for: `" + warningMessage + "`");
                    // send to channel that the warn function was called
                    channel.sendMessage(user.mention() + " has been warned for: " + "`" + warningMessage + "`");
                }
                return;
            // post a message to #board_of_punishments, pm the user informing they were banned,
            // and then ban the user.
            case "ban":
                if (command.length <=2) {
                    sendInvalidArgumentMessage("ban", channel, prefix);
                }
                Optional<IUser> userToBan = getUser(command[1], channel, guild);
                if (userToBan.isPresent()) {
                    IUser user = userToBan.get();
                    Optional<Message.Attachment> attachment =
                            message.getAttachments().isEmpty() ? Optional.empty() :
                                                        Optional.of(message.getAttachments().get(0));
                    String banMessage = createString(command, 2);
                    // TODO: create embed message to beautify the banning message
                    IMessage banMsg = new MessageBuilder(client)
                            .withContent(user.mention() + " has been banned for: `" + banMessage + "`\n")
                            .appendContent(attachment.isPresent() ? attachment.get().getUrl() : "")
                            .withChannel(message.getChannel())
                            .build();
                    user.getOrCreatePMChannel()
                            .sendMessage("You have been banned from the SC Loot Discord server for: `" + banMessage + "`");
                    guild.banUser(user);
                }
                return;
            case "weeklyreport":
                createWeeklyReport(client);
                return;
            // for testing purposes, will be automated.
            case "monthlyreport":
                //if (guild.getChannelByID(Constants.WEEKLY_REPORT_CHANNEL_ID).getFullMessageHistory().size() + 1 % (4*2) == 0) {
                    createMonthlyReport(client);
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
            default:
                sendInvalidArgumentMessage("invalidcommand", channel, prefix);
                return;
        }

    }

    public static void processReactionToMessage(IReaction reaction, IUser user) {
        IMessage message = reaction.getMessage();
        boolean userHasOtherReaction = message.getReactions()
                .stream()
                .filter(r -> r != reaction)
                .anyMatch(r -> r.getUsers()
                        .stream()
                        .anyMatch(u -> u == user));
        if (userHasOtherReaction) {
            message.getReactions()
                    .stream()
                    .filter(r -> r != reaction)
                    .forEach(r -> {
                        message.removeReaction(user, reaction);
                        updateRole(user, false);
                    });
        } else {
            updateRole(user, true);
        }
    }

    private static void updateRole(IUser user, boolean onlyRole) {
        if (onlyRole) {
            // create role and assign the colour
        } else {
            // remove all other roles
        }
    }

    /**
     * Concatenate the message contents into a String seperated by spaces.
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
     * Creates the weekly report and submits it to #weekly_report
     * @param client
     */
    public static void createWeeklyReport(IDiscordClient client) {
        IGuild guild = client.getGuildByID(Constants.SC_LOOT_GUILD_ID);
        IChannel channel = guild.getChannelByID(Constants.WEEKLY_REPORT_CHANNEL_ID);
        Map<String, Integer> itemCount = createHashTable();
        final Instant currentTime = Instant.now();
        // Zoneoffset.UTC for UTC zone (future reference)
        final LocalDateTime currentTimeLDT = LocalDateTime.ofInstant(currentTime, ZoneOffset.systemDefault());
        final MessageHistory messageHistory = guild
                .getChannelsByName(Constants.SC_LOOT_CHANNEL_NAME).get(0)
                .getMessageHistoryTo(currentTime.minus(Period.ofDays(7)));
        IMessage[] messages = messageHistory.asArray();
        Predicate<IMessage> withinSevenDays = m -> m.getTimestamp().isAfter(currentTime.minus(Period.ofDays(7)));
        ReactionMaxHelper reactionMax = new ReactionMaxHelper();
        long totalMessages = Stream.of(messages)
                .filter(withinSevenDays)
                .count();
        Stream.of(messages)
                .filter(withinSevenDays)
                .forEach(m -> {
                    processMessage(m, itemCount);
                    // find the message with the most number of reactions
                    Optional<IReaction> cReaction = m.getReactions()
                            .stream()
                            .sorted(Comparator.comparing(reaction -> reaction.getCount()))
                            .max(Comparator.comparingInt(IReaction::getCount));
                    if (cReaction.isPresent()) {
                        IReaction reaction = cReaction.get();
                        if (reaction.getCount() > reactionMax.maxNumReaction) {
                            reactionMax.maxNumReaction = reaction.getCount();
                            reactionMax.reaction = cReaction;
                            reactionMax.maxReactionMessage = Optional.of(m);
                        }
                    }
                });

        EmbedBuilder builder1 = new EmbedBuilder();
        EmbedBuilder builder2 = new EmbedBuilder();
        builder1.withTitle("Weekly Item Drop Count Report from __" +
                currentTimeLDT.toLocalDate().minusDays(7) + "__ to __" +
                currentTimeLDT.toLocalDate() + "__ with a total number of `" +
                totalMessages + "` submissions.");
        builder2.withTitle("cont.");
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        Color color = new Color(r, g, b);
        builder1.withColor(color);
        builder2.withColor(color);

        itemCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String k = entry.getKey();
                    int v = entry.getValue();
                    if (v > 0) {
                        if (builder1.getFieldCount() < 25) {
                            builder1.appendField("<:" + k + ":" + guild.getEmojiByName(k).getLongID() +
                                    ">", "`Drop Count: " + v + "`", true);
                        } else {
                            builder2.appendField("<:" + k + ":" + guild.getEmojiByName(k).getLongID() +
                                    ">", "`Drop Count: " + v + "`", true);
                        }
                    }
        });
        channel.sendMessage(builder1.build());
        channel.sendMessage(builder2.build());

        // MOST REACTION STATISTICS MESSAGE //
        ReactionEmoji emoji = reactionMax.reaction.get().getEmoji();
        String mostReactionMessage = "*The submission*:\n**" + reactionMax.maxReactionMessage.get() +
                "** *which has **" + reactionMax.maxNumReaction +
                "** <:" + emoji.getName() + ":" + emoji.getLongID() + ">" + " reactions.*";
        EmbedBuilder statistics = new EmbedBuilder();
        statistics.withTitle("Extras");
        statistics.appendField("Highest single reaction count for a submission during this week.", mostReactionMessage, true);
        statistics.withColor(color);

        channel.sendMessage(statistics.build());

        // send a log to #sc_loot_bot
        IChannel scLootBotChannel = guild.getChannelsByName(
                Constants.SC_LOOT_BOT_CHANNEL_NAME).get(0);
        new MessageBuilder(client).withChannel(scLootBotChannel)
                .withContent("`!weeklyreport` was last called on: " + currentTime + ".")
                .build();
    }

    // if #weekly_report.size + 1 % 8 == 0, call this function as it counts every 4 SCs
    private static void createMonthlyReport(IDiscordClient client) {
        IGuild guild = client.getGuildByID(Constants.SC_LOOT_GUILD_ID);
        IChannel channel = client.getChannelByID(413975567931670529L); // change this to #monthly_report when done.
        // its currently set to #test

        List<IMessage> messages = guild.getChannelByID(414243989009465344L) // weekly report ID
                .getMessageHistoryTo(Instant.now().minus(28, ChronoUnit.DAYS));
        messages.stream()
                .filter(m -> m.getTimestamp().isAfter(Instant.now().minus(28, ChronoUnit.DAYS)))
                .filter(m -> !m.getEmbeds().isEmpty())
                .map(m -> m.getEmbeds().get(0))
                .filter(iEmbed -> !iEmbed.getTitle().equals("Extras"))
                .flatMap(iEmbeds -> iEmbeds.getEmbedFields().stream())
                .forEach(field -> System.out.println(field.getName() + ": " + field.getValue()));

    }

    /**
     * Finds the IUser from the input of the command (command[1]).
     * @param userName
     * @param channel
     * @param guild
     * @return
     */
    private static Optional<IUser> getUser(String userName, IChannel channel, IGuild guild) {
        userName = userName.replaceAll("[<>@!]", "");
        for (char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
            if (userName.toUpperCase().indexOf(alphabet) >= 0){
                channel.sendMessage(userName + " Is not a valid user!");
                return Optional.empty();
            }
        }
        return Optional.of(guild.getUserByID(Long.parseLong(userName)));
    }

    /**
     * The bot will send a message with why the user has sent an invalid command
     * @param type
     * @param channel
     * @param prefix
     */
    private static void sendInvalidArgumentMessage(String type, IChannel channel, String prefix) {
        switch(type) {
            case "setwelcome":
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "setwelcome <String[]:message>]`");
                break;
            case "setrolecolour":
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "setrolecolour <role> <colour>]`");
                break;
            case "kick":
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "kick [@user]`");
                break;
            case "warn":
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "warn [@user <message>]`");
                break;
            case "ban":
                break;
            case "invalidcommand":
                channel.sendMessage("Please enter a valid command! Type `" + prefix + "help` to view the available commands");
                break;
        }
    }

    /**
     * Collect segments of the message and update item Count Hash table
     * @param message
     * @param itemCount
     */
    private static void processMessage(IMessage message, Map<String, Integer> itemCount) {
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
     * Initialises the map of items, each with a count of 0 for the weekly report
     * @return
     */
    private static Map<String, Integer> createHashTable() {
        Map<String, Integer> itemCount = new HashMap<>();
        Stream.of(Constants.ITEMS).forEach(item -> itemCount.put(item, 0));
        return itemCount;
    }

}
