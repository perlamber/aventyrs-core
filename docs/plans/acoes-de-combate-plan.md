# Ações de Combate, Danos e Resistências — import plan

Source: `V19 - Ações de Combate, Danos e Resistências 20261013.docx`, converted verbatim to
`docs/rules/acoes-de-combate.txt` (2026-10-14; the 20261013 revision added Postura Defensiva and the
"Manobras de Combate - Reações" section, the damage/resistance text is unchanged from 20261012). Its resistance section supersedes the matching part of
`docs/rules/defesas-e-resistencias.txt`, which was updated to agree with it.

## Table rulings (2026-10-13)

- **One table of resistance values.** Every RD/RM/RE/RA/RDS/RC figure is a count of *instances* times the
  per-instance value in `character.Resistance`. "RD" (with no number) or "RD 1" is one instance; "RDS 5" or
  "RDS igual a 1 + …" is that many instances. No source writes a raw point figure any more.
- **An unnamed item reduction is RDS.** An item Favor that says only "Dano … sofrido é reduzido em -N"
  (Armadura Completa, Couraça, Meia-Armadura, Escudo Médio/de Corpo, the Capas) grants **N RDS
  instances**. This deliberately reaches more damage types than RD does. Items whose text names RD/RM/RA keep
  that resistance.
- **Queda and Asfixia deal Dano Primordial, and RA does not reduce Primordial** — this follows the text. It replaces
  the 2026-10-12 interim "RA only" ruling.

## Phases

### Phase 0 — resistance constants and the rule changes — *built (core 0.1.6.13)*
Behaviour changes it made, all by the rulings above: Escudo de Sombras and Pele Rija give one RDS instead of 3/2
(the number in "+3 em suas Defesas e RDS" is the Defesas'); every item "reduzido em -N" Favor moved from RD to RDS
(Armadura Completa/de Justa/Couraça/Meia-Armadura/Robe de Guerra, Escudo Médio/de Corpo, Manto Nortenho, the
Equilibrada and Defensora Obras-Primas); Vigor de Épona ("-1 todo dano sofrido") is RDS; the Sobretudo do
Inquisidor's Favor lost its "RM 1" (it is a Duração clause — Phase 2). Non-item unnamed reductions that scope a type
(Rigidez da Montanha's Físico, the Nature summons' Físico, Benção Vulcana/Elduriana, Opala's Profano) keep their
scope, sized in RDS instances. Devoto's "Margem Crítica Menor de seus inimigos -1" stays an enumerated 1, not RC.

1. `character.Resistance`: `RD(2)`, `RM(2)`, `RE(3)`, `RA(2)`, `RDS(1)`, `RC(2)`, with `instances(n)`. Also
   `RC_MAJOR_MARGIN` (1), `RA_CRITICAL_MARGIN` (1), `DURATION_REDUCTION` (1) and `EFFECT_CHAIN_BASE` (5).
2. Retire the scattered constants: `DamageService.DEFAULT_DAMAGE_REDUCTION`/`DAMAGE_TAKEN_REDUCTION_INSTANCE`,
   `DamageServiceImpl.ELEMENTAL_RESISTANCE_PER_INSTANCE`, `CombatantSheet.CRITICAL_RESISTANCE_INSTANCE`, the
   three `MonstrousTraits` ones, `EffectChainService.BASE_REQUIRED_MARGIN`. Each one either delegates to
   `Resistance` or is removed, with its callers moved over.
3. Audit every grant site: `ItemBonus`, `TemporaryBonus`/`Blessing`, `@Modifier` methods, and the `resolve*`
   resistance hooks on `Feat`/`AttributeAbility`/`SkillCompetencyAbility`/`EgoAdvantage`/Título abilities/monster
   abilities. Rewrite each value as `Resistance.X.instances(n)`, with `n` read from its rules text. Unnamed item
   reductions → RDS.
4. Rule changes: RE −3; RDS skips Profano/Sagrado/Umbral hits; RA skips Primordial; `QUEDA`/`ASFIXIA` are
   Primordial (unreduced).
5. An architecture test that fails on a raw numeric literal beside a resistance `ModifierType`.

Phases 1–7 were re-planned on 2026-10-14 — see below.

## Movement, manoeuvre and reaction rulings (2026-10-14)

| Topic | Ruling |
|---|---|
| Vertical axis | Each token has one persisted signed **`elevation` (UD)**: positive = airborne/climbing, negative = depth under water. Reach/range distance = `max(hexDistance, |Δelevation|)`. Movement cost = horizontal path UD + |Δelevation| UD. A Queda covers the height = elevation − the hex's ground level, once support is lost. |
| Terrain | GM paints full **TerrainCells**: `difficult`, `kind` GROUND/WATER/CLIMBABLE/OBSTACLE, `groundLevel`, optional `gd` (climb/current). Today's `difficultTerrain` list migrates into these. |
| Defender o Perímetro trigger | A hostile may react the **first time any hex of the route (the start hex included, once the mover leaves it) lies inside its melee reach**. At most once per hostile per movement. |
| Interruption | The move **pauses** at the provoking hex and the reaction resolves there. It then continues, unless the mover became Caído/Imobilizado/Agarrado/Inconsciente/Desacordado, or (Saltar only) took damage ≥ half its PV. Unspent UD stays in the Turn budget. |
| Impedir Conjuração | Defined by the user's Reações text. Melee reach, Ataque CaC when the casting is declared. Each 5 points of damage **after mitigation** gives −2 on the Rolagem de Conjuração. Conjurar Defensivamente prevents it. |
| Rendido | A voluntary Condição that the controller (or the GM for monsters) declares. It carries the Estado Desprevenido and ends when its holder takes any action. Imobilizar accepts **Rendido or Inconsciente** (PV < 0) targets. |
| Recuo Defensivo | No step may reduce the hex distance to any enemy within Curta of the start. The end hex must be farther from each of them than the start was. |
| Asfixia out of combat | A per-character counter. In combat it ticks at the start of the character's Turn. Out of combat the GM ticks "+1 Rodada" from Passar Tempo. Leaving the water or gas resets it. |
| Preparação | 3PA: name one action of ≤2PA with its choices fixed. Until the holder's next Turn begins, they may fire it once at any moment of another combatant's Turn, using a dedicated extra Reação. The trigger is narrated and not checked; the action is lost if unused. |
| Postura Defensiva | +2 Defesas (+3 while using an Escudo, checked live; Asas Adamantinas count, per the existing ruling) until the holder's **own next Turn begins** ("por 1 Rodada" reading). Re-taken each Turn for 2PA. Not cumulative with itself. |
| Sacrifício | Once per combat Scene. The ally must be adjacent to the interceptor. The interceptor is hit automatically, the Efeito Crítico Menor applies even without a crit (a Maior stays Maior), and every Corrente fires regardless of the Resistência à Corrente margin. |

### Readings assumed in the plan (approved with it)
- **Correr/Saltar "+2UD para cada GD superada":** +2UD per GD level beaten, counting the starting Médio. Beating
  Médio gives +2, beating Difícil gives +4, and so on.
- **Nadar's "exceto quando ele tiver Movimento Base Vertical"** is a typo for **de Natação**.
- **Entering water:** a WATER hex can be entered only by Nadar, or with a SWIM mode, or while flying.
- **Escalar:** elevation above ground level can be gained only on a CLIMBABLE hex, or with a flight effect.
- **Encontrão:**
  - It is an Ataque CaC roll that deals **no damage**.
  - On a success the push goes along the charge direction.
  - A push into an OBSTACLE or occupied hex stops short.
  - A push off a ledge causes a Queda; a push into water puts the target to swimming.
- **Agarrar's "Bônus ou Redutor igual à Categoria de Tamanho"** uses the captor's own Categoria.
- **Hostile manoeuvres outside combat:** Investida, Encontrão, Agarrar and attacks need combat, so the GM starts
  it. Imobilizar and Atacar Objetos against unattended scenery work outside combat.

## Architecture

### A. Board model (core `scene/grid`, API scene document, client canvas)
- **`BoardPosition(GridPosition hex, int elevation)`** and `HexGrid.distance3d(a, b)` = max(planar, |Δz|).
  `RangeBand.fromHexDistance` is fed the 3D figure everywhere: reach, `AttackRangeService` callers, flanking.
- **`TerrainCell(boolean difficult, TerrainKind kind, int groundLevel, Integer gd)`**:
  - `MovementMap` holds a `Map<GridPosition, TerrainCell>` instead of the `difficultTerrain` set; GROUND, level
    0, not difficult is the default.
  - `StepRules` gains `canEnter(cell, mode)`: OBSTACLE is never enterable, WATER needs swim/flight, and climbing
    needs CLIMBABLE or flight.
- **`MovementPathfinder` becomes 3D-aware:**
  - It works over `(hex, elevation)` nodes. Vertical steps cost 1, and are only allowed for flight, on a
    CLIMBABLE cell, or underwater.
  - A 2D path still works: elevation follows the ground level.
- **`HexGrid.line(a, b)`** for the straight-line manoeuvres (Investida, Encontrão), plus
  `MovementMap#isLineClear`.
- **API:**
  - `SceneParticipantEntry.elevation`, plus a persisted `flying` flag (an active flight source).
  - `SceneDocument.terrain: Map<cell, TerrainCellDto>`. A Mongo migration converts the old list.
  - `TerrainPaintMessage` takes a cell template.
  - `TokenMoveMessage` takes `path: List<BoardPositionDto>` + `manoeuvre`.
- **Client:**
  - A GM terrain palette (kind / ground level / GD brush).
  - An elevation badge on tokens ("▲4", "▼1"), and up/down controls while moving (PgUp/PgDn).
  - `positionalStateOf` derives `EnvironmentalState` (flying, submerged, altitude) from the elevation and the
    cell instead of `ORDINARY`.

### B. Reaction windows (one mechanism for every interrupting reaction)
- **Core:**
  - `ReactionTrigger` gains `ENEMY_MOVES_IN_REACH`, `ENEMY_CASTS_IN_REACH`, `ALLY_ADJACENT_ATTACKED` (Sacrifício)
    and `GRAB_FAILED`.
  - A new `action/GenericReaction` enum holds DEFENDER_O_PERIMETRO, IMPEDIR_CONJURACAO, SACRIFICIO,
    CONJURACAO_REATIVA, ATIVACAO_REATIVA and PREPARACAO.
  - `ReactionOptionsService#getAvailableReactions` merges four sources: the generic reactions eligible for the
    trigger, Título abilities (existing), `REACAO` Magias the reactor knows, and reaction-timed monstrous
    abilities.
- **Core `RouteReactionService`** (supersedes `MovementReactionService#getProvokedReactors`, which stays as a
  thin wrapper):
  - Signature: `List<ReactionCheckpoint> checkpoints(mover, MovementPath, Manoeuvre, Map<CombatantSheet,
    BoardPosition>, ctx)`.
  - It walks the steps and emits `(stepIndex, reactors)` the first time each hostile's reach covers a step.
  - Reach comes from `AttackRangeService#getEffectiveRange`. Correr clamps it to adjacent.
  - Recuo Defensivo and Reposicionar produce none. Investida's target is excluded unless it has Mestre Escudeiro.
    The exemption sources are kept (Pedras "2UD iniciais", `exemptsFromMovementReactions`).
- **API:**
  - A persisted `PendingAction` on the scene: `{kind: MOVEMENT|CASTING|ATTACK_ON_ALLY|GRAB_FAILED, actorId,
    payload, checkpoints, cursor, answers}`.
  - Messages: `/movement-intent` and `/cast-intent` open it. The server broadcasts `ReactionWindowOpenedEvent`
    (`/topic/.../reaction-windows`) for each checkpoint. Reactors answer on `/reaction-window-answer`
    (PASS or REACT + option). A GM "Pular reações" control force-closes a window.
  - When every reactor has answered, the server broadcasts `MovementSegmentEvent(moverId, positions up to the
    next checkpoint)`, or `ReactionWindowClosedEvent` for casting.
  - Reconnecting clients replay the open window. The server still trusts client geometry, as today.
- **Client:**
  - The reactor sees the existing `ReactionModal` with the generic options.
  - A Defender o Perímetro / Impedir Conjuração REACT runs the existing attack path (`beginTargeting` →
    `/hits`/`/damage`, or the roll request for monsters), with the mover rendered at the checkpoint hex.
  - The mover's client decides whether to continue (the interruption ruling) and sends the next segment.
  - `MovementBudget` keeps the unspent UD.

## Phases 1–7 (each = one core 4th-number bump, then API, then client; core → api → client order)

### Phase 1 — Queda and Asfixia (core 0.1.6.14) — *built (core + API `/hazards` + client GM menu)*
- `FallService`:
  - `fallResistance(sheet)` = max(2, 2 × Categoria).
  - `fall(sheet, heightUd)` → `FallResult`: half the excess as Primordial damage, +1d6 and Feridas Dolorosas
    until the next Descanso per 10UD of excess, death past 40UD.
  - It also applies Caído.
- `AsphyxiaService`:
  - `tick(sheet)` on a counter stored on the sheet (`getAsphyxiaState`).
  - Damage starts after Vigor Rodadas: 1d6/2d6/3d6 (capped), Feridas Dolorosas at the 4th instance, death at
    the 6th.
  - `breathe(sheet)` resets it.
  - The counter ticks from `AbstractCombatantSheet#startTurn` when submerged (elevation < 0 on WATER, or a GM
    "sem ar" flag for gas).
- Tests reproduce both worked examples in the doc: Gigante/Goblin/Elfo, and the Vigor 4 Anão and Vigor 2
  Pequenino.
- API/client:
  - A GM "Queda" scene-menu action (height prompt) for ad-hoc falls.
  - "Sem ar" toggle.
  - Passar Tempo "+1 Rodada" ticks submerged characters out of combat.

### Phase 2 — Resistance durations (core 0.1.6.15) — *built (core only; nothing to wire)*
Unchanged from the existing plan:
- −1 Unidade on a matching continuous effect's Duração.
- Not cumulative between instances.
- RD applies to non-Primordial Físico damaging effects, RM to non-Primordial Mágico, RE to Elemental, RA to all.
- Hook point: where `TemporaryEffect` is applied to a sheet.

### Phase 3 — Board model (core 0.1.6.16 + API + client) — *built*
API note: no Mongo migration was needed — `SceneDocument.terrainCells` is additive beside the legacy `difficultTerrain` list, and painting a cell clears its legacy mark.
- Everything in Architecture A.
- Flight is persisted and broadcast: `CombatantStateMessage` gets `flying` + `elevation`.
- **Falls from losing support:**
  - When a flight source lapses, or an airborne/climbing combatant becomes Caído/Desacordado/Imobilizado, the
    client calls `FallService.fall(elevation − groundLevel)` and drops the token.
  - Over WATER, the token lands at elevation 0 and starts swimming instead.

### Phase 4 — Reaction framework (core 0.1.6.17 + API + client) — *built*
Owning clients advertise their combatants' melee reach on `/state` (`meleeReachUd`, core `MovementReactionService#getMeleeReachUd`): a client's stand-in for a remote combatant carries no weapon, so this is what lets the mover's client see who threatens it.
Everything in Architecture B, plus each Reação:

| Reação | Core | Client |
|---|---|---|
| Defender o Perímetro | `RouteReactionService` checkpoints; option on `ENEMY_MOVES_IN_REACH` and `GRAB_FAILED` | Window prompt → Ataque CaC |
| Impedir Conjuração | Option on `ENEMY_CASTS_IN_REACH`; `SpellCastingService` takes `disruptionDamage` → −2 per 5 on the casting roll | Window opened by `/cast-intent` before the roll |
| Sacrifício | Option on `ALLY_ADJACENT_ATTACKED`; once per combat (sheet counter reset by `Scene#startCombat`); `AttackResolution` flag "intercepted": auto-hit, forced Crítico Menor, Correntes forced | Reuses `interceptedForCharacterSheetId` (Guarda Vidas path) |
| Conjuração Reativa | `REACAO` Magias listed per trigger | Cast from `ReactionModal` through `SpellCastModal` |
| Ativação Reativa | Título (existing) + monstrous reaction abilities | Existing modal |
| Preparação | `PreparationService`: `prepare(sheet, PreparedAction)` (3PA, inner cost ≤2PA via existing cost services), extra Reação counted separately in `ReactionsService`, expires at own `startTurn` | "Preparar" button → action picker; off-Turn "Executar preparação" button runs the stored action |

### Phase 5 — Movements (core 0.1.6.18 + API + client) — *built* (client: the "Ações de Combate" menu)
A new `MovementManoeuvreService` returns a `MovementPlan(budgetUd, rollRequired, gd, reachClamp, provokes,
restrictions)` per `Manoeuvre`. Every PA-bought movement calls `consumeMovementThisRound` (the Reposicionar
exclusion ruling).

| Ação | PA | Core rule | In combat | Out of combat |
|---|---|---|---|---|
| Movimento Padrão | 1 | MB UD; vertical only with flight/CLIMB/underwater | Route checkpoints → windows | Free move, no PA, no reactions (today's behaviour) |
| Correr | 2 | Atletismo TRI_ATLETA, GD Médio; 2×MB + 2UD per GD beaten; no Terreno Difícil on the route; reach clamp = adjacent; Falha Crítica → moves a random 1..MB UD along the route, then Caído | Roll first, then route | Not offered (movement is free) |
| Escalar | 2 | CLIMBABLE cells; vertical up to MB/2; cell GD (default Médio); CLIMB mode skips the roll; fail ≤5 = no move, >5 = fall (Phase 1) + Caído; Estado Desprevenido while off the ground on a wall with a hostile within Longa, unless CLIMB mode | Roll → vertical route | Same roll (the client sends a self-CHECK at the cell GD); no PA |
| Nadar | 2 | WATER cells; MB/2; cell GD; SWIM mode skips the roll; fail >5 → submerge (elevation −1, asphyxia counter); Desprevenido rule as Escalar, with SWIM exempt | Same | Same roll; asphyxia via the GM tick |
| Saltar | 2 | MB/2 + 2UD per GD beaten; airborne route ignores Terreno Difícil cost and may cross WATER/gaps/occupied hexes, but must land on a standable free hex; provokes; damage ≥ half PV in a window → lands at that hex, Caído (+ fall/swim by the cell); Falha Crítica → random UD ≤ max, then Caído | Roll → route | Roll; no PA |
| Recuo Defensivo | 2 | Once per Rodada (new sheet counter); MB/2; the distance rule above; no checkpoints | Route validated step by step | Not offered |
| Reposicionar | Livre | max(1, ⌊Categoria/2⌋) UD, replacing the 1UD ruling; no reactions; exclusive with PA movement | Existing button | Free move |
| Voar | per source | Needs FLIGHT mode or an active Voo effect; take-off (elevation leaving ground level) opens a Defender o Perímetro window at the take-off hex; flight moves use the FLIGHT Movimento Base index; duration per source; lapse → fall | "Levantar voo"/"Pousar" toggles + PgUp/PgDn while moving | Same toggles, no reactions |
| Levantar-se | 1 | Built (`StandUpService`); now opens a real window instead of logging | Window | Free |

### Phase 6 — Attack manoeuvres (core 0.1.6.19 + API + client) — *core built; client wires Postura, Rendido, Imobilizar, Encontrão (player vs foe)*

| Manobra | Work |
|---|---|
| Ataque Básico | Built. Gets 3D reach only. |
| Investida | Add minimum distance = standard MB for the Categoria (`MovementService` default per size); straight line via `HexGrid.line` + `isLineClear` (no obstacles, occupants or difficult cells), replacing Dijkstra `chargeLandings`; the rest is built. |
| Encontrão | New `ShoveChargeService` mirroring `ChargeService` (3PA, same distance/line, `Manoeuvre.ENCONTRAO`); Ataque CaC with no damage; hit → push 1UD (crit 2UD) via a new `ForcedMovementService.push(target, direction, ud, map)` (stops at obstacle/occupant, ledge → Queda, water → swim); carries `LancarAoChao` at the Corrente margin; miss → −2 Defesas "por 1 Rodada" (`countsDownAtTurnStart`). Also wire `Rugido#getPushedBackUd` through `ForcedMovementService`. |
| Agarrar | Add the captor's Categoria as a bonus/penalty on the grab roll; a failed grab opens a `GRAB_FAILED` window for the target. Verify (add if missing) the Leve/Média/Natural weapon restriction while holding, and the Perícia Desvantagem on both sides. |
| Atacar Objetos / Desarmar | New `ObjectAttackService` (2PA): attack roll with GD +1 nível against the target's Defesa; pick an equipped item; damage halved (Meio-Dano, not stacking with another halving), then `Item#applyDamage`; carries `Desarmar` at the Corrente margin; provokes Defender o Perímetro from the target (`GRAB_FAILED`-style window, trigger renamed `PROVOKING_MANOEUVRE`). Unattended scenery is adjudicated by the GM (check at GD +1, GM-entered Dureza). |
| Imobilizar | New `ImmobilizeService` (3PA, adjacent): target must be Rendido or Inconsciente (PV < 0); applies `Immobilization.byCaptor` (Resistida escape already built). Works out of combat with no PA. |
| Postura Defensiva | New `DefensiveStanceService` (2PA, `Manoeuvre.POSTURA_DEFENSIVA`). Applies a `TemporaryBonus` on Defesas with `countsDownAtTurnStart`. The value is resolved live from `HandBudget` / the equipped Escudo or wings (+3) versus none (+2), so it re-reads if the shield is lost. Re-taking it refreshes the bonus instead of stacking. Client: a button in the Maneuvers panel and a token badge. Not offered outside combat. |
| Rendido | New `ConditionType.RENDIDO` (Estado Desprevenido); declared by the controller/GM as an Ação Livre; cleared by `recordAction` of any non-escape action. |

### Phase 7 — Perícia, Conjuração and Habilidades (core 0.1.6.20 + API + client) — *built (client: "Conjurar defensivamente" toggle)*
- **Conjurar Defensivamente:**
  - `SpellCastRequest.defensive`: +1PA in `resolveActivationTime`, GD +1 nível, Desvantagem on the Magia's
    Dano/Cura rolls, and no `/cast-intent` window.
  - Client: a checkbox in `SpellCastModal`.
- **Uso de Perícia (2PA):** the "no at-will Perícia rolls, GM check only" ruling stays. In combat, a GM CHECK
  addressed to the active combatant charges `getSkillRollCost` through `MovementBudget`. Competency actions keep
  their own prices.
- **Ativar Habilidades:** built. Only check that reaction-timed ones show up in Phase 4.
- **Leftovers from the 20261012 Correntes:** an in-scene draw-weapon / ready-shield action priced by
  `getEquipCost`, outside the inventory modal.


## Still open after Phases 1–7 (2026-10-14)

- **Atacar Objetos / Desarmar in the client.** Core's `ObjectAttackService` is built, but a client sees no remote
  combatant's equipment (a stand-in carries none, and a foe's stat block lists none), so the strike needs an owner-side
  item-damage relay like `/hits`.
- **A foe's Encontrão, and Atacar Objetos against a player**, need a roll-request kind of their own. Today the Narrador
  resolves them with an ATTACK request plus the hazard and condition tools.
- **Investida in the client** still lands through the Dijkstra `chargeLandings`. It does not yet apply
  `ChargeService#refusalForLine` (straight line, minimum distance), and its run opens no Reação windows for hostiles
  other than its target.
- **Saltar's "danos iguais ou superiores à metade dos PV interrompe o salto"** is not checked when a jump pauses at a
  window. `canStillMove` checks only the Condições.
- **Uso de Perícia charging 2PA** on a GM check answered in the active combatant's own Turn is not wired. The ruling
  "no at-will Perícia rolls" stands.
