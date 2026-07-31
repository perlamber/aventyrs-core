# Aventyrs Core — Project Conventions

Rules-engine core for the Aventyrs tabletop game. Pure Java library (Lombok + JUnit 5 + Gradle),
no framework dependencies — see `org.aventyrs.core.skill.Attention`/`Artes` and their
`Interaction`s for the reference implementation of everything below.

## Adding a new Perícia (Skill)

Every new Skill (e.g. `Artes`, `Attention`) must be created with **all** of the following
pieces — don't stop at just the `Skill` class:

1. **The `Skill` itself** (`org.aventyrs.core.skill`): a class extending `BasicSkill`
   implementing `Skill`, setting its `AttributeDomain` in the constructor. Mirror
   `Attention`/`Artes` — no extra fields needed on the class itself.

2. **A `SkillType` constant** (`org.aventyrs.core.skill.SkillType`): one enum value per
   concrete `Skill`, used to key `Character.skills` (a `Map<SkillType, CharacterSkill>`) for
   O(1) lookup instead of filtering a list.

3. **A `<Skill>Specialization` enum, if the Perícia's rules text calls for one** (e.g.
   `ArtesSpecialization`): one constant per named specialization, each with a `description`.
   Don't retype `CharacterSkill.specialization` (a plain `String`) to this enum — different
   skills have different specialization sets (some, like Attention, have none at all), so it
   stays a generic field; the enum is just the well-typed reference for that one skill.

4. **A `<Skill>CompetencyAbility` enum for its Habilidades de Competência, if any**,
   implementing `SkillCompetencyAbility` (`getSkillType()` + `getDescription()`) — the
   Perícia-level counterpart to `AttributeAbility`/`EgoAdvantage`. Store instances a
   character has acquired in `Character.skillCompetencyAbilities`.

   **For every ability whose mechanic depends on a system that doesn't exist yet**
   (a roll-vs-`DifficultyLevel` resolution engine, Vantagem/Desvantagem, ally-range effects,
   NPC-disposition/reputation, Cena/Combate-scoped state, etc.), add a `// TODO:` comment
   directly above the enum constant explaining:
   - *what* the ability is supposed to do mechanically (not just repeating the flavor text), and
   - *which specific system is missing* that would be needed to implement it for real.

   `ArtesCompetencyAbility` is the reference example for this — e.g.:
   ```java
   // TODO: activated ability (Artes roll vs GD Médio) granting nearby allies (Distância
   // Média), but not the user, +1 on Perícia rolls for 1 Round, scaling +1 per 5 points
   // over GD Médio — no roll-resolution-vs-DifficultyLevel engine or ally-range system
   // exists yet.
   DOM_BARDICO("..."),
   ```
   Don't invent the missing system just to make the TODO go away — this codebase's
   established pattern (see `GnoseAbility`, `InstinctAbility`, `CharismaAbility`) is to model
   the ability as real, tested data now and defer the mechanic honestly until its
   prerequisite system is actually built.

5. **Always implement a concrete `Interaction<CharacterSheet>` for the skill** (e.g.
   `AttentionInteraction`, `ArtesInteraction`) — never leave a Skill without one. It must:
   - take an optional injected `CharacterSkillService` (default `new CharacterSkillServiceImpl()`),
     matching this codebase's constructor-injection convention;
   - look up the target's own `CharacterSkill` via `character.getSkills().get(SkillType.X)`,
     falling back to a fresh untrained `CharacterSkill` whose `SkillGraduation` carries
     `Skill.UNTRAINED_PENALTY` when the character never trained it;
   - delegate the actual bonus computation to `CharacterSkillService.getValueForRoll` — never
     recompute attribute totals or graduation math inline;
   - if the skill has a `<Skill>Excellency` enum (see below), also compute
     `SkillExcellency.totalDifficultyReduction(<Skill>Excellency.class, graduationValue)` and
     set it on `InteractionResult.difficultyReduction`;
   - return an `InteractionResult` with `resultStatus` (the target's current status),
     `skillRollBonus` (the computed bonus), and `difficultyReduction` set.

6. **A `<Skill>Excellency` enum for its automatic Excelência bonuses** (e.g.
   `ArtesExcellency`), implementing `SkillExcellency` (`getSkillType()` + `getTier()` +
   `getDescription()`). **Every** skill uses the same three universal
   `ExcellencyTier`s — `FOCADO` (graduation 3), `PRODIGIO` (7), `LENDA` (10) — only the
   bonus content per tier differs per skill. Bonuses at different tiers are additive, never
   overriding — if a later tier's rules text reads like "changes to +N", model it as the
   *delta* over the earlier tier's value, not the new total (see `ArtesExcellency.LENDA`,
   which is worth +3 on top of `FOCADO`'s +2, totaling +5, rather than being worth +5 itself).

   Same TODO discipline as Habilidades de Competência applies: if a tier's bonus needs a
   system that doesn't exist yet (e.g. Fama Positiva/Negativa now exist on `CharacterSheet`,
   but nothing detects a graduation crossing a threshold to auto-trigger the bonus), TODO it
   with what's missing. If the bonus is mechanically expressible today even without a full
   consumer (e.g. reducing a `DifficultyLevel` — `DifficultyLevel.easier()` already exists),
   implement it for real by overriding `SkillExcellency.getDifficultyReduction()` on that
   constant (an int step count — `adjustDifficulty` derives from it automatically), don't TODO
   it just because nothing calls it yet.

7. **Test fixture support**: add a template for the new skill to `CharacterSkillFixture`
   (e.g. `ARTES_1`) if any test needs a trained instance of it.

8. **Tests**, one file per new type, following the existing enum-test shape (every
   ability/specialization/excellency has a non-blank description; the enum has the expected
   count; entries report the correct `SkillType`/`ExcellencyTier`) plus an
   `<Skill>InteractionTest` covering: trained bonus = attribute total + graduation, untrained
   = attribute total + `UNTRAINED_PENALTY`, and `CharacterSheet.receiveInteraction(interaction)`
   delegates correctly.

## Character-level stats aggregated from abilities (e.g. Reações)

Some Character-level counters need a fixed base value *and* a fully-modified total summed
from abilities — same shape as `actionPoints`/`ActionPointsService`. Don't compute the
modified total inside `Character` itself (it would need to instantiate `ModifierResolverImpl`
directly, which doesn't belong on a data class):

- `Character` holds only the plain fixed counter (e.g. `reactions`, a normal
  `@Builder.Default` field with Lombok's regular getter — no suppression, no manual method),
  defaulting to a constant declared on the **service** interface
  (`ReactionsService.DEFAULT_REACTIONS`, mirroring `ActionPointsService.DEFAULT_ACTION_POINTS`).
- A dedicated `<Stat>Service`/`<Stat>ServiceImpl` in `org.aventyrs.core.character.services`
  (e.g. `ReactionsService.getTotalReactions(Character)`) takes a constructor-injected
  `ModifierResolver` (default `new ModifierResolverImpl()`, same DI convention as every other
  service) and sums `@Modifier`/`ModifierType` bonuses across **three** sources:
  `character.getAttributeAbilities()`, `character.getSkillCompetencyAbilities()`, and — per
  trained Perícia in `character.getSkills()` — whichever `SkillExcellency` tiers that Perícia's
  graduation has unlocked (resolved generically via `SkillType.getExcellencyClass()` +
  `SkillExcellency.unlockedBy`, since the concrete `<Skill>Excellency` enum type isn't known at
  compile time from a bare `SkillType`). Clamp the total at 0.

Mirror this shape for any new stat abilities/competencies/excellencies can modify, and
remember to give the new `ModifierType` constant a `@Modifier`-annotated method on whichever
concrete ability/excellency should affect it (e.g. `AttentionExcellency.FOCADO` for
`REACTIONS`).

## Vantagem is a flat +2 bonus, not a reroll mechanic

"Grants Vantagem on X rolls" is one of the most common TODO reasons across every ability
enum, but it isn't a d20-style "roll twice, take the higher" mechanic — in this game
**Vantagem is just a flat +2 bonus to that specific roll** (`Skill.ADVANTAGE_BONUS`). So an
ability that grants Vantagem on a Perícia roll is implemented exactly like any other roll
bonus: a `@Modifier(ModifierType.SKILL_ROLL_BONUS)` method on the concrete ability/excellency
returning `Skill.ADVANTAGE_BONUS`, summed into `skillRollBonus` inside the skill's
`<Skill>Interaction.applyTo` — see `AttentionCompetencyAbility.PERCEPCAO_DE_FOXM` /
`AttentionInteraction`. No separate flag or dice-rolling engine needed.

Every `<Skill>Interaction.applyTo` should sum `ModifierType.SKILL_ROLL_BONUS` across the same
three sources `ReactionsService` uses for Reações — `attributeAbilities`,
`skillCompetencyAbilities`, and the trained skill's own unlocked `SkillExcellency` tiers —
even before any ability actually grants it for that specific skill, so future abilities work
without touching the Interaction again.

If the ability's Vantagem is scoped to a specific *purpose* within the skill (e.g. Fox'm's is
only for perceiving movement, not every Atenção roll), that narrowing can't be modeled yet —
this codebase doesn't track what a roll is *for*. Document that simplification in a comment on
the enum constant rather than silently narrowing or silently over-granting.
