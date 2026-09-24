package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.CraftingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class CraftingSourceParser extends SourceParser<CraftingSource> {

    public CraftingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public CraftingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        String[] items = context.requiredPluralizedArray("item", source, String.class);
        return new CraftingSource(plugin, context.parseValues(source), items);
    }
}
