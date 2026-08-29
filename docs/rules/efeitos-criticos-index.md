# Correntes de Efeitos e Efeitos Críticos — source index

Source: `V19 - Corrente de Efeitos e Efeitos Críticos 20260814 (1).docx` (2026-08-14), converted
verbatim to `efeitos-criticos.txt` with `textutil -convert txt`. The authoritative catalog for
both mechanisms; read it alongside [`magias-index.md`](magias-index.md), which counts how the
Magias *use* them.

**45 entries in three lists**, none of which is a single flat catalog:

| list | n | lines | in code |
| --- | --- | --- | --- |
| Corrente de Efeitos | 13 | 9–46 | — no type exists; only `Definhar` is built |
| Efeitos Críticos **Defensivos** | 9 | 58–92 | ✅ all 9 — `DefensiveCriticalEffectType` |
| Efeitos Críticos **Ofensivos** | 23 | 120–214 | ✅ all 23 — `CriticalEffectType` |

Every entry in both Efeito Crítico lists is written as a **Maior/Menor pair**, matching
`CriticalEffect`'s existing `ACERTO_CRITICO_MAIOR`/`ACERTO_CRITICO_MENOR` split exactly. The
Correntes are single-bodied — no severity tier — which matches `EffectChainService` firing on a
margin rather than on a critical.

## The document's own prose sections are empty

Five headings carry no body text: `O que são Correntes de Efeitos` (L4), `Quando ativar uma
Corrente de Efeitos` (L5), `Resistência a Corrente de Efeitos` (L6), `O que são Efeitos
Críticos` (L49), `Como aplicar um Efeito Crítico` (L50), `O que é a Resistência à Críticos`
(L51). **The trigger rules are not in this document** — the margin-of-5 rule still comes from
`magias.txt` L49, and *Resistência a Correntes / a Críticos* is named twice as a real mechanism
but never defined anywhere. `EffectChainService`'s `RESOLUTO` margin-of-7 remains uncorroborated.

**L119 `Lista de Efeitos Críticos Ofensivos <remake pendente>`** — the offensive half is
explicitly marked for revision by its own author. Treat its 23 entries as current-but-unstable;
the defensive list and the Correntes carry no such marker.

## Efeitos Críticos Defensivos — a whole category the code has no concept of

> L54: "Efeitos Críticos Defensivos **substituem as falhas críticas inimigas** em caso de Sucesso
> Crítico nas rolagens de Defesas."
> L55: "Apenas Armaduras e Escudos recebem Efeitos Críticos Defensivos e seus efeitos são
> **cumulativos**, outros equipamentos Defensivos não concedem este tipo de benefício."

This is the missing other half of `AttackReceiver`. CLAUDE.md currently describes that direction
as having one critical trigger — the defence roll's **Falha** Crítica, firing the *attacker's*
effect. There is a second, symmetric branch: the defence roll's **Acerto** Crítico fires a
*defensive* effect drawn from the defender's own armour or shield, which then stands in place of
the falha crítica the attacker would otherwise have landed.

Two consequences worth separating:

- `CriticalEffect#validateCriticalHit`'s demand for an *Acerto* Crítico is **exactly right** for
  this branch — a Defensive effect is triggered by the defender genuinely rolling a critical
  success, and it inherits Maior/Menor from that roll with no hand-translation.
- It does **not** resolve the awkwardness CLAUDE.md flags. That is about the *other* branch —
  the attacker's offensive effect landing on the defender's Falha Crítica — and this document
  says nothing about it. Two branches, one solved by this catalog and one still open.

The nine are now `DefensiveCriticalEffectType` (2026-08-28): **Choque de AEther**,
**Contra-Atacante**, **Faísca de Determinação**, **Ímpeto Defensivo**, **Liberdade de Ação**,
**Provocar**, **Repelir e Suprimir**, **Retorno de Danos**, **Surto Arcano**.

It is a **separate enum from `CriticalEffectType`, deliberately.** That enum exists so a
creature's *immunities* can name an effect with no class yet — and nothing is ever immune to a
Defensive effect, since the defender's own gear grants it to the defender. `CriticalEffect
#applicableTo` must never filter one. Merging them would offer every stat block nine immunities
that cannot mean anything.

Several reach mechanisms this core does not have — forced movement ("empurra o atacante 2UD"),
forced targeting ("o próximo ataque dele deverá ter você novamente como alvo"), reactive damage
("Atacante corpo-a-corpo e sua arma sofrem 3d6+Vigor"), and per-item durability. All three of
those are already named gaps in CLAUDE.md's catalog, now with a second consumer apiece.

### Defensive effects are an equipment column, and `ArmorItem` is ready for it

The document assigns one to every armour and shield (L94–116). **All 8 `ArmorItem` constants are
covered, 1:1, with no gaps and no extras**:

| `ArmorItem` | defensive effect | | `ArmorItem` | defensive effect |
| --- | --- | --- | --- | --- |
| `ARMADURA_COMPLETA` | Retorno de Danos | | `MEIA_ARMADURA` | Retorno de Danos |
| `ARMADURA_DE_GLADIADOR` | Contra-Atacante | | `ROBE_CERIMONIAL` | Surto Arcano |
| `ARMADURA_DE_JUSTA` | Provocar | | `ROBE_DE_GUERRA` | Surto Arcano |
| `COURACA` | Retorno de Danos | | `ROUPA_PESADA` | Liberdade de Ação |

**Wired (2026-08-28)** as `ArmorItem#getDefensiveCriticalEffect()` — an eleventh column, non-null
for every constant. It sits on `ArmorItem` rather than on `Item` because "outros equipamentos
Defensivos não concedem este tipo de benefício": a Capa or an Elmo grants none, and a defaulted
column on `Item` would make a helmet and a breastplate answer alike — the same mistake keeping
`getDamageBase()` off `Item` avoids. Promote it to a shared interface when a `ShieldItem` needs
the identical shape, not before. **Nothing reads it yet**, since no code resolves an Acerto
Crítico on a Defesa roll. `ArmorDefensiveCriticalEffectTest` pins the transcription.

Two populations that have nowhere to live:

- **Six Escudos** — Braçadeiras, Bracelete Arcano, Broquel, Escudo Médio, Escudo de Corpo,
  Repulsor. There is no `ShieldItem` enum; shields are entirely unauthored.
- **Five Defesas Naturais** — Espinhos, Cascos e Conchas, Pele Escamosa ou Emplumada, Pele
  Escorregadia, and **Corpo Exposto (Sem armaduras ou escudos)**. That last one is the
  *unarmoured default*, so this is not a monster-only concern: every combatant with an empty
  equipment list has a defensive effect (Liberdade de Ação) under these rules.

## Efeitos Críticos Ofensivos — `CriticalEffectType`, now complete

**Transcribed in full (2026-08-28).** All 23 constants exist, each carrying its own Maior/Menor
rules text verbatim; 5 have a class behind them and 18 are identities only. `CriticalEffectTypeTest`
pins the catalog, the implemented/named-only split, and the 14 an Magia can name.

| catalog entry | constant | | catalog entry | constant |
| --- | --- | --- | --- | --- |
| Amaldiçoar | `AMALDICOAR` | | Fortalecer | `FORTALECER` |
| Amenizar | `AMENIZAR` | | Guilhotina | `GUILHOTINA` |
| Atordoante | `ATORDOANTE` | | Imunizar | `IMUNIZAR` |
| Cataclismo | `CATACLISMO` | | Inflamar | `INFLAMAR` |
| Desmembrar | `DESMEMBRAR` | | Oferenda Maldita | `OFERENDA_MALDITA` |
| Dilacerar | `DILACERAR` | | Potencializar | `POTENCIALIZAR` |
| Empalar | `EMPALAR` | | Prevenir | `PREVENIR` |
| Estilhaçador | `ESTILHACADOR` | | Primor | `PRIMOR` |
| Excruciante | `EXCRUCIANTE` | | Purga-Mana | `PURGA_DE_MANA` ⚠ |
| Execução Real | `EXECUCAO_REAL` | | Sabotar | `SABOTAGEM` ⚠ |
| Ferida Profunda | `FERIDA_PROFUNDA` | | Sangramento | `SANGRAMENTO` |
| | | | Toque do AEther | `TOQUE_DO_AETHER` |

⚠ Two constants are named differently from the catalog: `PURGA_DE_MANA` for *Purga-Mana* and
`SABOTAGEM` for *Sabotar*. Same effect, no ambiguity — worth knowing before a rename looks like
a new entry.

All 23 are now constants. 11 of the 14 added are named by Magias; the three that are not
(Desmembrar, Empalar, Estilhaçador) are weapon-flavoured and reach this core through stat blocks
instead. **Nothing was invented** — every constant that predated the import is in the catalog too,
so the gap was one-directional.

### The five implemented effects match the catalog's numbers exactly

Worth stating, because these classes predate this document:

- **Sangramento** — 2PV immediate + 1PV/Rodada; Menor bounded by the target's Vigor, Maior
  unbounded. `IMMEDIATE_DAMAGE = 2`, `PER_ROUND_DAMAGE = 1`, Menor's `remainingRounds` read from
  Vigor. Exact.
- **Purga-Mana** — same shape, bounded by Foco instead of Vigor. `ManaPurge` reads Foco. Exact.
- **Excruciante** — same shape again, bounded by Instinto. **Not implemented**, but the catalog
  shows it is the third instance of one shared pattern; `Sangramento`/`ManaPurge` are already a
  matched pair and a third would earn the generalisation CLAUDE.md's second-consumer rule asks
  for.
- **Execução Real** — Maior unconditional; Menor conditional on current PV ≤ 2×Vigor.
  `MENOR_VIGOR_MULTIPLIER = 2`. Exact.
- **Primor** — Maior 2 temporary Ego points, Menor 1, from Sorte **or** Autocontrole chosen
  randomly. Note the catalog distinguishes the recovery: Maior recovers at the next **Descanso
  Longo**, Menor at the next Descanso. `PendingEgoRecovery` carries no such distinction.

## Corrente de Efeitos — 13 catalog entries, and none of them is what a Magia names

The 13: **Definhar**, **Enrijecer Musculatura**, **Escancarar Defesas**, **Excomungar**,
**Explosão Cataclísmica**, **Ferida Infecciosa**, **Golpe Trovejante**, **Magicae Mortis**,
**Oprimir**, **Remover Aflição**, **Rugido**, **Toque Sombrio**, **Veneno Vampírico**.

Two of these are already cited by name in this codebase, confirming both readings:
`Withering` is **Definhar**, and `ABRIR_DEFESAS` is **Escancarar Defesas**.

⚠ **There are two disjoint populations of Corrente de Efeitos, and they do not overlap at all.**
The 145 complete Magias name **69 distinct Correntes inline** (`Corrente de Efeitos – Domar`,
`– Pouso Seguro`, `– Nevasca Maior`…), plus 7 more in the Umbral draft. **Not one** of those 76
appears in this catalog's 13, and not one of the 13 is named by any Magia.

So a Corrente is not a reference into a shared enum. A Magia authors its own, bodily, the way it
authors its `Efeito Alternativo` — while the 13 here are the generic ones reached from weapons,
abilities and equipment. Modelling `Corrente de Efeitos` as an enum keyed like
`CriticalEffectType` would fit the 13 and refuse all 76.

### This closes the "Malefício classification" gap

CLAUDE.md lists *Malefício classification* as missing: "No Encantamento/Maldição/Doença tag
exists — see `Withering`, `ABRIR_DEFESAS`." The catalog tags them explicitly, and the taxonomy
is **Maldição / Envenenamento / Encantamento**:

- *Definhar* — "Este é um Efeito de **Maldição**."
- *Enrijecer Musculatura* — "Este é um efeito de **Envenenamento**."
- *Veneno Vampírico* — "Este é um efeito de **Envenenamento**."

And two entries **read** the tag, so it is not inert data:

- *Excomungar* — "Remove uma das **Maldições ou Encantamentos Naturais** presentes no alvo."
- *Imunizar* (offensive crit) — "alvo se torna imune a **encantamentos nocivos e maldições**."

*Doença* is named nowhere in this document; *Ferida Infecciosa* carries no tag despite its name.
The three confirmed tags are the ones above.

## Mechanisms these 45 entries need that this core lacks

Beyond the three named under the defensive list, and excluding anything already built:

| needed by | mechanism | CLAUDE.md gap |
| --- | --- | --- |
| Oprimir, Veneno Vampírico, Magicae Mortis | Roubo de Determinação; Roubo de Vida on a Corrente | *Roubo de Mana / de Determinação* — Oprimir is its first real consumer |
| Ferida Profunda, Veneno Vampírico | reducing a target's **Multiplicador de PV** | new — the field is a `Character` column with no effect path |
| Ferida Profunda, Excruciante, Sangramento | suppressing or halving **healing** | *healing inversion* — `heal` has no hook |
| Estilhaçador, Fortalecer, Repelir e Suprimir, Sabotar | per-copy item **Dureza**/damage | *owned/produced item copy* |
| Cataclismo, Toque Sombrio, Explosão Cataclísmica | damage typed **Elemental / Profano** | *damage-type-scoped mitigation* |
| Enrijecer Musculatura | Desvantagem scoped to *Perícias Físicas* | trackable — a named-skill scope, per CLAUDE.md's Vantagem rules |
| Atordoante, Repelir e Suprimir | raising an action's **PA cost** | new — `ActionProfile#adjustSkillRollCost` is the nearest hook |
| Golpe Trovejante | applying a weapon's natural Efeito Crítico **twice** | new |
| Definhar, Veneno Vampírico | duration in *Vigor* Rodadas | already the `Sangramento` pattern |
| Liberdade de Ação | ignoring **Terreno Difícil** | *Terreno difícil* — `TerrainType` is Scene-wide |
| Surto Arcano | casting immediately, off-turn, gated by rung | new |

## Bearing on the Magias import

Three of the six blockers in `magias-index.md` are now answerable from authored data rather
than inference:

1. **The 11 missing `CriticalEffectType` constants are confirmed**, with exact Maior/Menor text
   for each — `Potencializar` and `Amenizar` alone cover 58% of the spell catalog. Add 14 to
   cover the full offensive list, or 11 for the Magias alone.
2. **`Corrente de Efeitos` must be a per-Spell authored field, not an enum reference** — the
   69 names the Magias use are disjoint from this catalog's 13. That settles the "no column"
   note in `magias-index.md`.
3. **`Potencializar` requires a Magia to remember the unit its Duração was authored in** — "A
   duração da magia aumenta em +2d6 **unidades**", not *+2d6 Rodadas*. Rodadas *is* a workable
   canonical unit (1 minuto = 12 Rodadas, so every fixed Duração converts), but converting at
   authoring time discards the unit and makes this — the most common Efeito Crítico in the game,
   at 57 of 145 Magias — add 2d6 Rodadas where it should add 2d6 minutes, a 12× shortfall on the
   six minute-and-hour Magias. Store both. See `magias-index.md`.

Still unanswered by either document: `Tempo de Ativação` has no column, `MagicType` still lacks
`TEMPORAL`/`UMBRAL`, and the `getConjurationSkillType()`/`getAttackSkillType()` split.

## Source-document defects — do not "fix" silently

- **L119** `<remake pendente>` — the entire offensive list is marked for revision.
- **L149–150 *Estilhaçador* has two `Maior:` bullets and no `Menor:`.** The second is plainly the
  Menor tier (one random item rather than all items). The random-item table that follows names
  six slots but is numbered `1: Arma; 2: Armadura; 3: Escudo; **4;** Capa; 5: Elmo; 6: Núcleo
  Tecnológico` — a semicolon where a colon belongs.
- **Ímpeto Defensivo is granted by nothing.** It is catalogued in full (L70–72), but the
  "Atualizando os Equipamentos Defensivos" table assigns it to no Armadura, no Escudo and no
  Defesa Natural — the only one of the nine that no gear in the ruleset grants. Every other
  effect is assigned between one and four times. Adding a `ShieldItem` will not close this.
- **L129 *Atordoante* Maior** is missing its colon: "Maior o alvo não pode…".
- **L82 `Repelir e Suprimir:`** — the only entry heading with a trailing colon.
- **L196 *Primor*** has no blank line separating it from *Prevenir*'s body above it, so a
  paragraph-based parse merges the two entries.
- **L59/L67** "é reduzindo em -1" — typo for *reduzido*, in both Choque de AEther and Faísca de
  Determinação.
- **L84** "os danos são causados ao conjurador a supressão afeta" — a missing conjunction, and
  "por 1 Rodada … por 1 Rodada" is duplicated in the same sentence.
- **L167 *Fortalecer* Maior** grants "RD e **RM**" — `RM` appears nowhere else in this ruleset;
  the code models RD and RA. Likely *RA*, but it is not stated.
- **L180–181 *Inflamar*** — the two tiers deal **identical** damage (2 per Rodada); they differ
  only in the PA cost to extinguish (Maior 3PA, Menor 2PA). That ordering is right, but every
  other entry in the list separates its tiers by magnitude, so the equal damage looks unfinished.
