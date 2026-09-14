package org.aventyrs.core.skill.furtividade;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Furtividade Perícia test. Which of Furtividade's specializations ({@link
 * FurtividadeSpecialization}) the roll is for doesn't change {@code skillRollBonus}/{@code
 * difficultyReduction} — a held one can still be requested via {@code
 * SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds. See {@link AbstractSkillInteraction} for how the roll bonus/difficultyReduction are
 * actually computed.
 *
 * <p><b>Hiding on this roll is modelled</b>, one layer up: a caller hands this roll's total to
 * {@code org.aventyrs.core.character.services.HidingService#hide}, which turns it into the Grau de
 * Dificuldade every would-be observer's Atenção roll is then made against. The opposed contest is
 * resolved there and not here — this Interaction computes one character's roll, as every
 * Interaction does, and knows nothing about who is looking.
 */
public class FurtividadeInteraction extends AbstractSkillInteraction {

    public FurtividadeInteraction() {
        super(SkillType.FURTIVIDADE);
    }

    public FurtividadeInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.FURTIVIDADE, characterSkillService, modifierResolver);
    }
}
