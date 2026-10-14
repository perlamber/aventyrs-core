# Undead invocations (REANIMAR tree) + Escolhido de Macária — implementation plan

**Status: Phases 0–6 built (core 0.1.6.22–0.1.6.27, API, client on 0.1.6.27); not yet played live.** Core 0.1.6.27 was added during Phase 6: the anatomy now lives on every sheet (`MonsterSheet#transferAnatomyTo`) because the client acts for summons and foes through `CharacterSheet`s. Open: Invocar Abantesma cast alone; Servo Cadavérico's raise count is local only; the client's Foco check counts only invocations this client runs. Started 2026-10-14.

## Context

The user imported **v19 – Manual de Criação de Monstros – 20261013** (done: `aventyrs-core/docs/rules/criacao-de-monstros.txt`,
same conventions as before). What changed vs the 20260923 import:

- **New Modelo — Escolhido de Macária** (undead), fully written: Modelo-wide *Anatomia de Morto-Vivo* + 8 Habilidades:
  Renascido [Presa], **Carrasco Morimbundo** [Presa] (zombies/skeletons/ghouls), Senhor dos Mortos [Deviante]
  (Procissão dos Desencarnados summons a Carrasco), Espectral [Deviante], Filactéria [Predador], Anunciador da
  Escuridão [Predador], Cuspidor de Pragas [Apex] (2 Carrascos every odd Rodada, Praga Zumbi), Mestre das Marionetes.
- Text fixes: "Monstros adicionam"; GP+3 grants "uma nova **Habilidade Monstruosa**" (code already reads it that way —
  `MonsterRules.java:89` comment only); Fúria Elemental / Aura Elemental / Tormenta Cataclísmica now say **Dano Físico
  Elemental** (= `DamageType.ELEMENTAL` in the 0.1.6.10 taxonomy); Terror da Forma Verdadeira progresses to
  **Amedrontado** (not a defined Condição — ⚠️ read as Assustado, the middle fear rung).
- **Draft notes, not imported into code:** shorthand outlines for Caminhante Umbral, Cria de Neméia, Diminuto, Dracônico,
  Fera Marítima, Força Natural, Grandalhão, Herculano, Horda, Intelecto Superior, Praga, Presença Real, Sauróide,
  Selvagem, Vida Artificial, Extra-planar, and empty headers (Abissal, Celestial, Arauto da Casualidade, Caminhante
  Onírico, Coração Vazio). They are design notes ("> Solidez: Meio-Dano Físico"), not rules text.

With undead now defined, the REANIMAR tree's summons can be wired end to end (core → API → client), on the ALIADOS DA
NATUREZA summon machinery (core 0.0.92–0.0.98).

## Rulings (user, 2026-10-14)

1. Spell-raised undead use the **stat block printed in magias.txt**, but with the manual's base **Anatomia de
   Morto-Vivo** (no Habilidade Monstruosa).
2. **A corpse is required** for Reanimar / Criar Carniçal — a dead body in the scene, a fallen undead's included. A body
   may be raised **4 times**; after the 4th it can never be raised again.
3. **"Número máximo de invocações" = Foco** (Carisma stays the Subordinado cap).
4. Scope: Reanimar + Servo Cadavérico, Criar Carniçal + Açougueiro, Festim dos Mortos (incl. Abantesma), Escolhido de
   Macária Modelo with its monster-side summons.

Second round (same day):

5. Carrasco Morimbundo summoned by a monster (Procissão, Cuspidor, Praga Zumbi) uses a **generated stock blueprint**
   per category.
6. Raising **replaces the corpse token** (undead takes its square and Categoria); **dead PCs can be raised** (sheet kept,
   token leaves). A fallen undead becomes a corpse again, raise count carried.
7. The Foco cap is enforced by **every summon spell** (Aliados included): refused at the cap; only Festim goes over,
   its excess rising uncontrolled.
8. Readings accepted: Mestre das Marionetes = **Apex**; **every** Macária Habilidade grants the Anatomia once (shared
   trait, like Anatomia Vegetal); Amedrontado = **Assustado**; Abantesma gets the Anatomia too.

## Gaps found (exploration)

- Core: a summon at ≤0 PV is dismissed and leaves nothing (`Scene#settleSummons` L1407); no corpse / raise count;
  `InvocationPlan` typed to `NatureSummon`; blueprint monsters (`MonsterSheet.of(…, MonsterBlueprint)` L218) never get
  damage immunities or healing inversion; no Modelo hook for undead; no monster Efeito Ativo can spawn; Profano-heals
  exists only on `Race` (`Race#resolveInvertedSpellHealing`, read in `DamageInteraction:338`), and every monster's Raça
  is the shared `Monstruoso`.
- API: fallen summons' sheets are deleted (`SceneService#removeFromDocument` L223); `SummonEntry.toTemplate` only knows
  `NatureSummon`/`FamiliarTemplate`; no RNG; a summon is always the caster's player + group; no per-token metadata.
- Client: `controlledSummons` is `Map<String, NatureSummon>`; `broadcastStatusOf` dismisses a fallen summon;
  `SceneGridSheetFactory.aliadosBase` only detects Aliados; `resolveMonsterParticipants` skips every summon.

## Phases

Core versions continue on the 4th number from **0.1.6.21** (one bump per phase); API and client move to each core
version they consume. Each phase ends with a changelog `docs/changelog/0.1.6.N.CHANGELOG.md`, the plan doc
`docs/plans/undead-invocation-plan.md` (written in Phase 0 from this file) updated, and memory updated.

### Phase 0 — import housekeeping (core 0.1.6.22)
- Description strings that quote the changed text: `AlmaElementalAbility` (Fúria/Aura Elemental, Tormenta → "Físico
  Elemental"; any element typing in code → `DamageType.ELEMENTAL`), `AspectoHumanoideAbility` Terror (→ "Amedrontado",
  applied as `ASSUSTADO`), `MonsterRules.java:89` comment.
- Create `docs/plans/undead-invocation-plan.md`; save rulings 5–8 to the `undead-invocation-rulings` memory.

### Phase 1 — Anatomia de Morto-Vivo (core 0.1.6.23)
New `monster.UndeadAnatomy` (constants + helpers), applied wherever a sheet `isUndead()` with the anatomy:
- Efeito Crítico immunities: Atordoante, Dilacerar, Empalar, Ferida Profunda, Oferenda Maldita, Sangramento.
- Resist. Correntes +2 in `EffectChainService#getRequiredMargin` (sheet-side term beside the Feat scan).
- Immune to sangramento/doença/maldição/veneno: `isImmuneToCondition` for `DOENTE`/`ENVENENADO`/`AMALDICOADO` and the
  bleeding effects; `DiseaseService` candidates skip it.
- No asfixia: `AsphyxiaService#stopBreathing` refuses / Asfixia damage immune.
- Profano effects heal **1d6+½Vigor** and deal no damage: generalize the `DamageInteraction:338` read to ask the sheet
  (`CombatantSheet#resolveInvertedSpellHealing`, Race path unchanged) — Profano sanctity hit → heal.
- Sagrado / curative effects: no PV, no revival, deal **2d6+½Vigor Sagrado** instead (extend `invertsHealing` to carry
  the formula, dice from the existing `DiceRoller`); undead healers' Habilidades Monstruosas exempt.
- Replace `Zumbi.ANATOMIA_DE_MORTO_VIVO_MENOR` + its Profano/Natural immunity with this anatomy (ruling 1).
- Close the blueprint gap: `MonsterSheet.of(…, MonsterBlueprint)` copies anatomy from the grants.

### Phase 2 — corpses and raising (core 0.1.6.24)
- `CombatantSheet#getTimesRaised()` / `recordRaised()`; `RaiseDeadService#requireRaisable(sheet, status)`: status
  `DEAD`, or a fallen undead summon; `timesRaised < MAX_RAISES = 4`; not `isBeyondRevival()`.
- `Scene#settleSummons`: an undead **raised from a corpse** (Reanimar's Zumbi, Criar Carniçal) at ≤0 PV **leaves the
  corpse behind** on its square: summon record dropped, participant kept with status `DEAD`, its `timesRaised` and the
  body's Categoria, so it can be raised again until the 4th raise. `SceneSummon#raisedFromCorpse` marks them.
  Undead spawned **without** a corpse (Festim's sarcophagus, Procissão, Cuspidor, Abantesma) leave as today.
- `Scene#raise(caster, corpse, summonSheet, …)`: removes the corpse participant, places the summon after its caster,
  carries `timesRaised + 1` and the corpse's Categoria.
- Summon cap: `SummonLimit#of(caster)` = effective Foco; `Scene#countControlledSummons(caster)`;
  `NatureInvocationService`'s invoke methods and the new service refuse `SUMMON_LIMIT_REACHED`.

### Phase 3 — REANIMAR tree (core 0.1.6.25)
- Widen `InvocationPlan` to `SummonedMonsterTemplate`; new `magic.invocation.UndeadInvocationService` (+Impl),
  plan/place pairs like `NatureInvocationServiceImpl#place`.
- **Reanimar**: `Zumbi` raised from a corpse (Categoria clamped −2…+2), Concentração + 2.
  **Servo Cadavérico**: corpse becomes a Cavaleiro/Torre `Subordinate` tied to the body (new sustained + body form,
  since `Subordinate.sustained` drops `creatureId`); Escudeiro Cadavérico Corrente = Prodigioso for its first 2 Rodadas.
  ⚠️ reading: the corpse token stays as the servant, raise count +1.
- **Criar Carniçal**: new `Carnical` template (stat block from magias.txt, tiers 7/10, Categoria −1…+2), Concentração + 2.
  **Açougueiro**: +5PM reported (`extraManaCost`), no Duração, +1PA, Vantagem attack+damage, Roubo de Vida 1, 1d6 PV on
  killing a living character (via `DefeatBlessingService`), one per caster (exclusivity group); the +5PM lock until
  destroyed + Descanso Longo reported.
- **Invocar Abantesma** (needed by Festim): new `Abantesma` template — Força/Vigor 0 (⚠️ "Nula"), flight, Defesas +18,
  4PA, attacks vs DM, Possessão Furiosa Corrente (`POSSESSAO` 2 Rodadas); no PV: `MonsterTemplate#damageShortensDuration`
  → any landed damage takes 1 Rodada off its `SceneSummon` instead.
- **Festim dos Mortos**: spawner whose supplier rolls 1d6 (1–3 Zumbi, 4–5 Carniçal, 6 Abantesma), Duração 3; spawns
  can't be extended, act from the next Rodada, leave with the spawner. Over the Foco cap or **Horda Faminta** (+1 Zumbi
  each Rodada, none controlled) → `SceneSummon#uncontrolled`: hostile, attacks the nearest living. Grad 10: −2 níveis
  for allied undead within Curta of the sarcophagus (core computes, caller gives the distance).

### Phase 4 — Escolhido de Macária Modelo (core 0.1.6.26)
- `monster/model/escolhidodemacaria/EscolhidoDeMacariaAbility` (8 constants, 2/2/2/2; Mestre das Marionetes APEX), all
  sharing `sharedTraitKey` ANATOMIA_DE_MORTO_VIVO (Phase 1 anatomy); add `MonsterModel.ESCOLHIDO_DE_MACARIA`, new
  `MonstrousAbility#grantsUndeadAnatomy`, `MonsterModelTest` count → 7, per-Modelo test.
- Wire what existing hooks reach (Renascido For/Ins +2 + RDS; Carrasco Vigor +2, Autocontrole 0, Roubo de Vida 1→3→4,
  Instinto Predatório PA/Iniciativa; Espectral Incorpóreo immunities, flight; Filactéria/Anunciador/Mestre partial with
  notes per the honest-status convention).
- **Monster summons**: `MonstrousActiveAbility#summons` (a reported `SummonRequest`: category, count, Concentração + N);
  `CarrascoMorimbundo.stock(MonsterCategory)` — a generated blueprint at the category's floor GP (0/12/26), Escolhido de
  Macária + Carrasco Morimbundo, the spell Zumbi's attribute profile within the category cap, Ataque Corpo-a-Corpo
  trained, Garras Afiadas (⚠️ numbers are my authoring, listed in the plan doc). Procissão (Presa → Deviante@Predador →
  2 Deviante or 1 Predador@Apex, cap Carisma), Cuspidor passive spawner (2 Deviante on odd Rodadas in combat, gone with
  combat/caster), Praga Zumbi (Doente, −1 Vigor/Rodada; the dead rise as Deviante Carrascos after 2 Rodadas —
  reported, caller places).

### Phase 5 — API (on core 0.1.6.26)
- `SummonEntry` + `toTemplate`: kinds `ZUMBI`/`CARNICAL`/`ABANTESMA`/`CARRASCO_<CAT>`, body Categoria, Açougueiro flag.
- `timesRaised` on `MonsterSheetDocument` and `CharacterSheetDocument` (null = 0).
- Raise: `SummonCreateRequest.corpseParticipantId` → remove corpse participant (PC sheet kept), place at its square.
- Corpse-raised summon (`SceneSummonEntry.raisedFromCorpse`) at ≤0 PV: keep sheet + participant as the corpse, drop
  the `SceneSummonEntry` (no deletion); others are deleted as today.
- `SceneSummonEntry.uncontrolled` + hostile group UUID; GM `playerId` from `PlayerRepository` role GM.
- Spawner generalized (`kind` FESTIM / FESTIM_HORDA / CUSPIDOR): server `DiceRoller` (SecureRandom), face logged in the
  roster broadcast; Cuspidor only on odd combat Rodadas, removed at `endCombat`.
- `POST /scenes/{id}/summons/{summonId}/shorten` (Abantesma).

### Phase 6 — client (on core 0.1.6.26)
- Detect Reanimar invocations beside `aliadosBase`; **corpse pick mode** (modelled on loot mode,
  `onLootTarget`): adjacent, `requireRaisable`; Servo Cadavérico grade/benefit modal; Açougueiro alternate; Festim
  area centre adjacent.
- `controlledSummons` → `Map<String, SummonedMonsterTemplate>`; restore new kinds; a fallen corpse-raised summon stays
  on the board as a corpse (greyed, fallen cross, raisable) instead of `dismissSummon`.
- Uncontrolled undead listed as foes (GM drives them); `nearestCreatures` filtered to living.
- Abantesma damage → shorten call; monster Procissão/Cuspidor activations place summons; token art fallbacks
  (`tokens/summons/<kind>.png`); i18n keys.

## Verification
- Core: `./gradlew test` each phase; new tests per service/template (anatomy, raise limit at 4, settle → corpse,
  cap refusal, Festim roll table with `DiceRoller.fixed`, Abantesma shortening, Modelo test at 7), then
  `publishToMavenLocal`.
- API: `./gradlew test` incl. SceneService summon tests (raise replaces corpse, fallen undead kept, uncontrolled
  ownership, Festim spawns with a fixed roller).
- Client: unit tests for corpse targeting and summon restore; then `run` skill — cast Reanimar on a dead foe, kill the
  Zumbi, raise it again until the 4th refusal; Festim over the Foco cap shows hostile undead; GM builds an Escolhido de
  Macária monster and uses Procissão.
