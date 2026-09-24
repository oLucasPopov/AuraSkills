package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;
import org.jetbrains.annotations.Nullable;

public class TradingSource extends Source implements TradingXpSource {

    private final TradingTrigger trigger;
    private final @Nullable String villagerProfession;
    private final int minVillagerLevel;
    private final int maxVillagerLevel;
    private final @Nullable String barterItem;

    public TradingSource(AuraSkillsPlugin plugin, SourceValues values, TradingTrigger trigger,
                         @Nullable String villagerProfession, int minVillagerLevel, int maxVillagerLevel,
                         @Nullable String barterItem) {
        super(plugin, values);
        this.trigger = trigger;
        this.villagerProfession = villagerProfession;
        this.minVillagerLevel = minVillagerLevel;
        this.maxVillagerLevel = maxVillagerLevel;
        this.barterItem = barterItem;
    }

    @Override
    public TradingTrigger getTrigger() {
        return trigger;
    }

    @Override
    public @Nullable String getVillagerProfession() {
        return villagerProfession;
    }

    @Override
    public int getMinVillagerLevel() {
        return minVillagerLevel;
    }

    @Override
    public int getMaxVillagerLevel() {
        return maxVillagerLevel;
    }

    @Override
    public @Nullable String getBarterItem() {
        return barterItem;
    }

    @Override
    public boolean isVersionValid() {
        return barterItem == null || plugin.getPlatformUtil().isValidMaterial(barterItem);
    }
}
