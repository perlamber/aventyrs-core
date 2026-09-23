# Gigante Enfurecido — how each trait works

A caller's guide to Gigante Enfurecido's traits: what to put on the request, what comes back, and
**what you still have to do afterwards**. Authored against V19 (`docs/rules/titulos.txt`, lines
848–963) as of 0.0.51. Companion to [`santo.md`](santo.md) and [`senhor-da-briga.md`](senhor-da-briga.md).

> The authoritative contract is the javadoc on each class and on
> `org.aventyrs.core.title.package-info.java`. If they disagree, the javadoc wins.

---

## The one thing to internalise

**Gigante Enfurecido is a state.** Its Despertar is *activated*: `GiganteEnfurecidoDespertar.FRENESI`
starts a `sheet.Frenzy` on the activator, and almost every other trait either needs one running,
adds a mode to it, or reads it. The Frenzy's numbers reach play through
`CombatantSheet#getTemporaryBonus`, so Força, Iniciativa, Tamanho, the Multiplicadores, Defesas,
Movimento, Margem Crítica and RA need nothing from you. What you must do is small:

| When | Call |
| --- | --- |
| a Berserker is hit and damaged | `GiganteEnfurecido#recordHitTaken(holder, attacker)` |
| the Gigante kills a foe or lands a critical | `GiganteEnfurecido#recordTriumph(holder)` (unlocks Frenesi Assustador) |
| picking an attack target | if `isCompelledToAttackNearest()`, allow only the nearest creature(s), allies included |
| a hit is about to drop the Gigante to 0 PV | `DamageService#wouldDropToZeroOrBelow` → offer `SELF_WOULD_DROP_TO_ZERO_HP` Reações **before** applying |
| the Gigante's Defesa failed | offer `SELF_HIT_BY_SUCCESSFUL_ATTACK` Reações |
| an adjacent enemy attacks someone else | offer `ADJACENT_ENEMY_ATTACKS_OTHER` Reações to the Gigante |
| an activation returns `areaDamage` | apply each `AreaDamage` (`DamageService#applyDamage` with its descriptor) |
| an activation returns `inflictedConditions` / `grantedFrenzy` | deliver them to the clients owning those sheets |
| the player ends Frenesi (Uno com a Ira) | `GiganteEnfurecido#endFrenzyVoluntarily(holder)` |
| the GM passes time / grants a rest | `CombatantSheet#passHours(h)`; `RestService#applyRest(…, verdadeiro = true)` |

## Paying in Autocontrole

`AventyrTitleAbility#getEgoCost()` states a trait's temporary-Ego cost (`EgoCost`); the activation
checks it (`NOT_ENOUGH_EGO_POINTS`) and spends it. Pass a `DiceRoller` on the request so a Vantagem de
Ego reacting to the spend (Determinação Heroica) can roll; without one the points are spent plainly.

Every point spent on a Frenesi effect is owed back **1 per 2 hours** (`passHours`); under Uno com a
Ira, each Frenesi's first 2 points come back after 2 Rodadas if it still runs and the Gigante is
conscious.

---

## At a glance

| Trait | PD | Autocontrole | PA | Request needs | Caller must |
| --- | --- | --- | --- | --- | --- |
| **Frenesi** *(Despertar)* | — | 1 +1 per mode | 2 +1 per mode | `choices`: `FrenzyMode` (TITA_ENLOUQUECIDO, BERSERKER — each held) | nothing more |
| Prolongar Descontrole | 2 | — | 2 | — | — |
| Uno com a Ira | passive | — | — | — | offer "Encerrar Frenesi" |
| Frenesi Esmeralda *(Sup.)* | — | — | Ação Livre | a Frenesi running | — |
| Frenesi Reativo *(Sup.)* | — | 2 +1 per mode | Reação | `choices`: `FrenzyMode` | offer on `SELF_HIT_BY_SUCCESSFUL_ATTACK` |
| **Titã Enlouquecido** *(Esp.)* | — | +1 | +1 | a mode of Frenesi | add `getConcentrationActionPointSurcharge()` to Gnose Perícias |
| Gritos de Guerra | 3 | — | 1 | `choice`: `GritoDeGuerra`; `sceneContext`; `diceRoller` for Espinhoso | grant the Blessing (its `getReach()`), apply `areaDamage`, deliver `grantedFrenzy` |
| Frenesi Arcano | — | — | Ação Livre (+3PA on the cast) | `choice`: `SpellEmpowerment`; Titã active | cast normally — `castSpell` spends it |
| Colosso Enfurecido | passive | — | — | — | — |
| Cataclismo Elemental *(Sup.)* | — | 1 | 1 per element | `choices`: `ElementalType` (Cataclismo's 8) | apply `SpellCastingResult#getAreaDamage` after instant Magias |
| **Berserker** *(Esp.)* | — | +1 | +1 | a mode of Frenesi | `recordHitTaken` after each damaging hit |
| Retaliação Furiosa | — | — | Reação | `target` (the enemy), `sceneContext` | make the attack; fold `TitleAttackModifiers#damageOverride` |
| Frenesi Assustador | 2 | — | Ação Livre | `sceneContext`; a `recordTriumph` this Turn | deliver `inflictedConditions` |
| Desprezar Danos | passive | — | — | — | heal Roubo de Vida with `healFromLifeSteal` |
| Fanático de Cyt *(Sup.)* | — | all (min 1) | Reação | a Frenesi running | activate **before** applying the lethal hit |

## Each trait

### Frenesi (Despertar)
+2 Força and +2 Iniciativa (Bônus Variável — `STRENGTH_BONUS`/`INITIATIVE`) for 2 + ½ Vigor Rodadas,
+2 as Título Primário, counting down at the holder's Turn start. Blocks Gnose Perícias and Domínio do
Mana (`isSkillUsePrevented` → `SKILL_USE_PREVENTED`), Conjurar (`SPELL_CASTING_PREVENTED`) and
Mimetizar. At 0 temporary Autocontrole the holder is compelled to attack the nearest creature. Cannot be
ended except under Uno com a Ira. A second one while it runs: `FRENZY_ALREADY_ACTIVE`.

### Titã Enlouquecido / Colosso Enfurecido
Tamanho +1 and Multiplicador de PV +1; lifts the block; +1PA on Magias (`resolveActivationTime`) and
Gnose Perícias (`getConcentrationActionPointSurcharge`). Colosso adds Tamanho +1 and PD/PM Multiplicadores
+1 while Titã is a mode. **The size change must reach other clients.**

### Berserker
+1 Força, +1 Margem Crítica Menor. Each distinct attacker per Cena whose hit damages the Gigante adds
−2 Defesas and +1 Força (`recordHitTaken`).

### Gritos de Guerra
Once per Rodada, during a Frenesi. **Desdenho**: a `HALF_DAMAGE` Blessing, `SELF_AND_ALLIES`, reach
`DISTANCIA_MEDIA`, 1 Rodada counting down at Turn start. **Espinhoso**: 1d6 + ½ Multiplicador de PV
Primordial to each enemy in Curta, doubled adjacent (one die for all). **Inspirador**: an inspired copy
of the Frenesi — bonuses *and* drawbacks, not the compulsion — for 1 Rodada to allies in Curta.

### Cataclismo Elemental
While on (for the rest of the Frenesi): each instant Magia and each Grito adds one 1-point Mágico
Elemental hit per element to **everyone** in Curta, allies included, ×2 adjacent; and RE (−2 per hit)
against each chosen element.

### Frenesi Arcano
`DANO`: +1d6 to the next Magia; if that passes 3d6 the dice are maximised instead. `DURACAO`: +2
Rodadas on an Encantamento or Maldição. Either adds +3PA to that cast.

### Retaliação Furiosa
Grants one attack against the enemy named; `TitleAttackModifiers#damageOverride()` is 1d6 + ½ Força,
or 2d6 + Força at 0 PV or below — **roll that instead of the weapon's dano**. `consumeCharges` spends it.

### Frenesi Assustador
Foes in Curta whose temporary Autocontrole exceeds the Gigante's climb Abalado → Assustado → Apavorado
for 2 Rodadas (an Encantamento). While the Gigante is at 0 PV or below, those foes deal no Efeito
Crítico Menor and trigger no Corrente (`AttackDelivery` reads it).

### Fanático de Cyt
Spends every temporary Autocontrole (min 1); the next hit leaves 1 PV (`floorNextDamageAt`); Roubo de
Vida = points spent for the rest of the Frenesi. While the Frenesi runs at 0 Autocontrole: no death
(`getStatus` stops at Coma), everyone is an enemy, and any Magia from someone else must beat the
Fanático's DM (`SpellCastingResult#getMustOvercomeMagicDefense`).

## Game time and Descanso Verdadeiro

`CombatantSheet#passHours(h)` ends every Rodada-counted effect (hours outlast them) and pays hourly
debts. `RestService#applyRest(character, sheet, type, true)` is a **Descanso Verdadeiro** — the one a
GM grants — and lifts what waits on one (`applyEffectUntilTrueRest`: Uno com a Ira's exhaustion). A
Magia "como se passasse por um Descanso" is not one: use the three-argument overload.

## Pitfalls

- **Activate Fanático de Cyt before applying the hit**, not after — the floor catches the *next* damage.
- **The Despertar is not in `getAbilities()`.** It is in `getAllAbilities()`; don't offer it for acquisition.
- **An inspired copy is not the ally's own Frenesi.** `getOwnFrenzy()` stays empty on them; `getFrenzy()` sees it.
- **Especializações are not activated on their own** — they ride on the Frenesi request as `FrenzyMode`s.
