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
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
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
        if (userRoles.size() < 1 || !sender.hasRole(userRoles.get(0))) {
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
                    message.delete();
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
                    message.delete();
                    user.getOrCreatePMChannel().sendMessage("You have been banned from the SC Loot Discord server for: `" + banMessage + "`");
                    guild.banUser(user);
                }
                return;
            case "weeklyreport":
                createWeeklyReport(message, channel, guild, client);
                message.delete();
                return;
            case "help":
                channel.sendMessage(Constants.HELP_MESSAGE);
                return;
            default:
                sendInvalidArgumentMessage("invalidcommand", channel, prefix);
                return;
        }

    }

    public static void processReactionToMessage(IReaction reaction, IUser user) {
        IMessage wholeMessage = reaction.getMessage();
        boolean userHasOtherReaction = wholeMessage.getReactions()
                .stream()
                .filter(r -> r != reaction)
                .anyMatch(r -> r.getUsers()
                        .stream()
                        .anyMatch(u -> u == user));
        if (userHasOtherReaction) {
            wholeMessage.getReactions()
                    .stream()
                    .filter(r -> r != reaction)
                    .forEach(r -> {
                        wholeMessage.removeReaction(user, reaction);
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
     * @param message
     * @param channel
     */
    private static void createWeeklyReport(IMessage message, IChannel channel, IGuild guild, IDiscordClient client) {
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
                "** *by " + reactionMax.maxReactionMessage.get().getAuthor() +
                " which has **" + reactionMax.maxNumReaction +
                "** <:" + emoji.getName() + ":" + emoji.getLongID() + ">" + " reactions.*";
        EmbedBuilder statistics = new EmbedBuilder();
        statistics.withTitle("Extras");
        statistics.appendField("Highest single reaction count for a submission during this week.", mostReactionMessage, true);
        statistics.withColor(color);

        channel.sendMessage(statistics.build());

        // send a log to #sc_loot_bot
        IMessage lastMessage = messageHistory.getLatestMessage();
        IChannel scLootChannel = guild.getChannelsByName(Constants.SC_LOOT_BOT_CHANNEL_NAME).get(0);
        new MessageBuilder(client).withChannel(scLootChannel)
                .withContent("`!weeklyreport` was last called on: " + currentTime + "." +
                        " The `!weeklyreport` command was initiated by " + message.getAuthor() + ".")
                .build();
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
            if (segment.contains("aeonics")) {
                itemCount.put("aeonics", 1);
            }
            else if (segment.contains("overloaded")) {
                itemCount.put("overloaded", itemCount.get("overloaded") + 1);
            }
            else if (segment.contains("locust")) {
                itemCount.put("locust", itemCount.get("locust") + 1);
            }
            else if (segment.contains("frenzied")) {
                itemCount.put("frenzied", itemCount.get("frenzied") + 1);
            }
            else if (segment.contains("quaestor")) {
                itemCount.put("quaestor", itemCount.get("quaestor") + 1);
            }
            else if (segment.contains("stompers")) {
                itemCount.put("stompers", itemCount.get("stompers") + 1);
            }
            else if (segment.contains("armites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("armites", itemCount.get("armites") + 1);
                }
            }
            else if (segment.contains("calites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("calites", itemCount.get("calites") + 1);
                }
            }
            else if (segment.contains("bellites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("bellites", itemCount.get("bellites") + 1);
                }
            }
            else if (segment.contains("blockers")) {
                itemCount.put("blockers", itemCount.get("blockers") + 1);
            }
            else if (segment.contains("bombard")) {
                itemCount.put("bombard", itemCount.get("bombard") + 1);
            }
            else if (segment.contains("emites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("emites", itemCount.get("emites") + 1);
                }
            }
            else if (segment.contains("volta")) {
                itemCount.put("volta", itemCount.get("volta") + 1);
            }
            else if (segment.contains("motley")) {
                itemCount.put("motley", itemCount.get("motley") + 1);
            }
            else if (segment.contains("firites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("firites", itemCount.get("firites") + 1);
                }
            }
            else if (segment.contains("kunai")) {
                itemCount.put("kunai", itemCount.get("kunai") + 1);
            }
            else if (segment.contains("fireform")) {
                itemCount.put("fireform", itemCount.get("fireform") + 1);
            }
            else if (segment.contains("scorpion")) {
                itemCount.put("scorpion", itemCount.get("scorpion") + 1);
            }
            else if (segment.contains("velites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("velites", itemCount.get("velites") + 1);
                }
            }
            else if (segment.contains("hydra")) {
                itemCount.put("hydra", itemCount.get("hydra") + 1);
            }
            else if (segment.contains("salites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("salites", itemCount.get("salites") + 1);
                }
            }
            else if (segment.contains("mimic")) {
                itemCount.put("mimic", itemCount.get("mimic") + 1);
            }
            else if (segment.contains("blink")) {
                itemCount.put("blink", itemCount.get("blink") + 1);
            }
            else if (segment.contains("ultrites")) {
                if (!previous.contains("inf")) {
                    itemCount.put("ultrites", itemCount.get("ultrites") + 1);
                }
            }
            else if (segment.contains("hyrst")) {
                itemCount.put("hyrst", itemCount.get("hyrst") + 1);
            }
            else if (segment.contains("jerkin")) {
                itemCount.put("jerkin", itemCount.get("jerkin") + 1);
            }
            else if (segment.contains("doublebandos")) {
                itemCount.put("doublebandos", itemCount.get("doublebandos") + 1);
            }
            else if (segment.contains("vitreous")) {
                itemCount.put("vitreous", itemCount.get("vitreous") + 1);
            }
            else if (segment.contains("antiphon")) {
                itemCount.put("antiphon", itemCount.get("antiphon") + 1);
            }
            else if (segment.contains("frag")) {
                itemCount.put("frag", itemCount.get("frag") + 1);
            }
            else if (segment.contains("direfall")) {
                itemCount.put("direfall", itemCount.get("direfall") + 1);
            }
            else if (segment.contains("macron")) {
                itemCount.put("macron", itemCount.get("macron") + 1);
            }
            else if (segment.contains("aerolith")) {
                itemCount.put("aerolith", itemCount.get("aerolith") + 1);
            }
            else if (segment.contains("mantlet")) {
                itemCount.put("mantlet", itemCount.get("mantlet") + 1);
            }
            else if (segment.contains("valenki")) {
                itemCount.put("valenki", itemCount.get("valenki") + 1);
            }
            else if (segment.contains("aesc")) {
                itemCount.put("aesc", itemCount.get("aesc") + 1);
            }
            else if (segment.contains("exites")) {
                itemCount.put("exites", itemCount.get("exites") + 1);
            }
            else if (segment.contains("sledge")) {
                itemCount.put("sledge", itemCount.get("sledge") + 1);
            }
            else if (segment.contains("furious")) {
                itemCount.put("furious", itemCount.get("furious") + 1);
            }
            else if (segment.contains("tacent")) {
                itemCount.put("tacent", itemCount.get("tacent") + 1);
            }
            else if (segment.contains("platin")) {
                itemCount.put("platin", itemCount.get("platin") + 1);
            }
            else if (segment.contains("talons")) {
                itemCount.put("talons", itemCount.get("talons") + 1);
            }
            else if (segment.contains("trackers")) {
                itemCount.put("trackers", itemCount.get("trackers") + 1);
            }
            else if (segment.contains("kyeser")) {
                itemCount.put("kyeser", itemCount.get("kyeser") + 1);
            }
            else if (segment.contains("sabatons")) {
                itemCount.put("sabatons", itemCount.get("sabatons") + 1);
            }
            else if (segment.contains("woven")) {
                itemCount.put("woven", itemCount.get("woven") + 1);
            }
            previous = segment;
        }
        scan.close();
    }

    private static Map<String, Integer> createHashTable() {
        Map<String, Integer> itemCount = new HashMap<>();
        itemCount.put("aeonics", 0);
        itemCount.put("overloaded", 0);
        itemCount.put("locust", 0);
        itemCount.put("frenzied", 0);
        itemCount.put("quaestor", 0);
        itemCount.put("stompers", 0);
        itemCount.put("armites", 0);
        itemCount.put("calites", 0);
        itemCount.put("bellites", 0);
        itemCount.put("blockers", 0);
        itemCount.put("bombard", 0);
        itemCount.put("emites", 0);
        itemCount.put("volta", 0);
        itemCount.put("motley", 0);
        itemCount.put("firites", 0);
        itemCount.put("kunai", 0);
        itemCount.put("fireform", 0);
        itemCount.put("scorpion", 0);
        itemCount.put("velites", 0);
        itemCount.put("hydra", 0);
        itemCount.put("salites", 0);
        itemCount.put("mimic", 0);
        itemCount.put("blink", 0);
        itemCount.put("ultrites", 0);
        itemCount.put("hyrst", 0);
        itemCount.put("jerkin", 0);
        itemCount.put("doublebandos", 0);
        itemCount.put("vitreous", 0);
        itemCount.put("antiphon", 0);
        itemCount.put("frag", 0);
        itemCount.put("direfall", 0);
        itemCount.put("macron", 0);
        itemCount.put("aerolith", 0);
        itemCount.put("mantlet", 0);
        itemCount.put("valenki", 0);
        itemCount.put("aesc", 0);
        itemCount.put("exites", 0);
        itemCount.put("sledge", 0);
        itemCount.put("furious", 0);
        itemCount.put("tacent", 0);
        itemCount.put("platin", 0);
        itemCount.put("talons", 0);
        itemCount.put("trackers", 0);
        itemCount.put("kyeser", 0);
        itemCount.put("sabatons", 0);
        itemCount.put("woven", 0);
        return itemCount;
    }

}
