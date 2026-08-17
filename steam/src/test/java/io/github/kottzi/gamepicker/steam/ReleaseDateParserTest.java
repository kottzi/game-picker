package io.github.kottzi.gamepicker.steam;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReleaseDateParserTest {

    @Test
    void parsesNormalSteamDateFormat() {
        LocalDate result = ReleaseDateParser.parse("16 Apr, 2010");
        assertEquals(LocalDate.of(2010, Month.APRIL, 16), result);
    }

    @Test
    void comingSoon_returnsNull() {
        assertNull(ReleaseDateParser.parse("Coming soon"));
    }

    @Test
    void blankOrNull_returnsNull() {
        assertNull(ReleaseDateParser.parse(""));
        assertNull(ReleaseDateParser.parse(null));
        assertNull(ReleaseDateParser.parse("   "));
    }

    @Test
    void garbageInput_returnsNullInsteadOfThrowing() {
        assertNull(ReleaseDateParser.parse("не дата вообще"));
    }
}
