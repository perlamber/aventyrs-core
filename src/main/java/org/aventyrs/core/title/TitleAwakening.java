package org.aventyrs.core.title;

import lombok.NonNull;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PendingAcquisition;

/**
 * A Título owed to a character, to be awakened into slot at the end of a game session — see
 * {@code DespertarAntecipadoFeat}.
 *
 * <p>Goes through {@link TitleAcquisitionService#grantTitle}, so a held Talento's prohibition
 * still applies at the moment of awakening, not only when the Título was picked.
 */
public record TitleAwakening(@NonNull AventyrTitle title, @NonNull TitleSlot slot) implements PendingAcquisition {

    @Override
    public void acquire(final CharacterSheet sheet) throws IllegalOperationException {
        new TitleAcquisitionServiceImpl().grantTitle(sheet.getCharacter(), title, slot);
    }
}
