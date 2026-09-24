package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.SourceValues;
import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;

public class SmithingSource extends Source implements SmithingXpSource {

    private final String[] results;
    private final String[] templates;
    private final String[] bases;
    private final String[] additions;

    public SmithingSource(AuraSkillsPlugin plugin, SourceValues values, String[] results, String[] templates, String[] bases, String[] additions) {
        super(plugin, values);
        this.results = results;
        this.templates = templates;
        this.bases = bases;
        this.additions = additions;
    }

    @Override
    public String[] getResults() {
        return results;
    }

    @Override
    public String[] getTemplates() {
        return templates;
    }

    @Override
    public String[] getBases() {
        return bases;
    }

    @Override
    public String[] getAdditions() {
        return additions;
    }

    @Override
    public boolean isVersionValid() {
        return allValid(results) && allValid(templates) && allValid(bases) && allValid(additions);
    }

    private boolean allValid(String[] materials) {
        for (String material : materials) {
            if (!plugin.getPlatformUtil().isValidMaterial(material)) {
                return false;
            }
        }
        return true;
    }
}
