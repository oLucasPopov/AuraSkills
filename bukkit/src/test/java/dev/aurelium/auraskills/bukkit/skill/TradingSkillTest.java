package dev.aurelium.auraskills.bukkit.skill;

import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.util.TestSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradingSkillTest {

    private static AuraSkills plugin;

    @BeforeAll
    static void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(AuraSkills.class, TestSession.create());
        server.getScheduler().performOneTick();
    }

    @AfterAll
    static void unload() {
        MockBukkit.unmock();
    }

    @Test
    void testTradingSkillLoads() {
        assertTrue(Skills.TRADING.isEnabled());
        assertTrue(Skills.TRADING.getAbilities().size() == 5);
    }

    @Test
    void testTradingSourcesLoad() {
        List<SkillSource<TradingXpSource>> sources = plugin.getSkillManager().getSourcesOfType(TradingXpSource.class);
        assertFalse(sources.isEmpty());
        assertTrue(sources.stream().allMatch(s -> s.source().getXp() > 0));
        for (TradingXpSource.TradingTrigger trigger : TradingXpSource.TradingTrigger.values()) {
            assertTrue(sources.stream().anyMatch(s -> s.source().getTrigger() == trigger), "Missing trigger " + trigger);
        }
    }
}
