package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests an Ataque à Distância Perícia test. The rules text notes this Perícia is compared
 * against a target's DF or DM rather than a fixed GD, but that target-side lookup/conversion
 * is left to a layer above this core. When the roll delivers a Magia rather than a mundane
 * attack, see {@link org.aventyrs.core.magic.SpellCastingService}. If the character has a
 * {@code SkillCompetencyAbility} for this same skill whose {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO}), that Attribute is used in place of
 * Ataque à Distância's normal Destreza — see {@link AbstractSkillInteraction} for how the
 * roll bonus/difficultyReduction are actually computed.
 */
public class AtaqueADistanciaInteraction extends AbstractSkillInteraction {

    public AtaqueADistanciaInteraction() {
        super(SkillType.ATAQUE_A_DISTANCIA);
    }

    public AtaqueADistanciaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATAQUE_A_DISTANCIA, characterSkillService, modifierResolver);
    }
}
