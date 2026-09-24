# New Skills (Trading, Husbandry, Smithing) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 3 complete new skills (Trading, Husbandry, Smithing) to the AuraSkills fork, each with 5 abilities, 1 mana ability, own XP source types, rewards, menu entries, commands and messages.

**Architecture:** Follows the existing skill pattern exactly: enum constants (api) + config YAMLs (common resources) + source type stacks (api interface → common impl+parser → bukkit leveler) + ability listeners (bukkit) + mana ability providers (bukkit). All XP source detection uses Spigot-compatible events (the `bukkit` module compiles against `spigot-api`, NOT paper-api — `PlayerTradeEvent` is Paper-only and is therefore replaced by `InventoryClickEvent` on `MerchantInventory`, which works on both platforms).

**Tech Stack:** Java 21 toolchain, Gradle (`./gradlew`), Configurate YAML, Bukkit/Spigot API 26.1, JUnit 5 + MockBukkit (`mockbukkit-v1.21`) for tests.

**Spec:** `docs/superpowers/specs/2026-09-24-new-skills-design.md` — read it first; it defines values, anti-abuse rules and design decisions (e.g. anvil/grindstone XP stays in Enchanting).

## Global Constraints

- **Branch:** all work on `feature/fork-skills` in the fork (`origin` = github.com/oLucasPopov/AuraSkills.git). One commit per task minimum.
- **Merge-friendly rule (mandatory):** new code goes ONLY in new files. Edits to upstream files are append-only blocks at the end of the relevant section, marked `// === Fork skills: trading, husbandry, smithing ===` (Java) or `# === Fork skills ===` (YAML). Never reorder/rename/refactor upstream code.
- **Enum semicolon edits:** appending to `Skills.java`, `Abilities.java`, `ManaAbilities.java`, `SourceTypes.java`, `ManaAbilityMessage.java` requires changing the previous last entry's `;` to `,`. That single-character change on the last line is expected and allowed.
- **Ability conventions:** 5 abilities per skill; the 2nd ability in each `skills.yml` list is the XP-multiplier and is the one passed to the `Skills` enum constructor; `Abilities` constructor takes the legacy skill name string (e.g. `"trading"`); ability unlocks use `unlock: '{start}+N'` with N = position in the `skills.yml` abilities list.
- **Test stack:** tests live in `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/`, boot the full plugin via `MockBukkit.load(AuraSkills.class, TestSession.create())` (see `SkillManagerTest.java`), and must end with `MockBukkit.unmock()`.
- **Build:** `./gradlew build -x javadoc` must pass at the end of every task. Run focused tests with `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.<TestClass>"`.
- **Version range:** supported MC versions per `gradle.properties` are 1.20 through 26.x. All events used (`InventoryClickEvent`, `PiglinBarterEvent`, `EntityBreedEvent`, `EntityTameEvent`, `PlayerShearEntityEvent`, `SmithItemEvent`, `FurnaceExtractEvent`, `CraftItemEvent`) exist across the whole range. Material names that don't exist in older versions are filtered by `isVersionValid()` using `plugin.getPlatformUtil().isValidMaterial(...)`.
- **Mana ability providers** extend `ReadiedManaAbility` (constructor: `(AuraSkills plugin, ManaAbility, MessageKey startMsg, MessageKey endMsg, String[] heldItemMaterials, Action[] activationActions)`) and are registered in `BukkitManaAbilityManager.registerProviders()`.
- **Ability implementations** extend `BukkitAbilityImpl` (constructor: `super(plugin, Abilities.X, Abilities.Y, ...)`), use helpers `isDisabled(ability)`, `failsChecks(player, ability)`, `getValue(ability, user)`, `user.getAbilityLevel(ability)`, and the inherited `rand` (`Random`) field; registered in `BukkitAbilityManager.registerAbilityImplementations()`.
- **Levelers** extend `SourceLeveler` (constructor: `super(plugin, SourceTypes.X)`), listen at `EventPriority.MONITOR` with `ignoreCancelled = true`, start handlers with `if (disabled()) return;`, use `failsChecks(event, player, location, skill)` for region/anti-AFK checks, grant XP with `plugin.getLevelManager().addXp(user, skill, source, amount)`, and are registered in `BukkitLevelManager.registerLevelers()`.
- **Source display names** resolve from message key `sources.<source_type>.<source_key>` in `messages_en.yml` — every new source key needs an entry there.

## Task Map

| Task | Deliverable |
|------|-------------|
| 1 | Smithing: 3 source types (`smithing`, `smelting`, `crafting`) + full skill definition + loading test |
| 2 | Smithing: abilities implementation + `forge_overdrive` mana ability |
| 3 | Husbandry: source type (`breeding`) + full skill definition + loading test |
| 4 | Husbandry: abilities implementation + `animal_whisperer` mana ability |
| 5 | Trading: source type (`trading`) + full skill definition + loading test |
| 6 | Trading: abilities implementation + `grand_bargain` mana ability |
| 7 | Final integration: full build, menu/message version bumps, changelog, push |

---

### Task 1: Smithing — source types + skill definition

**Files:**
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/SmithingXpSource.java`
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/SmeltingXpSource.java`
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/CraftingXpSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/SmithingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/SmeltingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/CraftingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/SmithingSourceParser.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/SmeltingSourceParser.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/CraftingSourceParser.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/SmithingLeveler.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/SmeltingLeveler.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/CraftingLeveler.java`
- Create: `common/src/main/resources/sources/smithing.yml`
- Create: `common/src/main/resources/rewards/smithing.yml`
- Create: `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/SmithingSkillTest.java`
- Modify (append-only, banner): `api/.../api/skill/Skills.java`, `api/.../api/ability/Abilities.java`, `api/.../api/mana/ManaAbilities.java`, `common/.../source/SourceTypes.java`, `common/.../message/type/ManaAbilityMessage.java`, `bukkit/.../level/BukkitLevelManager.java`, `bukkit/.../commands/SkillCommands.java`, `bukkit/.../commands/CommandRegistrar.java`
- Modify (append-only, banner): `common/src/main/resources/skills.yml`, `common/src/main/resources/abilities.yml`, `common/src/main/resources/mana_abilities.yml`, `common/src/main/resources/messages/messages_en.yml`, `bukkit/src/main/resources/menus/skills.yml`

**Interfaces:**
- Consumes: nothing from other tasks (first task).
- Produces (relied on by Task 2 and later):
  - `Skills.SMITHING`, `Abilities.EFFICIENT_SMELTING/SMITH/MASTER_CRAFTED/RECYCLER/FORGE_MASTERY`, `ManaAbilities.FORGE_OVERDRIVE`
  - `SourceTypes.SMITHING/SMELTING/CRAFTING`
  - `SmithingXpSource` methods: `String[] getResults()`, `String[] getTemplates()`, `String[] getBases()`, `String[] getAdditions()`
  - `SmeltingXpSource` method: `String[] getItems()`; `CraftingXpSource` method: `String[] getItems()`
  - `CraftingLeveler.getAmountCrafted(CraftItemEvent)` (package-visible static, reused in tests)

- [ ] **Step 1: Create the branch**

```bash
git checkout -b feature/fork-skills
```

- [ ] **Step 2: Write the failing loading test**

Create `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/SmithingSkillTest.java`:

```java
package dev.aurelium.auraskills.bukkit.skill;

import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.CraftingXpSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.util.TestSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmithingSkillTest {

    private static AuraSkills plugin;

    @BeforeAll
    static void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(AuraSkills.class, TestSession.create());
        server.getScheduler().performOneTick();
    }

    @AfterAll
    static void unload() {
        MockBukkit.unmock();
    }

    @Test
    void testSmithingSkillLoads() {
        assertTrue(Skills.SMITHING.isEnabled());
        assertTrue(Skills.SMITHING.getAbilities().size() == 5);
    }

    @Test
    void testSmithingSourcesLoad() {
        assertFalse(plugin.getSkillManager().getSourcesOfType(SmithingXpSource.class).isEmpty());
        assertFalse(plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class).isEmpty());
        List<SkillSource<CraftingXpSource>> crafting = plugin.getSkillManager().getSourcesOfType(CraftingXpSource.class);
        assertFalse(crafting.isEmpty());
        // Every loaded source must grant XP
        assertTrue(crafting.stream().allMatch(s -> s.source().getXp() > 0));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.SmithingSkillTest"`
Expected: compilation FAILURE (`Skills.SMITHING` and the new source interfaces do not exist).

- [ ] **Step 4: Create the API source type interfaces**

`api/src/main/java/dev/aurelium/auraskills/api/source/type/SmithingXpSource.java`:

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface SmithingXpSource extends XpSource {

    /** Result item materials that match; empty array means any result. */
    String[] getResults();

    /** Template materials (e.g. netherite_upgrade, armor trims); empty means any. */
    String[] getTemplates();

    /** Base equipment materials; empty means any. */
    String[] getBases();

    /** Addition materials (e.g. netherite_ingot); empty means any. */
    String[] getAdditions();

}
```

`api/src/main/java/dev/aurelium/auraskills/api/source/type/SmeltingXpSource.java`:

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface SmeltingXpSource extends XpSource {

    /** Smelted result item materials. */
    String[] getItems();

}
```

`api/src/main/java/dev/aurelium/auraskills/api/source/type/CraftingXpSource.java`:

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface CraftingXpSource extends XpSource {

    /** Crafted result item materials. */
    String[] getItems();

}
```

- [ ] **Step 5: Create the common impls**

`common/src/main/java/dev/aurelium/auraskills/common/source/type/SmithingSource.java`:

```java
package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;
import dev.aurelium.auraskills.common.source.SourceValues;

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
```

`common/src/main/java/dev/aurelium/auraskills/common/source/type/SmeltingSource.java`:

```java
package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;
import dev.aurelium.auraskills.common.source.SourceValues;

public class SmeltingSource extends Source implements SmeltingXpSource {

    private final String[] items;

    public SmeltingSource(AuraSkillsPlugin plugin, SourceValues values, String[] items) {
        super(plugin, values);
        this.items = items;
    }

    @Override
    public String[] getItems() {
        return items;
    }

    @Override
    public boolean isVersionValid() {
        for (String item : items) {
            if (!plugin.getPlatformUtil().isValidMaterial(item)) {
                return false;
            }
        }
        return true;
    }
}
```

`common/src/main/java/dev/aurelium/auraskills/common/source/type/CraftingSource.java` — identical in shape to `SmeltingSource`, but implements `CraftingXpSource` (class name `CraftingSource`, same fields/methods).

- [ ] **Step 6: Create the parsers**

`common/src/main/java/dev/aurelium/auraskills/common/source/parser/SmithingSourceParser.java`:

```java
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
```

`SmeltingSourceParser.java`:

```java
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
```

`CraftingSourceParser.java` — identical in shape, building `CraftingSource`.

- [ ] **Step 7: Register the source types**

In `common/src/main/java/dev/aurelium/auraskills/common/source/SourceTypes.java`, change the trailing `STATISTIC(StatisticSourceParser.class);` to end with `,` and append:

```java
    STATISTIC(StatisticSourceParser.class),
    // === Fork skills: trading, husbandry, smithing ===
    SMITHING(SmithingSourceParser.class),
    SMELTING(SmeltingSourceParser.class),
    CRAFTING(CraftingSourceParser.class);
```

(`SourceTypeRegistry.registerDefaults()` loops `SourceTypes.values()` — no edit needed there.)

- [ ] **Step 8: Create the levelers**

`bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/SmeltingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class SmeltingLeveler extends SourceLeveler {

    public SmeltingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMELTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtract(FurnaceExtractEvent event) {
        if (disabled()) return;

        Player player = event.getPlayer();
        String itemName = event.getItemType().name();

        for (SkillSource<SmeltingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class)) {
            SmeltingXpSource source = entry.source();
            if (!matchesAny(source.getItems(), itemName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, event.getBlock().getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * event.getItemAmount());
            return;
        }
    }

    static boolean matchesAny(String[] candidates, String materialName) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(materialName)) return true;
        }
        return false;
    }
}
```

`bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/CraftingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.CraftingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingLeveler extends SourceLeveler {

    public CraftingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.CRAFTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        String itemName = result.getType().name();

        for (SkillSource<CraftingXpSource> entry : plugin.getSkillManager().getSourcesOfType(CraftingXpSource.class)) {
            CraftingXpSource source = entry.source();
            if (!SmeltingLeveler.matchesAny(source.getItems(), itemName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            int amount = getAmountCrafted(event);
            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * amount);
            return;
        }
    }

    static int getAmountCrafted(CraftItemEvent event) {
        int resultAmount = event.getRecipe().getResult().getAmount();
        if (!event.isShiftClick()) {
            return resultAmount;
        }
        int maxCrafts = Integer.MAX_VALUE;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null || ingredient.getType().isAir()) continue;
            maxCrafts = Math.min(maxCrafts, ingredient.getAmount());
        }
        if (maxCrafts == Integer.MAX_VALUE || maxCrafts < 1) {
            return resultAmount;
        }
        return resultAmount * maxCrafts;
    }
}
```

`bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/SmithingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmithingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class SmithingLeveler extends SourceLeveler {

    public SmithingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMITHING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        SmithingInventory inventory = event.getInventory();
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        String resultName = result.getType().name();
        String templateName = nameOrNull(inventory.getInputTemplate());
        String baseName = nameOrNull(inventory.getInputEquipment());
        String additionName = nameOrNull(inventory.getInputMineral());

        for (SkillSource<SmithingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmithingXpSource.class)) {
            SmithingXpSource source = entry.source();
            if (!matches(source, resultName, templateName, baseName, additionName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * result.getAmount());
            return;
        }
    }

    private boolean matches(SmithingXpSource source, String result, String template, String base, String addition) {
        if (source.getResults().length > 0 && (result == null || !SmeltingLeveler.matchesAny(source.getResults(), result))) return false;
        if (source.getTemplates().length > 0 && (template == null || !SmeltingLeveler.matchesAny(source.getTemplates(), template))) return false;
        if (source.getBases().length > 0 && (base == null || !SmeltingLeveler.matchesAny(source.getBases(), base))) return false;
        return source.getAdditions().length == 0 || (addition != null && SmeltingLeveler.matchesAny(source.getAdditions(), addition));
    }

    private static String nameOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.getType().name();
    }
}
```

Register all three in `bukkit/.../level/BukkitLevelManager.java` `registerLevelers()` — append after `registerLeveler(new StatisticLeveler(plugin));`:

```java
        // === Fork skills: trading, husbandry, smithing ===
        registerLeveler(new SmithingLeveler(plugin));
        registerLeveler(new SmeltingLeveler(plugin));
        registerLeveler(new CraftingLeveler(plugin));
```

- [ ] **Step 9: Create the XP sources config**

Create `common/src/main/resources/sources/smithing.yml`:

```yaml
sources:
  netherite_upgrade:
    type: smithing
    addition: netherite_ingot
    xp: 50.0
    menu_item:
      material: netherite_chestplate
  armor_trim:
    type: smithing
    templates:
      - sentry_armor_trim_smithing_template
      - vex_armor_trim_smithing_template
      - wild_armor_trim_smithing_template
      - coast_armor_trim_smithing_template
      - dune_armor_trim_smithing_template
      - ward_armor_trim_smithing_template
      - tide_armor_trim_smithing_template
      - snout_armor_trim_smithing_template
      - rib_armor_trim_smithing_template
      - eye_armor_trim_smithing_template
      - spire_armor_trim_smithing_template
      - wayfinder_armor_trim_smithing_template
      - shaper_armor_trim_smithing_template
      - raiser_armor_trim_smithing_template
      - host_armor_trim_smithing_template
      - silence_armor_trim_smithing_template
      - flow_armor_trim_smithing_template
      - bolt_armor_trim_smithing_template
    xp: 15.0
    menu_item:
      material: silence_armor_trim_smithing_template
  smelt_ores:
    type: smelting
    items:
      - iron_ingot
      - gold_ingot
      - copper_ingot
      - netherite_scrap
      - ancient_debris
    xp: 4.0
    menu_item:
      material: iron_ingot
  smelt_food:
    type: smelting
    items:
      - cooked_beef
      - cooked_porkchop
      - cooked_chicken
      - cooked_mutton
      - cooked_rabbit
      - cooked_cod
      - cooked_salmon
      - baked_potato
      - dried_kelp
    xp: 2.0
    menu_item:
      material: cooked_beef
  smelt_misc:
    type: smelting
    items:
      - glass
      - stone
      - smooth_stone
      - charcoal
      - brick
      - nether_brick
      - terracotta
    xp: 1.0
    menu_item:
      material: glass
  craft_diamond_equipment:
    type: crafting
    items:
      - diamond_sword
      - diamond_pickaxe
      - diamond_axe
      - diamond_shovel
      - diamond_hoe
      - diamond_helmet
      - diamond_chestplate
      - diamond_leggings
      - diamond_boots
      - diamond_spear
    xp: 25.0
    menu_item:
      material: diamond_sword
  craft_iron_equipment:
    type: crafting
    items:
      - iron_sword
      - iron_pickaxe
      - iron_axe
      - iron_shovel
      - iron_hoe
      - iron_helmet
      - iron_chestplate
      - iron_leggings
      - iron_boots
      - iron_spear
      - shield
    xp: 10.0
    menu_item:
      material: iron_chestplate
  craft_golden_equipment:
    type: crafting
    items:
      - golden_sword
      - golden_pickaxe
      - golden_axe
      - golden_shovel
      - golden_hoe
      - golden_helmet
      - golden_chestplate
      - golden_leggings
      - golden_boots
      - golden_spear
    xp: 12.0
    menu_item:
      material: golden_helmet
  craft_stone_equipment:
    type: crafting
    items:
      - stone_sword
      - stone_pickaxe
      - stone_axe
      - stone_shovel
      - stone_hoe
      - stone_spear
    xp: 3.0
    menu_item:
      material: stone_pickaxe
  craft_wooden_equipment:
    type: crafting
    items:
      - wooden_sword
      - wooden_pickaxe
      - wooden_axe
      - wooden_shovel
      - wooden_hoe
      - wooden_spear
      - leather_helmet
      - leather_chestplate
      - leather_leggings
      - leather_boots
      - bow
      - crossbow
      - fishing_rod
    xp: 2.0
    menu_item:
      material: wooden_pickaxe
```

- [ ] **Step 10: Enum entries + skill definition**

In `api/.../api/skill/Skills.java`, change `FORGING(Abilities.FORGER);` to end with `,` and append:

```java
    FORGING(Abilities.FORGER),
    // === Fork skills: trading, husbandry, smithing ===
    SMITHING(Abilities.SMITH);
```

In `api/.../api/ability/Abilities.java`, change the last entry (`SKILL_MENDER("forging");`) to end with `,` and append:

```java
    SKILL_MENDER("forging"),
    // === Fork skills: trading, husbandry, smithing ===
    EFFICIENT_SMELTING("smithing"),
    SMITH("smithing"),
    MASTER_CRAFTED("smithing"),
    RECYCLER("smithing"),
    FORGE_MASTERY("smithing");
```

In `api/.../api/mana/ManaAbilities.java`, change `LIGHTNING_BLADE;` to `LIGHTNING_BLADE,` and append:

```java
    LIGHTNING_BLADE,
    // === Fork skills: trading, husbandry, smithing ===
    FORGE_OVERDRIVE;
```

In `common/.../message/type/ManaAbilityMessage.java`, change the last entry's trailing `;` to `,` and append (mirroring the existing `NAME_START, NAME_END` convention):

```java
    // === Fork skills: trading, husbandry, smithing ===
    FORGE_OVERDRIVE_START,
    FORGE_OVERDRIVE_END;
```

(Verify the enum's trailing syntax while editing — if the last entry already ends with `;` on its own line, apply the same pattern.)

Append to `common/src/main/resources/skills.yml` (at the end of the top-level skills map, same 2-space indentation as the other `auraskills/` entries):

```yaml
  # === Fork skills ===
  auraskills/smithing:
    abilities:
      - auraskills/efficient_smelting
      - auraskills/smith
      - auraskills/master_crafted
      - auraskills/recycler
      - auraskills/forge_mastery
    mana_ability: auraskills/forge_overdrive
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

Append to `common/src/main/resources/abilities.yml`:

```yaml
  # === Fork skills ===
  auraskills/efficient_smelting:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+1'
    level_up: 5
    max_level: 14
  auraskills/smith:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/master_crafted:
    enabled: true
    base_value: 5.0
    value_per_level: 5.0
    unlock: '{start}+3'
    level_up: 5
    max_level: 10
  auraskills/recycler:
    enabled: true
    base_value: 3.0
    value_per_level: 3.0
    unlock: '{start}+4'
    level_up: 5
    max_level: 7
  auraskills/forge_mastery:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+5'
    level_up: 5
    max_level: 0
```

Append to `common/src/main/resources/mana_abilities.yml`:

```yaml
  # === Fork skills ===
  auraskills/forge_overdrive:
    enabled: true
    base_value: 15.0
    value_per_level: 3.0
    base_cooldown: 300
    cooldown_per_level: -5
    base_mana_cost: 50
    mana_cost_per_level: 5
    unlock: 6
    level_up: 6
    max_level: 0
    require_sneak: false
    check_offhand: true
    sneak_offhand_bypass: true
    radius: 10
```

Create `common/src/main/resources/rewards/smithing.yml`:

```yaml
patterns:
  - type: stat
    stat: toughness
    value: 1
    pattern:
      interval: 1
  - type: stat
    stat: strength
    value: 1
    pattern:
      interval: 2
levels:
```

- [ ] **Step 11: Messages**

Append to `common/src/main/resources/messages/messages_en.yml` under the existing top-level `skills:` map:

```yaml
  # === Fork skills ===
  smithing:
    name: Smithing
    desc: "Forge, smelt and craft equipment to earn Smithing {xp_unit}"
```

Under the top-level `abilities:` map:

```yaml
  # === Fork skills ===
  efficient_smelting:
    name: Efficient Smelting
    desc: "Grants a {value}% chance to get double results when extracting items from a furnace."
    info: "+{value}% Double Smelt Chance"
  smith:
    name: Smith
    desc: "Grants +{value}% Smithing {xp_unit}."
    info: "+{value}% Smithing {xp_unit}"
  master_crafted:
    name: Master Crafted
    desc: "Grants a {value}% chance for equipment you craft to come with Unbreaking already applied."
    info: "+{value}% Master Crafted Chance"
  recycler:
    name: Recycler
    desc: "Grants a {value}% chance to recover materials consumed in a smithing table."
    info: "+{value}% Material Recovery Chance"
  forge_mastery:
    name: Forge Mastery
    desc: "Reduces the experience level cost of anvil recipes by {value}% (max 40%)."
    info: "-{value}% Anvil Cost"
```

Under the top-level `mana_abilities:` map:

```yaml
  # === Fork skills ===
  forge_overdrive:
    name: Forge Overdrive
    desc: "For {value} seconds, furnaces within {radius} blocks smelt twice as fast and your manual extracts are doubled. <1>[Right click coal or charcoal to activate]"
    raise: "<gray>You ignite a piece of coal"
    lower: "<gray>You extinguish the coal"
    start: "<gold>Forge Overdrive Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>Forge Overdrive has worn off"
```

Under the top-level `sources:` map:

```yaml
  # === Fork skills ===
  smithing:
    netherite_upgrade: Netherite Upgrade
    armor_trim: Apply Armor Trim
  smelting:
    smelt_ores: Smelt Ores
    smelt_food: Cook Food
    smelt_misc: Smelt Materials
  crafting:
    craft_diamond_equipment: Craft Diamond Equipment
    craft_iron_equipment: Craft Iron Equipment
    craft_golden_equipment: Craft Golden Equipment
    craft_stone_equipment: Craft Stone Equipment
    craft_wooden_equipment: Craft Basic Equipment
```

- [ ] **Step 12: Menu + command**

In `bukkit/src/main/resources/menus/skills.yml`, inside `templates.skill.contexts:`, append after the last context entry:

```yaml
      # === Fork skills ===
      smithing:
        group: fourth_row
        order: 3
        material: netherite_upgrade_smithing_template
```

and inside `templates.skill.groups:`, append:

```yaml
      # === Fork skills ===
      fourth_row:
        start: 4,0
        end: 4,8
        align: center
```

(The menu has `size: 5`, so row index 4 exists and is unused.)

In `bukkit/.../commands/SkillCommands.java`, append an inner class next to the other skill commands:

```java
    // === Fork skills: trading, husbandry, smithing ===
    @CommandAlias("smithing")
    public static class SmithingCommand extends SkillCommand {

        public SmithingCommand(AuraSkills plugin) {
            super(plugin, Skills.SMITHING);
        }

        @Default
        public void onCommand(Player player) {
            openMenu(player);
        }

    }
```

In `bukkit/.../commands/CommandRegistrar.java` `registerSkillCommands(...)`, append:

```java
            // === Fork skills: trading, husbandry, smithing ===
            registerSkillCommand(new SkillCommands.SmithingCommand(plugin), map, manager);
```

- [ ] **Step 13: Run the test**

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.SmithingSkillTest"`
Expected: PASS. If a source fails to parse, the plugin log in test output names the offending key — fix the YAML and re-run.

- [ ] **Step 14: Full build + commit**

Run: `./gradlew build -x javadoc`
Expected: BUILD SUCCESSFUL (checkstyle included).

```bash
git add -A
git commit -m "Add Smithing skill definition with smithing, smelting and crafting XP sources"
```

---

### Task 2: Smithing — abilities + Forge Overdrive

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/smithing/SmithingAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/smithing/ForgeOverdrive.java`
- Modify (append-only, banner): `bukkit/.../ability/BukkitAbilityManager.java`, `bukkit/.../mana/BukkitManaAbilityManager.java`

**Interfaces:**
- Consumes: `Skills.SMITHING`, `Abilities.EFFICIENT_SMELTING/SMITH/MASTER_CRAFTED/RECYCLER/FORGE_MASTERY`, `ManaAbilities.FORGE_OVERDRIVE`, `ManaAbilityMessage.FORGE_OVERDRIVE_START/END` (Task 1).
- Produces: `SmithingAbilities` and `ForgeOverdrive` classes (used only via registration; nothing else depends on them).

Note on activation state: `ReadiedManaAbility` providers expose `isActivated(Player)` — check an existing provider that reacts to its own active state (e.g. how other code queries activation) and mirror it. If the manager-level accessor differs, use the one found in the codebase; do not invent a new mechanism.

- [ ] **Step 1: Write SmithingAbilities**

```java
package dev.aurelium.auraskills.bukkit.skills.smithing;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class SmithingAbilities extends BukkitAbilityImpl {

    public SmithingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.EFFICIENT_SMELTING, Abilities.SMITH, Abilities.MASTER_CRAFTED, Abilities.RECYCLER, Abilities.FORGE_MASTERY);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void efficientSmelting(FurnaceExtractEvent event) {
        var ability = Abilities.EFFICIENT_SMELTING;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        boolean guaranteedDouble = isOverdriveActive(player);
        if (!guaranteedDouble && rand.nextDouble() >= getValue(ability, user) / 100) return;

        ItemStack extra = new ItemStack(event.getItemType(), event.getItemAmount());
        giveOrDrop(player, extra);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void masterCrafted(CraftItemEvent event) {
        var ability = Abilities.MASTER_CRAFTED;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !isEquipment(result.getType())) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        int abilityLevel = user.getAbilityLevel(ability);
        if (abilityLevel <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        int enchantLevel = Math.min(3, 1 + abilityLevel / 5);
        result.addUnsafeEnchantment(Enchantment.UNBREAKING, enchantLevel);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void recycler(SmithItemEvent event) {
        var ability = Abilities.RECYCLER;
        if (isDisabled(ability)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        SmithingInventory inventory = event.getInventory();
        ItemStack addition = inventory.getInputMineral();
        if (addition == null || addition.getType().isAir()) return;

        giveOrDrop(player, new ItemStack(addition.getType(), 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void forgeMastery(PrepareAnvilEvent event) {
        var ability = Abilities.FORGE_MASTERY;
        if (isDisabled(ability)) return;

        AnvilInventory inventory = event.getInventory();
        int cost = inventory.getRepairCost();
        if (cost <= 0) return;
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        double reduction = Math.min(40.0, getValue(ability, user)) / 100;
        inventory.setRepairCost((int) Math.max(0, Math.floor(cost * (1 - reduction))));
    }

    private boolean isOverdriveActive(Player player) {
        return plugin.getManaAbilityManager().isActivated(player, ManaAbilities.FORGE_OVERDRIVE);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    static boolean isEquipment(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.endsWith("_SPEAR") || name.endsWith("_MACE") || material == Material.SHIELD
                || material == Material.BOW || material == Material.CROSSBOW || material == Material.TRIDENT;
    }
}
```

- [ ] **Step 2: Write ForgeOverdrive**

```java
package dev.aurelium.auraskills.bukkit.skills.smithing;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeOverdrive extends ReadiedManaAbility {

    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public ForgeOverdrive(AuraSkills plugin) {
        super(plugin, ManaAbilities.FORGE_OVERDRIVE, ManaAbilityMessage.FORGE_OVERDRIVE_START, ManaAbilityMessage.FORGE_OVERDRIVE_END,
                new String[]{"COAL", "CHARCOAL"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
        int radius = manaAbility.optionInt("radius", 10);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> accelerateFurnaces(player, radius), 2L, 2L);
        tasks.put(player.getUniqueId(), task);
    }

    @Override
    public void onStop(Player player, User user) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void accelerateFurnaces(Player player, int radius) {
        if (!isActivated(player)) {
            onStop(player, plugin.getUser(player));
            return;
        }
        int px = player.getLocation().getBlockX();
        int py = player.getLocation().getBlockY();
        int pz = player.getLocation().getBlockZ();
        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = Math.max(player.getWorld().getMinHeight(), py - radius); y <= Math.min(player.getWorld().getMaxHeight(), py + radius); y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    if (!(block.getState() instanceof Furnace furnace)) continue;
                    if (!furnace.isBurning()) continue;
                    // +1 cook progress per 2 ticks on top of vanilla = ~1.5-2x effective speed
                    furnace.setCookTime((short) Math.min(furnace.getCookTimeTotal() - 1, furnace.getCookTime() + 1));
                    furnace.update();
                }
            }
        }
    }
}
```

If the project scheduler abstraction (`plugin.getScheduler()`) exposes a repeating-task method, prefer it over `Bukkit.getScheduler().runTaskTimer` for Folia compatibility — check how other repeating tasks in the codebase are scheduled and match that idiom.

- [ ] **Step 3: Register implementations**

In `bukkit/.../ability/BukkitAbilityManager.java` `registerAbilityImplementations()`, append:

```java
        // === Fork skills: trading, husbandry, smithing ===
        registerAbilityImpl(new SmithingAbilities(plugin));
```

In `bukkit/.../mana/BukkitManaAbilityManager.java` `registerProviders()`, append:

```java
        // === Fork skills: trading, husbandry, smithing ===
        registerProvider(new ForgeOverdrive(plugin));
```

- [ ] **Step 4: Add a unit test for the equipment classifier**

Append a test to `SmithingSkillTest.java`:

```java
    @Test
    void testIsEquipment() {
        assertTrue(dev.aurelium.auraskills.bukkit.skills.smithing.SmithingAbilities.isEquipment(org.bukkit.Material.DIAMOND_SWORD));
        assertTrue(dev.aurelium.auraskills.bukkit.skills.smithing.SmithingAbilities.isEquipment(org.bukkit.Material.IRON_CHESTPLATE));
        org.junit.jupiter.api.Assertions.assertFalse(dev.aurelium.auraskills.bukkit.skills.smithing.SmithingAbilities.isEquipment(org.bukkit.Material.STICK));
    }
```

- [ ] **Step 5: Build + commit**

Run: `./gradlew build -x javadoc` — Expected: BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "Implement Smithing abilities and Forge Overdrive mana ability"
```

---

### Task 3: Husbandry — source type + skill definition

**Files:**
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/BreedingXpSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/BreedingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/BreedingSourceParser.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/BreedingLeveler.java`
- Create: `common/src/main/resources/sources/husbandry.yml`
- Create: `common/src/main/resources/rewards/husbandry.yml`
- Create: `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/HusbandrySkillTest.java`
- Modify (append-only, banner): `Skills.java`, `Abilities.java`, `ManaAbilities.java`, `SourceTypes.java`, `ManaAbilityMessage.java`, `BukkitLevelManager.java`, `SkillCommands.java`, `CommandRegistrar.java`, `skills.yml`, `abilities.yml`, `mana_abilities.yml`, `messages_en.yml`, `menus/skills.yml`

**Interfaces:**
- Consumes: the fork-skill append blocks from Tasks 1-2 (same files, new blocks).
- Produces: `Skills.HUSBANDRY`, `Abilities.TWINS/RANCHER/HEALTHY_GROWTH/GENTLE_HANDS/BOUNTIFUL_PASTURE`, `ManaAbilities.ANIMAL_WHISPERER`, `SourceTypes.BREEDING`, `BreedingXpSource` with `String[] getEntities()`, `BreedingTrigger getTrigger()`, `enum BreedingTrigger { BREED, TAME, SHEAR, MILK }`.

- [ ] **Step 1: Write the failing test**

Create `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/HusbandrySkillTest.java` — same skeleton as `SmithingSkillTest`, with:

```java
    @Test
    void testHusbandrySkillLoads() {
        assertTrue(Skills.HUSBANDRY.isEnabled());
        assertTrue(Skills.HUSBANDRY.getAbilities().size() == 5);
    }

    @Test
    void testBreedingSourcesLoad() {
        List<SkillSource<BreedingXpSource>> sources = plugin.getSkillManager().getSourcesOfType(BreedingXpSource.class);
        assertFalse(sources.isEmpty());
        assertTrue(sources.stream().allMatch(s -> s.source().getXp() > 0));
        // All four triggers must be represented
        for (BreedingXpSource.BreedingTrigger trigger : BreedingXpSource.BreedingTrigger.values()) {
            assertTrue(sources.stream().anyMatch(s -> s.source().getTrigger() == trigger), "Missing trigger " + trigger);
        }
    }
```

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.HusbandrySkillTest"` — Expected: compilation FAILURE.

- [ ] **Step 2: API interface**

`api/src/main/java/dev/aurelium/auraskills/api/source/type/BreedingXpSource.java`:

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface BreedingXpSource extends XpSource {

    /** Entity types (e.g. cow, wolf); empty means any entity of the trigger. */
    String[] getEntities();

    BreedingTrigger getTrigger();

    enum BreedingTrigger {

        BREED,
        TAME,
        SHEAR,
        MILK

    }

}
```

- [ ] **Step 3: Common impl**

`common/src/main/java/dev/aurelium/auraskills/common/source/type/BreedingSource.java`:

```java
package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.type.BreedingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;
import dev.aurelium.auraskills.common.source.SourceValues;

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
            if (!plugin.getPlatformUtil().isValidEntity(entity)) {
                return false;
            }
        }
        return true;
    }
}
```

(If `PlatformUtil` has no `isValidEntity`, find the equivalent validation used by `EntitySource` and mirror it; if none exists, return `true`.)

- [ ] **Step 4: Parser**

`common/src/main/java/dev/aurelium/auraskills/common/source/parser/BreedingSourceParser.java`:

```java
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
```

Register in `SourceTypes.java`: change `CRAFTING(CraftingSourceParser.class);` to `,` and append `BREEDING(BreedingSourceParser.class);` inside the fork-skills banner block.

- [ ] **Step 5: Leveler**

`bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/BreedingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.BreedingXpSource;
import dev.aurelium.auraskills.api.source.type.BreedingXpSource.BreedingTrigger;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BreedingLeveler extends SourceLeveler {

    private static final long PARENT_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final long MILK_COOLDOWN_MS = 60 * 1000L;

    private final Map<String, Long> parentPairCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> milkCooldowns = new ConcurrentHashMap<>();

    public BreedingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.BREEDING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (disabled()) return;
        if (!(event.getBreeder() instanceof Player player)) return;
        if (isParentPairOnCooldown(event.getFather(), event.getMother())) return;

        grant(player, event.getEntity(), BreedingTrigger.BREED, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (disabled()) return;
        if (!(event.getOwner() instanceof Player player)) return;

        grant(player, event.getEntity(), BreedingTrigger.TAME, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (disabled()) return;

        grant(event.getPlayer(), event.getEntity(), BreedingTrigger.SHEAR, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMilk(PlayerInteractEntityEvent event) {
        if (disabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        EntityType type = entity.getType();
        if (type != EntityType.COW && type != EntityType.GOAT && type != EntityType.MOOSHROOM) return;

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (held.getType() != Material.BUCKET && held.getType() != Material.BOWL) return;
        if (held.getType() == Material.BOWL && type != EntityType.MOOSHROOM) return;
        if (entity instanceof Animals animals && animals.isBaby()) return;

        long now = System.currentTimeMillis();
        Long last = milkCooldowns.get(entity.getUniqueId());
        if (last != null && now - last < MILK_COOLDOWN_MS) return;
        milkCooldowns.put(entity.getUniqueId(), now);

        grant(event.getPlayer(), entity, BreedingTrigger.MILK, event);
    }

    private void grant(Player player, Entity entity, BreedingTrigger trigger, org.bukkit.event.Cancellable event) {
        String entityName = entity.getType().name();
        for (SkillSource<BreedingXpSource> entry : plugin.getSkillManager().getSourcesOfType(BreedingXpSource.class)) {
            BreedingXpSource source = entry.source();
            if (source.getTrigger() != trigger) continue;
            if (source.getEntities().length > 0 && !SmeltingLeveler.matchesAny(source.getEntities(), entityName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, entity.getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp());
            return;
        }
    }

    private boolean isParentPairOnCooldown(LivingEntity father, LivingEntity mother) {
        String key = father.getUniqueId() + ":" + mother.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = parentPairCooldowns.get(key);
        if (last != null && now - last < PARENT_COOLDOWN_MS) {
            return true;
        }
        parentPairCooldowns.put(key, now);
        return false;
    }
}
```

(`AbstractHorse` import is unused — drop it; `Animals` covers horses for breeding.)

Register in `BukkitLevelManager.registerLevelers()` — append inside the fork banner block:

```java
        registerLeveler(new BreedingLeveler(plugin));
```

- [ ] **Step 6: XP sources config**

Create `common/src/main/resources/sources/husbandry.yml`:

```yaml
sources:
  breed_common:
    type: breeding
    trigger: breed
    entities:
      - cow
      - sheep
      - pig
      - chicken
      - rabbit
      - goat
      - frog
    xp: 8.0
    menu_item:
      material: wheat
  breed_horse:
    type: breeding
    trigger: breed
    entities:
      - horse
      - donkey
      - llama
    xp: 20.0
    menu_item:
      material: golden_carrot
  breed_mule:
    type: breeding
    trigger: breed
    entity: mule
    xp: 40.0
    menu_item:
      material: saddle
  breed_rare:
    type: breeding
    trigger: breed
    entities:
      - axolotl
      - turtle
      - sniffer
      - camel
      - panda
      - fox
      - bee
    xp: 35.0
    menu_item:
      material: lily_pad
  tame_wolf:
    type: breeding
    trigger: tame
    entity: wolf
    xp: 25.0
    menu_item:
      material: bone
  tame_cat:
    type: breeding
    trigger: tame
    entity: cat
    xp: 25.0
    menu_item:
      material: raw_cod
  tame_parrot:
    type: breeding
    trigger: tame
    entity: parrot
    xp: 30.0
    menu_item:
      material: wheat_seeds
  tame_horse:
    type: breeding
    trigger: tame
    entities:
      - horse
      - donkey
      - llama
    xp: 20.0
    menu_item:
      material: apple
  shear_sheep:
    type: breeding
    trigger: shear
    entity: sheep
    xp: 5.0
    menu_item:
      material: white_wool
  shear_mooshroom:
    type: breeding
    trigger: shear
    entity: mooshroom
    xp: 8.0
    menu_item:
      material: red_mushroom
  shear_snow_golem:
    type: breeding
    trigger: shear
    entity: snow_golem
    xp: 5.0
    menu_item:
      material: carved_pumpkin
  milk_animal:
    type: breeding
    trigger: milk
    entities:
      - cow
      - goat
      - mooshroom
    xp: 3.0
    menu_item:
      material: milk_bucket
```

- [ ] **Step 7: Enums + skill definition**

`Skills.java` — change `SMITHING(Abilities.SMITH);` to `,` and append `HUSBANDRY(Abilities.RANCHER);` (keep the fork banner comment above the block).

`Abilities.java` — change `FORGE_MASTERY("smithing");` to `,` and append:

```java
    TWINS("husbandry"),
    RANCHER("husbandry"),
    HEALTHY_GROWTH("husbandry"),
    GENTLE_HANDS("husbandry"),
    BOUNTIFUL_PASTURE("husbandry");
```

`ManaAbilities.java` — change `FORGE_OVERDRIVE;` to `,` and append `ANIMAL_WHISPERER;`.

`ManaAbilityMessage.java` — change `FORGE_OVERDRIVE_END;` to `,` and append:

```java
    ANIMAL_WHISPERER_START,
    ANIMAL_WHISPERER_END;
```

Append to `skills.yml` (inside the fork block):

```yaml
  auraskills/husbandry:
    abilities:
      - auraskills/twins
      - auraskills/rancher
      - auraskills/healthy_growth
      - auraskills/gentle_hands
      - auraskills/bountiful_pasture
    mana_ability: auraskills/animal_whisperer
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

Append to `abilities.yml`:

```yaml
  auraskills/twins:
    enabled: true
    base_value: 3.0
    value_per_level: 3.0
    unlock: '{start}+1'
    level_up: 5
    max_level: 10
  auraskills/rancher:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/healthy_growth:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+3'
    level_up: 5
    max_level: 0
  auraskills/gentle_hands:
    enabled: true
    base_value: 0.5
    value_per_level: 0.5
    unlock: '{start}+4'
    level_up: 5
    max_level: 10
    speed_boost_per_level: 2.0
  auraskills/bountiful_pasture:
    enabled: true
    base_value: 5.0
    value_per_level: 5.0
    unlock: '{start}+5'
    level_up: 5
    max_level: 0
```

Append to `mana_abilities.yml`:

```yaml
  auraskills/animal_whisperer:
    enabled: true
    base_value: 15.0
    value_per_level: 3.0
    base_cooldown: 300
    cooldown_per_level: -5
    base_mana_cost: 50
    mana_cost_per_level: 5
    unlock: 6
    level_up: 6
    max_level: 0
    require_sneak: false
    check_offhand: true
    sneak_offhand_bypass: true
    radius: 15
```

Create `common/src/main/resources/rewards/husbandry.yml`:

```yaml
patterns:
  - type: stat
    stat: health
    value: 1
    pattern:
      interval: 1
  - type: stat
    stat: regeneration
    value: 1
    pattern:
      interval: 2
levels:
```

- [ ] **Step 8: Messages**

Under `skills:` in `messages_en.yml`:

```yaml
  husbandry:
    name: Husbandry
    desc: "Breed, tame and care for animals to earn Husbandry {xp_unit}"
```

Under `abilities:`:

```yaml
  twins:
    name: Twins
    desc: "Grants a {value}% chance for a second baby to be born when breeding animals."
    info: "+{value}% Twin Chance"
  rancher:
    name: Rancher
    desc: "Grants +{value}% Husbandry {xp_unit}."
    info: "+{value}% Husbandry {xp_unit}"
  healthy_growth:
    name: Healthy Growth
    desc: "Animals bred by you grow {value}% faster (max 40%)."
    info: "+{value}% Growth Speed"
  gentle_hands:
    name: Gentle Hands
    desc: "Animals tamed by you gain {value} extra max health and a movement speed boost."
    info: "+{value} Pet Health"
  bountiful_pasture:
    name: Bountiful Pasture
    desc: "Grants a {value}% chance to get extra products when shearing or milking animals."
    info: "+{value}% Extra Product Chance"
```

Under `mana_abilities:`:

```yaml
  animal_whisperer:
    name: Animal Whisperer
    desc: "For {value} seconds, animals within {radius} blocks follow you and babies grow up faster. <1>[Right click wheat to activate]"
    raise: "<gray>You raise a bundle of wheat"
    lower: "<gray>You lower the wheat"
    start: "<gold>Animal Whisperer Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>Animal Whisperer has worn off"
```

Under `sources:`:

```yaml
  breeding:
    breed_common: Breed Farm Animals
    breed_horse: Breed Horses
    breed_mule: Breed a Mule
    breed_rare: Breed Rare Animals
    tame_wolf: Tame a Wolf
    tame_cat: Tame a Cat
    tame_parrot: Tame a Parrot
    tame_horse: Tame a Horse
    shear_sheep: Shear a Sheep
    shear_mooshroom: Shear a Mooshroom
    shear_snow_golem: Shear a Snow Golem
    milk_animal: Milk an Animal
```

- [ ] **Step 9: Menu + command**

`menus/skills.yml` contexts (inside the fork block):

```yaml
      husbandry:
        group: fourth_row
        order: 2
        material: wheat
```

`SkillCommands.java`:

```java
    @CommandAlias("husbandry")
    public static class HusbandryCommand extends SkillCommand {

        public HusbandryCommand(AuraSkills plugin) {
            super(plugin, Skills.HUSBANDRY);
        }

        @Default
        public void onCommand(Player player) {
            openMenu(player);
        }

    }
```

`CommandRegistrar.java`:

```java
            registerSkillCommand(new SkillCommands.HusbandryCommand(plugin), map, manager);
```

- [ ] **Step 10: Test + build + commit**

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.HusbandrySkillTest"` — Expected: PASS.
Run: `./gradlew build -x javadoc` — Expected: BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "Add Husbandry skill definition with breeding XP sources"
```

---

### Task 4: Husbandry — abilities + Animal Whisperer

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/husbandry/HusbandryAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/husbandry/AnimalWhisperer.java`
- Modify (append-only, banner): `bukkit/.../ability/BukkitAbilityManager.java`, `bukkit/.../mana/BukkitManaAbilityManager.java`

**Interfaces:**
- Consumes: `Skills.HUSBANDRY`, `Abilities.TWINS/RANCHER/HEALTHY_GROWTH/GENTLE_HANDS/BOUNTIFUL_PASTURE`, `ManaAbilities.ANIMAL_WHISPERER`, `ManaAbilityMessage.ANIMAL_WHISPERER_START/END` (Task 3); `isEquipment`-style helpers not needed here.
- Produces: `HusbandryAbilities`, `AnimalWhisperer` (registration-only consumers).

**Spec deviation note (intentional):** the spec's `gentle_hands` included "increased tame chance". Vanilla tame rolls happen entirely inside `Tameable` logic before `EntityTameEvent` fires and cannot be modified without replacing NMS behavior, so `gentle_hands` is implemented as: tamed-by-you animals gain +{value} max health (0.5 hearts/level, cap 5 hearts = 10 health points... use raw health units: value is in half-hearts) and +{secondary_value}% movement speed (2%/level). This matches the `abilities.yml` options defined in Task 3 Step 7.

- [ ] **Step 1: Write HusbandryAbilities**

```java
package dev.aurelium.auraskills.bukkit.skills.husbandry;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HusbandryAbilities extends BukkitAbilityImpl {

    private final Map<UUID, BukkitTask> growthTasks = new ConcurrentHashMap<>();

    public HusbandryAbilities(AuraSkills plugin) {
        super(plugin, Abilities.TWINS, Abilities.RANCHER, Abilities.HEALTHY_GROWTH, Abilities.GENTLE_HANDS, Abilities.BOUNTIFUL_PASTURE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        User user = plugin.getUser(player);

        applyTwins(event, player, user);
        applyHealthyGrowth(event.getEntity(), player, user);
    }

    private void applyTwins(EntityBreedEvent event, Player player, User user) {
        var ability = Abilities.TWINS;
        if (isDisabled(ability)) return;
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= Math.min(30.0, getValue(ability, user)) / 100) return;

        Entity baby = event.getEntity();
        LivingEntity twin = (LivingEntity) baby.getWorld().spawnEntity(baby.getLocation(), baby.getType());
        if (twin instanceof Animals animals) {
            animals.setBaby();
        }
    }

    private void applyHealthyGrowth(Entity baby, Player player, User user) {
        var ability = Abilities.HEALTHY_GROWTH;
        if (isDisabled(ability)) return;
        if (!(baby instanceof Animals animals)) return;
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;

        double boost = Math.min(40.0, getValue(ability, user)) / 100;
        // Negative age = baby. Every second, with probability `boost`, advance growth by one extra second.
        scheduleGrowth(animals, boost);
    }

    private void scheduleGrowth(Animals animals, double chancePerTick) {
        UUID id = animals.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Entity entity = plugin.getServer().getEntity(id);
            if (!(entity instanceof Animals grown) || !grown.isValid() || !grown.isBaby()) {
                BukkitTask self = growthTasks.remove(id);
                if (self != null) self.cancel();
                return;
            }
            if (rand.nextDouble() < chancePerTick) {
                grown.setAge(grown.getAge() - 20); // one extra second of growth
            }
        }, 20L, 20L);
        growthTasks.put(id, task);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void gentleHands(EntityTameEvent event) {
        var ability = Abilities.GENTLE_HANDS;
        if (isDisabled(ability)) return;
        if (!(event.getOwner() instanceof Player player)) return;

        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        int abilityLevel = user.getAbilityLevel(ability);
        if (abilityLevel <= 0) return;

        LivingEntity entity = event.getEntity();
        double extraHealth = Math.min(10.0, getValue(ability, user)); // value is in half-hearts
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() + extraHealth);
            entity.setHealth(Math.min(maxHealth.getValue(), entity.getHealth() + extraHealth));
        }

        double speedBoost = ability.optionDouble("speed_boost_per_level", 2.0) * abilityLevel / 100;
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * (1 + speedBoost));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void bountifulPasture(PlayerShearEntityEvent event) {
        var ability = Abilities.BOUNTIFUL_PASTURE;
        if (isDisabled(ability)) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player);
        if (failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        Entity entity = event.getEntity();
        if (entity instanceof Sheep sheep) {
            ItemStack extraWool = new ItemStack(woolForColor(sheep), 1);
            entity.getWorld().dropItemNaturally(entity.getLocation(), extraWool);
        } else if (entity.getType() == EntityType.MOOSHROOM) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.RED_MUSHROOM, 1));
        }
    }

    private Material woolForColor(Sheep sheep) {
        String color = sheep.getColor() != null ? sheep.getColor().name() : "WHITE";
        Material wool = Material.matchMaterial(color + "_WOOL");
        return wool != null ? wool : Material.WHITE_WOOL;
    }
}
```

(`Tameable` import may be unused — remove it if checkstyle complains. `Attribute.MAX_HEALTH` / `Attribute.MOVEMENT_SPEED` are the 1.21.3+ names; if the Spigot API version in use still exposes `GENERIC_MAX_HEALTH`/`GENERIC_MOVEMENT_SPEED`, use those — check what other trait classes in `bukkit/.../trait/` use and match it.)

- [ ] **Step 2: Write AnimalWhisperer**

```java
package dev.aurelium.auraskills.bukkit.skills.husbandry;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnimalWhisperer extends ReadiedManaAbility {

    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<UUID>> grownBabies = new ConcurrentHashMap<>();

    public AnimalWhisperer(AuraSkills plugin) {
        super(plugin, ManaAbilities.ANIMAL_WHISPERER, ManaAbilityMessage.ANIMAL_WHISPERER_START, ManaAbilityMessage.ANIMAL_WHISPERER_END,
                new String[]{"WHEAT"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, 1, 1.2f);
        int radius = manaAbility.optionInt("radius", 15);
        grownBabies.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> whisper(player, radius), 10L, 10L);
        tasks.put(player.getUniqueId(), task);
    }

    @Override
    public void onStop(Player player, User user) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        grownBabies.remove(player.getUniqueId());
    }

    private void whisper(Player player, int radius) {
        if (!isActivated(player)) {
            onStop(player, plugin.getUser(player));
            return;
        }
        java.util.Set<UUID> grown = grownBabies.getOrDefault(player.getUniqueId(), java.util.Set.of());
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Animals animals)) continue;
            if (animals.isBaby()) {
                // One growth step per activation per baby
                if (grown.add(animals.getUniqueId())) {
                    animals.setAge(animals.getAge() + 2400); // 2 minutes of growth
                }
                continue;
            }
            // Gentle pull toward the player ("follow without bait")
            Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (direction.lengthSquared() > 4) {
                direction.normalize().multiply(0.15);
                entity.setVelocity(entity.getVelocity().add(direction).setY(Math.max(entity.getVelocity().getY(), 0)));
            }
        }
    }
}
```

- [ ] **Step 3: Register implementations**

`BukkitAbilityManager.registerAbilityImplementations()` — append inside the fork banner block:

```java
        registerAbilityImpl(new HusbandryAbilities(plugin));
```

`BukkitManaAbilityManager.registerProviders()` — append inside the fork banner block:

```java
        registerProvider(new AnimalWhisperer(plugin));
```

- [ ] **Step 4: Build + commit**

Run: `./gradlew build -x javadoc` — Expected: BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "Implement Husbandry abilities and Animal Whisperer mana ability"
```

---

### Task 5: Trading — source type + skill definition

**Files:**
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/TradingXpSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/TradingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/TradingSourceParser.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/TradingLeveler.java`
- Create: `common/src/main/resources/sources/trading.yml`
- Create: `common/src/main/resources/rewards/trading.yml`
- Create: `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/TradingSkillTest.java`
- Modify (append-only, banner): `Skills.java`, `Abilities.java`, `ManaAbilities.java`, `SourceTypes.java`, `ManaAbilityMessage.java`, `BukkitLevelManager.java`, `SkillCommands.java`, `CommandRegistrar.java`, `skills.yml`, `abilities.yml`, `mana_abilities.yml`, `messages_en.yml`, `menus/skills.yml`

**Interfaces:**
- Consumes: fork append blocks from earlier tasks.
- Produces: `Skills.TRADING`, `Abilities.SILVER_TONGUE/MERCHANT/CHARISMA/MASTER_NEGOTIATOR/GUILD_REPUTATION`, `ManaAbilities.GRAND_BARGAIN`, `SourceTypes.TRADING`, `TradingXpSource` with `TradingTrigger getTrigger()`, `String getVillagerProfession()` (nullable), `int getMinVillagerLevel()`, `int getMaxVillagerLevel()`, `String getBarterItem()` (nullable), `enum TradingTrigger { VILLAGER_TRADE, WANDERING_TRADER_TRADE, PIGLIN_BARTER }`; `TradingLeveler.computeDecayMultiplier(int occurrencesInWindow)` (static, unit-tested in Task 6).

**Platform note:** the `bukkit` module compiles against Spigot API, so trade detection uses `InventoryClickEvent` on a `MerchantInventory` result slot (works on Spigot and Paper) instead of the Paper-only `PlayerTradeEvent`. This replaces the spec's event choice; all spec behaviors (tier multipliers, decay, anti-abuse) are preserved.

- [ ] **Step 1: Write the failing test**

Create `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/TradingSkillTest.java` — same skeleton as `SmithingSkillTest`, with:

```java
    @Test
    void testTradingSkillLoads() {
        assertTrue(Skills.TRADING.isEnabled());
        assertTrue(Skills.TRADING.getAbilities().size() == 5);
    }

    @Test
    void testTradingSourcesLoad() {
        List<SkillSource<TradingXpSource>> sources = plugin.getSkillManager().getSourcesOfType(TradingXpSource.class);
        assertFalse(sources.isEmpty());
        assertTrue(sources.stream().allMatch(s -> s.source().getXp() > 0));
        for (TradingXpSource.TradingTrigger trigger : TradingXpSource.TradingTrigger.values()) {
            assertTrue(sources.stream().anyMatch(s -> s.source().getTrigger() == trigger), "Missing trigger " + trigger);
        }
    }
```

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.TradingSkillTest"` — Expected: compilation FAILURE.

- [ ] **Step 2: API interface**

`api/src/main/java/dev/aurelium/auraskills/api/source/type/TradingXpSource.java`:

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;
import org.jetbrains.annotations.Nullable;

public interface TradingXpSource extends XpSource {

    TradingTrigger getTrigger();

    /** Required villager profession (e.g. librarian); null means any. */
    @Nullable
    String getVillagerProfession();

    /** Minimum villager level (1 = novice ... 5 = master). */
    int getMinVillagerLevel();

    /** Maximum villager level. */
    int getMaxVillagerLevel();

    /** Result item for piglin bartering sources; null means any outcome. */
    @Nullable
    String getBarterItem();

    enum TradingTrigger {

        VILLAGER_TRADE,
        WANDERING_TRADER_TRADE,
        PIGLIN_BARTER

    }

}
```

- [ ] **Step 3: Common impl**

`common/src/main/java/dev/aurelium/auraskills/common/source/type/TradingSource.java`:

```java
package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.Source;
import dev.aurelium.auraskills.common.source.SourceValues;
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
```

- [ ] **Step 4: Parser**

`common/src/main/java/dev/aurelium/auraskills/common/source/parser/TradingSourceParser.java`:

```java
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
```

Register in `SourceTypes.java` — inside the fork banner block, change the last entry to `,` and append `TRADING(TradingSourceParser.class);`.

- [ ] **Step 5: Leveler**

`bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/TradingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.TradingXpSource;
import dev.aurelium.auraskills.api.source.type.TradingXpSource.TradingTrigger;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradingLeveler extends SourceLeveler {

    private static final int DECAY_WINDOW = 5;

    // player id -> merchant key -> recent recipe keys
    private final Map<UUID, Map<String, Deque<String>>> tradeHistory = new ConcurrentHashMap<>();

    public TradingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.TRADING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (disabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof MerchantInventory inventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        MerchantRecipe recipe = inventory.getSelectedRecipe();
        if (recipe == null) return;
        // Trade must have stock left (result slot is empty when exhausted, checked above via currentItem)
        if (recipe.getUses() >= recipe.getMaxUses()) return;

        Entity merchantEntity = inventory.getMerchant() instanceof Entity entity ? entity : null;

        TradingTrigger trigger;
        String profession = null;
        int villagerLevel = 1;
        if (merchantEntity instanceof Villager villager) {
            trigger = TradingTrigger.VILLAGER_TRADE;
            profession = villager.getProfession().name();
            villagerLevel = villager.getVillagerLevel();
        } else if (merchantEntity instanceof WanderingTrader) {
            trigger = TradingTrigger.WANDERING_TRADER_TRADE;
        } else {
            return; // Merchant GUI not backed by an entity (e.g. custom merchants from other plugins)
        }

        for (SkillSource<TradingXpSource> entry : plugin.getSkillManager().getSourcesOfType(TradingXpSource.class)) {
            TradingXpSource source = entry.source();
            if (source.getTrigger() != trigger) continue;
            if (source.getVillagerProfession() != null && (profession == null || !source.getVillagerProfession().equalsIgnoreCase(profession))) continue;
            if (villagerLevel < source.getMinVillagerLevel() || villagerLevel > source.getMaxVillagerLevel()) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, player.getLocation(), skill)) return;

            String merchantKey = merchantEntity.getUniqueId().toString();
            String recipeKey = result.getType().name() + ":" + recipe.getIngredients();
            double decay = recordAndGetDecay(player.getUniqueId(), merchantKey, recipeKey);

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * decay);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBarter(PiglinBarterEvent event) {
        if (disabled()) return;
        Player player = event.getPlayer();
        if (player == null) return;

        for (SkillSource<TradingXpSource> entry : plugin.getSkillManager().getSourcesOfType(TradingXpSource.class)) {
            TradingXpSource source = entry.source();
            if (source.getTrigger() != TradingTrigger.PIGLIN_BARTER) continue;

            double total = 0;
            for (ItemStack outcome : event.getOutcome()) {
                if (source.getBarterItem() == null || source.getBarterItem().equalsIgnoreCase(outcome.getType().name())) {
                    total += source.getXp() * outcome.getAmount();
                }
            }
            if (total <= 0) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, event.getEntity().getLocation(), skill)) return;

            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, total);
            return;
        }
    }

    private double recordAndGetDecay(UUID playerId, String merchantKey, String recipeKey) {
        Deque<String> recent = tradeHistory
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(merchantKey, k -> new ArrayDeque<>());
        long occurrences = recent.stream().filter(recipeKey::equals).count();
        recent.addLast(recipeKey);
        while (recent.size() > DECAY_WINDOW) {
            recent.removeFirst();
        }
        return computeDecayMultiplier((int) occurrences);
    }

    /** occurrences = times this recipe already appeared in the current window (0 = first trade). */
    public static double computeDecayMultiplier(int occurrences) {
        if (occurrences < 2) return 1.0;
        return Math.max(0.1, Math.pow(0.5, occurrences - 1));
    }
}
```

Register in `BukkitLevelManager.registerLevelers()` — append inside the fork banner block:

```java
        registerLeveler(new TradingLeveler(plugin));
```

- [ ] **Step 6: XP sources config**

Create `common/src/main/resources/sources/trading.yml`:

```yaml
sources:
  novice_trade:
    type: trading
    trigger: villager_trade
    max_villager_level: 1
    xp: 10.0
    menu_item:
      material: emerald
  apprentice_trade:
    type: trading
    trigger: villager_trade
    min_villager_level: 2
    max_villager_level: 2
    xp: 15.0
    menu_item:
      material: emerald
  journeyman_trade:
    type: trading
    trigger: villager_trade
    min_villager_level: 3
    max_villager_level: 3
    xp: 20.0
    menu_item:
      material: emerald_block
  expert_trade:
    type: trading
    trigger: villager_trade
    min_villager_level: 4
    max_villager_level: 4
    xp: 25.0
    menu_item:
      material: emerald_block
  master_trade:
    type: trading
    trigger: villager_trade
    min_villager_level: 5
    xp: 30.0
    menu_item:
      material: diamond
  wandering_trader_trade:
    type: trading
    trigger: wandering_trader_trade
    xp: 15.0
    menu_item:
      material: blue_orchid
  piglin_barter_rare:
    type: trading
    trigger: piglin_barter
    barter_item: netherite_scrap
    xp: 25.0
    menu_item:
      material: netherite_scrap
  piglin_barter:
    type: trading
    trigger: piglin_barter
    xp: 5.0
    menu_item:
      material: gold_ingot
```

Order matters: the leveler returns on the first matching source, so the specific `piglin_barter_rare` entry must come before the generic `piglin_barter` one (which matches any outcome).

- [ ] **Step 7: Enums + skill definition**

`Skills.java` — change `HUSBANDRY(Abilities.RANCHER);` to `,` and append `TRADING(Abilities.MERCHANT);`.

`Abilities.java` — change `BOUNTIFUL_PASTURE("husbandry");` to `,` and append:

```java
    SILVER_TONGUE("trading"),
    MERCHANT("trading"),
    CHARISMA("trading"),
    MASTER_NEGOTIATOR("trading"),
    GUILD_REPUTATION("trading");
```

`ManaAbilities.java` — change `ANIMAL_WHISPERER;` to `,` and append `GRAND_BARGAIN;`.

`ManaAbilityMessage.java` — change `ANIMAL_WHISPERER_END;` to `,` and append:

```java
    GRAND_BARGAIN_START,
    GRAND_BARGAIN_END;
```

Append to `skills.yml` (inside the fork block):

```yaml
  auraskills/trading:
    abilities:
      - auraskills/silver_tongue
      - auraskills/merchant
      - auraskills/charisma
      - auraskills/master_negotiator
      - auraskills/guild_reputation
    mana_ability: auraskills/grand_bargain
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

Append to `abilities.yml`:

```yaml
  auraskills/silver_tongue:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+1'
    level_up: 5
    max_level: 10
    refund_percentage: 10.0
  auraskills/merchant:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/charisma:
    enabled: true
    base_value: 3.0
    value_per_level: 3.0
    unlock: '{start}+3'
    level_up: 5
    max_level: 0
    min_discount: 5.0
    max_discount: 15.0
  auraskills/master_negotiator:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+4'
    level_up: 5
    max_level: 0
  auraskills/guild_reputation:
    enabled: true
    base_value: 1.0
    value_per_level: 1.0
    unlock: '{start}+5'
    level_up: 5
    max_level: 0
```

Append to `mana_abilities.yml`:

```yaml
  auraskills/grand_bargain:
    enabled: true
    base_value: 10.0
    value_per_level: 2.0
    base_cooldown: 300
    cooldown_per_level: -5
    base_mana_cost: 50
    mana_cost_per_level: 5
    unlock: 6
    level_up: 6
    max_level: 0
    require_sneak: false
    check_offhand: true
    sneak_offhand_bypass: true
    discount_percentage: 20.0
```

Create `common/src/main/resources/rewards/trading.yml`:

```yaml
patterns:
  - type: stat
    stat: luck
    value: 1
    pattern:
      interval: 1
  - type: stat
    stat: wisdom
    value: 1
    pattern:
      interval: 2
levels:
```

- [ ] **Step 8: Messages**

Under `skills:`:

```yaml
  trading:
    name: Trading
    desc: "Trade with villagers and barter with piglins to earn Trading {xp_unit}"
```

Under `abilities:`:

```yaml
  silver_tongue:
    name: Silver Tongue
    desc: "Grants a {value}% chance to get {refund_percentage}% of the emeralds spent on a trade refunded."
    info: "+{value}% Refund Chance"
  merchant:
    name: Merchant
    desc: "Grants +{value}% Trading {xp_unit}."
    info: "+{value}% Trading {xp_unit}"
  charisma:
    name: Charisma
    desc: "Grants a {value}% chance for discounted prices when opening a villager's trades."
    info: "+{value}% Discount Chance"
  master_negotiator:
    name: Master Negotiator
    desc: "Grants a {value}% chance for a trade to not consume its stock (max 50%)."
    info: "+{value}% Free Trade Chance"
  guild_reputation:
    name: Guild Reputation
    desc: "Grants a {value}% chance to receive a trade's items twice (max 20%)."
    info: "+{value}% Double Trade Chance"
```

Under `mana_abilities:`:

```yaml
  grand_bargain:
    name: Grand Bargain
    desc: "For {value} seconds, all trades are {discount_percentage}% cheaper and never run out of stock. <1>[Right click an emerald to activate]"
    raise: "<gray>You raise an emerald"
    lower: "<gray>You pocket the emerald"
    start: "<gold>Grand Bargain Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>Grand Bargain has worn off"
```

Under `sources:`:

```yaml
  trading:
    novice_trade: Trade with a Novice Villager
    apprentice_trade: Trade with an Apprentice Villager
    journeyman_trade: Trade with a Journeyman Villager
    expert_trade: Trade with an Expert Villager
    master_trade: Trade with a Master Villager
    wandering_trader_trade: Trade with a Wandering Trader
    piglin_barter: Barter with a Piglin
    piglin_barter_rare: Rare Piglin Barter
```

- [ ] **Step 9: Menu + command**

`menus/skills.yml` contexts (inside the fork block):

```yaml
      trading:
        group: fourth_row
        order: 1
        material: emerald
```

`SkillCommands.java`:

```java
    @CommandAlias("trading")
    public static class TradingCommand extends SkillCommand {

        public TradingCommand(AuraSkills plugin) {
            super(plugin, Skills.TRADING);
        }

        @Default
        public void onCommand(Player player) {
            openMenu(player);
        }

    }
```

`CommandRegistrar.java`:

```java
            registerSkillCommand(new SkillCommands.TradingCommand(plugin), map, manager);
```

- [ ] **Step 10: Test + build + commit**

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.TradingSkillTest"` — Expected: PASS.
Run: `./gradlew build -x javadoc` — Expected: BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "Add Trading skill definition with trading XP sources"
```

---

### Task 6: Trading — abilities + Grand Bargain

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/trading/TradingAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/trading/GrandBargain.java`
- Modify (append-only, banner): `bukkit/.../ability/BukkitAbilityManager.java`, `bukkit/.../mana/BukkitManaAbilityManager.java`
- Modify: `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/skill/TradingSkillTest.java` (add decay test)

**Interfaces:**
- Consumes: everything from Task 5; `plugin.getManaAbilityManager().isActivated(player, ManaAbilities.GRAND_BARGAIN)` (verify the exact accessor against the codebase — `SmithingAbilities.isOverdriveActive` in Task 2 uses the same pattern for `FORGE_OVERDRIVE`; keep both consistent).
- Produces: `TradingAbilities`, `GrandBargain` (registration-only consumers).

- [ ] **Step 1: Write the failing decay unit test**

Append to `TradingSkillTest.java`:

```java
    @Test
    void testTradeDecayMultiplier() {
        assertEquals(1.0, dev.aurelium.auraskills.bukkit.source.TradingLeveler.computeDecayMultiplier(0));
        assertEquals(1.0, dev.aurelium.auraskills.bukkit.source.TradingLeveler.computeDecayMultiplier(1));
        assertEquals(0.5, dev.aurelium.auraskills.bukkit.source.TradingLeveler.computeDecayMultiplier(2));
        assertEquals(0.25, dev.aurelium.auraskills.bukkit.source.TradingLeveler.computeDecayMultiplier(3));
        assertEquals(0.1, dev.aurelium.auraskills.bukkit.source.TradingLeveler.computeDecayMultiplier(10));
    }
```

(Add `import static org.junit.jupiter.api.Assertions.assertEquals;`.) Run the test — Expected: PASS already (the method exists from Task 5). If it fails, fix `computeDecayMultiplier`. This step locks the anti-abuse formula: 1st-2nd identical trade in the window = full XP, 3rd = 50%, then halving, floor 10%.

- [ ] **Step 2: Write TradingAbilities**

```java
package dev.aurelium.auraskills.bukkit.skills.trading;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.ability.BukkitAbilityImpl;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class TradingAbilities extends BukkitAbilityImpl {

    private static final double MAX_TOTAL_DISCOUNT = 0.8;

    public TradingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SILVER_TONGUE, Abilities.MERCHANT, Abilities.CHARISMA, Abilities.MASTER_NEGOTIATOR, Abilities.GUILD_REPUTATION);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyDiscounts(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        User user = plugin.getUser(player);
        double discount = 0;

        var charisma = Abilities.CHARISMA;
        if (!isDisabled(charisma) && !failsChecks(player, charisma) && user.getAbilityLevel(charisma) > 0) {
            if (rand.nextDouble() < getValue(charisma, user) / 100) {
                double min = charisma.optionDouble("min_discount", 5.0);
                double max = charisma.optionDouble("max_discount", 15.0);
                discount += min + rand.nextDouble() * (max - min);
            }
        }

        if (isGrandBargainActive(player)) {
            discount += ManaAbilities.GRAND_BARGAIN.optionDouble("discount_percentage", 20.0);
        }

        if (discount <= 0) return;
        double finalDiscount = Math.min(MAX_TOTAL_DISCOUNT, discount / 100);

        var merchant = inventory.getMerchant();
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < merchant.getRecipeCount(); i++) {
            MerchantRecipe original = merchant.getRecipe(i);
            MerchantRecipe copy = new MerchantRecipe(original.getResult(), original.getUses(), original.getMaxUses(),
                    original.hasExperienceReward(), original.getVillagerExperience(), original.getPriceMultiplier());
            List<ItemStack> ingredients = new ArrayList<>();
            for (ItemStack ingredient : original.getIngredients()) {
                ItemStack adjusted = ingredient.clone();
                adjusted.setAmount(Math.max(1, (int) Math.ceil(adjusted.getAmount() * (1 - finalDiscount))));
                ingredients.add(adjusted);
            }
            copy.setIngredients(ingredients);
            recipes.add(copy);
        }
        merchant.setRecipes(recipes);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTradeCompleted(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof MerchantInventory inventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        MerchantRecipe recipe = inventory.getSelectedRecipe();
        if (recipe == null || recipe.getUses() >= recipe.getMaxUses()) return;

        User user = plugin.getUser(player);

        applySilverTongue(player, user, recipe);
        applyGuildReputation(player, user, result);
        applyMasterNegotiator(player, user, inventory, recipe);
    }

    private void applySilverTongue(Player player, User user, MerchantRecipe recipe) {
        var ability = Abilities.SILVER_TONGUE;
        if (isDisabled(ability) || failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= getValue(ability, user) / 100) return;

        int emeraldsSpent = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient.getType() == Material.EMERALD) {
                emeraldsSpent += ingredient.getAmount();
            }
        }
        if (emeraldsSpent == 0) return;

        int refund = (int) Math.ceil(emeraldsSpent * ability.optionDouble("refund_percentage", 10.0) / 100);
        if (refund <= 0) return;
        player.getInventory().addItem(new ItemStack(Material.EMERALD, refund)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void applyGuildReputation(Player player, User user, ItemStack result) {
        var ability = Abilities.GUILD_REPUTATION;
        if (isDisabled(ability) || failsChecks(player, ability)) return;
        if (user.getAbilityLevel(ability) <= 0) return;
        if (rand.nextDouble() >= Math.min(20.0, getValue(ability, user)) / 100) return;

        player.getInventory().addItem(result.clone()).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void applyMasterNegotiator(Player player, User user, MerchantInventory inventory, MerchantRecipe recipe) {
        var ability = Abilities.MASTER_NEGOTIATOR;
        boolean grandBargain = isGrandBargainActive(player);
        if (!grandBargain) {
            if (isDisabled(ability) || failsChecks(player, ability)) return;
            if (user.getAbilityLevel(ability) <= 0) return;
            if (rand.nextDouble() >= Math.min(50.0, getValue(ability, user)) / 100) return;
        }
        // Roll back the use this click is about to add, one tick later
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            recipe.setUses(Math.max(0, recipe.getUses() - 1));
        });
    }

    private boolean isGrandBargainActive(Player player) {
        return plugin.getManaAbilityManager().isActivated(player, ManaAbilities.GRAND_BARGAIN);
    }
}
```

Verify the exact signature of the mana-activation accessor against `BukkitManaAbilityManager`/`ManaAbilityManager` (e.g. `isActivated(Player, ManaAbility)`); use whatever the codebase provides and use the SAME call in `SmithingAbilities.isOverdriveActive` (Task 2) — fix Task 2's call site if they diverge.

- [ ] **Step 3: Write GrandBargain**

```java
package dev.aurelium.auraskills.bukkit.skills.trading;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public class GrandBargain extends ReadiedManaAbility {

    public GrandBargain(AuraSkills plugin) {
        super(plugin, ManaAbilities.GRAND_BARGAIN, ManaAbilityMessage.GRAND_BARGAIN_START, ManaAbilityMessage.GRAND_BARGAIN_END,
                new String[]{"EMERALD"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
        // Active state is managed by ReadiedManaAbility; TradingAbilities queries isActivated(...)
    }

    @Override
    public void onStop(Player player, User user) {
    }

    @Override
    public String replaceDescPlaceholders(String input, User user) {
        return TextUtil.replace(input, "{discount_percentage}", String.valueOf(manaAbility.optionDouble("discount_percentage", 20.0)));
    }
}
```

- [ ] **Step 4: Register implementations**

`BukkitAbilityManager.registerAbilityImplementations()` — append inside the fork banner block:

```java
        registerAbilityImpl(new TradingAbilities(plugin));
```

`BukkitManaAbilityManager.registerProviders()` — append inside the fork banner block:

```java
        registerProvider(new GrandBargain(plugin));
```

- [ ] **Step 5: Build + commit**

Run: `./gradlew :bukkit:test --tests "dev.aurelium.auraskills.bukkit.skill.TradingSkillTest"` then `./gradlew build -x javadoc` — Expected: all PASS / BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "Implement Trading abilities and Grand Bargain mana ability"
```

---

### Task 7: Final integration, versioning and release notes

**Files:**
- Modify: `common/src/main/resources/skills.yml` (`file_version`), `common/src/main/resources/abilities.yml`, `common/src/main/resources/mana_abilities.yml`, `bukkit/src/main/resources/menus/skills.yml` (`file_version`)
- Modify: `Changelog.md` (new fork section at top)
- Modify: `docs/superpowers/specs/2026-09-24-new-skills-design.md` (mark implementation status)

**Interfaces:**
- Consumes: all previous tasks.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Bump config file versions for existing servers**

`ConfigurateLoader.updateUserFile` merges missing embedded keys into user files only when the embedded `file_version` is higher than the user's. For each of these files, check the current `file_version` at the top and increment it by 1 (append-commented with the fork banner):

- `common/src/main/resources/skills.yml`
- `common/src/main/resources/abilities.yml`
- `common/src/main/resources/mana_abilities.yml`
- `bukkit/src/main/resources/menus/skills.yml`

Verify each file is actually loaded through `updateUserFile`/`MenuFileManager` — `skills.yml` is (see `SkillLoader`), menus are (see `MenuFileManager`). If `abilities.yml` or `mana_abilities.yml` is NOT loaded through `updateUserFile`, find its loader (`AbilityManager`/`ManaAbilityManager` in common) and confirm how new keys propagate; if no merge mechanism exists for that file, no bump is needed (new keys are read from embedded defaults).

Important known limitation: `MenuFileManager.updateConfigSection` only merges whole missing named sections under `items`/`templates`/`components`/`formats` — it CANNOT merge new entries into the existing `templates.skill.contexts` block. Servers upgrading from an earlier fork version must either delete `menus/skills.yml` (it regenerates) or paste the three context entries manually. Document this in the changelog entry (Step 3). Do NOT modify `MenuFileManager` (upstream file, behavior change — forbidden by the merge rules).

- [ ] **Step 2: Full verification**

Run: `./gradlew build -x javadoc` — Expected: BUILD SUCCESSFUL, all tests green (including `SmithingSkillTest`, `HusbandrySkillTest`, `TradingSkillTest`).

- [ ] **Step 3: Changelog**

Prepend a new section to `Changelog.md` (above the latest release, following its existing heading format):

```markdown
## Fork - New Skills (unreleased)

- Added 3 new skills: **Trading**, **Husbandry** and **Smithing**, each with 5 abilities, 1 mana ability, own XP sources, stat rewards, menu icons and `/<skill>` commands
- New XP source types: `trading`, `breeding`, `smithing`, `smelting`, `crafting`
- Trading mana ability: Grand Bargain (right click an emerald)
- Husbandry mana ability: Animal Whisperer (right click wheat)
- Smithing mana ability: Forge Overdrive (right click coal/charcoal)
- Note for existing servers: to see the 3 new skills in the `/skills` menu, delete `plugins/AuraSkills/menus/skills.yml` (it regenerates) or manually add the `trading`/`husbandry`/`smithing` contexts from the default file
```

- [ ] **Step 4: Manual smoke test on a Paper server**

Copy `build/libs/AuraSkills-2.4.0.jar` (version from `gradle.properties`) to a local Paper server and verify:

1. `/skills` shows 14 skill icons; the 3 new ones on the bottom row
2. `/trading`, `/husbandry`, `/smithing` open the level progression menus
3. XP gains: villager trade (each tier), piglin barter, breeding, taming, shearing, milking, smithing table upgrade, armor trim, furnace extract, crafting diamond gear
4. Trade XP decay: repeat the same trade on the same villager 5 times — XP drops to 50%, 25%, 12.5%→10%
5. Each mana ability activates with its held item, shows boss bar/messages, and expires
6. Placeholders: `%auraskills_trading%` etc. resolve if PlaceholderAPI is installed
7. Leaderboard: `/skilltop trading` works
8. Restart the server — levels persist, no console errors

- [ ] **Step 5: Merge to master and push**

```bash
git checkout master
git merge feature/fork-skills
git push origin master
git push origin feature/fork-skills
```

Update `docs/superpowers/specs/2026-09-24-new-skills-design.md` status line to `**Status:** Implementado` and commit it with the merge or as a final docs commit.

---

## Self-Review Notes (already applied)

- **Spec coverage:** all 3 skills, 5 source types, 15 abilities, 3 mana abilities, rewards, menus, commands, messages, anti-abuse rules (trade decay, parent-pair cooldown, milk cooldown, smelt/craft exploit guards), tests and merge strategy have tasks. Exceptions, documented inline: `gentle_hands` tame-chance sub-feature dropped (vanilla limitation, Task 4 note); `PlayerTradeEvent` replaced by `InventoryClickEvent` on `MerchantInventory` (Spigot-API constraint, Task 5 note); `SourceTag` addition dropped (no fork ability needs tag-based source filtering — YAGNI).
- **Type consistency:** `matchesAny` lives in `SmeltingLeveler` and is reused by `CraftingLeveler`, `SmithingLeveler`, `BreedingLeveler`; `computeDecayMultiplier` is static on `TradingLeveler`; `isGrandBargainActive`/`isOverdriveActive` use the same manager accessor (consistency enforced in Task 6 Step 2).
- **YAML key consistency:** message keys (`skills.<name>`, `abilities.<name>`, `mana_abilities.<name>`, `sources.<type>.<key>`) match the enum names and source file keys exactly.
