package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.api.source.type.TradingXpSource.TradingTrigger;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradingLeveler extends SourceLeveler {

    private static final int DECAY_WINDOW = 5;
    // Radius to attribute a piglin barter to the nearest player (spigot-api PiglinBarterEvent has no getPlayer())
    private static final double BARTER_PLAYER_RADIUS = 10.0;

    // player id -> merchant key -> recent recipe keys
    private final Map<UUID, Map<String, Deque<String>>> tradeHistory = new ConcurrentHashMap<>();

    public TradingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.TRADING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof MerchantInventory inventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        MerchantRecipe recipe = inventory.getSelectedRecipe();
        if (recipe == null) return;
        // Trade must have stock left (result slot is empty when exhausted, checked above via currentItem)
        if (recipe.getUses() >= recipe.getMaxUses()) return;

        Entity merchantEntity = inventory.getMerchant() instanceof Entity entity ? entity : null;

        TradingTrigger trigger;
        String profession = null;
        int villagerLevel = 1;
        if (merchantEntity instanceof Villager villager) {
            trigger = TradingTrigger.VILLAGER_TRADE;
            profession = villager.getProfession().name();
            villagerLevel = villager.getVillagerLevel();
        } else if (merchantEntity instanceof WanderingTrader) {
            trigger = TradingTrigger.WANDERING_TRADER_TRADE;
        } else {
            return; // Merchant GUI not backed by an entity (e.g. custom merchants from other plugins)
        }

        for (SkillSource<TradingXpSource> entry : plugin.getSkillManager().getSourcesOfType(TradingXpSource.class)) {
            TradingXpSource source = entry.source();
            if (source.getTrigger() != trigger) continue;
            if (source.getVillagerProfession() != null && (profession == null || !source.getVillagerProfession().equalsIgnoreCase(profession))) continue;
            if (villagerLevel < source.getMinVillagerLevel() || villagerLevel > source.getMaxVillagerLevel()) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            String merchantKey = merchantEntity.getUniqueId().toString();
            String recipeKey = result.getType().name() + ":" + recipe.getIngredients();
            double decay = recordAndGetDecay(player.getUniqueId(), merchantKey, recipeKey);

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * decay);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBarter(PiglinBarterEvent event) {
        if (disabled()) return;
        Player player = getNearestPlayer(event.getEntity());
        if (player == null) return;

        for (SkillSource<TradingXpSource> entry : plugin.getSkillManager().getSourcesOfType(TradingXpSource.class)) {
            TradingXpSource source = entry.source();
            if (source.getTrigger() != TradingTrigger.PIGLIN_BARTER) continue;

            double total = 0;
            for (ItemStack outcome : event.getOutcome()) {
                if (source.getBarterItem() == null || source.getBarterItem().equalsIgnoreCase(outcome.getType().name())) {
                    total += source.getXp() * outcome.getAmount();
                }
            }
            if (total <= 0) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, event.getEntity().getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, total);
            return;
        }
    }

    // spigot-api has no PiglinBarterEvent#getPlayer(), so attribute the barter to the nearest player
    private Player getNearestPlayer(Entity piglin) {
        Player nearest = null;
        double nearestDistance = BARTER_PLAYER_RADIUS * BARTER_PLAYER_RADIUS;
        for (Entity nearby : piglin.getNearbyEntities(BARTER_PLAYER_RADIUS, BARTER_PLAYER_RADIUS, BARTER_PLAYER_RADIUS)) {
            if (!(nearby instanceof Player candidate)) continue;
            double distance = candidate.getLocation().distanceSquared(piglin.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private double recordAndGetDecay(UUID playerId, String merchantKey, String recipeKey) {
        Deque<String> recent = tradeHistory
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(merchantKey, k -> new ArrayDeque<>());
        long occurrences = recent.stream().filter(recipeKey::equals).count();
        recent.addLast(recipeKey);
        while (recent.size() > DECAY_WINDOW) {
            recent.removeFirst();
        }
        return computeDecayMultiplier((int) occurrences);
    }

    /** occurrences = times this recipe already appeared in the current window (0 = first trade). */
    public static double computeDecayMultiplier(int occurrences) {
        if (occurrences < 2) return 1.0;
        return Math.max(0.1, Math.pow(0.5, occurrences - 1));
    }
}
