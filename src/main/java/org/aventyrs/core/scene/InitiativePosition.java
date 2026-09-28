package org.aventyrs.core.scene;

/**
 * Where a combatant stands in the Rodada's order of play — {@code EscudeiroFeat#INICIO_DEFENSIVO}'s
 * "se você não for o primeiro a agir … Se você for o último a agir". Read off {@code
 * Scene#getParticipantsInInitiativeOrder()} by {@code Scene#buildContext}. A combatant alone in the
 * order is {@link #FIRST}: it is not "não o primeiro". {@link #UNKNOWN} for a context built outside a
 * Scene or for someone not in the order yet.
 */
public enum InitiativePosition {
    FIRST,
    MIDDLE,
    LAST,
    UNKNOWN;

    /** Whether someone acts before this position — "não for o primeiro a agir". */
    public boolean isAfterFirst() {
        return this == MIDDLE || this == LAST;
    }
}
