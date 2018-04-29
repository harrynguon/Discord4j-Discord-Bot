package sc.loot.util;

import sc.loot.main.Main;
import sc.loot.processor.CommandProcessor;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Schedules the weekly report to be submitted every week on Saturday.
 */
public class SCLootScheduler implements Runnable {

    /**
     * Creates the weekly report when the day is Saturday.
     */
    public static void weeklyReport() {
        // UTC+12 Time. Cannot do NZ time because of daylight savings inconsistencies

        final Instant currentTime = Instant.now().plus(12, ChronoUnit.HOURS);
        final LocalDate day = LocalDateTime.ofInstant(currentTime, ZoneOffset.ofHours(0))
                .toLocalDate();

        DayOfWeek currentDayOfWeek = day.getDayOfWeek();

        int currentDayOfMonth = day.getDayOfMonth();
        int lastDayOfMonth = day.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();

        System.out.println("--- Data printed from the SCLootScheduler.class ---");

        System.out.println("The current time is: " + currentTime);
        System.out.println(
                "The day is " +
                currentDayOfWeek +
                " and the day of the week number is: " +
                currentDayOfWeek.get(ChronoField.DAY_OF_WEEK)
        );
        System.out.println("The day number of the month is: " + currentDayOfMonth);
        System.out.println("The last day of this month is: " + lastDayOfMonth);

        // 7 == Sunday
        if (currentDayOfWeek.get(ChronoField.DAY_OF_WEEK) == 7) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                System.out.println("I'm creating the weekly report now.");
                CommandProcessor.createReport(Main.bot.get(), Constants.WEEKLY_REPORT_CHANNEL_ID,
                        Constants.WEEKLY);
            }
        }

        // checks if the day is the last day of the month
        if (currentDayOfMonth == lastDayOfMonth) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                System.out.println("I'm creating the monthly report now.");
                CommandProcessor.createReport(Main.bot.get(), Constants
                        .MONTHLY_REPORT_CHANNEL_ID, Constants.MONTHLY);
            }
        }

        System.out.println("------");
    }

    @Override
    public void run() {}

}
