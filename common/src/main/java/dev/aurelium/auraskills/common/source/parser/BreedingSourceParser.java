package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.api.source.type.BreedingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.BreedingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class BreedingSourceParser extends SourceParser<BreedingSource> {

    public BreedingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public BreedingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        String[] entities = context.pluralizedArray("entity", source, String.class);
        if (entities == null) {
            entities = new String[0];
        }
        BreedingXpSource.BreedingTrigger trigger = source.node("trigger").get(BreedingXpSource.BreedingTrigger.class);
        if (trigger == null) {
            throw new SerializationException("Missing required field 'trigger' for breeding source");
        }
        return new BreedingSource(plugin, context.parseValues(source), entities, trigger);
    }
}
