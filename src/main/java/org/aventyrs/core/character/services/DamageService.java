package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.SkillExcellency;

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
     * (e.g. just "concede RD" with no number). Also reused as RA's own unspecified-bonus
     * default (e.g. {@code InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s "você recebe RA" with no
     * number of its own) — RD and RA are independent reductions, but nothing in the rules text
     * suggests a different unspecified-amount convention for one versus the other.
     */
    int DEFAULT_DAMAGE_REDUCTION = 2;

    /**
     * Total RD: summed from {@code attributeAbilities}, {@code skillCompetencyAbilities},
     * and the unlocked {@link org.aventyrs.core.skill.SkillExcellency} tiers of every trained
     * Perícia. Never negative.
     */
    int getTotalDamageReduction(Character character);

    /**
     * Same as {@link #getTotalDamageReduction(Character)}, but also summing every held {@code
     * AttributeAbility}'s own {@code resolveDamageReduction(DamageType, CharacterSheet source,
     * CharacterSheet target)} — e.g. {@code VigorAbility#RIGIDEZ_DA_MONTANHA}'s Dano-Físico-only,
     * attacker-size-conditioned reduction. target is character's own {@link CharacterSheet} —
     * required whenever an ability needs its own holder's data (e.g. {@code SizeCategory}) to
     * decide; damageType/source may be {@code null} (unclassified damage / no known attacker),
     * same restraint every other Scene/roll-conditioned {@code resolve*} hook in this core
     * already applies. Never negative.
     */
    int getTotalDamageReduction(Character character, CharacterSheet target, DamageType damageType, CharacterSheet source);

    /** Total RA, same three sources as RD. Never negative. */
    int getTotalAbsoluteDamageReduction(Character character);

    /**
     * Same as {@link #getTotalAbsoluteDamageReduction(Character)}, but also summing every
     * held {@code EgoAdvantage}'s own {@code resolveAbsoluteDamageReduction(sceneContext)} —
     * e.g. {@code InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s RA during a Cena de Combate's
     * first two Rounds. sceneContext may be {@code null} (no active Scene, or this damage
     * isn't happening in an encounter) — every {@code EgoAdvantage}'s own override is expected
     * to treat that as "condition not met," the same restraint {@code
     * EgoAdvantage#resolveConditionalRollBonus}/{@code #resolveDamageBonus} already apply.
     * Never negative.
     */
    int getTotalAbsoluteDamageReduction(Character character, SceneContext sceneContext);

    /**
     * The final damage a hit deals after mitigation. RD (unless {@code
     * ignoreDamageReduction}) and RA are subtracted first and floored at 0; then, if
     * {@code halfDamage}, what remains is halved (rounded down). Never negative.
     */
    int calculateFinalDamage(Character character, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #calculateFinalDamage(Character, int, boolean)}, but also folding in
     * every held {@code EgoAdvantage}'s own Scene-conditioned RA ({@link
     * #getTotalAbsoluteDamageReduction(Character, SceneContext)}) and half-damage ({@code
     * EgoAdvantage#resolveHalfDamage(sceneContext)}, additive with the existing {@code
     * ModifierType#HALF_DAMAGE} reflection scan — either source alone is enough to halve).
     * sceneContext may be {@code null}, same as {@link #getTotalAbsoluteDamageReduction(Character,
     * SceneContext)}.
     */
    int calculateFinalDamage(Character character, SceneContext sceneContext, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #calculateFinalDamage(Character, SceneContext, int, boolean)}, but also
     * folding in {@link #getTotalDamageReduction(Character, CharacterSheet, DamageType,
     * CharacterSheet)}'s type/source-scoped RD. target/damageType/source carry the same meaning
     * as that method's own — target is character's own {@link CharacterSheet}, damageType/
     * source may be {@code null}.
     */
    int calculateFinalDamage(Character character, CharacterSheet target, SceneContext sceneContext,
                              DamageType damageType, CharacterSheet source, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Computes the final damage (see {@link #calculateFinalDamage}) and applies it to the
     * target's CharacterSheet — Shield points are absorbed first, then Hit Points, per
     * {@link CharacterSheet#applyDamage}.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(Character character, CharacterSheet characterSheet, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #applyDamage(Character, CharacterSheet, int, boolean)}, but also folding
     * in sceneContext-conditioned mitigation — see {@link #calculateFinalDamage(Character,
     * SceneContext, int, boolean)}.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(Character character, CharacterSheet characterSheet, SceneContext sceneContext, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #applyDamage(Character, CharacterSheet, SceneContext, int, boolean)}, but
     * also folding in damageType/source-scoped RD — see {@link #calculateFinalDamage(Character,
     * CharacterSheet, SceneContext, DamageType, CharacterSheet, int, boolean)}. characterSheet
     * doubles as that method's own target parameter, since the sheet being damaged and the
     * ability holder whose RD is being resolved are the same one here.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(Character character, CharacterSheet characterSheet, SceneContext sceneContext,
                     DamageType damageType, CharacterSheet source, int rawDamage, boolean ignoreDamageReduction);
}
