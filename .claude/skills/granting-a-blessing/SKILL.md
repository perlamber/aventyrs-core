---
name: granting-a-blessing
description: This skill should be used whenever rules text describes granting a bonus of any kind or amount — Vantagem, a flat +N to a Perícia roll or dano roll, RD, RA, Movimento, Pontos de Ação, Iniciativa, Defesas, or any other ModifierType-style stat — to the character themselves and/or their allies, for a limited Duração/number of Rodadas. Trigger phrases include (not limited to) "você e seus aliados", "você e aliados em Distância X/adjacentes", "a eles, mas não a você", "ao vencer a iniciativa", "recebe Vantagem em rolagens de Perícias", "recebe RD/RA por N Rodadas", "concede um Bônus de +N", or any Vantagem de Ego/Habilidade/Habilidade de Competência/Habilidade de Título clause naming a temporary, trigger-based grant — whether the trigger is activating an ability/Perícia roll or winning initiative for the group. Also use it when explicitly asked to "add a Blessing", "grant a bonus to allies", or "add a temporary buff". Walks through confirming this is actually a `Blessing` case (not a permanent `@Modifier`, a per-roll conditional bonus, or a damage/RA hook), classifying the trigger, resolving/adding the right `ModifierType`, choosing the `TargetScope`, and wiring it — with `DOM_BARDICO` / `GRITO_DE_GUERRA_VULCANO` / `POSICIONAMENTO_ESTRATEGICO` as the reference constants.
---

# Granting a `Blessing`

A `Blessing` (`org.aventyrs.core.sheet`) is a small value object — `ModifierType`, `int value`,
`int rounds`, `TargetScope scope`, `String source` — for a **temporary, trigger-based bonus**
granted to the holder and/or their Scene allies. Three real constants use it today, covering
both triggers and every current `TargetScope`: `ArtesCompetencyAbility#DOM_BARDICO` (roll-time
activation, `TargetScope.ALLIES` — excludes the caster), `AbencoadoPelaLuzAbility
#GRITO_DE_GUERRA_VULCANO` (a Título Habilidade's own activation, `TargetScope.SELF_AND_ALLIES`,
reporting three `Blessing`s at once), and `InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO`
(winning-initiative trigger, `TargetScope.SELF_AND_ALLIES`). Read all three before assuming a
new case is a fourth genuinely distinct shape.

The `scene-context-and-positioning` skill covers the `Scene`-internal apply/revoke and
ally-propagation facts for the initiative-win trigger; `skill-roll-mechanics` covers "Vantagem
is a flat +2 bonus" and `aggregated-character-stats` covers the `MOVEMENT`/`ACTION_POINTS`/etc.
counters a `Blessing` can target. This skill is the operational checklist for the grant itself.

The consumer-side data model (for anyone reading `InteractionResult.blessings` without granting
one): `TemporaryBonus` (`org.aventyrs.core.sheet`) pairs a `ModifierType`, an `int value`, and a
`remainingRounds` countdown; `CharacterSheet#grantTemporaryBonus(ModifierType, value, rounds)`
adds one and `getTemporaryBonus(ModifierType)` sums every active one of that type;
`tickTemporaryBonuses()` (reached via `finishTurn()`) counts them down. `Blessing`
(`ModifierType`, `value`, `rounds`, `TargetScope scope`, `String source`) is what an Interaction
*reports*; `InteractionResult.blessings` is a `List` that stays `null` when nothing is granted.
`CharacterSheet` doesn't track *who* granted a bonus — a caller resolves recipients via
`Scene.getAllies`/`getEnemies` / `SceneContext.getAlliesWithin` and calls `grantTemporaryBonus`
on each.

## 1. Confirm this is actually a `Blessing` case

Not every "recebe um Bônus" clause is one — this codebase already has several *other*,
narrower mechanisms for a bonus, and reaching for `Blessing` on the wrong one silently
misrepresents the rules text. Rule these out first:

- **Permanent, unconditional bonus to a stat** ("Você recebe RD 2", no Duração/Rodadas
  mentioned, no activation) — a plain `@Modifier(ModifierType.X)` method on the ability/
  excellency, summed by the flat reflection-based scan (`ReactionsService`/`DamageService`/
  `AbstractSkillInteraction`/etc.). **Not** a `Blessing` — nothing to grant/revoke, it's just
  always active while the trait is held. See the `skill-roll-mechanics` and
  `aggregated-character-stats` skills.
- **A per-roll bonus conditioned on live `SceneContext` facts** (e.g. "durante as duas
  primeiras Rodadas de uma Cena de Combate", "se tiver inimigos em Distância Curta"), computed
  fresh every roll rather than granted-then-ticking-down — `EgoAdvantage
  #resolveConditionalRollBonus`/`SkillCompetencyAbility#resolveConditionalRollBonus`-equivalent
  hooks (see `InitiativeAdvantage#IMPETO`). **Not** a `Blessing` — there's no grant/duration to
  track, the condition is just re-evaluated live each time.
- **A bonus scoped to a dano roll specifically** — `resolveDamageBonus`/`DamageBonus` (see
  `AtaqueADistanciaCompetencyAbility#FRIEZA`, `InitiativeAdvantage#IMPETO`'s own dano half).
- **RA or half-damage conditioned on `SceneContext`** — `EgoAdvantage
  #resolveAbsoluteDamageReduction`/`#resolveHalfDamage` or `AventyrTitleAbility
  #resolveAbsoluteDamageReduction` (see `InitiativeAdvantage#TORRE_EM_MOVIMENTO`,
  `SantoAbility#BASTIAO_DOS_NECESSITADOS`).
- **A self-buff the holder spends a resource to switch on for a fixed Duração** ("como uma Ação
  Livre, gaste 3PV, dura 2 Rodadas" — a Poder Vampírico, `FocusAbility#CONCENTRACAO_PROFUNDA`) —
  an `ActiveAbility`, not a `Blessing`. It returns its own `TemporaryEffect`s from
  `resolveEffects(Character)`; `ActiveAbilityService#activate` does the affordability check
  (PA/PM/**PV**), the spend, and the apply. A Talento grants one via `Feat#resolveActiveAbility()`
  (stable singleton). See `PoderVampiricoActiveAbility`.
- **A `Blessing`** is what's left: the rules text names an actual **duration** ("por N
  Rodadas", "nas duas primeiras Rodadas", a graduation/PV-scaled duration) and an actual
  **trigger applied to *someone else* or the whole group** (activating an ability/Habilidade/
  Perícia roll that buffs allies, or winning initiative) — not a live per-roll condition, not a
  standing passive, not a self-only activated state (that's the `ActiveAbility` above).

## 2. Read the rules text for the four facts a `Blessing` needs

- **What** is granted — the stat/bonus itself (Vantagem is always `Skill.ADVANTAGE_BONUS`, per
  "Vantagem is a flat +2 bonus" in CLAUDE.md; otherwise the stated number, or a formula over
  already-real data, e.g. DOM_BARDICO's GD-tier lookup or a graduation/PV-spent scaling).
- **How long** — a fixed Rodada count, or a formula (DOM_BARDICO's 1/2/3 by Graduação;
  `AbracadoPelaEscuridaoAbility`'s own `1 + metade dos PV gastos` shape is the reference for a
  spent-resource-scaled duration).
- **Who** receives it — see step 4 for mapping this to `TargetScope`.
- **What triggers the grant** — see step 3.

## 3. Classify the trigger

- **"Ao vencer a iniciativa" / "se você tiver ganho a iniciativa, ..." (this specific clause's
  own trigger, not just anywhere in a longer ability's text)** — the initiative-win mechanism.
  Override `default List<Blessing> resolveInitiativeBlessings()` on whichever interface the
  granting trait already implements — `EgoAdvantage`, `AttributeAbility`, or
  `SkillCompetencyAbility` (**not** `AventyrTitleAbility` — Título abilities aren't scanned by
  this mechanism at all yet; a Título ability with this exact trigger stays TODO'd, citing that
  specific gap, until a fourth source is added to `InitiativeBlessingServiceImpl`). Resolved by
  `InitiativeBlessingService#resolveBlessings(Character)` and applied by `Scene
  #applyInitiativeBlessings` — this class doesn't touch `CharacterSheet` itself.
- **Any other explicit activation** (a Habilidade's own Custo de Ativação, or a Perícia roll's
  own rules text like DOM_BARDICO's) — report it via `InteractionResult#getBlessings()` on the
  relevant `<X>Interaction` (a `<Skill>Interaction` for a Perícia-roll-time grant, or a
  Título-ability `<X>Interaction` per the `adding-a-title-specialization` skill's own step 3
  for a directly-activated Habilidade). The Interaction **reports** the `Blessing`(s) — it does
  **not** call `CharacterSheet#grantTemporaryBonus` itself, mirroring `ArtesInteraction`/
  `GritoDeGuerraVulcanoInteraction`'s established "compute what, caller applies who" shape.
  (The one exception is a genuinely single, already-known, unambiguous recipient with no allies
  involved at all — e.g. `AbencoadoPelaLuzInteraction`'s touch-heal — which mutates directly
  instead and doesn't need `Blessing` at all; see `adding-a-title-specialization`'s own step 3
  for when that shape applies instead of this one.)

## 4. Choose the `TargetScope`

- **"Você e seus aliados..."** / **"você e aliados em Distância X/adjacentes..."** (always
  includes the holder, optionally extends to allies) → `TargetScope.SELF_AND_ALLIES`.
- **Holder only, rules text never mentions allies** → `TargetScope.SELF`.
- **"...a eles, mas não a você"** (explicitly excludes the caster) → `TargetScope.ALLIES` —
  genuinely different from `SELF_AND_ALLIES`, don't conflate them.
- **A hostile/enemy-facing grant** → `TargetScope.ENEMIES` (no real constant uses this yet —
  if the first one shows up, double check the rules text actually describes a *bonus* to the
  enemy, not a malus, before reusing this scope literally).
- **A single already-known recipient, no ally-spreading at all** → reconsider whether this
  needs `Blessing`/`TargetScope.SINGLE_TARGET` at all, or whether it's actually the direct-
  mutation shape from step 3's last bullet.

Resolving the *concrete* recipient list (who counts as "aliados") is never this ability's own
job — a caller does that later, via `Scene#getAllies` (for `ALLIES`) or `SceneContext
#getAlliesWithin(Range)` plus the actor itself (for `SELF`/`SELF_AND_ALLIES`).

## 5. Resolve or add the `ModifierType`

Check whether an existing constant already matches (`SKILL_ROLL_BONUS`, a per-skill
`<SKILL>_ROLL_BONUS`, `MOVEMENT`, `ACTION_POINTS`, `INITIATIVE`, `DAMAGE_REDUCTION`,
`ABSOLUTE_DAMAGE_REDUCTION`, `DEFESAS`, ...) before adding a new one.

**Before adding a brand-new constant, check whether that stat already has a *different*
consumption pathway you'd be creating a second, conflicting one for.** A brand-new
`ModifierType` with *no* existing consumer at all (like `DEFESAS` was before
`GritoDeGuerraVulcanoInteraction`) is safe to add and grant right away, even though it stays
inert until a consuming stat/service exists — same "can't apply it yet doesn't mean can't
compute/grant it yet" discipline as `ModifierType#ACTION_POINTS`. But a `ModifierType` that's
**already** consumed a different way (e.g. `ABSOLUTE_DAMAGE_REDUCTION`, read only via the
reflection-based ability scan and `resolveAbsoluteDamageReduction`, never via `CharacterSheet
#getTemporaryBonus`) would make a granted `Blessing` of that type provably inert forever, not
merely "not wired yet" — see `GLORIA_RELAMPEJANTE_DE_TESLA`'s own citation of this exact trap.
In that case, don't report a `Blessing` for it at all — TODO the clause instead, citing that
the `ModifierType` already has a working, different consumer.

## 6. Build the `Blessing`(s) and wire the grant

- `new Blessing(modifierType, value, rounds, scope, source)` — `source` identifies which trait
  granted it (needed since `InitiativeBlessingService#resolveBlessings` concatenates blessings
  from up to three sources into one flat list). **Prefer the granting enum constant's own
  `.name()`** over a hand-written string literal wherever it's already in hand — see
  `ArtesInteraction`/`GritoDeGuerraVulcanoInteraction`/`InitiativeAdvantage
  .POSICIONAMENTO_ESTRATEGICO` for the pattern — so the two can never drift apart. A literal
  constant is still correct where no such enum exists (e.g. a test double).
- **Initiative-win trigger**: `return List.of(new Blessing(...));` from the overridden
  `resolveInitiativeBlessings()`. Nothing else to wire — `InitiativeBlessingService`/`Scene
  #applyInitiativeBlessings` already pick it up generically.
- **Activation trigger**: set `InteractionResult.builder().blessings(List.of(...)).build()`
  (or `.toBuilder()` if extending a base result — see `ArtesInteraction`'s own `applyTo`
  override) inside the relevant `<X>Interaction`. If more than one `Blessing` applies at once
  (e.g. two Vantagem clauses plus a Defesas one), list them all — `blessings` is a `List`
  specifically to support this, don't invent a separate field per bonus. If the trigger is a
  Título ability's own activation (an `AventyrTitleAbility`/`AventyrTitleSpecialization`
  constant, not a Perícia roll), also set that constant's own `interactionClass` field to
  `Optional.of(<X>Interaction.class)` — `AventyrTitleAbility#getInteractionClass()` is the
  declared, checkable bond between the constant and the Interaction reporting its `Blessing`s;
  see `adding-a-title-specialization`'s own step 2/3 notes on this field.
- If **any** clause of the ability can't be expressed as a `Blessing` at all yet (step 5's trap,
  or a mechanism this core has no `ModifierType`/`TemporaryBonus` equivalent for), TODO that
  clause directly on the Interaction/enum constant rather than skipping the whole ability —
  report whichever clauses *are* real (see `GritoDeGuerraVulcanoInteraction`'s own class
  javadoc for the shape of a partial implementation).

## 7. Write tests

- The `Blessing`'s own fields: `modifierType`/`value`/`rounds`/`scope`/`source` match the
  rules text, including `source` matching the granting enum's own `.name()`.
- **Initiative-win trigger**: add cases to `InitiativeBlessingServiceTest`/the relevant
  `<Interface>Test` (e.g. `InitiativeAdvantageTest`) confirming only the intended constant
  resolves a blessing, and every sibling constant still resolves `List.of()`. If this is a
  genuinely new scanning path (not just a new constant on an existing one), add `SceneTest`
  coverage mirroring `applyInitiativeBlessingsGrantsAnAllyScopedBlessingToTheWinnerAndItsAllies`/
  `applyInitiativeBlessingsGrantsASelfOnlyBlessingOnlyToTheWinner`.
- **Activation trigger**: a `<X>InteractionTest` asserting the reported `blessings` list — see
  `GritoDeGuerraVulcanoInteractionTest`/`ArtesInteractionTest`'s DOM_BARDICO cases for the
  shape (including the "ability not held → `blessings` stays `null`" case, and, for
  DOM_BARDICO's own kind of value-depends-on-the-roll ability, the "roll didn't clear the
  threshold → no `Blessing` reported" case).
- A `Blessing` that's currently inert (step 5's `ModifierType`-with-no-consumer-yet case) still
  gets tested for real — assert it's reported correctly, same as any other field; the inertness
  is a fact about the *consuming* side, not a reason to skip testing the *granting* side.

## Reference files to read first

- `src/main/java/org/aventyrs/core/sheet/Blessing.java` — the value object itself.
- `src/main/java/org/aventyrs/core/sheet/TargetScope.java` — the five constants and when each
  applies.
- `src/main/java/org/aventyrs/core/skill/artes/ArtesInteraction.java` (`ArtesInteractionTest.java`)
  — the roll-time-activation trigger, `TargetScope.ALLIES`, a single `Blessing` whose value
  depends on the roll's own `reachedDifficultyLevel`.
- `src/main/java/org/aventyrs/core/title/santo/GritoDeGuerraVulcanoInteraction.java`
  (`GritoDeGuerraVulcanoInteractionTest.java`) — the Título-Habilidade-activation trigger,
  `TargetScope.SELF_AND_ALLIES`, three `Blessing`s reported at once including one
  currently-inert `ModifierType`.
- `src/main/java/org/aventyrs/core/ego/InitiativeAdvantage.java` (`POSICIONAMENTO_ESTRATEGICO`,
  `InitiativeAdvantageTest.java`) — the initiative-win trigger.
- `src/main/java/org/aventyrs/core/character/services/InitiativeBlessingService.java`/
  `InitiativeBlessingServiceImpl.java` (`InitiativeBlessingServiceTest.java`) — the three-source
  scan for the initiative-win trigger.
- `src/main/java/org/aventyrs/core/scene/Scene.java` (`applyInitiativeBlessings`,
  `SceneTest.java`) — how an initiative-win `Blessing` actually gets applied/revoked.
- `src/main/java/org/aventyrs/core/title/santo/AbencoadoPelaLuzInteraction.java` — the
  direct-mutation, single-known-recipient shape that does **not** use `Blessing` at all,
  included here specifically so it's easy to tell apart from the activation-trigger case above.
