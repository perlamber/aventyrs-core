package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.InstinctAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;

import static org.aventyrs.core.util.TranslatableMessages.EXTRA_SUPREMA_ALREADY_GRANTED;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_NOT_HELD;

public class TitleAbilityServiceImpl implements TitleAbilityService {

    /** The normal, otherwise-unenforced Suprema allotment every held Título gets. */
    private static final int BASE_SUPREMA_SLOTS = 1;

    @Override
    public int getAvailableSupremaSlots(final Character character, final AventyrTitle title) {
        int allowed = BASE_SUPREMA_SLOTS + (isExtraSupremaAvailable(character) ? 1 : 0);
        return (int) Math.max(0, allowed - countSupremas(title));
    }

    @Override
    public AventyrTitleAbility grantTitleAbility(final Character character, final CharacterSheet characterSheet,
                                                  final AventyrTitle title, final AventyrTitleAbility ability) throws IllegalOperationException {
        if (!character.getAllTitles().contains(title)) {
            throw new IllegalOperationException(TITLE_NOT_HELD);
        }
        if (!ability.isEligible(title)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }

        if (ability.isSupreme() && countSupremas(title) >= BASE_SUPREMA_SLOTS) {
            if (!hasCentelhaSuperior(character)) {
                throw new IllegalOperationException(REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD);
            }
            if (character.isCentelhaSuperiorSelected()) {
                throw new IllegalOperationException(EXTRA_SUPREMA_ALREADY_GRANTED);
            }
            characterSheet.useExperience(EXTRA_SUPREMA_COST);
            character.selectCentelhaSuperior();
        }

        title.grantAbility(ability);
        return ability;
    }

    /**
     * Whether CENTELHA_SUPERIOR's own one-time extra grant hasn't been spent yet — character
     * holds the ability, and {@link Character#isCentelhaSuperiorSelected()} hasn't already been
     * set by an earlier {@link #grantTitleAbility} call. Not derivable purely by counting
     * Supremas already held (see {@link Character#centelhaSuperiorSelected}'s own javadoc for
     * why): a Título that started with zero Supremas looks identical, count-wise, whether that
     * one Suprema came from the normal base allotment or from this ability's own extra grant.
     */
    private boolean isExtraSupremaAvailable(final Character character) {
        return hasCentelhaSuperior(character) && !character.isCentelhaSuperiorSelected();
    }

    private boolean hasCentelhaSuperior(final Character character) {
        return character.getAttributeAbilities().stream()
                .anyMatch(ability -> ability == InstinctAbility.CENTELHA_SUPERIOR);
    }

    private long countSupremas(final AventyrTitle title) {
        return title.getAbilities().stream().filter(AventyrTitleAbility::isSupreme).count();
    }
}
