package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Acquires a new Feat for a character — a Character-progression action, so, like {@link
 * CharacterAttributeService#upgradeBase}/{@link SkillGraduationService#upgradeGraduation}/{@link
 * TitleAbilityService#grantTitleAbility}, it can only happen with a {@link CharacterSheet} in
 * hand to spend XP from.
 */
public interface FeatService {

    /**
     * Validates feat's own {@link Feat#isEligible(Character)} prerequisite, spends {@code
     * character.getRace().getNewFeatCost(feat.getFeatCategory())} XP from characterSheet, then
     * grants feat via {@link Character#grantFeat(Feat)}.
     *
     * @throws IllegalOperationException if feat's prerequisite (Attribute base, Perícia
     *                                    Graduação, and/or an already-held required Feat) isn't
     *                                    met yet, or if characterSheet doesn't have enough
     *                                    unused experience
     */
    Feat grantFeat(Character character, CharacterSheet characterSheet, Feat feat) throws IllegalOperationException;
}
