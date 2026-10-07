# Bruxo — caller guide

Bruxo (`docs/rules/titulos.txt`, Centelha Abençoada) is two systems: **Misticismos**, the Árvores the Título teaches and
that are cast with PD, and **invocations**, what the Bruxo's summons gain. Built in core 0.1.2. Table rulings
2026-10-05.

## Misticismos

A Misticismo Magia is a `MimetizedSpell`, so it follows the mimicry path: no XP, no Arcanista tree slot, no
climb foothold, and `SpellFamiliarity#canCast` counts it.

- **Picks are stored, everything else is derived.** Each trait that teaches a Misticismo (`MisticismoTeacher`, keyed
  by its `name()`) holds one tree:
  - `Bruxo#getPendingMisticismoTeachers()` lists the picks still owed;
  - `#getMisticismoOptions(teacher)` lists what each may teach;
  - `#chooseMisticismo(teacher, tree)` records a pick, which is then locked.

  Pacto de Conjuração also owes `#choosePactoTrees(two trees)`. Prompt for these after the Título and after each trait
  is granted.
- **Castable set:** `Character#getMimetizedSpells()` includes every Magia of each Misticismo down to
  `#getMisticismoCeiling(tree)`, at `#getMisticismoCost(level)` PD:

  | Rung | Habilidades de Bruxo needed | PD |
  | --- | --- | --- |
  | Semente | — | 0 |
  | Broto | 2 | 1 |
  | Muda | 5 | 2 |
  | Emergente | 7 | 3 |
  | Florescente | a Pacto tree, with Pacto de Conjuração held | 5 |

  Both ramificações are castable.
- **Cast:** call `MimetizedSpellCastingService#cast(sheet, spell)`, which spends the PD, then `castSpell` for the rolls
  and effect. This is the existing mimicry flow.
- **O Grande Bruxo:** call `#resolveHitPointCost(sheet, spell)` and offer the PV option only when it is present. Then
  call `cast(sheet, spell, true)`. It pays PV = 1 + PD, once per Rodada, while Bruxo is the Primário. Only a Descanso
  Verdadeiro recovers that PV.

"Habilidades de Bruxo" (`#getBruxoAbilityCount`) counts Habilidades and Supremas, never Especializações. Each
Especialização's scalings count only its own (`#getAbilityCount(specialization)`).

## Invocations

1. Plan the Magia as usual (`NatureInvocationService#plan*`).
2. Run `#enhance(plan, caster, options)`. `InvocationOptions` carries:
   - Invocação Maior's `extraTimeForLife` (+1PA, Multiplicador de PV +3);
   - `boostedAttribute` (+2PD, Força or Destreza +4);
   - `doubled` (Invocação Dupla, +2PA, two creatures, half the Duração);
   - `comet` + `openSky` (Nevasca do Sudoeste).

   A choice no held trait permits is refused.
3. Pay `plan.extraActionPoints()` and `plan.extraDeterminationCost()` on top of the cast. Owe the Concentração upkeep
   `plan.concentrationUpkeepMultiplier()` times.
4. Place it (`NatureInvocationServiceImpl.place`, or the API). Persist each creature's `getEnhancement()` and restore
   it with `NatureSummon.restore(kind, grad, powers, enhancement)`.

**Applied by core:**
- Multiplicador de PV, Força/Destreza and the Encantamento ward (with both Invocação Maior effects);
- the GD reductions, RDS, regeneration, Categoria de Tamanho and +2PA;
- the critical changes (Cataclismo, +1d6, Margem Crítica);
- attacks against DM, Fogo Vivo, the Fogo/Sagrado retype (reported on `getRetypedDamage`, as every retype is);
- RM for allies adjacent to an Invocação Maior.

**Dealt by the caller** (`SummonEnhancement`, core 0.1.3 adds the pieces):
- `comet()`: rolled **instead of** the Conjuração roll — `COMET_SKILL` against `cometTargetValue(gd, highest DM)`;
  on success each target and those adjacent take it, and the creature appears there;
- `wings(d6)`: the creature pays `WINGS_ACTION_POINTS` and grants itself the returned flight `Blessing`;
- `damageEquipment(victim, rolled)`: the Raios burst's equipment half;
- Chamas' half damage through a Fogo immunity is core's (`Feat#halvesThroughImmunity`);
- `Bruxo#dismissFamiliar` when the familiar's token leaves.

**Reported for the caller to resolve** (`SummonEnhancement`):
- `comet()`: an Ataque à Distância against the higher of GD and DM, dealt to the targets and their adjacências;
- `profaneAura()`: 3 Profano damage to adjacent characters (not the Bruxo) at each Rodada start;
- `arrivalBurst()`: 1d6 Eletricidade to adjacent enemies on arrival, not cumulative across summons;
- `flightBonusRounds()`: wings for 3PA, flight for 1d6 + that many Rodadas.

Each of the first three is an `ElementalDischarge`; deal it with `dealTo`.

## Familiar Maior

1. **The ritual:** `Bruxo#performFamiliarRitual(character, familiar, egoDomain, egoPointsService)` returns the
   Character rebuilt one permanent Ego point lower. Persist and rebuild the sheet from it. The familiar is bound for
   life. The day the ritual takes is the table's.
2. **In a Cena:** `Bruxo#summonFamiliar(scene, sheet, gm, subordinateService)` places its token as the Bruxo's
   invocation with no Duração. It is commanded as a permanent Subordinado of the chosen benefit, and counts toward the
   Carisma limit.
3. **Limits:** the token cannot attack (`SUMMON_CANNOT_FIGHT`). Keep it within `#getFamiliarLeash()` UD, because this
   core holds no positions.

## Persisting a Bruxo

Store these and restore with `new Bruxo(specs, abilities, misticismoPicks, pactoTrees, familiar)`:
- `#getMisticismoPicks()` (trait name → tree; a `MagicTree` persists by `name()`);
- `#getPactoTrees()`;
- `#getFamiliar()`.

## Readings

See `docs/changelog/0.1.2.CHANGELOG.md`, "Readings".
