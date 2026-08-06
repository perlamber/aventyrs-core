package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;

/**
 * Defines what the Human race can do under each rule-set. Unlike every other race so far,
 * *nothing* here is currently overridden — but that's a deliberate match to the rules text,
 * not an oversight: Humanos are explicitly the size/attribute baseline ("são consideradas
 * referência do que é fisicamente comum... não recebem pontos de atributos adicionais",
 * "Categoria de Tamanho 0"), so {@link Race}'s own defaults (empty fixed/choosable attribute
 * bonuses, no override on {@link #generateEmptyCharacter}) already express that correctly.
 *
 * <ul>
 *   <li><b>Idiomas</b> (Continental + um adicional per Antecedente) — same "no Language/
 *   Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~80 anos) — same "no age/lifespan concept" gap as every other
 *   race; purely narrative today.</li>
 *   <li><b>2 Talentos Gerais + uma Especialização adicional em até 2 Perícias Treinadas</b> —
 *   same "no Feat catalog, no {@code Character.feats} list, {@link Race} has no hook for
 *   granting starting Perícia training" gap as every other race's free Talentos/
 *   Especializações.</li>
 *   <li><b>Aprendizado Rápido</b> (2 Perícias chosen at creation get their 2nd/3rd Graduação
 *   upgrade at -0.5 EXP) — {@code
 *   org.aventyrs.core.character.services.SkillGraduationService#getUpgradeCost} takes no
 *   {@link Race} at all, has no notion of a race-specific discount, and there's no persisted
 *   record of *which* 2 Perícias a character chose at creation for this racial ability to
 *   scope itself to (the {@code AcquiredChoice} mechanism records a single choice per ability
 *   instance, not a pair). Identical gap to {@code Pequenino}'s own Aprendizado Rápido.</li>
 *   <li><b>Adaptação</b> (Talentos cost 2.5 EXP instead of 3) — {@link
 *   #getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code int}, which
 *   can't represent a genuinely fractional 2.5 — the same int-vs-fractional mismatch already
 *   flagged on {@code Elfos}' Conexão com o Mana and {@code Pequenino}'s own Adaptação.</li>
 *   <li><b>Ofício hereditário</b> (choosing Conhecimentos or Profissões as a starting Perícia
 *   grants an extra Especialização or Habilidade de Competência in it) — same "{@link Race}
 *   has no hook for granting starting Perícia training/abilities" gap, plus needs a persisted
 *   record of which Perícias were chosen as starting ones at all.</li>
 *   <li><b>Prodígios</b> (a Habilidade de Competência from one of the Perícias chosen at
 *   creation) — same shape and same gap as {@code Elfos}' Origem Mística/{@code Anao}'
 *   Pequenos Gigantes (a race-granted *extra* Habilidade de Competência acquisition slot),
 *   here scoped to "any creation-time-chosen Perícia" rather than a fixed pair — no notion of
 *   a race-granted extra slot exists yet, restricted or not.</li>
 * </ul>
 *
 * <p>None of the four racial traits above fit {@code SkillCompetencyAbility}'s shape (they're
 * all about *granting acquisition slots*, not conditional roll bonuses), so {@link
 * #getRacialAbilities()} is left at {@link Race}'s own empty default, same reasoning as
 * {@code Gigantes}/{@code Pequenino}.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race.
 */
public class Human implements Race {

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
