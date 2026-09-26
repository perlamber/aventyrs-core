package org.aventyrs.core.monster;

import lombok.NonNull;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * A Habilidade Monstruosa a monster holds, with the pick it made when it took it — see {@link
 * MonstrousAbility#getChoiceOptions()}. {@code choice} is {@code null} for a Habilidade that asks
 * for nothing.
 */
public record MonstrousAbilitySelection(@NonNull MonstrousAbility ability, String choice) {

    public static MonstrousAbilitySelection of(@NonNull final MonstrousAbility ability) {
        return new MonstrousAbilitySelection(ability, null);
    }

    public static MonstrousAbilitySelection of(@NonNull final MonstrousAbility ability, final String choice) {
        return new MonstrousAbilitySelection(ability, choice);
    }
}
