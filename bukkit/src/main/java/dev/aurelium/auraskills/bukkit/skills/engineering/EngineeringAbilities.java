package dev.aurelium.auraskills.bukkit.skills.engineering;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EngineeringAbilities extends BukkitAbilityImpl {

    // Union of the craft/place items in sources/engineering.yml that power_surge can double
    public static final Set<Material> COMPONENTS = Set.of(
            Material.PISTON, Material.STICKY_PISTON, Material.OBSERVER, Material.DISPENSER,
            Material.DROPPER, Material.REPEATER, Material.COMPARATOR, Material.HOPPER,
            Material.REDSTONE_LAMP, Material.TARGET, Material.CRAFTER, Material.DAYLIGHT_DETECTOR,
            Material.LIGHTNING_ROD, Material.NOTE_BLOCK, Material.JUKEBOX, Material.RAIL,
            Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL, Material.MINECART,
            Material.CHEST_MINECART, Material.FURNACE_MINECART, Material.HOPPER_MINECART, Material.TNT_MINECART,
            Material.TNT, Material.REDSTONE_TORCH, Material.LEVER, Material.TRIPWIRE_HOOK,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Material.IRON_DOOR, Material.IRON_TRAPDOOR);

    // Cart id -> max speed before the railroad_baron boost, for restoration on exit
    private final Map<UUID, Double> boostedCarts = new ConcurrentHashMap<>();

    public EngineeringAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SPARE_PARTS, Abilities.ENGINEER, Abilities.EFFICIENT_CRAFTING,
                Abilities.RAILROAD_BARON, Abilities.TINKERERS_LUCK);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void spareParts(BlockBreakEvent event) {
        var ability = Abilities.SPARE_PARTS;
        if (isDisabled(ability)) return;

        Material type = event.getBlock().getType();
        if (!COMPONENTS.contains(type)) return;
        if (!type.isItem()) return;

        Player player = event.getPlayer();
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(type, 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void efficientCrafting(CraftItemEvent event) {
        var ability = Abilities.EFFICIENT_CRAFTING;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !COMPONENTS.contains(result.getType())) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        List<ItemStack> ingredients = new ArrayList<>();
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                ingredients.add(item);
            }
        }
        if (ingredients.isEmpty()) return;

        ItemStack refund = ingredients.get(rand.nextInt(ingredients.size()));
        giveOrDrop(player, new ItemStack(refund.getType(), 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void tinkerersLuck(CraftItemEvent event) {
        var ability = Abilities.TINKERERS_LUCK;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !COMPONENTS.contains(result.getType())) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        giveOrDrop(player, new ItemStack(result.getType(), result.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void railroadBaronEnter(VehicleEnterEvent event) {
        var ability = Abilities.RAILROAD_BARON;
        if (isDisabled(ability)) return;
        if (!(event.getVehicle() instanceof Minecart cart)) return;
        if (!(event.getEntered() instanceof Player player)) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;

        double base = cart.getMaxSpeed();
        boostedCarts.put(cart.getUniqueId(), base);
        cart.setMaxSpeed(base * (1 + getValue(ability, user) / 100));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void railroadBaronExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Minecart cart)) return;

        Double base = boostedCarts.remove(cart.getUniqueId());
        if (base != null) {
            cart.setMaxSpeed(base);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void railroadBaronDestroy(VehicleDestroyEvent event) {
        boostedCarts.remove(event.getVehicle().getUniqueId());
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

}
