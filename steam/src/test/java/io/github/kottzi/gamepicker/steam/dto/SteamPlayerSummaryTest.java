package io.github.kottzi.gamepicker.steam.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteamPlayerSummaryTest {

    @Test
    void visibilityStateThree_isPublic() {
        SteamPlayerSummary summary = new SteamPlayerSummary("1", "Nick", "url", 3);
        assertTrue(summary.isProfilePublic());
    }

    @Test
    void visibilityStateOne_isPrivate() {
        SteamPlayerSummary summary = new SteamPlayerSummary("1", "Nick", "url", 1);
        assertFalse(summary.isProfilePublic());
    }

    @Test
    void visibilityStateTwo_friendsOnly_isNotPublic() {
        SteamPlayerSummary summary = new SteamPlayerSummary("1", "Nick", "url", 2);
        assertFalse(summary.isProfilePublic());
    }
}
