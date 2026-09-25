package dev.aurelium.auraskills.bukkit.skills.culinary;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CulinaryAbilities extends BukkitAbilityImpl {

    public static final NamespacedKey CHEF_KEY = new NamespacedKey("auraskills", "chef");

    public static final Set<Material> SMELT_FOODS = Set.of(
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD,
            Material.COOKED_SALMON, Material.BAKED_POTATO, Material.DRIED_KELP);

    public static final Set<Material> CRAFTED_FOODS = Set.of(
            Material.BREAD, Material.COOKIE, Material.MUSHROOM_STEW, Material.BEETROOT_SOUP,
            Material.SUSPICIOUS_STEW, Material.PUMPKIN_PIE, Material.RABBIT_STEW,
            Material.CAKE, Material.HONEY_BOTTLE, Material.GOLDEN_CARROT, Material.GOLDEN_APPLE);

    // Vanilla nutrition values; cake is intentionally absent (it is placed, not eaten)
    private static final Map<Material, Integer> NUTRITION = Map.ofEntries(
            Map.entry(Material.COOKED_BEEF, 8),
            Map.entry(Material.COOKED_PORKCHOP, 8),
            Map.entry(Material.COOKED_CHICKEN, 6),
            Map.entry(Material.COOKED_MUTTON, 6),
            Map.entry(Material.COOKED_RABBIT, 5),
            Map.entry(Material.COOKED_COD, 5),
            Map.entry(Material.COOKED_SALMON, 6),
            Map.entry(Material.BAKED_POTATO, 5),
            Map.entry(Material.DRIED_KELP, 1),
            Map.entry(Material.BREAD, 5),
            Map.entry(Material.COOKIE, 2),
            Map.entry(Material.MUSHROOM_STEW, 6),
            Map.entry(Material.BEETROOT_SOUP, 6),
            Map.entry(Material.SUSPICIOUS_STEW, 6),
            Map.entry(Material.PUMPKIN_PIE, 8),
            Map.entry(Material.RABBIT_STEW, 10),
            Map.entry(Material.HONEY_BOTTLE, 6),
            Map.entry(Material.GOLDEN_CARROT, 6),
            Map.entry(Material.GOLDEN_APPLE, 4));

    public CulinaryAbilities(AuraSkills plugin) {
        super(plugin, Abilities.EXTRA_SERVINGS, Abilities.CHEF, Abilities.HEARTY_MEALS, Abilities.GOURMET, Abilities.WELL_FED);
    }

    // Smithing's efficient_smelting can double the same extraction; both firing is intentional
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void extraServings(FurnaceExtractEvent event) {
        var ability = Abilities.EXTRA_SERVINGS;
        if (isDisabled(ability)) return;
        if (!SMELT_FOODS.contains(event.getItemType())) return;

        Player player = event.getPlayer();
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        ItemStack extra = new ItemStack(event.getItemType(), event.getItemAmount());
        tagChef(extra, player);
        giveOrDrop(player, extra);
    }

    // The extracted stack cannot be edited directly, so matching untagged stacks are
    // tagged one tick later; this may also tag identical food already being carried
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void heartyMealsSmelt(FurnaceExtractEvent event) {
        var ability = Abilities.HEARTY_MEALS;
        if (isDisabled(ability)) return;
        if (!SMELT_FOODS.contains(event.getItemType())) return;

        Player player = event.getPlayer();
        if (failsChecks(player, ability)) return;
        if (plugin.getUser(player).getAbilityLevel(ability) <= 0) return;

        Material type = event.getItemType();
        plugin.getScheduler().scheduleSync(() -> {
            if (!player.isOnline()) return;
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && stack.getType() == type && !isTagged(stack)) {
                    tagChef(stack, player);
                }
            }
        }, 50, TimeUnit.MILLISECONDS);
    }

    // For shift-click batches only the result stack visible in the event carries the tag
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void heartyMealsCraft(CraftItemEvent event) {
        var ability = Abilities.HEARTY_MEALS;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !CRAFTED_FOODS.contains(result.getType())) return;
        if (failsChecks(player, ability)) return;
        if (plugin.getUser(player).getAbilityLevel(ability) <= 0) return;

        tagChef(result, player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void gourmet(CraftItemEvent event) {
        var ability = Abilities.GOURMET;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !CRAFTED_FOODS.contains(result.getType())) return;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        applyWellFed(player, item.getType());
        applyHeartyMeals(player, item);
    }

    private void applyWellFed(Player player, Material type) {
        var ability = Abilities.WELL_FED;
        if (isDisabled(ability)) return;
        if (!SMELT_FOODS.contains(type) && !CRAFTED_FOODS.contains(type)) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;

        int duration = (int) (getValue(ability, user) * 20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0));
    }

    private void applyHeartyMeals(Player player, ItemStack item) {
        var ability = Abilities.HEARTY_MEALS;
        if (isDisabled(ability)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String owner = meta.getPersistentDataContainer().get(CHEF_KEY, PersistentDataType.STRING);
        if (!player.getUniqueId().toString().equals(owner)) return;
        if (failsChecks(player, ability)) return;

        User user = plugin.getUser(player);
        if (user.getAbilityLevel(ability) <= 0) return;

        Integer nutrition = NUTRITION.get(item.getType());
        if (nutrition == null) return;

        int bonus = (int) Math.ceil(nutrition * getValue(ability, user) / 100);
        if (bonus <= 0) return;
        // One tick later so vanilla hunger restoration applies first
        plugin.getScheduler().scheduleSync(() -> {
            if (!player.isOnline()) return;
            int foodLevel = Math.min(20, player.getFoodLevel() + bonus);
            player.setFoodLevel(foodLevel);
            player.setSaturation(Math.min(foodLevel, player.getSaturation() + bonus));
        }, 50, TimeUnit.MILLISECONDS);
    }

    private void tagChef(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(CHEF_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        item.setItemMeta(meta);
    }

    private boolean isTagged(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(CHEF_KEY, PersistentDataType.STRING);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

}
