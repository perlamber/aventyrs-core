---
name: building-a-foe
description: This skill should be used when the user asks to "add a new monster", "build a foe", "create a monster/inimigo/criatura", "add a Modelo", "implement a Habilidade Monstruosa", "add a sample monster", "make a boss/bruiser/caster enemy", mentions Grau de Poder / Categoria (Presa, Deviante, Predador, Apex, Abominação) / Exemplar, or gives a monster's stat block. Walks through the rules-built path (MonsterBlueprint + MonsterRules, from docs/rules/criacao-de-monstros.txt), implementing a Modelo's Habilidades Monstruosas the way CireneiaAbility does, the fixed-stat-block path kept for summons, and the spawn-independence trap — mirroring org.aventyrs.core.monster as the reference implementation.
---

# Building a foe

A foe lives in `org.aventyrs.core.monster`. It is an ordinary `Character` — Attributes,
Perícias, abilities and equipment — wrapped in a `MonsterSheet` that adds the numbers a
creature presents *because it never rolls*: a GD per Perícia and two Defesas.

**There is no `Monster extends Character`, and there must not be.** A foe's `Character` has
`race` set to the single catch-all `Monstruoso` and `player` left `null`.

## 0. Pick the path

| Path | When | Shape |
| --- | --- | --- |
| `MonsterBlueprint` | **Any monster a Mestre authors.** The default. | `@Builder` of *choices*; `MonsterRules` derives every number |
| `SummonedMonsterTemplate` | An **invocação** — its numbers follow its Conjurador, not a Grau de Poder | its own class in `monster.summon`, e.g. `Zumbi` |
| `AbstractMonsterTemplate` | A **fixed stat block** outside the creation rules, or a hand-built test foe | `@Builder` of authored DF/DM/GDs |

`SampleMonster` holds legal worked blueprints (a Presa and a Predador). The old five
`GenericMonster` archetypes now live **in test sources only** (same package), because dozens of
combat tests use them as known DF/DM targets — don't move them back to main.

## 1. The rules-built path — `MonsterBlueprint`

Source of truth: `docs/rules/criacao-de-monstros.txt`. A blueprint holds what the Mestre
**chooses**; `MonsterRules` derives the rest, and `MonsterSheet` asks `MonsterRules` for every
number **live** (a Destreza an Impulso raised raises the GDs it governs).

| Choice | Derived |
| --- | --- |
| `powerDegree` | `MonsterCategory` (never chosen — `forPowerDegree`), its ceilings, ×PV, PA/Ego bonuses, every budget |
| `kind` (`REGULAR`/`EXEMPLAR`) | base GD (Muito Fácil / Fácil), extra Talentos, Exemplar-only traits, the Ego-effect cap |
| `attributeBases` | 10 points above base 1, max 5; Bônus Racial from Habilidades, trimmed to the Categoria's cap |
| `trainedSkills`, `gnoseUpgrades`, `progressionUpgrades` | each Perícia's `SkillDifficulty` — **there are no Graduações** |
| `models`, `abilities` | Habilidades Monstruosas' passives, actives, GD steps; +2PV each |
| `feats`, `egoAllocation` | Talentos Gerais/Monstruosos; Egos base 2 + allocation |
| `adjustments` (`MonsterAdjustments`) | the Mestre's signed deltas, applied last, **never validated** |

**A Perícia's GD** (see `MonsterRules` javadoc): base level → +1 Gnose upgrade, +1 per GP upgrade
(trained only) → clamp to the Categoria's "GD Máximo" (Exemplar +1) → Habilidade steps and the
Mestre's steps (these go *past* the clamp) → bonus = ⌊governing Atributo / 2⌋ + Habilidade roll
bonuses + the Mestre's.

**Defesas** = the Esquiva e Aparar GD's value + `DefenseService#getTotalDefense(sheet, type)`
(equipment, Habilidades, temporary bonuses, Condições) + the Mestre's. DF and DM share the base.

**PV/PD/PM** use `ResourceFormula.MONSTER` on the `Character`: PV = 20 + Vigor × Categoria
multiplier (+2 per Habilidade via `MonstrousAbilityGrant`); PD = PV/2 + Instinto × 3; PM = PV/2 +
Foco × 2. The three pool services branch on the formula, so their `Character`-only overloads
agree with the sheet ones.

**Validation returns, never throws.** `MonsterRules.validate` gives a `List<MonsterViolation>`
(code + subject + actual/limit) so an editor shows every problem at once. `spawn` doesn't
validate — an over-budget monster still plays, like the builder-bypassable `Character`.

**Open readings** are named constants, not scattered literals: an Exemplar's unstated extra
Atributo/GD ceilings are `MonsterKind#bonusMaximumAttribute`/`bonusMaximumSkillSteps`. Change them
there.

## 2. Implementing or extending a Modelo

All six Modelos are implemented, each an enum implementing `MonstrousAbility` (`CireneiaAbility`,
`AlmaElementalAbility`, `AspectoHumanoideAbility`, `MutanteMonstruosoAbility`, `BestaAladaAbility`,
`BrotosDeMapinguariAbility`), linked from `MonsterModel` by a `Supplier` of `values()`. Every hook
takes an **`AbilityContext`** — the holder's Categoria plus its picks — so gate each
Aprimoramento with `context.isAtLeast(…)`.

- **Picks** are `getChoiceSpecs(category)` → `ChoiceSpec(id, options, count)`; the count may grow
  with the Categoria. The picks travel as `Map<String, List<String>>` on
  `MonstrousAbilitySelection`; read them with `context.pick(id[, Enum.class])`/`picks(…)`.
  `MonsterRules.validate` checks each spec's count, options and distinctness.
- **Numbers** reach the stat services through `MonstrousAbilityGrant` (an `AttributeAbility`):
  `resolveModifier` for `@Modifier` types (add a method there for a new `ModifierType` —
  `ModifierResolverImpl` only indexes *declared* methods), and forwarded anatomy hooks: RE,
  scoped Meio-Dano / immunity / vulnerability (`DamageScope`), Efeitos Críticos, Resistência a
  Críticos, Condição immunity, Armas Naturais, flight, damage-taken effects. Those hooks live on
  `AttributeAbility` and are read by `AbstractCombatantSheet`/`DamageServiceImpl`, so they work on
  any sheet — including a client that wraps the monster's `Character` in a `CharacterSheet`.
- **Build-time grants**: `resolveStandingEffects` (open-ended `Regeneration.openEnded`, `LifeSteal`,
  `RecurringDice`), `resolveGrantedSpells`, `resolveGrantedFeats`, `resolveBonusFeatSlots`,
  `resolveAllowedFeatCategories`. `MonsterBlueprint#applyStandingEffects` applies the standing ones;
  `spawn` calls it, and so must any caller building its own sheet from `buildCharacter()`.
- **Actives** are `MonstrousActiveAbility`s: price, Duração, Resfriamento (`ONCE_PER_SCENE`), effects
  as `Supplier<TemporaryEffect>` (fresh per activation), `usableWhen` (a precondition checked before
  any cost), `onActivated` (landing, lifting Condições), and **dice**.
- **Dice: the caller rolls, core applies.** Never add a random source. An active declaring `dice(…)`
  must be activated through `ActiveAbilityService#activate(…, List<Integer> faces)`; the faces are
  validated before any cost, and `onRolled` receives the total. A per-Rodada roll is a
  `RecurringDice`: it queues a `PendingDiceRoll` each Rodada, and the caller hands the faces to
  `CombatantSheet#resolveDiceRoll`, which heals or damages through `DamageService`.
- **Flight** is `CombatantSheet#isFlying()`/`setFlying(boolean)`; "enquanto voando" clauses are
  `resolveWhileFlyingEffects`, applied on take-off and lifted on landing; `keepsFlying` never lands.
- **A trait two Habilidades share** (Anatomia Vegetal) names a `sharedTraitKey()`; only the first
  holder gets `context.ownsSharedTraits()` — grant the non-idempotent parts (Bônus Racial, RC) only then.
- **Timed GD steps** are `SkillDifficultyShift` effects, read by `MonsterRules`' live GD path.
- **Status**: derive it from the note (`getUnappliedNote() == null ? APPLIED : PARTIAL`), and write a
  note for every clause you leave out, naming the system that's missing. `MonsterModelTest` fails a
  non-APPLIED Habilidade with no note.

## 3. The fixed-stat-block path

`AbstractMonsterTemplate`'s builder states DF/DM, a general GD and per-Perícia GDs directly;
`MonsterSheet#getBlueprint()` is empty and every getter returns the authored number. Only reach
for it for summons or tests. Its optional hooks (`actionPoints`, `skillSpecialization`,
`undead`, `criticalEffectImmunity`) still apply; `undead` and the immunities exist on
`MonsterBlueprint` too.

**`isUndead()` is narrow on purpose** — the only vitality classification this core has. Don't
reach for it as a general anatomy tag. **Immunities are named by `CriticalEffectType`**, enforced
by `CriticalEffect#applicableTo` in both directions of an exchange.

For a summon: the parameter is a plain `int` (the Conjurador's Graduação), tier clauses fold in
at build time via `withConjurador(int)`, and tiered numbers need an instance-based ability — see
`Zumbi`/`ZumbiAbility`.

## 4. Everything else is already shared — don't re-implement it

`MonsterSheet` and `CharacterSheet` both extend `AbstractCombatantSheet`: damage, shields,
Mana/Determinação, Ego points, `TemporaryEffect`s, cooldowns, inventory (loot), the Turn
lifecycle. Type combat-facing signatures as `CombatantSheet`. The XP-spending services take a
`CharacterSheet`, so a monster can't reach them — no guard needed.

Monster-only state on `MonsterSheet`: the Regular's two-Efeitos-de-Ego-per-Cena counter
(`checkEgoEffectAvailable`/`recordEgoEffectUse`, enforced by `EgoPointsService#useEgoPointsForEffect`)
and `beginScene()`, which resets it and the once-per-Cena actives.

## 5. The spawn-independence trap

`spawn()` must return a **fully independent** foe each call. `MonsterBlueprint#buildCharacter`
builds new skills, feat/equipment lists and **new `MonstrousActiveAbility` instances** every call —
`ActiveAbilityService` and the cooldown map key actives by identity, so sharing one would let one
foe's Resfriamento lock another's. For a template: `SkillGraduation` is mutable, so each spawn
builds its own.

## 6. Write tests

- Blueprint: validation per violation code, the GD ladder (clamp, Exemplar, untrained, steps past
  the ceiling), PV/PD/PM through the real services, DF with equipment and adjustments,
  `twoSpawnsFromOneBlueprintAreFullyIndependent`. See `MonsterRulesTest`, `MonsterBlueprintTest`.
- A Modelo: each Habilidade at each tier boundary its Aprimoramentos name, plus one test driving
  an active through `ActiveAbilityServiceImpl` and reading the result from the real service. See
  `CireneiaAbilityTest` and the five sibling `*AbilityTest`s, built on `monster.model.MonsterTestKit`
  (`spawn(gp, selections…)`, `held(sheet, name)`, `finalDamage(sheet, type, element)`).
- The shared pieces: `MonsterExtensionsTest` (dice, scoped mitigation, pick validation).
- `MonsterModelTest` sweeps the catalog (8 Habilidades each, two Presa, back-links) — a new
  Modelo is picked up automatically.
- Keep `SampleMonster` legal: `MonsterRulesTest#everySampleIsLegal`.

## Reference files to read first

- `docs/rules/criacao-de-monstros.txt` — the rules.
- `monster/MonsterBlueprint`, `MonsterRules`, `MonsterCategory`, `MonsterKind`,
  `MonsterAdjustments`, `MonsterViolation`, `SampleMonster`.
- `monster/model/MonsterModel`, `MonstrousAbility`, `MonstrousActiveAbility`,
  `monster/MonstrousAbilityGrant`, and `monster/model/cireneia/CireneiaAbility` — the worked Modelo.
- `monster/MonsterSheet` — the two paths behind one set of getters; `beginScene`.
- `character/ResourceFormula` and the three pool services.
- `monster/MonsterTemplate`, `AbstractMonsterTemplate`, `SummonedMonsterTemplate`, `summon/Zumbi` — fixed stat blocks.
