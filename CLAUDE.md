# Aventyrs Core — Project Conventions

Rules-engine core for the Aventyrs tabletop game. Pure Java library (Lombok + JUnit 5 + Gradle),
no framework dependencies — see `org.aventyrs.core.skill.Attention`/`Artes` and their
`Interaction`s for the reference implementation of everything below.

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
   `AttentionInteraction`, `ArtesInteraction`) — never leave a Skill without one. It must:
   - take an optional injected `CharacterSkillService` (default `new CharacterSkillServiceImpl()`),
     matching this codebase's constructor-injection convention;
   - look up the target's own `CharacterSkill` via `character.getSkills().get(SkillType.X)`,
     falling back to a fresh untrained `CharacterSkill` whose `SkillGraduation` carries
     `Skill.UNTRAINED_PENALTY` when the character never trained it;
   - delegate the actual bonus computation to `CharacterSkillService.getValueForRoll` — never
     recompute attribute totals or graduation math inline;
   - if the skill has a `<Skill>Excellency` enum (see below), also compute
     `SkillExcellency.totalDifficultyReduction(<Skill>Excellency.class, graduationValue)`;
   - **also** sum `character.getSkillCompetencyAbilities().stream()
     .mapToInt(SkillCompetencyAbility::getDifficultyReduction).sum()` into the same total —
     every `<Skill>Interaction` must honor both sources, even before any ability for that
     specific skill grants one (see `ArtesInteraction`/`AttentionInteraction`/
     `AtletismoInteraction`), then set the combined value on
     `InteractionResult.difficultyReduction`;
   - return an `InteractionResult` with `resultStatus` (the target's current status),
     `skillRollBonus` (the computed bonus), and `difficultyReduction` set.

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

## Character-level stats aggregated from abilities (e.g. Reações, Ações Livres, Pontos de Ação)

Some Character-level counters need a fixed base value *and* a fully-modified total summed
from abilities — this is what `ReactionsService`, `FreeActionsService`, and
`ActionPointsServiceImpl.getMaxActionPoints` all do (the latter scans the same three sources
to support things like `AtletismoExcellency.LENDA`'s +1PA). Don't compute the modified total
inside `Character` itself (it would need to instantiate `ModifierResolverImpl` directly, which
doesn't belong on a data class):

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

Mirror this shape for any new stat abilities/competencies/excellencies can modify, and
remember to give the new `ModifierType` constant a `@Modifier`-annotated method on whichever
concrete ability/excellency should affect it (e.g. `AttentionExcellency.FOCADO` for
`REACTIONS`).

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

Some abilities require the player to pick a value when they're acquired — a Perícia
(`GnoseAbility.PERITO_TEORICO`), or, for a future ability, one of several fixed effects.
There are **two** patterns for storing that choice, and which one applies depends on what
consumes it:

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

- **When the choice is consumed by some *other* mechanism** (not a modifier on the ability
  itself — e.g. `PERITO_TEORICO`'s attribute substitution, which a future
  `<Skill>Interaction` lookup would read), use `AcquiredChoice<C>`: it pairs the specific
  ability instance with the value chosen (`C` is that value's type, e.g. `SkillType`), and
  `Character.abilityChoices` holds them alongside (not instead of) the normal
  `attributeAbilities`/`skillCompetencyAbilities` lists — the ability itself is still granted
  the normal way; this is purely the extra "what did they pick" data. Look a choice back up
  via `AbilityChoiceService.getChoiceFor(character, ability)`. This only solves *persisting*
  the choice — `PERITO_TEORICO` still needs its substitution mechanism; don't confuse "the
  choice can now be recorded" with "the ability now works."

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
  match's Attribute or `defaultDomain` if none. This one *is* factored into a single shared
  method rather than inlined per `<Skill>Interaction` — unlike the `difficultyReduction`/
  `SKILL_ROLL_BONUS` sums (which have real per-site variation: different `ModifierType`
  constants, extra unlocked-Excellency lists), this resolution is byte-for-byte identical at
  every call site, and it now has a second, non-`Interaction` consumer too
  (`SkillGraduationService`'s max-Graduação cap, see the cap section above) — a second
  category of caller is exactly the point where this codebase's usual "duplicate over
  centralize" preference stops applying. Each substitution-aware `<Skill>Interaction` (e.g.
  `AtaqueCorpoACorpoInteraction`) calls it with its own `CharacterSkill.getSkill()
  .getAttributeDomain()` as `defaultDomain`, then passes the (always non-null) result into
  `getValueForRoll`'s 4-arg overload.
- This only covers the *unconditional* case. A substitution scoped to a specific circumstance
  (e.g. `AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO`, only for thrown-weapon/spell
  attacks) can't be modeled this way — same "this codebase doesn't track what a roll is *for*"
  simplification already documented for scoped Vantagem bonuses below; document that gap in a
  TODO on the constant instead. `GnoseAbility.PERITO_TEORICO` is also a different shape
  entirely: which Attribute to substitute is fixed (Gnose), but *which Perícia* it applies to
  is an acquisition-time choice (see the previous section's `AcquiredChoice`), not a fixed
  enum-constant-to-enum-constant mapping like `ACUIDADE`'s — so it needs its own
  `AbilityChoiceService`-driven mechanism, not this one.
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

Every `<Skill>Interaction.applyTo` should sum `ModifierType.SKILL_ROLL_BONUS` across the same
three sources `ReactionsService` uses for Reações — `attributeAbilities`,
`skillCompetencyAbilities`, and the trained skill's own unlocked `SkillExcellency` tiers —
even before any ability actually grants it for that specific skill, so future abilities work
without touching the Interaction again.

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
