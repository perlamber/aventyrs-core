---
name: adding-a-title
description: This skill should be used when the user asks to "add a new Título", "add a new Aventyr Title", "implement a Título Aventyr", "add a new Santo-style title", or references adding a new entry to org.aventyrs.core.title. Walks through the full checklist of files/tests a new AventyrTitle needs, mirroring CLAUDE.md's "Títulos Aventyr" section.
---

# Adding a new Título Aventyr

A Título Aventyr (e.g. `Santo`) sits alongside `Race`/skills/ego advantages as a top-level
character concept in this codebase, but a character can hold several simultaneously — unlike
`Race`'s single field — one optionally flagged as the holder's Título Primário. This skill
walks through building a new one from scratch, following the exact conventions `Santo`
established as the reference implementation. Read `CLAUDE.md`'s "Títulos Aventyr"
section first — it carries the full architectural rationale; this skill is the operational
checklist on top of it.

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
- Its Custo de Ativação — in PD (Pontos de Determinação), PA (Pontos de Ação), "Reação", or
  "Ação Livre".
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
  from the start instead. Delegates `getName()`/`getBaseEffectDescription()`
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
  `#getRequiredOtherAbilities()` only ever counts sibling constants of this *same* enum
  (`<Title>Ability`, never a `<Specialization>Ability` from a different catalog, even though
  both end up in the same held `AventyrTitle#getAbilities()` list) — see `SantoAbility`'s own
  four constants (1 Especialização apiece; 0/2/2/4 outras Habilidades) for the worked example.
  Don't add a `getRequiredSpecialization()` override here — that's for one *specific*, named
  Especialização, which never applies to this Título-level enum (see the
  `adding-a-title-specialization` skill's own step 4 instead).

## 3. Classify each ability's mechanic

This applies to the Título's own base effect too, not just its Habilidades/Supremas — a
scaling formula in the base-effect text (e.g. "+1 para cada Especialização e Suprema que
possua") is exactly the same kind of "classify real-now vs. TODO'd" decision.

For each Habilidade/Suprema, decide real-now vs. TODO'd:
- **Real now**: pure arithmetic over already-real data (e.g. `Santo
  #getIgnoreCriticalEffectDurationInRounds` — a duration formula with no automatic caller yet,
  built ahead of one the same way `ArtesAprimorarComArteAbility#getBaseDamageBonus` was), or
  expressible via an existing hook (e.g. `AventyrTitleAbility#resolveAbsoluteDamageReduction`,
  the shape `SantoAbility.BASTIAO_DOS_NECESSITADOS` uses). **"Can't apply it yet" (the stat/
  system it scales doesn't exist) doesn't mean "can't compute it yet"** — `Santo
  #getDefesasBonus(SceneContext)`/`#getPrimaryTitleAllyDefesasBonus(SceneContext)` compute
  Despertar's Defesas bonus (and its Título-Primário half-share) for real, even though Defesas
  itself is entirely TODO'd — the arithmetic needs no missing system, only the *application*
  of the result does. TODO the application, not the formula, and say exactly which of the two
  in the TODO comment.
- **TODO'd**: cite the *specific* missing system. Check CLAUDE.md's existing gap catalog before
  assuming a new gap:
  - **Defesas** — no stat/service exists anywhere (`race/Gigantes.java`/`race/Elfo.java`
    already cite this for DF/DM).
  - **Item/Equipamento** — `org.aventyrs.core.item.ItemInteraction` is still a bare stub.
  - **Encantamento/Maldição classification** — no such tag exists anywhere (`Withering`'s own
    citation).
  - **Cross-character continuously-recomputed passive grants** — every existing cross-character
    bonus mechanism is either an explicit roll-time grant (`DOM_BARDICO`) or an
    initiative-win-triggered `InitiativeBlessing`; nothing supports "my always-on passive
    continuously grants a bonus to a nearby Character with no trigger event."
  - Positioning/teleportation, attack-redirection, floor-at-1PV, locked HP pools — also
    confirmed absent; see `SantoAbility`'s own TODOs for the exact wording style to match.

Never build the missing system just to close a TODO — this codebase's established discipline
is to model real data now and defer the mechanic honestly.

**Once a Título-level Habilidade/Suprema has at least one clause expressible as a real
`Blessing` (or a direct single-target mutation) and is Active (`isPassive() == false`), give
it its own `<X>Interaction`/`Santo#activate<X>` pair** — the same rule
`adding-a-title-specialization`'s own step 3 applies to a specialization's gated abilities,
generalized to the Título's own top-level ones too. "Real" means "expressible," not "has an
actual consumer" — a brand-new `ModifierType` with no reader yet still counts, as long as
nothing else already consumes it a different way (see step 3's own citation of
`GritoDeGuerraVulcanoInteraction`'s Defesas clause vs. `GLORIA_RELAMPEJANTE_DE_TESLA`'s RA one
for the distinction). No `SantoAbility` constant qualifies yet (all still fully TODO'd) — this
is a pointer for later, not new code to write now — but check
`AbencoadoPelaLuzInteraction`/`GritoDeGuerraVulcanoInteraction` (single-target-direct-mutation
vs. self-plus-allies-report-only-via-`Blessing` shapes) before assuming a new shape is needed
once one does. **Once a constant does get wired, go back and set its own `interactionClass`
field to `Optional.of(<X>Interaction.class)`** — see step 2's own note; leaving it at
`Optional.empty()` after wiring a real activation would make the constant's declared bond lie
about what it activates.

## 4. Wire `Character`

Grant via `character.grantTitle(new <Title>(...), TitleSlot.PRIMARY)` (or `SECONDARY`/
`TERTIARY`) — never rebuild through `.toBuilder()` for this. `Character` holds exactly three
plain nullable Título fields (`primaryTitle`/`secondaryTitle`/`tertiaryTitle`), not a list —
`TitleSlot` (`org.aventyrs.core.character`) names which one. Use `character.getAllTitles()`
when a scanning service needs every held Título regardless of slot (see CLAUDE.md's own
rationale).

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
- CLAUDE.md's "Títulos Aventyr" section if the checklist itself changed.

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
- `src/main/java/org/aventyrs/core/title/santo/AbencoadoPelaLuzInteraction.java` and
  `GritoDeGuerraVulcanoInteraction.java` — the two `<X>Interaction` shapes (single-target
  direct mutation, self-plus-allies `Blessing` reporting) a real Título-level ability would
  follow once one exists.
- `src/main/java/org/aventyrs/core/sheet/Blessing.java` — the shared value object a
  self-plus-allies grant reports.
