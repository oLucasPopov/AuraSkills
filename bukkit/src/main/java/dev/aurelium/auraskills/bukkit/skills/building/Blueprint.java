package dev.aurelium.auraskills.bukkit.skills.building;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.bukkit.util.VersionUtils;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Blueprint extends ReadiedManaAbility {

    private static final UUID MODIFIER_ID = UUID.fromString("8f3a2d4b-1c5e-4f6a-9b7d-2e3f4a5b6c7d");
    private static final String LEGACY_MODIFIER_NAME = "auraskills_blueprint";
    private static final String MODIFIER_KEY = "blueprint_mana_ability";
    private static final double REACH_BOOST = 1.5;
    private static final double REFUND_CHANCE = 0.15;

    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();

    public Blueprint(AuraSkills plugin) {
        super(plugin, ManaAbilities.BLUEPRINT, ManaAbilityMessage.BLUEPRINT_START, ManaAbilityMessage.BLUEPRINT_END,
                new String[]{"BRICK"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    @SuppressWarnings("removal")
    public void onActivate(Player player, User user) {
        activePlayers.add(player.getUniqueId());
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (attribute != null) {
            removeBlueprintModifier(attribute);
            if (VersionUtils.isAtLeastVersion(21)) {
                NamespacedKey key = new NamespacedKey(plugin, MODIFIER_KEY);
                attribute.addModifier(new AttributeModifier(key, REACH_BOOST, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            } else {
                attribute.addModifier(new AttributeModifier(MODIFIER_ID, LEGACY_MODIFIER_NAME, REACH_BOOST, Operation.ADD_NUMBER));
            }
        }
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
    }

    @Override
    public void onStop(Player player, User user) {
        activePlayers.remove(player.getUniqueId());
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (attribute == null) return;
        removeBlueprintModifier(attribute);
    }

    // Placing a block while readied activates the ability; while activated each
    // placed block has a chance to be refunded
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blueprintPlace(BlockPlaceEvent event) {
        if (isDisabled()) return;

        Player player = event.getPlayer();
        if (!activePlayers.contains(player.getUniqueId())) {
            if (!failsChecks(player) && isReady(plugin.getUser(player))) {
                checkActivation(player);
            }
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= REFUND_CHANCE) return;

        Material type = event.getItemInHand().getType();
        if (!type.isItem()) return;
        plugin.getScheduler().scheduleSync(() -> giveOrDrop(player, new ItemStack(type, 1)), 50, TimeUnit.MILLISECONDS);
    }

    private void removeBlueprintModifier(AttributeInstance attribute) {
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (isBlueprintModifier(modifier)) {
                attribute.removeModifier(modifier);
            }
        }
    }

    private boolean isBlueprintModifier(AttributeModifier am) {
        if (am.getName().equals(LEGACY_MODIFIER_NAME)) {
            return true;
        }
        if (VersionUtils.isAtLeastVersion(21)) {
            String namespace = am.getKey().getNamespace();
            String key = am.getKey().getKey();
            if (key.equals(MODIFIER_ID.toString())) {
                // When migrating to 1.21, old attributes are converted to the NamespacedKey minecraft:MODIFIER_ID
                return true;
            } else {
                final String attributeNamespace = "auraskills";
                return namespace.equals(attributeNamespace) && key.equals(MODIFIER_KEY);
            }
        }
        return false;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

}
