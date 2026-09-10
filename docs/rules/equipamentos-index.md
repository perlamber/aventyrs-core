# Equipamentos — source index

Source: `V19 Equipamentos - 20250514.docx` (2025-05-14), converted to `equipamentos.txt` with a
custom docx parser (`textutil` and `python-docx` both mangled the tables). Each item's stat
table is rendered pipe-separated with its header row kept:

```
Armadura Completa (Pesado/Raro)
Descrição:
Favor: Dano de Corte sofrido é reduzido em -2.
Preço | DF | DM | Dureza | Conjuração | Requisitos
20 | +5 | +2 | 32 | Desvantagem | Força 3
Efeitos Adicionais: Nenhum.
```

**Read with [`efeitos-criticos-index.md`](efeitos-criticos-index.md).** Every Arma's *Efeito
Crítico* column names an entry from that document's **Ofensivos** list, usually paired with a
Margem Crítica Menor in parentheses — `Sangramento (17)`, `Projétil (16)`. The *Efeito Crítico
Defensivo* that `ArmorItem` carries per Armadura is **not in this docx** — it comes from an
"Atualizando os Equipamentos Defensivos" table that lives elsewhere in the ruleset.

## What the document contains

**200 entries in five sections**, of which two trailing sections are empty stubs.

| section | subsection | n | state |
| --- | --- | --- | --- |
| **Equipamentos Comuns — Defensivos** | Armaduras | 8 | complete |
| | Botas | 6 | complete |
| | Capas | 7 | complete |
| | Escudos | 6 | complete |
| | Protetores de Cabeça | 7 | complete |
| **Equipamentos Comuns — Ofensivos (Armas)** | Arco | 3 | complete |
| | Arremesso | 4 | complete (2 defective — see below) |
| | Balestra | 3 | complete |
| | Chicote | 4 | complete |
| | Clava | 5 | complete |
| | Lâmina Leve | 6 | complete |
| | Lâmina Pesada | 6 | complete |
| | Lança | 5 | complete |
| | Fortalecimento | 9 | complete |
| | Artefatos de Conjuração | 9 | complete |
| | Projéteis | 6 | complete |
| | Próteses | 0 | **`<em produção>` stub** |
| **Equipamentos Naturais** | Armas Naturais | 7 | complete |
| | Defesas Naturais | 5 | complete (different column set) |
| **Itens Obras-Primas** | Obras-Primas Ofensivas | 17 | complete |
| | Aprimoramentos de Obras-Primas Ofensivas | 18 | complete |
| | Obras-Primas Defensivas | 15 | complete |
| | Aprimoramentos de Obras-Primas Defensivas | 17 | complete |
| **Pedras do Poder** | Tipos de Pedras do Poder | 17 | complete (3-mode effect) |
| | Qualidades / Raridades / Preço | 4 | complete |
| | Obras-Primas de Pedras do Poder | 5 | complete |
| | Aprimoramentos de Obras-Primas (Pedra) | 1 | complete |
| **Equipamentos Tecnológicos** | Ark'ano / Vapor / Xayah / Núcleos / Obras-Primas / Aprimoramentos | 0 | **`<em produção>` stub** |

- **Base equipment: 106** (34 Defensivos + 60 Armas + 12 Naturais).
- **Obra-Prima layer: 67** (32 Obras-Primas + 35 Aprimoramentos, two overlapping name-spaces).
- **Pedras do Poder: 27.**

## The column set is not uniform

- **Armaduras / Botas / Capas / Escudos / Protetores** — `Preço | DF | DM | Dureza | Conjuração
  | Requisitos`, plus `Descrição`, `Favor`, `Efeitos Adicionais`. This is exactly `ArmorItem`'s
  ten columns.
- **Armas** — `Preço` and `Dureza` are printed **inline on the title line** (`Arco Composto
  (Médio/Raro) | Preço 17 | Dureza`), Dureza usually blank. The table row is `Dano | Tipo |
  Efeito Crítico | Alcance | Requisitos`. `Dano` is a `DamageBase` figure (`1d6+2`, `2d6`);
  `Tipo` is Corte/Perfuração/Esmagamento/Elemental/Variável; `Alcance` is a `Range` band or
  `Adjacente`/`+1UD`.
- **Fortalecimento** weapons list `+1` as their Dano (they *raise* an existing Ataque
  Desarmado / Arma Natural's Dano Base rather than having their own) and each carries a
  "Considerado Ataque Desarmado / Arma Natural para efeitos de Talentos e Habilidades" line and
  often "ocupa o espaço de um item do tipo Bota".
- **Artefatos de Conjuração** are filed under Armas but several (`Grimório`, `Varinha`, `Bola de
  Crista`) have `-` for Dano/Tipo/Efeito/Alcance — they are pure casting foci.
- **Defesas Naturais** drop Preço and Dureza entirely: `DF | DM | Conjuração | Requisitos`.
- **Obras-Primas / Aprimoramentos** use `DF | DM | Ataque | Danos | Conjuração | Requisitos`
  (Aprimoramentos omit Requisitos), plus `Favor` and `Características Adicionais`. These are
  *deltas* applied to a base item, not standalone items.
- **Pedras do Poder** have no stat table at all: three prose modes (`Efeito Base` /
  `Equipamento Defensivo` / `Equipamento Ofensivo`) and a separate 4-row rarity table
  (`Preço | Cargas | Danos | Resfriamento | Duração do Efeito`).

## Source-document defects — do not "fix" silently

- **`Zarabatana` / `Zarabatana de Caça`** (Arremesso) — the whole stat row reads `Projétil |
  Projétil | Projétil | Médio` instead of numbers. The real figures are only in the prose: the
  dart's Dano Base rises +1 / +2 and the Distância Base becomes Longa / Muito Longa.
- **`Bola de Crista`** — name is truncated (Bola de Cristal), and its entire stat row is
  `- | - | - | -`.
- **`Grimório`** — Dano/Tipo/Alcance are `-` but the Efeito Crítico column still reads
  `Atordoante (17)`; treat as a casting focus with no attack.
- **Two overlapping Aprimoramento name-spaces.** `Oculta`, `Encaixe`, `Benção de Proteção`,
  `Benção Elemental`, `Benção Elduriana`, `Benção Vulcana`, `Benção Ymiriana` each appear in
  **both** the Ofensivo and Defensivo Aprimoramento lists with *different* Favor/Características.
  The name alone is not a key — the list it is printed under is. Same for the "Material
  Especial – …" Obras-Primas (Dyospiros, Gelo Verdadeiro, Mitral, Adamantina, Couro de Dragão,
  Espírito Umbral, Couro/Ossos de Monstro), which recur across the Ofensivo and Defensivo
  Obra-Prima lists.
- **`Benção de Proteção` (Aprimoramento Defensivo)** — its Favor text says "duas primeiras
  Rodada**s**" but `DefensiveImprovement.BENCAO_DE_PROTECAO` is coded to 3 (`_INITIAL_LAST_ROUND
  = 3`), matching an earlier revision of the clause. Flagged, not reconciled.
- **`Próteses` and every `Equipamentos Tecnológicos` heading** — empty `<em produção>` stubs,
  exactly like `talentos.txt`'s Regionais.
- **Combined weapon names** — `Adaga, Kunai ou Seax`, `Espada Longa ou Katana`,
  `Maça ou Mangual`. One catalog entry, several in-world names, like `talentos.txt`'s style.

## Coverage against `org.aventyrs.core.item`

**⚠️ The item subsystem was mid-redesign when this index was written** (uncommitted work on
`experimental-agent-1`). CLAUDE.md's inline "Itens/Equipamento" section and the `adding-an-item`
skill both describe the *pre-redesign* model (a catalog enum implementing `Item` directly, no
Improvement/Masterpiece layer) and are **stale**. The current shape:

- **`Item`** is now the *owned copy* contract (per-copy `damageTaken`, fitted `Masterpiece` /
  `Improvement`, a **Regalia** marker `getRegaliaGrade()` → `RegaliaGrade` [`isRegalia()` iff set
  — a per-copy property, **never an `ItemRarity`**], `ItemActiveAbility`); **`ItemTemplate extends
  Item`** is the catalog blueprint, with `forge()` bridging the two. `AbstractItem` is the
  builder-built copy; `AbstractWeapon extends AbstractItem implements Weapon` adds `damageBase` +
  `skillType`.
- **`Improvement` / `Masterpiece`** interfaces carry rich hooks (`getEffectiveDefenseBonus`,
  `resolveBonus`, `resolveDamageBaseIncrease`, `resolveDamageReduction`,
  `resolveDurationIncreaseInRounds`, `onFinalDamageTaken`, weight/hardness deltas).
  `ItemImprovement` / `ItemMasterpiece` are the per-copy wrappers holding creation-time choices
  (`camadaDeReforco`, `bencaoElemental`, `magistral`, `sobMedida`).

### Authored (7 of the 12 source subsections)

| source subsection | code | n | note |
| --- | --- | --- | --- |
| Armaduras | `ArmorItem` | 8 / 8 | the reference catalog; `ArmorItemTest` pins the count |
| Obras-Primas Defensivas | `DefensiveMasterpiece` | 15 / 15 | `DefensiveMasterpieceTest` pins 15 |
| Aprimoramentos de Obras-Primas Defensivas | `DefensiveImprovement` | 17 / 17 | `DefensiveImprovementTest` pins 17 |
| Tipos de Pedras do Poder | `PowerStoneType` | 17 / 17 | tri-modal (base + defensivo/ofensivo by host `ItemType`); `PowerStoneCatalogTest` pins the counts |
| Qualidades de Pedra (Jolda/Joia/Relíquia/AEthernum) | `PowerStoneQuality` | 4 / 4 | Preço + Cargas/Resfriamento/Vinculação/Duração — authored-inert, no consumer |
| Obras-Primas de Pedras do Poder | `PowerStoneMasterpiece` | 5 / 5 | charge-economy deltas folded by `PowerStone` |
| Aprimoramentos de Obras-Primas (Pedra) | `PowerStoneImprovement` | 1 / 1 | Conexão Veloz only — modeled like `ItemRarity` |

**Pedras do Poder — what's live vs authored-inert.** A `PowerStone` is a per-copy fitted
instance (`PowerStoneType` + `PowerStoneQuality` + optional masterpiece/improvement), socketed
via `AbstractItem#setPowerStone`, which requires `DefensiveImprovement.ENCAIXE` fitted — so an
armor/shield only until an offensive Encaixe exists. Its passive mode effects fold into the same
`Item` enhancement aggregation the Masterpiece/Improvement use (`resolvePowerStoneBonus`), so
they reach `DefenseService`/`DamageService`/`MovementService`/`DamageBaseService` with no service
change bar one: `MovementServiceImpl` gained the equipment `MOVEMENT` pass it lacked.

- **Expressible today** (typed `ItemBonus`, per the racial-feat-catalog ratio): Hematita /
  Relâmpago Dourado / Mitral Puro `MOVEMENT +2`; Rútilo Subterrâneo `DEFESAS +2` + Atletismo
  Vantagem; Mitral Puro / Hematita "Vantagem em Perícia de Ataque"; Calcita Vulcânica / Adamante
  Bruto / Sombra Solidificada `DAMAGE_REDUCTION 1` (the first two damage-type-simplified, Sombra
  unscoped); Sombra Solidificada offensive Dano Base +1.
- **Authored catalog + per-constant TODO** (~11 stones): blocked on no
  Resistência/Vulnerabilidade Elemental, no first-instance-per-Rodada damage tracking, no Área de
  Efeito, no attribute-grant-from-equipment hook, no multiplicative/halving stage, no
  Corrupção/immunity, no Roubo de Vida from equipment, no PV-regen tick.
- **The charge economy is authored-inert** — `PowerStoneQuality`'s Cargas/Resfriamento/Danos de
  Vinculação/Duração and the `PowerStoneMasterpiece` deltas are exact figures nothing reads (no
  activation service, no forge/bind step), same as Preço.

### Not authored

- **26 base Defensivos** — every Bota, Capa, Escudo and Protetor de Cabeça. `ItemCategory` has
  `BOOTS`/`CLOAK`/`SHIELD`/`HELMET`/`GLOVES`/`RING` waiting; no `<Category>Item` enum exists.
- **60 Armas** — the entire Ofensivo section. `Weapon`/`AbstractWeapon` exist but no catalog
  enum (`BowItem`, `LightBladeItem`, …) does. `Fortalecimento` and `Artefatos de Conjuração`
  have no `ItemCategory` at all (they are not a clean fit for `CLUB`/`GLOVES`).
- **12 Equipamentos Naturais** — `ItemCategory.NATURAL_WEAPON` exists; `Arma de Sopro` (breath
  weapon, Cone area) and the five Defesas Naturais have no category.
- **17 Obras-Primas Ofensivas + 18 Aprimoramentos Ofensivos** — no `OffensiveMasterpiece` /
  `OffensiveImprovement`. `Masterpiece`/`Improvement` were built defensive-first; the offensive
  halves are the obvious second consumer. `OffensiveImprovement.ENCAIXE` is also what would
  unblock socketing a Pedra do Poder into a weapon.
- **Equipamentos Tecnológicos** — stub in the source too.

### Mechanisms already built that an unwritten catalog would flow into

No wiring needed — these all scan `character.getEquipment()`:

- **DF / DM** — `DefenseServiceImpl.sumEquipment` → `Item#getEffectiveDefenseBonus` (base column
  + Masterpiece + Improvement + Favor `DEFESAS`), consumed by `EsquivaEApararInteraction`.
  `ItemWeightClass` drives that Interaction's Destreza penalty.
- **RD** — `DamageServiceImpl` sums both `resolveFavorBonus(DAMAGE_REDUCTION)` and
  `resolveEnhancementBonus(DAMAGE_REDUCTION)` plus `resolveEnhancementDamageReduction` per
  equipped item.
- **Dano Base scale-ups** — `DamageBaseServiceImpl` sums `resolveEnhancementDamageBaseIncrease`
  (weapon-source).
- **Perícia roll bonus** — `AbstractSkillInteraction` sums
  `resolveEnhancementBonus(skillType.getRollBonusType(), skillType, …)`.
- **Reações / Ações Livres** — `ReactionsServiceImpl` / `FreeActionsServiceImpl` sum
  `resolveEnhancementBonus(REACTIONS / FREE_ACTIONS)`.
- **Movimento** — `MovementServiceImpl` sums `resolveEnhancementBonus(MOVEMENT)` per equipped
  item (added with the Pedra do Poder work — it was the one `resolveEnhancement*` consumer
  missing).
- **Spell Duração** — `SpellDurationServiceImpl` sums `resolveEnhancementDurationIncreaseInRounds`.
- **Pedra do Poder** — folded into `resolveEnhancementBonus` /
  `resolveEnhancementDamageBaseIncrease` / `getEffectiveDefenseBonus` via
  `Item#resolvePowerStoneBonus(type)`, so all of the above pick it up.
- **Dureza / destruction** — `Item#applyDamage` spends `damageTaken`; at 0 PV every column above
  reads absent. `DamageServiceImpl` calls `notifyFinalDamageTaken` on each equipped item.
- **Regalia active abilities** — `ItemActiveAbility` + `AbstractItem#setActiveAbility`
  (guarded: only a Regalia — a copy with a `RegaliaGrade` — may hold one).
- **Regalia crafting (Talentos de Artífice)** — `EquipmentCraftingService#forgeRegalia(crafter,
  trade, base, RegaliaGrade, RegaliaDonation)`. `RegaliaGrade` (MENOR/SUPERIOR/DIVINA) carries the
  GD (Inimaginável / Milagre), the days (90 / 145 / 180), whether an Acerto Crítico is mandatory
  (Divina) and whether an external essence donor is required (Divina). The forge gates on the
  trade Especialização, the matching `ArtificeFeat` (`ArtificeFeat.requiredToForge`), a willing
  `RegaliaDonation` and — for Divina — a `CreatureType.DRAGAO`/`ELEMENTAL`/`ABISSAL`/`CELESTIAL`
  donor; on success it marks the copy, stamps `producedByCharacterId` and calls
  `Character#recordRegaliaCrafted`. Not modeled (GM's call): the Centelha *loss*, the *Forja do
  Olho de Deus* location, the mandatory-Crítico (reported via
  `regaliaCraftingRequiresCriticalResult`), and the PE cost.

Still no consumer: **Preço** (no PE economy) and the **Conjuração** column (no item-granted hook
on either `SpellCastingService` roll) — real, exact data per the "can't apply it yet doesn't
mean can't compute it yet" discipline.

## Clause shapes the current model cannot express

Recurring across the unwritten sections; each is why a catalog entry would carry prose, not a
typed bonus:

- **Disjunctive Requisitos** — `For 3 ou Des 3`, `For 4 ou Des 4` on most weapons.
  `ItemRequirements` is a single `AttributeDomain` + value, ANDed. Same limitation
  `FeatRequirements` disjunctions hit.
- **Damage-type-scoped mitigation** — "Dano de Corte e Perfuração reduzido em -2", "Danos
  Elementais sofridos -1". `DamageType` has no Corte/Perfuração/Esmagamento breakdown; RD/RA
  resolve with no damage type. Already documented on `ARMADURA_COMPLETA` et al.
- **Multiplicative stages** — "Movimento Base reduzido à metade" (Armadura de Justa, coded as
  prose), "reduzido à metade (efeito de Meio-Dano)" on several Materiais Especiais.
  `MovementService` has no halving stage; **do not add `MOVEMENT_HALVED`**.
- **"Muda para" overrides** — `DF muda para +2`, `Dano Base muda para 2d6`. The Masterpiece
  hooks handle this (`getEffectiveDefenseBonus` replaces rather than stacks); a weapon catalog
  would need the same for its Dano Base column.
- **Efeito Crítico / Corrente de Efeitos grants from an item** — most weapon Favores add or
  change one (`recebe Ferida Profunda como Efeito Crítico adicional`, `Corrente de Efeitos –
  Prender e Puxar`). `CriticalEffect` exists but nothing routes an *item-granted* one into a
  roll. Blocks nearly every weapon Favor.
- **Narrative-purpose scoping** — "Vantagem em Furtividade em tipo de ambiente definido na
  criação", "+1d6 se o alvo for um objeto, construto ou equipamento". This core doesn't track
  what a roll is *for*.
- **Activated item effects** — the `Bracelete Arcano` / `Repulsor` / `Adaga de Cerimônias`
  pattern ("1PA + 2PM para aumentar Bônus nas Defesas em +N por 3 Rodadas"). `ItemActiveAbility`
  is the hook, but only a Regalia may currently hold one, and these are ordinary items.
- **Two-handed / recarga / Tempo de Ação deltas** — "Exige uso de ambas as mãos", "Tempo de Ação
  dos ataques aumentado em +1PA", "Tempo de Recarga 1PA". No hand-slot or reload model.
- **Weapon-as-Arremesso dual mode** — "Pode ser usado como Arma de Arremesso, Dano Base muda
  para 1d6+1". One `Weapon` has one `SkillType` and one `DamageBase`; `getSkillType()`'s own
  javadoc already notes a thrown-lança can't say so.

## Fabricação e Reparo — the player-side crafting pipeline

Rules text: [`fabricacao-e-reparo.txt`](fabricacao-e-reparo.txt) (author-supplied; **not** in the
Equipamentos docx). Modeled by `org.aventyrs.core.character.services.EquipmentCraftingService`
(+ `…Impl`), caller-driven — it prices and times the work and resolves the GD, but does not roll
the Perícia (this core never rolls). `forge` / `repair` / `installImprovement` mutate only after
the caller has determined the roll succeeded.

- **Which Perícia** — fabrication is a **Profissão** (trade Especialização) roll, not the
  Conhecimentos the text names: the trades live on `ProfissaoSpecialization` and `ArtificeFeat` /
  `GoblinFeat` already call crafting a Profissão roll. Reparo keeps both — a Profissão (trade)
  roll for the labour plus the Conhecimentos roll whose GD `RepairAssessment` carries.
- **Rarity → GD** lives on `ItemRarity` (`getFabricationDifficulty`, `getImprovementInstallDifficulty`,
  `getRepairDifficulty`, `getMasterpieceRepairDifficulty`, `getMinimumMasterpieceGraduation`);
  `NATURAL` throws.
- **Multiple Aprimoramentos per copy** — `AbstractItem` now holds a `List<Improvement>`
  (`addImprovement`), capped 1 / 2 / 3 by `ItemWeightClass#getMaximumImprovements()` and gated on
  the copy being an Obra-Prima, both enforced in `installImprovement`. `Item#getImprovement()` is
  a deprecated first-or-null shim; every enhancement-aggregation default on `Item` sums the list,
  so no consuming service changed.
- **Still missing** — no PE economy (cost is reported, not spent — the `ResourcesAdvantage#BARGANHISTA`
  gap), no Aprimoramento Preço column (so install cost/time are unavailable), the "estende a
  Magias e Habilidades" clause of `REPARO_MELHORADO` (no Dureza on either), and the
  `FORJA_VULCANA` benefits (Resistência a Críticos / Margem Crítica Maior / item-scoped choices).

## When authoring starts

- **Follow `ArmorItem` for base Defensivos** (`BootsItem`, `CloakItem`, `ShieldItem`,
  `HelmetItem`); the column set is identical.
- **A weapon catalog** needs `AbstractWeapon`'s two extra columns per constant plus a
  per-`ItemCategory` enum; author `Dano` as `DamageBase`, `Tipo` as `DamageType` (once it has a
  breakdown — until then prose), Efeito Crítico via the efeitos-criticos catalog.
- **Add a pin test** in the `SpellCatalogTest` style once more than one subsection is authored —
  `PowerStoneCatalogTest` pins the Pedra counts; `ArmorItemTest` / `Defensive*Test` pin theirs.
- **Offensive Obras-Primas / Aprimoramentos** pair naturally with `OffensiveMasterpiece` /
  `OffensiveImprovement`; adding `OffensiveImprovement.ENCAIXE` also lets a weapon take a Pedra
  do Poder (loosen `AbstractItem#setPowerStone`'s guard to accept it too).
- **Update CLAUDE.md's inline section and the `adding-an-item` skill** to the
  `ItemTemplate`/`Improvement`/`Masterpiece`/`PowerStone` model — both are stale as of this index.
