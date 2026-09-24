package dev.aurelium.auraskills.common.source.parser;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.ConfigurateSourceContext;
import dev.aurelium.auraskills.common.source.type.SmithingSource;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class SmithingSourceParser extends SourceParser<SmithingSource> {

    public SmithingSourceParser(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public SmithingSource parse(ConfigurationNode source, ConfigurateSourceContext context) throws SerializationException {
        String[] results = pluralizedOrEmpty(context, "result", source);
        String[] templates = pluralizedOrEmpty(context, "template", source);
        String[] bases = pluralizedOrEmpty(context, "base", source);
        String[] additions = pluralizedOrEmpty(context, "addition", source);
        return new SmithingSource(plugin, context.parseValues(source), results, templates, bases, additions);
    }

    private String[] pluralizedOrEmpty(ConfigurateSourceContext context, String key, ConfigurationNode source) {
        String[] array = context.pluralizedArray(key, source, String.class);
        return array != null ? array : new String[0];
    }
}
