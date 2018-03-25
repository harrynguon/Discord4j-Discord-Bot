package sc.loot.util;

import sc.loot.main.Main;
import sc.loot.processor.CommandProcessor;
import sc.loot.processor.EventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;

/**
 * Schedules the weekly report to be submitted every week on Saturday.
 */
public class SCLootScheduler implements Runnable {

    /**
     * Creates the weekly report when the day is Saturday.
     */
    public static void weeklyReport() {
        // NZ Time
        LocalDate day = LocalDate.now(ZoneId.of("UTC+12"));
        System.out.println(day);
        System.out.println("The day is " + day.getDayOfWeek() +
                " and the day of the week number is: " +
                day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK));
        // 1 == Monday, 7 == Sunday
        if (day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK) == 6) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                System.out.println("I'm creating the weekly report now.");
                CommandProcessor.createWeeklyReport(Main.bot.get());
            }
        }

        System.out.println("The day of the month is: " + day.getDayOfMonth());
        System.out.println("The last day of this month is: " + day.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth());

    }

    /**
     * After four hours, the next message posted on #sc_loot will count towards another opening
     */
    public static void countSC() {
        EventListener.scOpenTracker = true;
    }

    @Override
    public void run() {}

}
