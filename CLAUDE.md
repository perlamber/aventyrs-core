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
  parameter, mirroring `RestService.applyRest(Character, CharacterSheet, ...)`'s existing
  split: compute from the Character-side data, but only the `CharacterSheet` carries
  `unUsedExperience` to spend from (and, for Graduação, the `CharacterSkill` instance being
  mutated is looked up from `character.getSkills()`, not `characterSheet` — `Character`
  remains the single source of truth for skills/attributes/abilities). This genuinely needs
  both parameters, unlike `DamageService`'s own methods — see "Damage mitigation" below for
  why those instead take a `CharacterSheet` alone wherever one is available at all, deriving
  `Character` from it via `getCharacter()` rather than asking the caller to pass both.
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

See `org.aventyrs.core.skill.attention.Attention`/`org.aventyrs.core.skill.artes.Artes` and
their `Interaction`s for the reference implementation — every Perícia's classes live together
under their own subpackage of `org.aventyrs.core.skill` (e.g. `org.aventyrs.core.skill.artes`
holds every `Artes*` class), and a new one needs a `Skill` class, a `SkillType` constant (plus
matching `ModifierType`/`PeritoTeoricoAbility` entries), a `<Skill>Specialization` enum, a
`<Skill>CompetencyAbility` enum, a `<Skill>Interaction` extending `AbstractSkillInteraction`, a
`<Skill>Excellency` enum, fixture support, and tests. The `adding-a-pericia` Claude Code skill
packages the full checklist as an invokable walkthrough.

## Adding a new Título Aventyr — `org.aventyrs.core.title`

A Título Aventyr (e.g. `Santo`, the first one modeled) sits alongside `Race`/skills/ego
advantages as a top-level character concept, but a character holds **exactly three** Título
slots — `TitleSlot.PRIMARY`/`SECONDARY`/`TERTIARY` — unlike `Race`'s single `race` field, each
Título with its own catalog of Especializações and Habilidades/Supremas.

**Catalog vs. instance**: `org.aventyrs.core.title.AventyrTitle` is the per-character *held
instance* (the same "instance carries the acquisition-time choice" shape as
`org.aventyrs.core.ego.MoralHerdadaAbility`/`ArtesAprimorarComArteAbility`), **not** a
stateless-per-family class like `Race` — a Título's held specializations/abilities are
genuinely per-acquisition data, unlike every holder of a given Race getting identical racial
abilities. "Which Título family this is" is answered by which concrete class implements
`AventyrTitle` (e.g. `Santo`), deliberately **not** a separate identity enum — defer that
until a second Título creates real pressure for shared dispatch, mirroring
`SorteAdvantage`/`ACE`'s own precedent for the same restraint. **Whether a held instance is
the holder's Título Primário is not a method on `AventyrTitle` at all** — an earlier design had
a self-reported `isPrimaryTitle()` boolean (unenforced against holding more than one); the
three-slot shape on `Character` (`TitleSlot`, see below) makes "primary" a fact about *which
slot* holds the instance, not something the instance reports about itself, so at most one
Título can ever be primary by construction, not by an unvalidated invariant.

**Subpackage per Título**: a concrete Título's classes live together under
`org.aventyrs.core.title.<titlename>` (lowercase, no separators — e.g.
`org.aventyrs.core.title.santo`), mirroring `org.aventyrs.core.skill.<skillname>`'s
one-subpackage-per-catalog convention — only the shared framework interfaces
(`AventyrTitle`/`AventyrTitleSpecialization`/`AventyrTitleAbility`) stay directly in
`org.aventyrs.core.title`.

**Required pieces**, mirroring the Perícia checklist (see the `adding-a-pericia` skill):
1. The concrete `<Title>` class implementing `AventyrTitle` — a constructor taking chosen
   specializations/chosen abilities (no "am I primary" parameter — see below), plus the
   base-effect (e.g. Santo's "Despertar") description text. **If that base effect's own rules
   text has a "Se este for seu Título Primário, ..." clause, split it into its own string and
   override `getPrimaryTitleBonusDescription()`** (default `null` — not every Título's base
   effect has one) rather than leaving it concatenated onto `getBaseEffectDescription()`:
   "does the base effect apply at all" (unconditional, just by holding the Título) and "is
   this held as the Título Primário specifically" (`Character#getPrimaryTitle() == title`,
   resolved by the caller, not the instance — see below) are two different facts, and a caller
   rendering just the unconditional text shouldn't have to string-parse out the conditional
   part. See `Santo`'s own `BASE_EFFECT_DESCRIPTION`/`PRIMARY_TITLE_BONUS_DESCRIPTION` split.
2. A `<Title>Specialization` enum implementing `AventyrTitleSpecialization` — **every Título
   has exactly two Especializações**, no more, no less; a character may hold both, one, or
   neither (player's choice). The enum may still start with **fewer than two constants** if
   the rules text for one of them wasn't supplied yet — don't invent content; add the second
   constant once its real rules text exists, with no other file needing to change. Unlike a
   Perícia's own `SkillSpecialization` (purely descriptive), a Título's Especialização can
   carry a real activatable effect of its own — `getPDCost()`/`getActionPointCost()` (inherited
   from `AventyrTitleAbility`, see below) default to 0 for a purely descriptive one, overridden
   when the rules text gives it a Custo de Ativação (see `SantoSpecialization`, which fully
   models both of Santo's own — `ABENCOADO_PELA_LUZ` at 1PD/2PA, `ABRACADO_PELA_ESCURIDAO` at
   genuinely 0PD/0PA, its own cost being the entirely-PV-based Fúria dos Deuses instead — a
   real "no fixed PD/PA cost at all" case, not merely an unmodeled one).

**A Título trait with a real activation cost is an Active Ability, whether it's cataloged as
an Especialização or a Habilidade/Suprema**: `AventyrTitleSpecialization extends
AventyrTitleAbility` — not a sibling sharing a separate parent — so every `<Title>Specialization`
constant automatically satisfies `AventyrTitleAbility` too (`isSupreme()` defaults to `false`,
never applicable to an Especialização; every other member — `getDescription()`,
`getPDCost()`/`getActionPointCost()`, `isReactionActivation()`/`isFreeActionActivation()`,
`isPassive()`, `resolveAbsoluteDamageReduction()` — is shared as-is). `AventyrTitle
#getAllAbilities()` combines `getSpecializations()` and `getAbilities()` into one
`List<AventyrTitleAbility>` for a scanning service that needs every held Título trait
regardless of which catalog it's in — `DamageServiceImpl`'s own Título-ability RA scan uses
this (see "Damage mitigation" below), so a future Especialização overriding
`resolveAbsoluteDamageReduction` is picked up automatically, the same as a Habilidade/Suprema
already was. This same "gains a cost, becomes an Active Ability" rule extends to a Título's
own base effect too (`AventyrTitle` itself) if a future Título's base effect ever carries a
real activation cost — none does yet (Santo's own Despertar is explicitly "no activation cost
of its own"), so `AventyrTitle` doesn't implement `AventyrTitleAbility` today; don't add that
implementation speculatively until a real Título's base effect actually needs it.

**`AventyrTitleAbility#getInteractionClass()`** (`Optional<Class<? extends Interaction>>`, one
per constant, no default — every implementor must answer it explicitly) is the declared bond
between a constant and whichever `<X>Interaction` actually activates it, so the two can never
silently drift apart the way a purely-in-comment cross-reference could. `Optional.of(X
.class)` once a real `<X>Interaction` exists for that constant (e.g. `SantoSpecialization
.ABENCOADO_PELA_LUZ` → `Optional.of(AbencoadoPelaLuzInteraction.class)`, `AbencoadoPelaLuzAbility
.GRITO_DE_GUERRA_VULCANO` → `Optional.of(GritoDeGuerraVulcanoInteraction.class)`);
`Optional.empty()` for every constant still fully TODO'd. Set on every constant, not just the
ones with a real Interaction — mirrors this codebase's "no default that could silently miss a
new case" precedent, same as `PeritoTeoricoAbility`'s own per-`SkillType` enumeration. This is
currently a **declared** bond only — nothing dispatches an activation via reflection off
`getInteractionClass()` yet; `Santo#activateAbencoadoPelaLuz`/`#activateGritoDeGuerraVulcano`
still construct their own `<X>Interaction` directly, same as before. Don't build a generic
reflective-dispatch mechanism off this field speculatively — it exists to make the association
machine-checkable (a test can assert the right class is named) and legible to a reader, not
because a caller needs to look it up dynamically yet.
3. A `<Title>Ability` enum implementing `AventyrTitleAbility`, one constant per Habilidade/
   Suprema whose prerequisite names the Título generically ("1 Especialização", not naming
   either one specifically) — `isSupreme()` distinguishes the two tiers (Suprema is limited to
   one per Título+Especializações combination held, except where a specific ability grants
   more — unenforced, see below). **A Habilidade/Suprema whose own prerequisite names one
   specific Especialização** (e.g. "Requer Especialização 'Abençoado pela Luz'") belongs in a
   **separate** `<Specialization>Ability` enum instead (e.g. `AbencoadoPelaLuzAbility`), living
   alongside `<Title>Ability` in the same subpackage — keeps which abilities are gated on which
   specific Especialização legible as more of them accumulate, rather than folding everything
   into one flat `<Title>Ability` enum regardless of which prerequisite it actually carries.
   **Because a gated `<Specialization>Ability` constant is held by the same character as the
   Título itself, the concrete `<Title>` class's own held-abilities field/constructor parameter
   must be typed `List<AventyrTitleAbility>`, not `List<<Title>Ability>`** — the narrower type
   compiles, since every `<Title>Ability`/`<Specialization>Ability` enum implements the shared
   interface, but a `<Specialization>Ability` constant is a genuinely different Java enum than
   `<Title>Ability`, so `List<<Title>Ability>` can never actually hold one; `Santo`'s own
   `abilities` field was originally typed `List<SantoAbility>` (a latent gap between this stated
   intent and the actual code — no test ever constructed a `Santo` holding a gated ability, so
   nothing caught it) and was widened to `List<AventyrTitleAbility>` once `Santo
   #activateGritoDeGuerraVulcano` needed to validate one was actually held. Get this right from
   the start on a new Título's own class rather than repeating the gap.

**Passive vs. active**: `AventyrTitleAbility#isPassive()` is a *derived* default method, not a
stored field — `getActionPointCost() == 0 && !isReactionActivation() && !isFreeActionActivation()`
— inherited by `AventyrTitleSpecialization` too, since that interface now extends
`AventyrTitleAbility`. A Reação or Ação Livre activation still counts as active even though its
own `actionPointCost` is 0 (both are explicit player triggers, not "always on") — only a
genuine "Custo de Ativação: Nenhum, habilidade passiva" constant (e.g. `SantoAbility
.BASTIAO_DOS_NECESSITADOS`/`PROTETOR_DA_VIDA_E_DA_MORTE`) should end up `isPassive() == true`.
No `PDCost` check is needed in the formula — no constant modeled so far combines a real PD
cost with zero PA/Reação/Ação Livre, but if one ever does, revisit whether it's actually
passive before assuming the existing formula still holds. **The derived formula can't
distinguish "genuinely no cost" from "0 PD/PA but a real cost expressed some other way"** —
`SantoSpecialization.ABRACADO_PELA_ESCURIDAO` is the reference example: its Custo de Ativação
is entirely PV-based (Fúria dos Deuses' 3-or-4-PV spend), not PD/PA at all, so the derived
formula would wrongly call it passive; it overrides `isPassive()` to return `false` explicitly
instead, with a comment explaining why the derived formula doesn't apply. Follow that pattern
for any future Título trait whose real cost isn't expressible through PD/PA/Reação/Ação Livre.

`isPassive()` itself still has no scanning-service consumer (the natural future one is passive
Título abilities joining `AbstractSkillInteraction`'s existing `attributeAbilities`/
`skillCompetencyAbilities`/unlocked-`SkillExcellency` scan for `SKILL_ROLL_BONUS`-style
bonuses, mirroring how `SkillCompetencyAbility.allFor` already folds racial abilities into
that same scan — not built yet, don't guess at it speculatively) — but `AventyrTitle
#getAllAbilities()` (see above) *is* a real, live consumer of the broader "Especialização is
also an Active Ability" rule: `DamageServiceImpl`'s Título-ability RA scan reads it today, so
this isn't purely speculative infrastructure the way `isPassive()`'s own consumer still is.

**"Requer N Especializações/Habilidades" prerequisites are real, enforced data** — the one
exception, across this whole codebase, to the "no eligibility validation service" restraint
`SkillCompetencyAbility`'s own acquisition prerequisites still follow (see "Adding a new
Perícia" above; every other "Requer N Graduações"-style clause elsewhere remains an unenforced
comment). `AventyrTitleAbility#getRequiredSpecializations()`/`#getRequiredSpecialization()`/
`#getRequiredOtherAbilities()` carry a constant's own numbers — the first pair for the
Especialização half (a bare count for "Requer N Especializações" generically, e.g.
`SantoAbility`'s own "1 Especialização" not caring which of the Título's two; or one specific
named Especialização, e.g. every `AbencoadoPelaLuzAbility` constant naming
`SantoSpecialization.ABENCOADO_PELA_LUZ` — a constant sets one or the other, never both), the
third for "N outras Habilidades," scoped to sibling constants of that same concrete
catalog only (`Enum#getDeclaringClass()`, not plain `getClass()` — a constant-specific class
body, e.g. `SantoAbility#BASTIAO_DOS_NECESSITADOS`'s own anonymous override, would otherwise
report a different runtime `Class` than a body-less sibling constant of the identical enum).
`#isEligible(AventyrTitle)` combines both halves; `TitleAbilityService#grantTitleAbility`
checks it before granting any ability to a held Título, throwing
`TITLE_ABILITY_PREREQUISITE_NOT_MET` if unmet. The Suprema-per-combination cap itself is a
softer case: `TitleAbilityService#getAvailableSupremaSlots` reports how many more a Título may
receive right now (the normal 1, plus one more while `InstinctAbility#CENTELHA_SUPERIOR`'s own
one-time extra grant is unspent) and `grantTitleAbility` enforces it on that one entry point —
but directly constructing an `AventyrTitle` with more Supremas than that (bypassing the
service entirely, same as every other builder-bypassable invariant in this codebase) is still
unchecked, so the cap isn't enforced *everywhere* the way the Especialização/other-Habilidades
prerequisite now is.

**TODO discipline**: same shape as `ArtesCompetencyAbility`'s reference example — what the
ability is supposed to do, and which specific system is missing. Check this codebase's
existing gap catalog before assuming a new gap: **Defesas** (no stat/service exists anywhere —
cite `race/Gigantes.java`'s "DF isn't a concept this core computes at all" / `race/Elfo.java`'s
matching DM citation), **an owned/produced item copy** (the `org.aventyrs.core.item.Item`
*catalog* entry is real — see "Itens/Equipamento" below — but per-copy state, inventory, a PE
economy, Obra-Prima/Aprimoramentos and a production/repair mechanic are all still missing, so
cite the specific one rather than a blanket "no Item entity"), **Encantamento/Maldição/Doença (Malefício) classification** (no such tag exists
anywhere — see `Withering`'s own citation and `AtaqueCorpoACorpoCompetencyAbility
.ABRIR_DEFESAS`'s "Malefício Desprevenido" one), **Área de Efeito** (cited but unbuilt — see
`EsquivaEApararCompetencyAbility.EVASAO`'s own TODO), **forced-attack-targeting/attack-interception** (same gap
`SantoAbility.GUARDA_VIDAS` already cites — this core has no equivalent of "another Character
becomes the target instead" mid-resolution), and **cross-character continuously-recomputed
passive grants** (every existing cross-character bonus mechanism is either an explicit
roll-time/activation `Blessing` grant like `DOM_BARDICO`/`GRITO_DE_GUERRA_VULCANO` or an
initiative-win-triggered one — nothing supports "my always-on passive continuously grants a
bonus to a nearby Character with no trigger event").

**"Can't apply it yet" doesn't mean "can't compute it yet"** — a base effect's own scaling
formula is real, tested data even when the stat it scales is itself entirely TODO'd. `Santo
#getDefesasBonus(SceneContext)`/`#getPrimaryTitleAllyDefesasBonus(SceneContext)` are the
reference example: Despertar's Defesas bonus (self, and its Título-Primário half-share to
adjacent allies) can't be *applied* to anything since Defesas doesn't exist as a stat, but the
*arithmetic* — base value + adjacent-ally count (via `SceneContext#countAlliesWithin`) +
`getSpecializationAndSupremaCount()`, then integer-halved for the Primário share — needs no
missing system to compute, so it's implemented for real, mirroring `getIgnoreCriticalEffectDurationInRounds()`'s
identical reasoning for Despertar's crit-ignore duration. Don't let "the consumer doesn't
exist" block "the formula is expressible" — TODO the *application*, not the *arithmetic*, and
say so explicitly in the TODO (see `Santo`'s own three-part TODO comment for the shape: one
line per genuinely separate missing piece, not one blanket "unimplemented").

**Activating a Título ability/Especialização with a direct, single-target effect reuses
`Interaction<CharacterSheet>`, mirroring `<Skill>Interaction` minus anything roll-specific.**
`SantoSpecialization.ABENCOADO_PELA_LUZ`'s own touch-heal-or-cure effect is the reference
example: holding the constant (and its real `resolveShortRestHealAmount` formula) gave no way
for a caller to actually *trigger* it — `AbencoadoPelaLuzInteraction implements
Interaction<CharacterSheet>` is the fix, with the exact same cascading-overload shape
`AbstractSkillInteraction` established (a safe-no-op 1-arg `applyTo(CharacterSheet)`, a longer
overload holding the real logic, `SceneContext` accepted even when this specific ability's own
rules text doesn't condition on it — for consistency with the established shape and so a
future ability that *does* need one doesn't need a differently-shaped entry point) but with no
`SkillRoll`/`skillRollBonus`/dice at all, since this isn't a Perícia test. `InteractionResult`
gained a `resourceGainValue`/`resourceGainType` pair for this — the exact mirror of the
existing `resourceLossValue`/`resourceLossType`, since no Interaction had ever reported a
*restored* resource before (only drained ones, e.g. `DamageInteraction`); reusing
`resourceLossValue` with a negative number was considered and rejected, since that field is
documented as what an Interaction "drained," and a negative "drained" amount would misrepresent
that rather than just adding the missing, symmetric field.

The **validation and dispatch live on `Santo` itself** (`Santo#activateAbencoadoPelaLuz`), not
on a free-standing call directly against the bare `SantoSpecialization` constant: `Santo`
mirrors `AbstractSkillInteraction#validateRequestedTrait`'s own "must actually be held" check
(throwing `IllegalOperationException`/`REQUIRED_TITLE_TRAIT_NOT_HELD` — a new message key,
mirroring `REQUIRED_SKILL_TRAIT_NOT_HELD`'s shape — rather than reusing that Perícia-specific
one) before delegating to the Interaction — because the *Título instance* is what actually
knows which Especializações/Habilidades it holds (the same data `getDefesasBonus`/
`getIgnoreCriticalEffectDurationInRounds` already need), and is the natural place to resolve
"is this held at all" before an activation proceeds. This is also where a *future* ability
needing "is this the holder's Título Primário" would resolve that fact, if one ever needs it —
Abençoado pela Luz's own effect doesn't, so no such parameter exists on this one method; a
caller resolves `Character#getPrimaryTitle() == titleInstance` externally and would pass it in
explicitly on whichever future method actually needs it, the same way `Santo` never stores or
self-derives that fact today (see "Whether a held instance is the holder's Título Primário"
in `AventyrTitle`'s own javadoc).

**One `<X>Interaction` per Active Ability, built the moment — and only the moment — at least
one clause of that ability's own effect is mechanically real and wireable with existing
classes/methods.** This is now a formalized, general rule, not just `AbencoadoPelaLuzInteraction`'s
own one-off shape: `GritoDeGuerraVulcanoInteraction` is the second real case, and it
deliberately does **not** reuse `AbencoadoPelaLuzInteraction`'s single-touched-target-direct-
mutation shape, exactly per this section's own earlier warning to check whether the target
shape actually matches before reusing a pattern — Grito's own rules text grants to "você e
seus aliados adjacentes," so it follows `ArtesCompetencyAbility#DOM_BARDICO`'s own
report-rather-than-mutate shape instead (see "Temporary bonuses from other Characters"
below for `Blessing`'s full shape): `applyTo(CharacterSheet actor, SceneContext sceneContext)`
returns an `InteractionResult` whose `blessings` list holds every bonus this activation grants
— it never calls `CharacterSheet#grantTemporaryBonus` itself, and never even needs
`sceneContext` to compute anything (a caller resolves recipients separately, via `SceneContext
.getAlliesWithin(Range.ADJACENTE)` for each `TargetScope.SELF_AND_ALLIES` entry, once it
actually applies the reported blessings). **"Mechanically real" is judged per clause, not per
ability, but "real" now means "expressible as a grantable `Blessing`," not "has an actual
consumer"** — Grito's "+2 em Defesas" half is reported for real (a `Blessing` typed
`ModifierType.DEFESAS`) even though nothing reads that `ModifierType` yet, since no Defesas
stat/service exists in this core; the "can't apply it yet doesn't mean can't compute it yet"
discipline documented further below now extends to *granting*, not just arithmetic — see
`GritoDeGuerraVulcanoInteraction`'s own class javadoc, and `ModifierType#ACTION_POINTS`'s
identical already-grantable-but-inert precedent for the general shape. Don't wait for an
ability's *entire* rules text to become expressible as a `Blessing` before building its
`<X>Interaction`/`activate<X>` pair, though — a clause that can't even be expressed as a
`Blessing` yet (e.g. one needing a mechanism this core has no `ModifierType`/`TemporaryBonus`
equivalent for at all) still gets a TODO directly on the Interaction class instead. **Passive
abilities never get one** (`isPassive() == true` — no activation exists to model; they're
reached via continuous scanning instead, e.g. `DamageServiceImpl`'s Título-ability RA scan).
The entry point is always a validating method on the concrete `AventyrTitle` class
(`Santo#activate<X>`), never a static/standalone call against the bare catalog constant,
mirroring `activateAbencoadoPelaLuz`'s own shape — this method itself doesn't apply anything
either for a report-only Interaction like Grito's; it just delegates and returns the
`InteractionResult`, the same "compute what, caller applies who" split every other
`blessings`-reporting Interaction already follows. Extract a shared base class once a *third*
real activation needs an identical cascade to one already built — still deferred for now,
mirroring `AbstractSkillInteraction`'s own extraction history (only two real shapes exist
today, and they're genuinely different: one touched target with direct mutation, vs. a
report-only list of `Blessing`s left for the caller to apply).

**Resolving "self + aliados adjacentes/em Distância X" turned out not to need a new
orchestration mechanism at all — `SceneContext#getAlliesWithin(Range)` already returns the
real, already-resolved `CharacterSheet` list a caller needs, whenever it actually applies a
reported `Blessing`.** This is what makes `GritoDeGuerraVulcanoInteraction`'s Vantagem clauses
real: `Skill.ADVANTAGE_BONUS` and the per-skill `ModifierType
.ATAQUE_A_DISTANCIA_ROLL_BONUS`/`ATAQUE_CORPO_A_CORPO_ROLL_BONUS` constants were already real
and already summed by `AbstractSkillInteraction` via `CharacterSheet#getTemporaryBonus` — the
only missing piece was ever *who* to call `grantTemporaryBonus` on, and that's exactly what
`SceneContext.getAlliesWithin` resolves for a caller applying a `TargetScope.SELF_AND_ALLIES`
blessing, the same way `Scene.getAllies` already resolves it for a caller applying one of
DOM_BARDICO's own `TargetScope.ALLIES` ones. **This doesn't generalize to every Título
ability's own grant clause, though** — check what a `ModifierType` in question is actually
*consulted by* before assuming a `Blessing` of that type will do anything once granted:
confirmed `DamageServiceImpl#getTotalAbsoluteDamageReduction` never reads `CharacterSheet
#getTemporaryBonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)` at all — RA is only ever summed
from the reflection-based ability scan and `AventyrTitleAbility#resolveAbsoluteDamageReduction`,
both continuously-scanned passive-style hooks with no "grant a bonus lasting N Rodadas after
this activation" shape — so `GLORIA_RELAMPEJANTE_DE_TESLA`'s own RA half remains genuinely
un-grantable-for-real even though its recipient-resolution technique (self + aliados em
Distância Curta) is now demonstrated and reusable, unlike Grito's own Defesas half (which
*can* be expressed as a `ModifierType.DEFESAS`-typed `Blessing`, even though nothing consumes
it yet — the difference is that `ModifierType.DEFESAS` is a real registry entry with no reader
yet, while RA's own grant path has no `TemporaryBonus`-based entry point at all to grant
through in the first place).

**`ActionPointsServiceImpl` never reads a temporary PA grant.** A "+1PA for N Rodadas"-style
clause (e.g. `GLORIA_RELAMPEJANTE_DE_TESLA`) can't be expressed as a real, eventually-consumable
`Blessing` the way Grito's own Defesas one can, independent of the recipient-resolution
technique demonstrated above: `ActionPointsServiceImpl#getMaxActionPoints` only ever reads
`Character#getTemporaryActionPointsBonus()` (a plain, non-Round-scoped int field mutated
directly via `.toBuilder()`) for its "temporary" component — it never calls `CharacterSheet
#getTemporaryBonus(ModifierType.ACTION_POINTS)` at all, so a granted `TemporaryBonus` of that
type would be silently inert. PA's own temporary-bonus pathway and this core's general
`TemporaryBonus`/`ModifierType` machinery haven't been connected — the same class of gap as
RA's above (a `ModifierType` whose value nothing actually consults yet), cited separately
because it's PA-specific, not shared with RA's own reason.

**`AbracadoPelaEscuridaoAbility`'s own gaps** (Santo's second Especialização) surfaced several
more, worth citing precisely rather than re-deriving next time: **no temporary/Round-scoped
Attribute bonus mechanism** (unlike every other stat this core tracks, `AttributeValue` only
has `base`/`racialBonus`/`variable` — all permanent, never summed via `ModifierType`/
`CharacterSheet#getTemporaryBonus` the way Reações/PA/RD/RA/skill rolls all are); **no
within-Turn activation-count tracker** (for a "this Habilidade's effect changes if activated
twice in the same Turn" clause — `CharacterSheet` tracks Round-scoped `TemporaryEffect`s, not
a same-Turn activation counter); **no reactive/retaliation-damage mechanism** (`DamageService`
only ever computes damage *to* a target *from* an attacker, never the reverse — "attacking a
protected Character damages the attacker back" has no equivalent); **no forced-movement/
positioning mechanism** ("empurrado 1UD... Reposicionar" — this core "never does geometry,"
per `Range`'s own javadoc, the same restraint that kept it from ever modeling knockback);
**Roubo de Mana/Roubo de Determinação don't exist** (only Roubo de Vida does, via
`LifeStealService` — see `FocusAbility`'s own "needs a Roubo de Mana effect to exist in the
first place" citation); **no "modify this one specific delivered attack" transaction** (a
bonus scoped to "este ataque," the one delivered as part of activating a *different* ability,
doesn't fit any existing per-roll `resolve*` hook — those all compute bonuses for *any* roll of
a skill type, generically, not one specific attack tied to another ability's activation); and
**"spend a resource for a one-time roll effect" (e.g. a GD reduction)** is the identical gap
`race/Orc.java`'s own Agnação Ancestral citation already flags — Pontos de Vida (or PM) spent
to modify a single roll's outcome, as opposed to casting a Magia, has no equivalent transaction
anywhere in this core.

**`Character` holds exactly three Título slots, not a list**: `primaryTitle`/`secondaryTitle`/
`tertiaryTitle` are plain nullable `AventyrTitle` fields (same "nullable, no default" shape as
`sexo`/`deity`) — an earlier design used a single `@Builder.Default List<AventyrTitle> titles`
field instead, mutated via `grantTitle(AventyrTitle)`, with each held instance self-reporting
`isPrimaryTitle()`; that's been replaced by the three-slot shape (see "Catalog vs. instance"
above for why). `TitleSlot` (`org.aventyrs.core.character`, alongside `Character` — mirrors
`EgoDomain`'s own placement: the key type lives with `Character`, the value type — `AventyrTitle`
— lives in its own domain package) is the enum naming which slot. Acquiring a Título costs no
XP and needs no `CharacterSheet`, so — unlike `CharacterAttributeService#upgradeBase`/
`SkillGraduationService#upgradeGraduation` — there's no reason to route it through a dedicated
service either: `Character#grantTitle(AventyrTitle, TitleSlot)` sets the named field directly,
overwriting whatever previously occupied that slot, mirroring `CharacterSkill#increaseGraduation`'s
own plain-mutator precedent. `Character#getAllTitles()` is the derived list (Primário first,
empty slots omitted) a scanning service needs to inspect every held Título regardless of slot
— see `DamageServiceImpl`'s own Título-ability scan below. `CharacterFixture` sets all three
fields to `null` in its Rule blocks; unlike the old list-based field, there's no cross-`gimme()`-call
sharing trap to document here — `null` carries nothing to leak between Characters built from
the same template.

**Extend a scanning service only when an ability's condition needs data no no-arg `@Modifier`
method can see** — follow `SantoAbility.BASTIAO_DOS_NECESSITADOS`/`DamageServiceImpl`'s
explicit (non-reflection) scan shape (see "Damage mitigation"'s own
`RA/half-damage conditioned on SceneContext` subsection) for the reference. Otherwise, a plain
unconditional bonus should just be an ordinary `@Modifier` method — `Modifier`'s own javadoc
already anticipates this ("an ability, a feat, a title, an item").

**Tests**: one file per new type (description/count/identity, mirroring the Perícia
checklist's item 8 above), a `<Title>Test` for the concrete class, an integration test
granting it to a `CharacterFixture` character, and scanning-service test additions for any
ability wired for real.

**Keep `org.aventyrs.core.title/package-info.java` current** whenever the granting API
(`Character#grantTitle`/`#getPrimaryTitle`/`#getAllTitles`) changes shape — same discipline as
`character.services`' own package-info (see "Consumer-facing documentation" below). The
`adding-a-title` Claude Code skill packages this same checklist as an invokable walkthrough.

## Adding a new Talento (Feat) — `org.aventyrs.core.feat`

A Talento (e.g. `ArtesMarciaisFeat.ARTISTA_MARCIAL`) is a flat catalog entry, one enum per
Talento *tree* named by `FeatCategory` (e.g. `ArtesMarciaisFeat` for `FeatCategory
.ARTE_MARCIAL`) — mirrors `<Skill>CompetencyAbility`'s one-enum-per-domain shape, not
`AventyrTitle`'s per-instance-class one, since a Talento (unlike a Título) never carries
per-acquisition player choices of its own today. `Feat`/`FeatRequirements`/`AbstractFeat`/
`FeatCategory` and `FeatService#grantFeat(Character, CharacterSheet, Feat)` (validate
`Feat#isEligible`, spend `Race#getNewFeatCost` XP, then mutate — the same shape as
`CharacterAttributeService#upgradeBase`/`TitleAbilityService#grantTitleAbility`) are the whole
mechanism; the `adding-a-feat` Claude Code skill packages the full checklist as an invokable
walkthrough.

Two facts worth knowing outside that checklist:

- **A Talento's prerequisites are real, enforced data** — the second exception (alongside
  `AventyrTitleAbility`'s "Requer N Especializações/Habilidades") to this codebase's usual
  "leave 'Requer N Graduações' as an unenforced comment" restraint, because a Talento's own
  Pré-requisito is always a simple numeric/identity threshold `FeatRequirements` can model
  directly.
- **`Character#feats` is a real *mutable* `List<Feat>`** — unlike `skillCompetencyAbilities`/
  `attributeAbilities`'s `@Singular`, fixed-at-creation-only shape, since a Talento is acquired
  well after a character exists; `Character#grantFeat(Feat)` is a plain mutator (like
  `#grantTitle`) that `FeatService` calls after spending XP. It defaults to a fresh `new
  ArrayList<>()` per `Character.builder().build()` call, but `CharacterFixture` defaults it to
  the same immutable `List.of()` every other trait list there uses — a test granting a Feat
  onto a fixture-built Character must first swap in a mutable list via `.toBuilder().feats(new
  ArrayList<>()).build()`.

## Itens/Equipamento — `org.aventyrs.core.item`

An `Item` is the **catalog entry** for a piece of Equipamento — what "an Armadura Completa" is,
the same way `Feat` describes a Talento — carrying every column an item's rules-text block
lists: its heading (`ItemWeightClass`/`ItemRarity`, the "(Pesado/Raro)" pair), `description`,
`price` (in **Pontos de Equipamento**, the PE `ResourcesAdvantage#BARGANHISTA` already cites),
`physicalDefenseBonus`/`magicDefenseBonus` (DF/DM), `hardness` (Dureza), `castingBonus`
(Conjuração), and an `ItemFavor`.

- **Catalog, not owned copy** — the same split `AventyrTitle`'s javadoc documents, resolved the
  *other* way than a Título's: an item's stats are identical for every copy, so the enum
  constant *is* the item. Per-copy state (Dureza actually remaining, Obra-Prima tier,
  Aprimoramentos, who produced it) is deliberately unmodeled, and would be a separate
  held-instance type wrapping a catalog entry. Nothing on `Character`/`CharacterSheet` holds an
  `Item` yet either — that inventory belongs with the per-copy type, not this one. Don't build
  it speculatively; several TODOs cite it (`ProfissaoCompetencyAbility`,
  `ResourcesAdvantage#HERANCA_FAMILIAR`), but none is unblocked by the catalog alone.
- **One enum per `ItemCategory`** (e.g. `ArmorItem` for `ItemCategory.ARMOR`), mirroring
  `<Skill>CompetencyAbility`/`ArtesMarciaisFeat`'s one-enum-per-domain shape;
  `AbstractItem` (`@Builder`) is the `AbstractFeat` equivalent for a one-off or
  caller-supplied item that doesn't belong in a catalog enum.
- **`ItemFavor` is the conditional half, and its bonuses are real data, not prose**: a Favor
  grants a bonus of any sort, exactly like a `SkillCompetencyAbility` does, so it carries a
  list of `ItemBonus` (a `ModifierType` + value pair) resolved via `ItemFavor#resolveBonus
  (ModifierType, Character)` / `Item#resolveFavorBonus(...)` — 0 unless the `ItemRequirements`
  (an `AttributeDomain` + value, e.g. "Força 3") are met. `ARMADURA_COMPLETA`'s "Dano de Corte
  sofrido é reduzido em -2" is `DAMAGE_REDUCTION` 2, landing on the RD `DamageService
  #getTotalDamageReduction` already sums for real. The rules text stays on `getDescription()`
  alongside it, same single-source-of-truth convention every ability enum follows.
  - It's **data, not `@Modifier` methods**, unlike every ability enum, and that's forced:
    `@Modifier`'s `ModifierType` is a compile-time-fixed annotation value, so one shared
    `ItemFavor` class can't vary which type a given item grants — the same limitation "A
    ModifierType per skill" documents. Don't try to route items through `ModifierResolver`.
  - `ItemBonus` is deliberately **not** `TemporaryBonus` or `Blessing`: an item's Favor lasts
    as long as the item is carried (no Rodada countdown) and never reaches anyone but its
    wielder (no `TargetScope`, no granting `source`), so all three of those fields would be
    dead weight.
  - A Favor clause with no `ModifierType` to express it yet contributes no `ItemBonus` and
    lives on in `getDescription()` until its mechanism exists. `ARMADURA_COMPLETA`'s own
    "de Corte" scoping is exactly that kind of simplification — no damage-type-scoped
    mitigation exists (`DamageType` has no Corte/Perfuração/Impacto breakdown, and RD/RA are
    resolved with no notion of damage type), so it's modeled as plain RD, documented on the
    constant rather than silently narrowed or over-granted. So is a clause whose *shape*
    `ItemBonus` can't hold even though a `ModifierType` for the stat exists —
    `ARMADURA_DE_JUSTA`'s "Movimento Base reduzido à metade" is a halving, and
    `MovementService` sums `MOVEMENT` additively with no multiplicative stage for one to feed
    (unlike `DamageService`'s own real `HALF_DAMAGE` stage); don't add a `MOVEMENT_HALVED`
    constant for it, since the missing piece is the mechanism, not just a reader.
  - The optional "Efeitos Adicionais" line is granted by the *same* requirement, not
    independently, so it lives here rather than on `Item`; `null` for an item with none, checked
    via `hasAdditionalEffects()`. It stays free text — what one does varies too widely per item
    to have a shared shape yet.
  - Everything else on `Item` applies to anyone carrying it; everything on `ItemFavor` needs
    `Item#grantsFavorTo(Character)` to hold first.
- **`ItemRequirements` checks `getTotal()`, not `getBase()`** — deliberately unlike
  `FeatRequirements`, which uses `base`: acquiring a Talento is gated on what the character
  personally invested in, but whether an item's Favor applies is a "can I meet this right now"
  question, so a Bônus Racial or a variable bonus counts. It's a narrower record than
  `FeatRequirements` (no `requiredSkillType`/`requiredFeat`) rather than a reuse of it — widen
  it only if a real item ever names a Perícia/Talento/Título.
- **`castingBonus` is a plain number, and "Desvantagem" is `Skill.DISADVANTAGE_MALUS` (-2)** —
  the symmetric counterpart to `Skill.ADVANTAGE_BONUS` this codebase's gap catalog used to
  list as missing. `ArmorItem.ARMADURA_COMPLETA`'s Conjuração column is the first
  real, unconditional Desvantagem in the ruleset and what motivated adding it. The *scoped*
  Desvantagem clauses elsewhere (`race/Bestial.java`'s Inocência Selvagem,
  `AbencoadoPelaLuzAbility`) are still blocked on their own separate gaps — this core doesn't
  track what a roll is *for* — not on the constant.
- **What's missing for a Favor is only the inventory**: no `Character`/`CharacterSheet` holds
  an `Item`, so no scanning service reaches one on its own — `DamageServiceImpl`'s RD scan
  covers `attributeAbilities`/`skillCompetencyAbilities`/excellencies and would need equipped
  items added as a fourth source. Until then a caller holding the item asks it directly. The
  *bonus* itself is already real; don't re-cite it as unexpressible.
- **None of the numeric *columns* has a consumer yet** (DF/DM/Dureza/Conjuração/Preço, as
  opposed to the Favor above), and each is blocked on a *different*
  missing system — PE has no budget/economy, DF/DM have no Defesas stat (`ModifierType.DEFESAS`
  is a real registry entry nothing reads), Dureza has no damage/repair mechanic, Conjuração has
  no item-granted hook on either of `SpellCastingService`'s two rolls. The values are real,
  exact data all the same, per the "can't apply it yet doesn't mean can't compute it yet"
  discipline.
- `ItemInteraction` is untouched by this — still the bare pre-existing "TODO implement" stub,
  since nothing yet *uses* an item as an `Interaction`.

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
mapping this core needs (see the "Adding a new Perícia" section/`adding-a-pericia` skill), so this reuses it rather than
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
  constant to `ModifierType` too** (see the "Adding a new Perícia" section/`adding-a-pericia`
  skill) — there's no default;
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
- `InteractionResult.blessings` (a `List<Blessing>`) is where a roll/activation that *grants*
  one or more of these (as opposed to merely being affected by an already-granted one) is
  expected to report them. `Blessing` (`org.aventyrs.core.sheet`) is a small value object
  pairing a `ModifierType`, an `int value`, an `int rounds`, and a `TargetScope scope` —
  exactly the "what" (value/type/duration) and "who kind" (scope) a caller needs, with nothing
  else. `modifierType` is a `ModifierType`, not a `SkillType` — deliberately matching
  `TemporaryBonus`'s own field, so it's exactly what a caller passes straight into
  `CharacterSheet#grantTemporaryBonus` with no extra `SkillType`→`ModifierType` mapping step
  (the broad `SKILL_ROLL_BONUS`, or one specific skill's own `rollBonusType`). `scope` is a
  `TargetScope` (`SINGLE_TARGET`/`ALLIES`/`ENEMIES`/`SELF`/`SELF_AND_ALLIES` — the last two
  added specifically for `Blessing`, see below) — `ALLIES` for DOM_BARDICO (excludes the
  caster: "a eles, mas não a você"). Who actually *receives* each blessing is still
  deliberately not this core's concern — a caller resolves the concrete recipient list via
  `Scene.getAllies`/`getEnemies` (for `ALLIES`/`ENEMIES`), the actor itself plus `SceneContext
  .getAlliesWithin` (for `SELF`/`SELF_AND_ALLIES`), or its own target lookup (for
  `SINGLE_TARGET`), and calls `grantTemporaryBonus` on each recipient itself.
  `InteractionResult` needed `@Builder(toBuilder = true)` added for this — a subclass
  overriding `applyTo` (see below) extends the base result via `.toBuilder()` rather than
  reassembling every field by hand. `blessings` is `null` when an Interaction can't grant one
  at all (same stays-`null`-when-not-applicable convention as every other field here) — a
  non-null (possibly single-entry) list means it's actively reporting one or more.

  `Blessing` also carries a `String source`, identifying which trait granted it (e.g.
  `"DOM_BARDICO"`, `"GRITO_DE_GUERRA_VULCANO"`) — needed once `InitiativeBlessingService
  .resolveBlessings` started concatenating blessings from up to three different sources
  (`EgoAdvantage`/`AttributeAbility`/`SkillCompetencyAbility`) into one flat list with no way
  to tell which one granted which. **Prefer the granting ability's own enum `.name()` over a
  hand-written string literal** wherever the granting site already has that constant in hand —
  `ArtesInteraction` passes `ArtesCompetencyAbility.DOM_BARDICO.name()`,
  `GritoDeGuerraVulcanoInteraction` passes `AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO
  .name()`, and `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO`'s own override passes `name()`
  directly (it's already inside that constant's own anonymous body) — so the two can never
  drift apart. A hand-written constant is still the right call where no such enum exists to
  reference (e.g. a test double implementing `AttributeAbility`/`SkillCompetencyAbility`
  directly). This core still doesn't track *who* (which Character) granted a bonus, only *what
  trait* did — the same restraint `CharacterSheet#grantTemporaryBonus` itself already applies;
  `source` doesn't change that.

  **`Blessing` unifies two mechanisms that used to be separate**: it's also what {@code
  InitiativeBlessingService}/`Scene#applyInitiativeBlessings` resolve and apply for the
  initiative-win trigger (see "Movimento Base, and blessings granted on winning initiative"
  below) — an earlier design had a distinct `InitiativeBlessing` class with a plain `boolean
  appliesToAllies` field instead of `TargetScope`, kept deliberately separate from
  `InteractionResult`'s own (then-singular) temporary-bonus fields specifically because
  `TargetScope.ALLIES` excludes the caster while `appliesToAllies` always includes them —
  reusing `TargetScope` back then would have silently conflated the two. Adding `SELF`/
  `SELF_AND_ALLIES` as their own distinct constants (rather than overloading `ALLIES`'s
  existing meaning) resolved that conflict once a second real Interaction-reported mechanism
  (`GritoDeGuerraVulcanoInteraction`, see below) needed the identical self-plus-allies shape —
  the same "generalize once a second real consumer needs the identical shape" restraint used
  throughout this codebase. `InitiativeBlessingService`/`Scene#applyInitiativeBlessings`/
  `resolveInitiativeBlessings()` keep their own names unchanged — they describe the
  initiative-win *trigger*, which didn't change, only the value type they operate on did.
- **A single Interaction can report more than one `Blessing` at once** — `blessings` is a
  `List`, not a single value, specifically because `GritoDeGuerraVulcanoInteraction` (see
  "Adding a new Título Aventyr" below) needs to report three simultaneously (two Vantagem
  bonuses toward different Perícias, plus a Defesas one), which an earlier singular-fields
  design (`temporaryBonusValue`/`temporaryBonusModifierType`/`temporaryBonusRounds`/
  `temporaryBonusScope`) had no way to express. `ArtesInteraction` (DOM_BARDICO, still only
  ever reports one) was migrated onto this same list rather than kept on the old singular
  shape, so `InteractionResult` has exactly one mechanism for "this Interaction granted a
  temporary bonus," not two.
- `ArtesInteraction` overrides `applyTo` to report one `Blessing` for a character holding
  `ArtesCompetencyAbility#DOM_BARDICO`, once a `SkillRoll` reaching at least GD Médio was
  supplied: `modifierType` (always `SKILL_ROLL_BONUS`, since this ability's own rules text is
  unrestricted — "rolagens de Perícias", not one specific Perícia), `scope` (always `ALLIES`),
  `rounds` (1 Rodada normally, 2 once Artes reaches 5 Graduações, 3 at 10 — a small
  graduation-threshold lookup specific to this one ability, not worth generalizing into
  `ExcellencyTier`'s fixed 3/7/10 shape since DOM_BARDICO's thresholds/values don't match it),
  and `value`, via a GD-tier-to-bonus lookup (`ArtesInteraction.domBardicoBonusValue`): GD
  Médio +1, Difícil +2, Muito Difícil +3, Improvável +4, Milagre +5, mapping onto
  `DifficultyLevel.MEDIUM/HARD/VERY_HARD/UNLIKELY/MIRACLE`. `UNIMAGINABLE` isn't named in the
  rules text (it falls between Improvável and Milagre); it's treated as inheriting
  Improvável's +4 until Milagre is actually reached — an inference, not confirmed text. Below
  Médio, or when no roll was supplied at all, there's nothing coherent to report a `value` for
  — rather than exposing a partially-filled `Blessing` (a real `modifierType`/`scope`/`rounds`
  but no meaningful `value`), `blessings` simply stays `null` in that case, same as when
  DOM_BARDICO isn't held at all. A caller gates on `blessings != null` before granting
  anything, the same restraint the old `temporaryBonusValue != null` check used to express.
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
  built ahead of a concrete consumer, same as `blessings` above was before `ArtesInteraction`
  started setting it.

The `granting-a-blessing` Claude Code skill packages this whole section (plus "Movimento Base,
and blessings granted on winning initiative" below) as an invokable walkthrough — including how
to tell a genuine `Blessing` case apart from the several narrower bonus mechanisms elsewhere in
this file (a flat `@Modifier`, `resolveConditionalRollBonus`, `resolveDamageBonus`,
`resolveAbsoluteDamageReduction`/`resolveHalfDamage`) before reaching for this one.

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
  `hasEnemyWithin(Range.DISTANCIA_CURTA)`), `countAlliesWithin(Range)`/
  `countEnemiesWithin(Range)` — for a bonus that scales with *how many* allies/enemies are
  nearby (a "gang up" bonus, a "surrounded" malus), not just whether any are — and
  `getAlliesWithin(Range)`/`getEnemiesWithin(Range)`, returning the matching `CharacterSheet`s
  themselves rather than just a boolean/count, for a caller that needs to inspect each
  qualifying ally/enemy individually (e.g. `DamageServiceImpl`'s own scan for `SantoAbility
  .BASTIAO_DOS_NECESSITADOS`, comparing PV against each adjacent ally in turn — see "Damage
  mitigation" below). All four `*Within` shapes share one private `isWithin` filter underneath,
  instead of separate implementations that could drift.
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

Building this mechanism didn't retroactively finish `POSICIONAMENTO_ESTRATEGICO`/
`TORRE_EM_MOVIMENTO` on its own — each still cited a *different* missing system (no Movimento
Base stat or movement-triggers-Reação mechanism for the former; `DamageService` taking no
`SceneContext` at all for the latter). Both are now real, through two different mechanisms:
`POSICIONAMENTO_ESTRATEGICO`'s movement-*amount* half via the initiative-blessing mechanism
below (a `MovementService` stat plus a grant/revoke pathway, not `SceneContext` itself) — see
"Movimento Base, and blessings granted on winning initiative" — and `TORRE_EM_MOVIMENTO`'s RA/
half-damage via `SceneContext` directly, the same shape as `IMPETO` above, not the blessing
mechanism (see "RA and half-damage conditioned on SceneContext" further below for why: both of
its clauses are about the holder's own defense, never granted to allies, so there was nothing
to spread via `Scene#applyInitiativeBlessings`). `POSICIONAMENTO_ESTRATEGICO`'s
Reação-suppression half is the only piece from this whole family still TODO'd — it has no
system to attach to at all, movement-triggered or otherwise. Check each constant's own TODO
rather than assuming one fix unblocks every citation of it.

### A skill-scoped roll bonus for `EgoAdvantage` — `resolveSkillSpecificRollBonus`, `ResourcesAdvantage`, `MoralHerdadaAbility`

`EgoAdvantage#resolveConditionalRollBonus` is summed identically into *every* Perícia's own
`AbstractSkillInteraction#applyTo` — correct for `InitiativeAdvantage#IMPETO` (genuinely
unscoped, "suas Rolagens de Perícia"), but it has no `SkillType` to condition on at all, so a
Vantagem de Ego scoped to specific *named* skills (e.g. `ResourcesAdvantage#MORAL_HERDADA`'s
"+1 em rolagens de Artes e Persuasão") can't use it without silently over-granting into every
other Perícia too — the same "don't silently narrow or over-grant" restraint already
established for purpose-scoped `SkillCompetencyAbility` Vantagens. Unlike those (scoped to a
narrative *purpose* this codebase genuinely can't track), a scope of specific named skills
*is* trackable — every `AbstractSkillInteraction` already knows its own `skillType`.

- `EgoAdvantage#resolveSkillSpecificRollBonus(SkillType, SceneContext, CharacterSheet target)`
  is the new default-empty hook this adds, mirroring `resolveConditionalRollBonus`'s shape plus
  the two extra parameters it actually needs: `skillType` to gate on (return `Optional.empty()`
  for anything not named), and `target` — the roller's own CharacterSheet, not a separate
  attack target like `SkillCompetencyAbility#resolveDamageBonus`'s `attackTarget` — passed
  explicitly because a Vantagem's skill-specific bonus may depend on the roller's own state
  (here, its current Fama) that isn't reflection-discoverable via a no-arg `@Modifier` method.
  Summed by `AbstractSkillInteraction#sumEgoAdvantageSkillSpecificRollBonuses`, called
  unconditionally alongside the existing `sumEgoAdvantageRollBonuses` — safe for every other
  `EgoAdvantage` constant, since it just falls through to `Optional.empty()`.
- `ResourcesAdvantage` (`org.aventyrs.core.ego`) is the Vantagem de Recursos catalog, the same
  shape as `InitiativeAdvantage`/`AutocontroleAdvantage` (`EgoDomain.RECURSOS`, `EGO_ADVANTAGE_MIN_BASE`
  gate). `BARGANHISTA`/`HERANCA_FAMILIAR` are both fully TODO'd — each needs a PE (Ponto de
  Equipamento) cost system and an Equipamento/Item entity (Raridade, Obra-Prima tiers,
  Aprimoramentos) that don't exist anywhere in this codebase yet (`org.aventyrs.core.item
  .ItemInteraction` is still a bare "TODO implement" stub) — consistent with `ProfissaoCompetencyAbility
  .FORJA_VULCANA`'s own citation of the same missing entity.
- `MORAL_HERDADA` needs a choice at acquisition (Fama Positiva vs. Negativa) that feeds its own
  bonus math, so — same pattern as `ArtesCompetencyAbility#APRIMORAR_COM_ARTE` — the enum
  constant stays as the catalog/rules-text entry, and a character who actually picks it is
  granted a `MoralHerdadaAbility` instance in `Character.egoAdvantages` instead.
  `MoralHerdadaAbility(FamaChoice)` (`FamaChoice` is a nested two-constant enum, `POSITIVA`/
  `NEGATIVA`) delegates `getEgoDomain()`/`getDescription()` to the catalog constant, and:
  - overrides `resolveSkillSpecificRollBonus` for real: `+1` (`BASE_ROLL_BONUS`) toward Artes
    and Persuasão only, `+1` more per 10 points (`FAMA_POINTS_PER_BONUS_STEP`, floor division)
    of whichever Fama type was chosen — read *live* off `target` every call (`CharacterSheet
    #getFamaPositiva`/`#getFamaNegativa`), not frozen at acquisition, since Fama keeps growing
    after creation (Excelência bonuses, Narrador rewards) and this Vantagem's own rules text
    tracks "a Fama escolhida," not a snapshot of it.
  - exposes `applyStartingFama(Character, CharacterSheet)` for the "recebe Fama... igual ao seu
    valor de Recursos" half — a real, tested one-time grant (`character.getEgos().getRecursos()
    .getTotal()` via `CharacterSheet#increaseFamaPositiva`/`#increaseFamaNegativa`), **not**
    TODO'd, since it's mechanically expressible today with existing pieces. It has no automatic
    caller yet, though: `CharacterCreationServiceImpl` only ever assembles a plain `Character`,
    and Fama only exists on `CharacterSheet` — no `CharacterSheet` exists yet at the point this
    Vantagem is actually chosen for this to grant onto. The same ordering gap
    `CharacterAttributeService#upgradeBase`/`SkillGraduationService#upgradeGraduation` already
    work around by taking both a `Character` and a `CharacterSheet` explicitly, rather than
    assuming one always has the other in hand.

### `SorteAdvantage` — a per-enum method outside the shared `EgoAdvantage` interface

`SorteAdvantage` (`org.aventyrs.core.ego`) is the Vantagem de Sorte catalog — same shape as
`InitiativeAdvantage`/`AutocontroleAdvantage`/`ResourcesAdvantage` (`EgoDomain.SORTE`,
`EGO_ADVANTAGE_MIN_BASE` gate), no acquisition-time choice needed by any of its three
constants, so no instance-based class like `MoralHerdadaAbility` was needed here.

- `AS_NA_MANGA` and `DILETO_DE_TYKHE` are both fully TODO'd, each blocked on a gap already
  cited elsewhere in this file rather than a new one: `AS_NA_MANGA`'s "2UD de movimento,
  ignorando Reações e terreno difícil, imediatamente após usar um Ponto de Sorte" needs three
  separate missing pieces — no "triggered the moment a resource is spent" hook exists anywhere
  (`CharacterSheet#spendTemporaryEgoPoints` is a plain mutator, nothing observes it — see the
  Iniciativa section above for why an observer/event mechanism was deliberately rejected in
  this codebase, the same reasoning applies here), no movement-triggers-Reação suppression
  mechanism (`InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO`'s own Reação-suppression half is
  still TODO'd on the identical gap), and `TerrainType` models only what kind of place a whole
  Scene is in, not a per-movement "terreno difícil" concept to ignore. `DILETO_DE_TYKHE`'s "+1
  ponto de Sorte temporário... por sessão de jogo" is the exact same "no game-session tracking
  system exists yet" gap `AutocontroleAdvantage#MOTIVACAO_DE_MOSES`'s own "por sessão" clause
  already cites.
- `ACE`'s `resolveCriticalMarginIncrease(SkillType, SceneContext)` is real, tested data — `+1`
  for a Perícia de Ataque during a Cena de Combate, `+3` for a non-Ataque Perícia outside one,
  `0` otherwise — the same shape as `ArtesAprimorarComArteAbility#getCriticalMarginReduction`,
  and it carries that method's identical gap too: `SkillRoll#getCriticalResult()` is a fixed
  dice-matching check (three or two matching faces at the extremes), not a threshold/margin
  comparison a bonus like this could actually widen, so there's still no roll-resolution engine
  anywhere in this codebase to consult either method automatically.
- This method is declared directly on `SorteAdvantage` itself, **not** added to the shared
  `EgoAdvantage` interface — unlike `resolveSkillSpecificRollBonus` above (added to the shared
  interface because a second, real consumer already existed in `AbstractSkillInteraction`),
  nothing else in this codebase has an analogous Margem Crítica concept to share this shape
  with yet, so widening the interface for a single constant would just be speculative. If a
  second `EgoDomain`'s own Vantagem ever needs the same shape, promote it to `EgoAdvantage`
  then — the same "build ahead of a *second* real consumer, not a hypothetical one" restraint
  this codebase applies everywhere else.

## Movimento Base, and blessings granted on winning initiative — `MovementService`, `InitiativeBlessingService`, `Scene#applyInitiativeBlessings`

Two more pieces finish out `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO`'s movement half:
a real "Movimento Base" character stat (previously missing everywhere it was cited — see the
paragraph above), and a way for a Vantagem/Habilidade/Habilidade de Competência to grant a
Round-scoped buff to a whole group the moment it wins initiative, that's both revoked when a
different group wins next and extended to a group member who joins *after* the win.

- **`org.aventyrs.core.character.services.MovementService#getTotalMovement(Character,
  turnNumber)`** is the `InitiativeService` variant of the "Character-level stats aggregated
  from abilities" shape above, not the Reações/Ações Livres one — no new `Character` field, no
  `CharacterFixture` template change. Its base is derived: {@code SizeCategory
  .getMovementPerActionPoint()} (via `CharacterSizeService#getEffectiveSizeCategory`, so
  Sangue de Gigante-style size-shifting is already reflected) times `ActionPointsService
  .getMaxActionPoints` for that Turn — how far the character could move if every Ponto de Ação
  that Turn were spent moving — plus the usual `ModifierType.MOVEMENT` sum across
  `attributeAbilities`, `skillCompetencyAbilities` (acquired **and** racial, via
  `SkillCompetencyAbility.allFor` — unlike `ReactionsService`/`InitiativeService`, which
  predate that fix and still only scan the acquired list; `MovementService`, written after,
  starts from the corrected combined one), and unlocked `SkillExcellency` tiers. Floored at 0,
  same as Reações/Ações Livres/RD/RA (a spendable-resource-like budget), not `InitiativeService`'s
  own no-clamp exception. This one service already finished three pre-existing TODOs purely
  blocked on "no Movimento Base stat exists" — `AtaqueCorpoACorpoExcellency.FOCADO`,
  `AtletismoCompetencyAbility.PASSO_LARGO`, and the flat "+1UD" half of `DexterityAbility
  .PASSOS_LONGOS` (its "primeiro movimento... distância aumentada" half stays TODO'd — that's
  conditioned on *which* movement in the Rodada this is, which a flat per-Turn total can't
  express) — each now a plain `@Modifier(ModifierType.MOVEMENT)` method, same shape as
  `AtletismoExcellency.FOCADO`'s pre-existing `FREE_ACTIONS` one. `AtletismoCompetencyAbility
  .ALPINISTA_VELOZ`/`ANFIBIO` (vertical/swim movement) and `DirigirECavalgarExcellency`
  (a mount/vehicle's own movement, not the character's) are a **different** sub-stat this
  service doesn't track — don't wire those into `ModifierType.MOVEMENT` just because the
  general stat now exists. Several race docs (`Colosso`, `Pequenino`, `Dolos`, `Aquan`) also
  cited "no Movimento Base stat" for their own still-unmodeled traits; their TODOs were
  rewritten to point at the real remaining gap instead (no `*RacialAbility` catalog constant
  for these races yet — see "Races live in..." below), not left claiming a solved problem.
  `MovementService` returns the **permanent** total only — a caller wanting what's actually
  available this Round also adds `CharacterSheet#getTemporaryBonus(ModifierType.MOVEMENT)`,
  the same combination `AbstractSkillInteraction` already performs for `skillRollBonus`.

- **`org.aventyrs.core.sheet.Blessing`** (`ModifierType`, `value`, `rounds`, plus a
  `TargetScope scope`) is what a trait grants the moment its holder wins initiative — the same
  class `InteractionResult#getBlessings()` uses for a directly-activated grant (see "Temporary
  bonuses from other Characters" above for that second mechanism, and for why this class was
  generalized from an earlier, initiative-only-scoped `InitiativeBlessing` with a plain
  `boolean appliesToAllies` field once a second real consumer needed the identical shape).
  `TargetScope.SELF_AND_ALLIES` is what a Vantagem/Habilidade grants here: unlike DOM_BARDICO's
  `ALLIES` (which *excludes* the caster — "a eles, mas não a você"), this always applies to the
  holder too, and only *additionally* extends to allies when scoped that way — `TargetScope
  .SELF` covers the "holder only, never extends to allies" case (the old `appliesToAllies=false`).
  A `default List<Blessing> resolveInitiativeBlessings()` (empty by default) was added
  identically to `EgoAdvantage`, `AttributeAbility`, and `SkillCompetencyAbility` — the exact
  three sources named "Vantagens de Ego, Habilidades, and Habilidades de Competência" — the
  method itself keeps its `resolveInitiativeBlessings` name (it still describes the
  initiative-win trigger specifically) even though the value type it returns is now the shared
  `Blessing` class. Deliberately **not** added to `SkillExcellency` — this is a narrower,
  three-source scan by design, unlike the four-source flat-`@Modifier` convention (which does
  include excellencies) used everywhere else in this section. No-arg, unlike
  `resolveConditionalRollBonus`/`resolveDamageBonus`: this resolves once at grant-time, not
  per-roll, so the Round-scoping lives in the granted blessing's own `rounds` countdown instead
  of a `SceneContext` check. `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO` is the only
  current constant overriding it, granting `(MOVEMENT, +2, 2 rounds, TargetScope
  .SELF_AND_ALLIES)`.

- **`InitiativeBlessingService#resolveBlessings(Character)`** (`org.aventyrs.core.character.services`)
  is the pure-function scan across all three sources, mirroring every other `<X>Service`'s DI
  shape. It doesn't grant or mutate anything itself. The service (and interface/impl class
  names) kept their own `InitiativeBlessing*` names through the `Blessing` rename above — same
  reasoning as `resolveInitiativeBlessings()`'s own method name.

- **`Scene#applyInitiativeBlessings(CharacterSheet winner, List<Blessing> blessings)`**
  is the method a caller invokes the moment winner's group actually wins initiative, passing
  winner's own already-resolved blessings (e.g. from `InitiativeBlessingService
  .resolveBlessings(winner.getCharacter())`, called by the caller — `Scene` deliberately never
  reaches into a Service to compute what a Character's abilities grant; the same restraint
  `buildContext` already applies to every other fact it assembles into a `SceneContext`, and
  `Scene` had a brief detour from it worth noting: an earlier version of this method resolved
  `blessings` itself via a constructor-injected `InitiativeBlessingService` field — reworked
  once it was clear that broke the "Scene never depends on a Service" pattern every other
  method here already followed). Throws `IllegalOperationException` — `INITIATIVE_NOT_WON` —
  if `wonInitiative(winner)` isn't already true. It revokes every blessing an earlier call
  granted first (via the new `CharacterSheet#removeEffect`, the symmetric counterpart to
  `applyEffect` — reference-based removal, since neither `TemporaryEffect` nor `TemporaryBonus`
  overrides `equals()`, so a caller can revoke exactly the instance it tracked without
  disturbing an unrelated `TemporaryBonus` of the same `ModifierType` from some other source),
  then grants *all* of `blessings` to winner directly, plus every `TargetScope
  .SELF_AND_ALLIES`-scoped one to each of `getAllies(winner)` too — each grant a fresh
  `TemporaryBonus`, tracked in a `Scene`-owned `grantedBlessings` map so the next call (a new
  group winning, or the same one winning again) can revoke precisely these and no more.

  `addParticipant(CharacterSheet, int, UUID group)` extends the same currently-active
  **ally-scoped** blessings to a CharacterSheet that joins an *already*-blessed group
  afterwards — it's part of a group that already won, even though it didn't personally roll
  the highest value. "Blessed" isn't cached in a separate field either (an earlier version had
  a `blessedGroup UUID` field for this — dropped for the same reason as the Service field:
  it was just a redundant cache of something `grantedBlessings` already implies, with its own
  risk of drifting out of sync). Instead, `isGroupBlessed(UUID group)` checks directly against
  the tracked grants: `grantedBlessings.keySet().stream().anyMatch(sheet ->
  groupOf(sheet).equals(group))` — a group is blessed exactly when someone already tracked
  belongs to it, which is always the group the newcomer is being inserted into, checked at the
  moment of insertion. The actual blessing *values* (`activeBlessings: List<Blessing>`)
  still need to be kept as their own field, though — unlike "is this group blessed" (a yes/no
  derivable from group membership), `grantedBlessings`' raw `TemporaryBonus`es don't carry the
  originating `Blessing`'s own `scope`, so there'd be no way to tell a self-only blessing apart
  from an ally-scoped one when deciding what to copy to a newcomer without it. A `TargetScope
  .SELF` blessing never propagates to a newcomer, or to allies at grant-time either — it
  belongs to whichever Character actually holds the granting trait, not the group at large. A
  lone newcomer added via the 2-arg `addParticipant` overload gets a fresh random group per
  that overload's own existing behavior, so it never accidentally matches the blessed group.

## Iniciativa can change mid-Scene — `InitiativeEntry#getEffectiveInitiativeValue`

`InitiativeEntry.getInitiativeValue()` is fixed once rolled — but a participant's actual
Iniciativa *standing* isn't necessarily fixed for their whole time in a `Scene`: a bonus
granted by another Character, an activated Ability, or a passive with a fixed Round trigger
can each change it mid-Scene. All three route through the pre-existing
`ModifierType.INITIATIVE` / `TemporaryBonus` machinery — `ModifierType.INITIATIVE` was already
fully summed by `InitiativeServiceImpl`, just with no production consumer yet — so this was a
"the computed total can now drift, and the Scene's turn order needs to reflect that" problem,
not a new bonus-computation one.

The key constraint: **`CharacterSheet` has no reference back to its `Scene`**, so a granting
Ability can never call into `Scene` itself — it only ever has the `CharacterSheet` (and maybe
`Character`) in hand, the same as every other ability effect in this codebase. An Ability
grants the bonus the *exact* same way `ArtesCompetencyAbility#DOM_BARDICO`/the
initiative-win-triggered `Blessing` mechanism already do — a plain `characterSheet
.grantTemporaryBonus(ModifierType.INITIATIVE, value, rounds)` call, nothing Scene-specific
about it. `Scene` picks this up itself, on its own schedule, rather than needing to be told:

- `InitiativeEntry#getEffectiveInitiativeValue()` is `getInitiativeValue()` plus whatever
  `ModifierType.INITIATIVE` currently sums to on `characterSheet` (`CharacterSheet
  #getTemporaryBonus`) — resolved fresh from the `CharacterSheet` the entry already holds a
  reference to, every time it's called. This is the same "recompute on demand from data
  already in hand" shape `Scene#wonInitiative`/`#buildContext` already use — no push
  notification needed, since `Scene` already references every `CharacterSheet` it's tracking
  and can just ask.
- `Scene#wonInitiative`/`#buildContext` (via `bestInitiativeValue`) now read
  `getEffectiveInitiativeValue()` instead of the fixed `getInitiativeValue()` — so a granted
  bonus flips "who's currently winning" the instant it's granted, same as every other fact
  these two already recompute fresh per call.
- Turn *order* (`Scene#getParticipantsInInitiativeOrder`/`#next()`) is the one thing
  deliberately **not** live — `activeEntries` stays in whatever sequence it was last sorted
  into until the next Round boundary, so a granted bonus never reshuffles turns already in
  progress this Round (an unconditional design requirement — same guarantee `addParticipant`
  already gives a mid-Round newcomer via `pendingEntries`). `Scene#next()`'s existing
  round-wrap point — previously just `mergePendingEntries()` — is now `startNewRound()`:
  merges in `pendingEntries` *and* fully re-sorts `activeEntries` by every entry's current
  `getEffectiveInitiativeValue()` (`List#sort`, stable, so ties keep whatever relative order
  they already had — same tie behavior `insertSorted` preserves elsewhere). `insertSorted`
  itself (used by `addParticipant`'s pre-rotation immediate-insert branch) also compares by
  effective value now, for the same reason.
- `Scene#next()` also now calls `CharacterSheet#finishTurn()` on whoever's turn is ending
  (skipped on the very first call, before anyone's had a turn yet) — the "turn shifter"
  `CharacterSheet#finishTurn()`'s own javadoc had been anticipating (`Scene` didn't call it
  automatically before this). Without this, `getEffectiveInitiativeValue()` would still work,
  but a granted `TemporaryBonus` would never actually count down toward expiry as a live Scene
  progresses. This is a real behavior change to `next()` (previously it never touched
  `CharacterSheet` state) — safe for every existing caller, since ticking a `CharacterSheet`
  with no active `TemporaryEffect`s is a no-op. One consequence worth knowing: a bonus granted
  with only 1 Rodada remaining, to a participant who's also last in this Round's order, expires
  in the very `next()` call that also triggers the Round-boundary re-sort (their own turn
  ending both ticks the bonus toward expiry *and* triggers `startNewRound()`) — so it never
  shows up in the resorted order. This matches the existing Rodada-countdown semantics
  (`TemporaryEffect#tick`/`finishTurn`) exactly as they already worked before this change; it's
  not something new this feature introduces.
- No new public method was added to `Scene` for this — the earlier framing considered one
  (`Scene#updateInitiative`, a caller-invoked push), but it doesn't fit: it would need whatever
  triggered the Ability to separately reach into `Scene` and recompute/push a value, which
  isn't how any other Ability effect in this codebase reaches a `Scene`-tracked value — every
  other one (`DOM_BARDICO`, the initiative-win `Blessing` mechanism, RD/RA, damage bonuses)
  grants straight to the `CharacterSheet`/target it already holds, and whatever reads that
  state (`DamageService`,
  every `<Skill>Interaction`, and now `Scene`) resolves it from there on its own. An
  observable/watcher mechanism was also considered and rejected for the same reason this one
  was preferred over it: this codebase has no event/observer pattern anywhere, and `Scene`
  reading `CharacterSheet` state it already references, on its own schedule, needs neither.
- `CharacterSheet#startTurn(int turnNumber)` is the mirror image of `finishTurn()`, called by
  `Scene#next()` the moment a participant's Turn begins (right before `next()` returns them,
  passing `getCurrentRound()`) rather than when one ends — unlike `finishTurn()`, it fires even
  on the very first `next()` call, since that call does start someone's Turn, just none has
  ended yet. turnNumber is 0-based, the same convention `ActionPointsService`/`ActionProfile`/
  `MovementService` already use for their own `turnNumber` parameter, and the same value
  `Scene#getCurrentRound()` itself exposes — this codebase already documented the two as the
  same concept (`getCurrentRound()`'s own javadoc: "same convention as the turnNumber used
  across `ActionPointsService`") before any code actually crossed from `Scene` into that
  family; this is the first real bridge. It's currently a no-op — no `TemporaryEffect` or other
  mechanic in this codebase yet triggers "no início do seu turno" specifically
  (`Bleeding`/`ManaDrain`/`Withering`'s own ongoing loss all apply at Turn-*end*, via
  `tickTemporaryEffects()`/`finishTurn()`) — but it's real and wired all the same, turnNumber
  already in hand for whatever plugs in next, the same "build the hook ahead of its first
  consumer" shape `finishTurn()` itself started as before `Scene` called it automatically.
  Because it's currently a no-op, its `Scene#next()` wiring has no test of its own yet (there's
  no observable side effect to assert on without mocking, which nothing else in this codebase
  uses) — add one alongside whatever future effect first overrides a start-of-Turn hook for
  real, the same way `finishTurn()`'s own wiring only became
  testable once a genuine `TemporaryBonus` existed to tick.

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
   post-mitigation amount. `DamageService.applyDamage(CharacterSheet characterSheet, int
   rawDamage, boolean ignoreDamageReduction)` bridges the two — compute the mitigated total
   (`calculateFinalDamage`, using `characterSheet.getCharacter()` for whatever ability data it
   needs), then apply the result to the sheet's own resource pools. Unlike `RestService
   .applyRest(Character, CharacterSheet, ...)` (which genuinely needs both — see the Attribute/
   Graduação section above), `applyDamage` never takes a separate `Character` parameter at
   all: applying damage always requires a concrete `CharacterSheet` to mutate, so there's
   never a case where a caller has a `Character` but no sheet to derive it from.

If an ability grants RD *or RA* without spelling out a number in its own rules text, its value
is `DamageService.DEFAULT_DAMAGE_REDUCTION` (+2) — RD and RA are independent reductions, but
nothing in the rules text suggests a different unspecified-amount convention for one versus
the other, so this one constant covers both (e.g. `InitiativeAdvantage#TORRE_EM_MOVIMENTO`'s
plain "você recebe RA" — see below). Only deviate from that when the text gives an explicit
number (e.g. `ArtesCompetencyAbility.APRIMORAR_COM_ARTE`'s "+1 RDS" is real data, not the
default, because the text says "+1").

Note that RD becoming mechanically real doesn't automatically make every RD-granting ability
real: `APRIMORAR_COM_ARTE` grants RD as *one branch of a choice* (which Perícia was picked —
solved by the instance-based ability pattern, see the next section: its RDS branch now works
for real, while its Dano Base and Margem Crítica Menor branches stay TODO'd on
`ArtesAprimorarComArteAbility` blocked on their own missing systems), and
`ProfissaoCompetencyAbility.FORJA_VULCANA` grants RD as one branch of a *different* per-item
choice (made at item creation, not ability acquisition) that's still blocked on the missing
Item/Equipamento entity entirely. Check what's *actually* stopping an ability before assuming
a newly-built mechanism resolves it completely.

### `DamageService` keeps `Character#status` in sync — `refreshStatus`

`Character.status` (a `CharacterStatus`) is a **stored** field, not one derived on read — a
`Character` has no `CharacterSheet` of its own to derive it from (damage taken lives on the
sheet, and one Character could back more than one sheet), the same reason
`ReactionsService`/`InitiativeService` compute their own totals in a service rather than on the
data class. It used to be pure dead data — every Effect reporting `InteractionResult
#resultStatus` just echoed whatever it was built with, and nothing ever wrote to it after
creation, so a character at 1 PV still read `CLEAN`. `DamageService#refreshStatus(CharacterSheet)`
is what closes that: it recomputes the tier via `HitPointsService#getStatus` against the
**unclamped** current Hit Points (`getMaxHitPoints - getDamageTaken`, deliberately *not*
`getCurrentHitPoints`, which floors at 0 — that negative range is exactly what distinguishes
`FALLEN`/`COMMA`/`DEAD`) and stores it via `Character#updateStatus`, a plain mutator in the same
family as `grantTitle`/`grantFeat`.

Every `DamageService#applyDamage` overload calls it automatically, after the sheet has actually
absorbed the hit (so Shield absorption is already reflected — a fully-shielded hit leaves the
status untouched). It's **public** for the one caller that mitigates and applies in two separate
steps rather than through `applyDamage`: `DamageInteraction` needs this hit's own final damage
for `resourceLossValue`/its next-stage gate, which `applyDamage`'s "total accumulated so far"
return can't supply, so it calls `calculateFinalDamage` + `CharacterSheet#applyDamage` itself
and then invokes `refreshStatus` explicitly for the status half — reporting that method's return
directly as its `resultStatus` instead of echoing `target.getCharacter().getStatus()`.

**Damage that never goes through `DamageService` still leaves the field stale** — `Sangramento`'s
immediate PV loss, `Bleeding`/`Withering`'s per-Rodada loss, `RealExecution`'s curse damage, and
`CharacterSheet#heal` all mutate the sheet's `ResourcePool` directly and hold no `DamageService`
to refresh through. That's why `RealExecution` computes its own `resultStatus` from
`HitPointsService#getStatus` rather than reading the field (it must be right about whether death
was just inflicted); the other Effects in `org.aventyrs.core.effect` still echo the stored value.
Wiring those paths means giving each one a `DamageService` (or moving the refresh down into
`CharacterSheet` itself, which would need a `HitPointsService` on a data class — the thing this
codebase deliberately doesn't do); don't assume this change made every damage path status-aware.

### RA/half-damage conditioned on `SceneContext` — `EgoAdvantage#resolveAbsoluteDamageReduction`/`#resolveHalfDamage`

`DamageService`/`DamageServiceImpl` originally took only a `Character` — no way to condition RA
or half-damage on Scene facts (which Round, whether a Cena de Combate, whether initiative was
won), the exact gap that kept `InitiativeAdvantage#TORRE_EM_MOVIMENTO` TODO'd even after
`SceneContext` grew `isWithinFirstCombatRounds`/`hasWonInitiative` for `IMPETO`. Rather than
route this through the initiative-blessing mechanism above (built for buffs that spread to
allies when a group wins initiative — `TORRE_EM_MOVIMENTO`'s "RA" and "dano... reduzido à
metade" are both about the holder's own defense only, never granted to anyone else, so there
was nothing to spread), `DamageService` gained the exact same cascading-overload treatment
`AbstractSkillInteraction` already has: `getTotalAbsoluteDamageReduction`/`calculateFinalDamage`/
`applyDamage` each gained a `SceneContext`-accepting overload (the existing `Character`-only
ones now delegate down with `null`), and `EgoAdvantage` gained two more default-empty hooks —
`resolveAbsoluteDamageReduction(SceneContext)` (summed alongside the existing reflection-based
`ModifierType#ABSOLUTE_DAMAGE_REDUCTION` scan) and `resolveHalfDamage(SceneContext)` (a plain
`boolean`, ORed with the existing `ModifierType#HALF_DAMAGE` scan being `> 0`) — mirroring
`resolveConditionalRollBonus`/`resolveDamageBonus`'s existing shape and reasoning exactly.
`TORRE_EM_MOVIMENTO` overrides both: `resolveAbsoluteDamageReduction` returns
`DamageService.DEFAULT_DAMAGE_REDUCTION` while `isWithinFirstCombatRounds` holds,
`resolveHalfDamage` additionally requires `hasWonInitiative()`. Reached end-to-end via
`DamageInteraction`, which gained the matching `SceneContext`-accepting overloads (same
cascading shape, `null` by default) so a caller with an active Scene can pass its context
straight through from `Scene#buildContext` to a dealt hit's actual mitigation, the same way
one already flows into a Perícia roll.

`getTotalAbsoluteDamageReduction(CharacterSheet target, SceneContext)` extends the
`SceneContext`-conditioned scan further — it takes a `CharacterSheet` **directly** rather than
a separate `Character` alongside it (a `CharacterSheet` already carries its own `Character`
via `getCharacter()`, so a caller with a sheet in hand has nothing to gain from also being
asked to pass the `Character` it already implies — an earlier version of this method took
both, redundantly, and a still-earlier version additionally kept a now-removed
`(Character, SceneContext)` public overload for sheet-less callers, dropped once nothing
genuinely needed it — see below) — to sum a **third** source alongside the reflection scan and
`EgoAdvantage`'s own hook: every held `AventyrTitleAbility`'s
`resolveAbsoluteDamageReduction(SceneContext, boolean hasLowerPvAdjacentAlly)` (see
`org.aventyrs.core.title.santo.SantoAbility#BASTIAO_DOS_NECESSITADOS`). Unlike `EgoAdvantage`'s
hook, a Título ability's own condition here needed one more fact neither `SceneContext` nor a
no-arg `@Modifier` method can resolve — a PV comparison between the holder and each adjacent
ally — so `DamageServiceImpl` (the only caller with a `HitPointsService` in hand) resolves
that comparison once, via `sceneContext.getAlliesWithin(Range.ADJACENTE)` and
`HitPointsService#getCurrentHitPoints` per returned ally, and passes the boolean result in
(`getAlliesWithin` itself lives on `SceneContext` — see "Range and SceneContext" above — since
range-filtering is that class's own concern, not `DamageServiceImpl`'s).

There is deliberately **no** sheet-less `(Character, SceneContext)` public overload of this
method (unlike `getTotalDamageReduction(Character)`/`calculateFinalDamage(Character, int,
boolean)`'s own genuinely-sheet-less forms) — every real caller needing Scene-conditioned RA
already has a `CharacterSheet` by that point (an attack always has a target), so once
`calculateFinalDamage(Character, SceneContext, int, boolean)`'s own sheet-less overload
stopped depending on the public method (it now calls the private
`computeTotalAbsoluteDamageReduction(Character, CharacterSheet target, SceneContext)` helper
directly, `target` nullable only internally — `EgoAdvantage`'s own contribution still applies
sheet-less there, just never the Título-ability one, since that needs `target` to be real) —
there was no genuine external consumer of a sheet-less overload left to preserve. The 6-arg
`calculateFinalDamage(CharacterSheet, SceneContext, DamageType, CharacterSheet, int, boolean)`
picks the sheet-based path automatically (it always has a real `target`), so Bastião's RA
reaches `applyDamage` for free — no new public `applyDamage` overload was needed.

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

`AbstractSkillInteraction.applyTo` (see the "Adding a new Perícia" section/`adding-a-pericia`
skill — every `<Skill>Interaction` extends it, so this is no longer duplicated per skill) sums
`ModifierType.SKILL_ROLL_BONUS`
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
