package dev.aurelium.auraskills.bukkit.skills.husbandry;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.api.util.NumberUtil;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.mana.ManaAbilityData;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.scheduler.Task;
import dev.aurelium.auraskills.common.scheduler.TaskRunnable;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import dev.aurelium.auraskills.paper.util.EntityPathUtil;
import dev.aurelium.auraskills.paper.util.PaperUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AnimalWhisperer extends ReadiedManaAbility {

    private final Map<UUID, Task> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> grownBabies = new ConcurrentHashMap<>();

    public AnimalWhisperer(AuraSkills plugin) {
        super(plugin, ManaAbilities.ANIMAL_WHISPERER, ManaAbilityMessage.ANIMAL_WHISPERER_START, ManaAbilityMessage.ANIMAL_WHISPERER_END,
                new String[]{"WHEAT"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, 1, 1.2f);
        int radius = manaAbility.optionInt("radius", 15);
        grownBabies.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());
        Task task = plugin.getScheduler().timerSync(new TaskRunnable() {
            @Override
            public void run() {
                whisper(player, radius);
            }
        }, 500, 500, TimeUnit.MILLISECONDS); // Every 10 ticks
        tasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void activationListener(PlayerInteractEntityEvent event) {
        if (isDisabled()) return;
        if (event.isCancelled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Animals)) return;

        Player player = event.getPlayer();
        if (failsChecks(player)) return;
        // Clicking an animal with wheat is the natural gesture, so ready the ability
        // on the first click instead of requiring a click on air first
        if (tryReady(player)) return;
        checkActivation(player);
    }

    private boolean tryReady(Player player) {
        if (!isHoldingMaterial(player)) return false;
        User user = plugin.getUser(player);
        if (user.getManaAbilityLevel(manaAbility) <= 0) return false;
        ManaAbilityData data = user.getManaAbilityData(manaAbility);
        if (data.isActivated() || data.isReady()) return false;
        Locale locale = user.getLocale();
        if (data.getCooldown() != 0) {
            if (data.getErrorTimer() == 0) {
                plugin.getAbilityManager().sendMessage(player, plugin.getMsg(ManaAbilityMessage.NOT_READY, locale).replace("{cooldown}",
                        NumberUtil.format0((double) data.getCooldown() / 20)));
                data.setErrorTimer(2);
            }
            return true;
        }
        data.setReady(true);
        plugin.getAbilityManager().sendMessage(player, plugin.getMsg(ManaAbilityMessage.valueOf(manaAbility.name() + "_RAISE"), locale));
        plugin.getScheduler().scheduleSync(() -> {
            if (!data.isActivated() && data.isReady()) {
                data.setReady(false);
                plugin.getAbilityManager().sendMessage(player, plugin.getMsg(ManaAbilityMessage.valueOf(manaAbility.name() + "_LOWER"), locale));
            }
        }, 4000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void onStop(Player player, User user) {
        Task task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        grownBabies.remove(player.getUniqueId());
    }

    @Override
    public String replaceDescPlaceholders(String input, User user) {
        return TextUtil.replace(input, "{radius}", String.valueOf(manaAbility.optionInt("radius", 15)));
    }

    private void whisper(Player player, int radius) {
        if (!isActivated(plugin.getUser(player))) {
            onStop(player, plugin.getUser(player));
            return;
        }
        Set<UUID> grown = grownBabies.get(player.getUniqueId());
        if (grown == null) return;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Animals animals)) continue;
            if (entity instanceof Sittable sittable && sittable.isSitting()) continue;
            if (!animals.isAdult()) {
                // One growth step per activation per baby
                if (grown.add(animals.getUniqueId())) {
                    animals.setAge(animals.getAge() + 2400); // 2 minutes of growth
                }
            }
            double distanceSq = entity.getLocation().distanceSquared(player.getLocation());
            if (PaperUtil.IS_PAPER) {
                // Real pathfinding so animals actually walk to the player
                if (distanceSq > 9) {
                    EntityPathUtil.moveTo(entity, player.getLocation(), 1.2);
                } else {
                    EntityPathUtil.stopPath(entity);
                }
            } else if (distanceSq > 4) {
                // Fallback for non-Paper servers: gentle velocity pull
                Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector());
                direction.normalize().multiply(0.15);
                entity.setVelocity(entity.getVelocity().add(direction).setY(Math.max(entity.getVelocity().getY(), 0)));
            }
        }
    }
}
