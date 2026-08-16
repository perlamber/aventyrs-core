# Aventyrs Core — Project Conventions

Rules-engine core for the Aventyrs tabletop game. Pure Java library (Lombok + JUnit 5 + Gradle),
no framework dependencies — see `org.aventyrs.core.skill.attention.Attention`/
`org.aventyrs.core.skill.artes.Artes` and their `Interaction`s for the reference
implementation of everything below. Every Perícia's classes live together under their own
subpackage of `org.aventyrs.core.skill` (e.g. `org.aventyrs.core.skill.artes` holds every
`Artes*` class) — only the shared, cross-skill machinery (`AbstractSkillInteraction`,
`Skill`, `SkillType`, `SkillCompetencyAbility`, `SkillExcellency`, `SkillRoll`,
`DifficultyLevel`, etc.) stays directly in `org.aventyrs.core.skill` itself.

## Attribute `base`/Perícia Graduação: hard caps, and both upgrades cost XP

An Attribute's `base` is capped at 5; a Perícia's Graduação is capped at twice the `base` of
whichever Attribute currently governs it. Raising either one point costs XP, at a different
formula per case, and can only happen with a `CharacterSheet` in hand (that's where
`unUsedExperience` lives) — this is a Character-*progression* action, not a pure computation
on a bare value.

- `AttributeValue.getTotal()` sums three independent components (`base`, `racialBonus`,
  `variable`), but only `base` is what a character invests in directly, and it's capped at
  `CharacterAttributeService.MAX_ATTRIBUTE_BASE` (5). `CharacterAttributeServiceImpl
  .upgradeBase(AttributeValue currentValue, CharacterSheet characterSheet)` raises it by
  exactly one point: checks the cap, spends `getUpgradeCost(currentValue)` XP from
  `characterSheet` (throwing `IllegalOperationException` on either failure), then returns the
  upgraded `AttributeValue`. Cost is the target base + 1 (e.g. 4→5 costs 6).
- A Perícia's Graduação is capped at `SkillGraduationService
  .GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER` (2) times the `base` — not the full total,
  `racialBonus`/`variable` don't widen this — of whichever Attribute currently *governs* that
  Perícia for this character. "Governs" isn't always the Perícia's own fixed
  `AttributeDomain`: if the character holds a `SkillCompetencyAbility` for that same skill
  granting an unconditional substitution (see the "Unconditional Perícia base-Attribute
  substitution" section below), the substituted Attribute's `base` is what the cap uses
  instead — `SkillGraduationServiceImpl.getMaxGraduation` resolves this via the same
  `SkillCompetencyAbility.resolveAttributeDomain` every substitution-aware
  `<Skill>Interaction` already calls for its roll, so the two never disagree on which
  Attribute is "in charge" of a given Perícia right now.
  `SkillGraduationServiceImpl.upgradeGraduation(Character character, CharacterSheet
  characterSheet, SkillType skillType)` raises it by exactly one point, same
  check-cap-then-spend-XP-then-mutate order as `upgradeBase`. Cost is **half** the target
  Graduação (e.g. 6→7 costs 3.5 — a `BigDecimal`, genuinely fractional, not rounded) — a
  different formula from Attributes, so don't share `getUpgradeCost` logic between the two
  services or assume one mirrors the other's numbers.
- Both `upgradeBase`/`upgradeGraduation` take the Character's data (`AttributeValue`/
  `CharacterSkill` in one, `Character` in the other) *and* a separate `CharacterSheet`
  parameter, mirroring `RestService.applyRest(Character, CharacterSheet, ...)` and
  `DamageService.applyDamage(Character, CharacterSheet, ...)`'s existing split: compute from
  the Character-side data, but only the `CharacterSheet` carries `unUsedExperience` to spend
  from (and, for Graduação, the `CharacterSkill` instance being mutated is looked up from
  `character.getSkills()`, not `characterSheet` — `Character` remains the single source of
  truth for skills/attributes/abilities).
- `CharacterSheet.useExperience` had a latent bug fixed alongside this: it used to subtract
  `expToUse` from `unUsedExperience` *before* checking whether the result was negative, so a
  rejected spend still silently corrupted the balance. Nothing called it before these two
  upgrade paths existed, so the bug was inert; it isn't anymore now that real callers exist —
  a rejected `upgradeBase`/`upgradeGraduation` call must leave `unUsedExperience` untouched
  (see `CharacterSheetTest#useExperienceLeavesUnusedExperienceUntouchedWhenRejected`).

Nothing stops `AttributeValue.builder().base(...)` or `CharacterSkill#increaseGraduation`
from being called directly with a value past either cap (or with no XP spent at all) though
(Lombok's builder has no such validation, `increaseGraduation` is a plain mutator, and
Fixture Factory/test code routinely builds `CharacterAttributes`/`CharacterSkill` straight
from the builder or fixture templates, bypassing both services entirely) — so when writing a
fixture or test:
- that needs an Attribute total above 5, put the excess in `variable` (representing
  spells/feats/equipment; `racialBonus` should stay small, matching
  `CharacterCreationServiceImpl`'s actual fixed-plus-chosen racial allocations), not `base`.
  `CharacterFixture.ATTRIBUTE_SUBSTITUTIONS` follows this (e.g. `base(5).variable(3)` for a
  total of 8) — don't regress it back to a raw `base(8)`.
- is specifically exercising `CharacterAttributeService`/`SkillGraduationService` (like
  `CharacterAttributeServiceTest`/`SkillGraduationServiceImplTest`), keep the Attribute
  `base`/Graduação and the `CharacterSheet`'s `unUsedExperience` consistent with both the cap
  and the real cost formula, the same way those test files do.

The many pre-existing `<Skill>InteractionTest` files that jump a skill straight to Graduação
7 or 10 (to unlock `PRODIGIO`/`LENDA`) alongside a low Attribute `base`, with no XP spent at
all, are **not** violating either the cap or the cost rule — they call `CharacterSkill
#increaseGraduation` directly, the same service-bypassing test convenience
`AttributeValue.builder().base(...)` already is for Attributes, and `SkillGraduationService`
doesn't gate that method. Both the cap and the cost only apply going forward through
`CharacterAttributeService.upgradeBase`/`SkillGraduationService.upgradeGraduation`
themselves; don't retrofit those existing Excelência-tier tests to comply with either.

## Adding a new Perícia (Skill)

Every new Skill (e.g. `Artes`, `Attention`) must be created with **all** of the following
pieces — don't stop at just the `Skill` class:

1. **The `Skill` itself** (in its own `org.aventyrs.core.skill.<skillname>` subpackage —
   e.g. `org.aventyrs.core.skill.artes` for `Artes`, `org.aventyrs.core.skill.atletismo` for
   `Atletismo` — lowercase, no separators): a class extending `BasicSkill` implementing
   `Skill`, setting its `AttributeDomain` in the constructor. Mirror `Attention`/`Artes` — no
   extra fields needed on the class itself. Every other piece below for this same Perícia
   (its `SkillType` constant aside, which stays in the shared base package — see step 2) goes
   in this same subpackage too, alongside its tests.

2. **A `SkillType` constant** (`org.aventyrs.core.skill.SkillType`): one enum value per
   concrete `Skill`, used to key `Character.skills` (a `Map<SkillType, CharacterSkill>`) for
   O(1) lookup instead of filtering a list.

3. **A `<Skill>Specialization` enum** (e.g. `ArtesSpecialization`): every Perícia gets one, no
   exceptions — one constant per named specialization, each with a `description`, implementing
   `SkillSpecialization` (`getSkillType()` + the inherited `getDescription()`, see
   `SkillTrait`/`SkillSpecialization` below). `CharacterSkill.specializations` is a
   `List<SkillSpecialization>` (`@Builder.Default` empty), not a per-skill-typed field — the
   enum is the well-typed *catalog* for that one skill, and any instance a character actually
   holds just needs to satisfy the shared `SkillSpecialization` interface, the same way
   `Character.skillCompetencyAbilities` holds any `SkillCompetencyAbility` regardless of which
   skill it targets. `CharacterSkill` itself never validates that a listed specialization's
   `getSkillType()` actually matches its own `skill` — same restraint as every other
   builder-bypassable invariant in this codebase (see the Attribute/Graduação section above).

4. **A `<Skill>CompetencyAbility` enum for its Habilidades de Competência, if any**,
   implementing `SkillCompetencyAbility` (`getSkillType()` + `getDescription()`, plus the
   default `getDifficultyReduction()` — override it only on constants that really grant a GD
   reduction to that *same* skill's own roll) — the Perícia-level counterpart to
   `AttributeAbility`/`EgoAdvantage`. Store instances a character has acquired in
   `Character.skillCompetencyAbilities`. (`AtletismoCompetencyAbility.ATLETA_VERSATIL` used to
   be the reference example for this override — a rules revision replaced it with `Passo
   Largo`, which needs a movement system instead, so no ability currently exercises this
   override; the mechanism itself is still real and tested generically — see the
   `noAbilityReducesDifficulty`-style test in most `<Skill>CompetencyAbilityTest` files —
   it's just waiting for the next ability that actually qualifies.)

   If an ability's acquisition is rules-gated on something like "Requer N Graduações", don't
   build a validation/eligibility service for it — none exists yet for
   `SkillCompetencyAbility` (unlike `AttributeAbilityService` for `AttributeAbility`). Just
   document the unenforced prerequisite in a comment on the constant, implementing its actual
   numeric effect for real whenever one is expressible today. No ability currently has an
   explicit "Requer N Graduações"-style clause in its own rules text to point to as a live
   example — `AtletismoCompetencyAbility.ATLETA_VERSATIL`,
   `EsquivaEApararCompetencyAbility.RECUO_RAPIDO`, and
   `MedicinaECuraCompetencyAbility.ALQUIMIA_MAIOR` each held that role in turn, and each lost
   the clause (or was removed outright) in its own later revision. Don't take this as license
   to skip documenting the next one that shows up — just don't expect to find a working
   example in the codebase to copy from right now.

   **If a constant's rules text requires the player to pick a value when acquiring it**
   ("Escolha uma Perícia…", or a pick-one-of-several-effects choice), don't TODO the whole
   ability as "no way to persist the choice" — there is one; see "Acquisition-time ability
   choices" below for the two patterns (an instance-based companion class when the choice
   feeds the ability's own `@Modifier` methods, `AcquiredChoice` otherwise) and which one
   applies.

   **For every ability whose mechanic depends on a system that doesn't exist yet**
   (a roll-vs-`DifficultyLevel` resolution engine, Vantagem/Desvantagem, ally-range effects,
   NPC-disposition/reputation, Cena/Combate-scoped state, etc.), add a `// TODO:` comment
   directly above the enum constant explaining:
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
   Don't invent the missing system just to make the TODO go away — this codebase's
   established pattern (see `GnoseAbility`, `InstinctAbility`, `CharismaAbility`) is to model
   the ability as real, tested data now and defer the mechanic honestly until its
   prerequisite system is actually built.

5. **Always implement a concrete `Interaction<CharacterSheet>` for the skill** (e.g.
   `AttentionInteraction`, `ArtesInteraction`) — never leave a Skill without one. Every
   `<Skill>Interaction` needs the exact same `applyTo`/`findCharacterSkill` machinery —
   compute the roll bonus via `CharacterSkillService.getValueForRoll`, resolve which Attribute
   currently governs the roll, sum every `SKILL_ROLL_BONUS` source, look up the trained
   `CharacterSkill` or fall back to an untrained one, sum `difficultyReduction` — so this is
   **not** something to hand-write per skill anymore: extend `AbstractSkillInteraction`
   (`org.aventyrs.core.skill`) and give it the new skill's `SkillType` constant. A concrete
   subclass needs nothing but two constructors delegating to `super(SkillType.X)`/
   `super(SkillType.X, characterSkillService, modifierResolver)` — see `ArtesInteraction` or
   `AttentionInteraction` for the ~15-line shape every skill without something genuinely
   unusual to say should match. Move any skill-specific rules-text nuance (an unenforced
   TODO, a note about a related mechanic) into the subclass's own class-level javadoc, same as
   before — it's still a real class per skill, just without the duplicated body.

   This only works because `SkillType` itself now carries everything `AbstractSkillInteraction`
   needs per skill: `excellencyClass` (already existed), a `Supplier<Skill> skillFactory`
   (`newSkillInstance()`) for the untrained-fallback `CharacterSkill`, a `ModifierType
   rollBonusType` (see "A ModifierType per skill" below), and a `Supplier<AbstractSkillInteraction>
   interactionFactory` (`newInteraction()` — see "Dispatching a roll by SkillType" below) — e.g.
   `ARTES(ArtesExcellency.class, Artes::new, ModifierType.ARTES_ROLL_BONUS,
   ArtesInteraction::new)`. **Whenever you add a new Skill, add its constant to `SkillType` with
   all four pieces, and a matching `<SKILL>_ROLL_BONUS` constant to `ModifierType`** —
   `AbstractSkillInteraction` has no other way to know which
   `Skill`/`SkillExcellency`/`ModifierType`/`<Skill>Interaction` a given `SkillType` maps to.
   Also add a matching constant to `org.aventyrs.core.ability.PeritoTeoricoAbility` (see
   "Acquisition-time ability choices" below) — it holds one `AttributeAbility` constant per
   `SkillType` so `GnoseAbility.PERITO_TEORICO` can be chosen for the new skill too; there's no
   default, so skipping it silently leaves the new skill impossible to pick.

   If a skill's Interaction genuinely needs to do something no other skill does, override
   `applyTo` in the subclass and call `super.applyTo(target)` first, then layer the addition
   on top of the returned result — see `ArtesInteraction`, which sets
   `temporaryBonusModifierType`/`temporaryBonusRounds` for a character holding `DOM_BARDICO`
   (below "Temporary bonuses from other Characters") — rather than duplicating
   `AbstractSkillInteraction`'s logic or forking it back out to a hand-written `applyTo`.

6. **A `<Skill>Excellency` enum for its automatic Excelência bonuses** (e.g.
   `ArtesExcellency`), implementing `SkillExcellency` (`getSkillType()` + `getTier()` +
   `getDescription()`). **Every** skill uses the same three universal
   `ExcellencyTier`s — `FOCADO` (graduation 3), `PRODIGIO` (7), `LENDA` (10) — only the
   bonus content per tier differs per skill. Bonuses at different tiers are additive, never
   overriding — if a later tier's rules text reads like "changes to +N" or "muda para +N"
   (regardless of the exact wording — this phrasing recurs across skills and always means the
   same thing), model it as the *delta* over the earlier tier's value, not the new total (see
   `ArtesExcellency.LENDA`, which is worth +3 on top of `FOCADO`'s +2, totaling +5, rather than
   being worth +5 itself — confirmed by Artes' own rules text changing from an explicit "+3
   adicional, totalizando +5" to "muda para +5" with the *same* numbers, across a revision).

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

   An instance-based choice-carrying ability class (see "Acquisition-time ability choices")
   gets its own test file too, shaped like `ArtesAprimorarComArteAbilityTest`: exercise each
   implemented branch through the *real* scanning service that consumes it (e.g.
   `DamageService.getTotalDamageReduction` on a `CharacterFixture` character granted the
   instance), not by calling the modifier method directly; a non-matching choice contributes
   0; a null choice is rejected at construction; and the instance reports the catalog
   constant's `SkillType` and description.

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
  `character.getRace().getRacialAbilities()` (unchanged from before); for a
  `SkillSpecialization` that means present in `characterSkill.getSpecializations()` instead
  (`characterSkill` is resolved via `findCharacterSkill` before this check runs, so an untrained
  skill's fallback `CharacterSkill` — with no specializations — correctly fails this check).
  Either failure throws `IllegalOperationException` (`REQUIRED_SKILL_TRAIT_NOT_HELD`). This
  applies generically to every skill, in one place, the same way every other `applyTo`
  computation already does.
- When the validated `requestedAbility` is a `SkillSpecialization`, `applyTo` resolves
  `InteractionResult#reachedDifficultyLevel` via `DifficultyLevel#reachedByAsExpert` instead of
  `DifficultyLevel#reachedBy` — thresholding against each tier's easier `expertValue` instead of
  its `baseValue`. This is the first real consumer of `expertValue`, which used to be dead data
  (see "Rolling the dice" below).
- This only validates *possession* of the trait, the same restraint CLAUDE.md already
  documents elsewhere (no "Requer N Graduações" enforcement, no acquisition-legality checks) —
  it doesn't validate that a `SkillCompetencyAbility`'s own mechanic is actually implemented, or
  gate on anything about the roll's circumstances (range, targets, etc.), and it doesn't
  validate that a requested `SkillSpecialization` is actually the right fit for whatever the
  roll is being used for narratively — this core still doesn't track what a roll is *for*. A
  caller can still request a trait whose effect is entirely TODO'd (an ability) or narratively
  mismatched (a specialization); the roll proceeds, it's just no longer possible to request a
  trait the character was never granted in the first place.

## Dispatching a roll by SkillType — `SkillInteractionFactory`, `SkillRollRequest`

A caller that only has a `SkillType` in hand (e.g. an API layer deserializing an incoming roll
request) previously had no way to reach the right concrete `<Skill>Interaction` without its
own hand-written switch over all of them. `SkillType` already carries every other per-skill
mapping this core needs (see "Adding a new Perícia" above), so this reuses it rather than
introducing a second, parallel "which skill is this" enum that could drift out of sync with
`SkillType` itself:

- `SkillType.newInteraction()` returns a fresh `<Skill>Interaction` via a stored
  `Supplier<AbstractSkillInteraction>` per constant, built through that Interaction's default
  (no-arg) constructor — the same one every concrete subclass already exposes.
- `SkillInteractionFactory` (`org.aventyrs.core.skill`) is the single call site for this:
  `create(SkillType)` just delegates to `newInteraction()`, and `resolve(SkillRollRequest)`
  looks up the right Interaction *and* calls its 3-arg `applyTo` in one step.
- `SkillRollRequest` (`@Builder`) is the wrapper bundling everything one roll needs:
  `skillType`/`target` (`@NonNull`, required) plus `sceneContext`/`skillRoll` (both optional,
  `null` same as every `applyTo` overload already accepts). A caller builds one of these once
  instead of separately resolving which `<Skill>Interaction` class to instantiate and calling
  its `applyTo` overload directly.
- This is purely a dispatch/ergonomics convenience — it doesn't change what any `applyTo`
  computes, and a caller that already knows which concrete `<Skill>Interaction` to use (most
  of this core's own tests) can keep constructing and calling it directly; nothing requires
  going through `SkillRollRequest`.

## Character-level stats aggregated from abilities (e.g. Reações, Ações Livres, Pontos de Ação, Iniciativa)

Some Character-level counters need a fixed base value *and* a fully-modified total summed
from abilities — this is what `ReactionsService`, `FreeActionsService`,
`ActionPointsServiceImpl.getMaxActionPoints` (the latter scans the same three sources to
support things like `AtletismoExcellency.LENDA`'s +1PA), and `InitiativeService` all do. Don't
compute the modified total inside `Character` itself (it would need to instantiate
`ModifierResolverImpl` directly, which doesn't belong on a data class):

- `Character` holds only the plain fixed counter (e.g. `reactions`/`freeActions`, a normal
  `@Builder.Default` field with Lombok's regular getter — no suppression, no manual method),
  defaulting to a constant declared on the **service** interface
  (`ReactionsService.DEFAULT_REACTIONS`, mirroring `ActionPointsService.DEFAULT_ACTION_POINTS`
  and `FreeActionsService.DEFAULT_FREE_ACTIONS`).
- A dedicated `<Stat>Service`/`<Stat>ServiceImpl` in `org.aventyrs.core.character.services`
  (e.g. `ReactionsService.getTotalReactions(Character)`) takes a constructor-injected
  `ModifierResolver` (default `new ModifierResolverImpl()`, same DI convention as every other
  service) and sums `@Modifier`/`ModifierType` bonuses across **three** sources:
  `character.getAttributeAbilities()`, `character.getSkillCompetencyAbilities()`, and — per
  trained Perícia in `character.getSkills()` — whichever `SkillExcellency` tiers that Perícia's
  graduation has unlocked (resolved generically via `SkillType.getExcellencyClass()` +
  `SkillExcellency.unlockedBy`, since the concrete `<Skill>Excellency` enum type isn't known at
  compile time from a bare `SkillType`). Clamp the total at 0.
- Ações Livres are mechanically identical to Reações (same 3-source aggregation, same
  `DEFAULT_*` shape) — the only difference between the two is *when* they may be spent (a
  Reação only in response to someone else's action; an Ação Livre also on the character's own
  Turn), which is a game-flow/Scene-timing concern this library doesn't enforce, not a
  computation difference. Don't build a distinct aggregation shape for a new counter just
  because it's spent under different narrative conditions — reuse this same pattern.
- Whenever a new fixed counter field is added to `Character` here, it must also be added to
  `CharacterFixture`'s `BLANK` template `Rule` (see the comment on `loadCharacterTemplates` —
  Fixture Factory never goes through the Lombok builder, so `@Builder.Default` values don't
  apply automatically).
- `InitiativeService.getTotalInitiative` is a variant of this same shape, not a byte-for-byte
  copy: its base isn't a plain `Character` counter defaulting to a `<Stat>Service` constant —
  Iniciativa already existed as one of the four `EgoDomain`s (`character.getEgos()
  .getIniciativa().getTotal()`, itself `base + variable`, see `EgoValue`), so that's the base
  the `ModifierType.INITIATIVE` sum from the usual three sources is added on top of, with no
  new `Character` field and nothing to add to `CharacterFixture`'s `BLANK` template. It also
  deliberately does **not** clamp at 0 like Reações/Ações Livres/RD/RA do — those are spendable
  resources where negative is meaningless, but Iniciativa is a turn-order value, so a large
  enough malus leaving it negative is still a valid (if late) position, not an error state.
  `InitiativeEntry`'s own javadoc documents how this fits into a `Scene`'s actual turn order:
  this service only computes the fixed Ego-plus-modifiers component, a caller still adds
  whichever dice roll they applied on top before handing the total to
  `Scene#addParticipant`, since this library never rolls dice itself.

Mirror this shape for any new stat abilities/competencies/excellencies can modify, and
remember to give the new `ModifierType` constant a `@Modifier`-annotated method on whichever
concrete ability/excellency should affect it (e.g. `AttentionExcellency.FOCADO` for
`REACTIONS`). No concrete ability grants `ModifierType.INITIATIVE` yet — `InitiativeService`
was built ahead of a first consumer, same as `ReactionsService` once was, not because building
it just unblocked one. The two existing rules-text mentions of a flat +2 Iniciativa
(`AttentionExcellency.LENDA`, `MeioElfo`'s Provar Seu Valor) are each still TODO'd on their
*own* missing system first — a graduation-threshold-crossing trigger for the former, a "game
session" concept for the latter's "1x por sessão" — so a plain `@Modifier(ModifierType
.INITIATIVE)` method on either wouldn't actually match its rules text yet; don't wire one in
just because the scanning now exists.

## Casting a Magia is two separate rolls — `org.aventyrs.core.magic.SpellCastingService`

Casting a Magia with a rolled effect always involves **two** rolls, not one: whichever
Perícia actually delivers the spell (e.g. `AtaqueADistanciaInteraction` for a ranged spell,
`AtaqueCorpoACorpoInteraction` for a Toque spell) rolled against the **target's** GD, followed
by a `DominioDoManaInteraction` roll against the **Magia's own** GD. `SpellCastingService
.castSpell(CharacterSheet, Interaction<CharacterSheet> deliveryInteraction)` orchestrates
this: it rolls the given delivery Interaction, then rolls Domínio do Mana, and returns both
`InteractionResult`s in a `SpellCastingResult` — it never picks the delivery Interaction
itself (the caller does, since only the caller knows which Magia/weapon is being used).

No `Magia` entity/list exists yet, so `SpellCastingService` only computes both rolls' bonuses
— it doesn't know either roll's target GD, so it can't resolve success/failure for either
roll yet. This is deliberately left as a TODO on the service itself rather than guessed at.

This is also where an ability whose effect targets the *delivery* roll, not Domínio do Mana's
own, would eventually plug in — don't try to force it onto
`SkillCompetencyAbility.getDifficultyReduction()`/`SkillExcellency`, since that hook only ever
feeds back into that *same* skill's own Interaction, never another skill's. No current ability
needs this: `DominioDoManaCompetencyAbility.FEITICEIRO` used to be the reference example (a -1
GD on the delivery roll), but a rules revision replaced it with `Magia Selvagem` (an attribute
substitution) — and `AtaqueADistanciaExcellency.LENDA`'s "bônus de conjuração" clause, the
other ability this service was built to eventually host, was *also* dropped in its own
revision. `SpellCastingService` itself is still correct (Domínio do Mana's own rules text still
describes casting as two separate rolls), it just currently has no concrete ability wired
toward this specific extension point — a reminder that these cross-references need
re-checking whenever a cited skill gets revised, and that a piece of infrastructure can outlive
the example that originally justified building it.

## Identity, and Scene sub-groups — `org.aventyrs.core.scene.Scene#getAllies`

Both `Character` and `CharacterSheet` carry their own `UUID id`, auto-generated by default but
deliberately **not** `final` — two *separate* identities, since the same `Character` could in
principle back more than one `CharacterSheet`. This project has no persistence layer of its
own, but is meant to be loaded from one (a DTO reconstructing a previously-saved Character/
CharacterSheet needs to set the *existing* id, not mint a new one), so both need a real,
settable path for that:
- `Character.id` is `@Builder.Default protected UUID id = UUID.randomUUID();` — already
  settable normally, via `Character.builder().id(existingId)...build()` (or
  `.toBuilder().id(existingId).build()`), no extra work needed there.
- `CharacterSheet` has no builder (`@RequiredArgsConstructor(staticName="of")`, not
  `@Builder`), so `id` gets its own second factory overload instead:
  `CharacterSheet.of(Character, Player)` mints a fresh one (the common case, a genuinely new
  entity); `CharacterSheet.of(Character, Player, @NonNull UUID id)` takes an existing one
  (the reconstruction-from-a-DTO case) — it just delegates to the first overload (reusing its
  `@NonNull` validation on `character`/`player` for free) and overwrites `id` before
  returning. There's still no public setter, so `id` can only be set at construction, through
  one of those two factories — don't add a general `setId(...)` just because the field is no
  longer `final`.

Neither class overrides `equals()`/`hashCode()`; every existing
`assertEquals(List.of(sheet, ...), ...)` test relies on reference equality and still does —
`id` is meant for callers (like `Scene`) that need to recognize "this is the same
participant" without holding the exact same object reference, not for changing what
`.equals()` means on these classes. Don't add `equals()`/`hashCode()` overrides keyed on `id`
without checking every existing reference-equality-based usage first.

`Scene` groups its participants into sub-groups via `InitiativeEntry.group` (a `UUID`, no
dedicated `Group` class — nothing about a group needs data beyond an identifier). Two
`addParticipant` overloads exist: the original 2-arg one now delegates to a 3-arg one, passing
`UUID.randomUUID()` — so a participant added without an explicit group starts in a group of
one, with no default allies, and every one of the 12 pre-existing `SceneTest` methods keeps
passing unchanged. Pass the *same* `UUID` to every `CharacterSheet` that should consider each
other allies (e.g. a party, or a pack of enemies) via the 3-arg overload.

`Scene.getAllies(CharacterSheet)` returns every other participant sharing that Perícia's
group — excluding the caller itself, matching how `DOM_BARDICO`'s targeting works ("a eles,
mas não a você"), which is the ability this method exists to eventually support (still not
wired: `DOM_BARDICO` itself remains TODO'd on the GD-tier-to-bonus lookup and Rodada-scoped
duration, neither of which this method solves). It searches both `activeEntries` and
`pendingEntries` — sub-group membership isn't a turn-order concern, so a participant added
mid-Round is already an ally before joining the rotation — and throws
`IllegalOperationException` (`CHARACTER_SHEET_NOT_IN_SCENE`) if asked about a `CharacterSheet`
that was never added to this `Scene`, rather than silently returning an empty list (which
would mask a caller bug the same way a typo'd wrong `CharacterSheet` would).

`CharacterFixture`'s `id` field is a trap worth knowing about: its `Rule`'s
`UUID.randomUUID()` call runs once, when `loadTemplates()` executes — not once per
`blank`/`gimme` call — so every `Character` built from the same template *within one test*
shares that one `id`. This doesn't affect `Scene`'s correctness (it keys off
`CharacterSheet.id`, and `CharacterSheet.of(...)` is a real constructor call that mints a
fresh `id` every time, never templated), but a test that specifically needs several distinct
`Character`s with distinct `id`s must override `.id(UUID.randomUUID())` on each one via
`CharacterFixture.blank(...)`'s returned builder.

## A ModifierType per skill — fixing cross-skill bonus leakage

`ModifierType.SKILL_ROLL_BONUS` applies broadly, to *every* Perícia's roll — that's correct
for abilities like `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS` (see the Vantagem
section below), but it was, until this was fixed, the *only* option `AbstractSkillInteraction`
summed — meaning a bonus meant for one specific Perícia (e.g. a temporary buff an ally
received "for Atletismo") had no way to avoid leaking into every other Perícia's roll too,
since `sumSkillRollBonusModifiers`/`getTemporaryBonus` had no per-skill filter to apply.

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
  constant to `ModifierType` too** (see "Adding a new Perícia" above) — there's no default;
  omitting it would make `SkillType.rollBonusType` uninitializable.

## Temporary bonuses from other Characters — `CharacterSheet#grantTemporaryBonus`

A `CharacterSheet` can hold bonuses/maluses that didn't come from its own `Character`'s
abilities — granted by *another* Character's action instead, lasting a few Rodadas. The
motivating example is `ArtesCompetencyAbility.DOM_BARDICO`: motivating allies (not the
caster) with a Perícia-roll bonus for 1-3 Rodadas depending on the caster's own Graduação.

- `TemporaryBonus` (`org.aventyrs.core.sheet`) pairs a `ModifierType`, an `int value`, and a
  `remainingRounds` countdown — reusing the existing `ModifierType` taxonomy rather than
  inventing a parallel one. Pass either the broad `ModifierType.SKILL_ROLL_BONUS` or one
  specific skill's own type (see "A ModifierType per skill" above) depending on whether the
  granting ability's rules text names one Perícia or, like `DOM_BARDICO`'s, is unrestricted.
  It counts down in *rounds remaining*, not an absolute expiry Round number, matching how
  DOM_BARDICO's own rules text describes duration ("por 1 Rodada", "por 2 Rodadas") rather
  than coupling `CharacterSheet` to `Scene`'s specific round-numbering.
- `CharacterSheet.grantTemporaryBonus(ModifierType, int value, int rounds)` adds one;
  `getTemporaryBonus(ModifierType)` sums every currently-active (non-expired) one of that
  type — every `<Skill>Interaction` now includes both the generic and its own skill-specific
  type in its `SKILL_ROLL_BONUS` sum. `CharacterSheet` doesn't track *who* granted a bonus —
  nothing about this mechanism needs that; find targets via `Scene.getAllies` at grant time
  instead.
- `CharacterSheet.tickTemporaryBonuses()` counts every held bonus down by one Rodada and
  discards any that expire as a result. **Nothing calls this automatically yet** — per this
  feature's own instructions, `Scene` doesn't have a "turn shifter" that advances every
  participant's `CharacterSheet` when a Round completes; `next()` only cycles whose turn it
  is and increments `getCurrentRound()`. Once that turn-shifter exists, it's expected to call
  `tickTemporaryBonuses()` on every participant's `CharacterSheet` — the mechanism here is
  real and tested (`CharacterSheetTest`, `ArtesInteractionTest
  #applyToIncludesAnActiveTemporarySkillRollBonus`/`#applyToIgnoresAnExpiredTemporarySkillRollBonus`),
  it's specifically the automatic *trigger* that's deferred.
- `InteractionResult.temporaryBonusValue`/`temporaryBonusModifierType`/`temporaryBonusRounds`/
  `temporaryBonusScope` are where a roll that *grants* one of these (as opposed to merely
  being affected by an already-granted one) is expected to report it.
  `temporaryBonusModifierType` is a `ModifierType`, not a `SkillType` — deliberately matching
  `TemporaryBonus`'s own field, so it's exactly what a caller passes straight into
  `CharacterSheet#grantTemporaryBonus` with no extra `SkillType`→`ModifierType` mapping step
  (the broad `SKILL_ROLL_BONUS`, or one specific skill's own `rollBonusType`).
  `temporaryBonusScope` is a `TargetScope` (`SINGLE_TARGET`/`ALLIES`/`ENEMIES`) — *who kind*
  of recipient, e.g. `ALLIES` for DOM_BARDICO. Who actually *receives* the bonus is still
  deliberately not this core's concern — these four fields are the computed "what" (value,
  type, duration) and "who kind" (scope), a caller resolves the concrete recipient list via
  `Scene.getAllies`/`getEnemies` (for `ALLIES`/`ENEMIES`) or its own target lookup (for
  `SINGLE_TARGET`) and calls `grantTemporaryBonus` on each recipient itself. `InteractionResult`
  needed `@Builder(toBuilder = true)` added for this — a subclass overriding `applyTo` (see
  below) extends the base result via `.toBuilder()` rather than reassembling every field by
  hand.
- `ArtesInteraction` overrides `applyTo` to set all four of these fields for a character
  holding `ArtesCompetencyAbility#DOM_BARDICO`: `temporaryBonusModifierType` (always
  `SKILL_ROLL_BONUS`, since this ability's own rules text is unrestricted — "rolagens de
  Perícias", not one specific Perícia), `temporaryBonusScope` (always `ALLIES` — "concedendo
  ... a eles, mas não a você"), `temporaryBonusRounds` (1 Rodada normally, 2 once Artes reaches
  5 Graduações, 3 at 10 — a small graduation-threshold lookup specific to this one ability, not
  worth generalizing into `ExcellencyTier`'s fixed 3/7/10 shape since DOM_BARDICO's
  thresholds/values don't match it), and — once a `SkillRoll` is supplied —
  `temporaryBonusValue`, via a GD-tier-to-bonus lookup (`ArtesInteraction
  .domBardicoBonusValue`): GD Médio +1, Difícil +2, Muito Difícil +3, Improvável +4, Milagre
  +5, mapping onto `DifficultyLevel.MEDIUM/HARD/VERY_HARD/UNLIKELY/MIRACLE`. `UNIMAGINABLE`
  isn't named in the rules text (it falls between Improvável and Milagre); it's treated as
  inheriting Improvável's +4 until Milagre is actually reached — an inference, not confirmed
  text. Below Médio, or when no roll was supplied at all, `temporaryBonusValue` stays `null`. A
  caller must still gate on `temporaryBonusValue != null` before granting anything.
- **`AbstractSkillInteraction.applyTo` actually has two overloads now**:
  `applyTo(CharacterSheet)` (the `Interaction` interface's own method) just delegates to
  `applyTo(CharacterSheet, SceneContext)` with a `null` context; the 2-arg one holds all the
  real logic. A subclass overrides the **2-arg** one (not the 1-arg one) — `ArtesInteraction`
  does — and calls `super.applyTo(target, sceneContext)` first; the 1-arg delegation still
  reaches the override correctly through ordinary virtual dispatch (`this.applyTo(target,
  null)` inside the base class's 1-arg method resolves to the subclass's override at
  runtime), so a subclass never needs to *also* override the 1-arg method. See "Range and
  SceneContext" below for what `SceneContext` is and why this exists, and "Rolling the dice"
  below for `SkillRoll` (the third parameter, added after `SceneContext`) — no ability
  consumes `SceneContext` for a real numeric effect yet, so passing `null` (or any actual
  `SceneContext`) currently produces identical results for every skill; this is deliberately
  built ahead of a concrete consumer, same as the four `TemporaryBonus`-related fields above
  were before `ArtesInteraction` started setting them.

## Rolling the dice — `SkillRoll`, `DifficultyLevel#reachedBy`

This core deliberately never rolls dice itself (see the `skill` package-info's "What this
library computes" section) — but plenty of abilities' effects depend on which GD tier a roll
*reached*, or whether it was a critical, so an API layer needs a way to hand an
already-physically-rolled result in, and this core needs a way to turn that into something
useful. Every Perícia roll in this ruleset is 3d6 — always exactly 3 six-sided dice.

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
  lookup against `expertValue` instead — see "Requesting a specific Habilidade de Competência
  or Especialização on a roll" above for how `AbstractSkillInteraction` picks between the two
  (a held, matching `SkillSpecialization` named as the roll's `requestedAbility` switches to
  `reachedByAsExpert`). Neither method validates *why* a caller picked one over the other —
  resolving "does this roll's Especialização match what it's being used for" beyond mere
  possession is still a separate, unbuilt concern (this core doesn't track what a roll is *for*
  — same gap as scoped Vantagem/substitution elsewhere).
- `AbstractSkillInteraction` gained a third `applyTo` overload —
  `applyTo(CharacterSheet, SceneContext, SkillRoll)` — following the exact same
  cascading-delegation shape as the `SceneContext` overload before it: each shorter overload
  just delegates down with `null` for the new parameter, and the longest one holds all the
  real logic. When `skillRoll` is non-`null`, it sets `InteractionResult.reachedDifficultyLevel`
  (from `DifficultyLevel.reachedBy`/`reachedByAsExpert(skillRollBonus + skillRoll.getTotal())`,
  picked per the previous bullet) and `.criticalResult` (from `skillRoll.getCriticalResult()`);
  both stay `null` otherwise, same as every other not-applicable `InteractionResult` field.
  `ArtesInteraction` now overrides this 3-arg overload (it used to override the 2-arg one) —
  a subclass always overrides the *longest* overload whose real logic it needs to build on
  top of, not a shorter one, even if it doesn't touch that overload's own newest parameter
  itself; every shorter overload still reaches its override correctly through virtual
  dispatch, so this changes nothing for callers using `applyTo(target)`.
- The engine to know *which* tier a roll reached now exists; each ability's own consumption
  of that is still separate, per-ability wiring — `ArtesCompetencyAbility.DOM_BARDICO` is done
  (its 5-tier bonus table now maps onto 5 consecutive `DifficultyLevel` constants, with only
  `UNIMAGINABLE` handled by inference rather than named text — see `ArtesInteraction`'s own
  javadoc), but e.g. `MedicinaECuraExcellency.FOCADO`'s auto-success is still unbuilt.
  Deliberately don't guess at a mapping where the rules text leaves genuine ambiguity — DOM_
  BARDICO's own `UNIMAGINABLE` inference is flagged as exactly that, an inference, not text.

## Range and SceneContext — `org.aventyrs.core.scene.SceneContext`

Some ability bonuses are conditioned on how close an ally or enemy currently is — e.g.
`MedicinaECuraExcellency.FOCADO`'s "se não tiver inimigos próximos (Distância Curta)" or
`AtaqueADistanciaCompetencyAbility.FRIEZA`'s "contra alvos em Distância Curta ou inferior".
This core has no grid/positioning system (it never will — same "this core doesn't roll dice"
philosophy applies to "this core doesn't do geometry"), so distances are always supplied
already-resolved by a caller, same as `InitiativeEntry`'s own `initiativeValue` already is.

- `Range` (`org.aventyrs.core.scene`) is "Calculando Unidades de Distância (UD)"'s seven bands
  — `ADJACENTE` (1 UD), `DISTANCIA_MUITO_CURTA` (2), `DISTANCIA_CURTA` (4), `DISTANCIA_MEDIA`
  (8), `DISTANCIA_LONGA` (16), `DISTANCIA_MUITO_LONGA` (24), and `AO_ALCANCE_DOS_OLHOS` (no
  fixed UD — "a distância máxima é limitada à capacidade visual do personagem" per the rules
  text, so its `maxUnidadesDeDistancia` is `null`, the one constant where that field doesn't
  apply). Ordered nearest-to-farthest so `isWithin(Range maxRange)` can express "Distância
  Curta ou inferior" directly via ordinal comparison. **The first version of this enum shipped
  without `DISTANCIA_MUITO_CURTA`** — built from memory instead of the actual rules text —
  even though `AttentionCompetencyAbility.PERCEPCAO_DE_FOXM` and `AtaqueADistanciaCompetencyAbility
  .DISPARO_RICOCHETE` already referenced that exact band; a reminder to get the source rules
  text for a new domain enum like this before modeling it, not just the handful of usages
  already seen in this codebase's own TODOs. `fromUnidadesDeDistancia(int)` resolves a raw UD
  count to its band (anything past 24 UD resolves to `AO_ALCANCE_DOS_OLHOS`) for a caller that
  tracks distance as a number rather than resolving the band itself.
- `Scene.getEnemies(CharacterSheet)` is the complement of the existing `getAllies` — every
  participant *not* sharing that Character's sub-group. This is a simplification worth
  remembering: with more than two sub-groups in one `Scene` (e.g. two feuding NPC factions
  plus the PCs), "not my group" and "actually hostile to me" aren't necessarily the same
  thing, but this core has no faction-relationship/allegiance concept beyond the binary "same
  group or not" — document that gap rather than silently over-scoping if it ever matters.
- `SceneContext` is a **plain, resolved snapshot** — `List<CharacterSheet> allies`,
  `List<CharacterSheet> enemies`, and a `Map<CharacterSheet, Range>` of known distances (only
  needs entries for whoever's actually close enough to matter — anyone missing from the map
  reads as out of range) — with **no `Scene` reference of its own**. It deliberately doesn't
  hold or query a `Scene`: an earlier version did (`allies`/`enemies` delegated live to
  `scene.getAllies(actor)`/`getEnemies(actor)` on every call), but that coupled a supposedly
  lightweight context to the whole `Scene` class, made it awkward to construct in isolation
  (a test needing one had to first build a real `Scene` with participants added), and gave it
  implicit "live" semantics — the ally/enemy lists could change between two calls if `Scene`
  mutated in between — that nothing about "a snapshot passed into one roll" actually wants.
  `Scene.buildContext(CharacterSheet, Map<CharacterSheet, Range>)` is the convenience for the
  common case of already having a live `Scene`: it resolves `getAllies`/`getEnemies` **once**
  and hands the result to `SceneContext`'s plain constructor. A test, or any caller without a
  `Scene`, builds one directly from two lists instead.
- Exposes `getDistanceTo(CharacterSheet)`, `hasAllyWithin(Range)`/`hasEnemyWithin(Range)` (the
  latter is exactly `MedicinaECuraExcellency.FOCADO`'s own condition,
  `hasEnemyWithin(Range.DISTANCIA_CURTA)`), and `countAlliesWithin(Range)`/
  `countEnemiesWithin(Range)` — for a bonus that scales with *how many* allies/enemies are
  nearby (a "gang up" bonus, a "surrounded" malus), not just whether any are. `hasAllyWithin`/
  `hasEnemyWithin` are themselves just `countXWithin(range) > 0` now — one counting primitive
  underneath both the boolean and the scaling query, instead of two separate implementations
  that could drift.
- Most proximity-gated abilities found so far still need at least one *other* missing system
  too (a roll-resolution-vs-`DifficultyLevel` engine, a trigger/temporary-buff-after-success
  mechanism, a GD-*increase* expression, or arbitrary-point targeting `SceneContext` doesn't
  do) — `FOCADO`, `AttentionCompetencyAbility.PERCEPCAO_DE_FOXM`,
  `PersuasaoCompetencyAbility.ESPALHAR_EMOCOES`, `FurtividadeCompetencyAbility
  .ESCONDER_OUTROS`, and `AtaqueADistanciaCompetencyAbility.DISPARO_RICOCHETE` are still
  blocked this way — but each one's TODO cites precisely which piece of its own gap
  `SceneContext` closed, and which piece(s) remain. `FRIEZA` is the first to be fully wired:
  its Vantagem-on-damage bonus needed a `DamageBonus` value class (`value`/`DamageType`,
  `org.aventyrs.core.character`) and an `InteractionResult#damageBonus` field to carry it. A
  first pass tried granting the flat amount the ordinary way — a `@Modifier
  (ModifierType.DAMAGE_BONUS)` method summed via `ModifierResolver`, like every other flat
  Vantagem in this codebase — but that only works for the *amount*; `ModifierResolver.invoke`
  always calls the annotated method with zero args, so it has no way to also gate on the real
  attack target's distance (same limit already documented in "Acquisition-time ability
  choices" for `getBaseDamageBonus(SkillType)`-style branches). That was reworked into a third
  `default` method on `SkillCompetencyAbility` itself — `resolveDamageBonus(SceneContext,
  CharacterSheet attackTarget)`, `Optional.empty()` by default, mirroring the existing
  `getSubstituteAttributeDomain()` default-method-plus-override shape — so both the amount
  *and* the condition live together on the constant that grants it (`FRIEZA` overrides it,
  checking `sceneContext.getDistanceTo(attackTarget).isWithin(Range.DISTANCIA_CURTA)` and
  returning `DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO)` only when that holds).
  `ModifierType.DAMAGE_BONUS` was removed again since nothing uses it anymore.
  `AtaqueADistanciaInteraction` gained a new `applyTo(CharacterSheet, SceneContext, SkillRoll,
  CharacterSheet)` overload taking the actual attack target explicitly (the plain 1-/2-/3-arg
  overloads have no notion of which `CharacterSheet` a given attack is against) and just scans
  `character.getSkillCompetencyAbilities()` for the first non-empty `resolveDamageBonus` —
  it doesn't know or care that `FRIEZA` is the only constant that currently ever returns one.
  Check each constant's own TODO before assuming proximity is the only thing standing between
  it and being real.
  `AbstractSkillInteractionTest
  .GangUpBonusInteraction` demonstrates the count-scaled shape a real ability of this kind
  would take (a private test-only subclass, same pattern `DamageServiceImplTest` uses for
  test-only ability classes) — `min(countAlliesWithin(range) * perAllyBonus, max)`, computed
  inside a subclass's `applyTo(CharacterSheet, SceneContext)` override — without inventing a
  named ability or a new abstraction for "count-scaled bonuses" to back it, since no real
  ability needing this shape has turned up in the ruleset yet; when one does, follow that
  test's shape rather than the boolean `hasAllyWithin`/`hasEnemyWithin` one.

## Vantagens de Ego are unified on `Character#egoAdvantages`, and can condition a bonus on `SceneContext`

A Vantagem de Ego (e.g. `AutocontroleAdvantage`, `InitiativeAdvantage`) is chosen once at
character creation, gated on that `EgoDomain`'s creation-time `base` reaching
`CharacterCreationService.EGO_ADVANTAGE_MIN_BASE` (3) — the same threshold for every domain,
checked via the single generic `isEgoAdvantageAvailable(EgoDomain, CharacterEgos)` rather than
a separate `isXAdvantageAvailable` method per domain (an earlier version had exactly that —
`isAutocontroleAdvantageAvailable`/`isInitiativeAdvantageAvailable`, and two matching
`_MIN_BASE` constants — before the threshold was confirmed identical across domains; don't
reintroduce a per-domain method/constant pair for a future domain's catalog unless its
threshold is ever confirmed to genuinely differ from 3). `Character` stores every domain's
choice in a
single `@Singular Map<EgoDomain, EgoAdvantage> egoAdvantages` — **not** one nullable field per
domain (an earlier version had separate `autocontroleAdvantage`/`initiativeAdvantage` fields;
that stopped scaling once a second domain's catalog needed generic scanning, see below). A
domain with no eligible or chosen Vantagem is simply absent from the map, never a `null` value
inside it. Read one back via `Character#getEgoAdvantage(EgoDomain)` (mirrors
`CharacterEgos#getEgo`), and set one on the builder via the `@Singular`-generated
`.egoAdvantage(EgoDomain, EgoAdvantage)` (mirrors `.skill(SkillType, CharacterSkill)`) — never
index `egoAdvantages` directly outside those two spots.

`EgoAdvantage` carries two `default` hooks alongside `getEgoDomain()`/`getDescription()`,
mirroring `SkillCompetencyAbility`'s own `resolveConditionalRollBonus`/`resolveDamageBonus`
shape (same reason: this data isn't reflection-discoverable via a no-arg `@Modifier` method) —
but summed generically across **every** skill rather than needing a per-skill `TemporaryBonus`-
style ModifierType, since a Vantagem de Ego was never tied to one Perícia to begin with:

- `default Optional<Integer> resolveConditionalRollBonus(SceneContext sceneContext)` — a bonus
  toward *any* Perícia roll, summed by `AbstractSkillInteraction#sumEgoAdvantageRollBonuses`
  into `skillRollBonus` for every skill's own `applyTo`, the same additive convention every
  other `skillRollBonus` source already uses.
- `default Optional<DamageBonus> resolveDamageBonus(SceneContext sceneContext)` — a bonus
  toward a dano roll, resolved by `AbstractSkillInteraction` itself (not a skill-specific
  Interaction) whenever `skillType.isAttackSkill()`, via
  `#resolveEgoAdvantageDamageBonus` — first non-empty wins, same "only one bonus expected to
  apply per roll" convention as `SkillCompetencyAbility#resolveDamageBonus`. Unlike that
  method's own wiring (only reachable through `AtaqueADistanciaInteraction`'s special 4-arg
  `applyTo(..., CharacterSheet attackTarget)`, since `FRIEZA`'s proximity condition needs the
  real target — melee stays unwired for it, per the "Racial Abilities reuse
  SkillCompetencyAbility" section above), an `EgoAdvantage`'s own `resolveDamageBonus` needs no
  `attackTarget`, so it's resolved for both Ataque à Distância *and* Ataque Corpo a Corpo for
  free, straight off the plain `applyTo(target, sceneContext)` overload.

Both default to `Optional.empty()`; only override on a constant whose rules text actually
grants a bonus scoped to per-roll `SceneContext` facts. `InitiativeAdvantage.IMPETO` is the
first (and, as of this writing, only) constant overriding either — see the next section for
what it needed `SceneContext` itself to grow first.

### Cena de Combate, Rounds, and "ganhou a iniciativa" — `Scene`/`SceneContext`

`IMPETO`'s own rules text ("nas duas primeiras Rodadas de cada Cena de Combate," "se tiver
ganho a iniciativa") needed three facts `SceneContext` didn't carry before: whether the Scene
is currently a Cena de Combate, which Round it's on, and whether the acting Character's own
sub-group won initiative. All three are resolved once, by `Scene`, and carried into the
snapshot the same already-resolved way `allies`/`enemies`/`distances`/`terrainType` already
are — this class still never queries a live `Scene` at bonus-resolution time.

- `Scene.combatScene` (`isCombatScene()`/`setCombatScene(boolean)`) is the "whether it's a
  Cena de Combate" state this class's own javadoc predicted back when `terrainType` was added.
  `false` until a caller flips it once combat actually breaks out — a Cena starts as a plain
  Cena, same as `terrainType` starts `null`/unset.
- `Scene.wonInitiative(CharacterSheet)` resolves "ganhou a iniciativa" at the sub-group level,
  not per-individual: a sub-group's own Iniciativa "value" is the highest individual
  `InitiativeEntry#getInitiativeValue()` among its members (matching how a party typically
  acts as a block on whichever member rolled best), compared against every *other* sub-group's
  own highest value. A tie for the overall highest is a win for every sub-group sharing it —
  the rules text this models names no tie-breaker, so don't invent one.
- `Scene#buildContext` now also resolves `combatScene`/`getCurrentRound()`/`wonInitiative(...)`
  into the `SceneContext` it builds, alongside the pre-existing allies/enemies/distances/
  terrain. A caller building a `SceneContext` directly (most tests, or no live `Scene` at all)
  gets non-combat defaults (`false`/`0`/`false`) from the shorter constructors instead — the
  same cascading-delegation shape `terrainType` already established.
- `SceneContext.isWithinFirstCombatRounds(int roundCount)` is the shared round-window
  primitive every "duas primeiras Rodadas de cada Cena de Combate"-style Vantagem needs (not
  just `IMPETO` — `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO`/`TORRE_EM_MOVIMENTO` use the
  identical window in their own still-TODO'd rules text), so it lives here once rather than
  duplicated per ability. **Round 0 never counts as one of these** — it's `Scene`'s own
  "before anyone has acted yet" starting value (see `Scene#getCurrentRound()`'s own javadoc),
  not a real first Round of combat, so eligibility starts at Round 1: `roundCount=2` covers
  Rounds 1 and 2, not 0 and 1. This is a deliberate reading of the rules text's Round
  numbering, not something the text spells out explicitly — worth rechecking if a future
  ability's own text seems to assume the window starts at Round 0 instead.

Building this mechanism doesn't retroactively finish `POSICIONAMENTO_ESTRATEGICO`/
`TORRE_EM_MOVIMENTO` — both still cite a *different* missing system each (no movement/
Reação-suppression mechanism or Movimento Base stat for the former; `DamageService` taking no
`SceneContext` at all for the latter, so its RA/`HALF_DAMAGE` summation still can't be scoped
to specific Rounds). Check each constant's own TODO rather than assuming `SceneContext`'s new
fields alone unblock it.

## Races live in `org.aventyrs.core.race`, not `org.aventyrs.core.character`

`Race` and every implementation (`Human`, `Anao`, `Elfos`, `Gigantes`, `Pequenino`, `Gnomos`,
`Orcs`, and any `*RacialAbility` enums) live in their own top-level package, `org.aventyrs.core
.race` — a sibling of `org.aventyrs.core.character`, not a subpackage of it, mirroring how
`org.aventyrs.core.ability`/`org.aventyrs.core.feat` already sit alongside `character` rather
than inside it. `Character` still holds a `Race race` field (and imports `org.aventyrs.core
.race.Race` for it) — the two packages reference each other (`Race#generateEmptyCharacter`
returns a `Character.CharacterBuilder`), which is an ordinary mutual class dependency Java has
no trouble with, not a layering violation. If a future race needs its own subpackage
(mirroring `org.aventyrs.core.skill.<skillname>`'s one-subpackage-per-Perícia convention),
nothing here rules that out — there just wasn't a need for it yet.

Not every race needs a `*RacialAbility` catalog: `Gigantes`/`Pequenino` (and the newly
fleshed-out `Human` javadoc) leave `getRacialAbilities()` at `Race`'s own empty default,
because none of their racial traits actually fit `SkillCompetencyAbility`'s shape — some
aren't roll-conditioned at all (a Defesa modifier, a conditional RD), one needs a target
classification this core can't make, one spans multiple `SkillType`s by `AttributeDomain`
rather than naming one, and several are really "grant an acquisition slot" traits (matching
`Elfos`' Origem Mística / `Anao`' Pequenos Gigantes' already-documented gap), not roll
bonuses. Don't force a `*RacialAbility` enum into existence just for symmetry with `Anao`/
`Elfos` — only build one once a trait's shape genuinely fits.

`Gigantes` is also the first race to override `getNewFeatCost(FeatCategory)` for real
(Talentos de Sobrevivência cost 2 XP instead of `Race.BASE_NEW_FEAT_COST`'s 3) — unlike the
2.5-XP-style discounts every other race's own "Talentos custam menos" trait wants (`Elfos`'
Conexão com o Mana, `Human`'s/`Pequenino`'s own Adaptação), 2 is a whole number `int` can
represent exactly, so this one didn't hit the int-vs-fractional mismatch those did.

## Racial Abilities reuse `SkillCompetencyAbility` — `Race#getRacialAbilities()`

A trait every member of a Race is automatically granted (e.g. Anões' Abatedores de Gigantes,
Vantagem on Ataque rolls against a target 2+ Categorias de Tamanho larger) contributes to a
roll the *exact* same way a player-acquired `SkillCompetencyAbility` does — so it's modeled as
one, rather than a parallel duplicate type: `Race.getRacialAbilities()` (default `List.of()`,
overridden e.g. by `Anao` returning `List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES)`) is
just another `List<SkillCompetencyAbility>`, differing from `Character
.getSkillCompetencyAbilities()` only in *where* it's sourced from — fixed per Race, not a
player's per-Character acquisition choice — never in how a consumer treats its entries. A
consumer that wants "every ability of this kind, acquired or racial" concatenates both lists
(see `AtaqueADistanciaInteraction`, below) rather than adding a second parallel scan.

This is also what motivated `SkillCompetencyAbility#resolveAttackRollBonus(CharacterSheet
actor, CharacterSheet attackTarget)`, a fourth `resolve*`/`get*` default method alongside
`getDifficultyReduction()`/`getSubstituteAttributeDomain()`/`resolveDamageBonus()` — Abatedores
de Gigantes' bonus targets `skillRollBonus` itself (the Perícia roll, not a dano roll), and,
like `FRIEZA`'s `resolveDamageBonus`, is conditioned on the real attack target (its
`SizeCategory` here, not its distance), which a reflection-invoked no-arg `@Modifier` method
still can't see. Unlike `resolveDamageBonus` (only one bonus expected to apply per roll, so
callers take the first non-empty result), `resolveAttackRollBonus` returns a plain `Optional
<Integer>` meant to be *summed* across every ability that grants one — the same additive
convention every other `skillRollBonus` source already follows.

Two consumers wire this in, at two different levels:

- **Generically, for every skill**: `AbstractSkillInteraction` itself now has a private
  `allSkillCompetencyAbilities(Character)` helper — `character.getSkillCompetencyAbilities()`
  concatenated with `character.getRace().getRacialAbilities()` — and every place its `applyTo`
  used to scan only the acquired list (the `@Modifier`-based `skillRollBonus` sum, the
  `getDifficultyReduction()` sum, and `SkillCompetencyAbility.resolveAttributeDomain`) now
  scans this combined one instead, with zero special-casing. `validateRequestedAbility` checks
  both lists too, so a `SkillRoll` can name a racial ability as its `requestedAbility` the same
  as an acquired one. This is what makes `ElfosRacialAbility.SENTIDOS_ABSOLUTOS` (unconditional
  Vantagem on every Atenção roll — an ordinary `@Modifier(ModifierType.ATTENTION_ROLL_BONUS)`
  method, granted the same flat-Vantagem way as any acquired ability, see "Vantagem is a flat
  +2 bonus" above) work automatically for every `Elfos` character, with no
  `AttentionInteraction`-specific code at all.
- **Explicitly, where an ability needs the real attack target**: `resolveDamageBonus`/
  `resolveAttackRollBonus` aren't part of that generic scan (they need `attackTarget`, which
  `AbstractSkillInteraction`'s shared `applyTo` has no parameter for) — `AtaqueADistanciaInteraction
  #applyTo(CharacterSheet, SceneContext, SkillRoll, CharacterSheet)` (the same overload FRIEZA
  needed) does its own concatenation and resolves both against it identically, never checking
  *which* constant or *which* source answered. `AnoesRacialAbility.ABATEDORES_DE_GIGANTES` only
  overrides `getSkillType()` to `ATAQUE_A_DISTANCIA` and is only wired into this one Interaction
  so far — the rules text actually covers every "Perícia de Ataque" (`SkillType#isAttackSkill()`),
  but `AtaqueCorpoACorpoInteraction` doesn't yet take an `attackTarget` parameter, so the melee
  side isn't wired; a future change extending it should follow this exact same shape rather
  than inventing a new one.

## Damage mitigation — `org.aventyrs.core.character.services.DamageService`

Damage isn't just "subtract from PV" — there are three layers of mitigation, applied in a
fixed order:

1. **RD (Redução de Dano)** and **RA (Redução Absoluta)** — two independent flat reductions,
   both summed via the standard three-source scan (`attributeAbilities`,
   `skillCompetencyAbilities`, unlocked `SkillExcellency` tiers) through
   `DamageService.getTotalDamageReduction`/`getTotalAbsoluteDamageReduction`. The only
   difference between them is whether an attack/effect can choose to bypass it: RD can be
   ignored (`calculateFinalDamage`'s `ignoreDamageReduction` flag skips it, but RA is always
   applied regardless). Floored at 0 individually, same as `ReactionsService`/
   `FreeActionsService`.
2. **Half damage** — applied *last*, after RD/RA have already reduced the raw amount, via the
   `halfDamage` flag on `calculateFinalDamage`. Rounds down (integer division), matching
   `RestServiceImpl`'s existing floor-on-half convention.
3. **Shield points** — unrelated to RD/RA/half-damage and unchanged by this: still absorbed
   inside `CharacterSheet#applyDamage` itself, *after* `DamageService` has already computed the
   post-mitigation amount. `DamageService.applyDamage(Character, CharacterSheet, int rawDamage,
   boolean ignoreDamageReduction, boolean halfDamage)` bridges the two, mirroring
   `RestService.applyRest`'s Character+CharacterSheet split — compute from the Character's
   abilities, then apply the result to the CharacterSheet's resource pools.

If an ability grants RD without spelling out a number in its own rules text, its value is
`DamageService.DEFAULT_DAMAGE_REDUCTION` (+2) — only deviate from that when the text gives an
explicit number (e.g. `ArtesCompetencyAbility.APRIMORAR_COM_ARTE`'s "+1 RDS" is real data, not
the default, because the text says "+1").

Note that RD becoming mechanically real doesn't automatically make every RD-granting ability
real: `APRIMORAR_COM_ARTE` grants RD as *one branch of a choice* (which Perícia was picked —
solved by the instance-based ability pattern, see the next section: its RDS branch now works
for real, while its Dano Base and Margem Crítica Menor branches stay TODO'd on
`ArtesAprimorarComArteAbility` blocked on their own missing systems), and
`ProfissaoCompetencyAbility.FORJA_VULCANA` grants RD as one branch of a *different* per-item
choice (made at item creation, not ability acquisition) that's still blocked on the missing
Item/Equipamento entity entirely. Check what's *actually* stopping an ability before assuming
a newly-built mechanism resolves it completely.

## Acquisition-time ability choices — `org.aventyrs.core.ability.AcquiredChoice`

Some abilities require the player to pick a value when they're acquired — a Perícia, or, for
a future ability, one of several fixed effects. There are **three** patterns for storing that
choice, and which one applies depends on what consumes it and whether the choice space is
open-ended or already fully known at compile time:

- **When the choice feeds the ability's own `@Modifier` methods** (i.e. the ability's numeric
  effect differs per character depending on what they picked), don't use `AcquiredChoice` —
  make the ability an *instance-based class* implementing the usual ability interface, with
  the chosen value as a constructor-injected final field, and grant that instance in the
  normal `Character` list. `ArtesCompetencyAbility.APRIMORAR_COM_ARTE` is the reference:
  the enum constant stays as the catalog/rules-text entry, but characters are granted an
  `ArtesAprimorarComArteAbility(chosenSkill)` in `skillCompetencyAbilities` instead. Because
  `ModifierResolver` invokes `@Modifier` methods *on the source instance* (and caches
  reflection per class, not per instance), a modifier method can branch on the instance's own
  choice field — the existing three-source scans (`DamageService`, `ReactionsService`, every
  `<Skill>Interaction`, …) then pick the right value up with **zero** changes to any scanning
  service. Use `SkillType.isAttackSkill()` when a branch keys on "Perícias de Ataque" (Ataque
  à Distância + Ataque Corpo-a-Corpo) as a category.

  Mirror `ArtesAprimorarComArteAbility`'s shape when writing one: name it
  `<Skill><AbilityName>Ability` in the same package as the `<Skill>CompetencyAbility` enum;
  mark the choice field `@NonNull` (reject a null choice at construction); delegate
  `getSkillType()`/`getDescription()` to the catalog constant so the rules text keeps a
  single source of truth; have each `@Modifier` method return 0 when the choice doesn't
  select its branch; keep the enum constant in the catalog (its count test is unchanged)
  with a comment redirecting to the class; and put the TODOs for branches blocked on
  missing systems on the *class*, next to where their modifier methods would go — not on
  the enum constant.

  **Not every branch of one of these abilities fits `@Modifier`**, though —
  `ArtesAprimorarComArteAbility`'s three branches split exactly on this line.
  `damageReduction()` (RDS for Esquiva e Aparar) is *unconditionally* active once chosen
  (RD/RA apply no matter which Perícia's roll is happening), so a no-arg `@Modifier` method
  works. `getBaseDamageBonus(SkillType attackingSkillType)` (Dano Base for a Perícia de
  Ataque) and `getCriticalMarginReduction(SkillType rolledSkillType)` (Margem Crítica Menor
  for any other Perícia) are each scoped to *one specific, dynamically-chosen* Perícia — the
  bonus should apply only when that particular Perícia is the one being rolled/attacked
  with, and a no-arg `@Modifier` method has no way to know which Perícia a given roll is for
  (`ModifierResolverImpl.invoke` calls `method.invoke(source)` with zero arguments,
  always). Model those as plain public instance methods taking the relevant `SkillType`
  explicitly instead, returning 0 when it doesn't match the stored choice — real, tested
  logic, just with no automatic caller yet, since no weapon/attack-damage entity or
  roll-resolution engine exists in this library to invoke them (and this library
  deliberately never rolls dice — see the `skill` package-info's "What this library
  computes" section). A future combat/roll-resolution layer calls these directly, passing
  whichever Perícia is actually being used. Don't force a branch shaped like this into
  `@Modifier`/`ModifierResolver` just for consistency with the first branch — the
  architecture genuinely doesn't support scoping to a dynamically-chosen skill, and an
  unconditional `@Modifier` version would incorrectly grant the bonus to every Perícia's
  roll, not just the chosen one.

- **When the choice is consumed by some *other* mechanism** (not a modifier on the ability
  itself) **and the choice space is genuinely open-ended** (not already enumerable from an
  existing fixed set), use `AcquiredChoice<C>`: it pairs the specific ability instance with
  the value chosen (`C` is that value's type, e.g. `SkillType`), and `Character.abilityChoices`
  holds them alongside (not instead of) the normal `attributeAbilities`/`skillCompetencyAbilities`
  lists — the ability itself is still granted the normal way; this is purely the extra "what
  did they pick" data. Look a choice back up via `AbilityChoiceService.getChoiceFor(character,
  ability)`. This only solves *persisting* the choice — the consuming mechanism (a
  `<Skill>Interaction` lookup, say) is still separate work; don't confuse "the choice can now
  be recorded" with "the ability now works." No ability in this codebase currently uses this
  pattern for real (see below) — it was built ahead of a first consumer, same as
  `ReactionsService`/`InitiativeService` once were.

- **When the choice is consumed by some *other* mechanism and the choice space is small and
  already fully known at compile time** (e.g. "pick a Perícia," and every `SkillType` already
  exists), skip `AcquiredChoice` entirely and model the choice as **one enum constant per
  legal option**, each already implementing the ability interface itself —
  `GnoseAbility.PERITO_TEORICO`'s reference: `org.aventyrs.core.ability.PeritoTeoricoAbility`
  has one `AttributeAbility` constant per `SkillType` (`PeritoTeoricoAbility.FURTIVIDADE`,
  etc.); granting a character the matching constant records both "they have Perito Teórico"
  and "which Perícia they chose" in a single object, with no separate persistence step and no
  `AbilityChoiceService` lookup at the consuming end at all — see "Unconditional Perícia
  base-Attribute substitution" below for how its own `resolveAttributeDomain` is consulted.
  The catalog enum constant (`GnoseAbility.PERITO_TEORICO` itself) stays in place as the
  rules-text entry, same "keep the constant, redirect via comment" convention as the
  instance-based pattern above. The tradeoff versus `AcquiredChoice`: more code up front (one
  constant per legal option, and a new one whenever the underlying set — here, `SkillType` —
  grows), traded for full compile-time enumerability/type safety and zero runtime
  choice-bookkeeping. Prefer this over `AcquiredChoice` whenever the choice is "pick one of a
  small, already-fixed set" rather than a genuinely open-ended value.

Don't build a validation service to check whether a choice is legal (e.g. that a chosen
Perícia is actually trained) — same restraint as the unenforced "Requer N Graduações"
prerequisites elsewhere in this codebase; just record what was picked.

## Unconditional Perícia base-Attribute substitution — `SkillCompetencyAbility.getSubstituteAttributeDomain()`

"Lets this Perícia use Attribute X instead of its normal base Attribute" is another common
TODO reason across ability enums (`AtaqueCorpoACorpoCompetencyAbility.ACUIDADE`,
`AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO`, `AtletismoCompetencyAbility.ACROBATA`,
`AttentionCompetencyAbility.ALMA_DE_SHERLOCK`'s substitution half, `DominioDoManaCompetencyAbility
.MAGIA_SELVAGEM`, `EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM`/`INSTINTO_ANIMAL`,
`FurtividadeCompetencyAbility.LADINO_TEORICO`, `PersuasaoCompetencyAbility.FORCA_OPRESSORA`),
and — for the *unconditional* case (always substitutes, no scoping to a specific
attack/delivery method) — it's now mechanically real, following `ACUIDADE` as the reference:

- `SkillCompetencyAbility` carries a `default Optional<AttributeDomain>
  getSubstituteAttributeDomain()` returning `Optional.empty()`, mirroring
  `getDifficultyReduction()`'s existing default-method-plus-override shape. Only override it
  on a constant whose rules text grants the substitution unconditionally, per-constant (an
  enum-constant body, exactly like `AtaqueCorpoACorpoExcellency.PRODIGIO`'s
  `getDifficultyReduction()` override) — never at the enum type level, which would force
  every other constant in that same enum to implement it too.
- `CharacterSkillService.getValueForRoll` has a second overload taking an extra
  `AttributeDomain substituteAttributeDomain` parameter — `null` means "use the Skill's own
  `getAttributeDomain()`, as before" (the original 3-arg overload just delegates to this one
  with `null`), non-null overrides it. The service itself never scans abilities — it only
  ever receives a resolved value.
- Resolving *which* Attribute (if any) applies is `SkillCompetencyAbility
  .resolveAttributeDomain(skillCompetencyAbilities, skillType, defaultDomain)`'s job — a
  static method on the interface, mirroring `SkillExcellency.unlockedBy`'s existing
  static-method-on-interface shape. It filters for entries whose `getSkillType()` matches
  `skillType` and whose `getSubstituteAttributeDomain()` is present, returning the first
  match's Attribute or `defaultDomain` if none — byte-for-byte identical at every call site
  (only `skillType`/`defaultDomain` vary), so it's a single shared method rather than
  duplicated. It's called unconditionally by `AbstractSkillInteraction.applyTo` for every
  skill now (see the next section) — safe even for skills with no substituting ability, since
  it just falls through to `defaultDomain` — and by `SkillGraduationService`'s max-Graduação
  cap for the same reason.
- This only covers the *unconditional* case. A substitution scoped to a specific circumstance
  (e.g. `AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO`, only for thrown-weapon/spell
  attacks) can't be modeled this way — same "this codebase doesn't track what a roll is *for*"
  simplification already documented for scoped Vantagem bonuses below; document that gap in a
  TODO on the constant instead. `GnoseAbility.PERITO_TEORICO` is also a different shape
  entirely: which Attribute to substitute is fixed (Gnose), but *which Perícia* it applies to
  is a per-character choice, not a fixed one enum-constant-to-enum-constant mapping like
  `ACUIDADE`'s — but unlike a truly open-ended choice, every legal option is already known at
  compile time (every `SkillType`), so it's wired via `org.aventyrs.core.ability
  .PeritoTeoricoAbility` — one `AttributeAbility` constant *per* `SkillType`, rather than the
  third pattern the previous section describes for an open-ended choice (`AcquiredChoice`).
  Granting a character `PeritoTeoricoAbility.FURTIVIDADE` both records that they acquired
  Perito Teórico *and* which Perícia they chose, in one object. Its own static
  `resolveAttributeDomain(Collection<AttributeAbility>, SkillType, AttributeDomain)` mirrors
  this section's own `SkillCompetencyAbility.resolveAttributeDomain` shape, and both
  `AbstractSkillInteraction.applyTo` and `SkillGraduationServiceImpl.getMaxGraduation` consult
  it first, feeding its result in as this section's own `resolveAttributeDomain`'s
  `defaultDomain` parameter — so a `SkillCompetencyAbility` substitution still wins if one
  somehow also targets the same Perícia, PERITO_TEORICO's Gnose applies otherwise, and the
  Perícia's own natural Attribute applies if neither does. Both call sites needed no
  constructor/DI changes for this — it's a pure function over `character.getAttributeAbilities()`,
  already in hand at both.
- Building this mechanism doesn't retroactively finish every ability that cites it — check
  each constant's own TODO. `ACUIDADE`, `ACROBATA`, `DISPARO_ARCANO`, and `MAGIA_SELVAGEM` are
  fully wired (enum override + `Interaction` filter + service overload) — see
  `AttributeSubstitutionFeatureTest` for an end-to-end test exercising all four on one
  Character at once, including a control Perícia (Persuasão) proving none of them leaks into
  an unrelated roll. `AttentionCompetencyAbility.ALMA_DE_SHERLOCK`'s substitution half,
  `EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM`/`INSTINTO_ANIMAL`,
  `FurtividadeCompetencyAbility.LADINO_TEORICO`, and `PersuasaoCompetencyAbility
  .FORCA_OPRESSORA` still need the same three-piece wiring applied to their own constant and
  `<Skill>Interaction` before they're real, even though the mechanism they were blocked on no
  longer needs to be invented.

## Vantagem is a flat +2 bonus, not a reroll mechanic

"Grants Vantagem on X rolls" is one of the most common TODO reasons across every ability
enum, but it isn't a d20-style "roll twice, take the higher" mechanic — in this game
**Vantagem is just a flat +2 bonus to that specific roll** (`Skill.ADVANTAGE_BONUS`). So an
ability that grants Vantagem on a Perícia roll is implemented exactly like any other roll
bonus: a `@Modifier(ModifierType.SKILL_ROLL_BONUS)` method on the concrete ability/excellency
returning `Skill.ADVANTAGE_BONUS`, summed into `skillRollBonus` inside the skill's
`<Skill>Interaction.applyTo` — see `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS` /
`DirigirECavalgarInteraction`. No separate flag or dice-rolling engine needed.

`AbstractSkillInteraction.applyTo` (see "Adding a new Perícia" above — every `<Skill>Interaction`
extends it, so this is no longer duplicated per skill) sums `ModifierType.SKILL_ROLL_BONUS`
across the same three sources `ReactionsService` uses for Reações — `attributeAbilities`,
`skillCompetencyAbilities`, and the trained skill's own unlocked `SkillExcellency` tiers — plus
a fourth, `CharacterSheet`-level one: `target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS)`
(see "Temporary bonuses from other Characters" below) — even before any ability actually
grants it for that specific skill, so future abilities work without touching any Interaction
at all.

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

## Consumer-facing documentation lives in `package-info.java`, and must stay current

This is a library other Java code (an API layer, or a game UI) imports directly — so
"how do I use this" docs go in `package-info.java` files next to the relevant package, not a
separate markdown guide that can silently drift out of sync. Two exist so far:

- `org.aventyrs.core.skill` — how to perform a Skill Roll via `Interaction`/`CharacterSheet`.
- `org.aventyrs.core.character.services` — the full ordered list of character-creation
  choices (Race → Attributes → Egos → conditional Vantagem de Autocontrole → ActionProfile →
  assemble via `Character.builder()`).

**Whenever a change adds a new creation-time choice** (a new Ego/Attribute-like allocation,
another permanent "pick one" enum like `ActionProfile`, a new conditional Vantagem like
Autocontrole's) **update the `character.services` package-info's numbered list and code
example in the same change** — don't leave it for later. The same applies to the `skill`
package-info if the Skill Roll protocol itself changes (new `InteractionResult` fields, a
different dispatch shape, etc.). A consumer coding against a stale list will silently miss
required or newly-available choices.

Before publishing a version other people can `javadoc`, note that `./gradlew javadoc`
currently fails on ~5 pre-existing errors — Lombok's generated `Builder` inner classes aren't
visible to the standalone javadoc tool without a delombok step. This is unrelated to the
package-info content itself (verified by running `javadoc` directly against just the relevant
sources); fixing it would need a delombok task (e.g. the `io.freefair.lombok` plugin).
