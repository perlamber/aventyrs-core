package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.artes.ArtesExcellency;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * The outcome of an {@link Interaction} applied via {@link Interactable#receiveInteraction}.
 * Different Interactions fill in different fields — e.g. a damage-dealing Interaction sets
 * {@code resultStatus}, while a Perícia test sets {@code skillRollBonus} — the rest stay
 * {@code null} when not applicable to that particular Interaction.
 */
@Getter @Builder(toBuilder = true)
public class InteractionResult {
    Interactable nextInteractable;
    CharacterStatus resultStatus;

    /** The Perícia roll bonus computed by a skill-test Interaction (e.g. AttentionInteraction). */
    Integer skillRollBonus;

    /**
     * The {@link AttributeDomain} that actually governed this Perícia roll, <b>after</b> every
     * substitution (Perito Teórico / {@code SkillCompetencyAbility} / {@code AttackSource}).
     * {@code null} unless a {@code org.aventyrs.core.skill.SkillRoll} was supplied. Reported so a
     * caller can build a {@link CombatantAction} for {@link CombatantSheet#recordAction} without
     * re-deriving the resolution — same stays-{@code null}-when-not-applicable convention as
     * every other field here.
     */
    AttributeDomain governingAttributeDomain;

    /**
     * Total GD (DifficultyLevel) steps reduced for this Perícia test, aggregated from
     * whatever's currently known on the CombatantSheet — for now, only the trained Skill's
     * unlocked {@link org.aventyrs.core.skill.SkillExcellency} tiers (e.g.
     * {@link org.aventyrs.core.skill.artes.ArtesExcellency#PRODIGIO}). More sources (Talentos,
     * temporary buffs, etc.) would add to this same total as they're built.
     */
    Integer difficultyReduction;

    /**
     * Every temporary bonus this roll/activation produced for *someone else* to receive — e.g.
     * {@code ArtesCompetencyAbility#DOM_BARDICO} (one entry, {@code TargetScope#ALLIES}) or
     * {@code org.aventyrs.core.title.santo.GritoDeGuerraVulcanoInteraction} (multiple entries
     * at once — two Vantagem bonuses plus a Defesas one, all {@code
     * TargetScope#SELF_AND_ALLIES}) — who actually receives each one is resolved by a layer
     * above this core, via {@code org.aventyrs.core.scene.Scene#getAllies}/{@code #getEnemies}
     * or the actor itself for {@code TargetScope#SELF}/{@code SELF_AND_ALLIES}. {@code null}
     * when this Interaction didn't grant any — same stays-{@code null}-when-not-applicable
     * convention as every other field here, same as {@link #egoGainDomains}'s own null-vs-empty
     * distinction: {@code null} means "not applicable," a non-null (possibly empty) list means
     * "this Interaction is the kind that can grant these, here's what it computed this time."
     * When non-{@code null}, a caller is expected to call {@code CombatantSheet
     * #grantTemporaryBonus} on each intended recipient with each {@link Blessing}'s own
     * {@code modifierType}/{@code value}/{@code rounds}.
     *
     * <p>A {@link Blessing} is a small, self-contained value object (not four parallel fields
     * here) specifically so more than one can be reported from a single Interaction without
     * ambiguity about which value pairs with which type/duration/scope — an earlier design had
     * exactly that shape (singular {@code temporaryBonusValue}/{@code temporaryBonusModifierType}/
     * {@code temporaryBonusRounds}/{@code temporaryBonusScope} fields), which worked for
     * DOM_BARDICO's own single grant but had no way to represent Grito de Guerra Vulcano's
     * three simultaneous ones.
     */
    List<Blessing> blessings;

    /**
     * The highest GD this roll reached — {@code null} unless the Interaction was given a
     * {@code org.aventyrs.core.skill.SkillRoll} to compute it from (see {@code
     * AbstractSkillInteraction#applyTo(CombatantSheet, org.aventyrs.core.scene.SceneContext,
     * org.aventyrs.core.skill.SkillRoll)}) — this core never rolls dice itself, so without an
     * already-rolled {@code SkillRoll} handed in, there's nothing to resolve a tier from.
     * Computed from {@code skillRollBonus + skillRoll.getTotal()} against {@link
     * DifficultyLevel#reachedBy}, e.g. what {@code ArtesCompetencyAbility#DOM_BARDICO} needs
     * to look up its own bonus value.
     */
    DifficultyLevel reachedDifficultyLevel;

    /**
     * Whether this roll <b>beat the Grau de Dificuldade it was made against</b> — {@code true}
     * on a tie or better, since a total equal to the threshold succeeds (the same reading {@code
     * org.aventyrs.core.combat.AttackReceiver} takes of a defence, where "a tie is a successful
     * defense").
     *
     * <p>{@code null} when nobody said what the roll was against — either no {@code SkillRoll}
     * was handed in at all, or one with no {@code targetValue}. <b>That is a third answer, not a
     * failure</b>: an ability gated on success must read {@code null} as "cannot tell", never as
     * "failed", the same three-state discipline {@link #reachedDifficultyLevel} already uses.
     *
     * <p>Any {@link #difficultyReduction} the roller holds is applied to the target before
     * comparing, so a GD-easing trait genuinely moves the bar.
     */
    Boolean succeeded;

    /**
     * By how much this roll beat its target — positive on a success, negative on a failure, and
     * {@code null} whenever {@link #succeeded} is. The margin several clauses need ("se exceder a
     * GD em 5 ou mais"), reported rather than recomputed by each caller.
     */
    Integer margin;

    /**
     * The critical outcome of the {@code SkillRoll} this Interaction was given, or {@code
     * null} for the same reason {@link #reachedDifficultyLevel} is — see {@link
     * CriticalResult}.
     */
    CriticalResult criticalResult;

    /**
     * A bonus this roll grants toward a dano roll it's about to deliver — e.g. {@code
     * org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility#FRIEZA}'s
     * Vantagem against a nearby target. {@code null} when this Interaction didn't grant one,
     * same stays-{@code null}-when-not-applicable convention as every other field here — a
     * caller adds {@link DamageBonus#getValue()} to its own already-rolled dano total when
     * present.
     */
    DamageBonus damageBonus;

    /**
     * The amount of {@link #resourceLossType} this Interaction drained — e.g. a {@code
     * org.aventyrs.core.effect.DamageInteraction}'s post-mitigation Hit Point damage (see
     * {@code org.aventyrs.core.character.services.DamageService#calculateFinalDamage}),
     * or {@code org.aventyrs.core.effect.ManaPurge}'s immediate Magic Point drain. {@code
     * null} for every Interaction that doesn't drain a resource, same stays-{@code
     * null}-when-not-applicable convention as every other field here — paired with
     * {@link #resourceLossType} rather than growing a new field per resource kind as more
     * resource-draining Interactions are added.
     */
    Integer resourceLossValue;

    /** Which {@link ResourceType} {@link #resourceLossValue} was lost from. */
    ResourceType resourceLossType;

    /**
     * The amount of {@link #resourceGainType} this Interaction restored — e.g. {@code
     * org.aventyrs.core.title.santo.AbencoadoPelaLuzInteraction}'s touch heal (Abençoado pela
     * Luz's "recupera PV como se passasse por um Descanso Curto" branch). A separate pair from
     * {@link #resourceLossValue}/{@link #resourceLossType} rather than a signed value on the
     * same field — {@code resourceLossValue} is documented as what an Interaction "drained,"
     * and overloading it with a negative number to mean "restored" would misrepresent that.
     * {@code null} for every Interaction that doesn't restore a resource, same
     * stays-{@code null}-when-not-applicable convention as every other field here.
     */
    Integer resourceGainValue;

    /** Which {@link ResourceType} {@link #resourceGainValue} was restored to. */
    ResourceType resourceGainType;

    /**
     * How many temporary Ego points this Interaction drained — e.g. {@code
     * org.aventyrs.core.effect.Primor}'s "perde 2/1 pontos temporários de Sorte ou
     * Autocontrole". {@code null} for every Interaction that doesn't drain temporary Ego
     * points, same stays-{@code null}-when-not-applicable convention as every other field
     * here. A separate pair from {@link #resourceLossValue}/{@link #resourceLossType}
     * rather than reusing it — temporary Ego points ({@link EgoDomain}: Autocontrole,
     * Recursos, Sorte, Iniciativa) are a genuinely different pool from PV/PM/PD ({@link
     * ResourceType}), not just another value for the same enum.
     *
     * <p>This is the amount a spend <em>actually</em> took (see {@link
     * EgoPointSpend#getValue()}), which against a nearly-empty pool is less than the effect
     * asked for. It always comes from the <em>temporary</em> half: nothing in this ruleset
     * drains permanent Ego points automatically. There is deliberately no {@link EgoPointType}
     * field alongside it — one whose value is always {@code TEMPORARY} would be building for a
     * consumer that doesn't exist.
     */
    Integer egoLossValue;

    /** Which {@link EgoDomain} {@link #egoLossValue} was lost from. */
    EgoDomain egoLossDomain;

    /**
     * Which Ego domains this roll already granted a non-cumulative temporary point in — e.g.
     * {@code org.aventyrs.core.ability.CharismaAbility#DESTINO_FAVORAVEL} granting Sorte and
     * Autocontrole on the roller's own Sucesso Crítico Maior. Unlike {@link
     * #temporaryBonusValue} (a grant for *someone else* this core can't resolve the recipient
     * for), this roll's own target is unambiguous, so the grant is already applied directly
     * via {@link CombatantSheet#grantTemporaryEgoPointBonus} — which raises that domain's
     * temporary <em>ceiling</em>, keyed by the granting ability as its source — by the time
     * this result is returned; this field
     * is purely a report of what happened, same as {@link #egoLossValue}/{@link
     * #egoLossDomain} already are for {@code org.aventyrs.core.effect.Primor}. Always exactly
     * 1 point per listed domain (no ability needing a different value exists yet). {@code
     * null} when this Interaction didn't grant any, same stays-{@code
     * null}-when-not-applicable convention as every other field here.
     */
    List<EgoDomain> egoGainDomains;

    /**
     * The next Interaction in the Skill -&gt; Damage -&gt; EffectChain -&gt;
     * CriticalEffect pipeline (see {@code org.aventyrs.core.effect} package-info), if
     * this stage's own outcome means another one should follow — e.g. a {@code
     * org.aventyrs.core.effect.DamageInteraction} that actually dealt damage handing off
     * to whatever {@code org.aventyrs.core.effect.EffectChain}/{@code
     * org.aventyrs.core.effect.CriticalEffect} the caller supplied. Typed as the shared
     * {@link Interaction}&lt;{@link CombatantSheet}&gt; interface, not any one concrete
     * stage, so every stage is interchangeable in this slot; a caller drives the whole
     * pipeline generically via {@code while (result.getNextInteraction() != null) result
     * = target.receiveInteraction(result.getNextInteraction());} without ever knowing
     * which concrete stages exist for a given roll. {@code null} means this stage didn't
     * apply, or had nothing to chain into — same stays-{@code null}-when-not-applicable
     * convention as every other field here; every stage decides for itself whether to
     * populate this, no central orchestrator exists to decide it on a stage's behalf.
     * Distinct from {@link #nextInteractable} (a possible *different target* for a
     * future hand-off, e.g. a Ricochete-style ability's new target) — this field is
     * about which *behavior* runs next on the current flow, not who it runs against.
     */
    Interaction<CombatantSheet> nextInteraction;
}
