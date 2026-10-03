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
     * The PM request would cost — what {@link #castSpell(SpellCastRequest)} reports as {@link
     * SpellCastingResult#getManaCost()} before a {@code payManaWithHitPoints} swap, for the version it
     * would cast. A pure read, so a caller can refuse an unaffordable cast before anything happens
     * ({@code castSpell} registers an area's effect and spends banked charges).
     */
    int resolveManaCost(SpellCastRequest request);

    /**
     * spell's Tempo de Ativação as caster would pay it right now, in the Scene's 0-based
     * currentRound: the authored {@link Spell#getActivationTime()}, less the summed {@code
     * Feat#resolveCastingActionPointReduction} when it is a Pontos de Ação cost, never below 1PA.
     * A Reação or Ação Livre is returned unchanged. A pure read — nothing is spent.
     */
    ActivationTime resolveActivationTime(Spell spell, CombatantSheet caster, int currentRound);

    /**
     * {@link #resolveActivationTime(Spell, CombatantSheet, int)} for a cast whose caster opted into
     * activatedFeats ({@code SpellCastRequest#getActivatedFeats}).
     */
    ActivationTime resolveActivationTime(Spell spell, CombatantSheet caster, int currentRound,
                                         java.util.Set<org.aventyrs.core.feat.Feat> activatedFeats);

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

    /**
     * Whether a Conjuração roll totalling castTotal cast the Magia result reports (core 0.0.93): it reaches {@code
     * SpellCastingResult#getCastingTargetValue()} (a tie casts it), or no GD was stated.
     */
    boolean castSucceeds(SpellCastingResult result, int castTotal);

    /**
     * Whether the cast's Corrente de Efeitos fires on target (core 0.0.93): the Conjuração succeeded and cleared the
     * target value by {@code EffectChainService#getRequiredMargin} (Resoluto raises it). A cast with no stated GD has no
     * margin, and fires nothing.
     */
    boolean isEffectChainTriggered(SpellCastingResult result, CombatantSheet target, int castTotal);

    /**
     * The Corrente the cast version builds once it fires ({@code Spell#getEffectChainKind()}), its die thrown on roller
     * — empty for a version with none. Chain it onto that target's effect with {@code AbstractEffect#chainInto}.
     */
    default Optional<org.aventyrs.core.effect.EffectChain> resolveEffectChain(Spell castVersion, CombatantSheet caster,
                                                                               org.aventyrs.core.util.DiceRoller roller) {
        return resolveEffectChain(castVersion, org.aventyrs.core.effect.SpellEffectContext.of(false, caster), roller);
    }

    /**
     * The Corrente this cast builds once it fires (core 0.1.0) — the longest form: context says who cast it, the
     * "Força ou Destreza" pick (Inflar o Ego), the resolved Duração every timed Corrente lasts, and whether the caster
     * aimed for the version's Corrente de Efeitos Alternativa (Serra-Pernas's Fraqueza Momentânea) instead of its
     * Corrente.
     */
    Optional<org.aventyrs.core.effect.EffectChain> resolveEffectChain(Spell castVersion,
                                                                       org.aventyrs.core.effect.SpellEffectContext context,
                                                                       org.aventyrs.core.util.DiceRoller roller);

    /**
     * The Efeito Crítico a Conjuração roll's critical success puts on whoever the Magia lands on (core 0.0.93) — the
     * version's own {@code getCriticalEffectType()} (Amenizar, Imunizar, Potencializar…), at the roll's severity. Empty
     * for a roll that is no critical success or a version with no Efeito Crítico. Build one per target.
     */
    Optional<org.aventyrs.core.effect.CriticalEffect> resolveCastingCriticalEffect(
            Spell castVersion, CombatantSheet caster, org.aventyrs.core.skill.SkillRoll castingRoll,
            org.aventyrs.core.util.DiceRoller roller);
}
