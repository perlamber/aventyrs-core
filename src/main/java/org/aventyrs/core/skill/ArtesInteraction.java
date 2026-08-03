package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests an Artes Perícia test. Which of Artes' specializations the roll is for doesn't
 * change the bonus — see {@link ArtesSpecialization} — so it isn't tracked here. See {@link
 * AbstractSkillInteraction} for how the roll bonus/difficultyReduction are actually computed.
 */
public class ArtesInteraction extends AbstractSkillInteraction {

    public ArtesInteraction() {
        super(SkillType.ARTES);
    }

    public ArtesInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ARTES, characterSkillService, modifierResolver);
    }
}
