package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.TradingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class TradingSourceParser extends SourceParser<TradingSource> {

    public TradingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public TradingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        TradingXpSource.TradingTrigger trigger = source.node("trigger").get(TradingXpSource.TradingTrigger.class, TradingXpSource.TradingTrigger.VILLAGER_TRADE);
        String profession = source.node("villager_profession").getString();
        int minLevel = source.node("min_villager_level").getInt(1);
        int maxLevel = source.node("max_villager_level").getInt(5);
        String barterItem = source.node("barter_item").getString();
        return new TradingSource(plugin, context.parseValues(source), trigger, profession, minLevel, maxLevel, barterItem);
    }
}
