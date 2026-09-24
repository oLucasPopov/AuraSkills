package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface SmeltingXpSource extends XpSource {

    /** Smelted result item materials. */
    String[] getItems();

}
