package org.aventyrs.core.title;

import java.util.Optional;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Interaction;

/**
 * A Habilidade or Suprema from one specific Título's own catalog (e.g. a
 * {@code SantoAbility} constant) — {@link #isSupreme()} distinguishes the top ability tier
 * (limited to one per Título+Especializações combination held, except where a specific
 * ability grants more — unenforced, same "no eligibility validation service" restraint
 * documented throughout this codebase, e.g. {@code SkillCompetencyAbility}'s own acquisition
 * prerequisites) from an ordinary Habilidade.
 *
 * <p>Also implemented by {@link AventyrTitleSpecialization} — any Título trait with a real
 * activation cost (PD/PA/Reação/Ação Livre) is an Active Ability in the sense this interface
 * models, regardless of whether it's cataloged as a Habilidade/Suprema or an Especialização
 * (e.g. Santo's own Abençoado pela Luz, "Custo de Ativação: 1PD, Tempo de Ativação: 2PA").
 * {@link #isSupreme()}/{@link #getPDCost()} both default here (rather than staying abstract)
 * specifically so a Especialização sharing this interface doesn't need to answer a question
 * that never applies to it ("is this a Suprema") or restate a cost it may genuinely lack —
 * every real {@code AventyrTitleAbility} catalog constant still explicitly overrides both via
 * its own enum constructor, so this default doesn't change any existing ability's behavior.
 */
public interface AventyrTitleAbility {

    String getDescription();

    /**
     * Whether this is a Suprema (the top ability tier) rather than an ordinary Habilidade —
     * {@code false} by default, since this question never applies to an
     * {@link AventyrTitleSpecialization} sharing this interface (Especializações aren't
     * Habilidade/Suprema-tiered at all); every real Habilidade/Suprema catalog constant
     * overrides this explicitly.
     */
    default boolean isSupreme() {
        return false;
    }

    /**
     * Custo de Ativação in PD (Pontos de Determinação) — 0 for a no-cost passive, or for an
     * {@link AventyrTitleSpecialization} with no activation cost of its own (the common case
     * for a purely descriptive Especialização).
     */
    default int getPDCost() {
        return 0;
    }

    /** Custo de Ativação in PA (Pontos de Ação) — 0 for a passive or a Reação-only activation. */
    default int getActionPointCost() {
        return 0;
    }

    /** Whether this ability's Tempo de Ativação is "Reação" rather than a PA cost. */
    default boolean isReactionActivation() {
        return false;
    }

    /** Whether this ability's Tempo de Ativação is "Ação Livre" rather than a PA cost. */
    default boolean isFreeActionActivation() {
        return false;
    }

    /**
     * Whether this Habilidade/Suprema is passive (always active, no player-triggered
     * activation) as opposed to something the holder spends a resource/action to do. Derived
     * from the existing activation-cost data rather than a separate stored flag: a nonzero
     * {@link #getActionPointCost()}, or an explicit {@link #isReactionActivation()}/
     * {@link #isFreeActionActivation()} (both still real player-triggered activations, even
     * though their own actionPointCost is 0), all count as active. Confirmed against every
     * currently-modeled Habilidade/Suprema: this formula picks out exactly the ones whose own
     * rules text says "Custo de Ativação: Nenhum, habilidade passiva" (e.g. Bastião dos
     * Necessitados, Protetor da Vida e da Morte) and no others — it does not need a PD-cost
     * check, since no current constant combines a real PDCost with zero PA/Reação/Ação Livre.
     */
    default boolean isPassive() {
        return getActionPointCost() == 0 && !isReactionActivation() && !isFreeActionActivation();
    }

    /**
     * RA (Redução Absoluta) this Habilidade de Título grants right now — mirrors
     * {@link org.aventyrs.core.ego.EgoAdvantage#resolveAbsoluteDamageReduction(SceneContext)}'s
     * shape, plus one extra parameter: hasLowerPvAdjacentAlly. A Título ability's own condition
     * (e.g. Santo's Bastião dos Necessitados — "enquanto estiver adjacente à um aliado com
     * menos PV que você") needs a PV comparison between the holder and each of sceneContext's
     * adjacent allies that neither sceneContext alone nor a no-arg {@code @Modifier} method can
     * resolve — {@code DamageServiceImpl} (the only caller with a {@code HitPointsService} in
     * hand) resolves that comparison once and passes the boolean result in, the same "explicit
     * parameter because it isn't reflection-discoverable" reasoning used throughout this core's
     * other {@code resolve*} hooks. Zero by default; only override on a constant whose rules
     * text grants RA conditioned on this specific comparison.
     */
    default int resolveAbsoluteDamageReduction(SceneContext sceneContext, boolean hasLowerPvAdjacentAlly) {
        return 0;
    }

    public Optional<Class <? extends Interaction>> getInteractionClass();
}
