package dev.aurelium.auraskills.bukkit.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingLevelerTest {

    @Test
    void positionCacheBlocksRepeatXpWithinWindow() {
        BuildingLeveler.PositionCache cache = new BuildingLeveler.PositionCache(60_000);
        long pos = 123456789L;
        assertTrue(cache.tryGrant(pos, 1000));
        assertFalse(cache.tryGrant(pos, 2000));
        assertTrue(cache.tryGrant(pos, 1000 + 60_001));
    }
}
