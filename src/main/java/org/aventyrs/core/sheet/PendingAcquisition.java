package org.aventyrs.core.sheet;

/**
 * Something a character has already earned but receives only when a game session ends — {@code
 * DestinoFeat#DESPERTAR_ANTECIPADO}'s "desperta seu Título Primário ao fim da primeira sessão de
 * Jogo" is the first one. {@link CharacterSheet#applySessionEndAcquisitions()} performs every one
 * owed.
 *
 * <p><b>Derived, never stored.</b> A held trait reports what it still owes ({@code
 * Feat#resolveSessionEndAcquisitions}), and stops reporting it once the acquisition has happened —
 * the awakened Título is in its slot, so nothing is owed any more. That is why the sheet keeps no
 * list of these, and why there is nothing to persist or to clear: a reloaded sheet owes exactly
 * what its Talentos say it owes. It is also what makes a repeated session-end call harmless.
 */
@FunctionalInterface
public interface PendingAcquisition {

    /**
     * Grants the acquisition to sheet's character.
     *
     * @throws IllegalOperationException if it can no longer be granted (a Título another held
     *         Talento now forbids, say)
     */
    void acquire(CharacterSheet sheet) throws IllegalOperationException;
}
