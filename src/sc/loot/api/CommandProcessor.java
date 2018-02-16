package sc.loot.api;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.MessageBuilder;

import java.util.List;
import java.util.Optional;

public class CommandProcessor {

    private static final String BOT_AUTH_NAME = "Bot Authorisation";

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
        List<IRole> userRoles = guild.getRolesByName(BOT_AUTH_NAME);
        if (userRoles.size() < 1 || !sender.hasRole(userRoles.get(0))) {
            return;
        }

        // process imessage into string array with args and remove the prefix, then check
        // the corresponding command below
        String[] command = message.getContent().replaceFirst(prefix, "").split(" ");
        // !ping or !PiNG ; doesn't matter
        command[0] = command[0].toLowerCase();

        switch (command[0]) {
            case "ping":
                channel.sendMessage("pong!");
                return;
            case "avatar":
                if (command.length != 2) {
                    sendInvalidArgumentMessage("avatar", channel, prefix);
                    return;
                }
                Optional<IUser> _user = getUser(command[1], channel, guild);
                if (_user.isPresent()) {
                    IUser user = _user.get();
                    channel.sendMessage(user.mention() + "'s avatar: " + user.getAvatarURL());
                }
                return;
            case "setwelcome":
                if (command.length <= 1) {
                    sendInvalidArgumentMessage("setwelcome", channel, prefix);
                    return;
                }
                StringBuilder welcomeMsg = new StringBuilder();
                for (int i = 1; i < command.length; i++) {
                    if (i == command.length - 1) {
                        welcomeMsg.append(command[i]);
                    } else {
                        welcomeMsg.append(command[i] + " ");
                    }
                }
                channel.sendMessage("Set welcome message to: " + welcomeMsg.toString());
                ConfigHandler.setProperty(guild, "welcome", welcomeMsg.toString());
                //message.delete();
                return;
            case "warn":
                if (command.length <= 2) {
                    sendInvalidArgumentMessage("warn", channel, prefix);
                    return;
                }
                Optional<IUser> __user = getUser(command[1], channel, guild);
                if (__user.isPresent()) {
                    IUser user = __user.get();
                    StringBuilder warningMessage = new StringBuilder();
                    for (int i = 2; i < command.length; i++) {
                        if (i == command.length - 1) {
                            warningMessage.append(command[i]);
                        } else {
                            warningMessage.append(command[i] + " ");
                        }
                    }
                    user.getOrCreatePMChannel().sendMessage(warningMessage.toString());
                    // send to channel that the warn function was called
                    channel.sendMessage(user.mention() + " has been warned for: " + "`" + warningMessage.toString() + "`");
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
                    StringBuilder banMessage = new StringBuilder();
                    for (int i = 2; i < command.length; i++) {
                        if (i == command.length - 1) {
                            banMessage.append(command[i]);
                        } else {
                            banMessage.append(command[i] + " ");
                        }
                    }
                    IMessage banMsg = new MessageBuilder(client)
                            .withContent(user.mention() + " has been banned for: " + banMessage + "\n")
                            .appendContent(attachment.isPresent() ? attachment.get().getUrl() : "")
                            .withChannel(message.getChannel())
                            .build();
                    message.delete();
                    user.getOrCreatePMChannel().sendMessage("You have been banned from the SC Loot Discord server for: " + banMessage.toString());
                    message.getGuild().banUser(user);
                    // channel.sendMessage(banMsg.toString());
                }
                return;
            case "help":
                channel.sendMessage(
                        "\n" +
                        "*alias*: **" + prefix + "**\n" +
                        "**ping** \npong! \n" +
                        "**avatar <@user>** \nreturn a link to the user's avatar \n" +
                        "**setwelcome <message>** \nset a welcome message that the bot will PM new users when joining \n" +
                        "**warn <@user> <message>** \ntells the bot to send a PM to the user with the warning message \n" +
                        "**ban <@user> <reason>** \nthe bot will create a post on #board_of_punishments with the following reason, " +
                        "PM the user informing they have been banned for the following reason, and then finally ban the user.");
                return;
            default:
                sendInvalidArgumentMessage("invalidcommand", channel, prefix);
                return;
        }

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
            case "invalidcommand":
                channel.sendMessage("Please enter a valid command! Type `" + prefix + "help` to view the available commands");
                break;
            case "avatar":
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "avatar <@user>]`");
                break;
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
        }
    }

}
