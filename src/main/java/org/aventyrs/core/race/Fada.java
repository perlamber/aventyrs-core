package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Fada race can do under each rule-set. Only one of this race's traits is
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Carisma, +1 Foco) —
 * everything else needs a system this core doesn't have yet. This race is nearly identical to
 * {@link Furia} (see that class's own javadoc): every gap below is shared between the two,
 * except where noted.
 *
 * <ul>
 *   <li><b>Categoria de Tamanho -1 a 0</b> (chosen at character creation; -3 instead, but only
 *   for characters of "linhagem Pixie") — unlike every other race so far, this isn't one fixed
 *   racial default: {@link #generateEmptyCharacter} can only seed one fixed {@code
 *   SizeCategory} per race (see {@code Anao}/{@code Gnomo}/{@code Pequenino}'s own
 *   {@code SizeCategory.MINUS_ONE}, {@code Gigantes}' {@code PLUS_TWO}), with no hook for
 *   offering the player a choice within a range the way {@link #getChoosableAttributes()} does
 *   for Atributos — same "the creation flow doesn't support this yet" gap {@code Pequenino}'s
 *   own Sexo-conditioned Atributo bonus already cites. Left at {@link Character}'s own {@code
 *   SizeCategory.ZERO} default (the upper end of the allowed range) rather than guessing which
 *   end is more "default". The Pixie-linhagem -3 exception compounds this with the same "no
 *   Feat catalog, so lineage can't be tracked" gap the Perícias e Talentos entry below cites —
 *   a character's linhagem is determined by *which* Talento Feérico they picked, which this
 *   core has nowhere to record.</li>
 *   <li><b>Idiomas</b> (Arcano dialeto Feérico + Continental + Antecedente) — same "no Language/
 *   Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~40 anos, aparência sem envelhecimento, potencialmente eterna via o
 *   rito das Dríades enquanto mantiver seus votos com a natureza) — same "no age/lifespan
 *   concept" gap as every other race, compounded here by a conditional-immortality-via-a-rite
 *   concept ("enquanto mantiverem seus votos") this core has no equivalent state machine for at
 *   all; purely narrative today.</li>
 *   <li><b>1 Talento Feérico adicional</b> (determines the character's linhagem within the
 *   race) — same "no Feat catalog, no {@code Character.feats} list" gap as every other race's
 *   free Talentos; {@code org.aventyrs.core.feat.FeatCategory#FEERICO} already exists as a
 *   category (alongside {@code #MONSTRUOSO}/{@code #ELFICO}/{@code #BESTIAL}), but {@code
 *   org.aventyrs.core.feat.Feat} still has no catalog of concrete named Feats within it.</li>
 *   <li><b>Treinamento em Conhecimentos</b> (+ Especialização adicional em Metamágico ou
 *   Natureza) <b>e em uma Perícia adicional</b> (Domínio do Mana, Persuasão ou Furtividade)
 *   <b>+ Especialização adicional na Perícia escolhida, ou uma Habilidade de Competência de
 *   cada uma destas Perícias Raciais</b> — same shape and gap as {@code Elfo}'s own Origem
 *   Mística (a race-granted *extra* Habilidade de Competência/Especialização acquisition slot
 *   restricted to named Perícias' catalogs): {@link Race} has no hook for granting starting
 *   Perícia training at all, so neither the base training nor the bonus slot on top of it can
 *   be granted yet.</li>
 * </ul>
 *
 * <p>None of this race's Características Raciais fit {@code SkillCompetencyAbility}'s shape
 * well enough today to catalog in a {@code FadaRacialAbility} enum — see {@link Furia}'s own
 * javadoc, which documents all four (Feromônio Encantador, Natureza Mágica, Magia Natural,
 * Conexão com o Mana) plus Imunidade a Encantamentos in detail, since every one of them is
 * identical between the two races except Natureza Mágica's specific spell. Fadas mimic
 * 'Piscar' (2PD) where Fúrias mimic 'Transformação Monstruosa' (2PD, restricted to female
 * Humanoid targets of Categoria de Tamanho 0 or lower) — both still need the same missing
 * Magia entity ({@code org.aventyrs.core.magic.SpellCastingService}'s own "No Magia entity/
 * list exists yet") regardless of which spell is mimicked, so this difference doesn't change
 * which piece is missing. So {@link #getRacialAbilities()} is left at {@link Race}'s own empty
 * default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race — "são
 * frequentemente bondosas, amistosas e sociáveis" is advisory, not a hard rule (unlike {@link
 * Furia}'s own more mixed advisory text, this doesn't change how either is modeled).
 */
public class Fada implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.FEERICO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
