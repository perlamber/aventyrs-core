package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.scene.ActiveAreaSpellEffect;
import org.aventyrs.core.sheet.CombatantAction;
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

    /**
     * The Magia's {@code Efeito:} line as a ready-to-apply {@link SpellEffect} — {@code null} when
     * the Magia authors none this core can express yet. See {@link
     * SpellCastingService#resolveEffect}.
     *
     * <p><b>Report-only, like {@link #primaryDamage} and {@link #recordedAction}.</b> The effect
     * mutates a sheet when run, but {@code castSpell} never runs it: this core resolves no target
     * GD, so it cannot tell whether the cast landed. The caller decides, then drives it:
     *
     * <pre>{@code
     * InteractionResult r = target.receiveInteraction(result.getSpellEffect());
     * while (r.getNextInteraction() != null) {
     *     r = target.receiveInteraction(r.getNextInteraction());
     * }
     * }</pre>
     *
     * <p>A Corrente de Efeitos the caller judged triggered is chained on first, with {@code
     * AbstractEffect#chainInto} — {@code Sobrecura} is the one this tree needs.
     */
    SpellEffect spellEffect;

    /**
     * The cast bundled as a ready-to-file {@link CombatantAction} — the delivering Perícia, the
     * {@link Spell} as its {@code attackSource} (which is what {@code
     * AttributeAbility#upgradesFirstSpellOfRoundFocusScaling}'s "primeira Magia da Rodada" reads
     * back), and the {@code turnNumber} from the request's Scene. {@code null} on the legacy
     * {@code castSpell(CombatantSheet, Interaction)} overload, which has no Scene.
     *
     * <p>Partial by nature: {@code castSpell} computes the two rolls' bonuses but the caller rolls
     * the dice, so {@code governingAttributeDomain} and the {@code ActionOutcome} verdict are not
     * filled — unlike {@code org.aventyrs.core.combat.DeliveredAttackResult#getRecordedAction()},
     * where the roll is supplied. {@link SpellCastingService#castSpell} does <b>not</b> record it;
     * the caller files it with {@code scene.recordAction(caster, action)}.
     */
    CombatantAction recordedAction;
}
