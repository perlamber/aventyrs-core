package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;

/**
 * Requests an Ataque Corpo-a-Corpo Perícia test. Like {@link AtaqueADistanciaInteraction}, the
 * rules text compares this roll against a target's DF or DM rather than a fixed GD — that
 * comparison lives in {@code org.aventyrs.core.combat.AttackDelivery}, which rolls this
 * Interaction and judges the total against the defender's Defesa. This class still only computes
 * the roll; it doesn't know what it's being compared to. This is also the
 * delivery Perícia for Magias with a Toque range descriptor — see {@link
 * org.aventyrs.core.magic.SpellCastingService}. If the character has a {@code
 * SkillCompetencyAbility} for this same skill whose {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * AtaqueCorpoACorpoCompetencyAbility.ACUIDADE}), that Attribute is used in place of Ataque
 * Corpo-a-Corpo's normal Força — see {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class AtaqueCorpoACorpoInteraction extends AbstractSkillInteraction {

    public AtaqueCorpoACorpoInteraction() {
        super(SkillType.ATAQUE_CORPO_A_CORPO);
    }

    public AtaqueCorpoACorpoInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATAQUE_CORPO_A_CORPO, characterSkillService, modifierResolver);
    }
}
