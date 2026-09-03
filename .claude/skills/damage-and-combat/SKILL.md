---
name: damage-and-combat
description: This skill should be used for any work on combat resolution, damage, or the sheet type hierarchy — `AttackDelivery`/`AttackReceiver` (the two mirrored entry points), `DamageBase`/`DamageBaseService` (the odometer scale, `getDamageBase(Character, Weapon|SkillType)`), `AttackRangeService`/`Weapon#getRange`/`Range#increasedBy`/`Feat#resolveAttackRangeIncrease` (an attack's maximum distance from the Weapon or Spell, widened by Talentos — `ArtilhariaFeat#TIRO_LONGO`), `DamageService` (RD/RA/half-damage mitigation order, `calculateFinalDamage`, `applyDamage`), `HitPointsService#getStatus`/`getMaxHitPoints` and `CharacterStatus` being derived-not-stored, ally-facing RA scans (`resolveAllyAbsoluteDamageReduction`, `sumAllyGrantedAbsoluteDamageReduction`), `SceneContext`-conditioned RA/half-damage hooks, `CombatantSheet` vs `CharacterSheet` vs `MonsterSheet`, `lifeMultiplier`/`ModifierType.HIT_POINTS`, or `CriticalEffect`. Also use it when asked why a `DamageBase` scale-up isn't a `DamageBonus`, why monsters can't level up, why `CharacterStatus` isn't stored, why an attack's range isn't a character stat, or why RA is scanned rather than granted.
---

# Damage, combat resolution, and the sheet hierarchy

`org.aventyrs.core.combat` + the damage half of `org.aventyrs.core.character.services`.
CLAUDE.md's "Recurring conventions" apply — especially **the three-source scan**, **build for
the second real consumer**, and the recompute-on-demand discipline.

## Two kinds of sheet — `CombatantSheet`, and why monsters can't level up

A combat participant is a `CombatantSheet` (`org.aventyrs.core.sheet`), not a `CharacterSheet`.
Two implementations exist, both extending `AbstractCombatantSheet`, which holds every shared
behaviour exactly once:

- **`CharacterSheet`** — a player character. Adds `player`, the experience wallet
  (`totalExperience`/`unUsedExperience`/`useExperience`/`accumulateExperience`) and Fama.
- **`MonsterSheet`** (`org.aventyrs.core.monster`) — a foe. Adds the four authored stat-block
  numbers a foe presents *because it never rolls*: `physicalDefense`/`magicDefense` (what a
  player's Ataque roll must beat) and `attackDifficulty`/`attackBonus` (what its own attacks
  present to a player's Esquiva e Aparar roll).

Everything else — damage, shields, Mana/Determinação, temporary Ego points, `TemporaryEffect`s,
inventory, the Turn lifecycle, `receiveInteraction` — is on the shared half and behaves
identically for both. **Type combat-facing signatures as `CombatantSheet`**; the only things that
should still name `CharacterSheet` are the four XP-spending services plus
`MoralHerdadaAbility#applyStartingFama` and `RestService#applyRest`.

- **That split is the enforcement mechanism, and it's the whole reason to prefer an interface
  here.** Experience lives on `CharacterSheet`, so `CharacterAttributeService#upgradeBase`,
  `SkillGraduationService#upgradeGraduation`, `FeatService#grantFeat` and
  `TitleAbilityService#grantTitleAbility` keep taking that concrete type and a monster cannot be
  passed to one. There is deliberately **no `isMonster()` flag and no runtime guard** — the
  compiler refuses it. `MonsterSheetTest` asserts this reflectively, because writing it as
  `monster instanceof CharacterSheet` doesn't compile, which is the point.
- **Ego points are on the shared half**, despite reading as player-facing: `Primor` applies to a
  *target*, so leaving them off `CombatantSheet` would break it against a foe. `EgoDomain`'s own
  javadoc says "a creature". Both pools live there — see the `ego-point-pools` skill.
- **`Character` is shared too** — a foe's Attributes, Perícias, abilities and equipment are an
  ordinary `Character`, with `race` set to the single catch-all `Monstruoso` and `player` left
  `null`. Don't build a parallel `Monster extends Character`; the stat-carrying half was never
  player-specific.
- **`lifeMultiplier`/`determinationMultiplier` are `Character` fields**, mirroring the
  `manaMultiplier` that already was one. This lets a foe's PV budget be tuned apart from its
  Vigor — previously the only way to make something tanky also inflated every Vigor-governed
  roll. They're not monster-only; a GM house rule uses them the same way.
- **`ModifierType.HIT_POINTS` is the *flat* PV grant, and is not interchangeable with
  `LIFE_MULTIPLIER`.** `HitPointsService#getMaxHitPoints` is `BASE_HIT_POINTS + Vigor.getTotal() *
  lifeMultiplier + getHitPointsBonus(character)`. Use the multiplier when bulk should scale with
  Vigor; use `HIT_POINTS` when rules text states an amount ("recebe Bônus Mágico de +10PV") — a
  stated number expressed as a multiplier uplift only lands correctly at one specific Vigor.
  `getHitPointsBonus` scans `attributeAbilities` **and** `SkillCompetencyAbility.allFor`;
  `getLifeMultiplier` deliberately still scans only the former. Neither scans `SkillExcellency`.

## Both directions of an attack — `AttackReceiver` and `AttackDelivery`

The player always rolls, so a foe contributes a fixed number whichever way an exchange runs, and
`org.aventyrs.core.combat` has **two mirrored entry points** rather than one:

| | Foe attacks the player | Player attacks a foe |
| --- | --- | --- |
| Entry point | `AttackReceiver.resolve` | `AttackDelivery.resolve` |
| Player rolls | Esquiva e Aparar | a Perícia de Ataque |
| Foe contributes | a GD + flat bonus | a flat Defesa (DF or DM) |
| Critical trigger | the roll's **Falha** Crítica | the roll's **Acerto** Crítico |

Neither ever calls the other. Both are report-only, both roll exactly once (the roll can grant
an Ego point on a critical — the only state it changes; the first-roll-of-Turn check it also
runs is non-mutating now), and both assemble the same pre-wired `Damage → Correntes → Críticos`
chain onto the result's `nextInteraction`. **Neither records the action** — the API calls
`CombatantSheet#recordAction` after `resolve` returns, using
`getAttackResult().getGoverningAttributeDomain()` and the `AttackSource`/`ActionCost` it supplied.

- `CriticalEffect#validateCriticalHit` demands an *Acerto* Crítico, which is correct for
  `AttackDelivery` and **still awkward for `AttackReceiver`**, where the trigger is a Falha
  Crítica — a caller there has to construct the Efeito with a value describing something that
  didn't happen, and the Maior/Menor severity it should inherit from the defence roll is picked by
  hand. That translation is still missing; don't assume the offensive direction fixed it.
- **`AttackDelivery` merges the attacker's Talentos' Efeitos Críticos** into the chain —
  `Feat#resolveExtraCriticalEffects(attacker, attackSkill, attackSource, criticalResult)` returns
  a `List<CriticalEffect>` (`AssassinoFeat#ABRIR_FERIDAS` → a `Sangramento`), concatenated with
  the caller-supplied `DeliveredAttack#getCriticalEffects()` and then filtered by
  `CriticalEffect#applicableTo`. `AttackReceiver` has no equivalent — `IncomingAttack` carries no
  attacker to scan.
- **The defeat trigger is `DefeatBlessingService`, not in either entry point.** Nothing reports
  "this blow was fatal" back to the attacker, so the caller checks its own
  `DeliveredAttackResult` / `HitPointsService#getStatus` and calls
  `applyDefeatBlessings(attacker, defeated, viaCriticalHit)`, which scans
  `Feat#resolveDefeatBlessings` and applies each `Blessing` on the attacker
  (`SANGUE_QUENTE`/`VIOLENCIA_DESCOMUNAL`/`ARCANISMO_AVASSALADOR`).
- The Corrente margin is inverted for `AttackReceiver` (attack beats defence by 5, or 7 vs
  `RESOLUTO`) — an inference from `EffectChainService`'s own text, flagged on the class.
- `AttackDelivery` **reports but does not apply** the attacker's `difficultyReduction`: it's
  denominated in níveis and a foe's Defesa is a flat integer, with no defined conversion. TODO'd
  on the class rather than guessed at. `AssassinoFeat#SAQUE_RELAMPAGO`'s "-1 nível" (via
  `Feat#resolveAttackCostDifficultyReduction`, gated on the roll's `ActionCost` and the
  per-Rodada action log) is the first authored clause that lands here — it flows into
  `getUnappliedDifficultyReduction()` on this path, and applies for real on the direct
  skill-roll path and via `AttackReceiver` (`DifficultyLevel#easier`).
- **The `attackTarget`-aware 4-arg `applyTo` lives on `AbstractSkillInteraction`**, gated on
  `isAttackSkill()`, not on `AtaqueADistanciaInteraction`. That was the fix
  `AnoesRacialAbility.ABATEDORES_DE_GIGANTES` needed — its rules text always covered every Perícia
  de Ataque, but melee had no way to see the target.
- **`DeliveredAttack#attackSource` carries what the attack is made with** (a `Weapon` or a
  `Spell`), and `AttackDelivery` hands it to the longest `applyTo` so a delivery-scoped ability
  like `ARREMESSO_PODEROSO` resolves against the real attack — see the
  `ability-acquisition-and-substitution` skill. It's optional, and applies on the
  `attackRoll == null` bonuses-only preview path too. `AttackReceiver` has no equivalent: the
  roll there is the defender's Esquiva e Aparar, and this core models nothing about what the
  *foe* swung.

### Efeito Crítico immunity — `CriticalEffect#applicableTo`

An Anatomia clause naming Efeitos Críticos a creature shrugs off is real, enforced data.
`CriticalEffectType` (`org.aventyrs.core.effect`) is the identity an immunity names, and
`CombatantSheet#getCriticalEffectImmunities()` is the set. It has **two sources, one per kind
of sheet**: `AbstractCombatantSheet` reads the holder's `Race#getCriticalEffectImmunities()`
(empty for every race but `Troll`'s Anatomia Vegetal), and `MonsterSheet` *overrides* that with
its `MonsterTemplate`'s own. A player-*acquired* trait still has no path in.

- **Keyed on an enum, not on `Class<? extends CriticalEffect>`.** Four of the five effects a
  Zumbi resists have no implementation at all; keyed on the enum they're authored, exact data
  now, and correct the day the effect lands.
- **The filter is on `CriticalEffect`, not in either attack entry point** — an immunity is a
  fact about the *victim*, identical whichever direction is running, so both `AttackDelivery`/
  `AttackReceiver` route their `criticalEffects` through the one static method.
- **It filters, it doesn't throw.** An attack that crits against an immune target is still a
  critical hit — it just produces a shorter chain.

## Dano Base is a position on a scale — `DamageBase`, `DamageBaseService`

**Dano Base and a dano *bonus* are different mechanics and never merge.** A `DamageBase`
(`org.aventyrs.core.character`) is the raw `<dice>d6+<value>` an attack starts from; a
`DamageBonus` is a flat number added to an already-rolled total. That's why a character can
attack at a Dano Base of 1d6+0 and still land for 1d6+20 — the twenty is bonuses, not twenty
scale-ups. Never sum the two, and never model a "+N Dano Base" clause as a `DamageBonus` (or
vice versa): a scale-up may be worth a whole extra die.

**The scale is an odometer with an open-ended overflow.** `value` climbs to `MAX_VALUE` (3),
then rolls over — reset to 0, add a die — until `diceCount` reaches `MAX_DICE` (3). Once *both*
are capped, every further scale-up adds a flat +2, forever:

```
0 → 1d6+0   4 → 2d6+0   8 → 3d6+0   12 → 3d6+5
1 → 1d6+1   5 → 2d6+1   9 → 3d6+1   13 → 3d6+7
2 → 1d6+2   6 → 2d6+2  10 → 3d6+2   14 → 3d6+9
3 → 1d6+3   7 → 2d6+3  11 → 3d6+3   (both capped)
```

**`scale` is the only stored component** — `diceCount()`/`value()` are derived, so an
unreachable pairing (2d6+7) simply cannot be constructed, and a negative scale clamps to
`UNARMED` (1d6+0), the bottom rung and literally what an Ataque Desarmado deals. `DamageBase.of
(dice, value)` is the *authoring* factory and validates the pre-overflow region only (1..3 dice,
0..3 value, `INVALID_DAMAGE_BASE` otherwise) — rows past 3d6+3 are only ever reached by scaling
up, never authored. Same "genuine system boundary" validation as `SkillRoll`'s dice.

**`DamageBaseService#getDamageBase`** comes in **two overloads taking two different inputs, not
a cascading pair**: `getDamageBase(Character, Weapon)` for a swing (starting row *and* attacking
Perícia both read off the weapon, via `Weapon#getSkillType()`) and `getDamageBase(Character,
SkillType)` for an Ataque Desarmado (starts at `UNARMED`, and needs the Perícia named because
there's no weapon to name it). Neither delegates to the other. The starting row is then advanced
by three summed sources —

| source | hook |
| --- | --- |
| `character.getFeats()` | `Feat#resolveDamageBaseIncrease(Character)` |
| `SkillCompetencyAbility.allFor(character)` | `resolveDamageBaseIncrease(SkillType, Character)` |
| **only the attacking Perícia's** unlocked tiers | `SkillExcellency#resolveDamageBaseIncrease()` |

Three things about that scan differ from the usual three-source shape, each deliberately:

- **The Excelência source is scoped to the attacking Perícia alone**, not every trained one —
  `AtaqueADistanciaExcellency.FOCADO` must not raise a Corpo-a-Corpo swing's Dano Base. That
  scoping is also why its hook needs no `SkillType` parameter, where the competency one does.
- **The competency source is *not* pre-filtered by the ability's own `getSkillType()`.** An
  ability may raise a Perícia other than its own — `ArtesAprimorarComArteAbility` is an *Artes*
  ability raising the Dano Base of whichever Perícia de Ataque its holder chose. Each override
  checks `attackingSkillType` itself.
- **`AttributeAbility`/`EgoAdvantage` are not scanned and carry no hook** — no constant on
  either grants Dano Base today.

**The weapon is passed, never looked up.** A character may carry several; only the caller knows
which one is swinging. Nothing checks it's equipped — but its *type* is checked, for free: the
parameter is a `Weapon`, so "what does this shield hit for" is unaskable. Both
`AbstractWeapon#damageBase` and `#skillType` are `@NonNull` — a weapon that deals bare-fist dano
says so with `DamageBase.UNARMED`.

**The attacking Perícia is a column of the weapon, not an argument beside it.** There used to be
a third parameter, and it let a caller pair a machado with Ataque à Distância — expressible and
meaningless. The one caller that still names a `SkillType` is the unarmed overload, which
genuinely has nothing to read it off — an Ataque Desarmado isn't assumed to be Corpo a Corpo,
since `ARTISTA_MARCIAL`-style grants and Armas Naturais both reach it.

### `BRUTALIDADE` is the reference, and needs no threshold trigger

`AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE` has all three of its tiers real: a flat +1
dano bonus below 5 Graduações, converting to +1 Dano Base at 5, becoming +2 at 10. **"Convertido"
is exclusive** — `resolveDamageBonus` returns empty from 5 on, so the two halves are never held
at once.

There is deliberately **no** "graduation crossed a threshold" trigger and nothing to migrate,
because neither half is ever *stored*: the bonus is resolved per dano roll and the increase per
attack, both reading the holder's Graduação live. Don't reach for a trigger mechanism when the
value can just be asked for.

Its flat-bonus half is what widened **`SkillCompetencyAbility#resolveDamageBonus`** to a
4-arg overload — `(SkillType attackingSkillType, SceneContext, CombatantSheet attackTarget,
Character actor)` — with the original 2-arg form delegating down with `null`s. `actor` is the
*roller*. `FRIEZA` moved its override onto the 4-arg form and reads neither new parameter — note
it also still doesn't check `attackingSkillType`, so an Ataque à Distância ability's dano bonus
applies to a Corpo-a-Corpo swing; that's pre-existing.

## An attack's maximum range — `AttackRangeService`

Same shape as `DamageBaseService`, one axis over: an attack's max distance is a property of the
**Weapon or Spell**, advanced by the attacker's Talentos. `AttackRangeService#getEffectiveRange`
has two non-cascading overloads — `(Character, Weapon)` → `Range`, `(Character, Spell)` →
`Optional<Range>` (empty for a Pessoal/Toque/Planar/caster-centred reach that names no placed
distance).

- **The authored column.** `Weapon#getRange()` is a `@Builder.Default` of `Range.ADJACENTE` on
  `AbstractWeapon` — *not* `@NonNull` like `damageBase`/`skillType`, because a weapon that never
  states an Alcance genuinely *is* a corpo-a-corpo one and almost no call site consults it (no
  test builder needed touching). `Weapon#getEffectiveRange()` drops to `ADJACENTE` once the
  weapon is destroyed, mirroring `getEffectiveDamageBase()` → `UNARMED`. A Magia's is
  `spell.getTargeting().range()`.
- **`Range#increasedBy(int steps)`** shifts a band up the nearest-to-farthest ladder, clamped at
  both ends. A step is a whole band (a nível/passo de distância), never a UD count — which is why
  the `Feat` hook returns an `int` and there is no `ModifierType.RANGE`, the same reasoning
  `SpellService#getMaxBranchLevel` uses.
- **`Feat#resolveAttackRangeIncrease(Character, AttackSource)`** is the only source scanned, and
  the **first `Feat` hook to take an `AttackSource`** — a range clause is scoped to *how* the
  attack is delivered. `ArtilhariaFeat#TIRO_LONGO` checks `getAttackSkillType() ==
  ATAQUE_A_DISTANCIA` (true of an arco and a ranged Magia alike — its "físicos e Mágicos"
  scope). `null` = "caller didn't say", read as no-match.
- **No ability or equipment source yet** — deliberately. No `SkillCompetencyAbility`/
  `AttributeAbility` constant states an unconditional "+N níveis de distância" (add the hook with
  its first consumer); the offensive Aprimoramento catalog ("Alcance Estendido") doesn't exist,
  and Arco Longo's "Alcance Base muda para" Favor is a *replacement* with no `ModifierType`. Add
  a `getEquipment()` pass when that lands, like `DamageBaseServiceImpl` already has one.
- **Nothing gates an attack on being in range** — `AttackDelivery` and
  `SpellCastingService#validateRequest` never compare `range()` to the target's distance. This
  service answers "how far can they reach", not "did it connect".

## Damage mitigation — `DamageService`

Three layers of mitigation, in a fixed order:

1. **RD (Redução de Dano)** and **RA (Redução Absoluta)** — two independent flat reductions,
   each summed via the standard three-source scan and floored at 0. The only difference:
   `calculateFinalDamage`'s `ignoreDamageReduction` flag skips RD, never RA.
2. **Half damage** — applied *last*, after RD/RA, via the `halfDamage` flag. Rounds down.
3. **Shield points** — absorbed inside `CharacterSheet#applyDamage` itself, after
   `DamageService` computed the post-mitigation amount.

`applyDamage(CharacterSheet, int rawDamage, boolean ignoreDamageReduction)` bridges the two.
It takes **no** separate `Character` — applying damage always needs a concrete sheet to
mutate, so `getCharacter()` always suffices (unlike `RestService.applyRest`, which genuinely
needs both — see the `attribute-graduation-progression` skill).

An ability granting RD *or* RA without a number in its rules text uses
`DamageService.DEFAULT_DAMAGE_REDUCTION` (+2); only deviate when the text states one (e.g.
`APRIMORAR_COM_ARTE`'s "+1 RDS").

RD being real doesn't make every RD-granting ability real — `APRIMORAR_COM_ARTE` grants it as
one branch of a choice, `ProfissaoCompetencyAbility.FORJA_VULCANA` as a per-item choice still
blocked on the missing owned-item copy. Check what's *actually* blocking an ability.

### `CharacterStatus` is derived, never stored

There is **no `status` field on `Character`** and no `updateStatus` mutator. A character's tier
is `HitPointsService#getStatus(CombatantSheet)`, resolved fresh on every call from the damage
currently on the sheet: `getMaxHitPoints(sheet.getCharacter()) - sheet.getDamageTaken()`, handed
to the pure `getStatus(int, int)`.

That subtraction is **unclamped**, deliberately not `getCurrentHitPoints` (which floors at 0) —
the negative range is exactly what distinguishes `FALLEN`/`COMMA`/`DEAD`, so clamping would make
those three tiers unreachable. `HitPointsServiceTest` guards this directly.

It lives on `HitPointsService` rather than on `CombatantSheet`, which holds both halves of the
input and would read more naturally, because **`org.aventyrs.core.sheet` must not depend on
`org.aventyrs.core.character.services`** and resolving a maximum needs `getMaxHitPoints`'s
Vigor/Life-Multiplier scan. Its shape sibling is `getCurrentHitPoints(Character, CombatantSheet)`
— not the cascading-overload convention.

**Why it isn't stored.** A stored copy needs every path that changes Hit Points to remember to
refresh it, and most have no service in scope: `Bleeding`/`Withering` tick inside
`tickTemporaryEffects`, `Sangramento` damages the sheet directly, `RealExecution` applies curse
damage, `CombatantSheet#heal`/`RestService#applyRest` recover it. Before 0.0.18 the field was
stale on six of the eight Hit-Point-mutation paths in `src/main`. `applyDamage` no longer mutates
the `Character` as a side effect at all.

A consumer may still **persist** a tier of its own alongside its stored damage (aventyrs-api
does). That is a boundary denormalization with no core field behind it.

### Ally-facing passive grants are scanned, not granted

An ability whose rules text buffs *someone else* continuously — Santo's Bastião dos Necessitados,
"Aliados adjacentes, apenas aqueles com menos PV que você, recebem RA" — is resolved by
**scanning**, never by handing the recipient a `TemporaryBonus`.

`AventyrTitleAbility#resolveAllyAbsoluteDamageReduction(SceneContext, boolean allyHasLowerPv)` is
the hook; `DamageServiceImpl#sumAllyGrantedAbsoluteDamageReduction` is the scan. It runs in the
opposite direction from every other source in `computeTotalAbsoluteDamageReduction`: those all
start from the target's own traits, this one walks `sceneContext.getAlliesWithin(ADJACENTE)` and
asks each neighbour what it grants outward. `DamageServiceImpl` is the only caller with a
`HitPointsService` in hand, so it resolves the PV comparison and passes it in.

**Why not a grant.** `TemporaryBonus` is a snapshot. Granting one on "an ally came adjacent"
creates a revocation obligation — moved away, died, left the Scene, grantor died — and a
persistence obligation for a value that is pure derivation. Scanned at the moment the recipient's
damage is calculated, the answer is correct by construction as characters move in and out of
range. `BastiaoDosNecessitadosTest#theGrantIsWithheldOnceTheAllyIsNoLongerAdjacent` pins this.

The second consumer that would justify generalizing this shape is Santo's Despertar (the same
thing for Defesas), which additionally needs `Santo#getDefesasBonus(SceneContext)` promoted from
the concrete class to the interface first. Build it when that lands.

### RA/half-damage conditioned on `SceneContext`

`getTotalAbsoluteDamageReduction`/`calculateFinalDamage`/`applyDamage` each have a
`SceneContext`-accepting overload (`Character`-only ones delegate down with `null`), and
`EgoAdvantage` has two matching default-empty hooks —
`resolveAbsoluteDamageReduction(SceneContext)` (summed alongside the reflection-based
`ABSOLUTE_DAMAGE_REDUCTION` scan) and `resolveHalfDamage(SceneContext)` (a boolean, ORed with
the `HALF_DAMAGE` scan being `> 0`). `InitiativeAdvantage#TORRE_EM_MOVIMENTO` overrides both.
Reached end-to-end via `DamageInteraction`'s own matching overloads.

`getTotalAbsoluteDamageReduction(CharacterSheet target, SceneContext)` sums a **third** source:
every held `AventyrTitleAbility`'s
`resolveAbsoluteDamageReduction(SceneContext, boolean hasLowerPvAdjacentAlly)` (see
`SantoAbility#BASTIAO_DOS_NECESSITADOS`). That boolean is a PV comparison neither `SceneContext`
nor a no-arg `@Modifier` can resolve, so `DamageServiceImpl` resolves it once via
`getAlliesWithin(Range.ADJACENTE)` and passes it in.

There is deliberately **no** sheet-less `(Character, SceneContext)` public overload — every
real caller has a sheet by then. The sheet-less `calculateFinalDamage` overload uses the private
`computeTotalAbsoluteDamageReduction(Character, CharacterSheet target, SceneContext)` helper
instead.

## Reference files to read first

- `src/main/java/org/aventyrs/core/combat/AttackDelivery.java` / `AttackReceiver.java`
  (`org.aventyrs.core.combat` tests).
- `src/main/java/org/aventyrs/core/effect/CriticalEffect.java` / `CriticalEffectType.java` /
  `EffectChainService.java`.
- `src/main/java/org/aventyrs/core/character/DamageBase.java` /
  `src/main/java/org/aventyrs/core/character/services/DamageBaseService.java` /
  `DamageBaseServiceImpl.java`.
- `src/main/java/org/aventyrs/core/character/services/AttackRangeService.java` /
  `AttackRangeServiceImpl.java` — the max-range twin of `DamageBaseService`;
  `src/main/java/org/aventyrs/core/scene/Range.java` (`increasedBy`),
  `src/main/java/org/aventyrs/core/item/Weapon.java` (`getRange`/`getEffectiveRange`),
  `Feat#resolveAttackRangeIncrease`, `ArtilhariaFeat#TIRO_LONGO`.
- `src/main/java/org/aventyrs/core/character/services/DamageService.java` /
  `DamageServiceImpl.java` — mitigation order, ally scans, `SceneContext` overloads.
- `src/main/java/org/aventyrs/core/character/services/HitPointsService.java`
  (`HitPointsServiceTest.java`) — `getStatus`, `getMaxHitPoints`.
- `src/main/java/org/aventyrs/core/character/CharacterStatus.java`.
- `src/main/java/org/aventyrs/core/sheet/CombatantSheet.java` /
  `AbstractCombatantSheet.java` / `src/main/java/org/aventyrs/core/monster/MonsterSheet.java`
  (`MonsterSheetTest.java`).
