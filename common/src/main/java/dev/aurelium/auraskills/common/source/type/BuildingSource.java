package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.BuildingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;

public class BuildingSource extends Source implements BuildingXpSource {

    private final String[] blocks;

    public BuildingSource(AuraSkillsPlugin plugin, SourceValues values, String[] blocks) {
        super(plugin, values);
        this.blocks = blocks;
    }

    @Override
    public String[] getBlocks() {
        return blocks;
    }

    @Override
    public boolean isVersionValid() {
        for (String block : blocks) {
            if (!plugin.getPlatformUtil().isValidMaterial(block)) {
                return false;
            }
        }
        return true;
    }
}
