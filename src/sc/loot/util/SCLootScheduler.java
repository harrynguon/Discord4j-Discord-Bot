package sc.loot.util;

import sc.loot.main.Main;
import sc.loot.processor.CommandProcessor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class SCLootScheduler implements Runnable {

    @Override
    public void run() {
        LocalDate day = LocalDate.now(ZoneId.of("UTC+12"));
        System.out.println(day);
        System.out.println(day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK));
        // 1 == Monday, 7 == Sunday
        if (day.getDayOfWeek().get(ChronoField.DAY_OF_WEEK) == 6) {
            if (Main.bot.isPresent() && Main.bot.get().isLoggedIn()) {
                CommandProcessor.createWeeklyReport(Main.bot.get());
            }
        }
    }

}
