package org.aventyrs.core.title.bruxo;

import lombok.NonNull;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.subordinate.SubordinateBenefit;

/**
 * A Bruxo's Familiar Maior, as the ritual bound it — "pelo resto de sua vida", and "Familiares Maiores são
 * Subordinado e podem ter quaisquer tipos, definido na invocação (a escolha não pode ser alterada)". Table ruling
 * (2026-10-05): the "tipo" is its Subordinado grade, picked at the ritual with one of that grade's two benefits (the
 * same pick every Subordinado makes); the creature itself is a non-combat token ({@link FamiliarTemplate}).
 *
 * @param name         what the player calls it
 * @param creatureType "normalmente um pequeno Animal Elemental, Abissal ou Celestial" — any type is accepted
 * @param benefit      its Subordinado benefit, which names its grade
 */
public record Familiar(@NonNull String name, @NonNull CreatureType creatureType, @NonNull SubordinateBenefit benefit) {
}
