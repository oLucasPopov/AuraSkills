# Building, Culinary and Engineering Skills — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three complete skills (Building, Culinary, Engineering) to the fork, in the same pattern as the previous round (Trading, Husbandry, Smithing).

**Architecture:** New `building` XP source type (BlockPlaceEvent + decay + position cache) shared by Building and Engineering; Culinary reuses `smelting`/`crafting` sources plus a new campfire listener; Smithing loses `smelt_food` via the versioned SourceFileUpdates mechanism. Each skill gets 5 abilities, 1 mana ability, rewards, menu entries, en+pt-BR messages and wiki docs.

**Tech Stack:** Java 21, Gradle multi-module (api / common / bukkit / paper), Spigot API 26.1 compile / Paper 26.2 runtime, Configurate YAML.

**Spec:** `docs/superpowers/specs/2026-09-25-more-skills-design.md`

## Global Constraints

- New code only in new files; edits to upstream files are append-only with a `// === Fork skills: building, culinary, engineering ===` (Java) or `# === Fork skills ===` (YAML) banner.
- Balance: flat/low XP for spammable actions; amount-multiplied XP only where input is expensive; decay on repeated identical actions.
- All percentage abilities capped per the spec tables.
- pt-BR names: Building = Construção, Culinary = Culinária, Engineering = Engenharia.
- Build command: `./gradlew build -x javadoc` must stay green. JDK is at `C:\Users\Lucas\.jdks\jdk-21` via `~/.gradle/gradle.properties` — never change JAVA_HOME.
- Reference implementation for every pattern: the fork's previous round (files under `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/{trading,husbandry,smithing}/`, plan at `docs/superpowers/plans/2026-09-24-new-skills.md`).

## Task Map

1. `building` source type (api + common + parser + leveler + registration + test)
2. Building skill definition + XP sources
3. Building abilities + Blueprint mana ability + messages
4. Culinary skill definition + sources + campfire listener + smelt_food migration
5. Culinary abilities + Banquet mana ability + messages
6. Engineering skill definition + XP sources
7. Engineering abilities + Power Surge mana ability + messages
8. Final integration: menus, file versions, SourceFileUpdates, Changelog, wiki, build/deploy/verify

---

### Task 1: `building` source type

**Files:**
- Create: `api/src/main/java/dev/aurelium/auraskills/api/source/type/BuildingXpSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/type/BuildingSource.java`
- Create: `common/src/main/java/dev/aurelium/auraskills/common/source/parser/BuildingSourceParser.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/BuildingLeveler.java`
- Modify: `common/src/main/java/dev/aurelium/auraskills/common/source/SourceTypes.java` (append enum entry under the fork banner)
- Modify: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/level/BukkitLevelManager.java` (append `registerLeveler` call under the fork banner)
- Test: `bukkit/src/test/java/dev/aurelium/auraskills/bukkit/source/BuildingLevelerTest.java`

**Interfaces:**
- Consumes: `TradingLeveler.computeDecayMultiplier(int)` (existing static, `bukkit/.../source/TradingLeveler.java:150`); `SourceLeveler` base class; `SmeltingLeveler.matchesAny(String[], String)`; `ConfigurateSourceContext.pluralizedArray` / `parseValues`.
- Produces: `SourceTypes.BUILDING`; `BuildingXpSource` with `String[] getBlocks()`; `BuildingLeveler` granting flat XP per `BlockPlaceEvent` with decay + 5-minute position cache.

- [ ] **Step 1: Write the failing test**

```java
package dev.aurelium.auraskills.bukkit.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildingLevelerTest {

    @Test
    void positionCacheBlocksRepeatXpWithinWindow() {
        BuildingLeveler.PositionCache cache = new BuildingLeveler.PositionCache(60_000);
        long pos = 123456789L;
        assertTrue(cache.tryGrant(pos, 1000));
        assertFalse(cache.tryGrant(pos, 2000));
        assertTrue(cache.tryGrant(pos, 1000 + 60_001));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :bukkit:test --tests "*BuildingLevelerTest*"`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement the source type**

`BuildingXpSource.java` (mirror `SmeltingXpSource`):

```java
package dev.aurelium.auraskills.api.source.type;

import dev.aurelium.auraskills.api.source.XpSource;

public interface BuildingXpSource extends XpSource {

    /** Block materials that give XP when placed; empty means any block. */
    String[] getBlocks();

}
```

`BuildingSource.java` (mirror `SmeltingSource`, replacing `items` with `blocks`):

```java
package dev.aurelium.auraskills.common.source.type;

import dev.aurelium.auraskills.api.source.type.BuildingXpSource;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.source.SourceValues;

public class BuildingSource extends Source implements BuildingXpSource {

    private final String[] blocks;

    public BuildingSource(AuraSkillsPlugin plugin, SourceValues values, String[] blocks) {
        super(plugin, values);
        this.blocks = blocks;
    }

    @Override
    public String[] getBlocks() {
        return blocks;
    }
}
```

Check the exact `Source` base class / `SourceValues` constructor against `common/.../source/type/SmeltingSource.java` and match it.

`BuildingSourceParser.java` (mirror `SmeltingSourceParser`):

```java
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
```

`SourceTypes.java` — append to the enum under the existing fork banner:

```java
    // === Fork skills: building, culinary, engineering ===
    BUILDING(BuildingSourceParser.class);
```

Note: the previous fork banner sits before `SMITHING...TRADING` entries; add the new entry after `TRADING(TradingSourceParser.class)` with the new banner comment above it, keeping one banner per round. Update the round banner to `trading, husbandry, smithing, building, culinary, engineering` if it is a single shared banner — pick whichever matches the file's existing style.

`BuildingLeveler.java`:

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.BuildingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuildingLeveler extends SourceLeveler {

    private static final int DECAY_WINDOW = 8;
    private static final long POSITION_CACHE_MS = 5 * 60 * 1000L;

    private final Map<UUID, Deque<String>> recentPlacements = new ConcurrentHashMap<>();
    private final PositionCache positionCache = new PositionCache(POSITION_CACHE_MS);

    public BuildingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.BUILDING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (disabled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        String blockName = block.getType().name();

        for (SkillSource<BuildingXpSource> entry : plugin.getSkillManager().getSourcesOfType(BuildingXpSource.class)) {
            BuildingXpSource source = entry.source();
            if (!SmeltingLeveler.matchesAny(source.getBlocks(), blockName)) continue;

            Skill skill = entry.skill();
            if (failsChecks(event, player, block.getLocation(), skill)) return;
            if (!positionCache.tryGrant(positionKey(block), System.currentTimeMillis())) return;

            double decay = recordAndGetDecay(player.getUniqueId(), blockName);
            User user = plugin.getUser(player);
            plugin.getLevelManager().addXp(user, skill, source, source.getXp() * decay);
            return;
        }
    }

    private static long positionKey(Block block) {
        // Pack chunk-stable coordinates; worlds are ignored deliberately so
        // mirrored builds in separate worlds share no cache (they are separate maps below if needed)
        return block.getLocation().hashCode();
    }

    private double recordAndGetDecay(UUID playerId, String blockName) {
        Deque<String> recent = recentPlacements.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        long occurrences = recent.stream().filter(blockName::equals).count();
        recent.addLast(blockName);
        while (recent.size() > DECAY_WINDOW) {
            recent.removeFirst();
        }
        return TradingLeveler.computeDecayMultiplier((int) occurrences);
    }

    /** Time-based "already gave XP here" cache. Extracted for testability. */
    static class PositionCache {
        private final long windowMs;
        private final Map<Long, Long> grants = new ConcurrentHashMap<>();

        PositionCache(long windowMs) {
            this.windowMs = windowMs;
        }

        boolean tryGrant(long key, long now) {
            Long last = grants.get(key);
            if (last != null && now - last < windowMs) {
                return false;
            }
            grants.put(key, now);
            if (grants.size() > 10_000) {
                grants.entrySet().removeIf(e -> now - e.getValue() >= windowMs);
            }
            return true;
        }
    }
}
```

Note: `positionKey` using `Location.hashCode()` is acceptable but prefer packing `x,z,y` plus `world.getUID().hashCode()`: `31 * (31 * (31L * block.getWorld().getUID().hashCode() + block.getX()) + block.getY()) + block.getZ()`.

`BukkitLevelManager.java` — append under the fork banner in `registerLevelers()`:

```java
        // === Fork skills: building, culinary, engineering ===
        registerLeveler(new BuildingLeveler(plugin));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :bukkit:test --tests "*BuildingLevelerTest*"`
Expected: PASS

- [ ] **Step 5: Compile the whole project**

Run: `./gradlew build -x javadoc -q`
Expected: green

- [ ] **Step 6: Commit**

```bash
git add api common bukkit
git commit -m "Add building source type with placement decay and position cache"
```

---

### Task 2: Building skill definition + XP sources

**Files:**
- Modify: `api/src/main/java/dev/aurelium/auraskills/api/skill/Skills.java` (append `BUILDING(Abilities.BUILDER)` under fork banner)
- Modify: `api/src/main/java/dev/aurelium/auraskills/api/ability/Abilities.java` (append 5 entries: `SPARE_MATERIALS("building"), BUILDER("building"), LONG_REACH("building"), RECLAIMER("building"), STEADY_HANDS("building")`)
- Modify: `common/src/main/resources/skills.yml` (append skill section, bump `file_version` 2→3)
- Create: `common/src/main/resources/sources/building.yml`
- Modify: `common/src/main/resources/abilities.yml` (append 5 ability sections, bump `file_version` 4→5)
- Create: `common/src/main/resources/rewards/building.yml`

**Interfaces:**
- Consumes: `SourceTypes.BUILDING` (Task 1); the `Skills`/`Abilities` enum pattern from round 1 (`Skills.java:34-36`, `Abilities.java:85-99`).
- Produces: skill `auraskills/building` loadable with sources; ability ids `auraskills/spare_materials`, `auraskills/builder`, `auraskills/long_reach`, `auraskills/reclaimer`, `auraskills/steady_hands`.

- [ ] **Step 1: Enum entries**

`Skills.java`, append after `TRADING(Abilities.MERCHANT);` — note the last enum constant currently ends with `;`, so convert it and add:

```java
    TRADING(Abilities.MERCHANT),
    // === Fork skills: building, culinary, engineering ===
    BUILDING(Abilities.BUILDER);
```

`Abilities.java`, same treatment after `GUILD_REPUTATION("trading");`:

```java
    GUILD_REPUTATION("trading"),
    // === Fork skills: building, culinary, engineering ===
    SPARE_MATERIALS("building"),
    BUILDER("building"),
    LONG_REACH("building"),
    RECLAIMER("building"),
    STEADY_HANDS("building");
```

- [ ] **Step 2: `skills.yml` section** (append before the end `# === Fork skills ===` / `file_version` line, then bump `file_version: 2` → `3` at the bottom of the file)

```yaml
  # === Fork skills ===
  auraskills/building:
    abilities:
      - auraskills/spare_materials
      - auraskills/builder
      - auraskills/long_reach
      - auraskills/reclaimer
      - auraskills/steady_hands
    mana_ability: auraskills/blueprint
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

(The mana ability is registered in Task 3; the reference is resolved lazily by name, but if startup complains, land Task 3's enum/config entries in the same commit.)

- [ ] **Step 3: `abilities.yml` sections** (append under fork banner, bump `file_version: 4` → `5`)

```yaml
  auraskills/spare_materials:
    enabled: true
    base_value: 1.5
    value_per_level: 1.5
    unlock: '{start}+1'
    level_up: 5
    max_level: 7
  auraskills/builder:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/long_reach:
    enabled: true
    base_value: 0.3
    value_per_level: 0.3
    unlock: '{start}+3'
    level_up: 5
    max_level: 5
  auraskills/reclaimer:
    enabled: true
    base_value: 3.0
    value_per_level: 3.0
    unlock: '{start}+4'
    level_up: 5
    max_level: 10
  auraskills/steady_hands:
    enabled: true
    base_value: 20.0
    value_per_level: 20.0
    unlock: '{start}+5'
    level_up: 5
    max_level: 5
```

- [ ] **Step 4: Generate `sources/building.yml` from the server jar**

Do not hand-guess block names. Repeat the extraction method used for the 26.2 source expansion:

```bash
cd /tmp && rm -rf bgen && mkdir bgen && cd bgen
unzip -o -q "/c/Users/Lucas/Desktop/backup_server_mine/minecraft/versions/26.2/paper-26.2.jar" "data/minecraft/recipe/*" -d recipes
# All results of crafting/smelting/stonecutting = the "processed" universe
```

Then build the tier lists per the spec §Skill: Building (basic 1.0 / mid 2.0 / noble 4.0 / prestigious 8.0), filtering the recipe-result universe down to placeable decorative/building blocks (exclude tools, armor, food, items without block form). Validate every name against the `Material` enum extracted from the paper-api jar (`javap` method used previously). The file format:

```yaml
sources:
  place_basic:
    type: building
    blocks: [<basic tier list>]
    xp: 1.0
    menu_item:
      material: oak_planks
  place_mid:
    type: building
    blocks: [<mid tier list>]
    xp: 2.0
    menu_item:
      material: bricks
  place_noble:
    type: building
    blocks: [<noble tier list>]
    xp: 4.0
    menu_item:
      material: gold_block
  place_prestigious:
    type: building
    blocks: [<prestigious tier list>]
    xp: 8.0
    menu_item:
      material: beacon

file_version: 1
```

- [ ] **Step 5: `rewards/building.yml`** (mirror `rewards/smithing.yml` exactly, swapping stats)

```yaml
patterns:
  - type: stat
    stat: toughness
    value: 1
    pattern:
      interval: 1
  - type: stat
    stat: health
    value: 1
    pattern:
      interval: 2
levels:
```

- [ ] **Step 6: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api common
git commit -m "Add Building skill definition with placement XP sources"
```

---

### Task 3: Building abilities + Blueprint + messages

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/building/BuildingAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/building/Blueprint.java`
- Modify: `api/src/main/java/dev/aurelium/auraskills/api/mana/ManaAbilities.java` (append `BLUEPRINT`)
- Modify: `common/src/main/java/dev/aurelium/auraskills/common/message/type/ManaAbilityMessage.java` (append `BLUEPRINT_START, BLUEPRINT_END, BLUEPRINT_RAISE, BLUEPRINT_LOWER`)
- Modify: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/mana/BukkitManaAbilityManager.java` (append `registerProvider(new Blueprint(plugin));`)
- Modify: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/ability/BukkitAbilityManager.java` (append `registerAbilityImpl(new BuildingAbilities(plugin));`)
- Modify: `common/src/main/resources/mana_abilities.yml` (append section, bump `file_version` 5→6)
- Modify: `common/src/main/resources/messages/messages_en.yml` and `messages_pt-BR.yml` (append entries; bump both `file_version` 40→41 at the end of this task — single bump covering Tasks 3/5/7)

**Interfaces:**
- Consumes: ability ids from Task 2; `ReadiedManaAbility` pattern (`bukkit/.../skills/husbandry/AnimalWhisperer.java`); `BukkitAbilityImpl` pattern (`bukkit/.../skills/smithing/SmithingAbilities.java`); `TradingLeveler.computeDecayMultiplier` not needed here.
- Produces: `BuildingAbilities` listener; `Blueprint` provider; messages keys `mana_abilities.blueprint.*`, `abilities.{spare_materials,builder,long_reach,reclaimer,steady_hands}.*`, `skills.building.*`.

- [ ] **Step 1: `BuildingAbilities.java`**

Mirror `SmithingAbilities` structure. Handlers:

```java
public class BuildingAbilities extends BukkitAbilityImpl {

    private static final UUID REACH_MODIFIER_ID = UUID.fromString("7e2f1c3a-9b4d-4e5f-8a6c-1d2e3f4a5b6c");

    public BuildingAbilities(AuraSkills plugin) {
        super(plugin, Abilities.SPARE_MATERIALS, Abilities.BUILDER, Abilities.LONG_REACH, Abilities.RECLAIMER, Abilities.STEADY_HANDS);
    }

    // spare_materials: on BlockPlaceEvent (MONITOR, ignoreCancelled), roll getValue/100;
    //   if success, schedule 1 tick later: refund one of the placed item to inventory (giveOrDrop).
    // reclaimer: on BlockPlaceEvent record position->player in an LRU Map (cap 50k);
    //   on BlockBreakEvent (MONITOR, ignoreCancelled) if position was placed by the breaker,
    //   roll and drop one extra ItemStack(blockType) via world.dropItemNaturally.
    // steady_hands: on EntityDamageEvent cause FALL, player holding a Block item in main hand,
    //   event.setDamage(damage * (1 - value/100)).
    // long_reach: on PlayerJoinEvent and on a 1-second repeating check ONLY if cheap — instead:
    //   apply on PlayerJoinEvent + after AbilityLevelUpEvent (check the event name used by the plugin,
    //   e.g. dev.aurelium.auraskills.api.event.ability.AbilityLevelUpEvent if it exists; otherwise
    //   SkillLevelUpEvent filtered by skill==Skills.BUILDING): set/remove a transient
    //   AttributeModifier on Attribute.BLOCK_INTERACTION_RANGE with the ability value.
    //   Remove the modifier when the level is 0. Blueprint adds a second modifier while active.
}
```

Implementation notes:
- `getValue(ability, user)`, `failsChecks(player, ability)`, `giveOrDrop(player, item)` and `rand` all exist on `BukkitAbilityImpl` — copy usage from `SmithingAbilities`.
- XP-boost abilities (`builder`) need no code — the `+% XP` is automatic from the ability value (same as `SMITH`/`RANCHER`/`MERCHANT`, which have no handler).
- Modifier: `new AttributeModifier(REACH_MODIFIER_ID, "auraskills.long_reach", value, AttributeModifier.Operation.ADD_NUMBER)`; apply via `player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)` after null-check, removing any existing modifier with the same id first.

- [ ] **Step 2: `Blueprint.java`**

Mirror `AnimalWhisperer` (including the `tryReady` improvement). Materials `new String[]{"BRICK"}`, actions both right-clicks. `onActivate`: add a transient `AttributeModifier` of +1.5 to `BLOCK_INTERACTION_RANGE` (own UUID, removed in `onStop`) and start a flag `activePlayers.add(uuid)` consumed by `BuildingAbilities` (Blueprint registers its own `BlockPlaceEvent` listener granting a 15% refund chance while active — implement the listener inside `Blueprint` to keep it self-contained). Duration via the standard `checkActivation` scheduling (no manual timer needed beyond effect removal — mirror how `AnimalWhisperer` schedules `onStop`).

- [ ] **Step 3: Registrations + `mana_abilities.yml`**

`ManaAbilities.java`: append `BLUEPRINT` to the fork list. `ManaAbilityMessage.java`: append the 4 enum entries. `BukkitManaAbilityManager.java`: append `registerProvider(new Blueprint(plugin));` under the fork banner. `BukkitAbilityManager.java`: append `registerAbilityImpl(new BuildingAbilities(plugin));` under the fork banner.

`mana_abilities.yml` (append, bump `file_version: 5` → `6`):

```yaml
  auraskills/blueprint:
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
```

- [ ] **Step 4: Messages** (en then pt-BR; `skills.building`, `abilities.*` name+desc+info where the pattern requires — mirror round-1 ability entries exactly, including `info` lines if round-1 abilities have them, `mana_abilities.blueprint.*`, and `sources.building.*`)

English:
```yaml
  building:
    name: Building
    desc: "Place processed blocks to earn Building {xp_unit}"
```
```yaml
    spare_materials:
      name: Spare Materials
      desc: "Grants a {value}% chance to not consume a block when placing it."
    builder:
      name: Builder
      desc: "Grants +{value}% Building {xp_unit}."
    long_reach:
      name: Long Reach
      desc: "Increases your block placement reach by {value} blocks."
    reclaimer:
      name: Reclaimer
      desc: "Grants a {value}% chance to get an extra drop when breaking a block you placed."
    steady_hands:
      name: Steady Hands
      desc: "Reduces fall damage by {value}% while holding a block."
```
```yaml
  blueprint:
    name: Blueprint
    desc: "For {value} seconds, +1.5 placement reach and 15% chance to not consume placed blocks. <1>[Right click bricks to activate]"
    raise: "<gray>You unroll a blueprint"
    lower: "<gray>You roll up the blueprint"
    start: "<gold>Blueprint Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>Blueprint has worn off"
```
```yaml
  building:
    place_basic: Place Basic Blocks
    place_mid: Place Processed Blocks
    place_noble: Place Noble Blocks
    place_prestigious: Place Prestigious Blocks
```

pt-BR:
```yaml
  building:
    name: Construção
    desc: "Coloque blocos processados para ganhar {xp_unit} de Construção"
```
```yaml
    spare_materials:
      name: Materiais Sobressalentes
      desc: "Concede {value}% de chance de não consumir o bloco ao colocá-lo."
    builder:
      name: Construtor
      desc: "Concede +{value}% de {xp_unit} de Construção."
    long_reach:
      name: Longo Alcance
      desc: "Aumenta seu alcance de colocação de blocos em {value} blocos."
    reclaimer:
      name: Recuperador
      desc: "Concede {value}% de chance de obter um drop extra ao quebrar um bloco que você colocou."
    steady_hands:
      name: Mãos Firmes
      desc: "Reduz o dano de queda em {value}% enquanto segura um bloco."
```
```yaml
  blueprint:
    name: Projeto
    desc: "Por {value} segundos, +1.5 de alcance e 15% de chance de não consumir blocos colocados. <1>[Clique com tijolos para ativar]"
    raise: "<gray>Você desenrola um projeto"
    lower: "<gray>Você enrola o projeto"
    start: "<gold>Projeto Ativado! <gray>(-{mana} {mana_unit})"
    end: "<gray>Projeto acabou"
```
```yaml
  building:
    place_basic: Coloque Blocos Básicos
    place_mid: Coloque Blocos Processados
    place_noble: Coloque Blocos Nobres
    place_prestigious: Coloque Blocos Prestigiados
```

Set `file_version: 41` in both message files only in Task 7 (one bump for the whole round).

- [ ] **Step 5: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api bukkit common
git commit -m "Implement Building abilities and Blueprint mana ability"
```

---

### Task 4: Culinary skill definition + sources + campfire listener + migration

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/CampfireLeveler.java`
- Modify: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/level/BukkitLevelManager.java` (append `registerLeveler(new CampfireLeveler(plugin));` under the fork banner)
- Modify: `api/src/main/java/dev/aurelium/auraskills/api/skill/Skills.java` (append `CULINARY(Abilities.CHEF)`)
- Modify: `api/src/main/java/dev/aurelium/auraskills/api/ability/Abilities.java` (append `EXTRA_SERVINGS("culinary"), CHEF("culinary"), HEARTY_MEALS("culinary"), GOURMET("culinary"), WELL_FED("culinary")`)
- Modify: `common/src/main/resources/skills.yml` (append `auraskills/culinary` section)
- Create: `common/src/main/resources/sources/culinary.yml`
- Modify: `common/src/main/resources/sources/smithing.yml` (remove `smelt_food`, bump `file_version` 2→3)
- Modify: `common/src/main/resources/abilities.yml` (append 5 sections)
- Create: `common/src/main/resources/rewards/culinary.yml` (health interval 1, regeneration interval 2)

**Interfaces:**
- Consumes: `SourceTypes.SMELTING` + `SmeltingXpSource` matching (`SmeltingLeveler.matchesAny`); `BlockCookEvent` (org.bukkit.event.block, confirmed present in 26.2 API with `getBlock()`, `getSource()`, `getResult()`).
- Produces: `CampfireLeveler` granting SMELTING-type sources on campfire cooks, attributed to the player who placed the food; skill `auraskills/culinary`; Smithing without `smelt_food` on new and existing servers (file_version 3 merge).

- [ ] **Step 1: `CampfireLeveler.java`**

Behavior: track who places raw food on a campfire (`PlayerInteractEvent` right-click on a `CAMPFIRE`/`SOUL_CAMPFIRE` holding an item that has a campfire recipe); when `BlockCookEvent` fires for that block, grant the matching SMELTING source's XP (flat, no amount multiplier — campfire cooks one item at a time) to that player if online.

```java
package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.SkillSource;
import dev.aurelium.auraskills.api.source.type.SmeltingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceTypes;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CampfireLeveler extends SourceLeveler {

    private static final long ATTRIBUTION_MS = 10 * 60 * 1000L;

    // block key -> (player id -> timestamp); one campfire has 4 slots, tracking per-block is enough
    private final Map<Long, Map<UUID, Long>> placements = new ConcurrentHashMap<>();

    public CampfireLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.SMELTING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceFood(PlayerInteractEvent event) {
        if (disabled()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        ItemStack held = event.getItem();
        if (held == null || !isCampfireCookable(held)) return;

        placements.computeIfAbsent(blockKey(block), k -> new ConcurrentHashMap<>())
                .put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (disabled()) return;
        Map<UUID, Long> cooks = placements.remove(blockKey(event.getBlock()));
        if (cooks == null) return;

        String resultName = event.getResult().getType().name();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> cook : cooks.entrySet()) {
            if (now - cook.getValue() > ATTRIBUTION_MS) continue;
            Player player = Bukkit.getPlayer(cook.getKey());
            if (player == null) continue;

            for (SkillSource<SmeltingXpSource> entry : plugin.getSkillManager().getSourcesOfType(SmeltingXpSource.class)) {
                SmeltingXpSource source = entry.source();
                if (!SmeltingLeveler.matchesAny(source.getItems(), resultName)) continue;

                Skill skill = entry.skill();
                if (failsChecks(event, player, event.getBlock().getLocation(), skill)) return;

                User user = plugin.getUser(player);
                plugin.getLevelManager().addXp(user, skill, source, source.getXp());
                return;
            }
        }
    }

    private boolean isCampfireCookable(ItemStack item) {
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof CampfireRecipe campfireRecipe
                    && campfireRecipe.getInputChoice().test(item)) {
                return true;
            }
        }
        return false;
    }

    private static long blockKey(Block block) {
        return 31L * (31L * (31L * block.getWorld().getUID().hashCode() + block.getX()) + block.getY()) + block.getZ();
    }
}
```

Note: one cook completing clears all pending cooks for the block (campfires finish slots independently but this is good enough — slots placed within the same window usually belong to the same player; re-placement re-registers). If `BlockCookEvent` does not fire for campfires on the test server (verify in-game with a campfire + raw beef and a `/sk xp add`-style check of the Culinary XP bar), fall back: drop this class and the campfire source, and note it in the final report.

- [ ] **Step 2: Enum + `skills.yml` + `abilities.yml` + rewards**

`Skills.java`: append `CULINARY(Abilities.CHEF),` after `BUILDING(Abilities.BUILDER)` (convert the `;`). `Abilities.java`: append the 5 culinary entries.

`skills.yml` (append after building):

```yaml
  auraskills/culinary:
    abilities:
      - auraskills/extra_servings
      - auraskills/chef
      - auraskills/hearty_meals
      - auraskills/gourmet
      - auraskills/well_fed
    mana_ability: auraskills/banquet
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

`abilities.yml` (append):

```yaml
  auraskills/extra_servings:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+1'
    level_up: 5
    max_level: 10
  auraskills/chef:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/hearty_meals:
    enabled: true
    base_value: 4.0
    value_per_level: 4.0
    unlock: '{start}+3'
    level_up: 5
    max_level: 5
  auraskills/gourmet:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+4'
    level_up: 5
    max_level: 10
  auraskills/well_fed:
    enabled: true
    base_value: 1.0
    value_per_level: 1.0
    unlock: '{start}+5'
    level_up: 5
    max_level: 5
```

`rewards/culinary.yml`: same pattern, `health` interval 1, `regeneration` interval 2.

- [ ] **Step 3: `sources/culinary.yml` + Smithing migration**

`culinary.yml`:

```yaml
sources:
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
    xp: 1.0
    menu_item:
      material: cooked_beef
  craft_bread_and_snacks:
    type: crafting
    items:
      - bread
      - cookie
    xp: 1.5
    menu_item:
      material: bread
  craft_soups:
    type: crafting
    items:
      - mushroom_stew
      - beetroot_soup
      - suspicious_stew
    xp: 2.5
    menu_item:
      material: mushroom_stew
  craft_rich_meals:
    type: crafting
    items:
      - pumpkin_pie
      - rabbit_stew
    xp: 4.0
    menu_item:
      material: pumpkin_pie
  craft_cake:
    type: crafting
    items:
      - cake
    xp: 8.0
    menu_item:
      material: cake
  craft_honey:
    type: crafting
    items:
      - honey_bottle
    xp: 2.0
    menu_item:
      material: honey_bottle
  craft_golden_food:
    type: crafting
    items:
      - golden_carrot
      - golden_apple
    xp: 10.0
    menu_item:
      material: golden_carrot

file_version: 1
```

`smithing.yml`: delete the whole `smelt_food:` block and set `file_version: 3`.

Register the removal for existing servers — `common/.../source/SourceFileUpdates.java`, inside the constructor (upstream file, append with banner):

```java
        // === Fork skills: building, culinary, engineering ===
        define(Skills.SMITHING, 3, this::removeSmeltFood);
```

plus the method:

```java
    private void removeSmeltFood(ConfigurationNode embedded, ConfigurationNode user) {
        user.node("sources").removeChild("smelt_food");
    }
```

- [ ] **Step 4: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api bukkit common
git commit -m "Add Culinary skill, move food XP out of Smithing, support campfire cooking"
```

---

### Task 5: Culinary abilities + Banquet + messages

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/culinary/CulinaryAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/culinary/Banquet.java`
- Modify: `api/.../api/mana/ManaAbilities.java` (append `BANQUET`)
- Modify: `common/.../message/type/ManaAbilityMessage.java` (append `BANQUET_START, BANQUET_END, BANQUET_RAISE, BANQUET_LOWER`)
- Modify: `bukkit/.../mana/BukkitManaAbilityManager.java` (append provider)
- Modify: `bukkit/.../ability/BukkitAbilityManager.java` (append impl)
- Modify: `common/src/main/resources/mana_abilities.yml` (append `auraskills/banquet`, same option shape as Blueprint, no extra options)
- Modify: both messages files (append)

**Interfaces:**
- Consumes: culinary ability ids (Task 4); `ReadiedManaAbility` pattern.
- Produces: `CulinaryAbilities`, `Banquet`; messages `mana_abilities.banquet.*`, `abilities.{extra_servings,chef,hearty_meals,gourmet,well_fed}.*`, `skills.culinary.*`, `sources.{smelting.smelt_food already exists, crafting.craft_*}.*`.

- [ ] **Step 1: `CulinaryAbilities.java`**

Mirror `SmithingAbilities`. Handlers:

```java
// extra_servings: FurnaceExtractEvent (HIGH, ignoreCancelled) — only when the extracted
//   item is one of the culinary food items (reuse a static Set<Material> of the 9 foods);
//   roll, then giveOrDrop an extra stack of the same type/amount.
// gourmet: CraftItemEvent — result is a culinary craft item (bread..golden_apple set);
//   roll, then refund one random non-air ingredient from event.getInventory().getMatrix().
// hearty_meals + well_fed: PlayerItemConsumeEvent —
//   well_fed: if the consumed item is edible and not a raw/basic crop (use a cooked/crafted
//     food set), apply PotionEffect REGENERATION duration value*20 ticks, level 0.
//   hearty_meals: tag ownership at production time: in extra_servings' FurnaceExtractEvent
//     and in craft handlers, edit the result ItemStack meta PersistentDataContainer with the
//     player's UUID (key "auraskills:chef"); on consume, if the tag matches the eater,
//     schedule 1 tick later: player.setFoodLevel(min(20, foodLevel + ceil(nutrition * value/100)))
//     using the item's default nutrition from a small static table (bread 5, cooked meats 8,
//     baked_potato 5, cake 2/slice is special — skip cake) — keep the table to the 9 smelt
//     foods + craft items; unknown items fall back to no bonus.
```

Keep the PDC key in a `public static final NamespacedKey CHEF_KEY` on `CulinaryAbilities` so `Banquet` could reuse it if needed.

- [ ] **Step 2: `Banquet.java`**

Mirror `AnimalWhisperer` (with `tryReady`). Materials `new String[]{"BOWL"}`; activation on second right-click with the bowl (air or block — use the plain `onReady`/`checkActivation` flow: ready via right-click air, then activate via right-click air again — mirror `ReadiedManaAbility` usage in `ForgeOverdrive` if it activates on second interact, otherwise implement `activationListener(PlayerInteractEvent)` calling `checkActivation` on the second right-click). `onActivate`: for every player within 10 blocks (including self) apply `Saturation` and `Regeneration` level 0 for the ability duration ticks. `onStop`: nothing (effects expire naturally).

- [ ] **Step 3: Registrations + `mana_abilities.yml`** (same as Task 3 Step 3, with `BANQUET`/`Banquet`)

- [ ] **Step 4: Messages**

English:
```yaml
  culinary:
    name: Culinary
    desc: "Cook and craft food to earn Culinary {xp_unit}"
```
```yaml
    extra_servings:
      name: Extra Servings
      desc: "Grants a {value}% chance to get double results when extracting cooked food."
    chef:
      name: Chef
      desc: "Grants +{value}% Culinary {xp_unit}."
    hearty_meals:
      name: Hearty Meals
      desc: "Food you cooked or crafted restores {value}% more hunger."
    gourmet:
      name: Gourmet
      desc: "Grants a {value}% chance to not consume one ingredient when crafting food."
    well_fed:
      name: Well Fed
      desc: "Eating cooked or crafted food grants Regeneration for {value} seconds."
```
```yaml
  banquet:
    name: Banquet
    desc: "For {value} seconds, you and nearby players gain Saturation and Regeneration. <1>[Right click a bowl to activate]"
    raise: "<gray>You raise a feast bowl"
    lower: "<gray>You lower the bowl"
    start: "<gold>Banquet Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>The banquet has ended"
```
```yaml
    craft_bread_and_snacks: Bake Bread and Snacks
    craft_soups: Cook Soups and Stews
    craft_rich_meals: Cook Rich Meals
    craft_cake: Bake a Cake
    craft_honey: Bottle Honey
    craft_golden_food: Craft Golden Food
```
Note: `sources.smelting.smelt_food` ("Cook Food") already exists from the Smithing round — no new entry needed. The `craft_*` keys go under `sources.crafting` in the messages file.

pt-BR:
```yaml
  culinary:
    name: Culinária
    desc: "Cozinhe e fabrique comidas para ganhar {xp_unit} de Culinária"
```
```yaml
    extra_servings:
      name: Porções Extras
      desc: "Concede {value}% de chance de obter resultado dobrado ao retirar comida cozida."
    chef:
      name: Chef
      desc: "Concede +{value}% de {xp_unit} de Culinária."
    hearty_meals:
      name: Refeições Fartas
      desc: "Comida que você cozinhou ou fabricou restaura {value}% mais fome."
    gourmet:
      name: Gourmet
      desc: "Concede {value}% de chance de não consumir um ingrediente ao fabricar comida."
    well_fed:
      name: Bem Alimentado
      desc: "Comer comida cozida ou fabricada concede Regeneração por {value} segundos."
```
```yaml
  banquet:
    name: Banquete
    desc: "Por {value} segundos, você e jogadores próximos ganham Saturação e Regeneração. <1>[Clique com uma tigela para ativar]"
    raise: "<gray>Você ergue uma tigela de banquete"
    lower: "<gray>Você abaixa a tigela"
    start: "<gold>Banquete Ativado! <gray>(-{mana} {mana_unit})"
    end: "<gray>O banquete terminou"
```
```yaml
    craft_bread_and_snacks: Asse Pão e Petiscos
    craft_soups: Cozinhe Sopas e Ensopados
    craft_rich_meals: Cozinhe Refeições Ricas
    craft_cake: Asse um Bolo
    craft_honey: Engarrafe Mel
    craft_golden_food: Fabrique Comida Dourada
```

- [ ] **Step 5: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api bukkit common
git commit -m "Implement Culinary abilities and Banquet mana ability"
```

---

### Task 6: Engineering skill definition + XP sources

**Files:**
- Modify: `api/.../api/skill/Skills.java` (append `ENGINEERING(Abilities.ENGINEER);`)
- Modify: `api/.../api/ability/Abilities.java` (append `SPARE_PARTS("engineering"), ENGINEER("engineering"), EFFICIENT_CRAFTING("engineering"), RAILROAD_BARON("engineering"), TINKERERS_LUCK("engineering")`)
- Modify: `common/src/main/resources/skills.yml` (append `auraskills/engineering`)
- Create: `common/src/main/resources/sources/engineering.yml`
- Modify: `common/src/main/resources/abilities.yml` (append 5 sections)
- Create: `common/src/main/resources/rewards/engineering.yml` (wisdom interval 1, luck interval 2)

**Interfaces:**
- Consumes: `SourceTypes.CRAFTING` (round 1), `SourceTypes.BUILDING` (Task 1).
- Produces: skill `auraskills/engineering` with sources.

- [ ] **Step 1: Enums + `skills.yml` + `abilities.yml` + rewards**

Same shapes as Tasks 2/4. `skills.yml` section:

```yaml
  auraskills/engineering:
    abilities:
      - auraskills/spare_parts
      - auraskills/engineer
      - auraskills/efficient_crafting
      - auraskills/railroad_baron
      - auraskills/tinkerers_luck
    mana_ability: auraskills/power_surge
    options:
      enabled: true
      max_level: 100
      check_cancelled: true
      check_multiplier_permissions: true
```

`abilities.yml` sections (values from the spec):

```yaml
  auraskills/spare_parts:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+1'
    level_up: 5
    max_level: 10
  auraskills/engineer:
    enabled: true
    base_value: 10.0
    value_per_level: 10.0
    unlock: '{start}+2'
    level_up: 5
    max_level: 0
  auraskills/efficient_crafting:
    enabled: true
    base_value: 2.0
    value_per_level: 2.0
    unlock: '{start}+3'
    level_up: 5
    max_level: 10
  auraskills/railroad_baron:
    enabled: true
    base_value: 5.0
    value_per_level: 5.0
    unlock: '{start}+4'
    level_up: 5
    max_level: 5
  auraskills/tinkerers_luck:
    enabled: true
    base_value: 1.5
    value_per_level: 1.5
    unlock: '{start}+5'
    level_up: 5
    max_level: 8
```

- [ ] **Step 2: `sources/engineering.yml`**

```yaml
sources:
  craft_bulk_rail:
    type: crafting
    items:
      - rail
      - activator_rail
    xp: 0.2
    menu_item:
      material: rail
  craft_special_rail:
    type: crafting
    items:
      - powered_rail
      - detector_rail
    xp: 0.5
    menu_item:
      material: powered_rail
  craft_basic_components:
    type: crafting
    items:
      - redstone_torch
      - lever
      - note_block
      - tripwire_hook
    xp: 1.0
    menu_item:
      material: redstone_torch
  craft_plates_and_doors:
    type: crafting
    items:
      - light_weighted_pressure_plate
      - heavy_weighted_pressure_plate
      - iron_door
      - iron_trapdoor
    xp: 1.5
    menu_item:
      material: iron_door
  craft_mid_components:
    type: crafting
    items:
      - repeater
      - lightning_rod
      - minecart
      - dropper
      - target
    xp: 2.5
    menu_item:
      material: repeater
  craft_advanced_components:
    type: crafting
    items:
      - observer
      - comparator
      - dispenser
      - redstone_lamp
      - piston
      - daylight_detector
    xp: 3.0
    menu_item:
      material: piston
  craft_utility_vehicles:
    type: crafting
    items:
      - chest_minecart
      - furnace_minecart
      - hopper_minecart
    xp: 3.5
    menu_item:
      material: hopper_minecart
  craft_heavy_components:
    type: crafting
    items:
      - tnt
      - sticky_piston
      - hopper
      - tnt_minecart
      - crafter
    xp: 4.0
    menu_item:
      material: hopper
  craft_jukebox:
    type: crafting
    items:
      - jukebox
    xp: 6.0
    menu_item:
      material: jukebox
  place_rail:
    type: building
    blocks:
      - rail
      - powered_rail
      - detector_rail
      - activator_rail
    xp: 0.25
    menu_item:
      material: rail
  place_basic_components:
    type: building
    blocks:
      - lever
      - redstone_torch
      - stone_pressure_plate
      - oak_pressure_plate
      - stone_button
      - oak_button
      - tripwire_hook
    xp: 0.5
    menu_item:
      material: lever
  place_components:
    type: building
    blocks:
      - piston
      - sticky_piston
      - observer
      - dispenser
      - dropper
      - repeater
      - comparator
      - hopper
      - redstone_lamp
      - target
      - crafter
      - daylight_detector
      - lightning_rod
      - note_block
      - jukebox
    xp: 1.0
    menu_item:
      material: observer

file_version: 1
```

- [ ] **Step 3: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api common
git commit -m "Add Engineering skill definition with component XP sources"
```

---

### Task 7: Engineering abilities + Power Surge + messages

**Files:**
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/engineering/EngineeringAbilities.java`
- Create: `bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/engineering/PowerSurge.java`
- Modify: `api/.../api/mana/ManaAbilities.java` (append `POWER_SURGE`)
- Modify: `common/.../message/type/ManaAbilityMessage.java` (append `POWER_SURGE_START, POWER_SURGE_END, POWER_SURGE_RAISE, POWER_SURGE_LOWER`)
- Modify: `bukkit/.../mana/BukkitManaAbilityManager.java` (append provider)
- Modify: `bukkit/.../ability/BukkitAbilityManager.java` (append impl)
- Modify: `common/src/main/resources/mana_abilities.yml` (append `auraskills/power_surge` with `double_chance: 50.0` custom option)
- Modify: both messages files; set `file_version: 41` in **both** `messages_en.yml` and `messages_pt-BR.yml` (single bump for Tasks 3/5/7)

**Interfaces:**
- Consumes: engineering ability ids (Task 6); component material sets — define `static final Set<Material> COMPONENTS` in `EngineeringAbilities` (union of the craft/place lists from `sources/engineering.yml`) and reference it from `PowerSurge` via `EngineeringAbilities.COMPONENTS`.
- Produces: `EngineeringAbilities`, `PowerSurge`; all engineering message keys.

- [ ] **Step 1: `EngineeringAbilities.java`**

```java
// spare_parts: BlockBreakEvent (MONITOR, ignoreCancelled) — block type in COMPONENTS,
//   roll, world.dropItemNaturally(block location, new ItemStack(blockType)).
// efficient_crafting: CraftItemEvent — result type in COMPONENTS, roll, refund one
//   random non-air ingredient from the matrix.
// tinkerers_luck: CraftItemEvent — result in COMPONENTS, roll, giveOrDrop an extra
//   result stack (same type/amount as recipe result).
// railroad_baron: VehicleEnterEvent — vehicle instanceof Minecart and passenger is Player;
//   store cart id -> previous max speed; cart.setMaxSpeed(base * (1 + value/100)).
//   VehicleExitEvent — restore. Also VehicleDestroyEvent — drop the record.
```

- [ ] **Step 2: `PowerSurge.java`**

Mirror `ForgeOverdrive`'s activation shape (it activates on a block click): materials `new String[]{"REDSTONE"}`; `activationListener(PlayerInteractEvent)` — `RIGHT_CLICK_BLOCK` where the clicked block is a `CRAFTING_TABLE` → `checkActivation(player)`. While active (track `Set<UUID>` with removal in `onStop`): `CraftItemEvent` where result is in `EngineeringAbilities.COMPONENTS` → `double_chance`% (option, default 50) roll → giveOrDrop extra result. Share the set via `EngineeringAbilities.COMPONENTS`.

- [ ] **Step 3: Registrations + `mana_abilities.yml`**

```yaml
  auraskills/power_surge:
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
    double_chance: 50.0
```

- [ ] **Step 4: Messages + file_version 41**

English:
```yaml
  engineering:
    name: Engineering
    desc: "Craft and place mechanical components to earn Engineering {xp_unit}"
```
```yaml
    spare_parts:
      name: Spare Parts
      desc: "Grants a {value}% chance of double drops when breaking mechanisms."
    engineer:
      name: Engineer
      desc: "Grants +{value}% Engineering {xp_unit}."
    efficient_crafting:
      name: Efficient Crafting
      desc: "Grants a {value}% chance to not consume one ingredient when crafting components."
    railroad_baron:
      name: Railroad Baron
      desc: "Minecarts you ride travel {value}% faster."
    tinkerers_luck:
      name: Tinkerer's Luck
      desc: "Grants a {value}% chance for a component craft to yield double results."
```
```yaml
  power_surge:
    name: Power Surge
    desc: "For {value} seconds, component crafts have a 50% chance of double output. <1>[Right click redstone dust, then click a crafting table]"
    raise: "<gray>You charge a handful of redstone"
    lower: "<gray>You pocket the redstone"
    start: "<gold>Power Surge Activated! <gray>(-{mana} {mana_unit})"
    end: "<gray>Power Surge has worn off"
```
```yaml
    craft_bulk_rail: Craft Rails
    craft_special_rail: Craft Special Rails
    craft_basic_components: Craft Basic Components
    craft_plates_and_doors: Craft Plates and Iron Doors
    craft_mid_components: Craft Components
    craft_advanced_components: Craft Advanced Components
    craft_utility_vehicles: Craft Utility Minecarts
    craft_heavy_components: Craft Heavy Components
    craft_jukebox: Craft a Jukebox
    place_rail: Lay Rails
    place_basic_components: Place Basic Components
    place_components: Place Components
```
(these go under `sources.crafting` and `sources.building` respectively in the messages file)

pt-BR:
```yaml
  engineering:
    name: Engenharia
    desc: "Fabrique e coloque componentes mecânicos para ganhar {xp_unit} de Engenharia"
```
```yaml
    spare_parts:
      name: Peças Sobressalentes
      desc: "Concede {value}% de chance de drops dobrados ao quebrar mecanismos."
    engineer:
      name: Engenheiro
      desc: "Concede +{value}% de {xp_unit} de Engenharia."
    efficient_crafting:
      name: Fabricação Eficiente
      desc: "Concede {value}% de chance de não consumir um ingrediente ao fabricar componentes."
    railroad_baron:
      name: Barão da Ferrovia
      desc: "Carrinhos que você conduz viajam {value}% mais rápido."
    tinkerers_luck:
      name: Sorte de Inventor
      desc: "Concede {value}% de chance de uma fabricação de componente render o dobro."
```
```yaml
  power_surge:
    name: Surto de Energia
    desc: "Por {value} segundos, fabricações de componentes têm 50% de chance de render o dobro. <1>[Clique com redstone, depois clique numa bancada]"
    raise: "<gray>Você carrega um punhado de redstone"
    lower: "<gray>Você guarda a redstone"
    start: "<gold>Surto de Energia Ativado! <gray>(-{mana} {mana_unit})"
    end: "<gray>Surto de Energia acabou"
```
```yaml
    craft_bulk_rail: Fabrique Trilhos
    craft_special_rail: Fabrique Trilhos Especiais
    craft_basic_components: Fabrique Componentes Básicos
    craft_plates_and_doors: Fabrique Chapas e Portas de Ferro
    craft_mid_components: Fabrique Componentes
    craft_advanced_components: Fabrique Componentes Avançados
    craft_utility_vehicles: Fabrique Carrinhos Utilitários
    craft_heavy_components: Fabrique Componentes Pesados
    craft_jukebox: Fabrique uma Jukebox
    place_rail: Assente Trilhos
    place_basic_components: Coloque Componentes Básicos
    place_components: Coloque Componentes
```

Then set `file_version: 41` at the end of `messages_en.yml` and `messages_pt-BR.yml`.

- [ ] **Step 5: Build + commit**

Run: `./gradlew build -x javadoc -q` (green)

```bash
git add api bukkit common
git commit -m "Implement Engineering abilities and Power Surge mana ability"
```

---

### Task 8: Final integration, menus, docs, deploy

**Files:**
- Modify: `bukkit/src/main/resources/menus/skills.yml` (add `building` to `fourth_row` order 4 material `bricks`; new `fifth_row` group with `culinary` order 1 material `cooked_beef`, `engineering` order 2 material `piston`)
- Modify: `bukkit/src/main/resources/menus/level_progression.yml` (append 3 contexts under the fork banner)
- Modify: `Changelog.md` (new fork section entry)
- Modify: `wiki/index.md`, `wiki/skills/index.md` (14 → 17 skills)
- Modify: `wiki/sources.md` (add `### Building` section between Block and Breeding)
- Modify: `wiki/abilities.md` (no new ability-specific options exist this round — verify; skip if none)
- Modify: `wiki/mana-abilities.md` (3 new rows + `double_chance` option row)

**Interfaces:**
- Consumes: everything above.
- Produces: a deployable jar and verified server.

- [ ] **Step 1: Menus**

`menus/skills.yml` — contexts (next to the existing fork contexts) and groups:

```yaml
      building:
        group: fourth_row
        order: 4
        material: bricks
      culinary:
        group: fifth_row
        order: 1
        material: cooked_beef
      engineering:
        group: fifth_row
        order: 2
        material: piston
```

```yaml
      fifth_row:
        start: 5,0
        end: 5,8
        align: center
```

`menus/level_progression.yml` — append after the fork contexts:

```yaml
      building:
        material: bricks
      culinary:
        material: cooked_beef
      engineering:
        material: piston
```

The startup template merge (MenuFileManager) distributes both to existing servers automatically. Do not bump menu `file_version` (the merge is version-independent).

- [ ] **Step 2: Wiki**

- `wiki/index.md` + `wiki/skills/index.md`: 14 → 17, add Building, Culinary, Engineering to the lists.
- `wiki/sources.md`, insert after the `### Block` section:

```markdown
### Building

The building source (`type: building`) gives XP when a player places a block. XP is flat per placement; repeating the same block type within a rolling window of 8 placements applies a decay that progressively reduces XP, and a position that already gave placement XP gives none again for 5 minutes (anti place/break loop).

#### Options

* `block` - A material name in all lowercase that gives XP when placed. (Required)
* `blocks` - A list of multiple valid materials.
```

- `wiki/mana-abilities.md`: add rows for Blueprint (Building), Banquet (Culinary), Power Surge (Engineering) mirroring the in-game descriptions; add `double_chance` (Power Surge) to the ability-specific options table.
- `wiki/abilities.md`: no new ability-specific options this round — verify each new ability only uses common options; if one was added in implementation, document it.

- [ ] **Step 3: Changelog**

Add a new entry at the top of `Changelog.md` describing the three skills, the Smithing `smelt_food` migration, and the `building` source type, under an `## Unreleased` or the fork's existing heading style.

- [ ] **Step 4: Full build**

Run: `./gradlew build -x javadoc`
Expected: green

- [ ] **Step 5: Deploy + startup verification**

```bash
cp bukkit/build/libs/AuraSkills-2.4.0.jar "/c/Users/Lucas/Desktop/backup_server_mine/minecraft/plugins/AuraSkills-2.4.0.jar"
```

Start (or restart) the test server and verify in `logs/latest.log`:
- `Loaded 17 skills with ... total sources` (source count increased)
- `Menu file skills.yml was updated: ... new template context(s)/group(s) added` and same for `level_progression.yml`
- No `Error loading source file`, no AuraSkills stack traces
- Server's `plugins/AuraSkills/sources/smithing.yml` has `file_version: 3` and no `smelt_food` key
- Server's message files contain `mana_abilities.blueprint` (file_version 41 merge)

- [ ] **Step 6: In-game checklist** (hand to the user)

- `/skills` shows 17 skills with icons and pt-BR descriptions; clicking each new skill shows the skill item (not empty)
- Placing stone bricks gives Building XP; placing dirt gives none; spamming the same block decays; breaking/replacing the same spot gives nothing
- Cooking beef in a furnace gives Culinary XP (not Smithing); campfire cook gives Culinary XP (verdict on `BlockCookEvent` — report to the user either way)
- Crafting a piston gives Engineering XP; placing it gives Engineering XP
- Each mana ability: ready → activate → visible effect
- `/trading`, `/smithing` etc. still work (regression pass on round-1 skills)

- [ ] **Step 7: Commit + push**

```bash
git add bukkit common wiki Changelog.md
git commit -m "Finalize Building, Culinary and Engineering integration: menus, changelog, wiki"
git push origin master
```

---

## Self-Review Notes (already applied)

- Campfire attribution is per-block, not per-slot: documented simplification with an in-game verification step and an explicit fallback (drop the campfire source) if `BlockCookEvent` does not fire for campfires on 26.2.
- `long_reach` uses a transient `AttributeModifier` on `BLOCK_INTERACTION_RANGE` (confirmed present in the 26.2 API). If the plugin has no `AbilityLevelUpEvent`, apply on `PlayerJoinEvent` and on a lightweight 20s periodic refresh for online players instead; do not add a per-tick check.
- `hearty_meals` nutrition table is intentionally small (9 smelt foods + craft foods); cake excluded (slice semantics).
- The message `file_version` bump to 41 happens once (Task 7) to trigger a single polyglot merge for all three skills.
- Menu merge is version-independent (MenuFileManager merges missing contexts/groups every startup), so menu files keep their `file_version`.
