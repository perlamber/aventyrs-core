---
name: adding-a-title
description: This skill should be used when the user asks to "add a new Título", "add a new Aventyr Title", "implement a Título Aventyr", "add a new Santo-style title", or references adding a new entry to org.aventyrs.core.title. Walks through the full checklist of files/tests a new AventyrTitle needs plus the architectural rationale, mirroring `Santo` as the reference implementation.
---

# Adding a new Título Aventyr

A Título Aventyr (e.g. `Santo`) sits alongside `Race`/skills/ego advantages as a top-level
character concept in this codebase, but a character can hold several simultaneously — unlike
`Race`'s single field — one optionally flagged as the holder's Título Primário. This skill
walks through building a new one from scratch, following the exact conventions `Santo`
established as the reference implementation. The architectural rationale is in the next
section (it used to live in CLAUDE.md's "Títulos Aventyr" section); the rest of this skill is
the operational checklist on top of it. `adding-a-title-specialization` covers a Título's two
Especializações and their gated abilities.

## 0. Architectural rationale

- **Catalog vs. instance**: `AventyrTitle` is the per-character *held instance* (same shape as
  `MoralHerdadaAbility`/`ArtesAprimorarComArteAbility`), **not** a stateless-per-family class
  like `Race` — held specializations/abilities are genuinely per-acquisition data. "Which
  Título family this is" is answered by which concrete class implements `AventyrTitle`,
  deliberately not a separate identity enum.
- **Three slots, not a list**: `Character` holds plain nullable `primaryTitle`/`secondaryTitle`/
  `tertiaryTitle` fields, keyed by `TitleSlot` (`org.aventyrs.core.character`, mirroring
  `EgoDomain`'s placement). **Whether an instance is the holder's Título Primário is not a
  method on `AventyrTitle`** — it's a fact about *which slot* holds it, so at most one can ever
  be primary by construction; a caller resolves `Character#getPrimaryTitle() == title`
  externally and passes it in where needed. `Character#grantTitle(AventyrTitle, TitleSlot)` sets
  the field directly, overwriting that slot — acquiring a Título costs no XP and needs no
  `CharacterSheet`, so unlike `upgradeBase`/`upgradeGraduation` there's no service to route
  through. `Character#getAllTitles()` is the derived list (Primário first, empty slots omitted)
  a scanning service uses. `CharacterFixture` sets all three to `null`.
- **"Requer N Especializações/Habilidades" prerequisites are real, enforced data** — one of
  only two exceptions in this codebase to the usual "leave prerequisites as an unenforced
  comment" restraint (the other is `Feat`), enforced by `TitleAbilityService#grantTitleAbility`
  (`TITLE_ABILITY_PREREQUISITE_NOT_MET`). The Suprema-per-combination cap is softer:
  `getAvailableSupremaSlots` reports how many more a Título may receive, and `grantTitleAbility`
  enforces it on that one entry point, but constructing an `AventyrTitle` directly with more is
  still unchecked.
- **Two reference Títulos, two shapes.** `Santo` is activation-heavy (Blessings, Auras, reported
  `EmpoweredAttack`s). `SenhorDaBriga` is passive-heavy and reaches the **attack itself** through the
  Título scans on `AventyrTitle` (`resolveCriticalMarginIncrease`, `resolveCriticalDamage`,
  `resolveDamageBaseIncrease`, `resolveAttackRollBonus`, `resolveDamageRollBonus`, the holder-aware
  `resolveBaseDefesasBonus(ctx, holder, sheet, primary)`) plus `TitleAttackModifiers` for what a
  caller must fold in. A trait whose activation empowers "the next attack" is modelled there as a
  **budget** (`grantEnhancedAttacks`) the scans read, so its Vantagem lands inside the roll rather
  than being folded by hand (`AgarrarEDerrubarInteraction`). A Título-Primário clause receives
  `primary` from the service (`Character#getPrimaryTitle() == title`); it never asks itself.
- **Keep `org.aventyrs.core.title/package-info.java` current** whenever the granting API changes
  shape — same discipline as `character.services`' own package-info.

## 1. Read the rules text first

Before writing any code, get the actual Título's name, its base-effect ("Despertar"-equivalent)
text, and every Título-level (not specialization-gated) Habilidade/Suprema's full Portuguese
rules text. **If the base effect's own text has a "Se este for seu Título Primário, ..."
clause, split it out** — it goes on `getPrimaryTitleBonusDescription()`, not concatenated
onto `getBaseEffectDescription()` (see step 2 and `Santo`'s own
`BASE_EFFECT_DESCRIPTION`/`PRIMARY_TITLE_BONUS_DESCRIPTION` split). Note, for each ability:
- Its "Requer" prerequisite verbatim — **this is now real, enforced data, not just a comment**
  (see step 2's own note below for the exact fields/mechanism). Parse it into two numbers: how
  many Especializações (almost always "1," not caring which of the Título's two), and how many
  *other* Título-level Habilidades/Supremas ("outras Habilidades de `<Title>`").
- Its Custo de Ativação — in PD (Pontos de Determinação), and its Tempo de Ativação in PA
  (Pontos de Ação), "Reação", "Ação Livre" or "Variável". The PD half is a `title.PDCost`; the
  action half is a `sheet.ActionCost` (`ofActionPoints(n)` / `REACTION` / `FREE_ACTION` /
  `dynamic(min)` / `NONE` for a passive) — neither is an `int`.
- Whether it's a Habilidade or a Suprema (the top tier — `TitleAbilityService
  #getAvailableSupremaSlots`/`#grantTitleAbility` enforce the normal one-per-Título-Aventyr
  allotment, plus one more while `InstinctAbility#CENTELHA_SUPERIOR`'s own one-time extra grant
  is unspent, but only through that one service entry point — directly constructing an
  `AventyrTitle` with more still bypasses it, same as any other builder-bypassable invariant).

Every Título has **exactly two** Especializações — but this skill only covers the Título's own
base (name, Despertar-equivalent, and any generically-gated Habilidades/Supremas). Don't invent
Especialização content that wasn't supplied yet: create `<Title>Specialization` with however
many of the two constants you actually have rules text for (see `SantoSpecialization`,
currently holding only `ABENCOADO_PELA_LUZ`) — **then invoke the `adding-a-title-specialization`
skill once per Especialização you do have text for**, right after finishing this skill. That
skill covers the Especialização's own activation cost/effect and its own gated
Habilidades/Supremas — don't duplicate that work here.

## 2. Create the subpackage

`org.aventyrs.core.title.<titlename>` (lowercase, no separators — e.g.
`org.aventyrs.core.title.santo`), holding three files:
- `<Title>.java` — implements `AventyrTitle`. Constructor takes chosen specializations, chosen
  abilities — no "am I primary" parameter; whether a held instance is the character's Título
  Primário is a fact about which `Character` slot holds it (`TitleSlot`, see step 4), never
  something the instance reports about itself. **Type the chosen-abilities constructor
  parameter/field as `List<AventyrTitleAbility>`, not `List<<Title>Ability>`** — a
  specialization-gated `<Specialization>Ability` constant (built later by
  `adding-a-title-specialization`) is a different Java enum than `<Title>Ability`, so the
  narrower type compiles but can never actually hold one; `Santo`'s own field needed retrofitting
  from `List<SantoAbility>` once a gated ability needed validating against it — get this right
  from the start instead. **Store both lists as mutable defensive copies** (`new ArrayList<>(...)`)
  — `grantAbility` and `grantSpecialization` append to them after construction, and `List.of(...)`
  (the common case at a call site) rejects that. Implement **both** mutators, each into its own
  list: an `AventyrTitleSpecialization` *is* an `AventyrTitleAbility`, so routing one through
  `grantAbility` compiles and quietly misfiles it out of `getSpecializations()` — and every
  "Requer N Especializações" prerequisite counts exactly what that method returns.
  `grantSpecialization` should refuse a constant from another Título's catalog
  (`TITLE_ABILITY_PREREQUISITE_NOT_MET`), which is the enforced half of each constant's own
  "Apenas '<Títulos>' podem adquirir esta especialização" line — a foreign constant would be
  counted by `getSpecializationAndSupremaCount()` and inflate the base effect's own arithmetic.
  Delegates `getName()`/`getBaseEffectDescription()`
  to its own fields/constants (there's no separate catalog constant to delegate to, since only
  one concrete class exists per Título family). Override `getPrimaryTitleBonusDescription()`
  too if step 1 found a "Se este for seu Título Primário" clause (default `null` if not — not
  every Título's base effect has one).
- `<Title>Specialization.java` — enum implementing `AventyrTitleSpecialization`. May hold fewer
  than its eventual two constants; leave it to the `adding-a-title-specialization` skill to add
  each one's real content (description, activation cost, effect).
- `<Title>Ability.java` — enum implementing `AventyrTitleAbility`, `@Getter @AllArgsConstructor`
  (mirror `ElfosRacialAbility`'s shape), one constant per **Título-level** Habilidade/Suprema —
  i.e. only ones whose prerequisite names "1 Especialização" generically, not a specific one by
  name. A Habilidade/Suprema gated on one *named* Especialização belongs in a separate
  `<Specialization>Ability` enum instead — see the `adding-a-title-specialization` skill. Every
  constant also needs an `interactionClass` field (`Optional<Class<? extends Interaction>>` —
  `AventyrTitleAbility#getInteractionClass()` is abstract, no default) — `Optional.empty()`
  until/unless step 3 below wires a real activation for that specific constant.
  **Also add `requiredSpecializations` (int) and `requiredOtherAbilities` (int) fields** for
  the "Requer" prerequisite from step 1 — these override `AventyrTitleAbility
  #getRequiredSpecializations()`/`#getRequiredOtherAbilities()` for free via Lombok's
  `@Getter` (matching the interface method names exactly, the same way the existing `PDCost`
  field already overrides `getPDCost()`); both default to 0 if the rules text names neither.
  `#getRequiredOtherAbilities()` on a `<Title>Ability` counts **every** Habilidade the Título
  holds — its own constants *and* the ones a held Especialização brought — because the clause
  names the Título ("2 outras Habilidades **de Santo**"), and a Habilidade an Especialização
  brought is a Habilidade de Santo too. That is what `#getRequiredOtherAbilitiesScope()` returning
  empty means, and it is the default here, since a `<Title>Ability` names no Especialização.
  See `SantoAbility`'s own four constants (1 Especialização apiece; 0/2/2/4 outras Habilidades)
  for the worked example. (A `<Specialization>Ability`'s clause names *its* Especialização
  instead and is scoped to it — see that skill.)
  Don't add a `getRequiredSpecialization()` override here — that's for one *specific*, named
  Especialização, which never applies to this Título-level enum (see the
  `adding-a-title-specialization` skill's own step 4 instead).

## 3. Classify each ability's mechanic

This applies to the Título's own base effect too, not just its Habilidades/Supremas — a
scaling formula in the base-effect text (e.g. "+1 para cada Especialização e Suprema que
possua") is exactly the same kind of "classify real-now vs. TODO'd" decision.

For each Habilidade/Suprema, decide real-now vs. TODO'd:
- **Real now**: pure arithmetic over already-real data (e.g. `Santo
  #resolveMinorCriticalImmunityRounds` — a duration formula written years before anything read it,
  built ahead of a caller the same way `ArtesAprimorarComArteAbility#getBaseDamageBonus` was), or
  expressible via an existing hook (e.g. `AventyrTitleAbility#resolveAbsoluteDamageReduction`,
  the shape `SantoAbility.BASTIAO_DOS_NECESSITADOS` uses). **"Can't apply it yet" (the stat/
  system it scales doesn't exist) doesn't mean "can't compute it yet"** — `Santo
  #resolveBaseDefesasBonus(SceneContext)`/`#resolvePrimaryTitleAllyDefesasBonus(SceneContext)`
  computed Despertar's Defesas bonus (and its Título-Primário half-share) for real long before
  anything could consume them, and were wired up untouched once the hooks existed — the arithmetic
  needed no missing system, only the *application* of the result did. TODO the application, not the
  formula, and say exactly which of the two in the TODO comment. **Re-check a TODO before trusting
  it**: two of Santo's three named blockers had gone stale by the time anyone returned to them,
  because the mechanism had been built for someone else in the meantime.
- **TODO'd**: cite the *specific* missing system. Check CLAUDE.md's existing gap catalog before
  assuming a new gap:
  - **Defesas** — built (`DefenseService`/`DefenseType`), and a Título's own base effect is one
    of the sources it scans. Don't cite it as missing.
  - **Item/Equipamento** — `org.aventyrs.core.item.ItemInteraction` is still a bare stub.
  - **Encantamento/Maldição classification** — no such tag exists anywhere (`Withering`'s own
    citation).
  - **Cross-character continuously-recomputed passive grants** — built, in two shapes, and which
    one you want depends on whose situation the number reads. Recipient's → scan it
    (`resolveAllyAbsoluteDamageReduction`). Holder's → project a `scene.ProjectedAura` and let
    `Scene#refreshProjectedAuras` grant and revoke it (Santo's Título-Primário half). See the
    `damage-and-combat` skill, "Ally-facing passive grants are scanned, not granted".
  - Positioning/teleportation, attack-redirection, floor-at-1PV, locked HP pools — also
    confirmed absent; see `SantoAbility`'s own TODOs for the exact wording style to match.

Never build the missing system just to close a TODO — this codebase's established discipline
is to model real data now and defer the mechanic honestly.

**Once a Título-level Habilidade/Suprema has at least one clause expressible as a real
`Blessing` (or a direct single-target mutation) and is Active (`isPassive() == false`), give
it its own `<X>Interaction extends title.AbstractTitleAbilityInteraction`** (reached through
`AventyrTitle#activateAbility`; a `Santo#activate<X>` convenience is optional and only ever a
one-line delegate) — the same rule
`adding-a-title-specialization`'s own step 3 applies to a specialization's gated abilities,
generalized to the Título's own top-level ones too. "Real" means "expressible," not "has an
actual consumer" — a brand-new `ModifierType` with no reader yet still counts, as long as
nothing else already consumes it a different way (see step 3's own citation of
`GritoDeGuerraVulcanoInteraction`'s Defesas clause vs. `GLORIA_RELAMPEJANTE_DE_TESLA`'s RA one
for the distinction). No `SantoAbility` constant qualifies yet (all still fully TODO'd) — this
is a pointer for later, not new code to write now — but check
`AbencoadoPelaLuzInteraction`/`GritoDeGuerraVulcanoInteraction` (single-target-direct-mutation
vs. self-plus-allies-report-only-via-`Blessing` shapes) and `OrgulhoEldurianoInteraction`
(Scene-registered, Variável-cost) before assuming a new shape is needed once one does. The base
class already does Silêncio, the `PDCost` check, affordability and the PD spend — the subclass
writes only `resolve` (and `validate` if it needs a Scene, target or choice); see
`adding-a-title-specialization` step 3. **Once a constant does get wired, set its own
`interactionClass` field to `Optional.of(<X>Interaction.class)`** — `activateAbility` reads it,
so leaving it empty makes the activation unreachable.

## 4. Wire `Character`

Grant via `character.grantTitle(new <Title>(...), TitleSlot.PRIMARY)` (or `SECONDARY`/
`TERTIARY`) — never rebuild through `.toBuilder()` for this. `Character` holds exactly three
plain nullable Título fields (`primaryTitle`/`secondaryTitle`/`tertiaryTitle`), not a list —
`TitleSlot` (`org.aventyrs.core.character`) names which one. Use `character.getAllTitles()`
when a scanning service needs every held Título regardless of slot (see section 0).

**Register the new family in `org.aventyrs.core.title.TitleCatalog#all()`** (one fresh, empty
instance). That list is what `DespertarAntecipadoFeat#optionsFor` offers, so a Título left out
of it can never be picked for Despertar Antecipado.

## 5. Extend a scanning service only when truly needed

If an ability's condition needs data no no-arg `@Modifier` method can see (e.g. a PV
comparison against `SceneContext`'s allies), follow `SantoAbility.BASTIAO_DOS_NECESSITADOS`/
`DamageServiceImpl`'s explicit-scan shape — add a `target`-carrying overload, resolve the
extra fact once, pass it to every held `AventyrTitleAbility`'s own `resolve*` hook. Otherwise,
prefer a plain `@Modifier` method — `Modifier`'s own javadoc already anticipates a title as a
bonus source ("an ability, a feat, a title, an item").

## 6. Write tests

One file per new type:
- `<Title>SpecializationTest` — count + (once non-empty) description/identity checks.
- `<Title>AbilityTest` — non-blank descriptions, expected count, correct `isSupreme()`/cost
  identity per constant, `getInteractionClass()` per constant (`Optional.of(<X>Interaction
  .class)` for any wired for real, `Optional.empty()` for the rest — see
  `SantoAbilityTest#noAbilityReportsAnInteractionClassYet` for the all-TODO'd baseline case),
  and any wired `resolve*` hook's real branch plus every other constant's default-zero
  behavior. **Also test the requirement fields**: `getRequiredSpecializations()`/
  `getRequiredOtherAbilities()` per constant, plus `isEligible(AventyrTitle)` — a title with no
  Especializações rejects every constant, one with enough of both accepts, and one with only
  *sibling-catalog* Habilidades (e.g. a `<Specialization>Ability` instance, if one already
  exists for this Título) still rejects — see `SantoAbilityTest
  #isEligibleRejectsBastiaoDosNecessitadosWithoutEnoughOtherAbilities`/
  `#isEligibleAcceptsBastiaoDosNecessitadosOnceEnoughOtherAbilitiesAreHeld` for the shape.
- `<Title>Test` — constructor null-rejection, identity methods, round-tripping
  specializations/abilities, any real formula methods.
- An integration test (see `SantoIntegrationTest`) granting the Título to a
  `CharacterFixture`-built Character and exercising any wired ability through the real
  scanning service, not by calling `resolve*` directly.
- Scanning-service test additions (e.g. `DamageServiceImplTest`) for any ability wired for
  real, mirroring the existing `TORRE_EM_MOVIMENTO` test shape.

## 7. Update docs

- `org.aventyrs.core.title/package-info.java` if the granting API's shape changed.
- This skill's section 0 if an *architectural* fact changed, and the skill index in CLAUDE.md
  if the trigger surface changed.

## 8. Invoke `adding-a-title-specialization`

Once the Título's own base is built and tested, invoke the `adding-a-title-specialization`
skill for each Especialização you have rules text for (one invocation per Especialização — up
to two). Don't build Especialização content as part of *this* skill.

## Reference files to read first

- `src/main/java/org/aventyrs/core/title/santo/Santo.java`,
  `SantoAbility.java` — the worked example this skill follows for the Título-level pieces
  (`SantoSpecialization.java`/`AbencoadoPelaLuzAbility.java` belong to the
  `adding-a-title-specialization` skill instead).
- `src/main/java/org/aventyrs/core/title/AventyrTitleAbility.java` —
  `getRequiredSpecializations()`/`getRequiredSpecialization()`/`getRequiredOtherAbilities()`/
  `isEligible(AventyrTitle)`, the requirement-check mechanism every new ability's "Requer"
  clause plugs into (see step 2's own note).
- `src/main/java/org/aventyrs/core/character/services/TitleAbilityService.java`/
  `TitleAbilityServiceImpl.java` — the single entry point (`grantTitleAbility`) that actually
  calls `isEligible` before granting an ability to a held Título, plus
  `getAvailableSupremaSlots` for the Suprema-cap/CENTELHA_SUPERIOR side of the same check.
- `src/main/java/org/aventyrs/core/skill/artes/ArtesCompetencyAbility.java`,
  `ArtesAprimorarComArteAbility.java` — the TODO-writing convention and the
  instance-based-acquisition-choice pattern.
- `src/main/java/org/aventyrs/core/ego/InitiativeAdvantage.java` (`TORRE_EM_MOVIMENTO`) and
  `src/main/java/org/aventyrs/core/character/services/DamageServiceImpl.java` — the
  explicit-`SceneContext`-scan shape for a condition a no-arg `@Modifier` can't express.
- `src/main/java/org/aventyrs/core/title/AbstractTitleAbilityInteraction.java` and
  `TitleAbilityActivationRequest.java` — the shared activation base (gates + PD payment) and its
  request; `AventyrTitle#activateAbility` is the entry point.
- `src/main/java/org/aventyrs/core/title/santo/AbencoadoPelaLuzInteraction.java`,
  `GritoDeGuerraVulcanoInteraction.java` and `OrgulhoEldurianoInteraction.java` — the three
  subclasses (single-target direct mutation, self-plus-allies `Blessing` reporting, a
  Scene-registered Aura scaled by a Variável cost).
- `src/main/java/org/aventyrs/core/sheet/Blessing.java` — the shared value object a
  self-plus-allies grant reports.
