package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;

import java.util.Optional;

/**
 * Orchestrates a {@link SpellCastRequest}: the caster's delivery roll, followed by their Domínio
 * do Mana roll. The request keeps the spell, targeting, Scene, and resolved scene snapshot
 * together so casting does not discard information that an item, ability, or area effect needs.
 *
 * <p>A lasting {@link SpellReach#AREA_DE_EFEITO} is stored on the request's {@code Scene} as an
 * {@code ActiveAreaSpellEffect}, never on a single {@code CombatantSheet}. Its footprint and the
 * combatants within it remain unresolved: a circle needs positioned participants, and an
 * emanation additionally needs a cast-time facing. An instantaneous area is deliberately not
 * registered, because it has no lasting Scene state to hold.
 *
 * <p>TODO: the service computes each roll's bonuses but does not resolve success/failure, because
 * the delivery roll still lacks a target-GD comparison. A {@link Spell}'s own casting GD is
 * authored data, including target magic-defense floors and target-effect-rung forms, but the
 * comparison stage remains absent.
 */
public interface SpellCastingService {
    /**
     * Casts request's Magia from its caster, deriving both required interactions from the Magia
     * itself and registering any lasting Área de Efeito on request's Scene.
     */
    SpellCastingResult castSpell(SpellCastRequest request);

    /**
     * Legacy interaction-driven entry point. Prefer {@link #castSpell(SpellCastRequest)} so the
     * caster, Magia, targeting, and Scene state remain available to the cast.
     */
    SpellCastingResult castSpell(CombatantSheet target, Interaction<CombatantSheet> deliveryInteraction);

    /**
     * The primary damage {@code spell} would deal cast by {@code caster} right now, or {@link
     * Optional#empty()} for a Magia that authors no {@link Spell#getPrimaryDamage()}.
     *
     * <p>{@code deterministicAmount} is the Magia's flat bonus plus its Foco term — {@code
     * Foco/2} for a {@code "Metade do Foco"} effect, upgraded to full {@code Foco} when this
     * would be {@code caster}'s first Magia of the Rodada ({@code
     * caster.getActionsThisRound()} holds no {@code Spell} action) <b>and</b> {@code caster} holds
     * an ability whose {@code AttributeAbility#upgradesFirstSpellOfRoundFocusScaling()} is true
     * ({@code FocusAbility#MAGIA_PODEROSA}). The {@code diceCount} d6 stay the caller's to roll —
     * this core never rolls dice.
     *
     * <p>This is a pure read; {@link #castSpell(SpellCastRequest)} calls it and puts the result
     * on {@link SpellCastingResult#getPrimaryDamage()}.
     */
    Optional<ResolvedSpellDamage> resolvePrimaryDamage(Spell spell, CombatantSheet caster);
}
