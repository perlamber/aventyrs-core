package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.CharacterSheet;
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

    /**
     * {@link #isPermitted(Character, AventyrTitle)} for a Despertar into slot — additionally refused
     * when a held Talento closes that slot ({@code Feat#permitsTitleSlot}: Herança de Gilgamesh's
     * "incapaz de Despertar seu Título Secundário"). What {@link #grantTitle} checks.
     */
    boolean isPermitted(Character character, AventyrTitle title, TitleSlot slot);

    /**
     * {@link #isPermitted(Character, AventyrTitle, TitleSlot)} with the character's sheet, for a slot gate on
     * what only the sheet holds (Centelha Dormente's "apenas com 25 EXP"). The sheet-less forms delegate
     * here with {@code null}, which such a gate reads as "cannot tell" and refuses.
     */
    boolean isPermitted(Character character, CharacterSheet sheet, AventyrTitle title, TitleSlot slot);

    /** {@link #grantTitle(Character, AventyrTitle, TitleSlot)} checked against the sheet too — see above. */
    AventyrTitle grantTitle(Character character, CharacterSheet sheet, AventyrTitle title, TitleSlot slot)
            throws IllegalOperationException;
}
