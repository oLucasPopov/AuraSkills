package dev.aurelium.auraskills.bukkit.skills.trading;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.api.util.NumberUtil;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.bukkit.util.ItemUtils;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradingAbilities extends BukkitAbilityImpl {

    private static final double MAX_TOTAL_DISCOUNT = 0.8;

    // merchant entity id -> original (undiscounted) recipes, snapshotted on first open of a session
    private final Map<UUID, List<MerchantRecipe>> originalRecipes = new ConcurrentHashMap<>();
    // merchant entity id -> rolled Charisma discount, rolled once per merchant per session
    private final Map<UUID, Double> rolledDiscounts = new ConcurrentHashMap<>();

    public TradingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SILVER_TONGUE, Abilities.MERCHANT, Abilities.CHARISMA, Abilities.MASTER_NEGOTIATOR, Abilities.GUILD_REPUTATION);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyDiscounts(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        // Custom merchants not backed by an entity are never discounted (matches TradingLeveler)
        if (!(inventory.getMerchant() instanceof Entity entity)) return;

        User user = plugin.getUser(player);
        // Charisma is rolled once per merchant per session so reopen-spamming cannot farm rerolls
        double discount = rolledDiscounts.computeIfAbsent(entity.getUniqueId(), k -> rollCharismaDiscount(player, user));

        if (isGrandBargainActive(player)) {
            discount += ManaAbilities.GRAND_BARGAIN.optionDouble("discount_percentage", 20.0);
        }

        if (discount <= 0) return;
        double finalDiscount = Math.min(MAX_TOTAL_DISCOUNT, discount / 100);

        Merchant merchant = inventory.getMerchant();
        // Always rebuild from the original snapshot so discounts never compound across reopens
        List<MerchantRecipe> baseline = originalRecipes.computeIfAbsent(entity.getUniqueId(), k -> deepCopyRecipes(merchant.getRecipes()));
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (MerchantRecipe original : baseline) {
            MerchantRecipe copy = copyRecipe(original);
            List<ItemStack> ingredients = new ArrayList<>();
            for (ItemStack ingredient : copy.getIngredients()) {
                ingredient.setAmount(Math.max(1, (int) Math.ceil(ingredient.getAmount() * (1 - finalDiscount))));
                ingredients.add(ingredient);
            }
            copy.setIngredients(ingredients);
            recipes.add(copy);
        }
        merchant.setRecipes(recipes);
    }

    @EventHandler
    public void restoreOriginalRecipes(InventoryCloseEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        if (!(inventory.getMerchant() instanceof Entity entity)) return;

        rolledDiscounts.remove(entity.getUniqueId());
        List<MerchantRecipe> baseline = originalRecipes.remove(entity.getUniqueId());
        if (baseline == null) return;

        // Restore original prices so the entity never keeps discounted recipes between sessions;
        // keep the live use counters so trades made this session still consume stock
        Merchant merchant = inventory.getMerchant();
        List<MerchantRecipe> restored = new ArrayList<>();
        for (int i = 0; i < baseline.size(); i++) {
            MerchantRecipe copy = copyRecipe(baseline.get(i));
            if (merchant.getRecipeCount() > i) {
                copy.setUses(Math.min(copy.getMaxUses(), merchant.getRecipe(i).getUses()));
            }
            restored.add(copy);
        }
        // Keep trades unlocked mid-session (e.g. villager level-up while the GUI was open)
        for (int i = baseline.size(); i < merchant.getRecipeCount(); i++) {
            restored.add(merchant.getRecipe(i));
        }
        merchant.setRecipes(restored);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTradeCompleted(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof MerchantInventory inventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        // Only reward clicks that actually take the trade result
        if (failsClickChecks(event)) return;
        MerchantRecipe recipe = inventory.getSelectedRecipe();
        if (recipe == null || recipe.getUses() >= recipe.getMaxUses()) return;

        User user = plugin.getUser(player);

        applySilverTongue(player, user, recipe);
        applyGuildReputation(player, user, result);
        applyMasterNegotiator(player, user, inventory);
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

    private void applyMasterNegotiator(Player player, User user, MerchantInventory inventory) {
        var ability = Abilities.MASTER_NEGOTIATOR;
        boolean grandBargain = isGrandBargainActive(player);
        if (!grandBargain) {
            if (isDisabled(ability) || failsChecks(player, ability)) return;
            if (user.getAbilityLevel(ability) <= 0) return;
            if (rand.nextDouble() >= Math.min(50.0, getValue(ability, user)) / 100) return;
        }
        // Roll back the use this click is about to add, one tick later. Re-resolve by index so a
        // recipe list replaced by applyDiscounts in the meantime cannot orphan the reference.
        Merchant merchant = inventory.getMerchant();
        int index = inventory.getSelectedRecipeIndex();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (merchant.getRecipeCount() > index) {
                MerchantRecipe current = merchant.getRecipe(index);
                current.setUses(Math.max(0, current.getUses() - 1));
            }
        });
    }

    private boolean isGrandBargainActive(Player player) {
        return plugin.getUser(player).getManaAbilityData(ManaAbilities.GRAND_BARGAIN).isActivated();
    }

    private double rollCharismaDiscount(Player player, User user) {
        var charisma = Abilities.CHARISMA;
        if (isDisabled(charisma) || failsChecks(player, charisma)) return 0.0;
        if (user.getAbilityLevel(charisma) <= 0) return 0.0;
        if (rand.nextDouble() >= getValue(charisma, user) / 100) return 0.0;
        double min = charisma.optionDouble("min_discount", 5.0);
        double max = charisma.optionDouble("max_discount", 15.0);
        return min + rand.nextDouble() * (max - min);
    }

    @Override
    public String replaceDescPlaceholders(String input, Ability ability, User user) {
        if (ability.equals(Abilities.SILVER_TONGUE)) {
            return TextUtil.replace(input, "{refund_percentage}",
                    NumberUtil.format1(Abilities.SILVER_TONGUE.optionDouble("refund_percentage", 10.0)));
        }
        return input;
    }

    private static List<MerchantRecipe> deepCopyRecipes(List<MerchantRecipe> recipes) {
        List<MerchantRecipe> copies = new ArrayList<>();
        for (MerchantRecipe recipe : recipes) {
            copies.add(copyRecipe(recipe));
        }
        return copies;
    }

    private static MerchantRecipe copyRecipe(MerchantRecipe original) {
        MerchantRecipe copy = new MerchantRecipe(original.getResult().clone(), original.getUses(), original.getMaxUses(),
                original.hasExperienceReward(), original.getVillagerExperience(), original.getPriceMultiplier());
        List<ItemStack> ingredients = new ArrayList<>();
        for (ItemStack ingredient : original.getIngredients()) {
            ingredients.add(ingredient.clone());
        }
        copy.setIngredients(ingredients);
        copy.setDemand(original.getDemand());
        copy.setSpecialPrice(original.getSpecialPrice());
        return copy;
    }

    // Mirrors SourceLeveler.failsClickChecks, which is not reachable from BukkitAbilityImpl
    private static boolean failsClickChecks(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return true;

        ClickType click = event.getClick();
        // Only allow right and left clicks if inventory full
        if (click != ClickType.LEFT && click != ClickType.RIGHT && ItemUtils.isInventoryFull(player)) return true;
        if (event.getResult() != Event.Result.ALLOW) return true; // Make sure the click was successful
        if (player.getItemOnCursor().getType() != Material.AIR) return true; // Make sure cursor is empty
        InventoryAction action = event.getAction();
        // Only give if item was picked up
        return action != InventoryAction.PICKUP_ALL && action != InventoryAction.MOVE_TO_OTHER_INVENTORY
                && action != InventoryAction.PICKUP_HALF && action != InventoryAction.DROP_ALL_SLOT
                && action != InventoryAction.DROP_ONE_SLOT && action != InventoryAction.HOTBAR_SWAP;
    }
}
