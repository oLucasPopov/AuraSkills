package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.SmeltingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class SmeltingSourceParser extends SourceParser<SmeltingSource> {

    public SmeltingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public SmeltingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        String[] items = context.requiredPluralizedArray("item", source, String.class);
        return new SmeltingSource(plugin, context.parseValues(source), items);
    }
}
