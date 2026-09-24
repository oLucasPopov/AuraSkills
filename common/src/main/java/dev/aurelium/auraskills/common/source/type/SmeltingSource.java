package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;

public class SmeltingSource extends Source implements SmeltingXpSource {

    private final String[] items;

    public SmeltingSource(AuraSkillsPlugin plugin, SourceValues values, String[] items) {
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
