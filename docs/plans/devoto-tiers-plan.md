# Devoto tiers — implementation plan

**Status: done (core 0.0.86; aventyrs-api and the client on 0.0.86)** — see `0.086.CHANGELOG.md`. The API stores
`devotionTier` and `devotionPicks` on the character and has `PUT /api/character-sheets/{id}/devotion-tier`. The client
has the tier on the wizard's identity step, the tier and any owed picks on the hub, and the Narrador's "Devoção"
screen on the GM console, outside the Cena. Table rulings (2026-10-01):
the player picks the tier at creation; the Narrador raises or lowers it freely, on a screen of its own outside the
Cena; a rung's pick is made when first reached and kept; Resistência às Correntes +2 is Resoluto's margin.

*Original plan follows.* It was written alongside core 0.0.85, which authored every generic Corrente de Efeitos. Three
of those Correntes (Remover Aflição, Excomungar, Toque Sombrio) and one use of Explosão Cataclísmica have their only
consumers in the Devoto Talentos below, and none of those Talentos is in core yet.

## What the rules say

`docs/rules/talentos.txt` ("TALENTOS DE DEVOTO", Panteão de Tellus, *Talentos de Devoção*) has nine Talentos. Each
takes a Divindade as its Pré-requisito ("Devoto de X") and has three rungs, **Adepto**, **Fiel** and
**Fundamentalista**. Each rung's effect holds only while the character is at least at that level of devotion:

| Talento | Divindade | Adepto | Fiel | Fundamentalista |
| --- | --- | --- | --- | --- |
| Acólito da Luz Primordial | Luz Primordial | Resistência às Correntes +2 | **Remover Aflição** on Magias/Habilidades targeting allies | +2 Defesas; Magias divinas carry **Excomungar** |
| Adépto da Escuridão Profunda | Escuridão Profunda | Vantagem on Furtividade and Ataque at night / out of the sun | attacks carry **Toque Sombrio** (and must shun light) | Roubo de Vida 2; every other heal -3 |
| Armamento Vulcano | Vulcano | Dano Base of all weapons +1 | Armadura/Escudo Defesas +1 | Margem Crítica Menor +1, enemies' -1 |
| Astúcia de Sylph | — (Persuasão 3, Atenção 3) | Atenção Competência | Persuasão Competência | one more of each |
| Benção de Surt'Eldur | Surt'Eldur | RM | Resistência às Correntes +2 | RDS against other faiths' devotees; Roubo de Vida 1 against them |
| Caminhar de Epona | Epona | +2UD on land | +2 Defesas moving on land; ignores Terreno Difícil | RDS |
| Escolhido de Gaea | Gaea | Vantagem in Conhecimentos: Natureza, Empatia Selvagem, Medicina e Cura | two Competências among those | -1 nível GD in those |
| Impacto Ymiriano | Ymir | +1 dano físico or +1 Defesas | 2PD, once per Rodada: Gelo damage and **Explosão Cataclísmica** | +1 Bônus Variável in Força or Vigor |
| Mentalidade de Tesla | Tesla | Conhecimentos Especialização | Conhecimentos Competência | -1 nível GD in Conhecimentos |
| Tocado por Undine e Haloi | Undine e Haloi | +1 to own healing effects | +1 Defesas and Vantagem afloat | +1 dano for 1 Rodada after being healed |
| Abraçado pela Esquecida | A Esquecida | Vantagem in Maestria da Ocultação | +1 Defesas vs non-umbral; senses umbrals | once per Cena, a Subordinado shade |
| Cultista Umbral | a Senhor Umbral (Cultista) | Vantagem in Maestria da Ocultação | +1 Margem Crítica vs non-umbrals | +3 Dano Crítico vs non-umbrals |

Other Talentos read the tier too:
- **Falsa Devoção** — "considerado Devoto Adepto … mesmo que não siga as Obrigações e Restrições".
- **Palavras de Poder** — Graça needs Fiel plus a Título Desperto; Milagre needs Fundamentalista plus two.
- **Sincretismo Religioso** — a second Divindade.

## Table ruling (2026-10-01)

**The GM grants the tier.** Devotion is not bought with EXP and not derived from anything on the sheet. The Narrador
sets it, and raises or lowers it as the story goes.

## Phases

1. **Model.**
   - `DevotionTier` enum (`ADEPTO < FIEL < FUNDAMENTALISTA`), plus "not devoted".
   - `Character#devotionTier` beside the existing `Character#deity` (a `Deity`, with a `DeityCategory`).
   - "Cultista" is the devotion of a `DeityCategory.SENHOR_UMBRAL`. Read it from the category, not as a fourth
     tier.
   - `DevotionService#setTier(character, tier)`, the Narrador's mutator. It is not progression-locked, since it is a
     story award.
   - Falsa Devoção counts as at least Adepto for Talentos and Habilidades.
2. **Prerequisites.**
   - `FeatRequirements#requiredDeity` (a `Deity`, or a `DeityCategory` for "any Primordial/Elemental" and "Cultista
     de um Senhor Umbral").
   - The tier is **not** a prerequisite: a Devoto Talento can be held at any tier, and each rung reads the tier live.
3. **The Talentos.**
   - A `DevotoFeat` enum (`FeatCategory.DEVOTO`), one constant per row above.
   - Each hook gates on `holder tier ≥ rung`, reading the tier through the sheet-aware overloads.
   - Wire the Correntes that are already built:
     - Remover Aflição — a heal chained onto an ally-targeted Magia or Título activation. This needs a "this cast
       targets allies" signal on `SpellCastRequest`/`TitleAbilityActivationRequest`.
     - Excomungar — on a Magia whose tree is `MagicType.DIVINA`.
     - Toque Sombrio — on every attack, through `Feat#resolveEffectChains`.
     - Explosão Cataclísmica — Impacto Ymiriano's 2PD opt-in, with the Gelo retype through
       `Feat#resolveDamageRetype`.
   - Clauses blocked on systems that don't exist get precise TODOs:
     - time of day / sunlight (Escuridão Profunda's Adepto and Fiel);
     - "umbral" classification (Esquecida, Cultista);
     - "devoto de outra Divindade" on the target (Surt'Eldur's Fundamentalista);
     - afloat (Undine e Haloi's Fiel);
     - Resistência às Correntes de Efeitos — `EffectChainService`'s margin has no resistance term yet.
4. **API.**
   - `devotionTier` on the Character entry/DTO/response, kept by the vitals save like `defects`.
   - A GM-only endpoint (or the existing GM sheet edit) to set it.
5. **Client.**
   - The hub shows the Divindade and tier.
   - A GM control sets the tier.
   - The wizard offers Devoto Talentos filtered by Divindade.

## Open questions

- **Starting tier:** may a new character start at a tier, and if so is it Adepto?
- **Lowering the tier:** when the GM lowers a tier (a broken Obrigação), are the higher rungs simply silent until
  restored? That is the reading this plan assumes.
- **Resistência às Correntes de Efeitos +2:** read as +2 to the margin a Corrente must clear against the holder, the
  way `AutocontroleAdvantage#RESOLUTO` raises it? Confirm.
- **Palavras de Poder:** a whole casting subsystem (Latência, Base/Graça/Milagre). It is out of scope here; it would
  need its own plan.
