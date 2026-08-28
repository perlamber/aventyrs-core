# Talentos — source index

Source: `ok V19 Talentos - 20250814 (2).docx` (2025-08-14), converted verbatim to
`talentos.txt` with `textutil -convert txt`. ~320 Talentos across three divisions.

The **category tag in parentheses after each Talento's name is authoritative**, not the
section heading it is printed under — several trees are scattered across the document
(`Bruto` appears under Assassino, Mobilidade, Anão, Górgona, Monstruoso and Órquico
headings). Grep by tag, not by page.

## Coverage against `org.aventyrs.core.feat`

| tree | in doc | authored | class |
| --- | --- | --- | --- |
| Metamágico | 15 | 13 | `MetamagicoFeat` — missing *Armazenar Magia Superior*, *Aptidão Mágica Dracônica* |
| Arte Marcial | 8 | 1 | `ArtesMarciaisFeat` — only `ARTISTA_MARCIAL` |
| every other tree | ~297 | 0 | — |

`FeatCategory` today has 15 constants; the document names **47** distinct trees.

## Trees, by division

Counts are of Talentos carrying that tag; a multi-tagged Talento is counted under each.

**Gerais (169)** — Duelista 19, Perito 18, Destino 16, Metamágico 15, Sobrevivência 14,
Escudeiro 13, Assassino 12, Artilharia 10, Mobilidade 9, Arte Marcial 8, Arcano 6,
Cavalaria 5, Regalia 5, Bruto 4, Devoto 4, Artífice 3, Especialista 2, Abençoado 1.

**Raciais (130)** — Feérico 13, Monstruoso 12, Vampírico 11, Bestial 9, Elemental 9,
Élfico 7, Górgona 7, Tellus 7, Feral 6, Anão 5, Dracônico 5, Troll 5, Ôgrico 4,
Pequenino 4, Aviano 3, Gnomo 3, plus singles/pairs (Goblin, Indômito, Fadas, Fúrias,
Mestiço, and cross-tagged Destino/Sobrevivência/Perito/Bruto/Escudeiro/Especialista/
Abençoado entries).

**Devoto (16)** — Tellus 12 (one per divindade), Devoto 4 (*Palavras de Poder*).

## Source-document defects — do not "fix" silently

- L635 `Se Mover e Atacar (Mobilidade)` — its `Descrição:` line reads `Talento 'Esquiva'`,
  i.e. a prerequisite mislabelled as the description. The real description is missing.
- L1848 `TALENTOS REGIONAIS (em produção)` — section is an empty stub.
- L2103 `Palavras de Poder: Verbum Draconum` — marked `<Em produção>`.
- Five Talentos carry no `Pré-requisito` line at all (Acerto Crítico Aprimorado,
  Coração de Ferro, Esquiva, Iniciativa Aprimorada, Se Mover e Atacar) — read as
  genuinely unrestricted, i.e. an empty `FeatRequirements`.

## Scope decisions (2026-08-28)

- **`Aventyr` is a tier gate, not a tree.** All 110 Aventyr-tagged Talentos gate on
  "N Títulos Aventyr Despertos", where N is 1 (94×) or 2 (7×). *Desperto* means the Título
  occupies one of `Character`'s three slots — confirming the reading `ArtesMarciaisFeat`,
  `MetamagicoFeat#MENTE_EXPANDIDA` and `InstinctAbility` already use, and closing the open
  question flagged in `Orc`'s javadoc. No Talento in this document gates on a Suprema
  Habilidade, so no such field was added.
- **`Bruto`/`Arcano`/`Abençoado`/`Especialista` are Título Aventyr archetypes**, not feat
  trees — the four *Centelha Aventyr* Talentos make this explicit, each requiring "1 Título
  Aventyr <archetype> Desperto". Modeled as `TitleArchetype` + `requiredTitleArchetype`.
- **`Tellus` is the setting**, not a tree.
- **A tag naming a Título is a gate, not a tree.** `Reconhecer suas Presas (Aventyr/Indômito/
  Gigante Enfurecido)` — *Gigante Enfurecido* is a Título Aventyr, and its Pré-requisito reads
  "o Título Aventyr Gigante Enfurecido **ou** raça Indômito", an **or**, so *Indômito* does not
  restrict the Talento either and cannot be its tree. It is an `AVENTYR` Talento.
- **`AVENTYR` is therefore a category as well as a tier** — 19 Talentos carry the `Aventyr` tag
  and no tree of their own. A Talento tagged `Aventyr` *and* a real tree belongs to that tree.
- **Which tag is the tree: racial beats general.** A Talento printed under TALENTOS RACIAIS is a
  racial Talento; a general tag alongside it (`Anão/Sobrevivência`) is supplementary. `Sátiro`
  is the one race tag that is never a tree — it always accompanies `Feérico`.
- **`FeatCategory` stays single-valued.** Once the tags above are reclassified, only ~13
  Talentos carry two genuine categories, and ~10 of those pair one general category with one
  race (`Anão/Sobrevivência`) — modeled as the general category plus `requiredRace`, matching
  their own "Apenas personagens da Raça X" prerequisite text. The remaining 4 keep their
  primary tag, with the secondary noted in the description.

### Excluded from authoring

- **Talentos de Devoção (20)** — every `Devoto`-tagged Talento. Their effect is split across
  three escalating devotion tiers (Adepto / Fiel / Fundamentalista) that `Character` has no
  field for; this is a second progression system, not a feat effect. Revisit once devotion
  exists.
- **Regional Talentos (7)** — the `Tellus/Sobrevivência` entries under `TALENTOS REGIONAIS
  (em produção)`. Empty templates in the source: no prerequisite, no description.

**Authorable total: 305**, across 37 categories.

## Authoring status — general trees (2026-08-28)

All general trees are authored except **Regalia** (5) and **Aventyr** (19), both deferred at the
user's request, and the 20 Talentos de Devoção excluded above.

| tree | constants | tree | constants |
| --- | --- | --- | --- |
| `DuelistaFeat` | 19 | `EscudeiroFeat` | 13 |
| `DestinoFeat` | 17 | `SobrevivenciaFeat` | 12 |
| `PeritoFeat` | 18 | `ArtilhariaFeat` | 10 |
| `AssassinoFeat` | 16 | `ArtesMarciaisFeat` | 8 |
| `MetamagicoFeat` | 15 | `CavalariaFeat` | 5 |
| `MobilidadeFeat` | 14 | `ArtificeFeat` | 3 |

**150 Talentos.** Every one carries its rules text verbatim and enforced Pré-requisitos; the
handful with working effects are listed in `GeneralFeatEffectIntegrationTest`.

### Clause shapes `FeatRequirements` still cannot express

Each is recorded on the constants it affects, and each makes that Talento's gate *looser* than
the rules text — never stricter.

- **Disjunctions** — "Destreza 3 e Saque Rápido, **ou** Foco 5". Every set clause combines with
  and. Eight constants; each records one branch, so the other route is wrongly refused.
- **Two required Talentos** — `requiredFeat` is singular. Four constants.
- **A required `SkillSpecialization`** — `requiredSkillCompetencyAbility` has no twin. Five.
- **Attribute *maximums*** — "Força igual ou inferior à 2", "Iniciativa 2 ou inferior". Three.
- **`CharacterSheet`-side values** — Fama, EXP total. `Feat#isEligible` takes only a `Character`.
- **"Any Attribute at N"**, with no particular domain named. Three.
- **Exclusions** — "nenhum outro Talento Dominar Arte Marcial". Seven.
