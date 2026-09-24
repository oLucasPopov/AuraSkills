package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface BreedingXpSource extends XpSource {

    /** Entity types (e.g. cow, wolf); empty means any entity of the trigger. */
    String[] getEntities();

    BreedingTrigger getTrigger();

    enum BreedingTrigger {

        BREED,
        TAME,
        SHEAR,
        MILK

    }

}
