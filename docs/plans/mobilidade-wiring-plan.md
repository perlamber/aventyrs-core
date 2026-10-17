# Fully wire MobilidadeFeat

**Status (2026-10-17): built** — core 0.1.7.17–0.1.7.22, API (`reaction` on roll requests) and client. Deviations
from the plan below: the Nadar roll and attacks are filed on the sheet's own log (not through `Scene#recordAction`);
Porta-Estandarte's heading window is core Rodadas 0–1 (the table's first two — `SceneContext#isWithinFirstCombatRounds`
counts from 1 and was left alone); a stopped Investida run still counts its whole path as distance moved.

## Context
`MobilidadeFeat` has 14 Talentos. Some are fully real (Movimento Rápido, Velocista, Mais Veloz que a Visão, Investida Selvagem's Explosão). The rest are partly real or inert:
- **ESQUIVA**: both clauses missing.
- **SE MOVER E ATACAR**: both clauses missing.
- **INICIATIVA APRIMORADA**: the Vantagem is missing.
- **PORTA-ESTANDARTE**: the PA and Movimento half is missing.
- **MOVIMENTO FURTIVO**: the halving price is missing, so the Talento over-grants.
- **INVESTIDA AQUÁTICA**: the first clause is missing.
- **MOVIMENTO ACROBÁTICO**: nothing is wired.
- **INVESTIDA SELVAGEM**: the movement RD is reported, never applied.
- **SOLAR / LUNAR**: nothing is wired.
- **LIDERAR O AVANÇO**: real in core, but no client or API code ever calls `Scene#applyInitiativeBlessings`, so it never fires in play.

Most of the TODOs in `MobilidadeFeat.java` are stale. These pieces have since been built:
- Reação windows and `GenericReaction`.
- `Manoeuvre.NADAR`, rolled through `MovementManoeuvreService`.
- `Feat#resolveTurnEndBlessings` (the Defesa Tartaruga mirror).
- `Feat#getGrantedEgoAdvantages` (the Autoconhecimento pattern).
- `Feat#halvesMovementBase`.
- `removeEffectsFrom` / `hasEffectFrom`.
- The client's `MovementBudget` and route checkpoints.

What's still missing:
- A record of the **distance** moved this Turn.
- A marker for "the attack before this movement".
- A flag saying an attack **is a Reação**.
- First-in-order detection.
- Heading-scoped movement.

Goal: wire every clause end to end (core, the API where the wire needs it, the client), add tests and record the rulings.

Core version: start at **0.1.7.17** and bump the 4th number once per phase. The API dependency and the client version follow core.

## Table rulings (2026-10-17), to save to memory
- **Se Mover e Atacar**:
  - "Após se mover por uma Distância Curta ou superior" = at least **3UD moved in total this Turn**, where `Range.DISTANCIA_CURTA` begins. Every kind of movement counts: PA movement, manoeuvre moves, Reposicionar, Acrobático, Investida and granted moves.
  - Every attack roll after that point in the same Turn gets the Vantagem.
  - "Imediatamente após efetuar uma Rolagem de Ataque" = the first movement bought after the Turn's latest action, when that action was an attack roll.
- **Movimento Acrobático** *replaces* that Turn's Reposicionar:
  - It is the same Ação Livre.
  - It is under the same mutual exclusion with PA movement (the reposition ruling, 0.0.55).
  - Its distance is half the Destreza total in UD, plus 2UD per Título held.
  - It provokes no Reações.
- **Porta-Estandarte**:
  - "Primeiro a agir" = the holder personally holds **the first slot of the turn order** (not just a group initiative win).
  - "Em direção a" = the move **ends at a smaller hex distance** to the target than where it started: an enemy for the holder, the holder for an ally.
- **Iniciativa Aprimorada**: the Vantagem de Iniciativa is **picked when the Talento is acquired** (the Autoconhecimento shape). It is granted only while the Iniciativa total is 4 or more, and the Talento's own +1 counts toward that.
- **Readings (no question needed)**:
  - "Reações de seus inimigos" = an attack made as a Reação: Defender o Perímetro, Impedir Conjuração, a fired Preparação, or a Título reaction attack.
  - Esquiva's "se moveu em seu último Turno" = any movement or Reposicionar that Turn. It is granted as a turn-end Blessing that lasts until the holder's next Turn, so it covers exactly the attacks "fora de seu Turno".
  - Solar/Lunar: "atingidos pela explosão" = the explosion's targets. "Causar danos" = that target actually lost PV. "Títulos Bruto Despertos" = held Títulos of `TitleArchetype.BRUTO`.

## Phase 0: movement ledger on the sheet (0.1.7.17)
- `CombatantSheet` / `AbstractCombatantSheet` get:
  - `recordDistanceMoved(int ud)` and `getDistanceMovedThisTurn()`, reset in `startTurn`.
  - `hasMovedThisTurn()`, true when movements, repositions or distance this Turn are above 0.
  - `getMovementsAtLatestAction()`: `recordAction` stores `getMovementsTakenThisRound()` at the moment each action is filed. Turn-sliced like `getActionsThisTurn()`.
- This core still never does geometry: the caller states the UD.
- The client calls `recordDistanceMoved` at every commit point in `SceneGridController`:
  - an ordinary move (≈11364)
  - a manoeuvre move
  - a Reposicionar (≈12370)
  - a charge's movement
  - an Ás na Manga granted move
- Update the gap-catalog Movimento Base row: "distance is now recorded per Turn; direction still is not".

## Phase 1: ESQUIVA (0.1.7.18)
Core:
- Add a `reaction` boolean to `IncomingAttack` and `DeliveredAttack`.
- Add the hook `Feat#resolveReactionDefenseBonus(DefenseType, CombatantSheet holder)`, default 0. ESQUIVA returns 2.
- Read the hook in two places:
  - `AttackReceiver` adds it to `defenseTotal` beside `areaOfEffectDefenseBonus` (combat/AttackReceiver.java:182).
  - `AttackDelivery`'s sheet-target Defesa (combat/AttackDelivery.java:577) adds it when the target is a PC and `reaction` is set.
- ESQUIVA's second clause: `resolveTurnEndBlessings` returns `Blessing(DEFESAS, +1, 1, SELF, name()).countingDownAtTurnStart()` when `holder.hasMovedThisTurn()`. This mirrors `EscudeiroFeat#DEFESA_TARTARUGA` (feat/EscudeiroFeat.java:171).

API:
- Add `boolean reaction` to `RollRequestMessage` and `RollRequestedEvent`, with a compatibility constructor that defaults it to false.
- `SceneService` relays the flag.

Client:
- Mirror the DTO fields.
- Set `reaction = true` when the attacker is acting as a Reação, i.e. `pendingReactionAttack` or `pendingPreparedFiring` is held by the attacker. This covers both `requestDefenceFromTargets` (≈14091) and the PvP `DeliveredAttack` path.
- `incomingAttackFor` (≈15014) passes the flag through.
- Log line: "Esquiva: +2 contra Reação".

## Phase 2: SE MOVER E ATACAR + INVESTIDA AQUÁTICA's first clause (0.1.7.19)
- **Vantagem**: SE_MOVER_E_ATACAR overrides the longest `resolveSkillRollBonus` (holder form). It returns `Skill.ADVANTAGE_BONUS` when the Perícia is an attack skill and `holder.getDistanceMovedThisTurn() >= 3`.
- **+2UD**: add a sheet-aware `Feat#resolveRoundMovementIncrease(int movementIndex, Character, CombatantSheet holder)` that defaults to the 2-arg form. `MovementServiceImpl`'s sheet overload calls it.
  - SE_MOVER_E_ATACAR returns 2 when both hold: the Turn's latest action is an attack roll, and `movementIndex == holder.getMovementsAtLatestAction()`, meaning this is the first movement bought after the attack.
  - Like every other movement figure, the 2UD is per PA.
  - The client needs nothing new: `allowanceAt` already calls `getMovementBase(mover, index)`.
- **Investida Aquática, first clause**:
  - The client records the Nadar manoeuvre's Atletismo roll as a `CombatantAction`, with its outcome, at `onMovementManoeuvre` (≈11440).
  - Add `manoeuvre` to `CombatantAction` if it isn't there yet.
  - INVESTIDA_AQUATICA's `resolveAttackActionPointAdjustment` returns −1 when the Turn's latest action is a successful NADAR roll. `ActionPointsService#getAttackCost` keeps its 1PA floor.

## Phase 3: MOVIMENTO FURTIVO's price + MOVIMENTO ACROBÁTICO (0.1.7.20)
**Furtivo**
- Add a sheet-aware `Feat#halvesMovementBase(Character, CombatantSheet)` that defaults to the 1-arg form. MOVIMENTO_FURTIVO returns true while the holder is Escondido (`getHidden()`) and has fewer than 7 Graduações in Furtividade.
- `MovementServiceImpl` asks it on the sheet path, in the existing halving stage.
- Update the "Multiplicative stages" and Esconder-se gap-catalog rows.

**Acrobático**
- Add `Manoeuvre.MOVIMENTO_ACROBATICO`.
- `RepositionService` gets `canMoveAcrobatically(sheet, round, ctx)`: the holder has the Talento, `canReposition` is true, and no acrobatic move was made yet this Rodada.
- It also gets `getAcrobaticDistance(sheet)`, returning `DEX total / 2 + 2 × getAllTitles().size()`.
- And `beginAcrobatic(...)`, which claims the Reposicionar slot and marks the acrobatic move.
- `MovementReactionService` exempts it, the same way it exempts Reposicionar.
- Client: a "Movimento Acrobático" button beside Reposicionar in the Ações de Combate panel. It reuses the Reposicionar path flow (≈12355–12396) with the acrobatic distance, plus the i18n keys.

## Phase 4: initiative: INICIATIVA APRIMORADA, LIDERAR O AVANÇO wiring, PORTA-ESTANDARTE (0.1.7.21)
**Iniciativa Aprimorada**
- Add `feat.VantagemDeIniciativaAprimoradaFeat`, modelled on `VantagemDeEgoEscolhidaFeat`:
  - Options: the `InitiativeAdvantage` values not already held.
  - Its `getGrantedEgoAdvantages` grants the pick only while the Iniciativa total is 4 or more. It keeps the +1 `resolveEgoBonus`.
- The constant declares `resolveRequiredChoices`.
- Guard against recursion if the Ego total ever reads `getAllEgoAdvantages`; check this during implementation.
- Client `FeatCatalog`: encode and decode the pick the way Autoconhecimento does (feat/FeatCatalog.java:226, 365, 435).

**Liderar o Avanço (client wiring)**
- In `onCombatStarted` (≈8494), each client runs `InitiativeBlessingService` for the winning group's holders and calls `coreScene.applyInitiativeBlessings` on its own Scene.
- Watch out: `applyInitiativeBlessings` revokes earlier grants, so merge all holders of the winning group into **one** call.

**Porta-Estandarte**
- Core:
  - `Scene#isFirstToAct(sheet)`: the first slot of the combat's order.
  - `Feat#resolveFirstToActBlessings()` returns two `ACTION_POINTS` `SELF_AND_ALLIES` Blessings: +1 for 2 Rodadas and +1 for 1 Rodada, which gives "+2PA na primeira, +1PA na segunda".
  - `Scene#applyFirstToActBlessings(holder, blessings)`, mirroring `applyInitiativeBlessings`.
- Heading:
  - A new record `scene.MovementHeading(Set<UUID> approachedSheetIds)`. The caller fills it with every combatant the move ends nearer to.
  - `Scene#getHeadingMovementBonus(mover, heading)`, valid only within `isWithinFirstCombatRounds(2)`, gives:
    - +2 when the mover is a first-to-act Porta-Estandarte holder approaching an enemy;
    - +2 when the mover is that holder's ally approaching the holder.
  - These don't stack past one +2 each way.
- Client:
  - Apply `applyFirstToActBlessings` in `onCombatStarted`.
  - In the move commit (≈11346), build the heading from `from` / `to` with `HexGrid.distance`, and wrap `allowanceAt` with the heading bonus. The hover `quote` does the same for each destination.

## Phase 5: INVESTIDA SELVAGEM movement RD + SOLAR / LUNAR (0.1.7.22)
**Movement RD**
- `ChargeService#openMovementWard(sheet)` grants an open-ended, sourced `DAMAGE_TAKEN_REDUCTION` of `Resistance.RDS.instances(titles)`.
- `closeMovementWard(sheet)` calls `removeEffectsFrom(INVESTIDA_SELVAGEM.name())`.
- `ChargeResult#unappliedMovementDamageReduction` stays as the figure the ward uses.
- Client:
  - Open the ward at charge begin (≈12644 / 13260).
  - Close it once the charge attack resolves or is abandoned.
  - `broadcastStatusOf` the charger so a Defender o Perímetro reactor's mirror sheet sees the RDS. Verify that the status payload carries a `TemporaryBonus`.

**Solar / Lunar**
- Add `Feat#resolveChargeAftermathEffects(CombatantSheet holder, Weapon chargeWeapon, int enemiesHit, int enemiesDamaged)` → `List<TemporaryEffect>`.
- Add `ChargeService#applySavageAftermath(...)`, which applies the effects only when `enemiesDamaged >= 3`.
  - **Solar**: a sourced DEFESAS bonus of `1 + Bruto Títulos (+ enemiesHit if an equipped Escudo is MEDIUM/HEAVY)`, lasting `enemiesHit` Rodadas.
  - **Lunar**: a sourced `LifeSteal(1 + Bruto Títulos (+1 if the charge weapon is MEDIUM/HEAVY), enemiesHit Rodadas)`.
- Client: after the explosion's targets have been resolved (≈13410 / 13646), count the enemies hit and those whose PV fell, call `applySavageAftermath`, log it and broadcast.

## Cross-cutting
- Rewrite `MobilidadeFeat`'s class javadoc and each constant's comments to describe the real wiring. No TODO should remain.
- CLAUDE.md gap catalog: update the Movimento Base, Multiplicative stages, Investida, Movement-triggered Reações and Esconder-se rows.
- Update `docs/rules/talentos-index.md` status.
- Add `docs/plans/mobilidade-wiring-plan.md` in core (this plan).
- Add the memory file `mobilidade-rulings.md` plus a MEMORY.md line.
- Add client i18n strings (all bundles).

## Verification
- After each phase, run `./gradlew test` in aventyrs-core. Add new tests in `GeneralFeatEffectIntegrationTest` / `MovementServiceTest` / `RepositionServiceTest` / `ChargeServiceImplTest` / `FeatInitiativeTest` / `AttackReceiver` tests:
  - **Esquiva**: +2 vs a reaction attack only; +1 after moving, gone at the next Turn start.
  - **Se Mover e Atacar**: Vantagem at 3UD but not at 2UD; +2UD only on the first movement after an attack.
  - **Aquática**: −1PA after a successful Nadar, none after a failed one; floor of 1PA.
  - **Furtivo**: halved while Escondido, not halved at 7 Graduações.
  - **Acrobático**: distance formula; refused after PA movement or a second time in a Rodada; provokes no Reações.
  - **Iniciativa Aprimorada**: the Vantagem appears exactly when the Iniciativa total reaches 4.
  - **Porta-Estandarte**: +2/+1 PA for the first slot only; heading bonus only in Rodadas 1–2.
  - **Savage**: RDS during the ward and gone after; Solar/Lunar only at 3 or more damaged, with the right Duração.
- aventyrs-api: `./gradlew test`, and check that the roll-request round-trip carries `reaction`.
- Client: `./gradlew test` (`SceneGridControllerTest`, plus `MovementBudget` / `FeatCatalog` round-trip tests). Then use the `run` skill for a two-client scene:
  - A foe Defender-o-Perímetros an Esquiva holder.
  - A Se Mover e Atacar holder moves 3UD then attacks.
  - A Porta-Estandarte holder first in order sees +2PA.
  - A Savage Lunar charge hits 3 foes.
