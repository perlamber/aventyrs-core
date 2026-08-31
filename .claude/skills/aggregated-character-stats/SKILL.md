---
name: aggregated-character-stats
description: This skill should be used for any work on a Character-level counter that is a fixed base plus an ability-summed total — Reações (`ReactionsService`), Ações Livres (`FreeActionsService`), Pontos de Ação (`ActionPointsService`), Iniciativa (`InitiativeService`), Movimento Base (`MovementService`) — including their per-Round `CombatantSheet` overloads, the `ActionProfile` adjustment hooks (`adjustActionPoints`/`adjustReactions`/`adjustFreeActions`/`adjustSkillRollCost`), the three-source `@Modifier`/`ModifierType` scan, adding a new such counter, or wiring a `@Modifier` method for `REACTIONS`/`MOVEMENT`/`ACTION_POINTS`/`INITIATIVE`. Also use it when asked why `Character` holds only a plain counter, why the `Character` and `CombatantSheet` overloads don't cascade, or why a fixture template uses `CONSCIENCIA_DEFENSIVA`.
---

# Character-level stats aggregated from abilities

Some Character-level counters need a fixed base value *and* a fully-modified total summed
from abilities — this is what `ReactionsService`, `FreeActionsService`,
`ActionPointsServiceImpl.getMaxActionPoints`, `InitiativeService`, and `MovementService` all do.
Don't compute the modified total inside `Character` itself (it would need to instantiate
`ModifierResolverImpl` directly, which doesn't belong on a data class). CLAUDE.md's "Recurring
conventions" — especially **the three-source scan** — apply throughout.

- `Character` holds only the plain fixed counter (e.g. `reactions`/`freeActions`, a normal
  `@Builder.Default` field with Lombok's regular getter — no suppression, no manual method),
  defaulting to a constant declared on the **service** interface
  (`ReactionsService.DEFAULT_REACTIONS`, mirroring `ActionPointsService.DEFAULT_ACTION_POINTS`
  and `FreeActionsService.DEFAULT_FREE_ACTIONS`).
- A dedicated `<Stat>Service`/`<Stat>ServiceImpl` (e.g. `ReactionsService.getTotalReactions
  (Character)`) takes a constructor-injected `ModifierResolver` (default `new
  ModifierResolverImpl()`, same DI convention as every other service) and sums
  `@Modifier`/`ModifierType` bonuses across **three** sources:
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
  This service only computes the fixed Ego-plus-modifiers component; a caller still adds
  whichever dice roll they applied on top before handing the total to `Scene#addParticipant`
  (see the `scene-context-and-positioning` skill).

Mirror this shape for any new stat abilities/competencies/excellencies can modify, and
remember to give the new `ModifierType` constant a `@Modifier`-annotated method on whichever
concrete ability/excellency should affect it (e.g. `AttentionExcellency.FOCADO` for
`REACTIONS`). No concrete ability grants `ModifierType.INITIATIVE` yet — `InitiativeService`
was built ahead of a first consumer, same as `ReactionsService` once was. The two existing
rules-text mentions of a flat +2 Iniciativa (`AttentionExcellency.LENDA`, `MeioElfo`'s Provar
Seu Valor) are each still TODO'd on their *own* missing system first — a graduation-threshold
trigger for the former, a "game session" concept for the latter's "1x por sessão" — so a plain
`@Modifier(ModifierType.INITIATIVE)` method on either wouldn't match its rules text yet.

## What a combatant has *this Round* — the `CombatantSheet` overloads, and `ActionProfile`

The aggregations above answer "what does this character permanently have". PA, Reações
and Ações Livres also have a *per-Round* answer, which is a different number, and each of the
three services carries a second and third overload for it:

| | permanent | per-Round |
| --- | --- | --- |
| PA | `getMaxActionPoints(Character, turn)` | `getMaxActionPoints(CombatantSheet, turn[, SceneContext])` |
| Reações | `getTotalReactions(Character)` | `getTotalReactions(CombatantSheet, turn[, SceneContext])` |
| Ações Livres | `getTotalFreeActions(Character)` | `getTotalFreeActions(CombatantSheet, turn[, SceneContext])` |

- **The `Character` overload is not a shorter form of the sheet one.** It's a genuinely
  different question with a different input, so it does *not* cascade-delegate the way
  `applyTo`'s overloads do — it has no sheet to read a `TemporaryBonus` from and (for
  Reações/Ações Livres) no turnNumber to apply a profile with, and that's deliberate, not a
  gap. Only the two *sheet* overloads cascade into each other, the 2-arg passing `null` for
  `SceneContext`. Prefer the sheet form for anything combat-facing.
- **Two things only the sheet overloads see**: `getTemporaryBonus(ACTION_POINTS/REACTIONS/
  FREE_ACTIONS)` — so a `Blessing` typed to any of the three is live, where it used to be
  inert — and, via `SceneContext#isCombatScene`, whether this is a Cena de Combate. A `null`
  `SceneContext` reads as "not a Cena de Combate", never as an error.
- **The `ActionProfile` adjustment is always applied last, and always exactly once.** Each
  `<Stat>ServiceImpl` computes an *unclamped* permanent baseline in a private helper, adds the
  sheet's `TemporaryBonus`, hands the result to the profile, and clamps at 0 once at the very
  end. That ordering is what makes a profile's *denial* clauses real rather than
  out-summable: `MOVIMENTO_PLANEJADO` returns exactly 0 Ações Livres on Turn 0 and
  `CALCULISTA` exactly 0 PA on Round 0, however many an ability or a `Blessing` granted.
  Don't clamp the baseline separately, and don't apply the profile inside a helper the other
  overload also calls.

`ActionProfile` (`org.aventyrs.core.action`) carries **three** parallel adjustment hooks —
`adjustActionPoints`, `adjustReactions`, `adjustFreeActions` — each in the usual cascading
pair, the 2-arg form delegating down with a `null` `SceneContext` and the 3-arg holding the
logic, so a constant body always overrides the **3-arg** one even when it ignores the context
(`MOVIMENTO_PLANEJADO` does). `adjustSkillRollCost` deliberately has **no** `SceneContext`
overload — no profile conditions a roll's cost on the Scene, so widening it would be building
for a hypothetical consumer.

Four of the six profiles are fully real: `IMPULSIVO`/`CALCULISTA` (PA by Round),
`MOVIMENTO_PLANEJADO` (0 Ações Livres on Turn 0, +1 from Turn 1 on), `REFLEXOS_RAPIDOS` (+1
Reação every Round, *not* gated on combat) and `ESTRATEGISTA` (-1 PA / +1 Reação / +1 Ação
Livre, all three gated on `isCombatScene`). `CONSCIENCIA_DEFENSIVA` is the one still TODO'd,
and `adjustReactions` is the **wrong hook** for it, not a missing one — its clause exempts
specific movements from *provoking* Reações, which is the movement-triggers-Reação gap, and
never changes how many a character *has*.

Because it adjusts none of the three, `CONSCIENCIA_DEFENSIVA` is also what
`MonsterTemplate.DEFAULT_ACTION_PROFILE` and `CharacterFixture`'s templates now use.
**Both used to be `REFLEXOS_RAPIDOS`, chosen back when that constant was inert** — once it
started granting a Reação for real, every fixture-built Character and every foe whose stat
block is silent would have silently carried an extra one. Worth remembering when picking a
placeholder constant: "does nothing today" is not a stable property.

## Movimento Base — `MovementService#getMovementBase(Character)`

Follows the `InitiativeService` variant of the aggregated-stat shape (no new `Character`
field, no `CharacterFixture` change). Base is derived — `SizeCategory.getMovementPerActionPoint()`
(via `CharacterSizeService#getEffectiveSizeCategory`, so size-shifting is reflected) — plus the
usual `ModifierType.MOVEMENT` three-source sum, using `SkillCompetencyAbility.allFor` so racial
abilities count (unlike `ReactionsService`/`InitiativeService`, which predate that fix). Floored
at 0. Returns the **permanent** figure only; a caller wanting this Round's actual one adds
`CharacterSheet#getTemporaryBonus(ModifierType.MOVEMENT)`.

**It is a figure *per Ponto de Ação*, not a whole-Turn allowance**, which is why it takes no
`turnNumber` and never touches `ActionPointsService`. A character spends their Pontos de Ação
across moving *and* everything else they do that Turn, and the split is the player's choice at
the table — so multiplying by `getMaxActionPoints` here would bake in an assumption this core
has no business making. A caller that wants distance covered multiplies this by however many
points were actually spent moving. `MovementServiceImpl` used to multiply, and its constructor
took an `ActionPointsService` for it; both are gone.

That per-point reading is also what a `MOVEMENT` bonus means, and it lands differently
depending on the rules text granting it. "Seu Movimento Base aumenta em +2UD"
(`AtaqueCorpoACorpoExcellency.FOCADO`, `AtletismoCompetencyAbility.PASSO_LARGO`,
`DexterityAbility.PASSOS_LONGOS`, `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO`) lands
exactly — every point spent moving is worth that much more. `SorteAdvantage.AS_NA_MANGA`'s
"você pode se mover até 2UD" is a **one-shot step**, and as a `MOVEMENT` `TemporaryBonus` it
over-grants once its holder spends more than one Ponto de Ação moving; that's flagged on the
constant and deliberately kept, since a one-shot movement allowance is a mechanism this core
doesn't have and granting nothing would be further from the clause. Check which of the two
shapes a new grant is before reaching for `ModifierType.MOVEMENT`.

Vertical/swim movement (`AtletismoCompetencyAbility.ALPINISTA_VELOZ`/`ANFIBIO`) and a
mount's own movement (`DirigirECavalgarExcellency`) are a **different** sub-stat — don't wire
them into `ModifierType.MOVEMENT`.

## Reference files to read first

- `src/main/java/org/aventyrs/core/character/services/ReactionsService.java` /
  `ReactionsServiceImpl.java` — the canonical 3-source shape.
- `src/main/java/org/aventyrs/core/character/services/FreeActionsServiceImpl.java` /
  `InitiativeService.java` / `MovementService.java` / `MovementServiceImpl.java`.
- `src/main/java/org/aventyrs/core/action/ActionPointsService.java` /
  `ActionPointsServiceImpl.java` / `ActionProfile.java`.
- `src/main/java/org/aventyrs/core/character/EgoValue.java` — Iniciativa's `base + variable`.
- `src/test/java/org/aventyrs/core/character/fixture/CharacterFixture.java` — the `BLANK` template `Rule`
  and its `loadCharacterTemplates` comment.
