package dev.aurelium.auraskills.bukkit.skills.trading;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GrandBargain extends ReadiedManaAbility {

    public GrandBargain(AuraSkills plugin) {
        super(plugin, ManaAbilities.GRAND_BARGAIN, ManaAbilityMessage.GRAND_BARGAIN_START, ManaAbilityMessage.GRAND_BARGAIN_END,
                new String[]{"EMERALD"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
        // Active state is managed by ReadiedManaAbility; TradingAbilities queries getManaAbilityData(...).isActivated()
    }

    // Ready by right-clicking with an emerald, then activate by right-clicking a villager or wandering trader
    @EventHandler
    public void activationListener(PlayerInteractEntityEvent event) {
        if (isDisabled()) return;
        if (event.isCancelled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof AbstractVillager)) return;

        Player player = event.getPlayer();
        if (failsChecks(player)) return;
        if (!isHoldingMaterial(player)) return;
        checkActivation(player);
    }

    @Override
    public void onStop(Player player, User user) {
    }

    @Override
    public String replaceDescPlaceholders(String input, User user) {
        return TextUtil.replace(input, "{discount_percentage}", String.valueOf(manaAbility.optionDouble("discount_percentage", 20.0)));
    }
}
