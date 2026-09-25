# Design: Building, Culinary and Engineering skills

Date: 2026-09-25
Status: Approved design (pending user spec review)

## Overview

Add three new skills to the fork, following the complete pattern established by the
previous round (Trading, Husbandry, Smithing): skill definition, 5 abilities,
1 mana ability, XP sources, rewards, menus, messages (en + pt-BR), wiki docs and
out-of-the-box upgrades for existing servers.

Approved scope decisions from brainstorming:

- **Building**: XP only for *processed* blocks (anything that is the result of a
  crafting, smelting or stonecutting recipe). Raw world blocks (dirt, cobblestone,
  sand, logs, ores, etc.) give nothing.
- **Culinary**: all food-cooking XP migrates out of Smithing into Culinary.
- **Engineering**: XP from crafting and placing redstone/mechanical components.
- **Balance mandate**: no inflated XP for spammable actions. Apply the lessons from
  the previous round (see Balance framework).

## Balance framework (lessons applied)

1. **Amount-multiplied XP only where the input is expensive.** Multiplication by
   stack size is what made ore smelting OP. Cheap spammable actions are flat or
   very low per unit.
2. **Tiers by material cost/rarity**, like farming crops and husbandry animals.
3. **Decay for repeated identical actions** — the Trading pattern: a rolling window
   of 8 recent actions per player; the same action repeated within the window gets
   `xp * max(0.1, 0.5^(n-1))`.
4. **Position cache for placement** — a location that already gave placement XP
   gives none again for 5 minutes (kills place/break/place loops).
5. **Batch-size awareness in crafting sources** — the crafting source multiplies by
   result amount, so items that craft in bulk (rails ×16) get proportionally tiny
   values; the target is roughly uniform XP per material spent.

## New source type: `building`

New file `BuildingSource` (+ parser, + `BuildingLeveler` with `BlockPlaceEvent`),
registered in `SourceTypes`. Also reused by Engineering (per-skill config).

Options:

* `block` / `blocks` - material name(s), lowercase. (Required)
* `xp` - flat XP per block placed, before decay. (Required)

Leveler behavior:

* Flat XP per placement (no amount multiplication).
* Per-player decay on repeating the same block type within a rolling window of 8
  placements (Trading formula, floor 0.1).
* Position cache: positions that granted placement XP are blocked for 5 minutes,
  even across block types (anti place/break loop).
* Respects `check_cancelled` and disabled worlds.

## Skill: Building (pt-BR: Construção)

Stats: Toughness (every level), Health (every 2 levels). Mana ability at unlock 6.

### Sources (`sources/building.yml`, type `building`)

XP per block placed (before decay):

* Tier basic processed — **1.0**: planks (all woods), stone bricks + variants,
  glass, slabs/stairs/walls/fences of wood and stone, doors, trapdoors, signs,
  ladders, scaffolding, buttons, pressure plates, glass panes
* Tier mid processed — **2.0**: bricks, all terracotta (incl. dyed), concrete +
  concrete powder, wool, cut/waxed copper blocks, mud bricks, resin bricks,
  quartz block + pillar, smooth stone variants, sandstone/red sandstone variants,
  stairs/slabs of brick-tier blocks, tinted glass
* Tier noble — **4.0**: glazed terracotta (all 16), purpur, end stone bricks,
  red/chiseled nether bricks, mineral blocks (iron, gold, copper, lapis, redstone,
  coal, emerald, diamond), amethyst block, honeycomb block, magma block,
  glowstone, sea lantern, shroomlight
* Tier prestigious — **8.0**: netherite block, enchanting table, anvil, ender
  chest, lodestone, respawn anchor, beacon, conduit, jukebox

The complete block list is generated from the server jar's recipe data (same
method used for the 26.2 source expansion), never guessed.

### Abilities

| Ability | Effect | base/per_level | max_level |
|---|---|---|---|
| spare_materials | % chance to not consume the block when placing | 1.5 / 1.5 | 7 (≈10%) |
| builder | +% Building XP | 10 / 10 | 0 |
| long_reach | +block placement reach via `BLOCK_INTERACTION_RANGE` attribute | 0.3 / 0.3 | 5 (+1.8) |
| reclaimer | % chance to drop the block back when breaking a block you placed | 3 / 3 | 10 (30%) |
| steady_hands | % fall damage reduction while holding a block | 20 / 20 | 5 (100%) |

Spare Materials + Reclaimer cannot dupe: worst case cycle still nets a material
loss (0.9 consumed × 0.3 recovered).

### Mana ability: Blueprint

Ready by right-clicking with bricks (`BRICK`), activate with a second right-click.
For 15s (+3s/level): +1.5 placement reach and +15% chance to not consume placed
blocks. Cooldown 300s (-5s/level), mana cost 50 (+5/level).

## Skill: Culinary (pt-BR: Culinária)

Stats: Health (every level), Regeneration (every 2 levels).

### Sources (`sources/culinary.yml`)

* `smelt_food` (type `smelting`) — **migrated from smithing.yml**: cooked beef,
  porkchop, chicken, mutton, rabbit, cod, salmon, baked potato, dried kelp at
  **1.0** (amount-multiplied, unchanged semantics).
* `campfire_food` (type `smelting`) — same items at **1.0**, granted when food
  finishes cooking on a campfire the player placed the item on. Implementation:
  Paper cook/block event if available on 26.2; otherwise interact-based
  detection. Feasibility confirmed during implementation planning; worst case
  this source is dropped and campfire gives nothing (documented).
* `craft_*` (type `crafting`) — crafted foods, flat values scaled to ingredient
  count: bread 1.5, cookie 1.0, mushroom_stew 2.0, beetroot_soup 2.5,
  suspicious_stew 3.0, pumpkin_pie 4.0, rabbit_stew 5.0, cake 8.0,
  honey_bottle 2.0, golden_carrot 8.0, golden_apple 12.0.

### Abilities

| Ability | Effect | base/per_level | max_level |
|---|---|---|---|
| extra_servings | % chance of double result when extracting cooked food | 2 / 2 | 10 (20%) |
| chef | +% Culinary XP | 10 / 10 | 0 |
| hearty_meals | +% hunger restored by food you cooked/crafted (tracked by NBT) | 4 / 4 | 5 (20%) |
| gourmet | % chance to not consume one ingredient when crafting food | 2 / 2 | 10 (20%) |
| well_fed | Regeneration I duration (s) after eating cooked/crafted food | 1 / 1 | 5 (5s) |

### Mana ability: Banquet

Ready by right-clicking with a bowl, activate with a second right-click. For 15s
(+3s/level): you and players within 10 blocks gain Saturation and Regeneration I.
Cooldown 300s (-5s/level), mana cost 50 (+5/level).

## Skill: Engineering (pt-BR: Engenharia)

Stats: Wisdom (every level), Luck (every 2 levels).

### Sources (`sources/engineering.yml`)

* `craft_*` (type `crafting`) — values are batch-aware (the source multiplies by
  result amount): rails 0.1, activator_rail 0.3, powered/detector_rail 0.5,
  redstone_torch 0.5, lever 0.5, note_block 1.0, tripwire_hook 1.0,
  weighted pressure plates 1.5, iron door/trapdoor 2.0, repeater 2.0,
  lightning_rod 2.0, minecart 2.5, dropper 2.5, target 2.5, observer 3.0,
  comparator 3.0, dispenser 3.0, redstone_lamp 3.0, piston 3.0,
  chest/furnace_minecart 3.5, hopper_minecart 3.5, tnt 4.0, sticky_piston 4.0,
  hopper 4.0, tnt_minecart 4.0, crafter 4.0, daylight_detector 3.0, jukebox 6.0.
* `place_*` (type `building`) — placing functional components, flat values with
  decay: rails 0.25, basic components (levers, torches, plates) 0.5, pistons,
  observers, dispensers, droppers, repeaters, comparators, hoppers, lamps,
  targets, crafters at 1.0.

### Abilities

| Ability | Effect | base/per_level | max_level |
|---|---|---|---|
| spare_parts | % chance of double drops when breaking mechanisms | 2 / 2 | 10 (20%) |
| engineer | +% Engineering XP | 10 / 10 | 0 |
| efficient_crafting | % chance to not consume one ingredient when crafting components | 2 / 2 | 10 (20%) |
| railroad_baron | +% minecart max speed when riding (`Minecart#setMaxSpeed`) | 5 / 5 | 5 (25%) |
| tinkerers_luck | % chance a component craft yields double result | 1.5 / 1.5 | 8 (12%) |

Out of scope (requires NMS/redstone tick manipulation): faster hoppers/pistons,
longer pulses, signal range changes.

### Mana ability: Power Surge

Ready by right-clicking with redstone dust, activate by clicking a crafting table.
For 15s (+3s/level): component crafts have a 50% chance of double output.
Cooldown 300s (-5s/level), mana cost 50 (+5/level).

## Migration: smelt_food leaves Smithing

* `sources/smithing.yml`: remove `smelt_food`, bump `file_version` to 3.
* Register `SourceFileUpdates.define(Skills.SMITHING, 3, ...)` that removes the
  `smelt_food` key from user files (same pattern as the upstream foraging
  update). Existing servers lose the Smithing food source and gain the Culinary
  file automatically.

## Skill registration & menus

* `skills.yml`: three new sections under the fork banner (abilities lists,
  mana_ability, enabled, max_level 100), bump `file_version`.
* Menu `skills.yml`: extend the fork rows — `fourth_row` gains order 4
  (building) and a new `fifth_row` group holds culinary (1) and engineering (2);
  the startup template merge distributes contexts to existing servers.
  Icons: building = bricks, culinary = cooked_beef, engineering = piston.
* Menu `level_progression.yml`: matching `templates.skill.contexts` entries.
* `abilities.yml`, `mana_abilities.yml`: new sections under the fork banner with
  the values above, bump `file_version`.
* Rewards: `rewards/building.yml`, `culinary.yml`, `engineering.yml` with the two
  stat patterns listed per skill.
* Messages (en + pt-BR): skill names/descriptions, all ability and mana ability
  names/descriptions/raise/lower/start/end, and `sources.*` display names for
  every new source. Bump `file_version` in both files (currently 40).

## Wiki

Update `wiki/index.md` and `wiki/skills/index.md` (14 → 17 skills),
`wiki/sources.md` (new `building` type + campfire note), `wiki/abilities.md`
(new ability-specific options), `wiki/mana-abilities.md` (three new rows).

Also update `Changelog.md`.

## Testing

1. `./gradlew build -x javadoc` green.
2. Deploy to the test server (`C:\Users\Lucas\Desktop\backup_server_mine\minecraft`),
   delete the stale generated `sources/smithing.yml` is NOT needed (file_version 3
   merge handles smelt_food removal — verify in log/file).
3. Startup checks: 17 skills loaded, source count increased, zero AuraSkills
   errors, merge messages for menus/messages/sources.
4. In-game: `/skills` shows the 3 new skills with icons and descriptions; placing
   processed blocks gives Building XP with decay on repeats; dirt gives nothing;
   cooking food gives Culinary (not Smithing) XP; crafting pistons gives
   Engineering XP; each mana ability ready/activate cycle works.

## Fork merge rules (unchanged)

New code only in new files. Edits to upstream files are append-only with the
`=== Fork skills ===` banner.
