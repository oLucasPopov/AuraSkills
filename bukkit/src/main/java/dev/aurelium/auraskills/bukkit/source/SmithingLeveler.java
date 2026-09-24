package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class SmithingLeveler extends SourceLeveler {

    public SmithingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMITHING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        SmithingInventory inventory = event.getInventory();
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        // Slots 0/1/2 are template/base/addition in the modern smithing inventory layout
        String resultName = result.getType().name();
        String templateName = nameOrNull(inventory.getItem(0));
        String baseName = nameOrNull(inventory.getItem(1));
        String additionName = nameOrNull(inventory.getItem(2));

        for (SkillSource<SmithingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmithingXpSource.class)) {
            SmithingXpSource source = entry.source();
            if (!matches(source, resultName, templateName, baseName, additionName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * result.getAmount());
            return;
        }
    }

    private boolean matches(SmithingXpSource source, String result, String template, String base, String addition) {
        if (source.getResults().length > 0 && (result == null || !SmeltingLeveler.matchesAny(source.getResults(), result))) return false;
        if (source.getTemplates().length > 0 && (template == null || !SmeltingLeveler.matchesAny(source.getTemplates(), template))) return false;
        if (source.getBases().length > 0 && (base == null || !SmeltingLeveler.matchesAny(source.getBases(), base))) return false;
        return source.getAdditions().length == 0 || (addition != null && SmeltingLeveler.matchesAny(source.getAdditions(), addition));
    }

    private static String nameOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.getType().name();
    }
}
