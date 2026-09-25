package dev.aurelium.auraskills.bukkit.skills.culinary;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Banquet extends ReadiedManaAbility {

    private static final double RADIUS = 10;

    public Banquet(AuraSkills plugin) {
        super(plugin, ManaAbilities.BANQUET, ManaAbilityMessage.BANQUET_START, ManaAbilityMessage.BANQUET_END,
                new String[]{"BOWL"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        int duration = getDuration(user); // Ticks; checkActivation already schedules the stop
        applyEffects(player, duration);
        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof Player nearby) {
                applyEffects(nearby, duration);
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
    }

    // A second right-click with the bowl while readied activates the ability
    @EventHandler
    public void activationListener(PlayerInteractEvent event) {
        if (isDisabled()) return;
        if (event.isCancelled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        Block block = event.getClickedBlock();
        if (block != null && isExcludedBlock(block)) return;

        Player player = event.getPlayer();
        if (!isHoldingMaterial(player)) return;
        if (failsChecks(player)) return;
        checkActivation(player); // No-op unless the ability is readied
    }

    @Override
    public void onStop(Player player, User user) {
        // Effects expire naturally
    }

    private void applyEffects(Player player, int duration) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0));
    }

}
