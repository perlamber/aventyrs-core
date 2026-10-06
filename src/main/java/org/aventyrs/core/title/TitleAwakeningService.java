package org.aventyrs.core.title;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * The ordinary Despertar: a character awakens their Título Primário at {@value
 * #PRIMARY_AWAKENING_EXPERIENCE} total EXP and their Secundário at {@value
 * #SECONDARY_AWAKENING_EXPERIENCE}, picking the Título at that moment. (The Terciário has no EXP
 * mark and is not offered here.)
 *
 * <p>The Talentos that bend this are honoured, not restated: a slot a Talento already awakens with
 * its own pick ({@code Feat#presetsTitleAwakening} — Despertar Antecipado, Centelha Gran-Aventyr
 * Antecipada) gets no picker; a slot a held Talento or Defeito closes or delays ({@code
 * Feat#permitsTitleSlot} — Herança de Gilgamesh, Centelha Dormente's 25 EXP, Abdicador) is not
 * due until it opens; and a Título needs a Centelha left to awaken from. Atrasar Despertar's "apenas
 * quando quiser e se quiser" is {@link #mayPostpone} — the caller decides what postponing looks
 * like; nothing is stored.
 */
public interface TitleAwakeningService {

    /** "Ao atingir 15 EXP" — the Título Primário. */
    int PRIMARY_AWAKENING_EXPERIENCE = 15;

    /** "Ao atingir 30 EXP" — the Título Secundário. */
    int SECONDARY_AWAKENING_EXPERIENCE = 30;

    /** The ordinary total-EXP mark for slot, or empty for a slot with none (the Terciário). */
    static Optional<BigDecimal> awakeningExperience(final TitleSlot slot) {
        return switch (slot) {
            case PRIMARY -> Optional.of(BigDecimal.valueOf(PRIMARY_AWAKENING_EXPERIENCE));
            case SECONDARY -> Optional.of(BigDecimal.valueOf(SECONDARY_AWAKENING_EXPERIENCE));
            case TERTIARY -> Optional.empty();
        };
    }

    /**
     * The slot sheet's character may awaken a Título into right now, picked by the player — or empty.
     * Slots fill in order: the Secundário is never due while the Primário is empty.
     */
    Optional<TitleSlot> dueSlot(CharacterSheet sheet);

    /**
     * Every Título the character may awaken into slot: each {@link TitleCatalog} family they don't
     * already hold that {@link TitleAcquisitionService#isPermitted} allows for that slot.
     */
    List<AventyrTitle> optionsFor(CharacterSheet sheet, TitleSlot slot);

    /** Atrasar Despertar: the holder may decline a due Despertar for now (and later, indefinitely). */
    boolean mayPostpone(Character character);

    /**
     * Awakens title into the {@link #dueSlot}, through {@link TitleAcquisitionService#grantTitle}.
     * title may already carry the Especializações and Habilidades the player picked.
     *
     * @return the slot it was awakened into
     * @throws IllegalOperationException {@code TITLE_AWAKENING_NOT_DUE} when no slot is due,
     *         {@code TITLE_AWAKENING_CHOICE_NOT_ELIGIBLE} when title isn't one {@link #optionsFor} offers
     */
    TitleSlot awaken(CharacterSheet sheet, AventyrTitle title) throws IllegalOperationException;
}
