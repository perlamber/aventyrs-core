---
name: adding-a-title-specialization
description: This skill should be used right after adding-a-title finishes building a Título Aventyr's base, and whenever the user asks to "add a specialization to a title", "add an Especialização", "add [SpecializationName] to [Title]", or gives the rules text for one of a Título's two Especializações (e.g. "Abençoado pela Luz"). Walks through modeling the Especialização itself plus its own gated Habilidades/Supremas.
---

# Adding a Título's Especialização

Every Título Aventyr has **exactly two** Especializações (no more, no less); a character may
hold both, one, or neither — the player's choice. This skill covers modeling **one** of them —
run it once per Especialização you have rules text for, normally right after
`adding-a-title` finishes the Título's own base. Santo's own two —
`SantoSpecialization.ABENCOADO_PELA_LUZ`/`AbencoadoPelaLuzAbility` and
`ABRACADO_PELA_ESCURIDAO`/`AbracadoPelaEscuridaoAbility` — are the worked reference examples;
between them they cover both a fixed-cost specialization and a fully variable-PV-cost one, so
compare both before assuming one shape is "the" pattern.

Read `CLAUDE.md`'s "Títulos Aventyr" section first (specifically the parts about
`AventyrTitleSpecialization`'s cost fields and the specialization-gated-ability-enum split) —
this skill is the operational checklist on top of it.

## 1. Read the rules text first

Get the Especialização's own full block: name, "Pré-requisitos" (almost always just "apenas
Títulos of this type can acquire it" — unenforced), Custo de Ativação (PD/PA — 0/0 if it's
purely descriptive with no activatable effect of its own), and description. Then get every
Habilidade/Suprema whose own "Requer" clause names **this specific Especialização** (not "1
Especialização" generically — those belong to the Título's own `<Title>Ability` enum instead,
built by `adding-a-title`, not this skill) — parse it into two pieces: the named Especialização
itself (real, enforced data — see step 4), and, if the clause also says something like "N
outras Habilidades de '<Especialização name>'," that count too (also enforced, scoped to only
this same `<Specialization>Ability` catalog — see step 4's own note).

## 2. Model the Especialização itself

Add a constant to the existing `<Title>Specialization` enum (created by `adding-a-title`):
description, `PDCost`/`actionPointCost` fields if it has a real Custo de Ativação (0/0
otherwise — `AventyrTitleSpecialization`'s cost hooks, inherited from `AventyrTitleAbility`,
default to 0, so a purely descriptive Especialização needs no override at all). **"Custo de
Ativação: Variável" can mean the PD/PA fields are genuinely 0** — not merely "not yet
modeled" — when the real cost is entirely PV, chosen per-use rather than fixed (see
`ABRACADO_PELA_ESCURIDAO`'s own comment: its "Variável" cost is Fúria dos Deuses' 3-or-4 PV,
not a PD amount at all). Say so explicitly in the constant's own comment so a later reader
doesn't mistake 0 for an oversight.

Also set the constructor's `interactionClass` field (`Optional<Class<? extends Interaction>>`
— `AventyrTitleAbility#getInteractionClass()` is abstract, so every constant must answer it,
no default to fall back on): `Optional.empty()` at this point, since step 3 hasn't built the
Interaction yet — come back and change it to `Optional.of(<Specialization>Interaction.class)`
once step 3 does. This field is the declared, checkable bond between the constant and whichever
`<X>Interaction` actually activates it — see `SantoSpecialization.ABENCOADO_PELA_LUZ`'s own
`Optional.of(AbencoadoPelaLuzInteraction.class)` for the filled-in shape.

**`AventyrTitleSpecialization extends AventyrTitleAbility`** — a Título trait with a real
activation cost is an Active Ability whether it's an Especialização or a Habilidade/Suprema,
so every constant here already gets `isSupreme()` (defaults `false`, never applicable),
`isReactionActivation()`/`isFreeActionActivation()`, and `isPassive()` for free. **Check
`isPassive()`'s derived result before leaving it alone**: it's `getActionPointCost() == 0 &&
!isReactionActivation() && !isFreeActionActivation()`, which can't tell "genuinely no cost"
apart from "0 PD/PA but a real cost expressed some other way" — see `ABRACADO_PELA_ESCURIDAO`'s
own override (its cost is entirely PV-based, so the derived formula would wrongly call it
passive) for the pattern to follow whenever a Especialização's real Custo de Ativação isn't
expressible through PD/PA/Reação/Ação Livre.

If the Especialização's own effect is mechanically real today (check against the same "is
there already a service/formula that computes exactly this?" bar every other piece of this
core is held to — e.g. `SantoSpecialization#resolveShortRestHealAmount` reuses
`RestService#getRecoveredHitPoints(character, RestType.CURTO)` verbatim, since that formula
already existed and needed no new system), add it as a real, tested method **on the enum
itself** (not a new shared `AventyrTitleSpecialization` default hook — that interface stays
minimal until a second Título's own Especialização needs the identical shape, mirroring
`SorteAdvantage`/`ACE`'s "defer promotion until a second real consumer" precedent). Guard the
method on `this == <Constant>` so it returns a safe default (0/empty) for sibling constants,
the same way `resolveShortRestHealAmount` does — future-proofing for the Título's second
Especialização without needing every existing method rewritten.

If the effect needs a system this core doesn't have, TODO it — check CLAUDE.md's
"Missing systems — the gap catalog" before assuming a new one. As of Santo's two
Especializações it covers: Defesas, Item/Equipamento, Malefício/Encantamento/Maldição/Doença
classification, Área de Efeito, a flat Desvantagem constant, forced-attack-targeting/
interception, cross-character continuously-recomputed passive grants, temporary/Round-scoped
Attribute bonuses, within-Turn activation-count tracking, reactive/retaliation damage,
forced-movement/positioning, Roubo de Mana/Determinação, "modify this one specific delivered
attack" (as opposed to a standing bonus to every future roll of a skill type), and "spend a
resource for a one-time roll effect" (e.g. a GD reduction bought with PV/PM rather than a
Magia's own casting cost).

**A PV/PM cost that equals an existing stat's own total (e.g. "gastar PV igual ao seu Vigor")
is real, tested data** — just read the stat the normal way (`character.getAttributes()
.getVigor().getTotal()`) — even when everything the spent resource buys is TODO'd. Same for a
**pure arithmetic Duração/count formula over a player-chosen spent amount** (e.g. "Duração
igual a 1 + metade dos PV gastos") — no missing system needed for the formula itself. If two
constants in the same enum need the *identical* formula for *different* purposes (e.g. a
Duração-in-Rodadas here, a count-of-enhanced-attacks there), give each its own clearly-named
public method but factor the shared arithmetic into one private static helper rather than
duplicating it (see `AbracadoPelaEscuridaoAbility`'s `onePlusHalfPvSpent`).

## 3. Wire an activation entry point once at least one clause is real and direct

Real formula data (step 2) isn't the same as a way to *trigger* it. This is the general rule,
not a one-off: **build one `<X>Interaction` per Active Ability — whether it's the
Especialização's own base effect or one of its gated Habilidades/Supremas from step 4 below —
the moment at least one clause of that ability's own rules text is mechanically real and
wireable with existing classes/methods.** Don't wait for the *whole* ability to become
expressible first, and don't skip it just because *some* clause is still TODO'd:

- **Direct, single-target effects that mutate immediately** (e.g. Abençoado pela Luz's own
  touch-heal-or-cure) follow `AbencoadoPelaLuzInteraction`'s shape — the Interaction itself
  calls `target.heal(...)`/`CharacterSheet` mutators directly, since there's exactly one,
  unambiguous recipient already in hand.
- **Effects granting to self-plus-allies** (e.g. `GRITO_DE_GUERRA_VULCANO`, a gated Habilidade
  of Abençoado pela Luz, not the Especialização itself) follow `GritoDeGuerraVulcanoInteraction`'s
  shape instead — a genuinely different target resolution, not a variant of the single-target
  one: it **reports** what to grant via `InteractionResult#getBlessings()` (a `List<Blessing>`)
  rather than mutating anything itself, mirroring `ArtesCompetencyAbility#DOM_BARDICO`'s own
  established "compute what, caller applies who" shape (see CLAUDE.md's "Temporary bonuses from
  other Characters" section for `Blessing`'s full shape) — just with more than one `Blessing`
  at once, since DOM_BARDICO only ever grants one. Check which shape actually matches (direct
  mutation of one known recipient, vs. reporting one-or-more blessings for a caller to apply to
  a resolved-later recipient set) before copying either pattern.
- In both cases: `<X>Interaction implements Interaction<CharacterSheet>` (same subpackage),
  the same cascading-overload shape `AbstractSkillInteraction` established (safe-default 1-arg
  `applyTo(CharacterSheet)`, a longer overload with the real logic, `SceneContext` accepted
  even if this ability's own text doesn't condition on it) but with **no `SkillRoll`, no
  `skillRollBonus`, no dice** — this isn't a Perícia test. If a direct-mutation effect restores
  a resource (a heal, not a drain), report it via `InteractionResult#getResourceGainValue()`/
  `getResourceGainType()` — the mirror of the existing `resourceLossValue`/`resourceLossType`
  — don't reuse the loss fields with a negative number. For a self-plus-allies grant, each
  `Blessing` is typed `TargetScope.SELF_AND_ALLIES` (or `TargetScope.SELF` if the rules text
  never extends to allies) — a caller resolves actual recipients later via `SceneContext
  #getAlliesWithin(Range)` (returns a real `List<CharacterSheet>`, not just a count) plus the
  actor itself, then calls `CharacterSheet#grantTemporaryBonus` on each; this class never needs
  to resolve or touch a `CharacterSheet` other than the actor's own `resultStatus`.
- **"Mechanically real" is judged per clause, not per ability — and now means "expressible as
  a grantable `Blessing`," not "has an actual consumer."** A clause whose value/duration/scope
  is fully known can still get a real `Blessing`, even when nothing reads that `ModifierType`
  yet, **as long as no other consumption pathway for it already exists to conflict with** — see
  `GritoDeGuerraVulcanoInteraction`'s own class javadoc: its "+2 em Defesas" clause is reported
  as a real, new `ModifierType.DEFESAS`-typed `Blessing`, mirroring `ModifierType
  .ACTION_POINTS`'s own already-grantable-but-inert precedent, because Defesas has *no*
  consumption pathway at all yet (no stat/service exists), so there's nothing for a future
  reader to reconcile against. **This does *not* extend to a `ModifierType` that already has a
  working, different consumption pathway** — `GLORIA_RELAMPEJANTE_DE_TESLA`'s RA half uses the
  identical self-plus-allies recipient shape but stays genuinely TODO'd, not reported as a
  `Blessing`: `ABSOLUTE_DAMAGE_REDUCTION` is already consumed for real, just via the
  reflection-based ability scan / `AventyrTitleAbility#resolveAbsoluteDamageReduction`, never
  via `CharacterSheet#getTemporaryBonus` — granting a `TemporaryBonus` of that type would be
  provably inert forever unless `DamageServiceImpl` deliberately grows a second, new
  consumption branch just for it, a real design decision this skill doesn't get to make on its
  own, unlike Defesas's genuinely-empty slate. Check whether a `ModifierType` already has
  *some* working consumer before assuming "not consumed via `getTemporaryBonus` yet" always
  means "safe to report as a `Blessing` anyway."
- Add the actual entry point as a method on `<Title>.java` (e.g. `Santo
  #activateAbencoadoPelaLuz`/`#activateGritoDeGuerraVulcano`), not a static/standalone call
  against the bare enum constant — validate the Título instance actually holds this trait first
  (mirroring `AbstractSkillInteraction#validateRequestedTrait`'s "must actually be held" check;
  throw `IllegalOperationException`/`REQUIRED_TITLE_TRAIT_NOT_HELD`), then delegate to the
  Interaction. The Título instance is where this belongs because it's what actually knows which
  Especializações/Habilidades are held — the same data its own scaling formulas (step 2 of
  `adding-a-title`) already need — and where a *future* ability needing "is this held as the
  Título Primário" would resolve that fact too (a caller-supplied fact, resolved via
  `Character#getPrimaryTitle() == titleInstance` — never self-derived on the instance).
- **Passive abilities never get one** — no activation exists to model for `isPassive() ==
  true`; they're reached via continuous scanning instead (e.g. `DamageServiceImpl`'s
  Título-ability RA scan).
- **Once the `<X>Interaction` exists, go back and set the constant's own `interactionClass`
  field to `Optional.of(<X>Interaction.class)`** (see step 2's own note) — don't leave it at
  `Optional.empty()` after wiring a real activation; that would make the constant's own
  declared bond lie about what it activates.
- Two real shapes exist today (single-target, self-plus-resolved-allies) — still hand-written,
  one-off classes, not a shared base, mirroring `AbstractSkillInteraction`'s own extraction
  history (only extracted once multiple Perícias needed the identical shape). Don't extract a
  shared "Título ability activation" base until a *third* real activation needs a cascade
  identical to one of the two that already exist.

## 4. Model its gated Habilidades/Supremas

Create `<Specialization>Ability.java` (e.g. `AbencoadoPelaLuzAbility`) in the **same
subpackage** as `<Title>`/`<Title>Ability` — `@Getter @AllArgsConstructor enum
<Specialization>Ability implements AventyrTitleAbility`, one constant per Habilidade/Suprema,
each with its own `interactionClass` field (`Optional.empty()` until/unless step 3 builds a
real activation for that specific constant — see step 2's own note on this field). Same
real-vs-TODO discipline as `adding-a-title`'s own step 3, plus three extra things this skill's
own worked example (`AbencoadoPelaLuzAbility`) surfaces that are easy to miss:

- **Every constant here needs a `requiredSpecialization` field of type
  `Optional<AventyrTitleSpecialization>`, set to `Optional.of(<Title>Specialization
  .<THIS_ESPECIALIZAÇÃO>)`** — overriding `AventyrTitleAbility#getRequiredSpecialization()`
  for free via Lombok's `@Getter`. This is real, enforced data (`AventyrTitleAbility
  #isEligible`/`TitleAbilityService#grantTitleAbility`), not merely documented in a comment —
  every constant in this whole catalog names the *same* one Especialização (matching the
  class-level javadoc you're about to write in step 4's own class doc), so this field is
  identical across every constant here. If a constant's own per-constant comment doesn't
  literally repeat "Requer Especialização '<name>'" (e.g. because its own "Requer" clause
  instead only says "N outras Habilidades de '<name>'" — see the next bullet), set this field
  anyway and flag it in the comment as an inference from the class-level statement rather than
  text repeated on that specific constant (see `AbencoadoPelaLuzAbility
  .GLORIA_RELAMPEJANTE_DE_TESLA`'s own comment for the exact wording to mirror) — don't leave
  it `Optional.empty()` just because the per-constant text didn't spell it out again.
- **Add a `requiredOtherAbilities` (int) field for a "Requer N outras Habilidades de
  '<Especialização name>'" clause** — overrides `AventyrTitleAbility
  #getRequiredOtherAbilities()`, 0 by default for constants with no such clause. This count is
  automatically scoped to **only sibling constants of this same `<Specialization>Ability`
  enum** — `isEligible` never counts the Título's own `<Title>Ability` constants (or a
  *different* Especialização's own gated catalog) toward it, even though all of them end up in
  the same held `AventyrTitle#getAbilities()` list — see `GLORIA_RELAMPEJANTE_DE_TESLA`'s own
  "2" for the worked example.
- **An ability can be individually close to real even when nothing wires it end-to-end yet —
  and once it is, wire it per step 3 above rather than leaving it TODO'd.** Before writing "no
  system exists for X," check whether the *value* is already fully expressible as a `Blessing`
  (a flat Vantagem via an existing `ModifierType`, or a brand-new `ModifierType` for a stat
  with no consumer at all yet — see step 3's own "mechanically real" bullet for the
  distinction). `GRITO_DE_GUERRA_VULCANO` is the worked example: **both** of its clauses ended
  up real. Its Vantagem half needed `Skill.ADVANTAGE_BONUS` + `ModifierType
  .ATAQUE_A_DISTANCIA_ROLL_BONUS`/`ATAQUE_CORPO_A_CORPO_ROLL_BONUS` (already summed by
  `AbstractSkillInteraction`); its "+2 em Defesas" half needed a brand-new `ModifierType
  .DEFESAS` constant, added specifically because nothing else consumed it yet, so there was
  nothing to conflict with. `GritoDeGuerraVulcanoInteraction` reports all three `Blessing`s
  (two Vantagem, one Defesas) — the Defesas one just happens to be inert until a future Defesas
  stat/service reads it. Don't assume a same-looking clause on a *different* constant is
  equally reportable, though — `GLORIA_RELAMPEJANTE_DE_TESLA`'s RA half uses the identical
  self-plus-nearby-allies recipient shape but stays genuinely TODO'd, since `ABSOLUTE_DAMAGE_REDUCTION`
  already has a working consumer (the reflection-based ability scan), just never via
  `CharacterSheet#getTemporaryBonus` — reporting a `Blessing` of that type would be provably
  inert forever, not merely "not wired yet," so it doesn't qualify the same way Defesas's
  genuinely-empty slate does.
- **"Custo de Ativação: Variável (mínimo NPD)"** — `getPDCost()` has no way to represent a
  variable cost, only a floor; report the stated minimum and note the "Variável" nuance in the
  constant's own comment (see `ORGULHO_ELDURIANO`).
- **"Tempo de Ativação: Ação Livre"** maps to `AventyrTitleAbility#isFreeActionActivation()`
  (added alongside `isReactionActivation()` specifically for this) — override it via an
  anonymous per-constant body the same way `isSupreme()`/`getPDCost()` are set through the
  constructor (see `GLORIA_RELAMPEJANTE_DE_TESLA`).
- **Don't add a separate passive/active flag** — `AventyrTitleAbility#isPassive()` is already
  derived from `getActionPointCost()`/`isReactionActivation()`/`isFreeActionActivation()`, so a
  new constant needs no extra data for this; just make sure a genuinely no-cost, no-Reação,
  no-Ação-Livre constant really is "Custo de Ativação: Nenhum, habilidade passiva" in its own
  rules text before leaving all three at their defaults (Reação/Ação Livre still count as
  active despite 0 PA).

## 5. Write tests

- `<Title>SpecializationTest` (already exists from `adding-a-title`) — add cases for the new
  constant: description, cost fields, `getInteractionClass()` (see
  `SantoSpecializationTest#onlyAbencoadoPelaLuzReportsAnInteractionClass`), and any real effect
  method (assert it matches whatever existing service/formula it delegates to, not a hardcoded
  number — see
  `SantoSpecializationTest#resolveShortRestHealAmountMatchesRestServicesOwnShortRestFormula`).
  Update the "how many constants exist so far" count test.
- `<Specialization>AbilityTest` (new file) — same shape as `<Title>AbilityTest`: non-blank
  descriptions, expected count, `isSupreme()`/cost/`isReactionActivation()`/
  `isFreeActionActivation()` identity per constant, any wired `resolve*` hook's behavior, and
  `getInteractionClass()` per constant — `Optional.of(<X>Interaction.class)` for the one(s)
  step 3 wired for real, `Optional.empty()` for every other, still-TODO'd constant (see
  `AbencoadoPelaLuzAbilityTest#onlyGritoDeGuerraVulcanoReportsAnInteractionClass`). **Also
  test the requirement fields from step 4**: `getRequiredSpecialization()` equals
  `Optional.of(<the named Especialização>)` for every constant; `getRequiredOtherAbilities()`
  matches each constant's own "Requer N outras..." number (0 for the rest);
  `isEligible(AventyrTitle)` rejects a title without the Especialização for every constant,
  accepts a no-other-abilities constant once the Especialização alone is held, and — the case
  most worth a dedicated test — rejects/accepts the "N outras Habilidades" constant based on
  how many *sibling* constants of this same catalog are held, while a title holding that many
  Habilidades from the **Título's own `<Title>Ability` catalog instead** (not this one) still
  rejects it — see `AbencoadoPelaLuzAbilityTest
  #isEligibleForGloriaRelampejanteDeTeslaIgnoresAbilitiesFromASiblingCatalog`/
  `#isEligibleAcceptsGloriaRelampejanteDeTeslaOnceEnoughSiblingAbilitiesAreHeld` for the shape.
- `<Specialization>InteractionTest` (new file, only if step 3 applied) — each real branch's
  actual effect (e.g. heal amount matches the formula it delegates to, not a hardcoded number),
  every TODO'd branch is an inert no-op, and the bare 1-arg `applyTo` also no-ops (see
  `AbencoadoPelaLuzInteractionTest`). Add `<Title>Test` cases for the new activation method
  too: throws when not held, delegates correctly when held (see `SantoTest
  #activateAbencoadoPelaLuzThrowsWhenNotHeld`/`#activateAbencoadoPelaLuzDelegatesToTheInteractionWhenHeld`).

## 6. Update docs

- `org.aventyrs.core.title/package-info.java` if the granting API's shape changed (it usually
  hasn't — a specialization's own gated abilities are granted through the same held-abilities
  list the Título's own use; the Especialização itself is picked up automatically by any scan
  reading `AventyrTitle#getAllAbilities()`, no separate wiring needed). **This only works if
  `<Title>.java`'s own held-abilities field/constructor parameter is typed
  `List<AventyrTitleAbility>`, not `List<<Title>Ability>`** — a `<Specialization>Ability`
  constant is a different Java enum than `<Title>Ability`, so the narrower type can never
  actually hold one, even though both implement the same interface. Confirm this on
  `<Title>.java` before assuming step 3's `activate<X>` method has anything to validate
  against — `Santo`'s own field needed exactly this widening (originally `List<SantoAbility>`)
  once `activateGritoDeGuerraVulcano` needed to check whether a gated Habilidade was held.
- CLAUDE.md's "Títulos Aventyr" section if this skill's own checklist changed.

## Reference files to read first

- `src/main/java/org/aventyrs/core/title/santo/SantoSpecialization.java`,
  `AbencoadoPelaLuzAbility.java` — the fixed-cost worked example (1PD/2PA specialization, a
  real touch-heal formula reusing `RestService`).
- `src/main/java/org/aventyrs/core/title/santo/AbracadoPelaEscuridaoAbility.java` — the
  variable-PV-cost worked example: a genuinely-0-PD/PA specialization, PV-cost-equals-Vigor
  formulas, and two constants sharing one arithmetic helper for different-meaning results.
- `src/main/java/org/aventyrs/core/title/AventyrTitleSpecialization.java` — now `extends
  AventyrTitleAbility`; the shared cost/activation-type/`isPassive()` shape every
  `<Title>Specialization` enum inherits.
- `src/main/java/org/aventyrs/core/title/AventyrTitle.java` — `getAllAbilities()`, the
  combined-list method a scanning service uses to reach both Especializações and Habilidades/
  Supremas uniformly.
- `src/main/java/org/aventyrs/core/title/AventyrTitleAbility.java` —
  `getRequiredSpecialization()`/`getRequiredOtherAbilities()`/`isEligible(AventyrTitle)`, the
  requirement-check mechanism step 4's two new fields plug into; its own javadoc explains why
  catalog-scoping uses `Enum#getDeclaringClass()` rather than plain `getClass()`.
- `src/main/java/org/aventyrs/core/character/services/TitleAbilityService.java`/
  `TitleAbilityServiceImpl.java` — the single entry point (`grantTitleAbility`) that actually
  calls `isEligible` before granting a gated Habilidade/Suprema to a held Título.
- `src/main/java/org/aventyrs/core/rest/RestService.java` — the kind of already-real formula
  worth reusing directly (as `resolveShortRestHealAmount` does) rather than re-deriving.
- `src/main/java/org/aventyrs/core/title/santo/AbencoadoPelaLuzInteraction.java` and
  `Santo.java`'s own `activateAbencoadoPelaLuz` — the worked example for step 3's
  single-target shape, including the `InteractionResult#resourceGainValue`/`resourceGainType`
  pair.
- `src/main/java/org/aventyrs/core/title/santo/GritoDeGuerraVulcanoInteraction.java` and
  `Santo.java`'s own `activateGritoDeGuerraVulcano` — the worked example for step 3's
  self-plus-allies, report-not-mutate shape: three `Blessing`s reported at once via
  `InteractionResult#getBlessings()`, including one (`ModifierType.DEFESAS`) that's real but
  currently unconsumed.
- `src/main/java/org/aventyrs/core/sheet/Blessing.java` — the shared value object both this
  report-based Interaction shape and the initiative-win-trigger mechanism (see CLAUDE.md's
  "Movimento Base, and blessings granted on winning initiative" section) use.
- `src/main/java/org/aventyrs/core/skill/AbstractSkillInteraction.java` — the cascading-overload
  shape and `validateRequestedTrait`'s "must actually be held" check being mirrored (minus
  everything roll-specific).
