---
name: skill-roll-mechanics
description: This skill should be used for any work on how a Perícia roll is dispatched, resolved, or bonus-summed inside org.aventyrs.core.skill — `AbstractSkillInteraction#applyTo`'s cascading overloads, `SkillRoll` (the 3d6 faces + `getCriticalResult`), `DifficultyLevel#reachedBy`/`reachedByAsExpert`, `SkillTrait`/`SkillRoll#getRequestedAbility` validation (`REQUIRED_SKILL_TRAIT_NOT_HELD`), `SkillInteractionFactory`/`SkillRollRequest`/`SkillType#newInteraction`, the per-skill `ModifierType` (`rollBonusType`, `<SKILL>_ROLL_BONUS`) and cross-skill bonus leakage, or how "grants Vantagem" is modeled (a flat +2, `Skill.ADVANTAGE_BONUS`). Also use it when asked why a bonus meant for one Perícia leaks into others, how an API layer turns a `SkillType` into a roll, or what `expertValue` is for. For adding a whole new Perícia use `adding-a-pericia`; this skill is the roll-resolution machinery those Perícias share.
---

# Skill-roll mechanics

The shared, cross-skill machinery in `org.aventyrs.core.skill` — everything a `<Skill>Interaction`
builds on. `org.aventyrs.core.skill.attention.Attention` / `org.aventyrs.core.skill.artes.Artes`
and their `Interaction`s are the reference implementations. CLAUDE.md's "Recurring conventions"
(especially **cascading overloads** and **a no-arg `@Modifier` can't see context**) all apply.

## Vantagem is a flat +2 bonus, not a reroll mechanic

"Grants Vantagem on X rolls" is one of the most common TODO reasons across every ability
enum, but it isn't a d20-style "roll twice, take the higher" mechanic — in this game
**Vantagem is just a flat +2 bonus to that specific roll** (`Skill.ADVANTAGE_BONUS`). So an
ability that grants Vantagem on a Perícia roll is implemented exactly like any other roll
bonus: a `@Modifier(ModifierType.SKILL_ROLL_BONUS)` method on the concrete ability/excellency
returning `Skill.ADVANTAGE_BONUS`, summed into `skillRollBonus` inside the skill's
`<Skill>Interaction.applyTo` — see `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS` /
`DirigirECavalgarInteraction`. No separate flag or dice-rolling engine needed.

`AbstractSkillInteraction.applyTo` (every `<Skill>Interaction` extends it, so this is not
duplicated per skill) sums `ModifierType.SKILL_ROLL_BONUS` across the same three sources
`ReactionsService` uses for Reações — `attributeAbilities`, `skillCompetencyAbilities`, and the
trained skill's own unlocked `SkillExcellency` tiers — plus a fourth, `CharacterSheet`-level
one: `target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS)` (see the `granting-a-blessing`
skill) — even before any ability actually grants it for that specific skill, so future
abilities work without touching any Interaction at all.

If the ability's Vantagem is scoped to a specific *purpose* within the skill (e.g.
`CONTROLAR_ANIMAIS`'s is only for animal/animal-drawn-vehicle rolls, not every Dirigir e
Cavalgar roll), that narrowing can't be modeled yet — this codebase doesn't track what a roll
is *for*. Document that simplification in a comment on the enum constant rather than silently
narrowing or silently over-granting.

Don't treat a citation of one of these examples as permanent, either — `AttentionCompetencyAbility
.PERCEPCAO_DE_FOXM` used to be this section's reference example (a scoped Vantagem bonus), but
a rules revision turned it into an auto-success effect instead, which no longer fits this
pattern at all. When revising a skill, check whether anything elsewhere cites it as a
precedent and fix those citations in the same change (see the `ATAQUE_A_DISTANCIA` and
`ATENÇÃO` revisions for two examples of this).

## A ModifierType per skill — fixing cross-skill bonus leakage

`ModifierType.SKILL_ROLL_BONUS` applies broadly, to *every* Perícia's roll — that's correct
for abilities like `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS`, but it was, until this
was fixed, the *only* option `AbstractSkillInteraction` summed — meaning a bonus meant for one
specific Perícia (e.g. a temporary buff an ally received "for Atletismo") had no way to avoid
leaking into every other Perícia's roll too, since `sumSkillRollBonusModifiers`/
`getTemporaryBonus` had no per-skill filter to apply.

- Every `SkillType` now also carries its own `ModifierType` — `rollBonusType` — e.g.
  `ATLETISMO`'s is `ModifierType.ATLETISMO_ROLL_BONUS`. This can't be a runtime-computed
  mapping, since `@Modifier(ModifierType.X)` is a compile-time-fixed annotation value;
  `ModifierResolver` has no way to scan "give me whatever modifiers apply to skillType Y" — it
  can only scan for one fixed `ModifierType` at a time. One real enum constant per skill is
  the only way an ability's `@Modifier` method (or a granted `TemporaryBonus`) can target one
  specific Perícia through this reflection-based mechanism.
- `AbstractSkillInteraction.sumSkillRollBonusModifiers` sums **both** `SKILL_ROLL_BONUS` and
  `skillType.getRollBonusType()` for every source (`attributeAbilities`,
  `skillCompetencyAbilities`, unlocked `SkillExcellency` tiers), and `applyTo` does the same
  for `target.getTemporaryBonus(...)` — additively, not either/or, so an ability or a granted
  `TemporaryBonus` can choose either the broad generic type or one specific skill's, and both
  still combine correctly on a roll that has some of each (see `ArtesInteractionTest
  #applyToCombinesTheGenericAndArtesSpecificTemporaryBonusesAdditively`).
- **Whenever a new `SkillType` constant is added, add a matching `<SKILL>_ROLL_BONUS`
  constant to `ModifierType` too** (see the `adding-a-pericia` skill) — there's no default;
  omitting it would make `SkillType.rollBonusType` uninitializable.

## Requesting a specific Habilidade de Competência or Especialização on a roll — `SkillRoll#getRequestedAbility`

Some rolls aren't a plain Perícia test — the caller is specifically invoking one of the
character's `SkillCompetencyAbility` maneuvers (e.g. "roll Furtividade using Disparo
Ricochete") or a held Especialização (e.g. "roll Atenção as an Investigar check"), and this
core should refuse the roll outright if the character never actually acquired that trait,
rather than silently computing a result for one they can't use.

- `SkillTrait` (`org.aventyrs.core.skill`) is the shared interface behind both:
  `SkillType getSkillType(); String getDescription();`. `SkillCompetencyAbility extends
  SkillTrait` (plus its own ability-specific default methods —
  `getDifficultyReduction()`/`getSubstituteAttributeDomain()`/etc., unaffected by this).
  `SkillSpecialization extends SkillTrait` too, with nothing added on top — every
  `<Skill>Specialization` enum constant implements it. Neither interface is sealed; only these
  two implementations exist today, but nothing requires that going forward.
- `SkillRoll` carries an optional `requestedAbility` (a `SkillTrait` — either a
  `SkillCompetencyAbility` or a `SkillSpecialization`), set via its second constructor overload
  — `SkillRoll(dice)` still delegates to `SkillRoll(dice, null)`, so every existing call site (a
  plain roll) is unaffected. `null` means "just a plain roll," and skips validation entirely —
  this is the common case.
- `AbstractSkillInteraction`'s 3-arg `applyTo` validates a non-null `requestedAbility` via
  `validateRequestedTrait` before doing anything else: it must belong to this same `skillType`
  (`requestedTrait.getSkillType() == skillType`) *and* actually be held — for a
  `SkillCompetencyAbility` that means present in `character.getSkillCompetencyAbilities()` or
  `character.getRace().getRacialAbilities()`; for a `SkillSpecialization` that means present in
  `characterSkill.getSpecializations()` instead (`characterSkill` is resolved via
  `findCharacterSkill` before this check runs, so an untrained skill's fallback `CharacterSkill`
  — with no specializations — correctly fails this check). Either failure throws
  `IllegalOperationException` (`REQUIRED_SKILL_TRAIT_NOT_HELD`). This applies generically to
  every skill, in one place, the same way every other `applyTo` computation already does.
- When the validated `requestedAbility` is a `SkillSpecialization`, `applyTo` resolves
  `InteractionResult#reachedDifficultyLevel` via `DifficultyLevel#reachedByAsExpert` instead of
  `DifficultyLevel#reachedBy` — thresholding against each tier's easier `expertValue` instead of
  its `baseValue`. This is the first real consumer of `expertValue`, which used to be dead data.
- This only validates *possession* of the trait, the same restraint the codebase applies
  elsewhere (no "Requer N Graduações" enforcement, no acquisition-legality checks) — it doesn't
  validate that a `SkillCompetencyAbility`'s own mechanic is actually implemented, or gate on
  anything about the roll's circumstances (range, targets, etc.), and it doesn't validate that a
  requested `SkillSpecialization` is actually the right fit for whatever the roll is being used
  for narratively — this core still doesn't track what a roll is *for*. A caller can still
  request a trait whose effect is entirely TODO'd (an ability) or narratively mismatched (a
  specialization); the roll proceeds, it's just no longer possible to request a trait the
  character was never granted in the first place.

## Dispatching a roll by SkillType — `SkillInteractionFactory`, `SkillRollRequest`

A caller that only has a `SkillType` in hand (e.g. an API layer deserializing an incoming roll
request) previously had no way to reach the right concrete `<Skill>Interaction` without its
own hand-written switch over all of them. `SkillType` already carries every other per-skill
mapping this core needs (see `adding-a-pericia`), so this reuses it rather than introducing a
second, parallel "which skill is this" enum that could drift out of sync with `SkillType`:

- `SkillType.newInteraction()` returns a fresh `<Skill>Interaction` via a stored
  `Supplier<AbstractSkillInteraction>` per constant, built through that Interaction's default
  (no-arg) constructor — the same one every concrete subclass already exposes.
- `SkillInteractionFactory` (`org.aventyrs.core.skill`) is the single call site for this:
  `create(SkillType)` just delegates to `newInteraction()`, and `resolve(SkillRollRequest)`
  looks up the right Interaction *and* calls its 3-arg `applyTo` in one step.
- `SkillRollRequest` (`@Builder`) is the wrapper bundling everything one roll needs:
  `skillType`/`target` (`@NonNull`, required) plus `sceneContext`/`skillRoll` (both optional,
  `null` same as every `applyTo` overload already accepts) plus `attackSource` (optional — see
  the `ability-acquisition-and-substitution` skill). A caller builds one of these once instead
  of separately resolving which `<Skill>Interaction` class to instantiate.
- This is purely a dispatch/ergonomics convenience — it doesn't change what any `applyTo`
  computes, and a caller that already knows which concrete `<Skill>Interaction` to use (most
  of this core's own tests) can keep constructing and calling it directly.

## Rolling the dice — `SkillRoll`, `DifficultyLevel#reachedBy`

This core deliberately never rolls dice itself (see the `skill` package-info's "What this
library computes" section) — but plenty of abilities' effects depend on which GD tier a roll
*reached*, or whether it was a critical, so an API layer needs a way to hand an
already-physically-rolled result in. Every Perícia roll in this ruleset is 3d6 — always exactly
3 six-sided dice.

- `SkillRoll` (`org.aventyrs.core.skill`) wraps the 3 individual die faces as a
  `List<Integer>`, not just their sum — a compact "111-666, sum the digits" packed-int
  encoding was considered and rejected: it buys "one int instead of a 3-element list" at the
  cost of no compile-time safety (an invalid value like `289` or `700` only fails at
  runtime, deep in parsing) and an unusual decode step every time someone reads a value like
  `435`. The individual faces matter, not just the total, because `getCriticalResult()`
  depends on *matching* dice at the extremes: three 1s is `FALHA_CRITICA_MAIOR`, two 1s is
  `FALHA_CRITICA_MENOR`, three/two 6s are the `ACERTO_CRITICO_MAIOR`/`_MENOR` equivalents (see
  `CriticalResult`) — the sum alone can't distinguish "rolled 1-1-4" from "rolled 2-2-2", but
  the first is a critical failure and the second isn't. The two/six-case "Acerto Crítico"
  pairing is inferred by symmetry with the confirmed "Falha Crítica" one, not itself
  independently confirmed against rules text — flagged in `CriticalResult`'s own javadoc.
  `SkillRoll`'s constructor validates exactly 3 dice, each 1-6 — a genuine system boundary
  (input from outside this core), unlike internal invariants this codebase otherwise trusts.
- `DifficultyLevel.reachedBy(int total)` resolves the highest tier a total clears, judged
  against each tier's `baseValue` — mirroring `SkillExcellency.unlockedBy`'s
  threshold-lookup shape, returning `Optional.empty()` if total falls short of even
  `VERY_EASY`. Its sibling `DifficultyLevel.reachedByAsExpert(int total)` does the identical
  lookup against `expertValue` instead — a held, matching `SkillSpecialization` named as the
  roll's `requestedAbility` switches to `reachedByAsExpert` (see the Requesting-a-trait section
  above). Neither method validates *why* a caller picked one over the other.
- `AbstractSkillInteraction`'s 3-arg `applyTo(CharacterSheet, SceneContext, SkillRoll)`
  overload: when `skillRoll` is non-`null`, it sets `InteractionResult.reachedDifficultyLevel`
  (from `reachedBy`/`reachedByAsExpert(skillRollBonus + skillRoll.getTotal())`) and
  `.criticalResult` (from `skillRoll.getCriticalResult()`); both stay `null` otherwise, same as
  every other not-applicable `InteractionResult` field.
- The engine to know *which* tier a roll reached now exists; each ability's own consumption
  of that is still separate, per-ability wiring — `ArtesCompetencyAbility.DOM_BARDICO` is done
  (its 5-tier bonus table maps onto 5 consecutive `DifficultyLevel` constants, with only
  `UNIMAGINABLE` handled by inference — see `ArtesInteraction`'s own javadoc), but e.g.
  `MedicinaECuraExcellency.FOCADO`'s auto-success is still unbuilt. Deliberately don't guess at
  a mapping where the rules text leaves genuine ambiguity.

### Margem Crítica — `resolveCriticalMarginIncrease`

`SkillRoll#getCriticalResult(int)` takes a widening margin, and
`AbstractSkillInteraction#sumCriticalMarginIncrease` feeds it the sum of
`resolveCriticalMarginIncrease(SkillType, SceneContext)` across `EgoAdvantage`/
`AttributeAbility`/`SkillCompetencyAbility` — those exact three, identical signature.
`SorteAdvantage.ACE` is the reference override. Its still-unconsumed differently-shaped
neighbour is `ArtesAprimorarComArteAbility#getCriticalMarginReduction`.

## Reference files to read first

- `src/main/java/org/aventyrs/core/skill/AbstractSkillInteraction.java`
  (`AbstractSkillInteractionTest.java`) — the cascading `applyTo` overloads, every bonus scan.
- `src/main/java/org/aventyrs/core/skill/SkillRoll.java` / `DifficultyLevel.java` /
  `CriticalResult.java`.
- `src/main/java/org/aventyrs/core/skill/SkillTrait.java` / `SkillType.java`
  (`rollBonusType`, `newInteraction`, `getExcellencyClass`).
- `src/main/java/org/aventyrs/core/skill/SkillInteractionFactory.java` / `SkillRollRequest.java`.
- `src/main/java/org/aventyrs/core/skill/artes/ArtesInteraction.java`
  (`ArtesInteractionTest.java`) — a `<Skill>Interaction` that overrides the longest `applyTo`.
- `src/main/java/org/aventyrs/core/modifier/ModifierType.java` — the per-skill `_ROLL_BONUS`
  constants.
