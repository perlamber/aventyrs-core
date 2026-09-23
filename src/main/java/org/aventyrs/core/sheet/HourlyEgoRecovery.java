package org.aventyrs.core.sheet;

import org.aventyrs.core.character.EgoDomain;

/**
 * Temporary Ego points owed back one every hoursPerPoint hours of in-game time — Frenesi's "Pontos de
 * Autocontrole perdidos durante o 'Frenesi' são recuperados 1 a cada 2 horas passadas fora deste
 * estado". bankedHours carries the hours that have passed but not yet earned a whole point.
 *
 * <p>Public so a caller persisting a sheet between sessions can read it ({@link
 * CombatantSheet#getHourlyEgoRecoveries()}) and hand it back ({@link
 * CombatantSheet#restoreHourlyEgoRecovery}).
 */
public record HourlyEgoRecovery(EgoDomain domain, int points, int hoursPerPoint, int bankedHours) {

    public HourlyEgoRecovery {
        if (hoursPerPoint < 1) {
            throw new IllegalArgumentException("A recovery needs at least one hour per point: " + hoursPerPoint);
        }
    }

    HourlyEgoRecovery withPoints(final int remaining) {
        return new HourlyEgoRecovery(domain, remaining, hoursPerPoint, bankedHours);
    }
}
