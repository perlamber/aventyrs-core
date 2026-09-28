package org.aventyrs.core.sheet;

import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.Dice;

import java.util.UUID;

/**
 * Dice a {@link RecurringDice} owes this Rodada, waiting for the caller to roll them. This core
 * never rolls: the caller reads {@link CombatantSheet#getPendingDiceRolls()}, rolls {@link #dice()},
 * and hands the faces to {@link CombatantSheet#resolveDiceRoll}, which applies them in full.
 *
 * @param descriptor the damage's kind, for a {@link RecurringDice.Kind#DAMAGE} roll — so RD, RE and
 *                   immunities reach it; {@code null} for a heal
 */
public record PendingDiceRoll(@NonNull UUID id, @NonNull RecurringDice.Kind kind, @NonNull Dice dice,
                              DamageDescriptor descriptor, String source) {
}
