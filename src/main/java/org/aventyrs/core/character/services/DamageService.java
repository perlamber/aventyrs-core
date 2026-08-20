package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
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
 *
 * <p>Every method here that has an actual {@link CharacterSheet} to work with takes that
 * sheet directly rather than a separate {@code Character} alongside it — a {@code
 * CharacterSheet} already carries its own {@code Character} via {@link
 * CharacterSheet#getCharacter()}, so a caller with a sheet in hand has nothing to gain from
 * also being asked to pass the {@code Character} it already implies. The plain
 * {@code Character}-only overloads that remain (no {@code CharacterSheet} parameter at all)
 * exist specifically for callers that genuinely have no concrete sheet yet (e.g. a bare
 * damage-preview computation) — those simply can't contribute whatever a sheet-dependent
 * source (like a Título ability's own PV comparison) would have added.
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
     * attacker-size-conditioned reduction. target is the character whose RD this computes —
     * its own data (e.g. {@code SizeCategory}) is what a held ability's condition may need;
     * damageType/source may be {@code null} (unclassified damage / no known attacker), same
     * restraint every other Scene/roll-conditioned {@code resolve*} hook in this core already
     * applies. Never negative.
     */
    int getTotalDamageReduction(CharacterSheet target, DamageType damageType, CharacterSheet source);

    /** Total RA, same three sources as RD. Never negative. */
    int getTotalAbsoluteDamageReduction(Character character);

    /**
     * Same as {@link #getTotalAbsoluteDamageReduction(Character)}, but also summing every
     * held {@code EgoAdvantage}'s own {@code resolveAbsoluteDamageReduction(sceneContext)} —
     * e.g. {@code InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s RA during a Cena de Combate's
     * first two Rounds — and every held {@code AventyrTitleAbility}'s own {@code
     * resolveAbsoluteDamageReduction} — e.g. Santo's Bastião dos Necessitados. sceneContext
     * may be {@code null} (no active Scene, or this damage isn't happening in an encounter),
     * same restraint every other Scene-conditioned {@code resolve*} hook in this core already
     * applies. Takes target's own {@link CharacterSheet} directly rather than a separate
     * {@code Character} (see this interface's own javadoc for why) — required specifically so
     * the Título-ability source can compare the holder's own current PV against each adjacent
     * ally's (via {@code HitPointsService}), the one extra fact its condition needs that
     * {@code EgoAdvantage}'s identically-shaped hook never did. There is deliberately no
     * sheet-less overload of this one (unlike {@link #getTotalDamageReduction(Character)}/
     * {@link #calculateFinalDamage(Character, int, boolean)}'s own bare-preview forms) — every
     * real caller needing Scene-conditioned RA already has a concrete {@code CharacterSheet}
     * in hand by that point (an attack is always against *someone*), so there was no genuine
     * sheet-less use case left to preserve once {@link #calculateFinalDamage(Character,
     * SceneContext, int, boolean)}'s own sheet-less overload stopped depending on this method
     * publicly. Never negative.
     */
    int getTotalAbsoluteDamageReduction(CharacterSheet target, SceneContext sceneContext);

    /**
     * The final damage a hit deals after mitigation. RD (unless {@code
     * ignoreDamageReduction}) and RA are subtracted first and floored at 0; then, if
     * {@code halfDamage}, what remains is halved (rounded down). Never negative.
     */
    int calculateFinalDamage(Character character, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #calculateFinalDamage(Character, int, boolean)}, but also folding in
     * every held {@code EgoAdvantage}'s own Scene-conditioned RA and half-damage ({@code
     * EgoAdvantage#resolveHalfDamage(sceneContext)}, additive with the existing {@code
     * ModifierType#HALF_DAMAGE} reflection scan — either source alone is enough to halve).
     * No {@code CharacterSheet} in hand here, so — unlike {@link
     * #calculateFinalDamage(CharacterSheet, SceneContext, DamageType, CharacterSheet, int,
     * boolean)} below — this can't also contribute a Título ability's own RA. sceneContext may
     * be {@code null} (no active Scene, or this damage isn't happening in an encounter), same
     * restraint every other Scene-conditioned {@code resolve*} hook in this core already
     * applies.
     */
    int calculateFinalDamage(Character character, SceneContext sceneContext, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #calculateFinalDamage(Character, SceneContext, int, boolean)}, but also
     * folding in {@link #getTotalDamageReduction(CharacterSheet, DamageType, CharacterSheet)}'s
     * type/source-scoped RD and {@link #getTotalAbsoluteDamageReduction(CharacterSheet,
     * SceneContext)}'s Título-ability source — both need a real {@link CharacterSheet}, which
     * this overload has (see this interface's own javadoc for why target replaces the separate
     * {@code Character} parameter). damageType/source may still be {@code null}.
     */
    int calculateFinalDamage(CharacterSheet target, SceneContext sceneContext,
                              DamageType damageType, CharacterSheet source, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Recomputes characterSheet's owner's {@link CharacterStatus} from the damage currently
     * accumulated on that sheet — {@code HitPointsService#getStatus} against the
     * <em>unclamped</em> current Hit Points ({@code getMaxHitPoints - getDamageTaken}, which
     * may be negative, since that negative range is exactly what distinguishes {@code
     * FALLEN}/{@code COMMA}/{@code DEAD}) — and stores it on the {@code Character} via
     * {@code Character#updateStatus}.
     *
     * <p>Called automatically by every {@link #applyDamage} overload, so a caller going
     * through this service never needs to invoke it. It's public for the one caller that
     * mitigates and applies damage in two separate steps instead — {@code
     * org.aventyrs.core.effect.DamageInteraction}, which needs this hit's own final damage
     * (for {@code InteractionResult#getResourceLossValue()} and its next-stage gate), a value
     * {@code applyDamage}'s "total accumulated so far" return can't give it.
     *
     * @return CharacterStatus the freshly resolved status, also now stored on the Character
     */
    CharacterStatus refreshStatus(CharacterSheet characterSheet);

    /**
     * Computes the final damage (see {@link #calculateFinalDamage}) and applies it to
     * characterSheet — Shield points are absorbed first, then Hit Points, per
     * {@link CharacterSheet#applyDamage} — then refreshes the damaged {@code Character}'s own
     * {@link CharacterStatus} to match, via {@link #refreshStatus(CharacterSheet)}. No
     * separate {@code Character} parameter — applying damage always requires a concrete
     * {@code CharacterSheet} to mutate, so there's never a sheet-less case to support here,
     * unlike {@code calculateFinalDamage}'s own bare-preview overloads.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(CharacterSheet characterSheet, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #applyDamage(CharacterSheet, int, boolean)}, but also folding in
     * sceneContext-conditioned mitigation — see {@link #calculateFinalDamage(CharacterSheet,
     * SceneContext, DamageType, CharacterSheet, int, boolean)}.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(CharacterSheet characterSheet, SceneContext sceneContext, int rawDamage, boolean ignoreDamageReduction);

    /**
     * Same as {@link #applyDamage(CharacterSheet, SceneContext, int, boolean)}, but also
     * folding in damageType/source-scoped RD — see {@link #calculateFinalDamage(CharacterSheet,
     * SceneContext, DamageType, CharacterSheet, int, boolean)}. characterSheet doubles as that
     * method's own target parameter, since the sheet being damaged and the ability holder
     * whose RD is being resolved are the same one here.
     * @return int total damage accumulated on the target's Hit Points so far
     */
    int applyDamage(CharacterSheet characterSheet, SceneContext sceneContext,
                     DamageType damageType, CharacterSheet source, int rawDamage, boolean ignoreDamageReduction);
}
