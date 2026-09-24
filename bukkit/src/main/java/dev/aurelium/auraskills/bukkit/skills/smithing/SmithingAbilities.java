package dev.aurelium.auraskills.bukkit.skills.smithing;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class SmithingAbilities extends BukkitAbilityImpl {

    private static final int SMITHING_ADDITION_SLOT = 2;

    public SmithingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.EFFICIENT_SMELTING, Abilities.SMITH, Abilities.MASTER_CRAFTED, Abilities.RECYCLER, Abilities.FORGE_MASTERY);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void efficientSmelting(FurnaceExtractEvent event) {
        var ability = Abilities.EFFICIENT_SMELTING;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        boolean guaranteedDouble = isOverdriveActive(player);
        if (!guaranteedDouble && rand.nextDouble() >= getValue(ability, user) / 100) return;

        ItemStack extra = new ItemStack(event.getItemType(), event.getItemAmount());
        giveOrDrop(player, extra);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void masterCrafted(CraftItemEvent event) {
        var ability = Abilities.MASTER_CRAFTED;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !isEquipment(result.getType())) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        int abilityLevel = user.getAbilityLevel(ability);
        if (abilityLevel <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        int enchantLevel = Math.min(3, 1 + abilityLevel / 5);
        result.addUnsafeEnchantment(Enchantment.UNBREAKING, enchantLevel);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void recycler(SmithItemEvent event) {
        var ability = Abilities.RECYCLER;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        SmithingInventory inventory = event.getInventory();
        ItemStack addition = inventory.getItem(SMITHING_ADDITION_SLOT);
        if (addition == null || addition.getType().isAir()) return;

        giveOrDrop(player, new ItemStack(addition.getType(), 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void forgeMastery(PrepareAnvilEvent event) {
        var ability = Abilities.FORGE_MASTERY;
        if (isDisabled(ability)) return;

        AnvilInventory inventory = event.getInventory();
        int cost = inventory.getRepairCost();
        if (cost <= 0) return;
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        double reduction = Math.min(40.0, getValue(ability, user)) / 100;
        inventory.setRepairCost((int) Math.max(0, Math.floor(cost * (1 - reduction))));
    }

    private boolean isOverdriveActive(Player player) {
        return plugin.getUser(player).getManaAbilityData(ManaAbilities.FORGE_OVERDRIVE).isActivated();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public static boolean isEquipment(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.endsWith("_SPEAR") || name.endsWith("_MACE") || material == Material.SHIELD
                || material == Material.BOW || material == Material.CROSSBOW || material == Material.TRIDENT;
    }
}
