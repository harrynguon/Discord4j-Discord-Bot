package sc.loot.api;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Optional;

public class CommandProcessor {

    public static void processCommand(IMessage message, String prefix) {
        // discord user sender
        IUser sender = message.getAuthor();
        // discord channel
        IChannel channel = message.getChannel();
        // discord server
        IGuild guild = message.getGuild();

        // process imessage into string array with args and remove the prefix, then check
        // the corresponding command below
        String[] command = message.getContent().toLowerCase().replaceFirst(prefix, "").split(" ");

        switch (command[0]) {
            case "ping":
                channel.sendMessage("pong!");
                break;
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
            case "kick":
                if (command.length != 2) {
                    sendInvalidArgumentMessage("kick", channel, prefix);
                }
                Optional<IUser> userToKick = getUser(command[1], channel, guild);
                return;
            case "help":
                channel.sendMessage(
                        "\n" +
                        "*alias*: **" + prefix + "**\n" +
                        "**ping** \npong! \n" +
                        "**avatar <@user>** \nreturn a link to the user's avatar \n" +
                        "**setwelcome <message>** \nset a welcome message that the bot will PM new users when joining \n");
            default:
                sendInvalidArgumentMessage("invalidcommand", channel, prefix);
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
        }
    }

}
