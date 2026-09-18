package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.IllegalOperationException;

/** Validated entry point for granting a Título to a character. */
public interface TitleAcquisitionService {

    /**
     * Grants title to slot after applying every held Talento's title-acquisition permission. The
     * plain {@link Character#grantTitle(AventyrTitle, TitleSlot)} mutator remains available for
     * builder and fixture assembly, as with other progression invariants.
     *
     * @throws IllegalOperationException if a held Talento prohibits acquiring this Título
     */
    AventyrTitle grantTitle(Character character, AventyrTitle title, TitleSlot slot)
            throws IllegalOperationException;

    /**
     * Whether character's held Talentos let them acquire title — the non-throwing form of {@link
     * #grantTitle}'s check, for a caller deciding what to offer.
     */
    boolean isPermitted(Character character, AventyrTitle title);
}
