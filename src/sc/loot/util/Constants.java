package sc.loot.util;

public class Constants {

    /** On user join */
    public static final String WELCOME_MESSAGE = "Welcome to the SC Loot Discord server! Please read " +
            "the #REPLACE_THIS channel before submitting any messages. Have a good one :)";

    /** Force JVM to create new String reference */
    public static final String PREFIX = new String("!");

    /** Role in the discord server which is required for access to the bot commands */
    public static final String BOT_AUTH_NAME = "Bot Authorisation";

    /** Name of the sc_loot channel */
    public static final String SC_LOOT_CHANNEL_NAME = "sc_loot";

    /** Name of the sc_loot_bot channel used to sending logs */
    public static final String SC_LOOT_BOT_CHANNEL_NAME = "sc_loot_bot";

    /** Called when the user sends the !help command */
    public static final String HELP_MESSAGE =
            "*alias*: **" + Constants.PREFIX + "**\n" +

            "**setwelcome <message>** \nset a welcome message that the bot will PM new users when joining \n" +

            "**warn <@user> <message>** \ntells the bot to send a PM to the user with the warning message \n" +

            "**ban <@user> <reason>** \nthe bot will create a post on #board_of_punishments with the following reason, " +
            "PM the user informing they have been banned for the following reason, and then finally ban the user from the server. \n" +

            "**weeklyreport** \ncreates a weekly report based on the sc_loot and submits it to #weekly_report";

}
