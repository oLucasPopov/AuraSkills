package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.BuildingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class BuildingSourceParser extends SourceParser<BuildingSource> {

    public BuildingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public BuildingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        String[] blocks = context.requiredPluralizedArray("block", source, String.class);
        return new BuildingSource(plugin, context.parseValues(source), blocks);
    }
}
