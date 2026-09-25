package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CampfireLeveler extends SourceLeveler {

    private static final long ATTRIBUTION_MS = 10 * 60 * 1000L;

    // block key -> (player id -> timestamp); one campfire has 4 slots, tracking per-block is enough
    private final Map<Long, Map<UUID, Long>> placements = new ConcurrentHashMap<>();

    public CampfireLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMELTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceFood(PlayerInteractEvent event) {
        if (disabled()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        ItemStack held = event.getItem();
        if (held == null || !isCampfireCookable(held)) return;

        placements.computeIfAbsent(blockKey(block), k -> new ConcurrentHashMap<>())
                .put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (disabled()) return;
        Map<UUID, Long> cooks = placements.remove(blockKey(event.getBlock()));
        if (cooks == null) return;

        String resultName = event.getResult().getType().name();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> cook : cooks.entrySet()) {
            if (now - cook.getValue() > ATTRIBUTION_MS) continue;
            Player player = Bukkit.getPlayer(cook.getKey());
            if (player == null) continue;

            for (SkillSource<SmeltingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class)) {
                SmeltingXpSource source = entry.source();
                if (!SmeltingLeveler.matchesAny(source.getItems(), resultName)) continue;

                Skill skill = entry.skill();
                if (failsChecks(event, player, event.getBlock().getLocation(), skill)) return;

                User user = plugin.getUser(player);
                plugin.getLevelManager().addXp(user, skill, source, source.getXp());
                return;
            }
        }
    }

    private boolean isCampfireCookable(ItemStack item) {
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof CampfireRecipe campfireRecipe
                    && campfireRecipe.getInputChoice().test(item)) {
                return true;
            }
        }
        return false;
    }

    private static long blockKey(Block block) {
        return 31L * (31L * (31L * block.getWorld().getUID().hashCode() + block.getX()) + block.getY()) + block.getZ();
    }
}
