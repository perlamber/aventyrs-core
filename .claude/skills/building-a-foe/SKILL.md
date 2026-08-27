---
name: building-a-foe
description: This skill should be used when the user asks to "add a new monster", "build a foe", "create a monster/inimigo/criatura", "add a stat block", "add an archetype to GenericMonster", "make a boss/bruiser/caster enemy", or gives a monster's stat block (its Atributos, Perícias, DF/DM, GD de ataque, Categoria de Tamanho). Walks through picking between AbstractMonsterTemplate's fill-in-the-form path and GenericMonster's ready archetypes, authoring the four combat numbers, tuning bulk via lifeMultiplier, and the spawn-independence trap — mirroring org.aventyrs.core.monster as the reference implementation.
---

# Building a foe

A foe lives in `org.aventyrs.core.monster`. It is an ordinary `Character` — Attributes,
Perícias, abilities and equipment — wrapped in a `MonsterSheet` that adds the four numbers a
creature presents *because it never rolls*.

**There is no `Monster extends Character`, and there must not be.** The stat-carrying half was
never player-specific: a foe's `Character` has `race` set to the single catch-all `Monstruoso`
and `player` left `null` (that field is nullable for exactly this reason, and nothing in main
source reads it).

## 0. Pick the path

Two, mirroring the `Item`/`AbstractItem`/`ArmorItem` split this codebase already uses:

| Path | When | Shape |
| --- | --- | --- |
| `AbstractMonsterTemplate` | A **designed** foe — it has a name, a story, a signature trait | `@Builder`; the builder *is* the form |
| `GenericMonster` | **A generic monster on-scene** — the Narrador needs an opponent now, not a designed one | enum catalog of ready archetypes |
| `SummonedMonsterTemplate` | An **invocação** — its numbers depend on who summoned it | its own class in `monster.summon`, e.g. `Zumbi` |

If the user hands you a named creature with abilities or equipment, that's
`AbstractMonsterTemplate`. Only add a `GenericMonster` constant for a genuinely role-shaped
stand-in — and note the existing five are **deliberately unnamed as species**: they're picked by
what the fight needs (something that swings, something at range, something that casts, something
that ends a session), not by monster fiction. A foe with a name and a story does not belong in
that enum.

**`GenericMonster` constants carry no abilities and no equipment**, on purpose — a generic
stand-in with a signature trait isn't generic any more. If your new archetype wants one, it's an
`AbstractMonsterTemplate`.

## 1. Author the four combat numbers

`MonsterSheet` adds exactly four fields to the shared sheet behaviour:

| Field | Meaning |
| --- | --- |
| `physicalDefense` (DF) | the target number a player's Ataque roll must reach to land a physical attack |
| `magicDefense` (DM) | the same, for a magical attack |
| `attackDifficulty` | the `DifficultyLevel` (GD) its own attacks present to a defender's Esquiva e Aparar roll |
| `attackBonus` | a flat modifier on top of that GD's threshold |

**These are authored, not derived, and that's the central design decision.** A stat block says
what a Goblin's DF *is*; it isn't recomputed from its Destreza and Graduação the way a player's
defence roll is. That keeps a stat block readable and tunable by hand, at the cost of the numbers
being free to drift from the Attributes behind them. **Nothing checks them against each other,
deliberately** — don't add validation, and don't try to compute DF from Destreza.

The player always rolls, so the direction of an exchange decides which pair is consulted — see
`AttackReceiver` (foe attacks: contributes `attackDifficulty` + `attackBonus`) and
`AttackDelivery` (player attacks: contributes a flat DF or DM). `MonsterSheet#getDefense`
selects the right column.

## 2. Fill in the form

```java
MonsterTemplate goblin = AbstractMonsterTemplate.builder()
        .name("Goblin Batedor")
        .attributeBase(AttributeDomain.DEXTERITY, 4)
        .attributeBase(AttributeDomain.VIGOR, 2)
        .skillGraduation(SkillType.ATAQUE_A_DISTANCIA, 4)
        .skillGraduation(SkillType.ESQUIVA_E_APARAR, 3)
        .equipmentItem(ArmorItem.ROUPA_PESADA)
        .sizeCategory(SizeCategory.MINUS_ONE)
        .physicalDefense(12)
        .magicDefense(11)
        .attackDifficulty(DifficultyLevel.EASY)
        .attackBonus(1)
        .build();
```

Note the `@Singular` names, which are not the plural field names: `attributeBase`,
`skillGraduation`, `equipmentItem`, plus plain `attributeAbility`/`skillCompetencyAbility`.

### The optional hooks

Four more, all defaulted — only reach for one when the rules text actually states it:

| Hook | Rules text it answers |
| --- | --- |
| `.actionPoints(2)` | "Possuem 2 Pontos de Ação (PA)". Without it a foe gets the standard 3. |
| `.skillSpecialization(TYPE, List.of(…))` | the bracketed tag, `Ataque Corpo-a-Corpo [Primal]` |
| `.undead(true)` | Anatomia de Morto-Vivo. See below. |
| `.criticalEffectImmunity(CriticalEffectType.SANGRAMENTO)` | "Imunes aos Efeitos Críticos …" |

**`isUndead()` is the only vitality classification this core has, and it is narrow on purpose.**
`CreatureType` has HUMANOIDE/FEERICO/MONSTRUOSO and nothing about being alive, so "contra
personagens vivos" is resolved as "not a foe that declared itself Morto-Vivo". That's exact for
every combatant this core can build — a `CharacterSheet` is always living — but don't reach for it
as a general anatomy tag. A construct or elemental that should count as non-living without being a
Morto-Vivo needs the real thing on `Character`, which doesn't exist.

**Immunities are named by `CriticalEffectType`, not by class**, precisely so a stat block can
resist an Efeito Crítico this core hasn't built (four of the five a Zumbi resists have no
implementation). Enforced by `CriticalEffect#applicableTo`, which both `AttackDelivery` and
`AttackReceiver` route through — one filter, both directions. If you add a constant, say in its
javadoc whether anything implements it.

Only `name` is `@NonNull`. Everything else has a sensible default — `sizeCategory` is
`SizeCategory.ZERO`, `attackDifficulty` is `DifficultyLevel.MEDIUM`, the three multipliers take
their `*Service.DEFAULT_*` constants, and the defences default to 0. **An Attribute omitted from
the map keeps `AttributeValue`'s own default** rather than becoming 0, so list only what the stat
block states.

### Attributes and Graduações are uncapped, by construction

A player's Attribute `base` caps at 5 and a Graduação at twice the governing Attribute's base.
A foe is exempt from both — and this needed **no exception mechanism at all**: those caps are
enforced only on the XP-spending services (`CharacterAttributeService#upgradeBase`,
`SkillGraduationService#upgradeGraduation`), which take a `CharacterSheet`. A `MonsterSheet`
isn't one, so a monster can't reach either entry point. `GenericMonster.ABERRACAO`'s Vigor 12
and Graduação 14 are both far past any player ceiling and need nothing special.

If you find yourself wanting a guard or an `isMonster()` flag: **there isn't one, and the
compiler is the enforcement.** `MonsterSheetTest` asserts this reflectively, because writing it
as `monster instanceof CharacterSheet` doesn't compile — which is the point.

### Tune bulk with `lifeMultiplier`, not Vigor

`lifeMultiplier`/`manaMultiplier`/`determinationMultiplier` are `Character` fields. Reach for
`lifeMultiplier` when a foe should be *tanky* — inflating Vigor instead would raise every
Vigor-governed roll along with its PV. `ABERRACAO` pays for its bulk with a `lifeMultiplier` of
8 rather than yet more Vigor. They're not monster-only; a GM house rule uses them the same way.

## 2b. If it's a summon

A creature whose stat block reads "se você possuir N ou mais Graduações em Domínio do Mana"
implements `SummonedMonsterTemplate` and gets **its own class** in `org.aventyrs.core.monster
.summon` — `Zumbi` is the reference. Not a `GenericMonster` constant, and usually not an
`AbstractMonsterTemplate` either: it carries per-instance choices (the Conjurador's Graduação, and
often one of its own like a Zumbi's corpse size) that a constant can't hold.

**The parameter is a plain `int`, never a summoner entity.** Every clause of this shape keys off
one number, so that number is the parameter. `0` means nobody summoned it — the Narrador placed it
for narrative reasons, or typed a number into a form — and is a real value, not a null stand-in.

```java
MonsterSheet raised  = Zumbi.builder().build().spawn(necromancer);  // reads their DdM Graduação
MonsterSheet fromGm  = Zumbi.summonedBy(7).spawn();                 // a number off a form
MonsterSheet loose   = Zumbi.builder().build().spawn();             // the inherited no-arg spawn
```

Three rules for the tier clauses:

- **Fold them in at build time, don't patch the sheet.** `withConjurador(int)` returns a *copy*;
  `spawn()` then reads the already-adjusted numbers. The Attributes, the ability and the authored
  attack bonus are all read off the template before the `Character` exists, so there is no "after"
  to patch. The exception is anything that lives on the *sheet* rather than the `Character` — a
  `LifeSteal`, a `TemporaryEffect` — which an overridden `spawn()` applies once the sheet is built.
- **Tiered numbers need an instance-based ability**, following `ArtesAprimorarComArteAbility`:
  `ModifierResolver` invokes `@Modifier` methods *on the source instance*, so a no-arg annotated
  method can return an instance field. The `ModifierType` stays compile-time-fixed; only the value
  varies. See `ZumbiAbility`.
- **Check whether a clause needs two consumers.** "Bônus em Perícia de Ataque" raises the
  creature's own Ataque roll (a `@Modifier` on the ability) *and* the threshold its attack presents
  when a defender rolls (the template's `getAttackBonus()`). One clause, both directions.

A flat "+NPV" clause is `@Modifier(ModifierType.HIT_POINTS)`, **not** a `lifeMultiplier` uplift —
a stated amount expressed as a multiplier only lands right at one specific Vigor.

## 3. Everything else is already shared — don't re-implement it

`MonsterSheet` and `CharacterSheet` both extend `AbstractCombatantSheet`, which holds every
shared behaviour exactly once: damage, shields, Mana/Determinação, temporary Ego points,
`TemporaryEffect`s, inventory (a foe's loot), the Turn lifecycle, and `receiveInteraction`.

Consequences worth knowing before you write anything foe-specific:

- **Equipment works with no wiring.** `DefenseServiceImpl.sumEquipment` and `DamageServiceImpl`
  scan `character.getEquipment()` for DF/DM and RD identically for both sheet kinds, Favores
  included.
- **Ego points are on the shared half**, despite reading as player-facing — `Primor` applies to a
  *target*, so leaving them off would break it against a foe.
- **Type combat-facing signatures as `CombatantSheet`**, never `CharacterSheet`. The only things
  that should still name `CharacterSheet` are the four XP-spending services plus
  `MoralHerdadaAbility#applyStartingFama` and `RestService#applyRest`.

## 4. The spawn-independence trap

`spawn()` returns a **fully independent** `MonsterSheet` each call — its own identity, its own
resource pools, its own mutable state — so spawning one template twice gives two foes that can
be damaged separately.

That independence needs care in exactly one place: **`SkillGraduation` is mutable and
`CharacterSkill#increaseGraduation` mutates it in place**, so each spawn builds its own
`SkillGraduation` instances rather than sharing the template's. Sharing one across spawns would
let one foe's growth raise another's. Equipment and ability lists *are* safe to share — those
are catalog constants — though `spawn()` still copies the equipment list, since
`Character#equipment` is a mutable inventory.

If you override `spawn()` (rarely needed), preserve this. The default implementation on
`MonsterTemplate` already does everything correctly for both paths.

## 5. Write tests

Follow `MonsterTemplateTest`:

- `spawnCarriesTheStatBlockOntoTheSheet` — every authored number reaches the sheet.
- `spawnBuildsACharacterWithTheMonsterRaceAndNoPlayer` — `Monstruoso`, `player == null`.
- `aSpawnedMonstersAttributesAndGraduacoesAreUncapped` — a value past the player ceiling survives.
- `twoSpawnsFromOneTemplateAreFullyIndependent` — damage one, assert the other is untouched, and
  raise one's Graduação, asserting the other's is unchanged. **Always write this one** for a new
  template shape; it's the regression guard for step 4.
- `theLifeMultiplierDecouplesPvFromVigor` — if the foe uses one.
- `anEquippedItemReachesTheSharedScanningServices` — if the foe carries equipment.
- `anAttributeOmittedFromTheTemplateKeepsItsDefault`.

For a summon, add on top: one test per tier boundary *and its control* (Graduação 3 vs 4, 6 vs 7,
9 vs 10 — the off-by-one is the whole risk), that `withConjurador` leaves the original template
untouched, that a Conjurador untrained in Domínio do Mana yields the untiered baseline identical to
`spawn()`, and that anything applied to the *sheet* (a `LifeSteal`) is per-spawn rather than shared.
See `ZumbiTest`.

For a new `GenericMonster` constant, extend `everyGenericMonsterSpawnsWithoutError` (it sweeps
`values()`, so it picks the constant up automatically) and add a `aGenericMonsterSpawnsReadyToUse`
-style assertion if the tier introduces something new.

Behaviour shared with a player sheet is already covered by `MonsterSheetTest` — don't duplicate
damage/shield/effect/turn-lifecycle tests per monster.

## Reference files to read first

- `org.aventyrs.core.monster.MonsterTemplate` — the interface, the two `*_RACE`/`DEFAULT_*`
  constants, and `spawn()`.
- `org.aventyrs.core.monster.AbstractMonsterTemplate` — the form.
- `org.aventyrs.core.monster.GenericMonster` — the five archetypes and how a tier scales as one dial.
- `org.aventyrs.core.monster.MonsterSheet` — the four numbers and `getDefense`.
- `org.aventyrs.core.monster.SummonedMonsterTemplate` / `org.aventyrs.core.monster.summon.Zumbi` —
  the summon path, and the worked example of tier clauses.
- `org.aventyrs.core.effect.CriticalEffectType` / `CriticalEffect#applicableTo` — immunities.
- `org.aventyrs.core.monster.package-info` — the consumer-facing overview; keep it current if the
  spawning API changes shape.
- `org.aventyrs.core.combat.AttackReceiver`/`AttackDelivery` — where the four numbers are consumed.
