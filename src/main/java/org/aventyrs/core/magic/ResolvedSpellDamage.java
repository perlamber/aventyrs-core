package org.aventyrs.core.magic;

import org.aventyrs.core.character.DamageType;

/**
 * A {@link SpellDamage} resolved against a concrete caster — what {@code
 * SpellCastingService#resolvePrimaryDamage} returns and {@code SpellCastingResult} carries.
 *
 * <p>{@code deterministicAmount} is {@code flatBonus} plus the Foco contribution (half or full),
 * already computed. {@code diceCount} d6 are still the caller's to roll and add — this core never
 * rolls — so the total a target takes is {@code deterministicAmount + <rolled Nd6>}, fed into an
 * ordinary {@code DamageInteraction} with {@code damageType}/{@code elementalType} for mitigation.
 *
 * <p>{@code focusFullyApplied} is {@code true} when a {@link FocusScaling#HALF} term was upgraded
 * to full by {@code FocusAbility#MAGIA_PODEROSA} (the caster's first Magia of the Rodada); it is
 * also {@code true} for a natively {@link FocusScaling#FULL} term and {@code false} for {@link
 * FocusScaling#NONE}.
 */
public record ResolvedSpellDamage(int deterministicAmount, int diceCount,
                                  DamageType damageType, ElementalType elementalType,
                                  boolean focusFullyApplied) {
}
