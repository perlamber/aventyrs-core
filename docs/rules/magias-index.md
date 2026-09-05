# Magias — source index

Source: `V19 Magias - 20250523.docx` (2025-05-23), converted verbatim to `magias.txt`
with `textutil -convert txt`. **189 Magias across 27 Árvores**, in two sections:

| section | lines | trees | Magias | state |
| --- | --- | --- | --- | --- |
| Módulo Básico + Lista de Arcanismos e Preces Divinas | 1–2332 | 20 | 145 | complete — every descriptor filled |
| Sombras da Umbra (Magias Umbrais) | 2333–3052 | 7 | 44 | **draft** — 265 printed-but-empty descriptor lines |

**Read with [`efeitos-criticos-index.md`](efeitos-criticos-index.md).** That document is the
authoritative catalog for the `Efeito Crítico:` and `Corrente de Efeitos –` fields below; this
one only counts how the Magias use them.

The **category tag in parentheses after each Árvore's name is authoritative** (`PIROMANCIA
(Divina/Elemental: Fogo)`), and it is a property of the *tree*, never of an individual Magia —
unlike Talentos, where the tag sits on each entry. A Magia's own identity line is its rung:
`Semente – Cativar Animal`. Grep by rung, then walk back to the nearest ALL-CAPS heading for
the tree.

## Coverage against `org.aventyrs.core.magic`

**All 145 complete Magias are authored** (2026-08-29), in `org.aventyrs.core.magic.catalog`: one
`MagicTree` constant per Árvore, one `<Tree>Spell` enum per Árvore holding its Magias, and
`MagicBranch` holding all 36 ramificações. `SpellCatalog` is the entry point;
`SpellCatalogTest` pins the per-tree counts, the branch invariants and every blank descriptor
listed below against this document, so a drift fails the build rather than silently dropping an
Árvore.

**The 44 Umbral Magias stay unauthored** — not because their columns are unrepresentable, but
because acquisition is gated on a *Força Umbral* Talento that does not exist. See "Sombras da
Umbra" below for the full listing, what each Magia actually carries, and the two rules that would
fill the universally blank columns. `MagicType.UMBRAL` is real so they can be typed the day the
Talento lands.

`TestSpellTree`/`TestSpellBranch`/`TestSpell` remain, deliberately: the acquisition-gate tests
need Magias placed at arbitrary spots in a tree of a known shape, and pinning the engine's own
tests to real catalog entries would make a rules revision break them.

### What this document confirms the code already has right

- **`BranchLevel`'s PM costs are exact.** The doc's *Pontos de Mana para Conjuração* table
  (L35–45) reads Semente Nenhum / Broto 1 / Muda 3 / Emergente 5 / Florescente 7 — byte-for-byte
  `SEMENTE(0)`, `BROTO(1)`, `MUDA(3)`, `EMERGENTE(5)`, `FLORESCENTE(7)`.
- **`SpellTree.validateBranches`' zero-or-two rule survives the real catalog.** All 20 complete
  trees carry either no ramificação or exactly two. Not one has a lone branch or a third.
- **The Corrente de Efeitos margin of 5 is stated text, not an inference** — L49: "se o
  resultado da rolagem superar a DM do alvo em 5 ou mais". That settles the offensive direction
  `AttackDelivery` uses. The *inverted* margin `AttackReceiver` applies is still an inference;
  this document says nothing about the defensive direction.
- **Casting is rolled against DM** (L48), and a single Área de Efeito roll is compared against
  every target's DM separately — matching `SpellCastingService`'s two-roll shape.

### ⚠️ The doc prints GD as `(expert|base)` — the reverse of `DifficultyLevel`

`DifficultyLevel` is declared `EASY(14, 13)` = `(baseValue, expertValue)`. The doc writes the
same tier as `Fácil (13|14)`. **Smaller number first, i.e. expert first, every time.** Verified
across all five tiers used:

| doc | constant |
| --- | --- |
| `Fácil (13\|14)` | `EASY(14, 13)` |
| `Médio (16\|18)` | `MEDIUM(18, 16)` |
| `Difícil (20\|23)` | `HARD(23, 20)` |
| `Muito Difícil (25\|28)` | `VERY_HARD(28, 25)` |
| `Improvável (32\|36)` | `UNLIKELY(36, 32)` |

A transcription that reads the pair left-to-right into `DifficultyLevel.of(...)` will get every
Magia's GD wrong in the same direction. Only these five tiers appear — `VERY_EASY`,
`UNIMAGINABLE` and `MIRACLE` are never used by a Magia.

## Trees, by section

Rung shape is `S/B/M/E/F`. A rung with 2 entries is where the tree diverges or stays split.

### Módulo Básico — 20 trees, 145 Magias

| tree | categories | S | B | M | E | F | n | branch shape |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ALIADOS DA NATUREZA | Natural/Invocação | 1 | 1 | 2 | 2 | 1 | 7 | Muda → Florescente |
| ANULAÇÃO | Primordial | 1 | 1 | 1 | 1 | 1 | 5 | *linear* |
| ARSENAL ELEMENTAL | Encantamento/Elemental: Todos | 1 | 1 | 2 | 2 | 1 | 7 | Muda → Florescente |
| ARTESÃO | Encantamento/Elemental: Magma | 1 | 1 | 2 | 1 | 1 | 6 | Muda → Emergente |
| CORPO ROCHOSO | Encantamento/Elemental: Terra | 1 | 1 | 2 | 2 | 1 | 7 | Muda → Florescente |
| DOMINIO PRIMORDIAL | Primordial | 1 | 2 | 1 | 1 | 1 | 6 | Broto → Muda |
| FÚRIA DE TESLA | Encantamento/Elemental: Eletricidade | 1 | 1 | 1 | 2 | 2 | 7 | Emergente → *never* |
| IRA DE VULCANO | Elemental: Magma | 1 | 1 | 1 | 1 | 1 | 5 | *linear* |
| MORTE | Profana | 1 | 2 | 2 | 2 | 1 | 8 | Broto → Florescente |
| OCULTAÇÃO | Encantamento | 1 | 1 | 2 | 2 | 2 | 8 | Muda → *never* |
| PIROMANCIA | Divina/Elemental: Fogo | 1 | 2 | 2 | 2 | 2 | 9 | Broto → *never* (Eldur / Boros) |
| POLIMORFISMO | Encantamento/Natural | 1 | 2 | 2 | 2 | 2 | 9 | Broto → *never* |
| PROFANAR | Profana | 1 | 2 | 2 | 2 | 1 | 8 | Broto → Florescente |
| PROTEÇÃO INVERNAL | Encantamento/Elemental: Gelo | 1 | 1 | 2 | 2 | 1 | 7 | Muda → Florescente |
| REANIMAR | Profana/Invocação | 1 | 1 | 2 | 2 | 2 | 8 | Muda → *never* |
| REGENERAÇÃO | Encantamento/Elemental: Água | 1 | 1 | 1 | 2 | 1 | 6 | Emergente → Florescente |
| TEMPO | Encantamento/**Temporal** | 1 | 2 | 2 | 1 | 1 | 7 | Broto → Emergente |
| TRANSPORTE | **Temporal**/Invocação | 1 | 2 | 2 | 2 | 2 | 9 | Broto → *never* |
| VIDA | Natural/Divina | 1 | 2 | 2 | 2 | 2 | 9 | Broto → *never* |
| VOO | Encantamento/Elemental: Ar | 1 | 1 | 2 | 2 | 1 | 7 | Muda → Florescente |

**Divergence is not always at Muda, and convergence is optional.** 8 trees diverge at Broto,
8 at Muda, 2 at Emergente, 2 never. 7 converge at Florescente, 2 at Emergente, 1 at Muda, and
**8 never converge at all**. `TestSpellTree` models only one of those twelve shapes
(Muda → Florescente); it is a valid stub, not a representative one.

### Sombras da Umbra — 7 trees, 44 Magias (draft)

⚠️ **This section used to say 45, and used to say only `Efeito:` was written. Both were wrong**,
and neither had been checked against the source until the complete section was authored. What
follows is parsed from L2333–3002 (the expanded blocks; L3003+ is the known outline duplicate).

Gated behind a *Força Umbral* Talento (`Geral/Devoto/Destino`) that `MetamagicoFeat` does not
carry — its prerequisite is "Apenas Devotos de Senhores Umbrais, Talentos Arcanismo ou Aptidão
Mágica Ampla", and only `APTIDAO_MAGICA_AMPLA` of those exists today.

| tree | categories | n |
| --- | --- | --- |
| Calor Invertido | Umbral/Elemental: Fogo e Gelo | 8 |
| Afogamento Umbral | Umbral/Elemental: Água | 5 |
| Invocação do Raio Negro | Umbral/Elemental: Eletricidade | 5 |
| Chamado do Observador | Umbral/Divina | 6 |
| Armada Decapitada | Umbral/Profana | 7 |
| Julgamento do Tecelão | Umbral/Elemental: Terra e Magma | 6 |
| Devorador de Mundos | Umbral/Elemental: Natural/Ar | 7 |

**Devorador de Mundos has 7, not the 8 first counted here** — see the L3000 stray in the defects
section below.

#### The 44, by tree and rung

Rungs are printed in document order, which for several of these trees is not depth order.

**Calor Invertido** (8) — *the best-specified tree in the section*: 5 of its 8 carry an Efeito
Crítico, and 5 carry an Alcance. Only Tempo/Perícia/GD are missing throughout.

| rung | Magia | filled beyond Descrição/Efeito |
| --- | --- | --- |
| Semente | Furta-Chamas | Alcance |
| Broto | Fumaça Furta-Luz | Efeito Crítico (Potencializar), Duração (3 Rodadas), Alcance |
| Muda | Privação de Calor | Efeito Crítico (Purga-Mana), Duração + Alcance (both *A mesma da Fumaça Furta-Luz*) |
| Emergente | Combustão Sombria | Efeito Crítico (Purga-Mana), Duração + Alcance (same reference) |
| Broto | Lanças Fuligens | Efeito Crítico (Inflamar), Duração (Instantâneo), Alcance |
| Muda | Lembranças de Amaterasu | Efeito Crítico (Inflamar), Alcance |
| Emergente | Invocação de Chamas-Negras | Efeito Crítico (Inflamar) |
| Florescente | Vislumbre do Titã de Chamas Negras | Efeito Crítico (Inflamar) |

**Afogamento Umbral** (5) — *the only tree with `Perícia Chave` filled*, and it holds all five of
the section's five: every one is **Ataque Corpo-a-Corpo**.

| rung | Magia | filled beyond Descrição/Efeito |
| --- | --- | --- |
| Semente | Hidrocinestesia | Perícia, Duração (Instantânea) |
| Broto | Correntes Oceânicas | Perícia, Duração, Alcance (Toque) |
| Muda | Cárcere Aquático | Perícia, Alcance (Toque ou Alvo Único Distante) |
| Emergente | Afogamento Umbral | Perícia, Duração, Alcance (Toque) |
| Florescente | Sombra da Coruja Afogada | Perícia |

**Invocação do Raio Negro** (5) — Induzir Condução *(Duração: 2 Rodadas)* · Estalo *(Efeito
Crítico: Atordoante)* · Invocação do Raio Negro · Impor Supercondução Sombria · Rugido do Dragão
Negro. The last three carry nothing but Descrição and Efeito.

**Chamado do Observador** (6) — Visão Longínqua · Mapear · Visão dos Justos *(3 Rodadas)* · Visão
do Pecadores *(3 Rodadas)* · Chamado do Observador · Avatar dos Muitos Olhos *(2 Rodadas)*. Note
it diverges at **Muda**, into the two Visões.

**Armada Decapitada** (7) — Conselheiro Umbral *(2 Rodadas)* · Guardiões Sombrios · Reverendo
Decapitado · Mula sem Cabeça · Sombras dos Berserks · Lorde Obscuro · Barganha dos Dullahan
*(Concentração + 1 Rodada)*. Diverges at **Muda** (Reverendo Decapitado / Sombras dos Berserks).

**Julgamento do Tecelão** (6) — Caminhar do Tece-Mortes *(2 Rodadas)* · Viagem Aracnídea · Domínio
das Teias Negras · Convocar Tecedeiras Vulcânicas · Aspecto do Destino · Julgamento do Tecelão.
⚠️ Its expanded block has **one Muda and two Emergentes**, which no complete-section tree does —
and the L3003 outline disagrees; see the defects section.

**Devorador de Mundos** (7) — Bote Inesperado · Envenenar a Mente · Envenenar com a Fúria ·
Corrupção por Contato *(Tempo de Ativação: **Reação** — the section's only filled one)* · Praga da
Corrupção · Forma da Serpente Emplumada · Devorador de Mundos. Diverges at **Broto**.

#### What is actually blank

Per descriptor, across the 44:

| descriptor | blank | filled |
| --- | --- | --- |
| `GD da Conjuração:` | **44** | 0 |
| `Tempo de Ativação:` | 43 | 1 |
| `Perícia Chave para Conjuração:` | 39 | 5 |
| `Efeito Crítico:` | 36 | 8 |
| `Alcance:` | 35 | 9 |
| `Duração:` | 30 | 14 |
| `Descrição:` | 34 | 11 |
| `Efeito:` | 4 | 38 |

**265 printed-but-empty descriptor lines**, not the 192 this document used to quote. Only `GD da
Conjuração` is universally blank.

#### Why they stay unauthored

**Not because the columns are unrepresentable.** `Spell` already tolerates a `null`
`getCastingDifficultyLevel()` (7 authored Magias have one) and a `null` `getAttackSkillType()`
(2 do), so a thin Magia is expressible. The real reasons:

1. **Acquisition is gated on a Talento that does not exist.** *Força Umbral* is not in
   `MetamagicoFeat`, and its own prerequisite ("Apenas Devotos de Senhores Umbrais") needs a
   devotion concept this core has none of — the same blocker Piromancia's two Florescentes hit
   with "Apenas devotos de Eldur". Authoring the Magias without it produces a catalog no character
   can ever reach.
2. **Two trees contradict themselves** between their expanded block and the L3003 outline, and
   Julgamento do Tecelão's rung shape (one Muda, two Emergentes) matches neither the outline nor
   any complete-section tree.

#### The two rules that would fill the universal blanks

Recorded now so that authoring them later is transcription rather than invention:

- **`GD da Conjuração` is derivable from the rung** — see the `GD da Conjuração` section below,
  where the rule is verified against all 145 authored Magias with zero deviations. Every one of
  these 44 has a `BranchLevel`, so every one has a GD.
- **An unstated `Tempo de Ativação` is 2PA.** It is also the modal value of the complete section
  (58 of 145). The one Umbral entry that *does* state a time (Corrupção por Contato) states a
  Reação, so the default must not be applied blindly over an authored value.

That leaves `Perícia Chave` as the only genuinely missing mechanical column on 39 of the 44 — and
the complete section's own rule at L47 (Corpo-a-Corpo for Toque, à Distância for Alvo
Distante/Área, Domínio do Mana otherwise) derives it from the Alcance, which 9 of them state.

## Descriptor columns vs. the `Spell` interface

Field-by-field, over the 145 complete Magias.

**Every column below now has a home.** The four that had none when this document was first
written — Corrente de Efeitos, Tempo de Ativação, the Duração unit, and the Elemental subtype —
were added while authoring; the sections that follow record what each one cost and what is *still*
missing behind it.

| doc field | `Spell` member | fit |
| --- | --- | --- |
| rung (`Semente –`) | `getBranchLevel()` | exact |
| tree heading | `getTree()` | exact — `MagicTree`, 20 constants |
| 2nd entry at a rung | `getBranch()` | exact — `MagicBranch`, 36 constants |
| `GD da Conjuração:` | `getCastingDifficultyLevel()` + 2 flags | exact — see below |
| `Descrição:` | `getDescription()` | exact |
| `Efeito:` | `getPrimaryEffectDescription()` | exact |
| `Efeito Alternativo –` (63×) | `getSecondaryEffectDescription()` | exact |
| `Corrente de Efeitos –` (60×) | `getEffectChainDescription()` | exact — prose, not an enum |
| `Efeito Crítico:` | `getCriticalEffectType()` | exact — 3 blanks stay `null` |
| `Perícia Chave para Conjuração:` | `getAttackSkillType()` | exact — 2 blanks stay `null` |
| `Duração:` | `getDuration()` (`SpellDuration`) | exact — see below |
| `Alcance:` | `getTargeting()` + `getAlternateTargeting()` | exact — see below |
| `Tempo de Ativação:`/`Tempo de Conjuração:` | `getActivationTime()` | exact |
| category tag | `getPrimaryType()`/`getSecondaryType()` | delegates to the tree |
| category tag's `Elemental: X` | `SpellTree#getElementalType()` | exact — `ElementalType` |

### `MagicType` was missing two constants — both added

CLAUDE.md flags this as an open question under "Árvores de Magia". This document settles it:

- **`TEMPORAL` is required by the complete list, not just the draft.** `TEMPO
  (Encantamento/Temporal)` and `TRANSPORTE (Temporal/Invocação)` are two fully-specified,
  16-Magia trees. **Added**, and both trees are authored against it.
- **`UMBRAL` is required by all 7 draft trees**, and by the *Força Umbral* Talento. **Added**,
  though nothing uses it yet — those trees stay unauthored.
- **`NATURAL` is listed as an Elemental subdivision**, not a top-level type: L15 reads
  "magias Elementais – subdivididas em Fogo, Magma, Terra, **Natural**, Água, Gelo, Ar e
  Eletricidade". Yet three trees tag it standalone (`Natural/Invocação`, `Natural/Divina`,
  `Encantamento/Natural`). The document uses it both ways and does not reconcile them.

`MagiaAlternativaAbility` gained a matching constant for each, per CLAUDE.md's
one-constant-per-`MagicType` rule. **`NATURAL` was kept as a top-level type** — three trees author
it standalone and dropping it would leave them unexemptable — *and* added to `ElementalType`, so
whichever reading a given tree uses has somewhere to go. That is not a resolution of the
document's inconsistency, only a refusal to pick a side on its behalf.

**The Elemental subtype is now `ElementalType`**, a separate column on `SpellTree` rather than
constants on `MagicType`: the two are asked about independently, and folding the element into the
type would silently narrow a `MagiaAlternativaAbility.ELEMENTAL` exemption from covering both
Piromancia and Fúria de Tesla. 11 of the 20 trees carry one.

**`getSecondaryType()` is now nullable, and *six* trees are single-typed** — not three, as first
counted here. `ANULAÇÃO (Primordial)`, `DOMINIO PRIMORDIAL (Primordial)`, `IRA DE VULCANO
(Elemental: Magma)`, `MORTE (Profana)`, `OCULTAÇÃO (Encantamento)` and `PROFANAR (Profana)`. Umbral
trees run the other way and carry three (`Umbral/Elemental: Terra e Magma`); no third column
exists, since those trees are unauthorable for unrelated reasons.

### `CriticalEffectType` — all 14 the Magias name now resolve

By frequency across the 145 complete Magias:

| Efeito Crítico | n | constant |
| --- | --- | --- |
| Potencializar | 57 | **missing** |
| Amenizar | 27 | **missing** |
| Fortalecer | 9 | **missing** |
| Inflamar | 8 | **missing** |
| Oferenda Maldita | 7 | **missing** |
| Imunizar | 7 | **missing** |
| Prevenir | 6 | **missing** |
| Dilacerar | 4 | `DILACERAR` |
| Atordoante | 4 | **missing** |
| Amaldiçoar | 3 | `AMALDICOAR` |
| Guilhotina | 3 | **missing** |
| Toque do AEther | 3 | **missing** |
| Execução Real | 2 | `EXECUCAO_REAL` |
| Cataclismo | 2 | **missing** |

**All 14 now resolve** — `CriticalEffectType` was completed to the full 23-entry offensive
catalog on 2026-08-28, each constant carrying its Maior/Menor rules text verbatim. This column is
no longer a blocker for authoring a Magia. See
[`efeitos-criticos-index.md`](efeitos-criticos-index.md).

Note the inverse too: `SANGRAMENTO`, `PURGA_DE_MANA`, `PRIMOR`, `SABOTAGEM`, `EXCRUCIANTE` and
`FERIDA_PROFUNDA` are named by **no** Magia in this document; they come from weapons and stat
blocks. The two catalogs barely overlap.

### `Corrente de Efeitos` is authored per Magia, not referenced from a catalog

The 145 complete Magias name **69 distinct Correntes inline** (`Corrente de Efeitos – Domar`,
`– Pouso Seguro`, `– Nevasca Maior`…), and the Umbral draft adds 7 more. **None of the 76
appears in the shared 13-entry catalog**, and none of those 13 is named by any Magia — the two
populations are completely disjoint.

So this is not a reference into an enum the way `getCriticalEffectType()` is. A Magia authors
its Corrente bodily, like its `Efeito Alternativo`. **Closed** as
`Spell#getEffectChainDescription()`, prose and nullable.

Three wrinkles authoring turned up, each handled on the constant rather than in the type:

- **Some Correntes are written inside the `Efeito:` rather than on their own line** (Abraço de
  Tesla's *Brilhantismo de Tesla*, Invisibilidade Verdadeira's *Invisibilidade Persistente*,
  Revigorar's *Sobrecura*). Only a `Corrente de Efeitos –` line is the "superar a DM do alvo em 5"
  descriptor, so those stay in the prose where the document puts them.
- **One Magia carries two Corrente lines** — Serra-Pernas has *Espremer* and *Corrente de Efeitos
  Alternativa – Fraqueza Momentânea*. Both go into the one prose field, headed as printed.
- **Two Magias name *Sobrecura* with no body**, relying on Revigorar's text for it.

### `getDuration()` — now `SpellDuration`, which keeps the authored unit

> **1 minuto = 12 Rodadas** (so 1 hora = 720). This conversion is **stated in none of the three
> source documents** — not `magias.txt`, not `efeitos-criticos.txt`, not `talentos.txt`. It is
> ruleset knowledge that has to be carried in, which is exactly why it is recorded here.

With it, every *fixed* Duração in the catalog reduces to a plain count of Rodadas:

| shape | n | as Rodadas |
| --- | --- | --- |
| `N Rodadas` | 67 | 1, 2, 3, 5, 10 |
| `Instantânea` / `Instantâneo` | 38 | 0, if that reading is adopted |
| `1 minuto` | 3 | 12 |
| `2 Minutos` / `Até 2 minutos` | 2 | 24 |
| `Até 1 Hora` | 1 | 720 |

That is **111 of 145** an `int` holds outright. The remaining 34 did not need a different numeric
type — they needed *companions* to the int, and each got one:

| shape | n | how it is held |
| --- | --- | --- |
| `Concentração + N Rodada(s)` | 19 | a `concentration` flag beside the count — see below |
| `A mesma de ‹other Magia›` | 8 | `DurationKind.SAME_AS_REFERENCED` + a `Supplier` |
| `‘Vigor do Alvo’ Rodadas` | 6 | `DurationKind.TARGET_ATTRIBUTE` + an `AttributeDomain` |
| `Até o final do turno` | 1 | `DurationKind.UNTIL_END_OF_TURN` |

⚠️ **The referential count is 8, and a previous revision of this document wrongly raised it to
10.** Five point at Rigidez Térrea, two at Solo Profano, one at Abraço de Tesla. The two extra were
`A mesma da Fumaça Furta-Luz` (L2378, L2391) — which are in the **Umbral** section and therefore
not among the authored 145. The code was always right: exactly 8 `SpellDuration.sameAs` call sites.

It is a `Supplier<SpellDuration>` rather than a resolved value, for the same reason `MetamagicoFeat` holds
its `FeatRequirements` as one: the referenced Magia is a sibling enum constant, and Java forbids
reading one from another's constructor arguments. That deferral is not cosmetic — Corpo Rochoso's
*Dádiva de Epona* raises Rigidez Térrea's own Duração, and a copied `3` would silently not follow.

**All six `‘Vigor do Alvo’` durations are the Regeneração tree**, which shares one Duração, one
Alcance and one Efeito Crítico across all six of its Magias.

There is deliberately **no** `CASTER_ATTRIBUTE` kind, although one caster-scaled duration exists:
Campo de Invisibilidade's Efeito Alternativo runs for "Concentração + Foco Horas". An `Efeito
Alternativo` is not a separate `Spell` and `SpellData` holds one Duração, so a kind for it would be
a constant no authored value could use.

#### `Concentração + N Rodadas` is two phases, and `N` is the *trailing* one

While the caster stays focused on the Magia its effect is active with **no countdown at all**.
Concentração breaks when the caster **casts another Magia or attacks** — and only then does the
`N Rodadas` count begin. So `N` is a trailing duration, not a total.

A naive `getDuration() = 2` is therefore wrong in both directions at once: it starts the clock
immediately, and it caps at two Rodadas an effect that could legitimately run the whole Cena.
`Semente - Queda Lenta` is the limiting case — `Duração: Concentração` with no trailing count at
all, i.e. it ends the instant concentration breaks.

**Phase 1 needs no new machinery.** `TemporaryEffect.remainingRounds` is a **nullable**
`Integer`: `tick()` no-ops and `isExpired()` returns false while it is `null`. That is already
this core's encoding for "runs until something stops it" — `Sangramento`/`ManaPurge` Maior use
exactly it, via `Optional.empty()`, for their "até o fim da cena" tiers.

**What is missing is the transition.** `remainingRounds` is private with a Lombok `@Getter` and
no mutator, so nothing can move an effect from `null` to `2`. One narrow state-change method is
the whole gap — not a new duration type.

**The break trigger has no single chokepoint.** Casting is one (`SpellCastingService#castSpell`),
and `AttackDelivery#resolve` is the other — breaking concentration there would follow the same
"the choice of entry point is the distinction" discipline CLAUDE.md documents for deliberate Ego
spends versus drains, and needs no observer, which this codebase deliberately has none of. But an
attack Perícia can be rolled straight through `AbstractSkillInteraction#applyTo` without passing
`AttackDelivery`, so that chokepoint is not airtight today. Note also that **being attacked must
not break concentration** — only the caster's own attack does, so `AttackReceiver` is not a
trigger.

**One Concentração at a time is derived, not enforced.** Casting a second Magia breaks the first
by the rule itself, so no "at most one" invariant needs checking anywhere — the same
recompute-rather-than-store discipline as a Conjurador's branch in a tree. *(A reading: the rules
text quoted here says casting breaks concentration, and does not separately state a limit.)*

⚠ **The concentrating character and the affected sheet are usually not the same.** Only 2 of the
19 are self-only (`Luz de Vela` Pessoal, `Corpo Fechado` Pessoal ou Toque). The other 17 land on
someone else — Toque (6), Adjacente (4), Alvo Único Distante (3), Área de Efeito (2). So the
effect sits on a *target's* `CombatantSheet` while the concentration is the *caster's* state, and
neither `TemporaryEffect` nor `TemporaryBonus` records who granted it.

There is a precedent for exactly this shape: `Scene.grantedBlessings` already holds a
grantor-to-granted-effects map and revokes by reference through `CombatantSheet#removeEffect`.
A caster's sheet holding the effects it currently sustains would mirror it — a known pattern
rather than a new mechanism.

**Concentração is never defined in any of the three source documents** — only used, as a Duração
value. The rule above (active while focused; broken by casting or attacking; trailing count
starts on the break) is carried-in ruleset knowledge, stated 2026-08-28.

And it is not perfectly uniform — four Magias attach their own clauses, so a single generic
mechanism will not cover everything:

- **L1410 `Solo Profano`** adds a restriction *while* concentrating: "você não pode se mover".
- **L1086 `Refúgio Invisível`** ends early on a **third party's** action — "se qualquer personagem
  em seu interior realizar ações ofensivas … a magia é encerrada imediatamente" — so the caster is
  not the only one who can break it. Its Duração is also `Concentração + Foco Horas`: derived from
  an Attribute *and* denominated in hours.
- **L1772 `Festim dos Mortos`** exempts its summons outright: they "não podem ter a Duração
  estendida por efeitos ou Concentração".
- **L940 `Raio Antivida`** binds both ends — caster and target share a condition "enquanto o
  conjurador se mantiver concentrado".

#### ⚠ Converting at authoring time silently breaks `Potencializar`

*Potencializar* is the most common Efeito Crítico in the game — 57 of 145 Magias — and it reads
"A duração da magia aumenta em +2d6 **unidades**", not *+2d6 Rodadas*. The unit is the Magia's
own. On a `1 minuto` Magia that is +2d6 **minutes**; stored as a bare `12`, it becomes +2d6
Rodadas instead — the right effect at **1/12th** the magnitude, on the six minute-and-hour
Magias.

So Rodadas is the correct canonical unit for arithmetic, but the authored unit is real data and
has to be kept alongside it. The same applies to one Efeito Alternativo (L1086) whose Duração is
`Concentração + Foco Horas` — derived from an Attribute *and* denominated in hours — and to the
Corrente *Duração Expandida* (L1988), which sets a duration **to** 1 hour rather than adding.

One Magia also carries a **second, independent duration field**: L1493 `Duração do Portal: 1
minuto`, alongside its own `Duração:`. A single `getDuration()` cannot hold both.

### `Alcance:` — four shapes `SpellTargeting` could not hold; three now do

The bulk always mapped cleanly (`Toque` 34, `Pessoal` 21, `Adjacente` 14, `Alvo Único – Distância
X` 18, `Área de Efeito – Área Circular X` ~15). Area *sizes* are named with the `Range` band words
(Muito Curta/Curta/Média/Longa), so `AreaOfEffect.circle(Range)` and its siblings resolve them with
no new vocabulary. The four exceptions resolved as follows:

- **Dual reach — `Pessoal ou Toque` (18×) and `Alvo Único – Toque ou Pessoal` (1×).** Closed by
  `Spell#getAlternateTargeting()`, a second whole `SpellTargeting` rather than a second reach
  inside one — a `SpellTargeting` pairs a reach with the parameters *that* reach takes, so
  splitting it would break the pairing. Exactly 19, pinned by `SpellCatalogTest`.
- **Two `AreaShape`s missing.** Closed: `PENETRANTE` (Ira de Vulcano's Semente and Broto) and
  `EXPLOSAO` (Voo em Massa). Penetrante is an emanation and so may carry no placement `Range`;
  Explosão is placeable like a Círculo.
- **Planar reach** — `Planos Elementais`, `Mesmo Plano` (1× each, both Transporte). Closed as a
  *classification only*: `SpellReach.PLANAR` takes no parameters, because there is no plane to
  name as a destination and no distance to state. Better than leaving two Magias with a `null`
  Alcance.
- **Foco-scaled footprints** (4×) — **still open**, and correctly so. "Área Circular Curta, aumenta
  para Média se tiver Foco 5 ou superior" depends on the caster's live Foco, and `AreaOfEffect`
  holds one fixed length. Each is authored at its base size with the growth clause kept in the
  Magia's prose. Closing it needs a footprint resolvable against a sheet, which is the missing
  Área de Efeito resolution rather than a missing column.
- **The referential (`O mesmo do ‘Solo Profano’`, 2×)** is restated literally rather than
  referenced. Unlike `SpellDuration`, `SpellTargeting` has no deferred form, and an enum constant
  may not read a sibling constant from its own arguments — see `ProfanarSpell`'s class javadoc.

One placement shape the original survey missed: **two Magias place an area's centre away from the
caster** (Festim dos Mortos at Adjacente, Paradoxo Temporal at Distância Média), which is the
`Range`-carrying form of `SpellTargeting.areaDeEfeito`. Every other area is centred on the caster.

### `Perícia Chave para Conjuração` is one field; `Spell` has two skill getters

The doc gives each Magia a single key Perícia — Domínio do Mana 109×, Ataque à Distância 23×,
Ataque Corpo-a-Corpo 11× — assigned by the rule at L47: Corpo-a-Corpo for Toque, à Distância for
Alvo Distante/Área, Domínio do Mana for Pessoal or unavoidable. That is the **delivery** roll,
i.e. `getAttackSkillType()`. `getConjurationSkillType()` has no separate column to read from;
under `SpellCastingService`'s two-roll model the second roll is always Domínio do Mana.

**Settled that way**: `getConjurationSkillType()` is a `default` returning `DOMINIO_DO_MANA` and no
Magia authors it, so the two getters are equal for the 109 whose key Perícia already is Domínio do
Mana and differ for the other 36. Pinned by `SpellCatalogTest`.

### `Tempo de Ativação` — now `ActivationTime`, and it is not an `int`

1PA (5×), 2PA (**58**×, not the 57 first counted here), 3PA (53×), 4PA (15×), 5PA (5×) — plus
**Reação (5×)** and **Ação Livre (4×)**, which are not a PA count at all, which is why the column
is an `ActivationTime` record over
an `ActivationType` rather than a number on `Spell`. The two non-PA counts are pinned by
`SpellCatalogTest`.

The conditional case — "2PA, pode ser conjurado como Reação por personagens com Domínio do Mana 5
ou superior ao custo de 3PM" (Aumentar Passos) — is authored as its plain 2PA with the condition
left in prose. Modelling it needs a second `ActivationTime` plus a Graduação threshold plus a PM
override, for exactly one consumer.

### `GD da Conjuração` — far more than 5 Magias are not a fixed tier

⚠️ **The original count here was badly wrong**, and authoring is what revealed it. Three separate
shapes, and the floor is not rare at all:

- **A floor against the target's DM — 49×, not 2.** Written either as `‹tier› ou DM do Alvo
  (maior)` (46×) or as a bare `DM do Alvo` with no tier at all (3×: Impor Arrepsia, Criar
  Distração, Lacerar a Alma). Both are `isCastingDifficultyFlooredByTargetMagicDefense()`; the bare
  form is that flag with a `null` tier, which reads correctly — a floor of nothing is always beaten
  by the target's DM. A third of the catalog, and whole trees lean on it (every rung of Ira de
  Vulcano but one).
- **A per-rung table — 3×, not 2.** Identificação, Remover Maldição **and Toque Curativo** scale
  their GD to the *target effect's* own rung: "Sementes: Fácil, Brotos: Médio, Mudas: Difícil,
  Emergentes: Muito Difícil, Florescentes: Improvável". That ladder is ordinal-aligned with
  `DifficultyLevel` starting at `EASY`, so it is one shared static (`Spell.castingDifficultyAgainst`)
  rather than three transcriptions. Remover Maldição carries an extra clause the table cannot hold
  ("Maldições provenientes de Habilidades Monstruosas ou Aventyrs são de Grau Muito Difícil"),
  which stays prose.
- **Conditioned on allegiance — 1×.** Rearranjo Corporal reads "Fácil (13|14) para alvos aliados ou
  pessoal, DM para alvo para inimigos", which is *not* a floor (a floor takes whichever is higher;
  this picks by side). Authored as the floor, which gives the same answer wherever a floor is asked
  about, with the exact clause on the constant. That makes **50** constants carrying the flag
  against 49 genuine floors in the document.
- The `Variável` counted here was Identificação's, which is the per-rung table's own label rather
  than a fourth shape.

None of the three has a reader: a foe's Defesa Mágica is an authored flat number and nothing
compares a roll against it. See `SpellCastingService`'s TODO.

#### ⚠️ The tier itself is not an independent column — it is a function of the rung

Of the 145 complete Magias, **134 state a plain tier, and all 134 follow one ladder with zero
deviations**:

| rung | GD |
| --- | --- |
| Semente | `Fácil (13\|14)` |
| Broto | `Médio (16\|18)` |
| Muda | `Difícil (20\|23)` |
| Emergente | `Muito Difícil (25\|28)` |
| Florescente | `Improvável (32\|36)` |

The other 11 are the 4 blanks, the 3 bare-`DM do Alvo`, the 3 per-rung tables and the 1
allegiance-conditioned entry. The `ou DM do Alvo (maior)` floor is **orthogonal** to this: all 46
of those still state their own rung's tier, and the floor only says what happens when the target's
DM is higher.

Three consequences worth carrying:

- **`Spell.castingDifficultyAgainst(BranchLevel)` already is this ladder.** It was written for the
  three dispelling Magias, whose GD scales to the rung of the effect they target; it turns out to
  be the general rule applied to a different rung, not a table peculiar to them.
- **The 4 blank-GD Magias are derivable, not unknowable** — Invocar Traje de Batalha and Proteção
  Primordial are Mudas (Difícil), Estalo Primordial and Aumentar Passos Sementes (Fácil). They are
  still authored as `null`, since deriving them in code is a separate decision; but "we cannot know
  what it should be" is no longer the reason.
- **Every one of the 44 Umbral Magias has a GD**, because every one has a rung. That removes the
  only universally blank column in that section.

## Source-document defects — do not "fix" silently

Line numbers are into `magias.txt`.

- **L51 `Tipos de Magias <Pendente>`** — section heading with only the *Magias de Invocação*
  subsection under it; the rest is unwritten.
- **Nine missing descriptors in the otherwise-complete first section.** Eight are printed with
  nothing after the colon; one has no line at all, which a "count the blanks" pass will not see —
  the section has 145 `Duração:`/`Alcance:`/`Perícia Chave:` lines but only **144** `Efeito
  Crítico:` ones.
  - `GD da Conjuração:` — L484 *Invocar Traje de Batalha*, L637 *Estalo Primordial*,
    L679 *Proteção Primordial*
  - `Efeito Crítico:` — L1588 *Necropotência*, L1617 *Reanimar*; and *Imbuir Fadiga* (Morte,
    Broto) has **no `Efeito Crítico:` line at all**, its block running straight from its Corrente
    to its Duração
  - `Perícia Chave para Conjuração:` — L1862 *Aumentar Passos*, L1912 *Lentidão*
  - L1863 *Aumentar Passos* is missing its `GD da Conjuração:` too
- **L3000 `Florescente – Barganha dos Dullahan` is a stray.** Unlike every other Magia in the
  Umbral section it carries no bullet prefix and no descriptor block at all — just a trailing
  `- Cóiste Bodhar` (the Dullahan's coach, in Irish myth). It duplicates Armada Decapitada's real
  Florescente at L2765 while sitting inside the Devorador de Mundos block. **This is why that
  section holds 44 Magias and not 45**: each of its six mechanical descriptors appears exactly 44
  times, and a rung-line count that does not require the bullet reads 45.
- **L3003–3052 duplicates two Umbral trees.** *Julgamento do Tecelão* and *Devorador de Mundos*
  each appear twice: an expanded descriptor block at L2809/L2904, and the designer's original
  shorthand outline at L3003/L3012 ("Semente – Caminhar do Tece-Mortes: movimento vertical +
  movimento aumentado…"). The outline is the earlier draft, left in place. Read L2809–3002 and
  ignore the tail — but note the outline carries more than extra detail. For *Julgamento do
  Tecelão* it carries **a whole extra Magia**: the outline lists a second Muda (`Casulo de Funeral
  Umbral`) that the expanded block has not got, and names the second Emergente `Aspecto da Aranha`
  where the expanded block says `Aspecto do Destino`. The expanded block's own rung shape — one
  Muda and two Emergentes — matches neither the outline nor any complete-section tree.
- **L2314** `GD da Conjuração: Improvável (32|36)e` — trailing stray `e`.
- **Spelling variants that break naive grouping**: `Domínio de Mana` (4×) for *Domínio do Mana*;
  `Ataque Corpo-a-corpo` (1×) for *Corpo-a-Corpo*; `Perícia Chave para conjuração` lowercase
  (7×); `Instantâneo` (12×) alongside `Instantânea` (26×); `Concentração +2 Rodadas` (3×) and
  `Concentração +2 Rodada` (1×) alongside `Concentração + 2 Rodadas` (5×).
- **One Corrente name runs on into prose** — `Corrente de Efeitos – Veneno Sombrio mesmo que não
  supere a DM do alvo em 5, também recebe adicionalmente a Corrente de Efeitos – Perdição
  Arcana`. Two Correntes and a condition on one line; a name-extracting parse reads it as a
  single 100-character name and loses *Perdição Arcana*.
- **L35** `PONTOS DE MANA (PM) PARA COMJURAÇÃO` — typo for *CONJURAÇÃO*.
- **L16** `odendo` — typo for *podendo*.
- **L49** dittography: "Algumas magias possuem também o descritor Corrente de Efeitos. Algumas
  magias possuem o descritor Corrente de Efeitos."

## Authoring status

**All 145 complete Magias are authored** (2026-08-29), across 20 Árvores. The 44 Umbral Magias
stay out until the *Força Umbral* Talento exists — see "Sombras da Umbra" above; their descriptors
are only partly blank, and the one column blank on all 44 is derivable from the rung.

Every prerequisite this section used to list is closed:

1. ~~Add `TEMPORAL` and `UMBRAL` to `MagicType`.~~ **Done**, with a `MagiaAlternativaAbility`
   constant each. `NATURAL` was kept as a top-level type *and* added to `ElementalType`, since the
   document uses it both ways and picking a side is a rules decision.
2. ~~Decide whether the Elemental subtype needs modeling.~~ **Done** — `ElementalType`, a column on
   `SpellTree`, carried by 11 of the 20 trees.
3. ~~Add the missing `CriticalEffectType` constants.~~ **Done (2026-08-28)** — all 23 Efeitos
   Críticos Ofensivos are constants. The 9 Defensivos are `DefensiveCriticalEffectType`.
4. ~~Keep the authored Duração unit beside the count.~~ **Done** — `SpellDuration` holds a count, a
   `DurationUnit`, a `concentration` flag and a `DurationKind`, so `Potencializar`'s "+2d6
   unidades" lands in the Magia's own unit.
5. ~~Add a `Tempo de Ativação` column.~~ **Done** — `ActivationTime`, with its Reação and Ação
   Livre cases as an `ActivationType` rather than a PA count.
6. ~~Settle `getConjurationSkillType()` vs `getAttackSkillType()`.~~ **Done** — the document's one
   field is the delivery roll; the conjuração roll defaults to Domínio do Mana and no Magia
   authors it.
7. ~~Add a per-Magia `Corrente de Efeitos` field.~~ **Done** — `getEffectChainDescription()`,
   prose.

### What authoring bought, and what it did not

The catalog is **complete, exact, and almost entirely inert**. Every column is real authored data;
almost none of it has a consumer. That is the deliberate "can't apply it yet doesn't mean can't
compute it yet" position, but it is worth being precise about which half is which.

Real today: the rung and its PM cost, the acquisition gates (`Spell#isEligible` runs against real
trees now), the category tags, and every descriptor as data a UI can print.

Blocked, with the gap named per constant: essentially every *effect*. The recurring blockers, in
rough order of how many Magias they hold up:

| blocker | Magias affected |
| --- | --- |
| No damage-type breakdown or elemental resistance/immunity | most of the 11 Elemental trees |
| Área de Efeito footprint resolution | every area Magia (~35) |
| Round-scoped Attribute bonuses | the whole Polimorfismo tree |
| Malefício classification (Maldição/Doença/Possessão) | Morte and Vida's Alternativo branch |
| Owned/produced item copy | the whole Artesão tree |
| Concentração's two-phase transition | 20 Magias — see above |
| Pontos de Ação ceilings and non-zero floors | the whole Tempo tree |
| Visibility / detection | the whole Ocultação tree |
| Flight and vertical movement as a sub-stat | the whole Voo tree |
| A Magia-to-`MonsterTemplate` link for Invocações | Aliados da Natureza, Reanimar, Transporte |
| The target's Defesa Mágica as a comparable number | all 49 GD floors |

Two of those are closer than they look. `Zumbi` already **is** Reanimar's stat block, as a real
`SummonedMonsterTemplate` — only the link from the Magia is missing. And the Concentração
mechanism needs exactly two things, both narrow: a way to move a `TemporaryEffect`'s
`remainingRounds` from `null` to a count when focus breaks, and a caster-to-sustained-effects map
(`Scene.grantedBlessings` is the precedent) for the 17 that land on another sheet.

### Branch roles are a reading, and four trees are close calls

`MagicBranch` names its 36 ramificações by **role** (`PRINCIPAL`/`ALTERNATIVO`) rather than
inventing names, because the document names none — but it does state what the two always are
(L30: "Um deles aprofunda o efeito principal da magia, enquanto outro foca na evolução dos Efeitos
Alternativos").

Assigning which entry holds which role is the one place this catalog contains judgement rather than
transcription. Each constant records its own trace. Worth knowing:

- **The document's printed order carries no meaning.** In 14 of 18 diverging trees the first-listed
  entry at a rung is also the one deepening the principal effect; in **Morte, Piromancia, Reanimar
  and Voo** it is the second. That 4-of-18 split is itself the evidence.
- **Six trees have no `Efeito Alternativo` before their divergence at all** — Corpo Rochoso,
  Dominio Primordial, Fúria de Tesla, Morte, Piromancia and Proteção Invernal — so L30's second
  half has nothing to point at and the trace runs on the principal effect alone.
- **Tempo and Regeneração are the clean cases** — Tempo's Semente grants +2UD and its Efeito
  Alternativo takes 2UD away, one per branch; Regeneração's two branches are named for the two
  deities its trunk Magia and its Efeito Alternativo honour (Undine / Haloi).
- **Polimorfismo is close to a coin flip** and says so: its Semente grants Vantagem *or*
  Desvantagem symmetrically, and neither branch touches its Efeito Alternativo.
- **Ocultação breaks the rule outright**: the Magia that evolves its Broto's Efeito Alternativo
  (Campo de Invisibilidade, from *Ocultação em Massa*) sits on the branch traced as PRINCIPAL.
- **Piromancia is the one tree whose branches effectively have names** — every Magia on each path
  carries a deity's (Eldur / Boros), so those two constants override their role label.
