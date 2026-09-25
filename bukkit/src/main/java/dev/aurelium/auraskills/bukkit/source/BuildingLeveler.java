package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.BuildingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuildingLeveler extends SourceLeveler {

    private static final int DECAY_WINDOW = 8;
    private static final long POSITION_CACHE_MS = 5 * 60 * 1000L;

    private final Map<UUID, Deque<String>> recentPlacements = new ConcurrentHashMap<>();
    private final PositionCache positionCache = new PositionCache(POSITION_CACHE_MS);

    public BuildingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.BUILDING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (disabled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        String blockName = block.getType().name();

        for (SkillSource<BuildingXpSource> entry : plugin.getSkillManager().getSourcesOfType(BuildingXpSource.class)) {
            BuildingXpSource source = entry.source();
            if (source.getBlocks().length > 0 && !SmeltingLeveler.matchesAny(source.getBlocks(), blockName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, block.getLocation(), skill)) return;
            if (!positionCache.tryGrant(positionKey(block), System.currentTimeMillis())) return;

            double decay = recordAndGetDecay(player.getUniqueId(), blockName);
            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * decay);
            return;
        }
    }

    private static long positionKey(Block block) {
        return 31L * (31L * (31L * block.getWorld().getUID().hashCode() + block.getX()) + block.getY()) + block.getZ();
    }

    private double recordAndGetDecay(UUID playerId, String blockName) {
        Deque<String> recent = recentPlacements.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        long occurrences = recent.stream().filter(blockName::equals).count();
        recent.addLast(blockName);
        while (recent.size() > DECAY_WINDOW) {
            recent.removeFirst();
        }
        return TradingLeveler.computeDecayMultiplier((int) occurrences);
    }

    /** Time-based "already gave XP here" cache. Extracted for testability. */
    static class PositionCache {
        private final long windowMs;
        private final Map<Long, Long> grants = new ConcurrentHashMap<>();

        PositionCache(long windowMs) {
            this.windowMs = windowMs;
        }

        boolean tryGrant(long key, long now) {
            Long last = grants.get(key);
            if (last != null && now - last < windowMs) {
                return false;
            }
            grants.put(key, now);
            if (grants.size() > 10_000) {
                grants.entrySet().removeIf(e -> now - e.getValue() >= windowMs);
            }
            return true;
        }
    }
}
