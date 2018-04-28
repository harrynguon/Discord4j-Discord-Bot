package sc.loot.util;

import sc.loot.main.Main;
import sc.loot.processor.CommandProcessor;

import java.time.*;
import java.time.temporal.ChronoField;
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

        final Instant currentTime = Instant.now(Clock.system(ZoneId.of("UTC+12")));
        final LocalDate day = LocalDateTime.ofInstant(currentTime, ZoneOffset.ofHours(12))
                .toLocalDate();

        System.out.println("--- Data printed from the SCLootScheduler.class ---");

        System.out.println("The day is " + day.getDayOfWeek() +
                " and the day of the week number is: " +
                day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK));

        System.out.println("The day number of the month is: " + day.getDayOfMonth());
        System.out.println("The last day of this month is: " + day.with(
                TemporalAdjusters.lastDayOfMonth()).getDayOfMonth()
        );

        // 1 == Monday, 7 == Sunday
        if (day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK) == 6) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                System.out.println("I'm creating the weekly report now.");
                CommandProcessor.createReport(Main.bot.get(), Constants.WEEKLY);
            }
        }

        // checks if the day is the last day of the month
        if (day.getDayOfMonth() == day.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth()) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                System.out.println("I'm creating the monthly report now.");
                CommandProcessor.createReport(Main.bot.get(), Constants.MONTHLY);
            }
        }

        System.out.println("------");
    }

    @Override
    public void run() {}

}
