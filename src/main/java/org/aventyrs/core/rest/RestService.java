package org.aventyrs.core.rest;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public interface RestService {
    /**
     * PV recovered: Vigor's total value times the Rest's Attribute multiplier, plus any
     * {@link org.aventyrs.core.ability.AttributeAbility#resolveRestHitPointsBonus} bonus (e.g.
     * {@code VigorAbility#METABOLISMO_RAPIDO}'s own +3PV on Longo/Total Rests) across
     * {@code character.getAttributeAbilities()}.
     */
    int getRecoveredHitPoints(Character character, RestType restType);

    /**
     * PM recovered: Foco's total value times the Rest's Attribute multiplier, plus any
     * {@link org.aventyrs.core.ability.AttributeAbility#resolveRestMagicPointsBonus} bonus
     * (e.g. {@code FocusAbility#CANALIZADOR_DE_MANA}'s own +2PM on Longo/Total Rests) across
     * {@code character.getAttributeAbilities()}.
     */
    int getRecoveredMagicPoints(Character character, RestType restType);

    /** PD recovered: Instinto's total value times the Rest's Attribute multiplier. */
    int getRecoveredDeterminationPoints(Character character, RestType restType);

    /**
     * Applies a Rest's PV, PM and PD recovery to the character's sheet in one step, and
     * resolves every {@code org.aventyrs.core.sheet.PendingEgoRecovery} this Rest's tier
     * satisfies (see {@code CharacterSheet#applyPendingEgoRecoveries}) — e.g. {@code
     * org.aventyrs.core.effect.Primor}'s own temporary Ego points owed back on Rest.
     *
     * <p><strong>A Rest deliberately does not refill the temporary Ego pool generally.</strong>
     * Temporary Ego points are recovered per <em>game session</em>, not per Descanso — see
     * {@code org.aventyrs.core.character.services.EgoPointsService}, which this never calls. The
     * only Ego points a Rest returns are the ones some effect specifically promised back, which
     * is exactly what a {@code PendingEgoRecovery} is. Don't "fix" the omission by wiring session
     * recovery in here.
     */
    void applyRest(Character character, CharacterSheet characterSheet, RestType restType);
}
