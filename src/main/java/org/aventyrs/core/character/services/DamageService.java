package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

/**
 * Computes and applies damage mitigation. Two independent flat reductions exist — RD
 * (Redução de Dano), which some attacks/effects can choose to ignore, and RA (Redução
 * Absoluta), which never can — followed by an optional half-damage reduction, applied last
 * (e.g. a partial dodge/parry). Shield points are absorbed separately, inside
 * {@link CharacterSheet#applyDamage}, after this mitigation has already reduced the amount.
 */
public interface DamageService {
    /**
     * The RD an ability grants when its own rules text doesn't spell out an explicit bonus
     * (e.g. just "concede RD" with no number).
     */
    int DEFAULT_DAMAGE_REDUCTION = 2;

    /**
     * Total RD: summed from {@code attributeAbilities}, {@code skillCompetencyAbilities},
     * and the unlocked {@link org.aventyrs.core.skill.SkillExcellency} tiers of every trained
     * Perícia. Never negative.
     */
    int getTotalDamageReduction(Character character);

    /** Total RA, same three sources as RD. Never negative. */
    int getTotalAbsoluteDamageReduction(Character character);

    /**
     * The final damage a hit deals after mitigation. RD (unless {@code
     * ignoreDamageReduction}) and RA are subtracted first and floored at 0; then, if
     * {@code halfDamage}, what remains is halved (rounded down). Never negative.
     */
    int calculateFinalDamage(Character character, int rawDamage, boolean ignoreDamageReduction, boolean halfDamage);

    /**
     * Computes the final damage (see {@link #calculateFinalDamage}) and applies it to the
     * target's CharacterSheet — Shield points are absorbed first, then Hit Points, per
     * {@link CharacterSheet#applyDamage}.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(Character character, CharacterSheet characterSheet, int rawDamage, boolean ignoreDamageReduction, boolean halfDamage);
}
