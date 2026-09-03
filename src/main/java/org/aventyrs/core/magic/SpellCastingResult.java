package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.scene.ActiveAreaSpellEffect;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The outcome of {@link SpellCastingService#castSpell} — the two rolls a Magia's casting
 * involves: whichever Perícia delivered it (e.g. Ataque à Distância for a ranged spell) and
 * the follow-up Domínio do Mana roll.
 */
@Getter
@Builder
public class SpellCastingResult {
    InteractionResult deliveryResult;
    InteractionResult dominioDoManaResult;
    Integer durationInRounds;
    ActiveAreaSpellEffect areaSpellEffect;

    /**
     * The Magia's primary damage resolved against the caster — {@code null} when the Magia
     * authors no {@link SpellDamage}. The {@code deterministicAmount} is ready; the caller still
     * rolls the {@code diceCount} d6, adds them, and runs its own {@code DamageInteraction} with
     * the type/element for mitigation. See {@link SpellCastingService#resolvePrimaryDamage}.
     */
    ResolvedSpellDamage primaryDamage;
}
