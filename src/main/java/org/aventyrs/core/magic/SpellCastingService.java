package org.aventyrs.core.magic;

import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.effect.SpellEffectContext;
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

    /**
     * How many níveis caster's held Talentos take off spell's GD da Conjuração — the summed {@code
     * Feat#resolveCastingDifficultyReduction}. {@link #castSpell(SpellCastRequest)} reports it, and
     * the Magia's authored tier eased by it, on {@link SpellCastingResult}. A pure read.
     */
    int resolveCastingDifficultyReduction(Spell spell, CombatantSheet caster);

    /**
     * spell's Tempo de Ativação as caster would pay it right now, in the Scene's 0-based
     * currentRound: the authored {@link Spell#getActivationTime()}, less the summed {@code
     * Feat#resolveCastingActionPointReduction} when it is a Pontos de Ação cost, never below 1PA.
     * A Reação or Ação Livre is returned unchanged. A pure read — nothing is spent.
     */
    ActivationTime resolveActivationTime(Spell spell, CombatantSheet caster, int currentRound);

    /**
     * {@code spell}'s {@code Efeito:} line as an applicable {@link SpellEffect}, or {@link
     * Optional#empty()} for a Magia whose effect this core cannot yet express — which is still
     * most of the catalog.
     *
     * <p>Built from the Magia's own authored columns: a {@link Spell#getHealing()} becomes a
     * {@code SpellHealingEffect}, a non-empty {@link Spell#getCleansedConditions()} a {@code
     * ConditionCleansingEffect}. Both are parameterized by that data rather than chosen per
     * constant, which is what lets one class serve a whole ramificação as it deepens.
     *
     * <p><b>Nothing is applied.</b> Like {@link #resolvePrimaryDamage} this is a pure read;
     * {@link #castSpell(SpellCastRequest)} calls it and puts the effect on {@link
     * SpellCastingResult#getSpellEffect()} for the caller to run through {@code
     * CombatantSheet#receiveInteraction} when it decides the cast landed. This core resolves no
     * target GD, so it is in no position to decide that itself.
     *
     * <p>{@code context} carries the per-cast facts a Magia's own columns cannot — today just
     * whether the target is hostile, which answers Nova Rejuvenescedora's "Inimigos do conjurador
     * recuperam apenas metade". The caller's to say, since resolving an Área de Efeito footprint
     * into a set of combatants is not something this core does.
     *
     * <p>{@code spell} is whichever version is being cast — which is also what settles <em>which
     * effect</em>, since a Magia and its {@code Efeito Alternativo} carry separate effect columns.
     * {@link #castSpell(SpellCastRequest)} resolves that from {@code
     * SpellCastRequest#isUseAlternateVersion()} before calling here.
     *
     * <p>A delegate: the construction itself lives in {@link
     * org.aventyrs.core.effect.SpellEffectFactory}, so this service holds no effect-building
     * collaborator of its own and a new effect category needs no change here.
     */
    Optional<SpellEffect> resolveEffect(Spell spell, SpellEffectContext context);
}
