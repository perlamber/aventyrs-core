# Ego spending — implementation plan

Source: `docs/rules/ego.txt` (imported 2026-09-29 from *Módulo Básico V18 ALPHA — 2.5: Ego* and its
Autocontrole / Iniciativa / Recursos / Sorte subpages). Target: core first, then aventyrs-api, then the
client. **Status: Phases 0–6 done (core 0.0.76–0.0.78; aventyrs-api and the client on 0.0.77); Phase 7 (Iniciativa) next.**

## Table rulings (2026-09-29)

- **Egos can be spent at any time.** No timing, turn-phase or action-economy gate on a spend.
- **No Ego is ever above 5.** Anything past 5 (Talentos, Antecedentes, Títulos, GM) arrives as **extra
  temporary points**, and the Ego counts as 5 everywhere else. Recursos 7 = Recursos 5 + 2 extras;
  spending one pays table row 5 (22PE per temporary point).
- **A received temporary point is worth the Ego's base (≤ 5), and is consumed — not a ceiling widening.**
  Receiving first refills spent temporaries up to the base; any remainder becomes an extra above the
  base, gone once spent and never recovered. Example: Ego 3 full (3 perm / 3 temp) receives 1 → 4 temp;
  spend 2 → 2; receive 1 → back to 3 / 3.
- **End of session:** the GM opens a modal, picks one Ego, and every character gets 1 temporary point in
  it. Session points **only refill** up to the base — they never become extras. Recursos is not offered.
  (Reading, unconfirmed: Motivação de Moses / Dileto de Tykhé still add +1 in their own Ego when that Ego
  is the one picked, also capped at base.)
- **The GM can grant temporary or permanent Ego points at any time** in play. Temporary = a received
  point (refill, then extras). Permanent = +1 to the Ego total (past 5 → extras).
- **Starting PE is granted by core at character creation** (table's *PE Iniciais*). The client wizard
  gets a store step whose max rarity is the table's *Raridade Inicial* for the character's Recursos.
- **Utilidades e Serviços are interpretation only** — kept as reference data, never enforced.

## The rules in one screen

Four Egos, 1–5. Two ways to spend each:

- **Temporary** — the lesser effect; recovers (session modal / GM).
- **Permanent** — the greater effect; never recovers, lowers the Ego.
- A temporary point can't trigger a permanent effect. At 0, roll 1d6 on that Ego's penalty table.
- PdN: only intelligent Exemplares spend; a permanent spend by a PdN gives every PJ one temporary point
  in that Ego at the end of the Cena.

| Ego | Temporary | Permanent |
| --- | --- | --- |
| Autocontrole | Halve a malefício's Duração · avoid a Corrente / ignore a Crítico Menor suffered · RA 1 Rodada + RD/RM/RE · Descanso Longo recovery | Remove an effect + immune for the Cena · avoid Corrente/Efeito Crítico + immune · zero all damage this Rodada · Descanso Total (PM/PD) + all PV |
| Iniciativa | Lower own Iniciativa (Cena/Rodada) · reroll Iniciativa with Vantagem · +1PA or +1 Reação this Rodada · Vantagem on ≤ 2 Perícia rolls (same Cena) | Set own Iniciativa · change a PdN's for 2 Rodadas · +2PA +1 Reação for 2 Rodadas · −GD on ≤ 2 Perícia rolls (same Cena) |
| Sorte | Reroll a Perícia with Vantagem · ask the GM for −GD vs a PdN · subtle scene change | Choose success (a Crítico Menor) · trigger Correntes + Críticos Maiores on a success · drastic scene change |
| Recursos | PE by the table, or a narrative purchase (bribe, information, mercenaries) | PE by the table (larger), or a narrative purchase |

**Recursos table** (looked up by **permanent points remaining**, capped at 5 — this is exactly the
book's "total": Recursos 5 with 3 available ≙ `permanentRemaining=5, temporaryRemaining=3`):

| Recursos | Classe Social | PE Iniciais | PE / temp | PE / perm | Raridade Inicial | Utilidades | Raridade Máx | Serviços/dia |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | Falência | 0 | 3 | 0 | Nenhum | 1 | Comum | 0 |
| 1 | Pobreza | 15 | 7 | 22 | Comum | 3 | Comum | 2 |
| 2 | Classe Média | 21 | 10 | 31 | Comum | 6 | Incomum | 3 |
| 3 | Baixa Nobreza | 28 | 14 | 42 | Incomum | 10 | Raro | 5 |
| 4 | Nobreza | 36 | 18 | 54 | Incomum | 15 | Épico | 8 |
| 5 | Alta Nobreza | 45 | 22 | 67 | Raro | 21 | Mítico | 11 |

## What core already has

`EgoPointPool` (two pools per domain, spent-counters), `CombatantSheet#spendEgoPoints`,
`EgoPointsService#useEgoPointsForEffect` (spend + the Vantagem's spend hooks — `DETERMINACAO_HEROICA`,
`AS_NA_MANGA`), `applySessionRecovery(Map<CombatantSheet, EgoDomain>)` (+ Moses/Dileto),
`TemporaryEgoPenalty`, `DelayedEgoGrant`, the depletion hook, `MonsterSheet`'s per-Cena Ego-effect cap,
all 12 Vantagens declared, `BARGANHISTA`'s purchase discount, the PE wallet
(`AbstractCombatantSheet#equipmentPoints`) and `ItemPurchaseService` / `ItemStore(maxRarity)`.
See the `ego-point-pools` skill.

**What contradicts the rulings:** `grantTemporaryEgoPoints` / `grantTemporaryEgoPointBonus` widen the
temporary *ceiling* per source, persistently — so a spent granted point comes back on the next recovery.
Under the ruling it is a consumable extra.

## Phases

0. **Pool rework (all four Egos).** *Done in 0.0.76* — see `0.076.CHANGELOG.md`. Narrador grants landed as
   `grantTemporaryByNarrator(sheet, …)` + `grantPermanentByNarrator(character, …)` (a sheet's Character is final).
   - Cap `Character#getEffectiveEgoTotal` at 5 (one funnel: pool max, Iniciativa order, Moral Herdada's
     Fama, Talento requirements).
   - `EgoPointPool` gains a consumable **extra-points** balance (per source where "não cumulativo"
     matters). Spends take extras first. `receiveTemporaryEgoPoints` = refill up to base, remainder →
     extras. `recoverTemporaryEgoPoints` stays refill-only (session recovery, `PendingEgoRecovery`).
   - The > 5 overflow is received as extras when the Ego crosses 5.
   - Migrate callers: `DelayedEgoGrant` delivery, the Ego-point loans in `AbstractCombatantSheet`,
     `AbstractSkillInteraction`'s `DESTINO_FAVORAVEL` grant (≤ 1 held extra from that source), `Orc`
     docs, `GiganteEnfurecido`, the depletion grants.
   - `EgoPointsService#grantByNarrator(sheet, domain, type, amount)` — temporary = receive; permanent =
     `CharacterEgos#withVariableBonus` +amount.
   - Tests: rewrite `EgoPointFeatureTest` / `EstabilidadeEmocionalFeatureTest` around the Ego 3 example;
     pin that session recovery never creates extras. Update the `ego-point-pools` skill and CLAUDE.md.
1. **Recursos table.** *Done in 0.0.77* — `ego.SocialClass`. Enum rows 0–5 with every column above; lookup by permanent remaining (≤ 5).
   Utilidades/Serviços columns are display data only.
2. **Starting PE at creation.** *Done in 0.0.77* — `CharacterCreationService#grantStartingEquipmentPoints`/`#getStartingStore`. An explicit, one-time creation call granting *PE Iniciais* to the new
   sheet (not inside `CharacterSheet.of`, which also rebuilds persisted sheets). An `ItemStore` factory
   for the wizard at *Raridade Inicial* (none at Recursos 0). Update the `character.services`
   package-info creation list.
3. **Spend Recursos for PE.** *Done in 0.0.77* — with Saber Investir's +2 (`Feat#resolveResourcesPointValueBonus`). `EgoPointsService#spendResourcesForEquipmentPoints(sheet, type, amount)`
   — priced point by point (a permanent spend lowers the rows for the points after it), through
   `useEgoPointsForEffect`, into the wallet. No timing gate.
4. **Recursos recovery and narrative spends.** *Done in 0.0.77* — session recovery refuses Recursos; wages/loot/rewards are `grantTemporaryByNarrator` (no reason field — the reason is the UI's); a narrative spend is `useEgoPointsForEffect`; extreme poverty is `SocialClass#isExtremePoverty`. `applySessionRecovery` refuses Recursos. Wages / loot /
   rewards are a GM temporary grant with a reason (may create extras, like any received point). A
   narrative spend (bribe, etc.) with no mechanical effect. Derived `isInExtremePoverty()` at 0.
5. **API + client.** *Done* — see the client's `docs/wiring-egos.md`: the API stores an `egoLedger` (permanent spent, extras, overflow received) beside `temporaryEgoPoints`; the session modal takes one Ego for the table, never Recursos; the hub has an Egos tab (Recursos → PE, the GM's grants); creation opens the shop at the Raridade Inicial with the starting PE. Deviation: the creation shop runs right after "Criar" rather than as a wizard step, since the client's shop works on a saved sheet. Endpoints: spend Recursos, GM grant (temp/perm), end-of-session recovery, starting
   store. Client: wizard store step, sheet spend button, GM grant control, end-of-session Ego modal
   (maps every participant to the picked Ego, one `applySessionRecovery` call).
6. **Sorte.** *Done in 0.0.78* — `ego.SorteEffect` on the roll, paid by `EgoPointsService#applySorte`/`#rerollWithSorte`; applied on Perícia rolls, attacks and defences. Readings in `0.078.CHANGELOG.md`. Client wiring (a Sorte button on a roll) not done yet. Reroll with Vantagem, −GD vs PdN, forced success (Crítico Menor), trigger Correntes +
   Críticos Maiores; scene changes narrative.
7. **Iniciativa.** Lower / set / reroll own Iniciativa, change a PdN's, +PA / Reações, Vantagem or −GD on
   2 rolls in the Cena (`InitiativeService`, `ActionPointsService`, `ReactionsService`).
8. **Autocontrole.** Halve / remove a malefício with Cena immunity, avoid a Corrente / Efeito Crítico,
   rest-style recoveries. Blocked pieces: RA has no grant path for "RA por 1 Rodada" (gap catalog);
   "zero all damage this Rodada" needs a per-Rodada damage shield.
9. **Ego at zero.** The four 1d6 penalty tables, rolled when a spend or drain empties the pool (reuse
   the depletion hook).
10. **PdN rules.** Exemplar-only spending; a PdN permanent spend queues one temporary point for every PJ
    at the end of the Cena.

## Cross-links

- Defeitos e Qualidades: `Quality#DESTINADO_A_FORTUNA` Menor ("Recursos points worth +2PE") reads the
  Phase 3 spend — add it as a hook on that spend when Phase 3 lands; its Maior (+1 permanent Recursos)
  goes through the Phase 0 cap.
- Antecedentes: an Antecedente's Ego +1 is a full point (counts toward the Vantagem threshold and the cap).
