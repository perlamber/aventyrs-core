# Monster manual 20261015 — import, text fixes, missing mechanics

## Context
"v19 - Manual de Criação de Monstros - 20261015" revises the manual imported as
`aventyrs-core/aventyrs-core/docs/rules/criacao-de-monstros.txt` (20260923 + Macária 20261013).
The 7 finished Modelos keep their structure but many Habilidades changed numbers, wording or
mechanics; the sketched Modelos (Caminhante Umbral … Extra-planar) and the five title-only
Modelos are unchanged outlines. Each Habilidade's rules text is embedded as the `description`
string in its core enum (`core/monster/model/<modelo>/*Ability.java`), so "fix the texts" =
re-import the .txt and update every embedded description; "implement what's missing" = make
the hooks match the new numbers/mechanics and close gaps the new text or newer systems allow.

**Already done by the user:** the new text is in the repo `.txt` (modified 2026-10-15 15:09,
uncommitted). It is the source of truth. Two import losses remain: the Categoria table after
"A tabela abaixo…" is missing (restore the old pipe table, same numbers), and the bullet glyphs
are gone (keep as is).

**Scope decided with the user:** text + new numbers, plus a **Corrente/resistance bridge**, plus a
**generic monster aura system**. Sketched Modelos (Caminhante Umbral … Fera Marítima … Extra-planar,
plus Abissal/Celestial/Arauto/Caminhante Onírico/Coração Vazio) stay **text only**.

## Rulings
- **User, 2026-10-15:** an aura's per-Rodada effect fires at the **holder's Turn start**, rolled
  once for everyone (the `runSummonTurnStart` precedent). Dádiva de Gaea heals the **monster and
  its monster allies only**. Aura do Paradoxo lasts as its text says: **Concentração + 2 Rodadas**,
  ending early after 2 Rodadas with no other character in the area.
- **Defaults I'm applying** (say if any is wrong):
  - Torpor = existing `EgoSetback.TORPOR`.
  - Retyping follows the 0.1.6.10 taxonomy: "Elementais em substituição" = `ELEMENTAL` + the
    chosen element; "Danos Mágicos" (Fúria Predador) = `MAGICO` + element.
  - A lost "Autocontrole temporário" (Carnivoria, Enfraquecer o Espírito, Marionetes) = a
    `TemporaryEgoPenalty`, recovered at the Descanso tier its text names.
  - Presença do Ceifeiro heals the total dealt, summed across victims.
  - Renascido Monstruoso's buff fires on any Macária Efeito Ativo or Título activation,
    reactions included, and renews rather than stacks.
  - Ventos' "−1 per UD" counts like `ElementalDischarge`.
  - Once-per-Cena resets move to `CombatantSheet#startNewScene()`, so they reach live scene foes
    (plain `CharacterSheet`s) and not only `MonsterSheet`.
- **Manual oddity, reported and not applied:** Aura do Paradoxo's Abominação line repeats
  Ofuscar's "Se torna um efeito Passivo" (looks like a copy slip). It stays text-only.

## Key findings
- Live scene foes are **plain `CharacterSheet`s** built from `blueprint.buildCharacter()`
  (client `SceneGridSheetFactory.buildCoreFoeSheet`), not `MonsterSheet`s. So every bridge reads
  held abilities at the **Character level**, through the `MonstrousAbilityGrant`s already in
  `character.getAttributeAbilities()`.
- Saved monsters key abilities by **enum constant name** (Mongo `monsterSheets` blueprint). No
  constant or `ChoiceSpec` pick constant is renamed; only display strings change
  (Renascido → "Renascido Monstruoso", Fisiologia → Ascenção picks, "Dádiva de Gaea Maior").
- The client reads descriptions straight from the core enums (`MonsterEditorController`
  tooltips). The API has no texts, so a rebuild against the new core is enough.
- Tests find actives by display name (`model/MonsterTestKit.held`). Update callers when an
  active is renamed (Dádiva de Gaea → Dádiva de Gaea Maior).

## Phases (core 0.1.7 → bump the 4th number per phase)

**Phase 0 — text (0.1.7.1)**
- Restore the Categoria table in `docs/rules/criacao-de-monstros.txt`.
- Re-copy every constant's description (3rd ctor arg) and display name in the seven
  `monster/model/*/…Ability.java` enums from the new text.
- New `MonsterModelTest#everyDescriptionIsVerbatimInTheRulesText` (whitespace-normalised
  `contains`) to keep them in sync from now on.
- Fix the "Imu4nidade" typo.
- Changelog `docs/changelog/0.1.7.1.CHANGELOG.md`; copy this plan to
  `docs/plans/monster-manual-v19-revision-plan.md`.

**Phase 1 — Corrente/resistance bridge (0.1.7.2)**
- New `MonstrousAbility` defaults: `resolveAttackEffectChains(SkillType, AttackSource, CombatantSheet holder, AbilityContext)`,
  `resolveEffectChainResistanceIncrease(ctx)`, `resolveTargetEffectChainResistanceReduction(ctx)`,
  `resolveAttackDamageRetype(SkillType, AttackSource, ctx)`, `resolveAdditionalCriticalEffects(...)`.
  Forward them through `MonstrousAbilityGrant`, with static scans `MonstrousAbilityGrant.attackEffectChains(holder, skill, source)` etc.
- Read them in:
  - `combat/AttackDelivery#effectChainsGrantedByFeats` and `combat/AttackEffectsPreview.of`;
  - `effect/EffectChainService#getRequiredMargin`, next to the `UndeadAnatomy` +2;
  - the retype fallback after `Feat#resolveDamageRetype` in `AbstractSkillInteraction`;
  - `combat/CriticalEffectResolver` for the extra crits.
- Leave `MonsterSheet.of(…, MonsterBlueprint)`'s `attackEffectChains` empty, with a comment: the
  Character scan already covers it, so setting it would apply chains twice.
- "Neste Turno as Perícias de Ataque recebem…" actives apply a `MonstrousChainWindow`
  (a `TemporaryEffect` keyed by source). The hook checks `holder.hasEffectFrom(name)`.
- New Correntes in `effect/monstrous/`, shaped like `Oprimir`: `Ofuscar` (cumulative ±1PA),
  `TerrorDaFormaVerdadeira` (Assustado → Apavorado, +1 Rodada per re-hit), `Murchar`,
  `VenenoPrimaveril`, `MarcaElemental`, `MarcaDaEscuridao`, `SangriaProfana`,
  `EnfraquecerOEspirito`, `Carnivoria`, `PragaZumbi` (reuse `sheet.Disease`), `Expurgo`.
  Reuse existing ones where the text matches: `RouboDeVida`/`RouboDeMana`/`RouboDeDeterminacao`,
  `Amaldicoar`, `LancarAoChao`.
- Move the once-per-Cena reset to `CombatantSheet#startNewScene()`.

**Phase 2 — per-Modelo deltas + bridge use (0.1.7.3, one commit per Modelo)**
Apply every catalog item below with the existing hooks plus the Phase 1 ones. Notably:
- Alma Elemental:
  - Anatomia and Fúria retype;
  - Fúria Margem +3 and Cataclismo;
  - Armamento Deviante "Explosão +3" through `AttackTargetingService#resolveAttackArea`
    (`AreaOfEffect.ATTACK_EXPLOSION`);
  - Conjuração gives Bônus Racial Foco +2 and the new tree rungs;
  - Sopro gets Cataclismo; Tormenta is `ONCE_PER_SCENE`.
- Aspecto Humanoide: Forma Verdadeira +3/+3/−4 + Terror; Mímico 3PD; Possessão and Enfraquecer
  o Espírito through the bridge (control transfer stays partial).
- Mutante: Fisiologia Estranha +2 resistance; Camuflagem +2 GD; Adaptação 5PD, stacking 1 Rodada.
- Besta Alada: Mergulho Prevenir; Forma de Vento immune except Elemental/Profano/Primordial;
  Empurrão Atroz via `LancarAoChao` + reported push.
- Brotos:
  - Regeneração +1d6/+2d6; Carnivoria; Primaveril (Veneno + Roubos vs poisoned);
  - Murchar (−2 Força + Feridas Dolorosas); Raízes Apex 2d6; Flagelo 4PD.
- Macária:
  - new hook `MonstrousAbility#resolveActivationTriggeredEffects(ctx)`, read by
    `ActiveAbilityService#activate` and `title.AbstractTitleAbilityInteraction#activate`, for
    Renascido Monstruoso's 3-Rodada buff;
  - Sanguenomancia −2/−3, +2; Título grant counts (note);
  - Instinto Resfriamento 2; Garras 3/5;
  - Marca [3PA,4PD] melee Mágico, Resfriamento 2, Abominação "2 attacks" reported;
  - Títere immune 1 Rodada / possessed 2 Rodadas, Abominação Resfriamento 2.
- Cireneia: Ofuscar through the bridge (Apex: Duração 1 + melee Defesas; Abominação passive);
  Aura do Paradoxo's passive Destreza +2.
Each Modelo extends its `*AbilityTest`; refresh every `getUnappliedNote()` to what's actually
still missing.

**Phase 3 — generic monster aura (0.1.7.4)** (`org.aventyrs.core.scene`)
- `MonsterAura(source, Range radius, AuraReach reach, whileInside effects, AuraPulse pulse, exempt, difficultTerrain, holderActionPointsWhileOccupied, exitLingerRounds, emptyAreaEndsAfterRounds)`.
- `AuraReach {ENEMIES, LIVING_ENEMIES, ALL_OTHERS, SELF_AND_MONSTER_ALLIES}`.
- `AuraPulse(dice, flat, DamageDescriptor, reductionPerUd, lifeSteal, conditions, conditionRounds, healAsRest, autocontroleLoss, everyNRounds, pushUd)` → pure `AuraHit`s.
- Producers:
  - passive `MonstrousAbility#resolveAuras(ctx, holder)` (Ventos reads `isFlying`);
  - active `MonstrousActiveAbility.aura(...)`, applying an `EmittedAura` `TemporaryEffect` so
    Duração and Concentração tick through existing machinery.
- `AbstractCombatantSheet#resolveMonsterAuras()`.
- Ledger: `Scene#refreshMonsterAuras(holder, ctx)` mirrors `refreshProjectedAuras`
  (revoke-then-grant, open-ended bonuses, the linger turns into timed bonuses, the holder's +3PA
  while occupied, cleanup in `removeParticipant`).
- Tick: `Scene#pulseMonsterAuras(holder, ctx, round, faces)` → `List<AuraHit>`; the caller
  applies them.
- Condition immunity (Gaea: Medo/Possessão) via a `ConditionWard` read by `isImmuneToCondition`.
- Wire the six auras:
  - Paradoxo, Aura Elemental, Ventos Caóticos (+ Invocar Tempestade pulses), Dádiva de Gaea
    (+ Maior cleanse via existing cleansing), Presença do Ceifeiro, Mestre das Marionetes;
  - plus Flagelo de Lacerto's fear aura (Abalado in Média).

**Phase 4 — API**
- `SceneMonsterAuraEntry` on `SceneDocument` (null-tolerant), ticked in
  `SceneService#advanceTurnReporting` beside `runRoundBoundaryForSummons`.
- Nullable `auras` on `CombatantStateMessage`.
- STOMP `/app/scenes/{id}/aura-pulses` → `/topic/scenes/{id}/aura-pulses`
  (`AuraPulseMessage`, validated like `HazardMessage#isWellFormed`); the owning client applies.

**Phase 5 — client**
- `SceneGridAuras` helper:
  - `refreshAll()` wherever `refreshProjectedAuras()` is called today (moves, joins);
  - `pulseAt(holder)` from `onTurnAdvanced`, generalising `runSummonTurnStart`, rolling with
    `SceneGridDice` and relaying hits for sheets it doesn't own.
- Ventos' difficult terrain goes into `movementMap()`; its push goes through
  `ForcedMovementService#push`.
- Radius overlay + "Na aura de X" in the info panel.
- Bump the core dependency.

### Stays PARTIAL (documented in notes)
- Senhor dos Mortos control; Torpor-after-possession timing beyond the setback.
- Possessão control transfer; Abduzir carry; Mímico replay.
- Mimetizar Raça/Centelha; limbs.
- Tanatose / Preferido de Cireneia (TABLE_ONLY).
- Ventos' physical-ranged immunity, unless the defender-side scan is cheap in `AttackDelivery`.
- Filactéria object; Marca Abominação's second attack (reported).
- Effect-created terrain for Tormenta/Raízes, other than the aura difficult-terrain hexes.

## Verification
- Core, per phase: `./gradlew test --tests 'org.aventyrs.core.monster.*' --tests '*EffectChain*' --tests '*AttackDelivery*' --tests '*MonsterAura*' --tests '*SceneTest*'`,
  then the full `./gradlew test` and `./gradlew publishToMavenLocal`.
- API: `./gradlew test` in aventyrs-api (`SceneServiceTest`, the relay test, and
  `MonsterSheetControllerIntegrationTest` still loading saved constant names).
- Client: `./gradlew test` in aventyrs-game-client (`MonsterBlueprintsTest`,
  `MonsterEditorControllerTest`, new `SceneGridAurasTest`).
- Then a live scene: an Apex Cireneia emitting Paradoxo (−2PA on PCs, +3PA on the holder, ends
  after 2 empty Rodadas), and a Brotos Apex healing a monster ally at its Turn start.
- Save memory `monster-manual-v19-revision-rulings` with the rulings above.

## Revision change catalog (old → new)

### General
- "adicional" → "adicionam" typo; GP+3 now grants "uma nova Habilidade Monstruosa" (was
  "Aprimoramento Monstruoso") — repo already had this.

### Abençoado de Cireneia
- Ofuscar: Corrente "… Efeito Cumulativo." (−1PA target / +1PA monster stacks).
- Aura do Paradoxo: NEW passive Bônus Racial +2 Destreza; active [2PA,3PD] now "Emite a Aura",
  Resfriamento 2; aura = Circular Média, others −2PA, monster +3PA (was +2) while ≥1 enemy
  affected; PA recovered 1 Rodada after leaving; holders of the aura are immune.
- Preferido de Cireneia (Abominação): "Apex Preferidos de Cireneia" wording.

### Alma Elemental
- Anatomia Elemental: + "Danos causados por este Monstro são Elementais em substituição".
- Fúria Elemental: attacks become Elementais (substitution), Margem Crítica Menor +3, Cataclismo.
- Armamento Elemental Predador: "Ataques recebem Área de Efeito – Explosão e +3 Dano" (was +2
  atk/dano on Arma Natural). Apex Marca Elemental "não cumulativo".
- Sangue Elemental (Deviante): Aura Elemental now [2PA,2PD]; Retribuição dano +1d6 (was →1d6);
  Aura Apex "dano muda para 2d6".
- Conjuração Elemental: passive Foco +2 → Bônus Racial +2 Foco; Predador: +1 tree Divina ou
  Profana only, + Emergentes; Apex: Florescentes only (no extra tree).
- Sopro Cataclísmico: + Cataclismo crit.  Sopro Apocalíptico: "Efeito Crítico Menor Cataclismo".
- Invocar Elemental: summons Alma Elemental Presas; Apex split into Predador / alternative single Apex.
- Tormenta Cataclísmica: once per Cena; storm damage "Dano Elemental" (was Físico Elemental).
- Ascenção: Fisiologia Abissal/Celestial renamed Ascenção Abissal/Celestial (typo fix).

### Aspecto Humanoide
- Máscara Social typo; Mímico 4PD → 3PD; Possessão is "Ataque à Distância (Mágico)";
  Tanatose wording; Forma Verdadeira: +3 all Atributos (was +2), Margem +3 (was +2),
  Resist. Correntes of targets −4 (was −2), Terror = Assustado → Apavorado (was Abalado ladder).

### Mutante Monstruoso
- Bote Inesperado "(Físico)"; two-Atributos typo; Camuflagem Perfeita Furtividade +2 níveis
  (was +1); Adaptação Milagrosa 6PD → 5PD, cumulative heal "Duração reduzida para 1 Rodada".

### Besta Alada
- Mergulho Atroz "(físico)", Predador crit Atordoante → Prevenir; Investida Aérea wording.
- Forma de Vento: immune except Elementais, Profanos ou Primordiais (+Profanos).
- Invocar de Tempestades: aura renamed Aura dos Ventos Caóticos (passive); Cego condition named;
  NEW Abominação: aura Área Circular Longa while flying.

### Brotos de Mapinguari
- Nascido das Lágrimas de Flora / Regeneração Selvagem: regen +1d6 for 2 Rodadas (was →
  1d6+½Instinto); Predador total +2d6.
- Carnivoria typo; Margem typo. Lançar Esporos: Ataque Mágico Circular Curta applying
  "Veneno Primaveril" (−1 Mult PV, −1 Mult **PD** (was PM), 2 Rodadas).
- Murchar: Doença; −2 Força + Condição Feridas Dolorosas (was cura → 0); Predador "1º ataque da Rodada".
- Raízes Longas Apex poison 2d6 (was 1d6).  Flagelo de Lacerto 5PD → 4PD.
- Dádiva de Gaea: aura named; active renamed "Dádiva de Gaea Maior", ends Maldição/**Veneno**/Doença
  (was Possessão), Roubo de Bônus Base 2 (was 1), only allies in the aura.

### Escolhido de Macária
- Renascido → "Renascido Monstruoso": +2 For/Ins, RDS, Roubo de Vida 2 now a 3-Rodada buff after
  activating any Macária Habilidade or Título Habilidade (non-cumulative, renews). Sanguenomancia
  Ataque (Mágico), Vigor recovered after 1 Descanso; Predador −2 Vigor + 2 Habilidades de Título
  (was 3); Apex −3 / +2 Vigor, 1 Título + 1 Suprema (Especialização dropped).
- Carrasco Morimbundo: Instinto Predatório Mortis Resfriamento 2.
- Procissão wording.  Espectral: Garras Duração 3 (was 2), Apex 5 (was 4).
- Anunciador da Escuridão: Marca [3PA,4PD] melee Mágico attack, Resfriamento 2; Apex raises
  "Renascidos Deviantes"; Abominação = 2 melee attacks (was indefinite duration).
- Cuspidor: Praga Zumbi is "Ataque à Distância (Mágico)".
- Mestre das Marionetes [Apex] tag; possessed 2 Rodadas then Torpor, Autocontrole recovered per
  Descanso; Títere: immune 1 Rodada, ranged Mágico attack, possessed 2 Rodadas; Abominação Resfriamento 2.

<!-- implementation phases follow -->
