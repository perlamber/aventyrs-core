# Wire the remaining AttributeAbilities

## Context
Ten Habilidades de Atributo are still only partly wired, or not wired at all:
- Six carry TODOs: SUBJUGAR, ESTILHAÇADOR, METABOLISMO RÁPIDO, CANALIZADOR DE MANA, RECUPERAÇÃO ASSOMBROSA and PARANOIA SAUDÁVEL.
- Four have no TODO and no wiring: BESTA DE CARGA, AGRESSÃO ANUNCIADA, VOZ DE OURO and FLOREIO ARCANO.

Most of the TODOs are stale. `HealingSource` (Magia/Título), `effect.RouboDeMana`, Encontrão, per-combat trackers and `TemporaryEffect` now exist. The goal is to wire each clause end to end (core, API where needed, client), add tests, and record the table rulings.

Core version: start at **0.1.7.10** and bump the 4th number once per phase. The API dependency and client version follow core.

## Table rulings (2026-10-16), to save to memory
- **Subjugar**: Derrubar is covered by Encontrão, which carries Lançar ao Chão. Vantagem applies to melee rolls on AGARRAR and ENCONTRAO.
- **Estilhaçador**: objects only (Atacar Objetos). Construto stays a TODO until the rules define one.
- **Paranoia Saudável**: a "0.0" pre-phase before Rodada 0. Rodada 0 itself is unchanged; the surprise / 2PA opening Rodada is out of scope.
- **Recuperação Assombrosa**: a core `TemporaryEffect#isHarmful()` that mirrors the status panel's buff/debuff split. Only counted Durações over 3 are capped, at application. Open-ended (null), until-Descanso and until-combat effects count as permanent.
- **Metabolismo / rest-equivalent heals**: Magia/Título heals get exactly +2. Rest bonuses apply only to a real Descanso; this needs the split in Phase 1.

## Phase 1 — Real Descanso vs rest-equivalent recovery (0.1.7.10)
Proposed design (open for your comments):
- Add `rest.RecoveryOrigin { DESCANSO, EQUIVALENTE }`.
- In `RestService`/`RestServiceImpl`, add `getRecovered{Hit,Magic,Determination}Points(Character, RestType, RecoveryOrigin)`. The 2-arg forms delegate with `DESCANSO`.
  - With `EQUIVALENTE`, return the Atributo × tier base (`recovered(...)`) plus only the new equivalent-scoped hooks.
- Add new default hooks returning 0: `AttributeAbility#resolveRestEquivalent{HitPoints,MagicPoints,DeterminationPoints}Bonus(RestType)`, plus `Feat` twins.
  - Only SUPERMOTIVADO overrides one, with `+1PD`, because its text is "Efeitos que te permitam recuperar PD …". Its LONGO +2 stays real-Descanso only.
- Change every "as if a Descanso" call site to pass `EQUIVALENTE`:
  - `effect/SpellHealingEffect.java:146,147,173`
  - `magic/catalog/VidaSpell` (the getRecovered* callers)
  - `title/curandeiro/DonsDoCurandeiroInteraction.java:76`
  - `character/services/AutocontroleEgoServiceImpl.java:69,102,103`
  - `skill/CompetencyActions.java:160-164` (Medicina)
  - `scene/MonsterAuraLedger.java:159`
- The current overrides (QualidadeFeat Saúde de Ferro ×2, MetamagicoFeat :481, DestinoFeat :90, the three ability enums) stay real-Descanso only. Each `resolveRest*Bonus` javadoc gets a line saying so.

## Phase 2 — Healing riders: METABOLISMO RÁPIDO + CANALIZADOR DE MANA (0.1.7.11)
- Move the private `DevotoFeat#isMagiaOrHabilidade` (feat/DevotoFeat.java:724) onto `HealingSource#isMagiaOrTituloAbility()` and reuse it in DevotoFeat.
- Add `AttributeAbility#resolveHealingReceivedBonus(HealingSource)` (default 0).
  - METABOLISMO returns +2 when `source.isMagiaOrTituloAbility()`.
  - It is added in `AbstractCombatantSheet#heal` (sheet/AbstractCombatantSheet.java:706-710), next to the Talento recipient adjustment, and skipped for life steal. It is therefore halved and Coma-capped like everything else.
- Add `AttributeAbility#resolveHealingReceivedManaBonus(HealingSource)` (default 0).
  - CANALIZADOR returns +1.
  - It is applied after `hitPoints.recover` when `recovered > 0`, the heal isn't life steal, and the source is a Magia/Título, via `recoverMagicPoints` (which already respects Feridas Dolorosas).
- Fix the unsourced magic heal at `effect/SpellEffectBonus.java:22` so it passes the parent spell's `HealingSource`. Otherwise Toque Divino / Marcas da Guerra miss the rider.
- Add `AttributeAbility#resolveManaStealBonus()` (default 0); CANALIZADOR returns 1.
  - In `combat/AttackDelivery.java:518-522`, when `manaSteal > 0`, add the attacker's bonuses once (non-cumulative), mirroring `LifeStealServiceImpl` ("amplify, never grant").
- Side fix found during exploration: `effect/OferendaMaldita.java:45` should call `healFromLifeSteal`.
- Rewrite the TODO comments in VigorAbility/FocusAbility and update the gap-catalog "Roubo de Mana" row in CLAUDE.md.

## Phase 3 — SUBJUGAR + ESTILHAÇADOR (0.1.7.12)
**SUBJUGAR**
- `ability/StrengthAbility.java:51`: the condition becomes `manoeuvre == AGARRAR || manoeuvre == ENCONTRAO`, with `ATAQUE_CORPO_A_CORPO`.
- Add `AttributeAbility#resolveDamageRollBonus(SkillType, CombatantSheet holder, CombatantSheet attackTarget, SceneContext)` (default 0).
  - SUBJUGAR returns `Skill.ADVANTAGE_BONUS` when `attackTarget.hasCondition(CAIDO|DESPREVENIDO, ctx)`. Caído already implies Desprevenido.
  - Sum it in `AbstractSkillInteraction#sumDamageBonus` (:1092-1141) beside the Título term, under a new `DamageContributionSource.ATTRIBUTE_ABILITY`. A null target (first pass) gives 0, and the attack-target pass (:637-647) picks it up.

**ESTILHAÇADOR**
- Objects: Vantagem on a `Manoeuvre.ATACAR_OBJETOS` roll with a Força-governed attack Perícia, via `resolveManoeuvreRollBonus`.
  - The hook lacks the governing Atributo, so add an overload that takes the resolved `AttributeDomain`, passed from `sumManoeuvreRollBonuses`.
  - Vantagem em Danos goes on `ObjectAttackServiceImpl#strike`'s `rawDamage` (+2) when the attacker holds it.
- Shield/armour: add `DeliveredAttackResult#getShieldBreak()` (target item + 2), resolved inside `AttackDelivery`'s `if (hit)` block (:308-324). It applies to the primary target, on a melee Perícia, when the attacker holds ESTILHACADOR.
  - Target item: the first equipped non-destroyed `ItemCategory.SHIELD`, else ARMOR (same filter as `effect/DefensiveCriticalEffects.java:35-40`).
  - If the shield is destroyed by those 2, the TODO's literal reading says the next hit falls to armour. The plan applies the damage only to the shield (which is how I read "caso o escudo seja destruído") and lets the following hit fall through.
  - Foes have no equipment, so nothing happens against them.
- Client: apply the report like `applyEquipmentRetaliation` (SceneGridController.java:13436) by calling `Item#applyDamage(2)`, and log it. Persist the target's equipment through the existing item-damage sync path used by retaliation.
- Keep one TODO: construto has no classification.

## Phase 4 — RECUPERAÇÃO ASSOMBROSA (0.1.7.13)
- Add `TemporaryEffect#isHarmful()` (default false), overridden as follows:
  - `Condition` → `type != ESCONDIDO`
  - `TemporaryBonus` → sign rule (negative = harmful, inverted for `SKILL_ROLL_COST`)
  - the continuous-damage effects (`getContinuousNature() != null`: Bleeding, ManaDrain, Withering, Poisoning, RecurringDamage/Dice, VampiricVenom, DeterminationDrain) → true
  - `Enchantment#isHarmful()` implementers already return it
  - ForcedTargeting → true
- Add `AttributeAbility#resolveHarmfulDurationCap()` → `OptionalInt` (empty by default); RECUPERACAO returns 3.
- In `AbstractCombatantSheet#applyEffect` (:1515, after the resistance shortening at :1525-1536) and in `applyCondition`/`received()` (:2660/:2720): if `effect.isHarmful()` and `remainingRounds != null && > cap`, call `shortenTo(cap)`. Skip it for until-Rest / until-combat-scoped wrappers.
- Client: `SceneGridCharacterInfoMapper#effects` (:113-160) reads the core classification (`isHarmful`) instead of its local rules, so the panel and the rule agree. Add a test that pins every current panel classification.

## Phase 5 — The four unmarked constants (0.1.7.14)
- **BESTA DE CARGA**: add `AttributeAbility#resolveCarriedWeightClassShift()` (default 0); it returns 1. Add `Character#getCarriedWeightClass(Item)`, the effective class shifted down, never below LIGHT.
  - Only consumers where lighter is a *benefit* read it: the Destreza penalty in `EsquivaEApararInteraction.java:184`, the two-hand budget in `item/HandBudget.java:48`, and the LIGHT checks in `ArtesMarciaisFeat.java:212` / `DuelistaFeat.java:279`.
  - Base-category readers stay unchanged: Especializações, the Aprimoramento cap, Especialista em Armaduras, and the "heavy weapon" benefits in AtaqueCorpoACorpo/Escudeiro.
- **AGRESSÃO ANUNCIADA / FLOREIO ARCANO**: add a sheet-aware `AttributeAbility#resolveAttributeDomainRollBonus(AttributeDomain, Character, CombatantSheet)` that defaults to the existing 2-arg form, called from `AbstractSkillInteraction.java:421`.
  - "Imediatamente após" means the last entry of `sheet.getActionsThisTurn()`.
  - AGRESSÃO: Força/Destreza roll whose previous action's governing domain was CHARISMA → +2.
  - FLOREIO: Carisma roll whose previous action was a Magia cast → +2. Identify the cast by the `CombatantAction`'s `AttackSource instanceof Spell` or its Conjuração skill; confirm which during implementation.
  - The client needs no change beyond making sure spell casts and Perícia rolls are recorded. Verify that `SpellCastingResult#getRecordedAction` is filed.
- **VOZ DE OURO**: add `AttributeAbility#resolveSkillRollCostReduction(AttributeDomain governing)` (VOZ: 1 for CHARISMA) plus a determination price constant (2PD).
  - Add a `voiceOfGold` opt-in on `SkillRoll`. `ActionPointsService#getSkillRollCost` subtracts 1 (min 1PA) when the opt-in is set and the ability is held.
  - The caller spends 2PD via `spendDeterminationPoints`.
  - Client: a "Voz de Ouro (−1PA, 2PD)" toggle on Carisma-based roll dialogs, disabled with fewer than 2PD.

## Phase 6 — PARANOIA SAUDÁVEL: the 0.0 pre-phase (0.1.7.15, plus API and client)
Core:
- `InstinctAbility.PARANOIA_SAUDAVEL` provides `resolveActiveAbility()` → a `ParanoiaSaudavelActiveAbility`: 2PD cost, once per combat via `markAffectedThisCombat` (house reading: "uma vez por Cena" = per combat, `skill/UseWindow`).
- In `Scene`, add a pre-Initiative phase:
  - `openPreInitiativePhase(List<CombatantSheet> takers)` runs after `startCombat()` while the cursor is -1.
  - `getPreInitiativeActor()`, `finishPreInitiativeAction()`, and `isInPreInitiativePhase()`.
  - `SceneContext` exposes it.
  - The taker's budget: one action of `ActionCost` ≤ FIXED(2), FREE_ACTION or REACTION. Enforce it with a `CombatantSheet` pre-phase PA cap of 2 for that window.
  - When the phase empties, `next()` proceeds to Rodada 0, index 0, unchanged. The Rodada counter does not move, and Turn-start/Rodada effects don't tick.
- Tests: ordering among several takers (Initiative order among them, a reading), the refusal at <2PD or the second use, and the cap at 2PA.

API (`aventyrs-api/.../scene/SceneService.java`, `SceneRealtimeController.java`):
- After `startCombat`/`resetRotation` (:1114, :1164), the server collects opt-ins.
- Add a STOMP `/app/scenes/{id}/pre-initiative` endpoint: `take` (the holder pays 2PD) and `done`. The phase state is persisted and broadcast on `/topic/.../combat`, and the turn order stays server-owned.

Client (`SceneGridController.java`):
- In `onCombatStarted` (:8454), offer holders the opt-in, mirroring `announceLutherAllyOffers` (:8704).
- During the phase, the action-economy checks (:2303-2316, :4986, `isCurrentCombatant` :11106) treat the pre-phase actor as current, with a 2PA budget.
- The turn panel shows "Rodada 0.0 — Paranoia Saudável".

## Cross-cutting
- Replace each TODO comment with a description of the real wiring; keep only the construto TODO.
- Update CLAUDE.md gap-catalog rows: Roubo de Mana, the item-damage callers list ("Nothing calls applyDamage automatically yet" now has Estilhaçador), and the turn-order notes for the pre-phase.
- Add `docs/plans/attribute-abilities-wiring-plan.md` in core (this plan) and a memory file for the rulings.
- Client `AttributeAbilityCatalog` descriptions are unchanged.

## Verification
- `./gradlew test` in aventyrs-core after each phase. New unit tests per constant, next to `AttributeAbilityServiceTest`, `DamageInteractionTest`, `SceneTest`, `ActionPointsServiceImplTest`, `MovementServiceTest`/`DefenseServiceImplTest`.
  - Regression tests: a rest-equivalent Cura spell no longer gets +3 from Metabolismo, and a Coma heal with Metabolismo is still capped at 1PV.
- `./gradlew test` in aventyrs-api (pre-phase endpoint test) and the client (mapper classification test).
- Manual: run the API and client (`run` skill). Start combat with a Paranoia holder and take the 0.0 action. Hit a shielded character with an Estilhaçador and confirm the shield's Dureza drops by 2. Cast a heal on a Metabolismo + Canalizador target (+2PV, +1PM), and check that a long harmful effect on a Recuperação holder shows 3 Rodadas in the status panel.

## Status (2026-10-17) — built, core 0.1.7.10–0.1.7.15 + API + client

Deviations from the plan above, decided while building:
- **Phase 1**: no `Feat#resolveRestEquivalent*Bonus` twins — no Talento needs one yet (build for the second consumer); a Talento's Descanso bonus is simply a real Descanso's.
- **Phase 2**: `effect/SpellEffectBonus` stays unsourced — giving it the parent Magia's `HealingSource` would pay Metabolismo's +2 twice for one Magia (and refuse it in Coma as the same effect).
- **Phase 3**: the shield damage travels as an *amount* (`DeliveredAttackResult#getDefensiveEquipmentDamage`), resolved by the target's owner through `combat.ShieldBreak` — the attacker's client may hold only a stand-in. Remote targets get it on `AttackHitMessage#equipmentDamage`. Atacar Objetos has no client caller, so Estilhaçador's object half is core-only.
- **Phase 4**: the cap lives in `AbstractCombatantSheet#capHarmfulDuration`; the combat/Descanso-scoped wrappers skip it.
- **Phase 5**: Voz de Ouro is a parameter on `ActionPointsService#getSkillRollCost(…, buyReduction)` plus `#getBoughtReductionCost`, not a `SkillRoll` opt-in (the client prices the roll before building it). The client offers it on PA-priced competency rolls; GM checks charge no PA.
- **Phase 6**: a `PreInitiativeService` instead of an `ActiveAbility` (nothing timed to apply). No client subscription to the queue broadcast and no reconnect restore of a Rodada 0.0 underway (the client `Scene` DTO has ~115 construction sites) — the GM's next advance carries on.
