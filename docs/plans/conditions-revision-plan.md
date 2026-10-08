# Condições e Malefícios revision + base Agarrar manoeuvre

## Context

`docs/rules/condicoes-e-maleficios-.txt` was re-imported in full. The first import was truncated, and the new text rewrites the catalogue as a **two-layer model**:

- **Condições** (Abalado, Caído, Agarrado, …) are built from **Estados**:
  - Desprevenido: −4 Defesas, up from −2.
  - Fraqueza: Desvantagem on Perícias and Dano.
  - Favorecido: Vantagem on named Perícias.
  - Desacordado: no actions for 2 Rodadas, and applies Caído.
- **Desarmado** is gone.
- **Escape actions are fully restated:**
  - Agarrado: resisted Ataque.
  - Imobilizado: Furtividade.
  - Devorado: a single big hit.

Core's `ConditionType` was authored from the truncated text, so most constants are now wrong. The rules also never define how to *grab*, so a base **Agarrar** manoeuvre available to every character is in scope too.

## Rulings (user, 2026-10-07)

### General
- **Estados are binary.** Two Condições that confer the same Estado apply it once (Caído + Envenenado = one Fraqueza, −2). Different Estados combine. Vantagem/Desvantagem stay flat ±2.
- **"Rolagens de Perícia" include defensive rolls** (Esquiva e Aparar).
- **Durations:** per the inflicting source unless the text says the condition is open-ended (Caído, Agarrado, Imobilizado, Devorado).
- **Inconsciente** = PV below 0. The other undefined conditions (Surdo, Enredado, Exausto, …) will be described later and are out of scope.

### Fear
- **One rung at a time, strongest wins.** A weaker fear never overrides a stronger one.
- **Apavorado:**
  - Fraqueza within Longa.
  - Desprevenido within Média.
  - Within Curta of the source, core **refuses every non-movement action**.
- **Assustado:** Fraqueza within Média, Desprevenido within Curta.
- **Abalado:** Fraqueza within Curta.
- **Decay:** Apavorado → Assustado → Abalado, with a default of 2 Rodadas per rung.

### Other Condições
- **Amaldiçoado:** always Desprevenido.
- **Flanqueado:**
  - The surrounders needed are 1 + the target's Categoria, with a minimum of 2, spread around the target.
  - The **client derives it** from token positions.
  - The target is Desprevenido, and its attackers are Favorecido on the **Ataque roll**. This replaces the current Dano Vantagem.
- **Caído and Cego:**
  - Attackers are Favorecido on Ataque.
  - Anyone who attacked the target also gets Vantagem on Esquiva e Aparar against that target's attacks for as long as it stays Caído or Cego.
- **Desarmado:** removed entirely. A disarmed character just has no weapon in hand.
- **Devorado:**
  - "Imobilizado → Pronto" in the text is a typo for Devorado → Pronto.
  - The escape GD is the devourer's DF, converted to a GD level and dropped 2 levels, with a minimum of Médio.

### Agarrar
- **Cost and roll:** 2PA, Ataque Corpo-a-Corpo against the target's Defesa (GD). No damage, no Críticos, no Correntes. Success makes the target Agarrado, with the captor as the source.
- **Imobilizado** comes only from specific effects, never from base Agarrar.
- **Captor constraints:**
  - The captor needs a free hand or a usable limb/Arma Natural.
  - The target can be at most 2 Categorias larger than the captor.
  - The captor moving releases the target.
  - The captor can release at will.
  - The captor becoming Caído, Imobilizado, Desacordado or ≤0PV releases the target.
- **Escapes** cost 2PA each.
- **Escape resolution against a monster:** the character's Ataque is rolled against the monster's Ataque GD, as a regular roll that must surpass the GD.
- **A monster escaping a character:** the same contest applies, with the player rolling to keep the hold.
- **Character vs character:** left open.

<!-- implementation phases appended after design -->

### Follow-up rulings (same day)

**Imobilizado and Desacordado defence**
- The holder still rolls Esquiva e Aparar. It **succeeds unless it is a Falha Crítica**.
- Monsters make no defence roll, so a monster under either condition just has its DF with Desprevenido −4.

**Agarrar limbs and hands**
- Membro Ausente (Braços) means **no Agarrar at all**, even with an Arma Natural. Today's veto stays.
- Holding someone **occupies one hand per hold**:
  - that hand can't wield anything;
  - two-handed weapons are unusable while holding;
  - a second grab needs another free hand or limb.

**Flanking count**
- Only able enemies count toward flanking. Caído, Imobilizado, Desacordado and Devorado enemies don't count; Agarrado ones do.

**Sourceless fear**
- A fear with no source (Pânico's Apavorado) is **always in range**, so every band applies.

**Bocarra**
- Bocarra's own damage accumulation overrides the generic single-hit ≥ 2×Vigor escape for its victims.

**After escaping Devorado**
- Abalado and Desprevenido both last 1 Rodada, with the devourer as the source.

**Assumed defaults (not asked)**
- Caído and Membro Ausente (pernas) halve Movimento Base **once**, not twice.
- Disease immunity covers the infecting monster's whole template.

## Version
- Core is at 0.1.4.1.
- This change removes `DESARMADO` and `getAttackerDamageBonus`, so the bump is **0.1.5**: a manual minor bump per `RELEASING.md`.
- Each phase ships as its own build: 0.1.5.0, 0.1.5.1, ….
- API and client pin the core version as each relay or UI phase lands.

## Key findings from exploration
- A roll veto already exists: `AbstractSkillInteraction#applyTo` → `isSkillUsePrevented` (~l.354), throwing `SKILL_USE_PREVENTED`. Extend it rather than adding a second one.
- **Monsters ignore their own Condições** on most paths:
  - `MonsterRules.skillDifficulty` doesn't add `getConditionBonus`;
  - fixed-stat `MonsterSheet#getDefense` returns the raw value.
  
  This has to be fixed, or Estados on foes do nothing.
- Envenenado's hard-coded −1 multiplier is relied on by three sources: `RetaliationResolver:38` (Espinhos), `effect/InocularVeneno:48` and `effect/VenenoVampirico:55`.
- `ChargeServiceImpl` is the service template, because it reports PA instead of spending them. Its pieces:
  - cost getter;
  - `canX` check;
  - `begin` that throws;
  - shared `refusalFor`;
  - record result;
  - `applyOutcome`.
- `MovementReactionService#getProvokedReactors` already lists Defender o Perímetro opportunities.

All paths below are relative to `aventyrs-core/aventyrs-core/src/main/java/org/aventyrs/core/`.

## Phase 0: Estados layer and catalogue rewrite (core)

**Design.**
- Desprevenido, Fraqueza and Desacordado become `ConditionType` constants with `isEstado()`. They ride the existing `getImplied()` graph:
  - `activeConditionOrigins` keys by type, which gives the binary rule for free;
  - range scoping and `Feat#suppressesImpliedCondition` keep working.
- Favorecido is outward-facing, so it is a hook, not a held condition.

**`sheet/ConditionType.java`**

*New Estados*
- `DESPREVENIDO` −4 (make the constant public).
- `FRAQUEZA`: `SKILL_ROLL_BONUS` −2 and `DAMAGE_ROLL_BONUS` −2. Esquiva e Aparar is covered because it reads `SKILL_ROLL_BONUS`.
- `DESACORDADO`: 2 Rodadas. On apply it also applies an open-ended `CAIDO`. This is not an implication, because Caído outlives it.

*Fear*
- `ABALADO` → Fraqueza@Curta.
- `ASSUSTADO` → Fraqueza@Média and Desprevenido@Curta.
- `APAVORADO` → Fraqueza@Longa, Desprevenido@Média, and an action restriction @Curta.
- Add `fearRank()`.

*Other conditions*
- `AMALDICOADO` → always Desprevenido.
- `FLANQUEADO` → Desprevenido, plus attacker favour `ATTACK_ROLL`.
- `CAIDO` → Desprevenido + Fraqueza:
  - `halvesMovementBase()`;
  - favours `ATTACK_ROLL` and `DEFENCE_AGAINST_HOLDER`.
- `AGARRADO` → Desprevenido; refuses movement; no Desvantagem.
- `IMOBILIZADO` → Desprevenido; refuses everything but its escape; favour `ATTACK_ROLL`.
- `DEVORADO` → Fraqueza; attacks limited to Arma Leve or Arma Natural.
- `CONFUSO`:
  - +1PA surcharge;
  - refuses Ação Livre and Reação;
  - favour `ATTACK_ROLL`.
- `CEGO` → Desprevenido; favours `ATTACK_ROLL` and `DEFENCE_AGAINST_HOLDER`.
- `FERIDAS_DOLOROSAS` → `preventsHealing` + `preventsResourceRecovery()`.
- `ENVENENADO` and `DOENTE` → Fraqueza only. Drop `LIFE_MULTIPLIER` and the Selvagem TODO.

*Removals and docs*
- **Delete `DESARMADO`.**
- Replace `getAttackerDamageBonus()` with `Set<AttackerFavour> getAttackerFavours()`, where `AttackerFavour` is `{ATTACK_ROLL, DEFENCE_AGAINST_HOLDER}`.
- Rewrite the javadocs for the new source text.

**`sheet/Condition.java`**
- `extraEffects` holds per-source magnitudes. They are not binary-deduplicated.
- `attackedBy` ledger.
- `decayed()` carries both forward.

**New subclasses**
- `Poisoning`: Dano Natural each Rodada via `applyRoundEffect`, as `Bleeding` does.
- `Disease`: has a `propagates` field.
- `Grappled`: carries the captor and the hand it occupies.
- `Immobilization`: carries an optional pre-set escape GD.

**`sheet/AbstractCombatantSheet.java`**

`applyCondition` (~2354)
- Fear exclusivity: a held fear that is stronger refuses the incoming one; otherwise the incoming fear replaces every rung.
- Desacordado applies Caído alongside itself.
- Source-aware immunity via `isImmuneToConditionFrom(type, source)` (diseases).

`heldConditions()`
- Keeps only the strongest fear, including Pânico's.
- Prunes a `Grappled` whose captor can no longer hold.

Sourceless fear
- `Condition#appliesWithin` treats a null source on a fear as in range.

`getConditionBonus` (~2563)
- Sums the Estado effects once per type, plus each instance's `extraEffects`.

New methods
- `getAttackerAttackRollBonus(ctx)`.
- `favoursDefenceBy(defender, ctx)`.

`disarm` / `rearm`
- Drop the `DESARMADO` handling and keep the weapon bookkeeping.

**Wiring**
- `skill/AbstractSkillInteraction`:
  - remove the `targetCondition` dano term (~1099);
  - add the attacker roll favour in `applyAttackTargetBonuses` (~636).
- `combat/AttackReceiver` (~169) and `combat/AttackDelivery`: apply both favour directions.
- `scene/Scene#recordAttack` calls `defender.noteAttackedBy(attacker)`.
- Monster conditions:
  - `monster/MonsterRules.skillDifficulty` adds the condition bonus;
  - `monster/MonsterSheet#getDefense` adds the DEFESAS condition bonus.
- Envenenado magnitude: `combat/Retaliation`, `RetaliationResolver`, `InocularVeneno` and `VenenoVampirico` build `ENVENENADO` with `LIFE_MULTIPLIER −1` as an extra effect.
- `InflictedCondition` gains a nullable `source`.
- `feat/GorgonaFeat`: update the `MARCA_DA_MALDICAO` TODO, which would now imply a permanent −4.

**Tests**
- Update:
  - `ConditionTest` (−4, new fear bands, Agarrado has no Desvantagem, DESARMADO cases deleted);
  - `ConditionTypeTest`;
  - `ImpliedConditionSuppressionTest` (Caído now also gives Fraqueza);
  - `DamageBonusSummingTest`;
  - `WeaponDrawServiceTest`;
  - Envenenado tests (Espinhos, Correntes, Titã, NatureSummon, AreaAttack, Autocontrole, ConditionCleansing);
  - Defesas asserts in `CriticalEffectsTest`, `PolimorfismoCorrentesTest`, `AlmaElementalAbilityTest`, `EgoSetbackTest`.
- New:
  - `EstadoDeduplicationTest`;
  - `FearLadderTest` (strongest wins, decay, bands, sourceless);
  - `AttackerFavourTest`;
  - `MonsterConditionTest`;
  - `DesacordadoTest`.

## Phase 1: Action gating

**Action kinds**
- New `action/ActionKind`: MOVEMENT, ATTACK, SKILL_ROLL, DEFENCE, ABILITY_ACTIVATION, SPELL_CAST, ARMING, FREE_ACTION, REACTION, STAND_UP, the ESCAPE_* kinds, RELEASE_GRAPPLE.
- `ActionKind.of(SkillType, SkillRoll)` maps a roll to its kind.

**Refusals**
- `ConditionType#refuses(ActionKind)` + `getRestrictionRange()`, used by Apavorado at Curta. The old boolean gates become derived from these.
- `AbstractCombatantSheet#refusalForAction(kind, ctx)` is the single veto. `isMovementPrevented` and the others wrap it.
- It is enforced by:
  - the existing veto in `AbstractSkillInteraction`, under the new message `ACTION_PREVENTED_BY_CONDITION`;
  - `MovementServiceImpl`, `RepositionServiceImpl`, `ChargeServiceImpl`, `WeaponDrawServiceImpl`, `MountServiceImpl`;
  - activation paths (`ActiveAbilityServiceImpl`, `AbstractTitleAbilityInteraction`);
  - casting paths (`SpellCastingServiceImpl`, `MimetizedSpellCastingServiceImpl`);
  - `ReactionOptionsService`.

**Defence under Imobilizado or Desacordado**
- DEFENCE is never refused.
- In `AbstractSkillInteraction`, an Esquiva e Aparar roll while Imobilizado or Desacordado is an automatic success unless it is a Falha Crítica. Reuse the existing critical-failure detection.

**Other rules**
- Confuso +1PA: `ActionPointsServiceImpl` and the cost getters of the movement, charge, draw, mount, activation and cast services. Reposicionar, being an Ação Livre, is refused.
- Caído halves Movimento Base once: `MovementServiceImpl#getMovementBase`, OR'd with `Feat#halvesMovementBase`.
- Torpor's derived Imobilizado now refuses every action, and it cannot be escaped.
- Petrificado refuses everything.

**Tests**
- New: `ActionVetoTest`, `ConfusoTest`, `ImmobilizedDefenceTest`.
- Update: `ChargeServiceImplTest`, `MovementModeTest`, `EgoSetbackTest`.

## Phase 2: Agarrar, escapes, release, Levantar-se

**Manoeuvres**
- `action/Manoeuvre` gets AGARRAR, LIBERTAR_SE_DO_AGARRAO, MANTER_AGARRAO, LIBERTAR_SE_DA_IMOBILIZACAO, LIBERTAR_SE_DA_PREDACAO, LEVANTAR_SE.

**New `character/services/GrappleService` (+Impl), ChargeService-shaped**
- `grabMonster`: Ataque CaC vs the foe's DF.
- `defendGrab`: the player's Esquiva e Aparar vs the foe's Ataque GD.
- `escapeFromMonster`: the player's Ataque vs the foe's Ataque GD.
- `holdAgainstMonster`: the player's Ataque vs the foe's Ataque GD; failing releases the foe.
- `escapeImmobilization`: Furtividade vs the pre-set GD or the captor's Ataque GD.
- `applyOutcome`, `release` (Ação Livre), `releaseAllHeldBy(captor)` (on captor movement), `canMaintainGrapple`.
- Costs: grab and escapes 2PA each.
- Rolls go straight through the Interaction with `SkillRoll.targetValue`, so there is no damage, Crítico or Corrente.

**Gates**
- Size: at most 2 Categorias larger. Promote `AgarrarEDerrubar.MAXIMUM_SIZE_DIFFERENCE` into a shared helper using `CharacterSizeService`.
- A free hand or limb:
  - promote `CharacterSheet#handCost` into a shared `HandBudget`;
  - a held grapple occupies one hand;
  - Membro Ausente (Braços) refuses.
- Immunity: Raízes Longas is reported as a refusal before the roll.

**Release triggers**
- The captor becomes Caído, Imobilizado or Desacordado, or drops to PV < 0 (pruning in `heldConditions`).
- The captor moves (the caller invokes `releaseAllHeldBy`).
- The captor lets go at will.

**New `StandUpService` (+Impl)**
- 1PA; removes Caído; returns provoked reactors via `getProvokedReactors(…, LEVANTAR_SE)`, which is not exempt.
- Add `Feat#standsUpAsFreeAction` for Submissão.

**Hooks**
- Subjugar (`StrengthAbility#resolveManoeuvreRollBonus`).
- Submissão (`ArtesMarciaisFeat`).
- Herança Anfíbia (`BestialFeat` 72–89).
- Pele Escorregadia stays listed only.

**Tests**
- `GrappleServiceImplTest`: both directions, the gates, every release trigger, no damage or crit, the hand budget.
- `StandUpServiceTest`.
- Immobilization escape test.

## Phase 3: Devorado, Cego, Confuso, Envenenado, Doente, Feridas Dolorosas

**Devorado**
- `DevourService` + `sheet/Devoured`.
- Escape GD = the devourer's DF as a `DifficultyLevel`, dropped 2 levels, minimum Médio.
- Arma Leve or Arma Natural only.
- Meio-Dano inside, OR'd into `DamageService` half-damage.
- A single hit ≥ 2×Vigor frees the victim. Bocarra keeps its own accumulation.
- On escape: Abalado and Desprevenido for 1 Rodada, with the devourer as the source.
- Shielding: `isShieldedFromExternalEffects()` is refused in attack and cast target validation unless the actor is the devourer.

**Cego**
- `BlindCheck` reach tiers: PERSONAL ≤2, ADJACENT_OR_SCENERY ≤3, BEYOND_ADJACENT ≤5.
- Only Força- and Destreza-governed Perícias roll the d6.
- Visual Atenção auto-fails via a `SkillRoll#visual()` flag.
- `DuelistaFeat` Combater às Cegas keeps exempting everything but BEYOND_ADJACENT.

**Doente**
- `DiseaseService` propagation: adjacent characters roll a d6 at the start of each Rodada, and a 1 infects them.
- The infecting template is immune.

**Feridas Dolorosas**
- `preventsResourceRecovery` is read by PD/PM recovery, `Regeneration`, life steal and `RestService`.

**Tests**
- `DevoradoEscapeTest`, `BlindReachTest`, `DiseasePropagationTest`, `FeridasDolorosasRecoveryTest`.
- Update `ArtilhariaActivationTest` and `DuelistaActivationTest`.

## Phase 4: API relay (aventyrs-api)

**New `/scenes/{id}/conditions` channel** (APPLY/REMOVE, relayed and not persisted, using the `/hidden` pattern)
- Request: `ConditionChangeMessage(target, type, op, rounds, sourceId, extraEffects, damagePerRound, escapeDifficulty, propagates, enchantment)`.
- Broadcast: `ConditionChangedEvent`.

**Other changes**
- `CombatantStateMessage` gains `List<HeldConditionDto>` so that clients mirroring a combatant can read its outward favours.
- `InflictedConditionDto` gains `sourceCharacterSheetId`.
- `RollRequestMessage` gains an optional `manoeuvre`, so a foe's grab or escape becomes a player roll.
- PvP is left open.

**Tests**
- Controller and DTO round-trip tests.

## Phase 5: Client (aventyrs-game-client)

**Flanqueado**
- A pure core helper, `scene/grid/Flanking#isFlanked(target, size, adjacentAbleEnemies)`:
  - required count is max(2, 1 + Categoria);
  - the largest angular gap must be ≤ 180°.
- The client's `SceneGridController#refreshFlanking()` derives it for every sheet on move, rebuild and condition events. Only able enemies count.

**Grid actions**
- Maneuvers panel: Agarrar, Soltar, the three Libertar-se, Levantar-se (with the `ReactionModal` for Defender o Perímetro).
- Moving the captor's own token releases its holds and relays a REMOVE.
- A roll-request modal handles grabs and escapes made by foes.
- Vetoed actions are greyed out. Confuso shows its +1PA.

**Relay and display**
- `title/TitleEffects` generalises `applyFear` into a general condition apply.
- Display: Estados shown apart from Condições, source chips and token badges (`SceneGridCharacterInfoMapper`, `EffectDisplayNames`, i18n keys).

## Docs and memory
- CLAUDE.md:
  - the "Vantagem flat +2 on dano" bullet (Flanqueado moves to the roll);
  - the gap catalog rows: Malefícios, Movement-triggered Reações, Reposicionar/Investida.
- Skills: `damage-and-combat`, `skill-roll-mechanics`, `building-a-foe`.
- A changelog for each release, per `RELEASING.md`.
- Save a `conditions-rulings` memory with every ruling above.

## Verification
- After each phase: `./gradlew test` in core, then API and client builds against the new core.
- Phases 0–3 are covered by the new tests listed in each phase. Grep shows no leftover `DESARMADO` or `getAttackerDamageBonus`.
- End to end, on a dev scene:
  1. A player grabs a blueprint foe.
  2. The foe's token can't move.
  3. Its Defesa shows −4.
  4. The player is Favorecido on attacks against it.
  5. The escape roll request appears on the foe's turn.
  6. Moving the captor releases the hold.
  7. Three tokens around a Categoria +1 foe flank it.
