package discord.api;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class CommandProcessor {

    public static void processCommand(IMessage message, String prefix) {
        // discord user sender
        IUser sender = message.getAuthor();
        // discord channel
        IChannel channel = message.getChannel();
        // discord server
        IGuild guild = message.getGuild();

        // process imessage into string array with args and remove the prefix
        String[] command = message.getContent().toLowerCase().replaceFirst(prefix, "").split(" ");

        if (command[0].equals("ping")) {
            channel.sendMessage("pong!");
        } else if (command[0].equals("avatar")) {
            if (command.length == 2) {
                command[1] = command[1].replaceAll("[<>@!]", "");
                for (char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
                    if (command[1].toUpperCase().indexOf(alphabet) >= 0) {
                        channel.sendMessage(command[1] + " Is not a valid user!");
                        return;
                    }
                }
                IUser user = guild.getUserByID(Long.parseLong(command[1]));
                channel.sendMessage(user.mention() + "'s avatar: " + user.getAvatarURL()) ;
            } else {
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "avatar <@user>]`");
            }
        } else if (command[0].equals("setwelcome")) {
            if (command.length > 1) {
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
                message.delete();
                return;
            } else {
                channel.sendMessage("Please enter valid arguments! `[" + prefix + "setwelcome <String[]:message>]`");
            }
        }
    }

}
