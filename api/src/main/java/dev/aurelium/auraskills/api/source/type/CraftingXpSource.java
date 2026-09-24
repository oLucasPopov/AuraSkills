package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface CraftingXpSource extends XpSource {

    /** Crafted result item materials. */
    String[] getItems();

}
