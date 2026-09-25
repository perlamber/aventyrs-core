---
name: damage-and-combat
description: This skill should be used for any work on combat resolution, damage, or the sheet type hierarchy — `AttackDelivery`/`AttackReceiver` (the two mirrored entry points), `DamageBase`/`DamageBaseService` (the odometer scale, `getDamageBase(Character, Weapon|SkillType)`), `AttackRangeService`/`Weapon#getRange`/`Range#increasedBy`/`Feat#resolveAttackRangeIncrease` (an attack's maximum distance from the Weapon or Spell, widened by Talentos — `ArtilhariaFeat#TIRO_LONGO`), `DamageService` (RD/RA/half-damage mitigation order, `calculateFinalDamage`, `applyDamage`), the melee half-Força dano term (`AbstractSkillInteraction#resolveMeleeStrengthDamage`, `StrengthAbility#DESTRUIDOR_DE_MUROS`, `AttributeAbility#upgradesFirstMeleeAttackOfRoundStrengthScaling`), multi-target attacks (`AttackTargetingService`/`Feat#resolveAdditionalTargets`/`DeliveredAttack#additionalTargets`/`DamageInteraction#halvingDamage` — `ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA`), `HitPointsService#getStatus`/`getMaxHitPoints` and `CharacterStatus` being derived-not-stored, ally-facing RA scans (`resolveAllyAbsoluteDamageReduction`, `sumAllyGrantedAbsoluteDamageReduction`), `SceneContext`-conditioned RA/half-damage hooks, `CombatantSheet` vs `CharacterSheet` vs `MonsterSheet`, `lifeMultiplier`/`ModifierType.HIT_POINTS`, or `CriticalEffect`. Also covers the **Investida** and movement-provoked Reações — `Manoeuvre`, `SkillRoll#getManoeuvre`, `ChargeService`/`ChargeResult` (its 3PA cost, its ×2 Movimento Base allowance, the drawn-weapon gate, the +2 on a hit and the -2 Defesas on a miss) and `MovementReactionService#getProvokedReactors` (who may react to a movement, and why nothing fires a Reação). Also use it when asked why a `DamageBase` scale-up isn't a `DamageBonus`, why monsters can't level up, why `CharacterStatus` isn't stored, why an attack's range isn't a character stat, why RA is scanned rather than granted, why this core enforces how many targets an attack has but not which ones, why the melee Força term hangs off no `resolve*` hook and doesn't follow an Atributo substitution, or why a charge's movement allowance is a total distance when every other movement figure is per Ponto de Ação.
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
- **`MonsterSheet`** (`org.aventyrs.core.monster`) — a foe. Adds the authored stat-block
  numbers a foe presents *because it never rolls*: `physicalDefense`/`magicDefense` (what a
  player's Ataque roll must beat) and a `SkillDifficulty` (tier + bonus) per Perícia —
  `getSkillDifficulty(SkillType)`, a per-Perícia entry or the general GD. Its attacks present
  `getSkillDifficulty(ATAQUE_CORPO_A_CORPO)` (or `ATAQUE_A_DISTANCIA`) to a player's Esquiva e
  Aparar roll.

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
chain onto the result's `nextInteraction`. **Neither records the action**, but both build it:
`DeliveredAttack`/`IncomingAttack` carry an optional `Scene` (mirroring `SpellCastRequest`'s
"live Scene beside the `SceneContext` snapshot"), and `resolve` bundles the roll as a ready
`CombatantAction` on `DeliveredAttackResult`/`IncomingAttackResult#getRecordedAction()` — skill,
resolved `governingAttributeDomain`, the `AttackSource`/`ActionCost` supplied, `turnNumber` from
`Scene#getCurrentRound()` (0 with no Scene), and the verdict (`AttackReceiver`'s is signed from
the *defender's* side — `succeeded` = defence held, `margin` positive when it held). `null` on
the preview path. The caller files it with one `scene.recordAction(actor, result.getRecordedAction())`
(or `actor.recordAction(...)` with no Scene). `SpellCastingService#castSpell` does the same on
`SpellCastingResult#getRecordedAction()`, partial there (no roll is handed to `castSpell`, so
domain/verdict stay unset; the `Spell` as `attackSource` is the part the "primeira Magia da
Rodada" clause reads back).

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

### A provoking Aura constrains the target — `AuraTargeting`

Both entry points run one shared, package-private gate (`combat.AuraTargeting#resolvePenalty`)
when the request carries a live `scene` **and** an `attacker`: an attacker bound by a
`scene.ActiveAura` whose first attack of the Rodada isn't aimed at the Aura's holder is refused
(`FORCED_ATTACK_TARGET_REQUIRED`), unless the request sets `forcedTargetUnavailable` — "você não
for um alvo válido" is the caller's judgement. After that attack, later ones against anyone else
carry `Skill#DISADVANTAGE_MALUS`, applied where each direction keeps its number: `requiredTotal`
on `AttackReceiver` (the foe's GD drops) and `attackTotal` on `AttackDelivery` (so it reaches
every additional target of that one roll too). Both report it as `getAuraPenalty()`. The gate
judges only the primary `defender`, and learns the holder was attacked only through
`Scene#recordAttack` — `resolve` stays report-only. See the `scene-context-and-positioning`
skill for binding.

### One attack, several targets

`ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA`'s "seus ataques afetam um alvo adicional" is
real, and it runs entirely through `AttackDelivery` — `AttackReceiver` has no counterpart, since a
foe attacking the player is one foe against one roller.

**How many is a character stat; which ones is not.** `Feat#resolveAdditionalTargets(SkillType,
Character)` states how many targets beyond the primary a Talento grants (its own conditions
checked inside the hook — `ARTE_FLUIDA` returns 0 while a non-natural weapon is drawn or a Escudo
is equipped), and `AttackTargetingService#getMaximumTargets` sums that onto `BASE_TARGETS` = 1.
`AttackDelivery#resolve` refuses an attack naming more (`TOO_MANY_ATTACK_TARGETS`) — a cap
enforced on the service entry point that applies it, like every other cap here.

Every clause granting an extra target also requires it to be **adjacent to the primary one**. That
is pairwise geometry between two combatants who are both *not* the roller, and a `SceneContext`
holds distances measured only from its own holder — so nothing in this core can check it.
**Choosing the targets is the caller's (in practice the UI's) job**; this core enforces the count
and nothing else. Don't add an adjacency check here; it belongs with the missing `scene.grid`.

**The roll is one roll.** `AttackDelivery` calls `applyTo` once — rolling twice would double-grant
the critical-success Ego point — and compares the one `attackTotal` against each target's own
Defesa:

| | primary target | each additional target |
| --- | --- | --- |
| carried on | `DeliveredAttack#defender`/`defenseValue` | `DeliveredAttack#additionalTargets` (`List<AttackTarget>`) |
| reported on | `DeliveredAttackResult`'s flat fields | one `DeliveredAttackTargetResult` each, in order |
| margin / hit / Corrente threshold | its own | its own — all three are per-target |
| `CriticalResult` | the roll's | the same roll's; only *whether it landed* differs |
| Efeitos Críticos | filtered against its anatomy | filtered against **its** anatomy |
| chain head | plain `DamageInteraction` | `DamageInteraction#halvingDamage()` |

**The extra targets are a trailing parameter, not a widened `attackTarget`.**
`AbstractSkillInteraction`'s longest `applyTo` is now `(target, sceneContext, skillRoll,
attackTarget, attackSource, List<CombatantSheet> additionalTargets)`. `attackTarget` stays the
**primary** target and remains the one every target-conditioned hook resolves against
(`resolveDamageBonus`, `resolveAttackRollBonus`) — the rules scope the extra targets to the
comparison and the damage, not to the bonuses, and recomputing those per target would need a
per-target result no authored trait asks for. Widening the existing parameter into a list was
tried and rejected: it makes a plain `applyTo(t, ctx, roll, null, null)` call ambiguous.

**The dano roll is one roll too**, so a clause conditioned on *how many* targets there are is a
property of the attack rather than of any target: `Feat#resolveDamageBonus(SkillType, SceneContext,
CombatantSheet, Character, AttackSource, int targetCount)` is the hook, and `ARTE_FLUIDA` returns a
flat `Skill#DISADVANTAGE_MALUS` from it while `targetCount > 1`. `targetCount` is `0` when no
target was named at all (the bonuses-only preview path), which an override reads as "condition not
met" — never as "one target".

**Meio-Dano on the additional target is real mitigation, not a halved input.**
`DamageInteraction#halvingDamage()` (a fluent marker, the sibling of `chainInto`) reaches
`DamageService#calculateFinalDamage(…, boolean halfDamage)`, which **OR**s it into the target's own
`ModifierType.HALF_DAMAGE`/`EgoAdvantage#resolveHalfDamage` sources. So it still applies last —
after RD and RA — and two half-damage sources never quarter. The caller feeds the *same* dano
figure to every chain; the halving happens inside.

## The Investida — a manoeuvre, not a second pipeline

An Investida bundles a movement with an Ataque Corpo-a-Corpo. **No document under `docs/rules/`
defines it** — the nearest thing is `magias.txt`'s Bote Inesperado deferring to "os critérios
padrões" — so every figure carries its provenance on `ChargeService`: 3PA is authored, the ×2
allowance is *read off* `DexterityAbility#IMPLACAVEL`'s "o triplo … ao invés do dobro", and the -2
is confirmed by that same clause calling it "o redutor **padrão**".

**The charge does not have its own attack path, and that is the whole design.** `ChargeService
#begin` claims the movement and answers four questions on a `ChargeResult`; the caller then builds
an ordinary `DeliveredAttack` whose `SkillRoll` carries `Manoeuvre.INVESTIDA` and the resolved
`ActionCost`, and runs it through `AttackDelivery` unchanged. A charge therefore picks up every
Talento, Condição, Forma and Efeito Crítico clause the normal path already resolves, instead of a
parallel pipeline having to re-earn all of them.

| Half of the manoeuvre | Where it lives | Why there |
| --- | --- | --- |
| cost (3PA, `Feat#resolveChargeActionPointReduction`) | `ChargeService#getActionPointCost` | reported, never deducted — no PA economy |
| allowance (×2 Movimento Base) | `ChargeService#getMovementAllowance` | needs `MovementService` plus a multiplier |
| **+2 on a hit** | `AbstractSkillInteraction#resolveChargeDamage` | a property of the manoeuvre, held by nobody |
| **-2 Defesas on a miss** | `ChargeService#applyOutcome` | `AttackDelivery` applies nothing, ever |
| who may react | `MovementReactionService` | the rule is general to *movement* |
| RD during the movement | `ChargeResult` (unapplied) | the window is shorter than a `TemporaryBonus` |

- **`Manoeuvre` rides the `SkillRoll`**, beside `ActionCost`, and lives in `org.aventyrs.core.action`
  rather than `skill` because it names an action both halves must read. Putting it on
  `DeliveredAttack` would reach `AttackDelivery` and not the direct `applyTo` path; widening the
  `applyTo` cascade was rejected outright (see the multi-target section — that cascade is at its
  limit). `null` is "an ordinary attack", never "not a charge".
- **The +2 hangs off no `resolve*` hook**, exactly like the melee half-Força term two sections up
  and for the identical reason: every other `DamageBonus` contributor is something the attacker
  *holds*, while this is true of anyone who charges. Don't go looking for the trait that grants it.
- **It needs no "did it hit" test.** A `DamageBonus` only ever reaches a dano roll, and
  `AttackDelivery` only builds the chain carrying one inside `if (hit)`. A conditional would
  duplicate a decision already made — and be unanswerable on the direct skill-roll path, which
  compares against no Defesa at all.
- **The miss penalty is a *sourced* `Blessing`**, not `grantTemporaryBonus`. A sourceless bonus
  stacks without limit, so two failed charges would reach -4; with a source, `applyEffect` trims by
  (source, `ModifierType`) and the second renews the window instead. Negative-valued, which
  `DefenseServiceImpl` handles — it sums `DEFESAS` additively and clamps nothing, the same way
  `ConditionType.DESPREVENIDO` delivers its own -2. Deliberately *not* modelled as applying
  `DESPREVENIDO`: that condition carries implications (Flanqueado/Caído/Cego confer it) the clause
  does not name.
- **Melee is the `SkillType`, never the `ItemCategory`** — which is what lets an Arma Natural charge
  (Guampo's Chifres Majestosos, the Empalador's Favor both require it). A `null` weapon is an Ataque
  Desarmado and charges fine.
- **The weapon must already be drawn — a gate, not a price.** `WeaponDrawService` is deliberately
  not consulted. The test is membership of `getEquipment()` rather than `treatsAsNaturalWeapon`,
  which exempts a body part for the right reason (nothing to draw) while keeping a *reclassified*
  ordinary weapon (`DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA`) needing to be in hand.
- **The allowance is a total distance**, the one exception to `MovementService`'s per-Ponto-de-Ação
  rule, because a charge is one fixed-cost action. Don't add it to a Movimento Base.

### Movement provokes Reações — the trigger is built, the Reação is not

`MovementReactionService#getProvokedReactors(mover, sceneContext, manoeuvre)`: an enemy threatens
if they hold a **drawn** melee weapon — or an Arma Natural, which cannot be sheathed — whose
`AttackRangeService` band covers the distance to the mover. Ataque à Distância never threatens.

- **Drawn, not equipped**, per "'Utilizando uma arma' means *drawn*". Read natural weapons through
  `CombatantSheet#getNaturalWeapons()` so a Forma is reflected.
- **The band form of `getEffectiveRange` is right here** — this is an `isWithin` question, not a
  measurement, and a `SceneContext` holds bands anyway.
- **The mover's own `SceneContext` is the correct snapshot**, unlike the attacker's snapshot
  `resolveCriticalResistance` is warned about: distance between two combatants is mutual.
- **Nothing fires a Reação.** `ReactionsService` computes a maximum and nothing tracks spent ones,
  so the count is deliberately not consulted and the caller adjudicates. A clause exempting a
  movement is still exempt from nothing that *happens* — cite that, not "no mechanism exists".
- Exemption is `AttributeAbility#exemptsFromMovementReactions(Manoeuvre)`. **No `Feat` twin yet**:
  `MOVIMENTO_ACROBATICO` is scoped to Reposicionar and `CONSCIENCIA_DEFENSIVA` to a movement's first
  2UD, neither of which this core has.

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

**`Feat#resolveDamageBonus` has a trailing-`AttackSource` overload** — same
defaults-to-the-shorter-form relationship as `resolveSkillRollBonus`/`resolveCriticalMarginIncrease`.
`AbstractSkillInteraction#sumDamageBonus` now threads the delivery channel through, so a
Talento clause scoped to *what the attack was made with* is expressible: `MonstruosoFeat#FEROCIDADE`
narrows with `actor.treatsAsNaturalWeapon(weapon)` for its "+1 em rolagens de danos de suas
Armas Naturais". `SkillCompetencyAbility`/`EgoAdvantage` did **not** grow the parameter — add it
there with their first consumer.

## The melee Força term — the one dano bonus nobody holds

**An Ataque Corpo-a-Corpo adds half its attacker's Força to the dano roll**, rounded down.
`AbstractSkillInteraction#resolveMeleeStrengthDamage` resolves it and `sumDamageBonus` folds it
into `DamageBonus#total`'s flatModifier beside the four trait scans.

**It hangs off no `resolve*` hook, and that's the point.** Every other contributor is something
the character *holds* — a Habilidade, a Vantagem, a Talento, a Condição. This one is a property of
the **Perícia itself**, true of anyone who swings, so there is nothing to scan and no interface to
widen. Don't go looking for the ability that grants it, and don't add one.

**Melee only.** `resolveMeleeStrengthDamage` returns 0 for anything but `ATAQUE_CORPO_A_CORPO`.
The corroboration is the Arco Composto, whose authored Favor is "Adiciona Metade da Força aos
Danos Causados" (`docs/rules/equipamentos.txt`) — a Favor that would be redundant if a ranged
attack already had the term. That Favor is *unauthored*, and blocked twice over: an `ItemBonus` is
a flat `ModifierType`-and-value pair and cannot express a derived half-Atributo, and the offensive
weapon catalog doesn't exist (`ItemCatalog` registers `ArmorItem` and `NaturalWeapon` only).

**A dano bonus, never a Dano Base scale-up.** The section above says the two never merge; this is
the clause most likely to tempt you, because "+ Metade da Força" reads like part of a weapon's
damage line. It isn't — the rules' own stat-block notation, `1d6+4 (Base 2 + Metade da Força)`,
names the row and the term as separate quantities, exactly the split this codebase keeps.

**`StrengthAbility#DESTRUIDOR_DE_MUROS` upgrades the Rodada's first attack to the full value**,
via `AttributeAbility#upgradesFirstMeleeAttackOfRoundStrengthScaling()` — the deliberate mirror of
`upgradesFirstSpellOfRoundFocusScaling()`, down to the gate: no attack yet in
`CombatantSheet#getActionsThisRound()`. Three things to know about that gate:

- **"O primeiro ataque" is any attack**, not the first *melee* one. Opening with a bow shot spends
  the upgrade. A non-attack Perícia doesn't.
- **It reads the per-Rodada log, not the per-Turn slice.** `isFirstAttackRollOfTurn()` is a
  different window; this clause names the Rodada.
- **The log is written by the caller.** `applyTo` only reads it, so a consumer that never calls
  `recordAction` leaves every attack looking like the Rodada's first — the standing caveat every
  reader of that log carries, not a bug in this term.

**It stays Força under a substitution.** `ACUIDADE`/`SAGACIDADE_ARCANA` replace the Atributo
governing the *roll* via `getSubstituteAttributeDomain()`; this term's own rules text names Força.
Two statements about two different numbers — a finesse swordsman rolls on Destreza and still adds
half their Força. It reads `AttributeValue#getTotal()`, the permanent value, exactly as
`SpellCastingService#resolvePrimaryDamage` does for its Foco term: a round-scoped
`ModifierType#STRENGTH_BONUS` reaches a Força-governed Perícia roll and nothing else, and a dano
roll is not that roll.

## An attack's maximum range — `AttackRangeService`

Same shape as `DamageBaseService`, one axis over: an attack's max distance starts with the
**Weapon or Spell**, is widened by the attacker's **Categoria de Tamanho**, and is then advanced by
the attacker's Talentos. `AttackRangeService#getEffectiveRange` has two non-cascading overloads —
`(Character, Weapon)` → `Range`, `(Character, Spell)` → `Optional<Range>` (empty for a
Pessoal/Toque/Planar/caster-centred reach that names no placed distance).

- **Size always contributes, in one of two ways** (`docs/rules/categorias-de-tamanho.txt`, Alcance
  column). `Range.ADJACENTE` is *not* a distance — it means no space between the combatants — so
  nothing is added to it: a weapon stating no reach of its own takes `SizeCategory#getRange()`
  wholesale, while a weapon that *does* state one (Lança, arco) has `SizeCategory#getRangeModifier()`
  added, **ranged and thrown included**. That split is what keeps a giant's lança out-reaching its
  own fist. Magias take no size widening.
- **Two forms, and the difference matters.** `getEffectiveRangeInUnidadesDeDistancia` is the reach;
  `getEffectiveRange` rounds it *up* into a band. `Range` is geometric (1/2/4/8/16/24 UD) and the
  size modifier is linear, so most reaches have no band: an adaga at Categoria +4 reaches 3 UD and
  comes back as `DISTANCIA_CURTA`, worth 4. The band stays correct for `isWithin` questions — it is
  the smallest band *covering* the reach — but **anything measuring against a position wants the UD
  form**. Talento steps are band steps, so they compose after the rounding and never reach the UD
  figure.

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

**Two stages also have a timed source, and both need a `CombatantSheet`.** RA sums
`getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)` on top of its four passive sources
(`AbencoadoPelaLuzAbility#GLORIA_RELAMPEJANTE_DE_TESLA`), and the Meio-Dano stage ORs in
`getTemporaryBonus(HALF_DAMAGE)` (`SantoAbility#PROTECAO_UNGIDA`) — read as a flag, so two sources
still halve exactly once and the halving still lands last. The `Character`-only overloads see
neither: they have no sheet to ask.

Three layers of mitigation, in a fixed order:

1. **RD (Redução de Dano)** and **RA (Redução Absoluta)** — two independent flat reductions,
   each summed via the standard three-source scan and floored at 0. The only difference:
   `calculateFinalDamage`'s `ignoreDamageReduction` flag skips RD, never RA.
1b. **RM (Resistência à Magias)** — `getTotalMagicReduction(CombatantSheet)`, added to the same
   flat stage but **only when the caller typed the hit `DamageType.MAGICO`** (`null` means
   "caller didn't say", not "this was magic"). Same five sources as RD — the `@Modifier` scan of
   `ModifierType.MAGIC_REDUCTION`, equipped items, `Feat#resolveMagicReduction`, and a
   `TemporaryBonus` — and skipped by the same `ignoreDamageReduction` flag (an inference; the
   rules name only RA as un-ignorable). **RD is still type-blind**, so a MAGICO hit currently
   takes RD *and* RM; narrowing RD is the damage-type system, not this.
2. **Half damage** — applied *last*, after RD/RA, via the `halfDamage` flag. Rounds down.
3. **Shield points** — absorbed inside `CharacterSheet#applyDamage` itself, after
   `DamageService` computed the post-mitigation amount.

`applyDamage(CharacterSheet, int rawDamage, boolean ignoreDamageReduction)` bridges the two.
It takes **no** separate `Character` — applying damage always needs a concrete sheet to
mutate, so `getCharacter()` always suffices (unlike `RestService.applyRest`, which genuinely
needs both — see the `attribute-graduation-progression` skill).

An ability granting RD *or* RA without a number in its rules text uses
`DamageService.DEFAULT_DAMAGE_REDUCTION` (+2); only deviate when the text states one (e.g.
`APRIMORAR_COM_ARTE`'s "+1 RDS"). A *round-scoped* RD grant (a `Blessing`/`TemporaryBonus` of
`ModifierType.DAMAGE_REDUCTION` — `AnaoFeat#VIGOR_DO_INVERNO`'s combat-start grant) is summed
only on the `CombatantSheet` overloads of `getTotalDamageReduction`, not the `Character`-only
one, which has no sheet to read `getTemporaryBonus` from.

**Resistência a Críticos (RC)** — a *defender-side* narrowing of an attacker's Margem Crítica
Menor. `AbstractSkillInteraction` subtracts the attack target's
`getTotalCriticalResistance(sceneContext)` from the summed `criticalMarginIncrease` before
`SkillRoll#getCriticalResult` (so it reaches `AttackDelivery`, not the `AttackReceiver` mirror —
the attacker rolls nothing there). One instance = `CombatantSheet.CRITICAL_RESISTANCE_INSTANCE`
(2) per `docs/rules/defesas-e-resistencias.txt`, and a clause stating no figure grants one.

**Two kinds of source, summed by the sheet.** A *standing* grant comes from the holder's Raça
(`Race#getCriticalResistance()` — `Troll`'s Anatomia Vegetal) or a held Talento
(`Feat#resolveCriticalResistance` — `MonstruosoFeat#ANATOMIA_INCOMUM`,
`ElementalFeat#TRANSFORMACAO_ELEMENTAL`, `DuelistaFeat#CORACAO_DE_FERRO`,
`SobrevivenciaFeat#PROTETOR_TERRITORIALISTA`); a *round-scoped* one is a `Blessing`/`TemporaryBonus`
of `ModifierType.CRITICAL_RESISTANCE` (`AnaoFeat#VIGOR_DO_INVERNO` at combat start). Read
`getTotalCriticalResistance`, never `getTemporaryBonus(CRITICAL_RESISTANCE)` alone — that sees
only the timed half. There is deliberately **no `SkillCompetencyAbility`/`AttributeAbility` hook**:
no constant asks for one (`ProfissaoCompetencyAbility#FORJA_VULCANA`'s RC is *item*-scoped).

⚠️ The `sceneContext` passed to `getTotalCriticalResistance` at that call site is the
**attacker's** snapshot — the only one in reach, since `SceneContext` holds no `Scene`. An
override may read Scene-*wide* facts from it (`getTerrainType()`, `isCombatScene()`,
`getCurrentRound()`); it must never read proximity (`getAlliesWithin`, `getOpposedCharacter`),
which would answer about the wrong combatant.

Still missing: the "-1 à Margem Crítica Maior" clause (no Maior margin modelled), PRIMORDIAL
scoping, an item-granted RC, and pushing a crit *below* baseline (the net widening floors at 0,
which approximates the "até o mínimo de 17" clamp).

RD being real doesn't make every RD-granting ability real — `APRIMORAR_COM_ARTE` grants it as
one branch of a choice, `ProfissaoCompetencyAbility.FORJA_VULCANA` as a per-produced-item choice
still blocked on the missing *item-scoped* Resistência a Críticos / Margem Crítica Maior /
item-granted-bonus mechanisms (the forge pipeline itself now exists — `EquipmentCraftingService`,
and a *character's* own RC is real). Check what's *actually* blocking an ability.

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

### Healing the fallen — the Coma cap and the Dead gate (0.0.57)

The rules limit healing on fallen characters:
- A character in Coma recovers at most **1PV from each distinct heal effect**, as a total for the
  whole time they are in Coma.
- A dead character is reached by **no** heal unless an Ability says so.

`CombatantSheet#heal(int, HealingSource)` enforces both. `sheet.HealingSource` says which effect a heal
is (its `key`, whether it is `repeatableInComa`, the `healer`, the `spell` or `titleAbility`).
Callers pass the source through its factories:
- `spell` is keyed by *name*;
- `spellChain` shares its Magia's key, so a Corrente is one effect with its Magia;
- `rest` is the one repeatable source: 1PV per real Descanso;
- `regeneration` keys by the instance, so one Regeneração budget is one effect;
- `titleAbility` and `egoSpend`.

**Judged before the heal.** The tier is read *before* recovering. A heal that revives someone is
judged against `DEAD`, so it is not the one the Coma cap trims.

**Refusals leave a Sangramento running**, the same as Feridas Dolorosas: no cure landed. This covers
a dead target, and a used key in Coma.

**The Títulos that bend it are the healer's, never the target's.** `heal` scans
`source.healer().getCharacter().getAllTitles()` for `AventyrTitle#bypassesComaHealingCap` and
`#claimRevival`. The second is a *claim*, not a query: it spends what the permission costs (a Curar
os Mortos charge, `CombatantSheet#consumeRevivalCharge`). `isBeyondRevival()` (set by
`RealExecution`) outranks every claim. See `docs/curandeiro.md`.

**Transitions are bookkept, the tier is still derived.** `sheet.FallenHealingLedger`
(package-private, owned by `AbstractCombatantSheet`) records only *when* things happened:
- the Coma's used keys (cleared on entering or leaving Coma);
- whether the Coma began in this Cena (`hasEnteredComaThisScene`);
- Rodadas since death (`getRoundsSinceDeath`);
- the permanent beyond-revival mark.

It is fed a freshly derived status after every `applyDamage`/`applyCurseDamage`/`heal`, at the start
of every `heal`, and at `startNewRound`/`startNewScene`. So a transition with no PV change (a Forma
or Frenesi ending) is seen late, not missed. A new Cena closes the death window; the used keys
survive it.

**Exempt:** the unsourced `heal(int)` and `healFromLifeSteal`. Every heal the rules name goes through
the sourced overload.

**Test gotcha:** a `CharacterFixture.blank` character has **14** max PV, so 28 damage is DEAD and
21–27 is Coma. A test of heal *arithmetic* must keep its target alive (`lifeMultiplier(200)`, or
smaller figures), or the heal is refused.

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

### The exception: when the figure reads the *holder's* situation

Scanning works here because the value derives from the **recipient's** own situation — Bastião's
clause is a PV comparison, and the recipient's own `SceneContext` answers it.

It does **not** work when the value derives from the **holder's**. Santo's Título-Primário clause
("aliados adjacentes recebem metade do seu Bônus do Efeito Base") is half of a figure that counts
*the holder's* adjacent allies. A scan running from the recipient sees only its own neighbours, so
with two allies beside the Santo it would hand each of them half of the wrong number. Resolving
holder-side is the only way to get it right, and a holder-side resolution has nowhere to put its
answer except onto the recipient — so that one is **granted**, as a `scene.ProjectedAura`
(`CombatantSheet#resolveProjectedAuras`) that `Scene#refreshProjectedAuras` grants and revokes
against a per-holder ledger, the same revoke-then-regrant shape `Scene#applyInitiativeBlessings`
already uses. The revocation obligation is real and `Scene` carries it, including on
`removeParticipant` in both directions; the bonus is `TemporaryBonus.openEnded` so no Rodada
countdown expires it early. `ProjectedAuraTest#theAmountIsTheHoldersAdjacencyNotTheRecipients` pins
the difference.

**Choose by asking whose adjacency the number reads**, not by preference: recipient's → scan,
holder's → Aura. And note the Aura, being a grant, is caller-driven — this core watches nothing, so
a consumer calls `refreshProjectedAuras` after any movement or teleportation.

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

### Reacting to a hit — `CombatantSheet#notifyDamageTaken`

Damage used to be a one-way street: `applyDamage` mutated the sheet and the victim never got a
turn. It does now, for the *victim's own* traits — not for damage travelling back the other way,
which is still missing.

`DamageService#notifyDamageTaken(target, finalDamage, source, sceneContext)` is called from
exactly **two** places, and they are the two that deliver an attack's damage:
`DamageServiceImpl#applyDamage` and `DamageInteraction#applyTo` (which splits mitigation from
application and so has to fire it explicitly). It lives on the *service*, not the sheet: a
reaction is a rules resolution like mitigation itself, and `org.aventyrs.core.sheet` holds state
rather than orchestrating. Everything else that removes PV — `Bleeding`'s
per-Rodada tick, `Withering`, an `ActiveAbility`'s `getHitPointCost` — goes through the bare
`CombatantSheet#applyDamage(int)` and fires nothing. **When you add a new way to lose PV, that
choice is the decision to make**: is this an attack landing, or an effect draining?

Two things happen there:

- **`getAttacksSufferedThisRound()` advances** — every call, including a hit fully absorbed by
  RD. It is the defence-side twin of the roll-action log, reset by `startNewRound()`, and it is
  what a "somente ao primeiro ataque sofrido a cada Rodada" clause reads
  (`TrollFeat#REGENERACAO_REATIVA_INVERNAL`, through
  `Feat#resolveDamageReduction(Character, CombatantSheet)`). The count advances *after* the hit's
  own mitigation was computed, so the attack being mitigated still sees zero. A
  `calculateFinalDamage` preview never advances it.
- **Every held ability's damage-taken `Blessing`s are granted** —
  `SkillCompetencyAbility#resolveDamageTakenBlessings(holder, finalDamage)`, scanned across
  `SkillCompetencyAbility.allFor(character, sheet)`. **The ability states what it grants; the
  service only applies it**, so a new damage-triggered clause is written entirely on its own
  constant. That includes the stacking ceiling: an ability whose effect another Talento buffs
  scans its holder's Talentos for that buff *itself* and puts the answer on the Blessing.
  `DamageServiceImpl` resolves nothing — it used to compute one ceiling per damage event and hand
  it to every ability in the scan, which would have let a Troll Talento widen an unrelated
  ability's Blessing. **A trigger consumer applies, it does not resolve.** Regeneração Reativa is the one consumer, and is a *Habilidade Racial* like any other
  (`TrollsRacialAbility#REGENERACAO_REATIVA`) rather than a hook on `Race` and `Feat`: `allFor`
  already unifies the born-with / acquired / Talento-granted sources, deduplicates them, and drops
  the racial term under a Forma that abandons its holder's traços raciais.

### A Blessing renews, it does not stack

`CombatantSheet#grantBlessing(Blessing[, maximumSimultaneous])` is the one path a `Blessing` takes
to reach a sheet — `Scene#applyInitiativeBlessings`, `startCombat()` and `notifyDamageTaken` all go
through it — and it is where two rules live:

- **Same source, same `ModifierType` ⇒ replace, don't add.** The resulting `TemporaryBonus` carries
  `Blessing#getSource()`, and `applyEffect` trims by (concrete class, `TemporaryEffect#stackingKey()`),
  where a bonus's key is source + type. Replacing is what renews the duration. **The type is part
  of the key on purpose**: one trait may grant several bonuses at once — `AnaoFeat#VIGOR_DO_INVERNO`
  hands its holder RD *and* Resistência a Críticos — and keying on source alone made the second
  evict the first. The sourceless `grantTemporaryBonus(type, value, rounds)` path is unchanged and
  still stacks without limit, because without a source there is nothing to call "the same grant".
- **`ModifierType.REGENERATION` becomes a `Regeneration`**, the one type whose `TemporaryBonus`
  *acts* each Rodada rather than contributing to a stat. The branch is in `TemporaryBonus#from`,
  the single point a Blessing turns into a held effect, so no granting site knows about it.

`Regeneration` is `Bleeding` turned around, plus one thing no other `TemporaryBonus` has: a
**total budget**, `Blessing#getTotalLimit()`. "Não pode superar os danos sofridos" caps what the
whole effect ever heals at the damage that started it, so it stops the moment either the budget or
the Rodada count runs out — and it spends the budget by what the sheet *actually* recovered, so a
holder at full PV or one whose healing is prevented spends none. A `null` limit means no cap.

`TemporaryEffect#maximumSimultaneous()` is the ceiling: a boolean cannot say "up to 1+Títulos at
once", so it is a number — derived from `isCumulative()` by default (which `Withering`, `Condition`
and `FormEffect` still rely on), 1 for any sourced bonus, and raised by whatever the granting
ability states. It rides on the `Blessing` (`getMaximumSimultaneous()`, honoured for **every**
`ModifierType`, not just `REGENERATION`) because the ability granting a Blessing is precisely the
one that knows which traits may lift its own ceiling.

Two gotchas worth knowing before you add a second reaction:

- **`tickTemporaryEffects` iterates a snapshot** for the `applyRoundEffect` pass, because
  `Regeneration` heals and healing clears every active `Bleeding` — a per-Rodada effect that
  mutates the effect list would otherwise fault its own iteration.
- **"De fontes inimigas" is approximated.** The `SceneContext` in hand at the damage site may be
  either party's snapshot (`AttackDelivery` hands out a chain the *caller* invokes), so it cannot
  be asked whose ally the attacker is. A hit arriving through an attack path counts as hostile
  unless `source` is the victim's own sheet; friendly fire currently triggers regeneration.

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
  `DamageServiceImpl.java` — mitigation order, ally scans, `SceneContext` overloads, the
  attacker-side `halfDamage` parameter.
- `src/main/java/org/aventyrs/core/character/services/AttackTargetingService.java` /
  `AttackTargetingServiceImpl.java`, `src/main/java/org/aventyrs/core/combat/AttackTarget.java` /
  `DeliveredAttackTargetResult.java`, `Feat#resolveAdditionalTargets`,
  `DamageInteraction#halvingDamage` — multi-target resolution
  (`MultiTargetAttackTest.java`, `AttackTargetingServiceImplTest.java`).
- `src/main/java/org/aventyrs/core/action/Manoeuvre.java`,
  `src/main/java/org/aventyrs/core/character/services/ChargeService.java` / `ChargeServiceImpl.java`
  / `ChargeResult.java`, `MovementReactionService.java` / `Impl` — the Investida
  (`ChargeServiceImplTest.java`, `MovementReactionServiceImplTest.java`,
  `src/test/java/org/aventyrs/core/combat/ChargeAttackTest.java`);
  `AbstractSkillInteraction#resolveChargeDamage`, `DexterityAbility#IMPLACAVEL`,
  `MobilidadeFeat#INVESTIDA_AQUATICA`/`INVESTIDA_SELVAGEM`.
- `src/main/java/org/aventyrs/core/character/services/HitPointsService.java`
  (`HitPointsServiceTest.java`) — `getStatus`, `getMaxHitPoints`.
- `src/main/java/org/aventyrs/core/sheet/HealingSource.java` / `FallenHealingLedger.java`,
  `AbstractCombatantSheet#heal(int, HealingSource, boolean)` — the fallen-healing limits
  (`FallenHealingTest.java`, `title/curandeiro/CurandeiroIntegrationTest.java`).
- `src/main/java/org/aventyrs/core/character/CharacterStatus.java`.
- `src/main/java/org/aventyrs/core/sheet/CombatantSheet.java` /
  `AbstractCombatantSheet.java` / `src/main/java/org/aventyrs/core/monster/MonsterSheet.java`
  (`MonsterSheetTest.java`).
- `src/main/java/org/aventyrs/core/sheet/Regeneration.java` /
  `TemporaryEffect.java` (`maximumSimultaneous`), `CombatantSheet#notifyDamageTaken`,
  `TrollsRacialAbility`, `SkillCompetencyAbility#resolveDamageTakenBlessings`,
  `DamageService#notifyDamageTaken`, `Blessing#getTotalLimit`, `TrollFeat` — the damage-taken trigger
  (`ReactiveRegenerationTest.java`).
