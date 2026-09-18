package org.aventyrs.core.title;

import java.util.Optional;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.ActionCost;
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
     * Custo de Ativação in PD (Pontos de Determinação) — {@link PDCost#NONE} for a no-cost
     * passive, or for an {@link AventyrTitleSpecialization} with no activation cost of its own
     * (the common case for a purely descriptive Especialização). A {@link PDCost.Variable} cost
     * is chosen by the activating player at or above its minimum.
     */
    default PDCost getPDCost() {
        return PDCost.NONE;
    }

    /**
     * The Tempo de Ativação — {@link ActionCost#NONE} for a passive, {@code
     * ActionCost.ofActionPoints(n)} for "Tempo de Ativação: NPA", {@link ActionCost#REACTION} for
     * "Reação", {@link ActionCost#FREE_ACTION} for "Ação Livre", or {@code ActionCost.dynamic(min)}
     * for a Variável one.
     *
     * <p>This was three separate members until {@code ActionCost} grew to hold all of them: an
     * {@code int} plus an {@code isReactionActivation()}/{@code isFreeActionActivation()} pair,
     * awkward enough that {@code AbencoadoPelaLuzAbility#GLORIA_RELAMPEJANTE_DE_TESLA} needed an
     * anonymous class body to say "Ação Livre" on an enum with no field for it.
     *
     * <p>Reported, never deducted — {@code AbstractTitleAbilityInteraction} reads only the {@link
     * #getPDCost()}, the same restraint every other cost in this core keeps.
     */
    default ActionCost getActionPointCost() {
        return ActionCost.NONE;
    }

    /**
     * Whether this Habilidade/Suprema is passive (always active, no player-triggered
     * activation) as opposed to something the holder spends a resource/action to do. Derived
     * from the activation cost rather than a separate stored flag: exactly the constants whose
     * own rules text says "Custo de Ativação: Nenhum, habilidade passiva" (e.g. Bastião dos
     * Necessitados, Protetor da Vida e da Morte) name {@link ActionCost#NONE} and no others.
     * A Reação and an Ação Livre are real player-triggered activations, and say so in their own
     * {@link ActionCost.Kind} rather than needing to be excluded by hand here. It does not need a
     * PD-cost check, since no current constant combines a real PDCost with a {@link
     * ActionCost#NONE} Tempo de Ativação.
     */
    default boolean isPassive() {
        return getActionPointCost().kind() == ActionCost.Kind.NONE;
    }

    /**
     * What this ability reacts <em>to</em> — {@code null}, the default, for everything that is not
     * a Reação. Only meaningful alongside a {@link ActionCost#REACTION} {@link
     * #getActionPointCost()}: the cost says "this is a Reação", this says which event offers it,
     * and {@code ReactionOptionsService} needs both to put it in front of a player.
     */
    default ReactionTrigger getReactionTrigger() {
        return null;
    }

    /**
     * Whether this Reação's own rules text is satisfied by what just happened — the per-ability
     * half of availability, after {@code ReactionOptionsService} has matched the {@link
     * #getReactionTrigger()}. {@code true} by default, so a Reação with nothing further to say
     * needs no override; {@code SantoAbility#GUARDA_VIDAS} overrides it to check the threatened
     * ally is actually an ally and actually within its {@link #resolveTeleportation()} reach.
     *
     * <p>This is an <b>availability</b> question, not an affordability one — whether the reactor
     * has a Reação and the PD to spend is the service's business, and an unaffordable option is
     * still returned. Nor is it the activation's own {@code validate}: the {@code
     * AbstractTitleAbilityInteraction} re-checks what it needs when the player actually commits,
     * since a list is a snapshot and the world moves.
     */
    default boolean isReactionAvailable(ReactionContext context) {
        return true;
    }

    /**
     * How far activating this ability teleports its holder, or {@code null} — the default, and
     * every ability that moves nobody. A client reads this to offer the destination; this core
     * neither picks one nor applies it, since it holds no positions at all. See {@link
     * Teleportation}.
     */
    default Teleportation resolveTeleportation() {
        return null;
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

    /**
     * RA this Habilidade de Título grants right now to an <i>adjacent ally</i> — the outward
     * half of an ability whose rules text buffs someone else continuously, with no activation
     * and no trigger (Santo's Bastião dos Necessitados: "Aliados adjacentes, apenas aqueles com
     * menos PV que você, recebem RA"). {@code allyHasLowerPv} is that clause's comparison,
     * resolved by {@code DamageServiceImpl} — the only caller with a {@code HitPointsService} in
     * hand — and passed in, exactly as {@link #resolveAbsoluteDamageReduction}'s own {@code
     * hasLowerPvAdjacentAlly} is for the self-facing half. The two are duals of the same
     * comparison in opposite directions, and stay separate methods because an ability can grant
     * both, at different values.
     *
     * <p>This is deliberately <b>not</b> a {@code Blessing}/{@code TemporaryBonus} grant, unlike
     * every other cross-character bonus in this core. A granted bonus is a snapshot: it would
     * have to be revoked the moment either character moved out of adjacency, died, or left the
     * Scene, and something would have to notice each of those to do the revoking. Scanned
     * instead, at the moment the recipient's damage is actually calculated, the answer is
     * correct by construction as allies move in and out of range — nothing to grant, nothing to
     * revoke, nothing to persist. Same "recompute on demand from data already in hand"
     * discipline as {@code InitiativeEntry#getEffectiveInitiativeValue} (see CLAUDE.md's
     * "Iniciativa can change mid-Scene").
     *
     * <p>Zero by default; only override on a constant whose rules text grants RA to someone
     * other than its holder.
     */
    default int resolveAllyAbsoluteDamageReduction(SceneContext sceneContext, boolean allyHasLowerPv) {
        return 0;
    }

    /**
     * How many Especializações of this same Título title must already hold before this
     * ability can be acquired, <b>not caring which ones</b> — e.g. {@code
     * SantoAbility#GUARDA_VIDAS}'s own "Requer 1 Especialização e 2 outras Habilidades de
     * Santo" names 1 here, satisfied by either of Santo's two. {@code 0} by default (no
     * Especialização prerequisite); only override on a constant whose rules text names a bare
     * count like this. When a constant's rules text instead names one <i>specific</i>
     * Especialização by name (e.g. {@code AbencoadoPelaLuzAbility}'s own "Requer Especialização
     * 'Abençoado pela Luz'"), override {@link #getRequiredSpecialization()} instead — a
     * constant sets one or the other, never both, since {@link #isEligible} only consults this
     * bare count when that one is absent. Checked by {@link #isEligible} — the first "Requer
     * N..." acquisition prerequisite this codebase actually enforces for a Título ability,
     * unlike every other "Requer N..."-style clause elsewhere in this core, which stays a
     * documented-but-unenforced comment (see CLAUDE.md's "no eligibility validation service"
     * restraint).
     */
    default int getRequiredSpecializations() {
        return 0;
    }

    /**
     * The one specific Especialização this ability's own rules text names by name, if any —
     * e.g. every {@code AbencoadoPelaLuzAbility} constant requires {@code
     * SantoSpecialization#ABENCOADO_PELA_LUZ} specifically (matching that whole catalog's own
     * class-level javadoc: every ability there is gated on holding that one Especialização),
     * not merely "any 1 Especialização" the way {@link #getRequiredSpecializations()}' bare
     * count means. {@code Optional.empty()} by default; {@link #isEligible} falls back to the
     * bare count only when this is empty.
     */
    default Optional<AventyrTitleSpecialization> getRequiredSpecialization() {
        return Optional.empty();
    }

    /**
     * How many <b>other</b> Habilidades/Supremas from this <i>same catalog</i> — the same
     * concrete {@code AventyrTitleAbility} enum this constant itself belongs to, e.g. {@code
     * AbencoadoPelaLuzAbility#GLORIA_RELAMPEJANTE_DE_TESLA}'s own "Requer 2 Habilidades de
     * 'Abençoado pela Luz'" counting only sibling {@code AbencoadoPelaLuzAbility} constants,
     * never {@code SantoAbility} ones even though both live in the same held {@code
     * AventyrTitle#getAbilities()} list — must already be held before this ability can be
     * acquired. "Outras" (the rules text's own wording) excludes this ability itself, so
     * checking this against title's own current per-catalog count (taken <i>before</i> this
     * ability is added) already gets that exclusion for free. {@code 0} by default; only
     * override on a constant whose rules text names one. Doesn't distinguish Habilidade from
     * Suprema when counting — every "Requer N outras Habilidades" clause modeled so far never
     * separately counts held Supremas, so a held Suprema from the same catalog counts toward
     * this too, same as {@code AventyrTitle#getSpecializationAndSupremaCount()} already treats
     * both uniformly for Despertar's own duration/Defesas formulas.
     */
    default int getRequiredOtherAbilities() {
        return 0;
    }

    /**
     * Whether title currently satisfies this ability's own Especialização/other-Habilidades
     * prerequisite. Every prerequisite modeled so far is entirely self-contained to title's own
     * held Especializações/Habilidades — no Character-level data is needed — so this compares
     * directly against title rather than taking a {@code Character}, mirroring {@link
     * AventyrTitle#getSpecializationAndSupremaCount()}'s own "pure arithmetic over title
     * itself" shape. The Especialização half prefers {@link #getRequiredSpecialization()}
     * (containment against title's own {@link AventyrTitle#getSpecializations()}) when present,
     * falling back to {@link #getRequiredSpecializations()}'s bare count otherwise. The
     * other-Habilidades half only counts title's held abilities belonging to this same concrete
     * catalog — matched via {@link Enum#getDeclaringClass()} where the ability is an {@code
     * Enum} (a constant-specific class body, e.g. {@code SantoAbility#BASTIAO_DOS_NECESSITADOS}'s
     * own anonymous override, would otherwise report a different {@code Class} per constant
     * than its sibling constants with no body, which {@code getClass()} alone can't see past),
     * or plain {@link Object#getClass()} for a non-enum implementor (e.g. a test double) —
     * see {@link #isSameCatalog}. {@code true} by default (no prerequisite); a constant
     * overriding any of the three hooks above is automatically checked here without needing its
     * own {@code isEligible} override too. Checked by {@code
     * org.aventyrs.core.character.services.TitleAbilityService#grantTitleAbility} before
     * granting.
     */
    default boolean isEligible(AventyrTitle title) {
        boolean specializationSatisfied = getRequiredSpecialization()
                .map(required -> title.getSpecializations().contains(required))
                .orElseGet(() -> title.getSpecializations().size() >= getRequiredSpecializations());
        long sameCatalogAbilityCount = title.getAbilities().stream()
                .filter(held -> isSameCatalog(held, this))
                .count();
        return specializationSatisfied && sameCatalogAbilityCount >= getRequiredOtherAbilities();
    }

    /**
     * Whether one and other belong to the same concrete {@code AventyrTitleAbility} catalog —
     * see {@link #isEligible}'s own javadoc for why a plain {@code getClass()} comparison isn't
     * safe here.
     */
    private static boolean isSameCatalog(final AventyrTitleAbility one, final AventyrTitleAbility other) {
        return catalogOf(one) == catalogOf(other);
    }

    private static Class<?> catalogOf(final AventyrTitleAbility ability) {
        return ability instanceof Enum<?> enumConstant ? enumConstant.getDeclaringClass() : ability.getClass();
    }

    public Optional<Class <? extends Interaction>> getInteractionClass();
}
