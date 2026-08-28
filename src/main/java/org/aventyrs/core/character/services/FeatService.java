package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

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

    /**
     * Every authored Talento character is <b>allowed</b> to take right now — prerequisites met,
     * not already held — regardless of whether they can pay for it. Delegates to {@link
     * FeatCatalog#availableFor}; call either, this one just keeps a caller that already holds a
     * {@code FeatService} from reaching for a second type.
     *
     * <p>Lists the authored catalog only. A homebrew Talento built on {@code AbstractFeat} is
     * grantable but never appears here — see {@code FeatCatalog}'s javadoc.
     */
    List<Feat> getAvailableFeats(Character character);

    /**
     * {@link #getAvailableFeats} narrowed to what characterSheet's unused experience can
     * actually pay for, at {@code character.getRace().getNewFeatCost(category)} apiece.
     *
     * <p>Deliberately a separate method rather than a flag: "am I allowed this?" and "can I
     * afford it?" are different questions with different answers over time, and a UI usually
     * wants to show both — an ineligible Talento is hidden or greyed out permanently, an
     * unaffordable one is a savings goal. Cost is per-Race, so this can differ between two
     * characters with identical Talentos and identical XP ({@code Gigantes} pays 2 for
     * Sobrevivência where everyone else pays 3).
     */
    List<Feat> getAffordableFeats(Character character, CharacterSheet characterSheet);
}
