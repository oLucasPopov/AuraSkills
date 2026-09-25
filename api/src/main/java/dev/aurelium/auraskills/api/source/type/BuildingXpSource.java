package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface BuildingXpSource extends XpSource {

    /** Block materials that give XP when placed; empty means any block. */
    String[] getBlocks();

}
