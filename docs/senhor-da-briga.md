# Senhor da Briga — how each trait works

A caller's guide to Senhor da Briga's traits: what to put on the request, what comes back, and
**what you still have to do afterwards**. Authored against V19 (`docs/rules/titulos.txt`, lines
974–1090) as of 0.0.48. Companion to [`santo.md`](santo.md) — read its "Activating anything" and
"four carriers" sections first; nothing there changes here.

> The authoritative contract is the javadoc on each class and on
> `org.aventyrs.core.title.package-info.java`. If they disagree, the javadoc wins.

---

## The one thing to internalise

**Santo is activated; Senhor da Briga mostly isn't.** Twelve of its eighteen traits are passive,
and most of them change *the attack itself*. So the work moves from "discharge what an activation
returned" to "ask one question before every attack, and make one call after it":

```java
TitleAttackModifiers mods = TitleAttackModifiers.resolve(attacker, weapon, defender, context);
// ... build the DeliveredAttack with mods folded in, resolve it ...
TitleAttackModifiers.consumeCharges(attacker, weapon);   // AFTER: the roll reads the budgets
```

Everything that lands **inside a roll** this core applies for you, through the Título scans:

| Scan | Service | Senhor da Briga uses it for |
| --- | --- | --- |
| `resolveBaseDefesasBonus(ctx, holder, sheet, primary)` | `DefenseServiceImpl` | Primário +2, Fantasma do Ringue +1/+1/+3, Campeão's stacks |
| `resolveDamageBaseIncrease(holder, weapon, primary)` | `DamageBaseServiceImpl` | Primário +1 Dano Base |
| `resolveCriticalMarginIncrease(skill, source, holder)` | `CriticalServiceImpl` | Punho Inigualável +2, Campeão +1, Fantasma +2 (Defesas), Cruz de Sangue +2 |
| `resolveCriticalDamage(skill, source, crit, holder)` | `CriticalServiceImpl` | Campeão: Menor +2, Maior +1d6 |
| `resolveAttackRollBonus(skill, source, holder, target, ctx)` | `AbstractSkillInteraction` | Vantagem from Impacto / Agarrar / Rolamento (GM) / Malícia |
| `resolveDamageRollBonus(...)` | `AbstractSkillInteraction` | Malícia de Valentão's Vantagem em Danos |

What it **can't** apply comes back on `TitleAttackModifiers`:

| Field | What you do with it |
| --- | --- |
| `extraDamageDice()` | roll that many more d6 — this core never rolls |
| `actionPointReduction()` | lower the attack's PA price (never below the cheapest legal one) |
| `defenseType()` | when non-null, roll against **that** Defesa (Impacto GM: `MAGIC`) |
| `damageDescriptor()` | when non-null, type the damage with it |
| `effectChains()` | add to `DeliveredAttack#effectChains` |
| `criticalEffectOverride()` | informational — `AttackDelivery` applies it itself |

**Armas Naturais** always means `Character#treatsAsNaturalWeapon(weapon)`. Pass
`NaturalWeapon.ATAQUE_DESARMADO` as the attack source for a punch — an attack naming no weapon at
all is *not* a natural-weapon attack.

---

## The eighteen at a glance

| Trait | PD | PA | Request needs | Caller must |
| --- | --- | --- | --- | --- |
| **Despertar** | passive | — | — | nothing (vacuous — see below) |
| **Título Primário** | passive | — | — | nothing — scanned |
| **Punho Inigualável** *(Esp.)* | passive | — | — | nothing — scanned |
| **Fantasma do Ringue** *(Esp.)* | passive | — | — | nothing — scanned |
| Finalização | 1 | 1 | — | pass a `DiceRoller` on the attacks it repeats |
| Chamar pra Briga | passive | — | — | call `resolveChamarPraBriga` after each hit; mirror the `ForcedTargeting` |
| Punho de Ferro *(Sup.)* | passive | — | — | fold `TitleAttackModifiers` |
| Campeão da Taverna *(Sup.)* | passive | — | — | call `recordCriticalHit` on each crit; GM ends combat |
| Impacto Elemental | 1 | 2 | element chosen on the Título | fold `TitleAttackModifiers`; `consumeCharges` |
| Rolamento Ofensivo | 1 | Ação Livre | `target` (enemy ≤ Curta), `sceneContext` | move the token `movementTowardTarget` UD toward the target |
| Agarrar e Derrubar | 2 | 3 | — | next attack with a natural weapon; fold `effectChains`; `consumeCharges` |
| Grande Mestre das Brigas *(Sup.)* | passive | — | — | offer the free attack vs Caído (`grantsFreeAttackAgainstFallen`) |
| Cruz de Sangue | 2 | Reação | — | list it on `SELF_TARGETED_BY_MELEE_ATTACK`; offer the counter-attack on a failed Defesa |
| Fingir Fraquezas | **= 1 + foes at Muito Curta** | 1 | `sceneContext`, `determinationPoints` = `resolveCost(ctx)` | mirror each foe's `ForcedTargeting` |
| Entre as Pernas | passive | — | — | — *(TODO: no occupancy)* |
| Malícia de Valentão *(Sup.)* | passive | — | — | nothing — scanned |

---

## Each trait

### Despertar
Both clauses hold **vacuously**: this core imposes no Desvantagem for attacking with an Arma
Natural nor for defending without Equipamento Defensivo, so there is nothing to be exempt from.
The **Armas Improvisadas** half is ignored (table ruling, 2026-09-23): no Habilidade creates an
improvised weapon any more — the Especialização that did was replaced by Fantasma do Ringue. Not a TODO.

### Título Primário
`Character#getPrimaryTitle() == title` is resolved by the services, never self-reported:
- **Dano Base +1** with Armas Naturais (`DamageBaseServiceImpl`) — one scale-up, the same reading
  `ArtesMarciaisFeat#ARTISTA_MARCIAL` takes.
- **Defesas +2** while `Character#isArmedOnlyWithNaturalWeapons()` — a drawn sword ends it,
  a sheathed one doesn't.

### Punho Inigualável *(Especialização, passive)*
Margem Crítica Menor **+2** with Armas Naturais.
Guilhotina rides every natural-weapon critical as an additional Efeito Crítico (0.0.49) —
the **attacker** gains Vantagem and a cumulative Margem Crítica Menor widening.

### Fantasma do Ringue *(Especialização, passive)*
**Table ruling (2026-09-22):** the printed "2PD / 2PA, Efeito Ativo" is read as a *conditional
passive*. Defesas **+1** always, **+1** more while armed only with natural weapons, **+3** more
while wearing no Equipamento Defensivo (read as cumulative — up to +5). Defesa rolls (Esquiva e
Aparar) get Margem Crítica Menor **+2**.
Ímpeto Defensivo joins the worn gear's Efeitos Críticos Defensivos (0.0.49).

### Finalização — `FinalizacaoInteraction`
**1PD, 1PA.** Opens a one-Rodada window, `SenhorDaBriga#isFinalizacaoActive(sheet)`.
While it is open, `AttackDelivery` applies the Arma Natural's own Efeito Crítico once more on a
critical, and at Menor on a plain hit (0.0.49) — pass a `DiceRoller`, since several natural effects
throw dice.

### Chamar pra Briga *(passive)*
After a hit, call:
```java
boolean provoked = senhor.resolveChamarPraBriga(attackerSheet, defenderSheet, weapon, scene.getCurrentRound());
```
It records the natural-weapon hit on the **attacker's** sheet (the one you always own), and when the
same target was also hit last Rodada, is armed (`isWieldingAWeapon`), and hasn't been provoked by
this holder this combat, casts a 2-Rodada `ForcedTargeting` through `applyEnchantment`. From there
it behaves exactly like Orgulho Elduriano's binding: `AttackDelivery`/`AttackReceiver` enforce it,
and `Scene#recordAttack` discharges it. **The target's sheet may be a remote stand-in** — mirror
the binding over the wire, as with Orgulho.

### Punho de Ferro *(Suprema, passive)*
On the Rodada's **first** Arma Natural attack (per `getActionsThisRound()` — so record actions):
odd Rodada (0-based round 0, 2, …) → `actionPointReduction` 1; even → `extraDamageDice` 1.
> **TODO** "pode afetar Habilidades de Senhor da Briga" — nothing prices an activation by the attack inside it.

### Campeão da Taverna *(Suprema, passive)*
Margem Crítica Menor +1; Dano Crítico Menor +2 / Maior +1d6 (both scanned). For "suas Defesas
aumentam em +1 até o final da Cena", call `senhor.recordCriticalHit(sheet)` on every crit the
holder lands: it **stacks** one per crit and lapses at `Scene#endCombat()` (table ruling).
Its Margem Crítica **Maior** +1 is real too (0.0.49): 17 becomes an Acerto Crítico Maior.

### Impacto Elemental — `ImpactoElementalInteraction`
**1PD, 2PA.** The element is chosen **once**, on the Título:
`senhor.chooseImpactoElementalElement(ElementalType.FOGO)` (`TITLE_ABILITY_CHOICE_LOCKED` for a
different one; "Ar" is `VENTO`; `TODOS` refused). Persist it and restore it through the
three-argument constructor. Activating without one → `TITLE_ABILITY_CHOICE_REQUIRED`.

Grants a **combat-scoped budget** of `2 + other Punho Inigualável traits` attacks. While it lasts,
each Arma Natural attack gets Vantagem (in the roll) and `FISICO_ELEMENTAL(element)`. Under Grande
Mestre: `defenseType = MAGIC`, `DamageType.ELEMENTAL(element)` — ⚠️ *a reading*: "causam danos
mágicos" while keeping the element, which `MAGICO` cannot carry — and `CATACLISMO` replacing the weapon's own Efeito Crítico (0.0.49).
`consumeCharges` spends one per **natural-weapon** attack; `Scene#endCombat` drops the rest.

### Rolamento Ofensivo — `RolamentoOfensivoInteraction`
**1PD, Ação Livre.** Needs `target` = an enemy within `ENEMY_RANGE` (Curta) in `sceneContext`
(`TITLE_ABILITY_REQUIRES_TARGET` / `TITLE_ABILITY_TARGET_OUT_OF_RANGE`). Returns
`InteractionResult#getMovementTowardTarget()` — 2UD, 3UD under Grande Mestre — which **you apply**.
Grande Mestre also opens a one-Rodada Vantagem on natural attacks.
It also guards the activator (+3 Defesas) against that enemy — an `AttackerGuard`,
read when the defence roll's `SceneContext` names the enemy as its opposed character.
**Table ruling (2026-09-23):** both "por 1 Rodada" clauses last until the activator's Turn begins
in the next Rodada (0.0.50 — counted down at Turn start, `TemporaryEffect#countsDownAtTurnStart`).

### Agarrar e Derrubar — `AgarrarEDerrubarInteraction`
**2PD, 3PA.** Grants a one-attack budget. The next **Arma Natural** attack gets Vantagem in the
roll, and `TitleAttackModifiers#effectChains` carries an `effect.AgarrarEDerrubar` Corrente: on a
hit clearing the Corrente threshold it leaves the target **Caído** (open-ended) if they are at most
two Categorias de Tamanho larger (effective size, both sides). Offer only natural-weapon attacks
while `getRemainingEnhancedAttacks(AGARRAR_E_DERRUBAR) > 0`.
Grande Mestre: `grantsFreeAttackAgainstFallen(target, ctx)` — a once-per-Rodada Ação Livre attack
**you** offer and limit.

### Cruz de Sangue — `CruzDeSangueInteraction`
**2PD, Reação** on `ReactionTrigger.SELF_TARGETED_BY_MELEE_ATTACK`. Build the `ReactionContext`
with `attacker`, `attackSkill` and `soleTarget` (default `true`); it is offered only for an Ataque
Corpo-a-Corpo, as sole target, attacker **adjacent**. For 1 Rodada, applied to the holder:
Defesas −½ Destreza; Margem +2 on Defesas and natural melee; Malícia → RDS 2. It also grants one
counter-attack charge, whose **+1d6** shows up in `TitleAttackModifiers`. When the Defesa fails,
**offer** that counter-attack against the aggressor.
While active, Contra-atacante joins the Efeitos Críticos Defensivos (0.0.49) — its counter-attack
comes back on `DefensiveCriticalOutcome#counterAttack()`. There is still no "Defesa failed" trigger,
so offering the ordinary counter-attack is the caller's.

### Fingir Fraquezas — `FingirFraquezasInteraction`
**Variável PD, 1PA.** The PD **must equal** `FingirFraquezasInteraction.resolveCost(sceneContext)`
= 1 + enemies at Muito Curta (`INVALID_PD_AMOUNT` otherwise); refused with any non-natural weapon
drawn (`TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY`). Grants Defesas + that figure for **2
Rodadas** (table ruling), and casts a 2-Rodada `ForcedTargeting` on each of those foes not already
caught since their last Descanso. **Mirror each binding over the wire** — the foes' sheets are
usually not yours. Malícia de Valentão adds Vantagem on attack **and** dano against a foe bound
this way (scanned).
> **TODO** "Inimigos inteligentes" — no intelligence classification; pass a context without the
> beasts if the table wants the distinction.

### Entre as Pernas / Grande Mestre / Malícia de Valentão
Flags. Entre as Pernas is entirely TODO (no occupancy model).

---

## Ending a combat

`Scene#endCombat()` (the GM's "encerrar combate") turns `isCombatScene()` off, puts the Rodada back
to 0, and calls `CombatantSheet#endCombat()` on everyone, dropping everything combat-scoped:
Campeão's stacks, Impacto's budget, Chamar pra Briga's once-per-combat marks and its hit ledger.
A budget granted with plain `grantEnhancedAttacks` (Furor de Sylph) survives.

---

## Pitfalls

- **`consumeCharges` goes after the attack**, not before: the roll reads the budget, so spending the
  last charge first silently removes the Vantagem it was paying for.
- **Pass the weapon.** Every clause is Arma-Natural-scoped; a `null` attack source matches none.
- **Record actions.** Punho de Ferro's "first attack of the Rodada" reads `getActionsThisRound()`.
- **Remote sheets are stand-ins.** Chamar pra Briga and Fingir Fraquezas bind *other people's*
  sheets; mirror those bindings the way Orgulho Elduriano's are.
