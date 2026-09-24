package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class SmeltingLeveler extends SourceLeveler {

    public SmeltingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMELTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtract(FurnaceExtractEvent event) {
        if (disabled()) return;

        Player player = event.getPlayer();
        String itemName = event.getItemType().name();

        for (SkillSource<SmeltingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class)) {
            SmeltingXpSource source = entry.source();
            if (!matchesAny(source.getItems(), itemName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(player, event.getBlock().getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * event.getItemAmount());
            return;
        }
    }

    static boolean matchesAny(String[] candidates, String materialName) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(materialName)) return true;
        }
        return false;
    }
}
