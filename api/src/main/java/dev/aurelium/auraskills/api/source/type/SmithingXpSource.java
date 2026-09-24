package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface SmithingXpSource extends XpSource {

    /** Result item materials that match; empty array means any result. */
    String[] getResults();

    /** Template materials (e.g. netherite_upgrade, armor trims); empty means any. */
    String[] getTemplates();

    /** Base equipment materials; empty means any. */
    String[] getBases();

    /** Addition materials (e.g. netherite_ingot); empty means any. */
    String[] getAdditions();

}
