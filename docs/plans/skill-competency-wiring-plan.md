# Habilidades de Competência — core → API → client wiring plan

**Status (2026-10-02):** Phase 0 built — core 0.0.99, API (`skillType` on ability activations), client
(`docs/wiring-habilidades-de-competencia.md`). Phase 1 built — core 0.0.100 (no client change needed: the client
already prices attacks, records actions and passes the Scene and sheet through core). Phase 2 built — core 0.0.101 (Concentração Inabalável has nothing to
protect until damage breaks Concentração). Phase 3 built — core 0.0.102 + client (limited uses, Reações, the
Kit de Primeiros Socorros). Phases 4–6 open.

**Phase 3, as built:**

| Ability | Core | Client |
| --- | --- | --- |
| Salto Poderoso | `CompetencyUses` (1/2/3 per Cena), `StepRules#ignoringDifficultTerrainCost` | Atletismo row toggle; spent only when the move touched Terreno Difícil |
| Instinto de Luther | `CompetencyUses#use` → 1-Rodada `REACTIONS` +1/2/3; `useAllyInstinct` | Atenção row button; allies' button from a local real sheet or the holder's announcement (`INSTINTO_DE_LUTHER_ALLIES`, sent on entering the Scene and at combat start) |
| Socorro Imediato | 1/2/3 per Descanso Longo (`spendOrdinaryRestScopedUse`) | Medicina e Cura row checkbox → the roll is an Ação Livre |
| Bom Doutor | `resolveSkillRollActionPointAdjustment`, `UtilityItem#KIT_DE_PRIMEIROS_SOCORROS` | Perícia rolls now priced by the sheet-aware `getSkillRollCost` (also fixes Perito Veloz/Lembrar); kit in the store's Utilidades tab |
| Recuo Rápido | once per Rodada (`spendRoundScopedUse`) | Log button after damage from an attack that hit; spends the Reação, then a Reposicionar-range move off-Turn |
| Charme Feérico | `CompetencyUses#charm` (2PD, per creature per Cena) | Log button after a failed Empatia Selvagem roll with a GD; pick the creature, reroll as Ação Livre |

**Still open from Phase 3:** a late joiner misses Luther's announcement until combat (re)starts; Recuo Rápido is
offered only through the damage modal (where this client lands an enemy's hit on its own sheet).

**Phase 0, as built:**

| Gap | Fix |
| --- | --- |
| 1. No Scene on plain rolls | Every panel roll and Narrador CHECK carries `sceneContextFor(sheet)`. |
| 2. No GD on plain rolls | Optional GD picker on the Perícias panel, cleared per roll; the log shows Sucesso/Falha. |
| 3. Skill-roll Blessings dropped | Granted to the roller and controlled allies, relayed with `skillType`, granted by each receiving client. Dom Bárdico reaches every ally in the Scene (ruling). |
| 4. No action surface | Deferred to Phase 4, where the activated abilities need it; a Perícia roll with a GD covers the success-triggered ones meanwhile. |
| 5. Aprimorar com Arte | Picker asks the Perícia; wire `APRIMORAR_COM_ARTE:<SkillType>`; core instance equal by choice. |
| 6. Generalista | Hub follow-up asks for its 2 Especializações. |
| Rules text | Hub chip tooltip; Scene "Habilidades de Competência" section (covers every display-only ruling). |

**Still open from Phase 0:** an Aprimorar com Arte granted by an Antecedente or Qualidade ("uma Habilidade de
Competência") stays the bare constant — `BackgroundCodec`/`DefectCodec` round-trip traits as enum names and core's
`SkillTraitCatalog` lists only constants.

70 constants across the 14 `*CompetencyAbility` enums, plus `ArtesAprimorarComArteAbility`, the choice-carrying
instance of APRIMORAR_COM_ARTE. (The racial and summon implementors — `*RacialAbility`, `NatureSummonAbility`,
`ZumbiAbility` — are out of scope here: they reach the sheet through race/blueprint and are wired with those.)

## How an ability reaches play today

- **Pick & persist** — built. The hub picker and the wizard step offer `SkillCompetencyAbilityCatalog#optionsFor`,
  the API stores `CharacterSkillEntry#competencyAbilities` as enum `name()`s, and the client parses them back.
- **Into core** — built. `SceneGridController` (≈ l.6544) puts the held constants on the core `Character`, so every
  passive hook that core scans via `SkillCompetencyAbility#allFor` counts on any path the client runs through core.
- **What the client drops** (the common gaps, numbered for the phases below):
  1. **Plain Perícia rolls state no Scene.** `applySkillInteraction` and `resolveRequestedCheck` pass a `null`
     `SceneContext` (except Esquiva e Aparar), so every `resolveConditionalRollBonus`/`resolveAutomaticSuccess`/
     `resolveSuccessBlessings` override that reads proximity, terrain or `isCombatScene` sees "condition not met".
  2. **Plain Perícia rolls state no GD.** The Perícias-panel button rolls against nothing, so `succeeded` is `null`
     and nothing "após ser bem-sucedido" can fire. Only a GM `CHECK` states one.
  3. **`InteractionResult#blessings` / `#temporaryBonusValue` from a skill roll are never granted.** Only the Título
     path (`TitleAbilityActivation`) grants blessings. So Dom Bárdico and Fintar Aprimorado compute and evaporate.
  4. **No activated-ability surface for Perícias.** Títulos have activation buttons; a Habilidade de Competência
     that is an *action* (Estudar Defesas, Esconder Outros, Medicina Alternativa, Milagreiro…) has nowhere to be used.
  5. **APRIMORAR_COM_ARTE is stored as the bare constant.** Core only grants it through an
     `ArtesAprimorarComArteAbility(chosenSkill)` instance; the constant has no overrides, so a player who picks it
     gets nothing. The wire format (`name()` only) can't carry the chosen Perícia either.
  6. **GENERALISTA's two Especializações are never asked for.** `resolvePendingSpecializationChoices` has no caller
     in the client.

## Per-ability status

Legend — **Core**: ✅ real · 🟡 partial · ❌ no mechanism · 📖 narrative only.
**Client**: ✅ reaches play · ⛔ blocked by gap N above · — nothing to do client-side beyond display.

### Artes
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Dom Bárdico | ✅ (`ArtesInteraction` → `temporaryBonusValue`) | ⛔ 2,3,4 | An "inspire allies" action with a GD; grant the bonus to allies (not self) for 1/2/3 Rodadas. |
| Domínio Cultural | ❌ | ⛔ | Perícia-for-Perícia substitution (roll Artes where Conhecimentos is asked). Shares a shape with Manutenção Veicular. |
| Aprimorar com Arte | ✅ (instance) | ⛔ 5 | Picker must ask for the Perícia; wire must carry it (`APRIMORAR_COM_ARTE:ATAQUE_CORPO_A_CORPO`). |
| Desempenho Extraordinário | ❌ | 📖 | No artistic-creation time. Display-only unless such a system is wanted. |
| Espalhar Reputação | 📖 | ⛔ 4 | Roll with GD ≥ Médio, success reported to the log; disposition is the Narrador's. |

### Ataque à Distância
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Arremesso Poderoso | ✅ | ✅ verify | Confirm the attack path hands the `Weapon`/`Spell` as `AttackSource`. |
| Disparo Arcano | ✅ | ✅ | — |
| Frieza | ✅ | ✅ verify | Needs the attack's `SceneContext` (attacks have one; confirm). |
| Disparo Ricochete | ❌ | ⛔ | Corrente de Efeitos on a damaging ranged hit: pick a 2nd target within Muito Curta of the 1st, re-roll, 1d6 (or the effect's, lower). `EffectChain` exists now. |
| Mirar na Cabeça | ❌ (TODO is stale) | — | `CriticalDamage`/`sumCriticalDamage` exist now — add a hook so this grants Vantagem on Danos Críticos. |

### Ataque Corpo-a-Corpo
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Acuidade | 🟡 | ✅ substitution | Desvantagem em Danos with a Categoria Pesada weapon — weapon `ItemWeightClass` and dano rolls both exist now. |
| Brutalidade | ✅ | ✅ | — |
| Sagacidade Arcana | ✅ | ✅ | — |
| Ataque Preciso | ✅ | ✅ | — |
| Abrir Defesas | ❌ | — | On a critical hit, Desprevenido 1 Rodada on the target. Needs an "on critical hit" hook on the attack path. |

### Atletismo
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Alpinista Veloz | ✅ | ✅ verify | Movement-mode UI shows Vertical. |
| Salto Poderoso | ❌ | ⛔ | Ignore Terreno Difícil once per Cena (+1 use at 5ª/10ª Grad). Per-Scene use counters exist on Títulos — reuse. |
| Anfíbio | ✅ | ✅ verify | As Alpinista. |
| Acrobata | ✅ | ✅ | — |
| Passo Largo | ✅ | ✅ | — |

### Atenção
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Percepção de Foxm | ✅ (any distance) | ⛔ 1 | Auto-success only if the roll states a GD — GM CHECKs do. |
| Alma de Sherlock | 🟡 | ✅ substitution | Labyrinth half is 📖. |
| Intuição de Scully | 📖 | — | Narrator-adjudicated sense. Display-only. |
| Ardil de Marple | ❌ | ⛔ 1 | Vantagem on every Perícia roll outside Combate and in Rodada 0. `isCombatScene` exists; needs Rodada 0 on `SceneContext` and a non-null context on plain rolls. |
| Instinto de Luther | ❌ | ⛔ | Per-Cena extra Reações usable in one chosen Rodada (+1 at 5ª/10ª), and from 5 Grad every ally gets one too. |

### Conhecimentos
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Viajante Planar | 📖 | — | Display-only. |
| Teogonia | 📖 | — | Display-only. |
| Memória Eidética | 📖 | — | Display-only. |
| Generalista | ✅ | ⛔ 6 | Prompt for 2 Especializações on acquisition; enforce the cap of 2. |
| O Professor | 🟡 (unconditional) | ✅ | Should apply only when the roll is under one of the holder's Especializações — `requestedAbility` makes that checkable now. |

### Dirigir e Cavalgar
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Controlar Animais | 🟡 (unconditional) | ✅ | Should apply to animal/animal-drawn rolls only — purpose-scoped (see Perito ruling: opt-in). |
| Manutenção Veicular | ❌ | ⛔ | Perícia substitution, as Domínio Cultural. |
| Ginete | ✅ | ✅ verify | Riding state in client. |
| Direção Segura | ❌ | ⛔ 1 | Auto-success "enquanto não estiver sob grande estresse" — **ruling needed**. |
| Direção Acrobata | 📖 | — | Narrator-vetoed manoeuvres. Display-only. |

### Domínio do Mana
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Letalidade Arcana | ❌ (TODO stale) | — | Spells exist now: Margem Crítica Menor +1/+2/+3 on the Conjuração roll (5ª/10ª Grad). |
| Magia Selvagem | ✅ | ✅ | — |
| Conjuração Duradoura | ❌ (TODO stale) | — | `SpellCastingServiceImpl#extendDuration` exists — add +1/+2/+3 Rodadas. Only matters where Duração is tracked (area effects, summons, `Sustained`). |
| Arcanismo Explosivo | ❌ (TODO stale) | — | +2/+3/+4 to a Magia's dano and cura — hook `resolvePrimaryDamage` and `SpellHealing`. |
| Concentração Inabalável | ❌ | — | Today Concentração breaks on casting/attacking only, not on damage — so the ability has nothing to protect yet. Wire when (if) damage breaks Concentração. |

### Empatia Selvagem
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Acadêmico Selvagem | ✅ | ✅ | — |
| Amainar a Selvageria | 📖 | — | GM-side targeting priority. Display-only. |
| Charme Feérico | ❌ | ⛔ | 2PD reroll of a failed Empatia Selvagem roll, Ação Livre, once per creature. Reroll UI exists for Sorte/Ego — reuse. |
| Instinto Animal | ✅ | ✅ | — |
| Aliado da Natureza | ❌ (TODO stale) | ⛔ 4 | Subordinados exist now (docs/rules/subordinados.txt): GD Difícil roll trains a creature into Cavaleiro/Peão/Torre; one active per Cena. |

### Esquiva e Aparar
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Encouraçado e Veloz | ✅ | ✅ | — |
| Evasão | ✅ (attacks) | ✅ verify | Area Magia effects reaching a Defesa — check against the spell-landing path. |
| Movimento Defensivo | 🟡 (unconditional +3) | ✅ | Should apply only vs. Reação effects — purpose-scoped. |
| Estudar Defesas | ❌ | ⛔ 4 | Action: Esquiva e Aparar vs GD Difícil instead of attacking; on success, −1 GD to the holder's attacks against that one target for 2 Rodadas. Needs a per-opponent GD-step blessing. |
| Recuo Rápido | ❌ | ⛔ | After taking damage from an enemy attack, once per Rodada, spend a Reação to Reposicionar (`RepositionService` exists). |

### Furtividade
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Esconder Outros | ❌ | ⛔ 4 | Hide an adjacent ally (Maestria da Ocultação/Infiltrador only), GD +1 step. `HidingService` exists. |
| Ação Surpresa | ❌ | ⛔ 1 | Vantagem on all Perícias while Escondido in a Combat Scene — hooks need the holder's sheet (Condições). |
| Agora Estou, Agora Não Estou | ❌ | ⛔ | Hide while observed at GD +1 — needs an "observed" refusal to lift first. |
| Morte Oculta | ❌ | — | +2/+3/+4 dano while Escondido (traps 📖) — same holder-sheet gap as Ação Surpresa. |
| Ladino Teórico | ✅ | ✅ | — |

### Medicina e Cura
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Bom Doutor | ❌ | ⛔ | −1PA once per Rodada with a kit in hand. Inventory exists now; PA cost is not per-Perícia yet. |
| Medicina Alternativa | ❌ | ⛔ 4 | Roll vs GD Médio during another's Descanso; +1d6 PV/PM/PD at its end. Hook into `RestService`. |
| Milagreiro | ❌ | ⛔ 4 | Raise GD to Difícil; success = Descanso Curto PV; once per target per Descanso Longo (`markAffectedUntilRest` pattern exists); Corrente Milagre Maior adds PM/PD. |
| Socorro Imediato | ❌ | ⛔ | Medicina e Cura as Ação Livre, 1/2/3 uses per Descanso Longo. |
| Alquimia Maior | ❌ | — | No potion/antidote Duração. Display-only until potions exist. |

### Persuasão
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Força Opressora | ✅ | ✅ | — |
| Espalhar Emoções | ❌ | ⛔ 2,3 | After a success on Comunicação/Mentir ou Omitir/Intimidação, Vantagem on the same Especialização vs. other characters within Curta. |
| Sedutor | ❌ | ⛔ | Target-attraction scoped Vantagem — purpose-scoped opt-in (roller declares). |
| Cambalacho | ❌ | — | −1PE on buying/producing Equipamento. No PE economy yet. |
| Fintar Aprimorado | 🟡 | ⛔ 2,3 | Vantagem lasts the whole Rodada (should be next attack only); the −1PA half is missing. |

### Profissão
| Ability | Core | Client | What's missing |
| --- | --- | --- | --- |
| Construtor Eficiente | ✅ | ✅ verify | Is crafting reachable from the client? |
| Forja Vulcana | ❌ | ⛔ | Item-scoped RC, RD 1 / +1 Defesas, Margem Crítica Maior +1, Dano Base +1 / Conjuração +1, chosen at forge. |
| Reparo Melhorado | 🟡 | ✅ verify | 5-Grad "Magias e Habilidades" clause 📖. |
| Aumentar a Dureza | ✅ | ✅ verify | As Construtor. |
| Expandir Carga | ❌ | — | No Carga stat. Display-only until one exists. |

## Tally

- Core ✅ and reaching play (or after a quick check): ~24.
- Core ✅ but stranded by a client gap: Dom Bárdico, Aprimorar com Arte, Generalista, Percepção de Foxm (partly).
- Core ❌ but buildable now (the system it waited for exists): Mirar na Cabeça, Abrir Defesas, Acuidade's half,
  Letalidade Arcana, Conjuração Duradoura, Arcanismo Explosivo, Aliado da Natureza, Recuo Rápido, Salto Poderoso,
  Milagreiro, Medicina Alternativa, Charme Feérico, Socorro Imediato, Esconder Outros, Ação Surpresa, Morte Oculta,
  Ardil de Marple, Instinto de Luther, Estudar Defesas, Espalhar Emoções, Fintar's −1PA & next-roll scope, Bom Doutor.
- Needs a whole new system (economy/items/creation time): Cambalacho, Expandir Carga, Forja Vulcana, Alquimia Maior,
  Desempenho Extraordinário, Domínio Cultural / Manutenção Veicular (Perícia substitution).
- Narrative only: Viajante Planar, Teogonia, Memória Eidética, Intuição de Scully, Direção Acrobata, Amainar a
  Selvageria, Espalhar Reputação's effect.

## Proposed phases

0. **Client plumbing** (no rulings needed): Scene context + optional GD on plain rolls (gaps 1–2), grant skill-roll
   blessings/temporary bonuses with ally targeting (3), a Habilidades de Competência action panel (4), APRIMORAR_COM_ARTE
   choice + wire format `CONST:SKILL` through API (5), Generalista prompt (6), and every ability's rules text on the sheet.
1. **Combat hooks in core**: Mirar na Cabeça, Abrir Defesas (on-crit hook), Acuidade's Desvantagem, Morte Oculta /
   Ação Surpresa (holder-sheet overloads), Fintar's next-roll consumption + −1PA.
2. **Domínio do Mana**: Letalidade Arcana, Conjuração Duradoura, Arcanismo Explosivo.
3. **Per-use counters & Reações**: Salto Poderoso, Instinto de Luther, Socorro Imediato, Recuo Rápido, Charme Feérico,
   Bom Doutor.
4. **Activated abilities**: Dom Bárdico, Estudar Defesas, Esconder Outros, Espalhar Emoções, Milagreiro, Medicina
   Alternativa, Aliado da Natureza, Espalhar Reputação.
5. **Scene-conditioned & opt-in**: Ardil de Marple, Direção Segura (outside Combat Scenes), Sedutor (roller opt-in).
6. **Economy & forge**: Cambalacho in the existing PE pricing; Forja Vulcana's benefits chosen and stamped at
   `ItemForgery#forge`.

## Rulings (2026-10-02)

- **Narrative abilities are display-only.** Viajante Planar, Teogonia, Memória Eidética, Intuição de Scully, Direção
  Acrobata, Amainar a Selvageria: name and rules text on the sheet and in the Scene; the Narrador adjudicates.
- **Domínio Cultural and Manutenção Veicular are interpretative.** No Perícia-substitution mechanism — shown on the
  sheet and in the Scene so the table knows the character may roll Artes / Dirigir e Cavalgar instead.
- **Cambalacho goes into the existing PE spending.** The −1PE is added wherever the price of buying or producing an
  Equipamento is computed.
- **Forja Vulcana is built into item forgery.** `ItemForgery` applies its benefits, including the creation-time
  choice (RD 1 or +1 Defesas; Dano Base +1 or Conjuração +1).
- **Deferred:** Expandir Carga (no Carga stat), Alquimia Maior (no potion creation). Display-only until those exist.
  Desempenho Extraordinário stays display-only too (no artistic creation time).
- **Direção Segura's "grande estresse" = a Combat Scene.** Auto-success on Dirigir e Cavalgar whenever
  `SceneContext#isCombatScene()` is false.
- **Dom Bárdico reaches every ally in the Scene**; plain Perícia rolls get an **optional GD picker** (Phase 0).
- **Bom Doutor's kit is a real item**: a "Kit de Primeiros Socorros" in a new utilities category of the store (where
  potions and the like will go); the −1PA needs it carried.
- **Charme Feérico's "uma vez para cada criatura" resets per Cena.**
- **Espalhar Emoções lasts the rest of the Cena**; **Esconder Outros hides the ally one nível below the roll's tier**;
  **Aliado da Natureza's creatures are lasting, saved on the sheet** (one's benefit per Cena); **Medicina Alternativa
  rolls one d6** for PV, PM and PD alike (Phase 4).
- **Purpose-scoped abilities stay unconditional.** O Professor, Controlar Animais and Movimento Defensivo keep
  their flat bonus on every roll of their Perícia; not narrowed.
