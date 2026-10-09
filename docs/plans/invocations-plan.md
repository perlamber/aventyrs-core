# Invocations and Subordinados — implementation plan

**Status: Phases 1–5 built** — core 0.0.92–0.0.98, the API and the client (see the 0.092–0.098 changelogs and
`spell-casting-audit.md`). Next on this machinery: the other trees' summons — Reanimar, Armada Decapitada, the Título
summons — and the Alma Imperador Título. Started 2026-10-01 with the ALIADOS DA NATUREZA tree.

**How it landed, where it differs from the phases below:**
- Summons live on the server: a blueprint-less `MonsterSheetDocument` with a `summon` descriptor, plus `SceneSummonEntry`
  rows that `SceneService` runs.
- The caster's client controls each summon (`syncControlledSummons`).
- Subordinados ride the character's status frame (`SubordinateDto`) and are restored when it enters a Cena; the hub
  lists them.
- Agnação Ancestral Superior's Peão (until the next Descanso) and the Esquecida's sombra conselheira (Concentração +1)
  are real since core 0.0.98. The same machinery later serves
Reanimar, Armada Decapitada, the Título summons (Convocar Daemons, the Elementais) and the Alma Imperador Título (a
second character), which is not written yet.

**GM-granted Subordinados (core 0.1.5.6 + API + client, 2026-10-09).**
- **Rulings (2026-10-08):**
  - The GM grants from the Cena's GM menu ("Subordinados") to characters and foes.
  - No Carisma limit or one-per-grade rule; the modal only warns.
  - Permanent until the GM dismisses them from the same modal.
- **Wire:**
  - `/app/scenes/{id}/subordinates` (GM-only), relayed; the target's owner applies it with `SubordinateService#grant`/`dismiss` and saves it with its status frame.
  - `SubordinateDto` carries the Subordinado's id.
  - Status broadcasts carry `subordinates`, so stand-ins mirror them, and a Prodigioso one reaches allies on other boards.
  - A foe's are stored on `MonsterSheetDocument`.
- **Rainha +2 Iniciativa:** the server's turn order adds it from the persisted lists (`SubordinateInitiative`), never on top of an override.
- **Rei:**
  - Non-cumulative per Rei.
  - A Descanso Longo renews an ally's Prodigioso Rei too.
  - A Prodigioso Rei arriving on another board gives its points via `shareKingsEgo`.

## Rulings (2026-10-01)

- **The summon in a Cena:** an invoked creature joins the Scene as its own participant, with its own token and its own
  PA. It acts right after its caster in the initiative order, and the caster's player controls and rolls for it.
- **Experimento de Lacerto's power** ("escolhido aleatoriamente") is rolled (1d6) when the Magia is cast.
- **Subordinados:** rules supplied by the table, in `docs/rules/subordinados.txt`. A Subordinado takes no actions;
  it grants its commander (or, if Prodigioso, the whole group) one benefit by grade.
  - Commanded at most up to Carisma.
  - No two of one grade unless one is Prodigioso.
  - Common and Prodigioso benefits stack.

## Readings to confirm

- **Benefit pick:** "escolhidos entre" (Bispo's 2PV/Rodada or Roubo de Vida 1, Cavaleiro's attack or damage, Peão's
  Vantagem or Margem Crítica, Rainha's PA or Iniciativa, Rei's Sorte or Autocontrole, Torre's Defesas or RA) is made
  when the Subordinado is gained, and kept.
- **Peão's "reduzindo a margem crítica … em -2":** the Margem Crítica Menor widens by two numbers on any Perícia but
  the Ataques and Esquiva e Aparar.
- **Prodigioso's "todos os Personagens do Grupo":** the commander's Scene sub-group, characters only.
- **"Cenas de Combate" / "cenas de estresse":** `SceneContext#isCombatScene()`.
- **Rei:** its 2 temporary Ego points are received when the Subordinado is gained, and again after each Descanso Longo.
- **Cativar Animal:** the touched animal leaves the enemy side for the Duração and becomes the caster's Subordinado
  (Cavaleiro or Torre, the caster's pick; Prodigioso with Falsa Matilha). It takes no actions while it is one, then
  returns to its own side. It can't be targeted again until a Descanso.

## Phases

1. **Subordinados (core).**
   - `SubordinateGrade` (6), a held `Subordinate` (grade, Prodigioso, benefit pick, source, optional Duração) on
     `CombatantSheet`.
   - Limits enforced by a `SubordinateService#command`.
   - Each benefit read where its stat is read: Defesas/RA, attack and damage bonuses, skill bonus, critical margin, PA,
     Iniciativa, Roubo de Vida, a per-Rodada heal, Ego on a Descanso Longo.
   - Prodigioso reaches allies through `SceneContext#getAllies()` (an ally-facing scan, like the Bastião's).
   - Also the users already in core: Agnação Ancestral's Peão and Abraçado pela Esquecida's shade.
2. **Invocation framework (core).** *Built differently than first planned:* no `Spell#getInvocation()` column and no
   concrete `InvocationEffect` (an effect sees only its target). Each tree gets an invocation service called after the
   cast — `magic.invocation.NatureInvocationService` — which spawns and places the creatures.
   - `Scene#addSummon(caster, sheet)` places it right after the caster in the order and records who it serves.
   - It is dismissed at the end of its Duração, on lethal damage ("o animal desaparece"), or when a newer summon of
     the same exclusivity group replaces it ("o anterior desaparece ao final do Turno").
3. **ALIADOS DA NATUREZA (core).**
   - `AliadoDaNatureza` / `PredadorRegional` / `ExperimentoDeLacerto` / `Anciente` as `SummonedMonsterTemplate`s
     with their Domínio do Mana tiers.
   - Each Magia's own rules:
     - Cativar Animal and Falsa Matilha (Subordinados);
     - Canção de Flora (counts, +2PM each, Fauna Flora);
     - Lacerto's rolled power;
     - Totem de Gaea (a Predador per Rodada; +1PA, Força +2, Vigor +2, −1 nível to animal allies in Distância Longa);
     - Orgulho de Lacerto (a GM-picked monster; Laboratório: 2 Experimentos);
     - Despertar Anciente.
4. **aventyrs-api.**
   - A Scene keeps its summons (`SceneSummonEntry`: id, caster, template key, parameters, position, joined/expiry
     Rodada, PV/PM/PD spent), with create/dismiss endpoints and STOMP broadcasts.
   - A character sheet keeps its Subordinados.
5. **Client.**
   - Casting an invocation Magia places the summon's token next to the caster, under the caster's control.
   - Summons are rebuilt locally from the template and their stored parameters, and dismissed on expiry or lethal
     damage.
   - The hub and Cena show a character's Subordinados.
