package dev.aurelium.auraskills.bukkit.skills.husbandry;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.bukkit.util.AttributeCompat;
import dev.aurelium.auraskills.common.scheduler.Task;
import dev.aurelium.auraskills.common.scheduler.TaskRunnable;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
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
import java.util.concurrent.TimeUnit;

public class HusbandryAbilities extends BukkitAbilityImpl {

    private final Map<UUID, Task> growthTasks = new ConcurrentHashMap<>();

    public HusbandryAbilities(AuraSkills plugin) {
        super(plugin, Abilities.TWINS, Abilities.RANCHER, Abilities.HEALTHY_GROWTH, Abilities.GENTLE_HANDS, Abilities.BOUNTIFUL_PASTURE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        User user = plugin.getUser(player);

        applyTwins(event, player, user);
        applyHealthyGrowth(event.getEntity(), player, user);
    }

    private void applyTwins(EntityBreedEvent event, Player player, User user) {
        var ability = Abilities.TWINS;
        if (isDisabled(ability)) return;
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= Math.min(30.0, getValue(ability, user)) / 100) return;

        Entity baby = event.getEntity();
        LivingEntity twin = (LivingEntity) baby.getWorld().spawnEntity(baby.getLocation(), baby.getType());
        if (twin instanceof Animals animals) {
            animals.setBaby();
        }
    }

    private void applyHealthyGrowth(Entity baby, Player player, User user) {
        var ability = Abilities.HEALTHY_GROWTH;
        if (isDisabled(ability)) return;
        if (!(baby instanceof Animals animals)) return;
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        double boost = Math.min(40.0, getValue(ability, user)) / 100;
        scheduleGrowth(animals, boost);
    }

    private void scheduleGrowth(Animals animals, double chancePerTick) {
        UUID id = animals.getUniqueId();
        // Baby age is negative and counts up to 0; each second, with probability `chancePerTick`, advance growth by one extra second
        TaskRunnable runnable = new TaskRunnable() {
            @Override
            public void run() {
                Entity entity = Bukkit.getEntity(id);
                if (!(entity instanceof Animals grown) || !grown.isValid() || grown.isAdult()) {
                    growthTasks.remove(id);
                    cancel();
                    return;
                }
                if (rand.nextDouble() < chancePerTick) {
                    grown.setAge(grown.getAge() + 20); // one extra second of growth
                }
            }
        };
        Task task = plugin.getScheduler().timerSync(runnable, 1000, 1000, TimeUnit.MILLISECONDS);
        growthTasks.put(id, task);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void gentleHands(EntityTameEvent event) {
        var ability = Abilities.GENTLE_HANDS;
        if (isDisabled(ability)) return;
        if (!(event.getOwner() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        int abilityLevel = user.getAbilityLevel(ability);
        if (abilityLevel <= 0) return;

        LivingEntity entity = event.getEntity();
        double extraHealth = Math.min(10.0, getValue(ability, user)); // value is in half-hearts
        AttributeInstance maxHealth = entity.getAttribute(AttributeCompat.maxHealth);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() + extraHealth);
            entity.setHealth(Math.min(maxHealth.getValue(), entity.getHealth() + extraHealth));
        }

        double speedBoost = ability.optionDouble("speed_boost_per_level", 2.0) * abilityLevel / 100;
        AttributeInstance speed = entity.getAttribute(AttributeCompat.movementSpeed);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * (1 + speedBoost));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void bountifulPasture(PlayerShearEntityEvent event) {
        var ability = Abilities.BOUNTIFUL_PASTURE;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        Entity entity = event.getEntity();
        if (entity instanceof Sheep sheep) {
            ItemStack extraWool = new ItemStack(woolForColor(sheep), 1);
            entity.getWorld().dropItemNaturally(entity.getLocation(), extraWool);
        } else if (entity.getType() == EntityType.MOOSHROOM) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.RED_MUSHROOM, 1));
        }
    }

    private Material woolForColor(Sheep sheep) {
        String color = sheep.getColor() != null ? sheep.getColor().name() : "WHITE";
        Material wool = Material.matchMaterial(color + "_WOOL");
        return wool != null ? wool : Material.WHITE_WOOL;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void bountifulPastureMilking(PlayerInteractEntityEvent event) {
        var ability = Abilities.BOUNTIFUL_PASTURE;
        if (isDisabled(ability)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Animals animals) || !animals.isAdult()) return;
        EntityType type = animals.getType();
        if (type != EntityType.COW && type != EntityType.GOAT && type != EntityType.MOOSHROOM) return;

        Player player = event.getPlayer();
        Material held = player.getInventory().getItemInMainHand().getType();
        boolean bowl = type == EntityType.MOOSHROOM && held == Material.BOWL;
        if (held != Material.BUCKET && !bowl) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        // Vanilla milking replaces the held bucket/bowl, so one interact can only roll once
        ItemStack extra = new ItemStack(bowl ? Material.MUSHROOM_STEW : Material.MILK_BUCKET, 1);
        player.getInventory().addItem(extra).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
