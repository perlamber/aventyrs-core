package org.aventyrs.core.sheet;

import lombok.NonNull;

/**
 * A Condição an activation cast on someone — who, which, and for how many Rodadas — reported on
 * {@link InteractionResult#getInflictedConditions()} so a caller can hand it to the client that owns
 * the target's real sheet.
 */
public record InflictedCondition(@NonNull CombatantSheet target, @NonNull ConditionType type, int rounds) {
}
