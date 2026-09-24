package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.BreedingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;

public class BreedingSource extends Source implements BreedingXpSource {

    private final String[] entities;
    private final BreedingTrigger trigger;

    public BreedingSource(AuraSkillsPlugin plugin, SourceValues values, String[] entities, BreedingTrigger trigger) {
        super(plugin, values);
        this.entities = entities;
        this.trigger = trigger;
    }

    @Override
    public String[] getEntities() {
        return entities;
    }

    @Override
    public BreedingTrigger getTrigger() {
        return trigger;
    }

    @Override
    public boolean isVersionValid() {
        for (String entity : entities) {
            if (!plugin.getPlatformUtil().isValidEntityType(entity)) {
                return false;
            }
        }
        return true;
    }
}
