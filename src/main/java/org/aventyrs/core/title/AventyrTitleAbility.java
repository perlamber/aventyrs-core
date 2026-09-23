package org.aventyrs.core.title;

import java.util.Optional;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
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
     * Custo de Ativação in temporary Ego points — Gigante Enfurecido's "1 Ponto Temporário de
     * Autocontrole". {@link EgoCost#NONE} by default: every other trait pays in PD alone. This is the
     * <em>stated</em> cost a client shows and affords; an activation whose price grows with the
     * options chosen (Frenesi's Especializações, Cataclismo's elements) resolves the real figure in
     * its own {@code AbstractTitleAbilityInteraction#resolveEgoCost}.
     */
    default EgoCost getEgoCost() {
        return EgoCost.NONE;
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
     * RDS (Redução de Danos Sofridos) this Habilidade de Título grants its own holder right now —
     * the RD twin of {@link #resolveAbsoluteDamageReduction}, and the hook Santo's Bastião dos
     * Necessitados uses under V19 ("Enquanto estiver protegendo ao menos 1 aliado você recebe RDS
     * igual a metade do valor capaz de fornecer").
     *
     * <p><b>RDS is RD, not RA</b> — {@code ModifierType#DAMAGE_REDUCTION}; see {@code
     * GorgonaFeat}'s own "the source text is redundant: RDS <i>is</i> RD" note. The RA pair above
     * stays for clauses that really do name Redução Absoluta, and is not deprecated by this one:
     * the two mitigate at different stages, RA being the one an attack can never ignore.
     *
     * <p>Takes the granting {@link AventyrTitle} as well, which the RA pair does not need: a value
     * like "1+ Metade das Habilidades de Santo que você possuir" is a fact about the <em>holder's
     * own Título</em>, and an enum constant has no holder. {@code DamageServiceImpl} has the
     * {@code AventyrTitle} in hand at both call sites, so it passes it rather than making every
     * constant re-derive it. {@code hasLowerPvAdjacentAlly} is resolved by that same caller, for
     * the reason {@link #resolveAbsoluteDamageReduction}'s own javadoc gives.
     *
     * <p>Zero by default; only override on a constant whose rules text grants RDS to its holder.
     */
    default int resolveDamageReduction(SceneContext sceneContext, AventyrTitle holder,
                                       boolean hasLowerPvAdjacentAlly) {
        return 0;
    }

    /**
     * RDS this Habilidade de Título grants right now to an <i>adjacent ally</i> — the RD twin of
     * {@link #resolveAllyAbsoluteDamageReduction}, and the outward half of Santo's Bastião dos
     * Necessitados under V19 ("Seus aliados, que tenham menos quantidade de PV atuais que você,
     * recebem RDS igual a 1+ Metade das Habilidades de Santo que você possuir. Apenas aliados
     * adjacentes recebem este benefício").
     *
     * <p><b>Scanned, never granted</b>, for exactly the reasoning {@link
     * #resolveAllyAbsoluteDamageReduction}'s javadoc sets out at length: a granted bonus would be
     * a snapshot needing revocation the moment either character moved, and recomputing at the
     * moment the recipient's damage is calculated is correct by construction instead.
     *
     * <p>Note the two directions read different things: {@code allyHasLowerPv} is the
     * <em>recipient's</em> PV comparison, while {@code holder} is the <em>granter's</em> Título —
     * which is precisely why the Título has to be passed rather than inferred from whoever is
     * taking the damage.
     *
     * <p>Zero by default; only override on a constant whose rules text grants RDS to someone other
     * than its holder.
     */
    default int resolveAllyDamageReduction(SceneContext sceneContext, AventyrTitle holder,
                                           boolean allyHasLowerPv) {
        return 0;
    }

    /**
     * Pontos de Ação this Habilidade de Título adds to its holder's maximum right now — scanned by
     * {@code ActionPointsServiceImpl} beside the {@code ModifierType#ACTION_POINTS} sources.
     *
     * <p><b>Scanned rather than granted</b>, and that is the whole point: {@code
     * AbracadoPelaEscuridaoAbility#FUROR_DE_SYLPH}'s "+1PA" lasts while it still has attacks left
     * to enhance ({@code CombatantSheet#getRemainingEnhancedAttacks}), which is a budget rather
     * than a Duração. A {@link org.aventyrs.core.sheet.TemporaryBonus} only counts down in
     * Rodadas, so granting it would have meant inventing a Duração the clause never states;
     * recomputing from the budget is correct by construction as it is spent, and needs no revoke.
     * Same reasoning {@link #resolveAllyDamageReduction} gives for its own scan.
     *
     * <p>Zero by default; needs the holder's sheet, so a {@code Character}-only caller never sees
     * it — the same limitation the aggregate PA/Reações/Ações Livres reads already carry.
     */
    default int resolveActionPointBonus(CombatantSheet holder) {
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
     * How many <b>other</b> Habilidades/Supremas must already be held before this ability can be
     * acquired — {@link #getRequiredOtherAbilitiesScope()} says which ones count. "Outras" (the
     * rules text's own wording) excludes this ability itself. {@code 0} by default; only override
     * on a constant whose rules text names one. Doesn't distinguish Habilidade from Suprema when
     * counting — no "Requer N outras Habilidades" clause modeled so far counts held Supremas
     * separately, same as {@code AventyrTitle#getSpecializationAndSupremaCount()} already treats
     * both uniformly for Despertar's own duration/Defesas formulas.
     */
    default int getRequiredOtherAbilities() {
        return 0;
    }

    /**
     * Which Habilidades {@link #getRequiredOtherAbilities()} counts: those gated on the
     * Especialização this names, or — {@code Optional.empty()} — <b>every</b> Habilidade the
     * Título holds, whichever Especialização (if any) each one came from.
     *
     * <p>The two readings are both real, and the rules text is what distinguishes them:
     * <ul>
     *   <li>{@code SantoAbility#GUARDA_VIDAS}'s "Requer 1 Especialização e 2 outras Habilidades
     *       <b>de Santo</b>" names the <i>Título</i>. Every Habilidade the Santo holds is a
     *       Habilidade de Santo — its own top-level ones and the ones an Especialização brought
     *       alike — so all of them count. This is the empty case, and the default.</li>
     *   <li>{@code AbencoadoPelaLuzAbility#GLORIA_RELAMPEJANTE_DE_TESLA}'s "Requer 2 Habilidades
     *       <b>de 'Abençoado pela Luz'</b>" names one <i>Especialização</i>, so only Habilidades
     *       gated on that one count.</li>
     * </ul>
     *
     * <p><b>Defaults to {@link #getRequiredSpecialization()}</b>, which gets both catalogs right
     * with nothing to declare per constant: a {@code <Specialization>Ability} constant is by
     * definition gated on its own Especialização and its "Requer N Habilidades" clause names that
     * same Especialização, while a {@code <Title>Ability} constant names no Especialização at all
     * and its clause names the Título. Override only where a constant's text disagrees with that
     * — e.g. a Título-level Habilidade whose clause names one Especialização, or a gated one whose
     * clause names the Título at large.
     *
     * <p>An earlier version scoped this by <i>Java enum</i> instead — same catalog class as the
     * constant itself. That read {@code SantoAbility}'s clause wrongly: a Santo holding one
     * Especialização and two of its gated Habilidades satisfies "2 outras Habilidades de Santo" in
     * the rules, but counted 0 under that rule and could never acquire Guarda-Vidas or Bastião dos
     * Necessitados at all.
     */
    default Optional<AventyrTitleSpecialization> getRequiredOtherAbilitiesScope() {
        return getRequiredSpecialization();
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
     * other-Habilidades half counts whichever of title's held abilities {@link
     * #getRequiredOtherAbilitiesScope()} admits — every one of them, or only those gated on one
     * named Especialização — and never this ability itself, which is what the rules text's own
     * "outras" means. {@code true} by default (no prerequisite); a constant overriding any of the
     * hooks above is automatically checked here without needing its own {@code isEligible}
     * override too. Checked by {@code
     * org.aventyrs.core.character.services.TitleAbilityService#grantTitleAbility} before
     * granting.
     */
    default boolean isEligible(AventyrTitle title) {
        boolean specializationSatisfied = getRequiredSpecialization()
                .map(required -> title.getSpecializations().contains(required))
                .orElseGet(() -> title.getSpecializations().size() >= getRequiredSpecializations());
        Optional<AventyrTitleSpecialization> scope = getRequiredOtherAbilitiesScope();
        long otherAbilityCount = title.getAbilities().stream()
                .filter(held -> held != this)
                .filter(held -> scope.isEmpty() || scope.equals(held.getRequiredSpecialization()))
                .count();
        return specializationSatisfied && otherAbilityCount >= getRequiredOtherAbilities();
    }

    /**
     * {@link #isEligible(AventyrTitle)} for a prerequisite that also names something the
     * <em>character</em> holds beyond the Título — Frenesi Arcano's "Especialização 'Titã
     * Enlouquecido' e o talento 'Arcanista'". Checked by {@code TitleAbilityService#grantTitleAbility};
     * delegates to the Título-only check by default.
     */
    default boolean isEligible(AventyrTitle title, org.aventyrs.core.character.Character character) {
        return isEligible(title);
    }

    public Optional<Class <? extends Interaction>> getInteractionClass();
}
