package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Dirigir e Cavalgar Perícia test. Which of Dirigir e Cavalgar's specializations
 * ({@link DirigirECavalgarSpecialization}) the roll is for doesn't change {@code
 * skillRollBonus}/{@code difficultyReduction} — a held one can still be requested via
 * {@code SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds; see {@link AbstractSkillInteraction} for how that and the roll
 * bonus/difficultyReduction are actually computed.
 */
public class DirigirECavalgarInteraction extends AbstractSkillInteraction {

    public DirigirECavalgarInteraction() {
        super(SkillType.DIRIGIR_E_CAVALGAR);
    }

    public DirigirECavalgarInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.DIRIGIR_E_CAVALGAR, characterSkillService, modifierResolver);
    }
}
