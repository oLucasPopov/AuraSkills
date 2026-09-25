package dev.aurelium.auraskills.bukkit.skills.building;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import dev.aurelium.auraskills.api.event.user.UserLoadEvent;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.bukkit.util.VersionUtils;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BuildingAbilities extends BukkitAbilityImpl {

    private static final UUID REACH_MODIFIER_ID = UUID.fromString("7e2f1c3a-9b4d-4e5f-8a6c-1d2e3f4a5b6c");
    private static final String LEGACY_REACH_MODIFIER_NAME = "auraskills.long_reach";
    private static final String REACH_MODIFIER_KEY = "long_reach";
    private static final int RECLAIMED_POSITIONS_CAP = 50000;

    private final Map<String, UUID> placedBlocks = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
            return size() > RECLAIMED_POSITIONS_CAP;
        }
    });

    public BuildingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SPARE_MATERIALS, Abilities.BUILDER, Abilities.LONG_REACH, Abilities.RECLAIMER, Abilities.STEADY_HANDS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void spareMaterials(BlockPlaceEvent event) {
        var ability = Abilities.SPARE_MATERIALS;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        Material type = event.getItemInHand().getType();
        plugin.getScheduler().scheduleSync(() -> giveOrDrop(player, new ItemStack(type, 1)), 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void reclaimerRecord(BlockPlaceEvent event) {
        var ability = Abilities.RECLAIMER;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (failsChecks(player, ability)) return;
        if (plugin.getUser(player).getAbilityLevel(ability) <= 0) return;

        placedBlocks.put(positionKey(event.getBlock()), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void reclaimerBreak(BlockBreakEvent event) {
        var ability = Abilities.RECLAIMER;
        if (isDisabled(ability)) return;

        Block block = event.getBlock();
        UUID placer = placedBlocks.remove(positionKey(block));
        if (placer == null) return;

        Player player = event.getPlayer();
        if (!placer.equals(player.getUniqueId())) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        Material type = block.getType();
        if (!type.isItem()) return;
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(type, 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void steadyHands(EntityDamageEvent event) {
        var ability = Abilities.STEADY_HANDS;
        if (isDisabled(ability)) return;
        if (event.getCause() != DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getInventory().getItemInMainHand().getType().isBlock()) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;

        event.setDamage(event.getDamage() * (1 - getValue(ability, user) / 100));
    }

    @EventHandler
    public void longReachJoin(UserLoadEvent event) {
        applyLongReach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void longReachLevelUp(SkillLevelUpEvent event) {
        if (!event.getSkill().equals(Skills.BUILDING)) return;
        applyLongReach(event.getPlayer());
    }

    @SuppressWarnings("removal")
    private void applyLongReach(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (attribute == null) return;
        // Remove existing modifier if exists
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (isLongReachModifier(modifier)) {
                attribute.removeModifier(modifier);
            }
        }
        if (isDisabled(Abilities.LONG_REACH)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(Abilities.LONG_REACH) <= 0) return;

        double value = getValue(Abilities.LONG_REACH, user);
        if (VersionUtils.isAtLeastVersion(21)) {
            NamespacedKey key = new NamespacedKey(plugin, REACH_MODIFIER_KEY);
            attribute.addModifier(new AttributeModifier(key, value, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        } else {
            attribute.addModifier(new AttributeModifier(REACH_MODIFIER_ID, LEGACY_REACH_MODIFIER_NAME, value, Operation.ADD_NUMBER));
        }
    }

    private boolean isLongReachModifier(AttributeModifier am) {
        if (am.getName().equals(LEGACY_REACH_MODIFIER_NAME)) {
            return true;
        }
        if (VersionUtils.isAtLeastVersion(21)) {
            String namespace = am.getKey().getNamespace();
            String key = am.getKey().getKey();
            if (key.equals(REACH_MODIFIER_ID.toString())) {
                // When migrating to 1.21, old attributes are converted to the NamespacedKey minecraft:REACH_MODIFIER_ID
                return true;
            } else {
                final String attributeNamespace = "auraskills";
                return namespace.equals(attributeNamespace) && key.equals(REACH_MODIFIER_KEY);
            }
        }
        return false;
    }

    private String positionKey(Block block) {
        return block.getWorld().getUID() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

}
