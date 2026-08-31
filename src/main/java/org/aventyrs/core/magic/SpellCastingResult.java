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
}
