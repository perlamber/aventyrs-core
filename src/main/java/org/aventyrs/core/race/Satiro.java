package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Sátiro race can do under each rule-set. Two of this race's traits are
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Instinto, +1 Carisma) and
 * {@link #generateEmptyCharacter} seeding {@link SizeCategory#MINUS_ONE} — everything else
 * needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Idiomas</b> (Continental + Arcano dialeto Feérico ou Silvestre, à escolha do
 *   jogador) — same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~80 anos, como os Humanos) — same "no age/lifespan concept" gap as
 *   every other race; purely narrative today.</li>
 *   <li><b>1 Talento Feérico adicional</b> (exceto Pixie e Asas) — same "no Feat catalog, no
 *   {@code Character.feats} list" gap as every other race's free Talentos; the "exceto Pixie e
 *   Asas" exclusion would, even once a Feat catalog exists, be an unenforced prerequisite same
 *   as every other "Requer N Graduações"-style clause this codebase deliberately leaves
 *   unvalidated.</li>
 *   <li><b>Treinamento em Conhecimentos</b> (+ Especialização adicional em Natureza ou
 *   Metamágico) <b>+ Especialização de Perícia adicional</b> (Artes, Furtividade ou Persuasão,
 *   apenas quando treinado) — same shape and gap as {@code Fada}'s/{@code Furia}'s own
 *   Conhecimentos-training clause (itself modeled on {@code Elfo}'s Origem Mística): {@link
 *   Race} has no hook for granting starting Perícia training at all. The "apenas quando
 *   treinado" condition is, again, an unenforced prerequisite by this codebase's own
 *   convention, not a new gap.</li>
 * </ul>
 *
 * <p>None of this race's four Características Raciais fit {@code SkillCompetencyAbility}'s
 * shape well enough today to catalog in a {@code SatiroRacialAbility} enum:
 *
 * <ul>
 *   <li><b>Feromônio Encantador</b> (spend 1PD for Vantagem on the character's próxima rolagem
 *   de Persuasão against an adjacent target; an Encantamento effect) — a simpler variant of
 *   {@code Fada}'s/{@code Furia}'s own Feromônio Encantador (one skill instead of two, "próxima
 *   rolagem" instead of a Rodada-scoped duration, {@code Range.ADJACENTE} instead of Distância
 *   Curta), but the same core piece is still missing either way: nothing in this core can
 *   trigger a standalone, opt-in Ação Livre activation outside of any skill's own roll to grant
 *   a self-targeted temporary bonus (every existing use of {@code CharacterSheet
 *   #grantTemporaryBonus} is the *result* of a roll, e.g. {@code
 *   ArtesCompetencyAbility#DOM_BARDICO}), nor does {@code PersuasaoInteraction} take an {@code
 *   attackTarget}-style parameter to check adjacency against (same "melee/non-Ataque skills
 *   have no target parameter" gap {@code Gorgona}'s own Olhar de Lacerto cites). Being an
 *   "efeito de Encantamento" also needs the same missing Encantamento-effect classification
 *   {@code Fada}'s/{@code Furia}'s/{@code Gorgona}'s own Imunidade a Encantamentos cite.</li>
 *   <li><b>Natureza Mágica</b> (mimic 'Aliado da Natureza' at 2PD, mesmo que não a conheçam) —
 *   same missing Magia entity as {@code Fada}'s/{@code Furia}'s own Natureza Mágica ({@code
 *   org.aventyrs.core.magic.SpellCastingService}'s own "No Magia entity/list exists yet"), plus
 *   the same "cast without knowing/training" override neither of those two races' version could
 *   reach either. (Coincidentally shares its name with the unrelated {@code
 *   EmpatiaSelvagemCompetencyAbility#ALIADO_DA_NATUREZA} ability — that's a Subordinado-training
 *   Habilidade de Competência, not a spell, and the naming collision doesn't change either
 *   gap.)</li>
 *   <li><b>Herança Druídica</b> (the first Magia Natural conjurada a cada Turno Par custa -1PM
 *   ou -1PD a menos, com Tempo de Conjuração reduzido em -1PA nesses Turnos) — needs the same
 *   missing Magia entity/Natural-type classification as Natureza Mágica above, plus a
 *   spell-*casting*-cost system this core has no equivalent of ({@code SpellCastingService}
 *   only computes the two rolls a cast involves, per its own javadoc — it has no PM/PA/PD cost
 *   model at all), plus Turno-*parity* tracking ("a cada Turno Par") and "first cast this Turn"
 *   state, neither of which exist (a step beyond {@code Pequenino}'s own Distraído e Motivado
 *   gap, which only needed to remember *which Perícia*, not *which Turno number's parity*).</li>
 *   <li><b>Falar com Animais</b> (spend 2PD to communicate with animals, limited by each
 *   animal's own intelligence, lasting a whole Cena) — identical trait and wording to {@code
 *   Bestial}'s own Falar com Animais (the source rules text names "Bestiais" verbatim here too,
 *   evidently reused from that race's own entry) — same gap: no character-to-creature
 *   communication concept exists anywhere in this core, and no Cena-scoped duration tracking
 *   either (same gap {@code Gnomo}'s own Mente Veloz, Sorriso Veloz cites).</li>
 * </ul>
 *
 * <p>So {@link #getRacialAbilities()} is left at {@link Race}'s own empty default, same
 * reasoning as {@code Gigantes}/{@code Pequenino}/Humanos/{@code Gnomo}/{@code Orc}/{@code
 * Bestial}/{@code Fada}/{@code Furia}/{@code Gorgona}.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Bondosos ou Neutros" is advisory, not a hard rule.
 */
public class Satiro implements Race {

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.INSTINCT, 1, AttributeDomain.CHARISMA, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(SizeCategory.MINUS_ONE);
    }

}
