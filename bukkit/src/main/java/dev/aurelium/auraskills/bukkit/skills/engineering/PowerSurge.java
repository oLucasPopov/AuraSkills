package dev.aurelium.auraskills.bukkit.skills.engineering;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PowerSurge extends ReadiedManaAbility {

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> readyTicks = new ConcurrentHashMap<>();

    public PowerSurge(AuraSkills plugin) {
        super(plugin, ManaAbilities.POWER_SURGE, ManaAbilityMessage.POWER_SURGE_START, ManaAbilityMessage.POWER_SURGE_END,
                new String[]{"REDSTONE"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        active.add(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 0.8f);
    }

    // A right-click on a crafting table while readied activates the ability
    @EventHandler
    public void activationListener(PlayerInteractEvent event) {
        if (isDisabled()) return;
        if (event.isCancelled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CRAFTING_TABLE) return;

        Player player = event.getPlayer();
        if (!isHoldingMaterial(player)) return;
        if (failsChecks(player)) return;
        // The click that readied the ability must never activate it; handler
        // ordering between this method and onReady is JVM-unspecified
        Long readyTick = readyTicks.get(player.getUniqueId());
        if (readyTick != null && readyTick == player.getWorld().getGameTime()) return;
        if (checkActivation(player)) { // No-op unless the ability is readied
            readyTicks.remove(player.getUniqueId());
        }
    }

    @Override
    public void onReady(PlayerInteractEvent event) {
        boolean wasReady = plugin.getUser(event.getPlayer()).getManaAbilityData(manaAbility).isReady();
        super.onReady(event);
        if (!wasReady && plugin.getUser(event.getPlayer()).getManaAbilityData(manaAbility).isReady()) {
            readyTicks.put(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getGameTime());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void doubleOutput(CraftItemEvent event) {
        if (isDisabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !EngineeringAbilities.COMPONENTS.contains(result.getType())) return;
        if (ThreadLocalRandom.current().nextDouble() >= manaAbility.optionDouble("double_chance", 50.0) / 100) return;

        giveOrDrop(player, new ItemStack(result.getType(), result.getAmount()));
    }

    @Override
    public void onStop(Player player, User user) {
        active.remove(player.getUniqueId());
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

}
