package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.hooks.NexoHook;
import dev.aurelium.auraskills.bukkit.menus.shared.SkillItem;
import dev.aurelium.auraskills.common.api.ApiAuraSkills;
import dev.aurelium.auraskills.common.util.file.FileUtil;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MenuFileManager {

    private final AuraSkills plugin;
    public static final String[] MENU_NAMES = {
            "abilities", "leaderboard", "level_progression", "skills", "sources", "stats", "stat_info"
    };

    public MenuFileManager(AuraSkills plugin) {
        this.plugin = plugin;
    }

    public void generateDefaultFiles() {
        for (String menuName : MENU_NAMES) {
            File file = new File(plugin.getDataFolder() + "/menus", menuName + ".yml");
            if (!file.exists()) {
                plugin.saveResource("menus/" + menuName + ".yml", false);
            }
        }
    }

    public void loadMenus() {
        // Add menu directories as merge directories in Slate
        var api = (ApiAuraSkills) plugin.getApi();
        for (NamespacedRegistry registry : api.getNamespacedRegistryMap().values()) {
            registry.getMenuDirectory().ifPresent(dir -> plugin.getSlate().addMergeDirectory(dir));
        }

        int menusLoaded = plugin.getSlate().loadMenus();

        // Post load operations
        SkillItem.loadFormats(plugin);

        plugin.getLogger().info("Loaded " + menusLoaded + " menus");
    }

    // Should only be called on startup
    public void loadMenusOnStartup() {
        if (plugin.getHookManager().isRegistered(NexoHook.class)) {
            // Timeout for loading menus in case the event in NexoHook didn't fire
            plugin.getScheduler().scheduleSync(() -> {
                NexoHook hook = plugin.getHookManager().getHook(NexoHook.class);
                if (!hook.getItemsLoadedCallbacks().isEmpty()) {
                    loadMenus();
                }
            }, 5 * 50, TimeUnit.MILLISECONDS);
        } else {
            loadMenus();
        }
    }

    public void updateMenus() {
        for (String menuName : MENU_NAMES) {
            File userFile = new File(plugin.getDataFolder() + "/menus", menuName + ".yml");
            if (!userFile.exists()) continue;

            try {
                ConfigurationNode embeddedConfig = FileUtil.loadEmbeddedYamlFile("menus/" + menuName + ".yml", plugin);
                ConfigurationNode userConfig = FileUtil.loadYamlFile(userFile);

                updateAndSave(menuName, embeddedConfig, userConfig, userFile);
            } catch (IOException e) {
                plugin.logger().warn("Error updating menu file " + userFile.getName());
                e.printStackTrace();
            }
        }
    }

    private void updateAndSave(String menuName, ConfigurationNode embedded, ConfigurationNode user, File userFile) throws SerializationException {
        // Files that don't have updating enabled yet, since they haven't had changes
        if (embedded.node("file_version").virtual()) return;

        // === Fork skills: trading, husbandry, smithing ===
        // The MenuFileUpdates mechanism only merges whole missing named sections, so template
        // contexts/groups added inside an existing template never reach existing servers.
        // Merge them here (missing keys only, user customizations are never overwritten).
        mergeMissingTemplateKeys(embedded, user, userFile);
        // === End fork skills ===

        int embVersion = embedded.node("file_version").getInt();
        int userVersion = user.node("file_version").getInt(0);

        // User file is already up-to-date
        if (userVersion >= embVersion) {
            return;
        }

        List<MenuFileUpdates> updates = MenuFileUpdates.getUpdates(menuName, userVersion, embVersion);
        if (updates.isEmpty()) return;

        int changed = 0;
        for (MenuFileUpdates update : updates) {
            List<String> addedItems = update.getAddedKeys().getOrDefault("items", new ArrayList<>());
            changed += updateConfigSection("items", embedded, user, addedItems);
            List<String> addedTemplates = update.getAddedKeys().getOrDefault("templates", new ArrayList<>());
            changed += updateConfigSection("templates", embedded, user, addedTemplates);
            List<String> addedComponents = update.getAddedKeys().getOrDefault("components", new ArrayList<>());
            changed += updateConfigSection("components", embedded, user, addedComponents);
            List<String> addedFormats = update.getAddedKeys().getOrDefault("formats", new ArrayList<>());
            changed += updateStringSection("formats", embedded, user, addedFormats);
        }

        user.node("file_version").set(embVersion);

        try {
            FileUtil.saveYamlFile(userFile, user);

            plugin.logger().info("Menu file " + userFile.getName() + " was updated: " + changed + " new sections added");
        } catch (IOException e) {
            plugin.logger().warn("Error saving menu file " + userFile.getName());
            e.printStackTrace();
        }
    }

    private int updateConfigSection(String name, ConfigurationNode embedded, ConfigurationNode user, List<String> keys) throws SerializationException {
        if (keys.isEmpty()) return 0;
        int changed = 0;
        if (!embedded.node(name).virtual() && !user.node(name).virtual()) {
            for (ConfigurationNode embSec : embedded.node(name).childrenMap().values()) {
                String key = (String) embSec.key();
                if (key == null) continue;
                if (!keys.contains(key)) continue; // Only update sections passed in the keys list
                if (!embSec.isMap()) continue;
                // User file does not have embedded key
                if (user.node(name).node(key).virtual()) {
                    user.node(name).node(key).set(embSec);
                    changed++;
                }
            }
        }
        return changed;
    }

    private int updateStringSection(String name, ConfigurationNode embedded, ConfigurationNode user, List<String> keys) throws SerializationException {
        if (keys.isEmpty()) return 0;
        int changed = 0;
        if (!embedded.node(name).virtual() && !user.node(name).virtual()) {
            for (ConfigurationNode embSec : embedded.node(name).childrenMap().values()) {
                String key = (String) embSec.key();
                if (key == null) continue;
                if (!keys.contains(key)) continue;

                String value = embSec.getString();
                if (value == null) continue;
                // User file does not have embedded key
                if (user.node(name).node(key).virtual()) {
                    user.node(name).node(key).set(value);
                    changed++;
                }
            }
        }
        return changed;
    }

    // === Fork skills: trading, husbandry, smithing ===
    private void mergeMissingTemplateKeys(ConfigurationNode embedded, ConfigurationNode user, File userFile) throws SerializationException {
        int changed = 0;
        for (ConfigurationNode template : embedded.node("templates").childrenMap().values()) {
            Object templateKey = template.key();
            if (templateKey == null) continue;
            changed += mergeMissingChildren(template.node("contexts"), user.node("templates", templateKey, "contexts"));
            changed += mergeMissingChildren(template.node("groups"), user.node("templates", templateKey, "groups"));
        }
        // Merged groups may reference rows beyond the user's menu size (e.g. fifth_row in a size 5 menu)
        boolean sizeChanged = ensureSizeFitsGroups(user);
        if (changed > 0 || sizeChanged) {
            try {
                FileUtil.saveYamlFile(userFile, user);
                String message = "Menu file " + userFile.getName() + " was updated:";
                if (changed > 0) {
                    message += " " + changed + " new template context(s)/group(s) added";
                }
                if (sizeChanged) {
                    message += (changed > 0 ? "," : "") + " size increased to fit merged groups";
                }
                plugin.logger().info(message);
            } catch (IOException e) {
                plugin.logger().warn("Error saving menu file " + userFile.getName());
                e.printStackTrace();
            }
        }
    }

    private boolean ensureSizeFitsGroups(ConfigurationNode user) throws SerializationException {
        ConfigurationNode sizeNode = user.node("size");
        if (sizeNode.virtual()) return false;
        int size = sizeNode.getInt(-1);
        if (size < 0) return false;
        int maxRow = -1;
        for (ConfigurationNode template : user.node("templates").childrenMap().values()) {
            for (ConfigurationNode group : template.node("groups").childrenMap().values()) {
                maxRow = Math.max(maxRow, parseRow(group.node("start").getString()));
                maxRow = Math.max(maxRow, parseRow(group.node("end").getString()));
            }
        }
        if (maxRow >= 0 && size < maxRow + 1) {
            sizeNode.set(maxRow + 1);
            return true;
        }
        return false;
    }

    private int parseRow(String pos) {
        if (pos == null) return -1;
        int comma = pos.indexOf(',');
        String row = (comma >= 0 ? pos.substring(0, comma) : pos).trim();
        try {
            return Integer.parseInt(row);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int mergeMissingChildren(ConfigurationNode embeddedSection, ConfigurationNode userSection) throws SerializationException {
        if (embeddedSection.virtual()) return 0;
        int changed = 0;
        for (ConfigurationNode child : embeddedSection.childrenMap().values()) {
            Object key = child.key();
            if (key == null) continue;
            if (userSection.node(key).virtual()) {
                userSection.node(key).set(child);
                changed++;
            }
        }
        return changed;
    }
    // === End fork skills ===

}
