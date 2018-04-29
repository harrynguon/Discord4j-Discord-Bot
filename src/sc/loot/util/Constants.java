package sc.loot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {

    /** ID of the SC Loot Server */
    public static final long SC_LOOT_GUILD_ID = 249099808910999554L;

    /** ID of the Weekly Report channel for sending the report to */
    public static final long WEEKLY_REPORT_CHANNEL_ID = 414243989009465344L;

    /** ID of the Monthly Report channel for sending the report to */
    public static final long MONTHLY_REPORT_CHANNEL_ID = 427329434971013132L;

    /** Used for testing out commands, etc. */
    public static final long TEST_CHANNEL_ID = 413975567931670529L;

    /** Ensures correct spelling of these globally-used variables */
    public static final String WEEKLY = "Weekly";
    public static final String MONTHLY = "Monthly";

    /** ID of the Read This First channel */
    public static final long READ_THIS_FIRST_CHANNEL_ID = 249099808910999554L;

    /** Discord Member join message */
    public static final String WELCOME_MESSAGE = "Welcome to the SC Loot Discord server! Please read " +
            "the #REPLACE_THIS channel before posting any messages.";

    /** Alias for using the bot commands */
    public static final String PREFIX = new String("!");

    /** Role in the discord server which is required for access to the bot commands */
    public static final long BOT_AUTH_ROLE_ID= 412858360447369225L;

    /** Name of the sc_loot channel */
    public static final long SC_LOOT_CHANNEL_ID = 249104816171646978L;

    /** Name of the sc_loot_bot channel used to sending logs */
    public static final long SC_LOOT_BOT_CHANNEL_ID = 412865060818386956L;

    public static final long SC_LOOT_BOT_ROLE_ID = 425576014744387594L;

    /** Called when the user sends the !help command */
    public static final String HELP_MESSAGE =
            "*alias*: **" + Constants.PREFIX + "**\n" +

            "**setwelcome <message>** \nplease message Rebuked \n" +

            "**warn <@user> <message>** \ntells the bot to send a PM to the user with the warning message \n" +

            "**ban <@user> <reason>** \nthe bot will create a post on #board_of_punishments with the following reason, " +
            "PM the user informing they have been banned for the following reason, and then finally ban the user from the server. \n" +

            "**weeklyreport** \n**(DEPRECATED)** creates a weekly report based on the sc_loot and submits it to #weekly_report \n" +

            "**changestatus <newStatus>** \nChanges the bot's online status \n" +

            "**getroleid <role name>** \nGets the role ID for the role name";

    /** Array of all the items for the weekly report */
    public static final String[] ITEMS = {
            "aeonics", "overloaded", "locust", "frenzied", "quaestor", "stompers", "delites",
            "armites", "calites", "bellites", "blockers", "bombard", "emites", "volta", "motley",
            "firites", "kunai", "fireform", "scorpion", "velites", "hydra", "salites", "mimic",
            "blink", "ultrites", "hyrst", "jerkin", "doublebandos", "vitreous", "antiphon",
            "frag", "direfall", "macron", "aerolith", "mantlet", "valenki", "aesc", "exites",
            "sledge", "furious", "tacent", "platin", "talons", "trackers", "kyeser", "sabatons",
            "woven"
    };

    public static final List<String> IMPLANTS = new ArrayList<>(Arrays.asList(
            "delites", "armites", "calites", "bellites",
            "emites", "firites", "velites", "salites", "ultrites")
    );

    /**
     * These are used for new users joining the server.
     */
    public static final long NEW_USER_ROLE_ID = 427229154006794260L;

    public static final String MY_IGN_PREFIX = "My IGN is";

    /** User PMs */
    public static final long SC_LOOT_LOG_ID = 427234922458251264L;

    public static final long AUTHENTICATION_CHANNEL_ID = 439257873936023579L;

    public static final long AUTHENTICATION_LOG_CHANNEL_ID = 439287499412602881L;

    public static final long MEMBER_ROLE_ID = 439258076034498583L;

    /** Link to the all time leaderboards */
    public static String ALL_TIME_LEADERBOARDS_WEBSITE = "https://wrya.net/tmp/sbleaderboards/all-time.txt";
}
