package dev.aurelium.auraskills.bukkit.skill;

import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.CraftingXpSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skills.smithing.SmithingAbilities;
import dev.aurelium.auraskills.common.util.TestSession;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmithingSkillTest {

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
    void testSmithingSkillLoads() {
        assertTrue(Skills.SMITHING.isEnabled());
        assertTrue(Skills.SMITHING.getAbilities().size() == 5);
    }

    @Test
    void testSmithingSourcesLoad() {
        assertFalse(plugin.getSkillManager().getSourcesOfType(SmithingXpSource.class).isEmpty());
        assertFalse(plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class).isEmpty());
        List<SkillSource<CraftingXpSource>> crafting = plugin.getSkillManager().getSourcesOfType(CraftingXpSource.class);
        assertFalse(crafting.isEmpty());
        // Every loaded source must grant XP
        assertTrue(crafting.stream().allMatch(s -> s.source().getXp() > 0));
    }

    @Test
    void testIsEquipment() {
        assertTrue(SmithingAbilities.isEquipment(Material.DIAMOND_SWORD));
        assertTrue(SmithingAbilities.isEquipment(Material.IRON_CHESTPLATE));
        assertTrue(SmithingAbilities.isEquipment(Material.NETHERITE_PICKAXE));
        assertTrue(SmithingAbilities.isEquipment(Material.SHIELD));
        assertTrue(SmithingAbilities.isEquipment(Material.TRIDENT));
        assertFalse(SmithingAbilities.isEquipment(Material.STICK));
        assertFalse(SmithingAbilities.isEquipment(Material.DIAMOND));
        assertFalse(SmithingAbilities.isEquipment(Material.FURNACE));
    }
}
