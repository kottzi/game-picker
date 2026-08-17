package io.github.kottzi.gamepicker.steam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReleaseDateParser {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH);

    private ReleaseDateParser() {
    }

    public static LocalDate parse(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim(), FORMAT);
        } catch (Exception e) {
            return null;
        }
    }
}
