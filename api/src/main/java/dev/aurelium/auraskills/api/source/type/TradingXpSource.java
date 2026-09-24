package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;
import org.jetbrains.annotations.Nullable;

public interface TradingXpSource extends XpSource {

    TradingTrigger getTrigger();

    /** Required villager profession (e.g. librarian); null means any. */
    @Nullable
    String getVillagerProfession();

    /** Minimum villager level (1 = novice ... 5 = master). */
    int getMinVillagerLevel();

    /** Maximum villager level. */
    int getMaxVillagerLevel();

    /** Result item for piglin bartering sources; null means any outcome. */
    @Nullable
    String getBarterItem();

    enum TradingTrigger {

        VILLAGER_TRADE,
        WANDERING_TRADER_TRADE,
        PIGLIN_BARTER

    }

}
