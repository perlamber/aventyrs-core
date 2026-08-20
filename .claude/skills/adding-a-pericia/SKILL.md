---
name: adding-a-pericia
description: This skill should be used when the user asks to "add a new Perícia", "add a new Skill" (in the org.aventyrs.core.skill sense, not a Claude Code Skill), "add [SkillName] as a skill", or references adding a new entry to org.aventyrs.core.skill. Walks through the full checklist of files/tests a new Perícia needs — the Skill class, SkillType constant, Specialization enum, CompetencyAbility enum, Interaction, Excellency enum, fixture support, and tests — mirroring `org.aventyrs.core.skill.attention.Attention`/`org.aventyrs.core.skill.artes.Artes` as the reference implementation.
---

# Adding a new Perícia (Skill)

A Perícia (e.g. `Artes`, `Attention`) is this codebase's most-repeated top-level character
concept — see `org.aventyrs.core.skill.attention.Attention`/`org.aventyrs.core.skill.artes
.Artes` and their `Interaction`s as the reference implementation of everything below. Every
Perícia's classes live together under their own subpackage of `org.aventyrs.core.skill` (e.g.
`org.aventyrs.core.skill.artes` holds every `Artes*` class) — only the shared, cross-skill
machinery (`AbstractSkillInteraction`, `Skill`, `SkillType`, `SkillCompetencyAbility`,
`SkillExcellency`, `SkillRoll`, `DifficultyLevel`, etc.) stays directly in
`org.aventyrs.core.skill` itself.

Every new Skill must be created with **all** of the following pieces — don't stop at just the
`Skill` class:

## 1. The `Skill` itself

In its own `org.aventyrs.core.skill.<skillname>` subpackage — e.g.
`org.aventyrs.core.skill.artes` for `Artes`, `org.aventyrs.core.skill.atletismo` for
`Atletismo` — lowercase, no separators: a class extending `BasicSkill` implementing `Skill`,
setting its `AttributeDomain` in the constructor. Mirror `Attention`/`Artes` — no extra fields
needed on the class itself. Every other piece below for this same Perícia (its `SkillType`
constant aside, which stays in the shared base package — see step 2) goes in this same
subpackage too, alongside its tests.

## 2. A `SkillType` constant

`org.aventyrs.core.skill.SkillType`: one enum value per concrete `Skill`, used to key
`Character.skills` (a `Map<SkillType, CharacterSkill>`) for O(1) lookup instead of filtering a
list.

## 3. A `<Skill>Specialization` enum

E.g. `ArtesSpecialization`: every Perícia gets one, no exceptions — one constant per named
specialization, each with a `description`, implementing `SkillSpecialization`
(`getSkillType()` + the inherited `getDescription()`, from `SkillTrait`/`SkillSpecialization`).
`CharacterSkill.specializations` is a `List<SkillSpecialization>` (`@Builder.Default` empty),
not a per-skill-typed field — the enum is the well-typed *catalog* for that one skill, and any
instance a character actually holds just needs to satisfy the shared `SkillSpecialization`
interface, the same way `Character.skillCompetencyAbilities` holds any `SkillCompetencyAbility`
regardless of which skill it targets. `CharacterSkill` itself never validates that a listed
specialization's `getSkillType()` actually matches its own `skill` — same restraint as every
other builder-bypassable invariant in this codebase (e.g. Attribute `base`/Perícia Graduação
caps not being enforced by the bare builder either).

## 4. A `<Skill>CompetencyAbility` enum for its Habilidades de Competência, if any

Implementing `SkillCompetencyAbility` (`getSkillType()` + `getDescription()`, plus the default
`getDifficultyReduction()` — override it only on constants that really grant a GD reduction to
that *same* skill's own roll) — the Perícia-level counterpart to `AttributeAbility`/
`EgoAdvantage`. Store instances a character has acquired in
`Character.skillCompetencyAbilities`. (`AtletismoCompetencyAbility.ATLETA_VERSATIL` used to be
the reference example for this override — a rules revision replaced it with `Passo Largo`,
which needs a movement system instead, so no ability currently exercises this override; the
mechanism itself is still real and tested generically — see the `noAbilityReducesDifficulty`-
style test in most `<Skill>CompetencyAbilityTest` files — it's just waiting for the next
ability that actually qualifies.)

If an ability's acquisition is rules-gated on something like "Requer N Graduações", don't
build a validation/eligibility service for it — none exists yet for `SkillCompetencyAbility`
(unlike `AttributeAbilityService` for `AttributeAbility`). Just document the unenforced
prerequisite in a comment on the constant, implementing its actual numeric effect for real
whenever one is expressible today. No ability currently has an explicit "Requer N
Graduações"-style clause in its own rules text to point to as a live example —
`AtletismoCompetencyAbility.ATLETA_VERSATIL`, `EsquivaEApararCompetencyAbility.RECUO_RAPIDO`,
and `MedicinaECuraCompetencyAbility.ALQUIMIA_MAIOR` each held that role in turn, and each lost
the clause (or was removed outright) in its own later revision. Don't take this as license to
skip documenting the next one that shows up — just don't expect to find a working example in
the codebase to copy from right now.

**If a constant's rules text requires the player to pick a value when acquiring it** ("Escolha
uma Perícia…", or a pick-one-of-several-effects choice), don't TODO the whole ability as "no
way to persist the choice" — there is one; see "Acquisition-time ability choices" below for the
two patterns (an instance-based companion class when the choice feeds the ability's own
`@Modifier` methods, `AcquiredChoice` otherwise) and which one applies.

**For every ability whose mechanic depends on a system that doesn't exist yet** (a
roll-vs-`DifficultyLevel` resolution engine, Vantagem/Desvantagem, ally-range effects,
NPC-disposition/reputation, Cena/Combate-scoped state, etc.), add a `// TODO:` comment directly
above the enum constant explaining:
- *what* the ability is supposed to do mechanically (not just repeating the flavor text), and
- *which specific system is missing* that would be needed to implement it for real.

`ArtesCompetencyAbility` is the reference example for this — e.g.:
```java
// TODO: motivates allies, granting them (not the user) a Perícia-roll bonus for 1
// Rodada, extending to 2/3 Rodadas at 5/10 Graduações — but the bonus itself is a
// lookup by which GD tier the Artes roll hit (Fácil +1 ... Milagre +5), not a flat
// value. Needs a roll-resolution-vs-DifficultyLevel engine (to know which GD tier was
// reached, then look up its bonus), ally-targeting, and Rodada-scoped duration
// tracking, none of which exist yet.
DOM_BARDICO("..."),
```
Don't invent the missing system just to make the TODO go away — this codebase's established
pattern (see `GnoseAbility`, `InstinctAbility`, `CharismaAbility`) is to model the ability as
real, tested data now and defer the mechanic honestly until its prerequisite system is
actually built.

## 5. Always implement a concrete `Interaction<CharacterSheet>` for the skill

E.g. `AttentionInteraction`, `ArtesInteraction` — never leave a Skill without one. Every
`<Skill>Interaction` needs the exact same `applyTo`/`findCharacterSkill` machinery — compute
the roll bonus via `CharacterSkillService.getValueForRoll`, resolve which Attribute currently
governs the roll, sum every `SKILL_ROLL_BONUS` source, look up the trained `CharacterSkill` or
fall back to an untrained one, sum `difficultyReduction` — so this is **not** something to
hand-write per skill anymore: extend `AbstractSkillInteraction` (`org.aventyrs.core.skill`)
and give it the new skill's `SkillType` constant. A concrete subclass needs nothing but two
constructors delegating to `super(SkillType.X)`/`super(SkillType.X, characterSkillService,
modifierResolver)` — see `ArtesInteraction` or `AttentionInteraction` for the ~15-line shape
every skill without something genuinely unusual to say should match. Move any skill-specific
rules-text nuance (an unenforced TODO, a note about a related mechanic) into the subclass's
own class-level javadoc, same as before — it's still a real class per skill, just without the
duplicated body.

This only works because `SkillType` itself now carries everything `AbstractSkillInteraction`
needs per skill: `excellencyClass` (already existed), a `Supplier<Skill> skillFactory`
(`newSkillInstance()`) for the untrained-fallback `CharacterSkill`, a `ModifierType
rollBonusType` (see "A ModifierType per skill" below), and a
`Supplier<AbstractSkillInteraction> interactionFactory` (`newInteraction()` — see "Dispatching
a roll by SkillType" below) — e.g. `ARTES(ArtesExcellency.class, Artes::new,
ModifierType.ARTES_ROLL_BONUS, ArtesInteraction::new)`. **Whenever you add a new Skill, add its
constant to `SkillType` with all four pieces, and a matching `<SKILL>_ROLL_BONUS` constant to
`ModifierType`** — `AbstractSkillInteraction` has no other way to know which
`Skill`/`SkillExcellency`/`ModifierType`/`<Skill>Interaction` a given `SkillType` maps to. Also
add a matching constant to `org.aventyrs.core.ability.PeritoTeoricoAbility` (see
"Acquisition-time ability choices" below) — it holds one `AttributeAbility` constant per
`SkillType` so `GnoseAbility.PERITO_TEORICO` can be chosen for the new skill too; there's no
default, so skipping it silently leaves the new skill impossible to pick.

If a skill's Interaction genuinely needs to do something no other skill does, override
`applyTo` in the subclass and call `super.applyTo(target)` first, then layer the addition on
top of the returned result — see `ArtesInteraction`, which reports a `Blessing` on `blessings`
for a character holding `DOM_BARDICO` (see the `granting-a-blessing` skill) — rather than
duplicating `AbstractSkillInteraction`'s logic or forking it back out to a hand-written
`applyTo`.

## 6. A `<Skill>Excellency` enum for its automatic Excelência bonuses

E.g. `ArtesExcellency`, implementing `SkillExcellency` (`getSkillType()` + `getTier()` +
`getDescription()`). **Every** skill uses the same three universal `ExcellencyTier`s —
`FOCADO` (graduation 3), `PRODIGIO` (7), `LENDA` (10) — only the bonus content per tier differs
per skill. Bonuses at different tiers are additive, never overriding — if a later tier's rules
text reads like "changes to +N" or "muda para +N" (regardless of the exact wording — this
phrasing recurs across skills and always means the same thing), model it as the *delta* over
the earlier tier's value, not the new total (see `ArtesExcellency.LENDA`, which is worth +3 on
top of `FOCADO`'s +2, totaling +5, rather than being worth +5 itself — confirmed by Artes' own
rules text changing from an explicit "+3 adicional, totalizando +5" to "muda para +5" with the
*same* numbers, across a revision).

Same TODO discipline as Habilidades de Competência applies: if a tier's bonus needs a system
that doesn't exist yet (e.g. Fama Positiva/Negativa now exist on `CharacterSheet`, but nothing
detects a graduation crossing a threshold to auto-trigger the bonus), TODO it with what's
missing. If the bonus is mechanically expressible today even without a full consumer (e.g.
reducing a `DifficultyLevel` — `DifficultyLevel.easier()` already exists), implement it for
real by overriding `SkillExcellency.getDifficultyReduction()` on that constant (an int step
count — `adjustDifficulty` derives from it automatically), don't TODO it just because nothing
calls it yet.

## 7. Test fixture support

Add a template for the new skill to `CharacterSkillFixture` (e.g. `ARTES_1`) if any test needs
a trained instance of it.

## 8. Tests

One file per new type, following the existing enum-test shape (every
ability/specialization/excellency has a non-blank description; the enum has the expected
count; entries report the correct `SkillType`/`ExcellencyTier`) plus an
`<Skill>InteractionTest` covering: trained bonus = attribute total + graduation, untrained =
attribute total + `UNTRAINED_PENALTY`, and `CharacterSheet.receiveInteraction(interaction)`
delegates correctly.

An instance-based choice-carrying ability class (see "Acquisition-time ability choices") gets
its own test file too, shaped like `ArtesAprimorarComArteAbilityTest`: exercise each
implemented branch through the *real* scanning service that consumes it (e.g.
`DamageService.getTotalDamageReduction` on a `CharacterFixture` character granted the
instance), not by calling the modifier method directly; a non-matching choice contributes 0; a
null choice is rejected at construction; and the instance reports the catalog constant's
`SkillType` and description.

## Reference files to read first

- `src/main/java/org/aventyrs/core/skill/artes/Artes.java`,
  `ArtesSpecialization.java`, `ArtesCompetencyAbility.java`, `ArtesExcellency.java`,
  `ArtesInteraction.java` — the fullest worked example (includes a `<Skill>CompetencyAbility`
  override, TODO discipline, and a `blessings`-reporting `applyTo` override).
- `src/main/java/org/aventyrs/core/skill/attention/Attention.java` and its sibling files — the
  simpler, no-override reference shape most new skills should match.
- `src/main/java/org/aventyrs/core/skill/AbstractSkillInteraction.java` — the shared
  `applyTo`/`findCharacterSkill` machinery every `<Skill>Interaction` extends.
- `src/main/java/org/aventyrs/core/skill/SkillType.java` — the four per-skill pieces
  (`excellencyClass`, `skillFactory`, `rollBonusType`, `interactionFactory`) every new
  constant must set.
- `src/main/java/org/aventyrs/core/skill/ModifierType.java` — where the matching
  `<SKILL>_ROLL_BONUS` constant goes.
- `src/main/java/org/aventyrs/core/ability/PeritoTeoricoAbility.java` — the per-`SkillType`
  constant that must also be added for `GnoseAbility.PERITO_TEORICO` to reach the new skill.
- `src/main/java/org/aventyrs/core/skill/artes/ArtesAprimorarComArteAbility.java` — the
  instance-based acquisition-choice pattern referenced in steps 4 and 8.
- `src/test/java/org/aventyrs/core/character/fixture/CharacterSkillFixture.java` — where a new
  trained-instance template (step 7) is added.
