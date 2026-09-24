package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.CraftingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingLeveler extends SourceLeveler {

    public CraftingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.CRAFTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        String itemName = result.getType().name();

        for (SkillSource<CraftingXpSource> entry : plugin.getSkillManager().getSourcesOfType(CraftingXpSource.class)) {
            CraftingXpSource source = entry.source();
            if (!SmeltingLeveler.matchesAny(source.getItems(), itemName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            int amount = getAmountCrafted(event);
            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * amount);
            return;
        }
    }

    static int getAmountCrafted(CraftItemEvent event) {
        int resultAmount = event.getRecipe().getResult().getAmount();
        if (!event.isShiftClick()) {
            return resultAmount;
        }
        int maxCrafts = Integer.MAX_VALUE;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null || ingredient.getType().isAir()) continue;
            maxCrafts = Math.min(maxCrafts, ingredient.getAmount());
        }
        if (maxCrafts == Integer.MAX_VALUE || maxCrafts < 1) {
            return resultAmount;
        }
        return resultAmount * maxCrafts;
    }
}
