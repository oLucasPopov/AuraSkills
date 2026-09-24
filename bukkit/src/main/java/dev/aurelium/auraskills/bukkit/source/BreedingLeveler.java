package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.BreedingXpSource;
import dev.aurelium.auraskills.api.source.type.BreedingXpSource.BreedingTrigger;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BreedingLeveler extends SourceLeveler {

    private static final long PARENT_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final long MILK_COOLDOWN_MS = 60 * 1000L;

    private final Map<String, Long> parentPairCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> milkCooldowns = new ConcurrentHashMap<>();

    public BreedingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.BREEDING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (disabled()) return;
        if (!(event.getBreeder() instanceof Player player)) return;
        if (isParentPairOnCooldown(event.getFather(), event.getMother())) return;

        grant(player, event.getEntity(), BreedingTrigger.BREED, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (disabled()) return;
        if (!(event.getOwner() instanceof Player player)) return;

        grant(player, event.getEntity(), BreedingTrigger.TAME, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (disabled()) return;

        grant(event.getPlayer(), event.getEntity(), BreedingTrigger.SHEAR, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMilk(PlayerInteractEntityEvent event) {
        if (disabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        EntityType type = entity.getType();
        if (type != EntityType.COW && type != EntityType.GOAT && type != EntityType.MOOSHROOM) return;

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (held.getType() != Material.BUCKET && held.getType() != Material.BOWL) return;
        if (held.getType() == Material.BOWL && type != EntityType.MOOSHROOM) return;
        if (entity instanceof Animals animals && !animals.isAdult()) return;

        long now = System.currentTimeMillis();
        Long last = milkCooldowns.get(entity.getUniqueId());
        if (last != null && now - last < MILK_COOLDOWN_MS) return;
        milkCooldowns.put(entity.getUniqueId(), now);

        grant(event.getPlayer(), entity, BreedingTrigger.MILK, event);
    }

    private void grant(Player player, Entity entity, BreedingTrigger trigger, org.bukkit.event.Cancellable event) {
        String entityName = entity.getType().name();
        for (SkillSource<BreedingXpSource> entry : plugin.getSkillManager().getSourcesOfType(BreedingXpSource.class)) {
            BreedingXpSource source = entry.source();
            if (source.getTrigger() != trigger) continue;
            if (source.getEntities().length > 0 && !SmeltingLeveler.matchesAny(source.getEntities(), entityName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, entity.getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp());
            return;
        }
    }

    private boolean isParentPairOnCooldown(LivingEntity father, LivingEntity mother) {
        String key = father.getUniqueId() + ":" + mother.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = parentPairCooldowns.get(key);
        if (last != null && now - last < PARENT_COOLDOWN_MS) {
            return true;
        }
        parentPairCooldowns.put(key, now);
        return false;
    }
}
