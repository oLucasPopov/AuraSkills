package dev.aurelium.auraskills.bukkit.skills.trading;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class TradingAbilities extends BukkitAbilityImpl {

    private static final double MAX_TOTAL_DISCOUNT = 0.8;

    public TradingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SILVER_TONGUE, Abilities.MERCHANT, Abilities.CHARISMA, Abilities.MASTER_NEGOTIATOR, Abilities.GUILD_REPUTATION);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyDiscounts(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        User user = plugin.getUser(player);
        double discount = 0;

        var charisma = Abilities.CHARISMA;
        if (!isDisabled(charisma) && !failsChecks(player, charisma) && user.getAbilityLevel(charisma) > 0) {
            if (rand.nextDouble() < getValue(charisma, user) / 100) {
                double min = charisma.optionDouble("min_discount", 5.0);
                double max = charisma.optionDouble("max_discount", 15.0);
                discount += min + rand.nextDouble() * (max - min);
            }
        }

        if (isGrandBargainActive(player)) {
            discount += ManaAbilities.GRAND_BARGAIN.optionDouble("discount_percentage", 20.0);
        }

        if (discount <= 0) return;
        double finalDiscount = Math.min(MAX_TOTAL_DISCOUNT, discount / 100);

        var merchant = inventory.getMerchant();
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < merchant.getRecipeCount(); i++) {
            MerchantRecipe original = merchant.getRecipe(i);
            MerchantRecipe copy = new MerchantRecipe(original.getResult(), original.getUses(), original.getMaxUses(),
                    original.hasExperienceReward(), original.getVillagerExperience(), original.getPriceMultiplier());
            List<ItemStack> ingredients = new ArrayList<>();
            for (ItemStack ingredient : original.getIngredients()) {
                ItemStack adjusted = ingredient.clone();
                adjusted.setAmount(Math.max(1, (int) Math.ceil(adjusted.getAmount() * (1 - finalDiscount))));
                ingredients.add(adjusted);
            }
            copy.setIngredients(ingredients);
            recipes.add(copy);
        }
        merchant.setRecipes(recipes);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTradeCompleted(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof MerchantInventory inventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        MerchantRecipe recipe = inventory.getSelectedRecipe();
        if (recipe == null || recipe.getUses() >= recipe.getMaxUses()) return;

        User user = plugin.getUser(player);

        applySilverTongue(player, user, recipe);
        applyGuildReputation(player, user, result);
        applyMasterNegotiator(player, user, recipe);
    }

    private void applySilverTongue(Player player, User user, MerchantRecipe recipe) {
        var ability = Abilities.SILVER_TONGUE;
        if (isDisabled(ability) || failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        int emeraldsSpent = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient.getType() == Material.EMERALD) {
                emeraldsSpent += ingredient.getAmount();
            }
        }
        if (emeraldsSpent == 0) return;

        int refund = (int) Math.ceil(emeraldsSpent * ability.optionDouble("refund_percentage", 10.0) / 100);
        if (refund <= 0) return;
        player.getInventory().addItem(new ItemStack(Material.EMERALD, refund)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void applyGuildReputation(Player player, User user, ItemStack result) {
        var ability = Abilities.GUILD_REPUTATION;
        if (isDisabled(ability) || failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= Math.min(20.0, getValue(ability, user)) / 100) return;

        player.getInventory().addItem(result.clone()).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void applyMasterNegotiator(Player player, User user, MerchantRecipe recipe) {
        var ability = Abilities.MASTER_NEGOTIATOR;
        boolean grandBargain = isGrandBargainActive(player);
        if (!grandBargain) {
            if (isDisabled(ability) || failsChecks(player, ability)) return;
            if (user.getAbilityLevel(ability) <= 0) return;
            if (rand.nextDouble() >= Math.min(50.0, getValue(ability, user)) / 100) return;
        }
        // Roll back the use this click is about to add, one tick later
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            recipe.setUses(Math.max(0, recipe.getUses() - 1));
        });
    }

    private boolean isGrandBargainActive(Player player) {
        return plugin.getUser(player).getManaAbilityData(ManaAbilities.GRAND_BARGAIN).isActivated();
    }
}
