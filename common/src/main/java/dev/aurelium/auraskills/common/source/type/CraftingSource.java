package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.CraftingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;

public class CraftingSource extends Source implements CraftingXpSource {

    private final String[] items;

    public CraftingSource(AuraSkillsPlugin plugin, SourceValues values, String[] items) {
        super(plugin, values);
        this.items = items;
    }

    @Override
    public String[] getItems() {
        return items;
    }

    @Override
    public boolean isVersionValid() {
        for (String item : items) {
            if (!plugin.getPlatformUtil().isValidMaterial(item)) {
                return false;
            }
        }
        return true;
    }
}
