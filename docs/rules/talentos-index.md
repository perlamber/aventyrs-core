# Talentos — source index

Source: `ok V19 Talentos - 20250814 (2).docx` (2025-08-14), converted verbatim to
`talentos.txt` with `textutil -convert txt`. ~320 Talentos across three divisions.

The **category tag in parentheses after each Talento's name is authoritative**, not the
section heading it is printed under — several trees are scattered across the document
(`Bruto` appears under Assassino, Mobilidade, Anão, Górgona, Monstruoso and Órquico
headings). Grep by tag, not by page.

## Coverage against `org.aventyrs.core.feat`

See the two authoring-status sections at the end of this file: all 12 general trees are
authored (150 Talentos), and the racial trees are being authored in batches.

`FeatCategory` has 37 constants — every tree this document authorises. The document names 47
distinct tags, of which 10 turned out not to be trees at all (see *Scope decisions*).

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
- `Poder Vampírico – Abominação` — its entire `Descrição:` line is the single
  character `V`. Transcribed as a named placeholder in `VampiricoFeat#ABOMINACAO`.
- `Escudo Que Anda (Escudeiro/Gigante/Aventyr)` — its `Descrição:` line opens with the bare text
  `Talento Zelo pelos Frágeis`, a prerequisite that slipped into the description field. Same
  defect as L635; read as a prerequisite.
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

## Authoring status — racial trees (2026-08-29)

Authored in batches. Racial Talentos are far more gap-blocked than general ones: most extend a
Característica Racial that is itself unbuilt, so a tree's constants are usually catalog entries
with enforced Pré-requisitos and a named blocker.

| batch | tree | constants | real clauses |
| --- | --- | --- | --- |
| 1 | `AnaoFeat` | 5 | Filho de Ymir (PV mult. + Dano Base), Vigor do Inverno (PV mult.), Vantagem de Tamanho + Glória Ymiriana (opposed-combatant, see below) |
| 1 | `AvianoFeat` | 4 | Visão da Verdade (−1 nível de GD em Atenção) |
| 2 | `DraconicoFeat` | 5 | Asas de Dragão (+2 Defesas) |
| 2 | `OgricoFeat` | 4 | none — every constant extends the unbuilt Bocarra |
| 2 | `OrquicoFeat` | 5 | Terra nas Veias (PV mult. per Título), Tremor's Efeito Passivo (PV mult.) |
| 3 | `TrollFeat` | 5 | none — every constant extends the unbuilt Regeneração Reativa or Sono de Pedra |
| 3 | `GnomoFeat` | 4 | Duende (+1 DM), Favoritos de Tesla (−1 nível de GD em Profissão) |
| 3 | `GoblinFeat` | 2 | none — both GD clauses are purpose-scoped, and both Dureza clauses set the value at *creation*, which needs a production mechanic (Dureza itself is damageable now) |
| 3 | `HumanoFeat` | 3 | none — all three extend the unbuilt Aprendizado Rápido |
| 3 | `MesticoFeat` | 1 | none — the inherited-Característica cap is fixed at construction |
| 3 | `IndomitoFeat` | 1 | none — Ferocidade de Lacerto is unbuilt |
| 4 | `BestialFeat` | 9 | none — every Herança is built from four systemically blocked clauses |
| 4 | `FeralFeat` | 6 | none — three grant Atributos, three reshape the unbuilt Forma Híbrida |
| 5 | `ElementalFeat` | 9 | none — the whole tree waits on Resistência/Vulnerabilidade Elemental |
| 5 | `GiganteFeat` | 4 | none — two withheld whole rather than half-implemented (see below) |
| 5 | `PequeninoFeat` | 4 | none — two change Tipo de Personagem, which cannot vary per character |
| 6 | `ElficoFeat` | 7 | Guardião dos Bosques + das Dunas (terrain Vantagem), Sentidos Absolutos (−1 nível de GD em Atenção) |
| 6 | `FeericoFeat` | 12 | Ninfa, Sirenídeo, Fauno, Lupercal — all unconditional Vantagem on named Perícias |
| 6 | `FadasFeat` | 1 | none — no cost step in `SpellCastingService` to discount |
| 6 | `FuriasFeat` | 1 | none — same blocker as its Fada twin |
| 7 | `GorgonaFeat` | 7 | Proteção do Deus dos Monstros + da Rainha das Fadas (RD) |
| 7 | `MonstruosoFeat` | 12 | Pele Rija (+2 DF and +2 RD), Ossos Ocos (+1UD Movimento, −1 PV mult.) |
| 8 | `VampiricoFeat` | 11 | none — no Vampiro race exists, and every Poder is temporary |

**The racial catalog is complete: 122 Talentos across 23 trees.** Combined with the 150 general
Talentos, `FeatCatalog` now holds **272** — the whole authorable ruleset bar Regalia (5), Aventyr
(19) and the 20 Talentos de Devoção, all deferred above.

**14 constants carry live effects.** They cluster in Élfico, Feérico, Monstruoso and Górgona;
batches 3–5 produced 2 between them, because those trees mostly extend a Característica Racial
that is itself unbuilt.

### Vampírico is the one tree with no race behind it

All 11 constants read "Apenas personagens da Raça Vampiro", and `org.aventyrs.core.race` has no
such class — the race lives in a section of the ruleset this core has not transcribed. So the
whole tree is **ungated**, far looser than any other. `requiredCreatureType(MONSTRUOSO)` was
deliberately *not* used as a stand-in: it would open these Talentos to every Troll, Goblin and
Ogro. `RacialFeatEffectIntegrationTest#everyVampiricoTalentoIsUngatedBecauseNoVampiroRaceExists`
pins this, and is written to fail the day the race lands so the clauses get tightened.

A second reading holds that tree together: **a "Poder Vampírico" is an activated power with a
Duração**, evidenced by `PODER_VAMPIRICO_DURADOURO` existing solely to extend it and by
`CELERIDADE_VAMPIRICA` granting its PA "temporariamente". That is an inference — the source never
prints an activation cost or base Duração — and it is what keeps six otherwise-implementable
clauses (notably Osteomancia's "+2 em suas Defesas") from being granted as permanent bonuses.
Re-check it first if the tree is revisited. Effects are pinned in `RacialFeatEffectIntegrationTest`.

### Two mechanisms the racial trees earned

- **`Feat#resolveDifficultyReduction(SkillType, Character)`** — summed by
  `AbstractSkillInteraction#applyTo` alongside the `SkillCompetencyAbility`/`SkillExcellency`
  scans it already ran. "A Feat cannot reduce a roll's GD" was already a recorded TODO on four
  general trees; three racial trees state the plain unconditional form on a named Perícia, which
  is what made the hook worth building. Only for that shape — a GD clause scoped to a narrative
  purpose, bought with a resource, or on a Conjuração roll still does not qualify.
- **`FeatRequirements#requiredDeity`** — "Apenas personagens Orcs Devotos de Epona". `Deity` and
  `Character#getDeity()` both already existed, so the gate is enforced rather than commented.
  It tests devotion alone, not the Adepto/Fiel/Fundamentalista *tier* the excluded Talentos de
  Devoção are split across.

### A third convention: when a malus *is* applied

Batch 5 established that a Talento is never half-implemented when the missing half is the bonus.
Batch 7 shows the other side: `MonstruosoFeat#OSSOS_OCOS` applies a real −1 Multiplicador de PV,
because the trade its rules text frames — lighter, therefore faster, therefore frailer — is
expressible on *both* sides (+1UD Movimento against −1 PV). A third clause of that Talento (a
Destreza-scoped Vantagem) is still missing, but the framed trade stands on its own. Contrast
`GiganteFeat`'s two Clã Talentos and `GorgonaFeat#CABELO_SERPENTINO`, withheld whole because only
their malus could be expressed.

The same asymmetry decides two Dano Base clauses. `AnaoFeat#FILHO_DE_YMIR` grants "+1 Dano Base de
armas" with its over-grant documented, because the only excluded case is an Ataque Desarmado;
`MonstruosoFeat#SELVAGERIA` is withheld, because its "Armas Naturais" scope would over-grant to
every weapon the holder ever wields. Same hook, opposite direction of error.

### Recurring racial blockers

Most constants cite one of these rather than a novel gap:

- **No Arma Natural** — no weapon catalog is authored, and nothing marks a weapon as natural.
  Blocks all of Dracônico's repertoire and every Herança Bestial.
- **No flight state** — no Movimento Base de Voo distinct from the ordinary one. Blocks three of
  four Aviano Talentos outright.
- **No form/transformation state** — blocks Draconato, Metamorfose Selvagem, the Homem-Fera line.
- **Bocarra / Devorar Inteiro** — blocks the whole Ôgrico tree.
- **No Atributo grant from a Talento** — `Race#getFixedAttributeBonuses()` is the only racial
  bonus hook, and nothing reads a `Feat` for one. Blocks every "+1 Bônus Racial em X" clause.
- **No `Feat` dano-bonus hook** — `resolveDamageBonus` lives on `SkillCompetencyAbility`/
  `EgoAdvantage`, both reached through a skill Interaction rather than `character.getFeats()`.
- **No extra acquisition slot** — "você aprende uma Habilidade de Competência" has no shape;
  `AttributeAbilityService#getUnlockedAbilitySlots` counts slots from a raw Atributo base. The
  single most-cited blocker of the racial catalog.
- **Aprendizado Rápido is unbuilt** — blocks the whole Humano tree plus `GnomoFeat#SABICHAO`.
- **Resistência / Vulnerabilidade Elemental** — blocks all 9 `ElementalFeat` constants outright,
  plus clauses in Dracônico, Troll and the Guampo/Nascido do Dragão races. `DamageType` has no
  elemental breakdown feeding RD/RA, nothing nullifies a damage type, nothing amplifies one.
  **The single highest-value mechanism outstanding for this catalog.**
- **The six Mestiços Elementais record no element.** Both Gana Elemental and Resistência
  Elemental print a RAÇA→ELEMENTO table, and three Talentos key off "seu elemento", but no
  `AbstractMesticoRace` subclass carries an `ElementalType` — only `NascidoDoDragao` does. A
  missing *column on the races*, and the first thing to add before the Elemental tree can work.
  The tables also name an "Elemental da Madeira" with no race class at all.
- **A per-character `CreatureType`** — `Race#getCreatureType()` takes no `Character`, so a type
  that changes with what its holder acquired is inexpressible. Blocks both `PequeninoFeat`
  Linhagem Talentos and `Indomito`'s own Monstros em Potencial.
- **Movimento de Natação / Voo / Vertical is a different sub-stat** from Movimento Base, and
  deliberately not wired into `ModifierType#MOVEMENT` (see `AtletismoCompetencyAbility
  #ALPINISTA_VELOZ`/`ANFIBIO`). Routing a swim or flight clause there would raise ground movement.

### Two conventions applied throughout

- **The `Aventyr` tag means `requiredAwakenedTitles(1)`** unless the Pré-requisito line states a
  different number, even where that line omits the Título clause entirely (`FeralFeat
  #TRANSFORMACAO_RAPIDA`, `PequeninoFeat#LINHAGEM_DE_FLORA`). This follows the scope decision
  above; the omission is read as a source slip, not as an ungated Talento.
- **A Talento is never half-implemented when the missing half is the *bonus*.** `GiganteFeat
  #GIGANTE_DO_CLA_EMPUSA`'s "-2 em suas Defesas" is expressible today, but its paired Categoria
  de Tamanho and Atributo bonuses are not — granting only the malus would leave a character
  strictly worse off for acquiring the Talento, which is further from the text than granting
  nothing. Pinned in `RacialFeatEffectIntegrationTest`.

### Mechanisms the racial catalog has earned so far

- **`Feat#resolveDifficultyReduction(SkillType, Character)`** (batch 1) — three live consumers:
  `AvianoFeat#VISAO_DA_VERDADE`, `GnomoFeat#FAVORITOS_DE_TESLA`, `ElficoFeat#SENTIDOS_ABSOLUTOS`.
- **`Feat#resolveSkillRollBonus(SkillType, SceneContext, SkillTrait, Character)`** (batch 6) —
  six live consumers across two trees. The four Guardiões Élficos earned it: each grants Vantagem
  on the same four Perícia scopes while in its own environment, and two of those environments are
  exactly a `TerrainType`. Its four parameters mirror `SkillCompetencyAbility
  #resolveConditionalRollBonus` because the clauses are the same shape — including the
  "Conhecimentos: Natureza" scope, resolved through `requestedAbility` exactly as
  `AnoesRacialAbility#FILHOS_DA_MONTANHA` resolves the identical clause.
- **`FeatRequirements#requiredDeity`** (batch 2) — two Órquico Talentos.
- **`Feat#resolveDamageReduction(Character)`** (batch 7) — four live consumers:
  `MonstruosoFeat#PELE_RIJA`, both `GorgonaFeat` Proteções, and `ElementalFeat
  #TRANSFORMACAO_ELEMENTAL`, which was authored TODO'd in batch 5 and wired up retroactively when
  the hook landed. One consumption point in `DamageServiceImpl`. Unconditional grants only —
  every other RD clause in the catalog is gated on current PV, a form, an active effect, or which
  attack of the Rodada it is.
- **`FeatRequirements#requiredCreatureType`** (batch 6) — "Apenas personagens de raça Feérica"
  spans five race classes with no common supertype, so `requiredRace` cannot express it. Six
  consumers in `FeericoFeat` alone, and Monstruoso will add more.

### `SceneContext#getOpposedCharacter()` — the clause-shape unlock

The combatant on the other side of the roll: the **target** on a Perícia de Ataque, the
**attacker** on an Esquiva e Aparar roll, disambiguated by `SkillType#isAttackSkill()`. One
exchange has one opponent, so a single reference describes it from either side.

This is what makes a clause conditioned on *who is opposite* expressible from a `Feat`, which
carries no per-roll parameters of its own. `AnaoFeat` is the first tree rebuilt on it, taking it
from two live constants to four:

- **`VANTAGEM_DE_TAMANHO`** — +½ Vigor to Defesas, only against a larger attacker.
- **`GLORIA_YMIRIANA`** — Vantagem on either Perícia de Ataque against a target that is not
  smaller, plus +2 Margem Crítica Menor against one that is larger (a stricter test than the
  first half).

Two further hooks fell out, both with a single consumption point:

- **`Feat#resolveDefenseBonus(DefenseType, Character, SceneContext)`** — a **defaulting**
  relationship, not a cascade: the longer form falls through to the shorter one, so the four
  constants already overriding the unconditional form are untouched. Getting that direction wrong
  would silently zero `ARCANISTA`, `DUENDE`, `ASAS_DE_DRAGAO` and `PELE_RIJA`. Same direction
  `SkillCompetencyAbility#resolveSubstituteAttributeDomain(AttackSource)` uses.
- **`Feat#resolveCriticalMarginIncrease(SkillType, SceneContext, Character)`** — the `Feat`
  counterpart of the hook already on `AttributeAbility`/`SkillCompetencyAbility`/`EgoAdvantage`.

**Bonuses stack.** `GLORIA_YMIRIANA` and `AnoesRacialAbility#ABATEDORES_DE_GIGANTES` both apply
against a target 2+ Categorias larger, and the roll gets both — Talentos and Habilidades Raciais
accumulate as the character progresses, and no hook anywhere suppresses another.

Many constants across other trees are now unblocked by the same field — anything scoped to "contra
alvos X", "efetuadas contra Personagens de Tendência Y", or "quando roladas contra outros
Vampiros". Each still needs revisiting individually.

### One hook considered and deliberately not built

- **`Feat#resolveAttributeBonus`** — cited by 9+ constants across five trees ("+1 Bônus Racial em
  X"), and the most-wanted hook in the racial catalog. **Not batch-sized**: unlike
  `resolveDifficultyReduction`, which had exactly one consumption point, an Attribute total is
  read at **27 call sites across 9 files** with no chokepoint. Building it means first
  introducing something like `CharacterAttributeService#getTotalAttribute(Character,
  AttributeDomain)` and routing every reader through it; done piecemeal, the bonus would silently
  apply to rolls but not to PV, or to PM but not to the Graduação cap. Its own piece of work.
  **Built in batch 6** — see above. `BestialFeat#HERANCA_CANINA` remains withheld even so: its
  Faro Apurado is purpose-scoped ("a partir do olfato"), and the hook is for scopes this core can
  actually express.

### Three clause shapes worth knowing before the next batch

- **A purpose-scoped GD reduction is refused, a blanket one is granted.** `GnomoFeat
  #FAVORITOS_DE_TESLA` and `GoblinFeat#ENGENHEIRO_DE_IMPROVISOS` both take −1 nível off a
  Profissão roll; only the first is real, because the second is scoped to "para criar
  equipamento" and this core does not track what a roll is *for*. Pinned in
  `RacialFeatEffectIntegrationTest`.
- **"Recém-criado" is dropped.** Four Talentos so far restrict themselves to a character at
  creation. Nothing records when a Talento was acquired, so only the race clause is enforced.
- **"Apenas personagens Mestiços" is inexpressible.** `requiredRace` is a `Class` tested with
  `isInstance`, and there is no common Mestiço supertype — `AbstractMesticoRace` covers only the
  six Elementais, while `MeioElfo`/`NascidoDoDragao` implement `Race` directly. One Talento needs
  it, short of the second-consumer bar for a `requiredMestico` flag.
