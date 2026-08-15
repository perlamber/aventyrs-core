package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
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
     * Total GD (DifficultyLevel) steps reduced for this Perícia test, aggregated from
     * whatever's currently known on the CharacterSheet — for now, only the trained Skill's
     * unlocked {@link org.aventyrs.core.skill.SkillExcellency} tiers (e.g.
     * {@link org.aventyrs.core.skill.artes.ArtesExcellency#PRODIGIO}). More sources (Talentos,
     * temporary buffs, etc.) would add to this same total as they're built.
     */
    Integer difficultyReduction;

    /**
     * A temporary bonus this roll produced for *someone else* to receive — e.g. {@code
     * ArtesCompetencyAbility#DOM_BARDICO}: motivating allies grants a bonus this roll
     * computes, but who actually receives it is resolved by a layer above this core, via
     * {@code org.aventyrs.core.scene.Scene#getAllies}. {@code null} when this Interaction
     * didn't grant one — same stays-{@code null}-when-not-applicable convention as every
     * other field here. When non-{@code null}, a caller is expected to call {@code
     * CharacterSheet#grantTemporaryBonus} on each intended recipient with this value,
     * {@link #temporaryBonusModifierType}, and {@link #temporaryBonusRounds}.
     */
    Integer temporaryBonusValue;

    /**
     * The {@link ModifierType} {@link #temporaryBonusValue} should be granted as — the broad
     * {@code ModifierType#SKILL_ROLL_BONUS} (DOM_BARDICO's own case — its rules text says
     * "rolagens de Perícias", unrestricted, rather than naming one specific Perícia) or one
     * specific Perícia's own type (e.g. {@code org.aventyrs.core.skill.SkillType#ATLETISMO
     * .getRollBonusType()}), matching {@link org.aventyrs.core.sheet.TemporaryBonus}'s own
     * field — this is a {@code ModifierType}, not a {@code SkillType}, for exactly that
     * reason: it's what {@code CharacterSheet#grantTemporaryBonus} actually takes, with no
     * extra mapping step for the caller to get wrong.
     */
    ModifierType temporaryBonusModifierType;

    /**
     * How many Rodadas {@link #temporaryBonusValue} lasts once granted — e.g. DOM_BARDICO's
     * own 1/2/3 Rodadas depending on the caster's Artes Graduação.
     */
    Integer temporaryBonusRounds;

    /**
     * Who {@link #temporaryBonusValue} applies to — {@code ALLIES} for DOM_BARDICO. A caller
     * resolves the actual recipient list from this (via {@code Scene#getAllies}/
     * {@code #getEnemies}, or its own single-target lookup) — this core only says *who kind*,
     * never the concrete list, same as {@link #temporaryBonusValue} itself only says *what*,
     * never *who receives it*.
     */
    TargetScope temporaryBonusScope;

    /**
     * The highest GD this roll reached — {@code null} unless the Interaction was given a
     * {@code org.aventyrs.core.skill.SkillRoll} to compute it from (see {@code
     * AbstractSkillInteraction#applyTo(CharacterSheet, org.aventyrs.core.scene.SceneContext,
     * org.aventyrs.core.skill.SkillRoll)}) — this core never rolls dice itself, so without an
     * already-rolled {@code SkillRoll} handed in, there's nothing to resolve a tier from.
     * Computed from {@code skillRollBonus + skillRoll.getTotal()} against {@link
     * DifficultyLevel#reachedBy}, e.g. what {@code ArtesCompetencyAbility#DOM_BARDICO} needs
     * to look up its own bonus value.
     */
    DifficultyLevel reachedDifficultyLevel;

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
     * {@link #resourceLossType} the same way {@link #temporaryBonusValue} pairs with
     * {@link #temporaryBonusModifierType}, rather than growing a new field per resource
     * kind as more resource-draining Interactions are added.
     */
    Integer resourceLossValue;

    /** Which {@link ResourceType} {@link #resourceLossValue} was lost from. */
    ResourceType resourceLossType;

    /**
     * How many temporary Ego points this Interaction drained — e.g. {@code
     * org.aventyrs.core.effect.Primor}'s "perde 2/1 pontos temporários de Sorte ou
     * Autocontrole". {@code null} for every Interaction that doesn't drain temporary Ego
     * points, same stays-{@code null}-when-not-applicable convention as every other field
     * here. A separate pair from {@link #resourceLossValue}/{@link #resourceLossType}
     * rather than reusing it — temporary Ego points ({@link EgoDomain}: Autocontrole,
     * Recursos, Sorte, Iniciativa) are a genuinely different pool from PV/PM/PD ({@link
     * ResourceType}), not just another value for the same enum.
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
     * via {@link CharacterSheet#gainNonCumulativeTemporaryEgoPoints} (keyed by the granting
     * ability as its source — see that method) by the time this result is returned; this field
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
     * {@link Interaction}&lt;{@link CharacterSheet}&gt; interface, not any one concrete
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
    Interaction<CharacterSheet> nextInteraction;
}
