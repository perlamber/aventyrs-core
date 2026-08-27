package org.aventyrs.core.ego;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

public interface EgoAdvantage {
    EgoDomain getEgoDomain();
    String getDescription();

    /**
     * A bonus toward a Perícia roll this Vantagem grants right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#IMPETO}'s Vantagem during a Cena de
     * Combate's first two Rounds. Mirrors {@code org.aventyrs.core.skill.SkillCompetencyAbility
     * #resolveConditionalRollBonus}'s shape (same reason: this data isn't reflection-discoverable
     * via a no-arg {@code @Modifier} method), but on the {@code EgoAdvantage} side instead — a
     * Vantagem de Ego is granted once at creation, not tied to any one Perícia, so it's summed
     * generically across every Perícia's own {@code AbstractSkillInteraction#applyTo}, the same
     * additive convention every other {@code skillRollBonus} source already uses. Empty by
     * default; only override on a constant whose rules text grants a bonus scoped to per-roll
     * Scene facts like this.
     */
    default Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext) {
        return Optional.empty();
    }

    /**
     * A bonus toward a dano roll this Vantagem grants right now — mirrors {@code
     * SkillCompetencyAbility#resolveDamageBonus}'s shape, minus the {@code attackTarget}
     * parameter: unlike {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s proximity condition,
     * no {@code EgoAdvantage} granting this needs to know the real attack target, only {@link
     * SceneContext} facts, so this is safe to resolve generically for every attack-skill roll
     * inside {@code AbstractSkillInteraction} itself rather than needing a skill-specific
     * overload with an explicit target. Empty by default; only override on a constant whose
     * rules text grants a dano bonus. Same "only one bonus expected to apply per roll"
     * convention as {@code resolveDamageBonus} — not additive with it.
     */
    default Optional<DamageBonus> resolveDamageBonus(final SceneContext sceneContext) {
        return Optional.empty();
    }

    /**
     * Every {@link Blessing} this Vantagem grants the moment its holder wins
     * initiative for their group — e.g. {@link InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s
     * +2UD Movimento Base. Resolved once, at grant-time (not per-roll like {@link
     * #resolveConditionalRollBonus}/{@link #resolveDamageBonus} above, so no {@code
     * SceneContext} parameter — the Round-scoping lives in each returned blessing's own {@code
     * rounds} countdown once granted), by {@code
     * org.aventyrs.core.character.services.InitiativeBlessingService}, which scans this same
     * default-empty hook across {@code EgoAdvantage}, {@code
     * org.aventyrs.core.ability.AttributeAbility}, and {@code
     * org.aventyrs.core.skill.SkillCompetencyAbility} identically — see that service's own
     * javadoc for the full mechanism. Empty by default; only override on a constant whose
     * rules text grants a bonus specifically for winning initiative.
     */
    default List<Blessing> resolveInitiativeBlessings() {
        return List.of();
    }

    /**
     * Extra temporary Ego points — beyond {@code
     * org.aventyrs.core.character.services.EgoPointsService#SESSION_TEMPORARY_RECOVERY}'s single
     * baseline point — that this Vantagem recovers per game session, e.g. {@link
     * AutocontroleAdvantage#MOTIVACAO_DE_MOSES}'s "Você recupera 1 ponto de Autocontrole
     * temporário adicional por sessão de jogo" and {@link SorteAdvantage#DILETO_DE_TYKHE}'s
     * identical clause for Sorte. Zero by default; only override on a constant whose rules text
     * grants one.
     *
     * <p>No-arg, like {@link #resolveInitiativeBlessings}: the points always land in this
     * Vantagem's own {@link #getEgoDomain()}, so attributing them there rather than returning a
     * domain is what keeps the two from ever drifting apart. A Vantagem recovering points in
     * some <em>other</em> domain would need a domain-returning shape instead — none exists, and
     * this shouldn't be widened speculatively for one.
     *
     * <p>Resolved by {@code EgoPointsService#getExtraSessionRecovery} and applied by {@code
     * EgoPointsService#applySessionRecovery}. That value is real, tested data even though
     * nothing calls the service automatically — this core has no game-session boundary to
     * trigger it from.
     */
    default int resolveExtraSessionEgoRecovery() {
        return 0;
    }

    /**
     * How much PV, PM <em>and</em> PD this Vantagem additionally recovers because its holder
     * just deliberately used Ego points for some effect — e.g. {@link
     * AutocontroleAdvantage#DETERMINACAO_HEROICA}'s "Usar pontos de Autocontrole para qualquer
     * efeito adicionalmente recupera +1d6PV, PM e PD; se o ponto for permanente, o valor
     * recuperado é dobrado." One figure, applied to all three pools alike, per that rules text.
     * Zero by default; only override on a constant whose rules text reacts to a spend this way.
     *
     * <p>{@code rolledValue} is the caller's <strong>already-rolled</strong> die — this core
     * never rolls dice itself, the same boundary {@code org.aventyrs.core.skill.SkillRoll} and
     * {@code org.aventyrs.core.combat.IncomingAttack} sit on. An override doubling it for a
     * permanent point returns {@code rolledValue * 2}; the doubling is arithmetic on a value
     * that arrived from outside, not a second roll.
     *
     * <p>{@code spend} is a <strong>completed</strong> {@link EgoPointSpend}: it reports which
     * pool was actually drawn from ({@link EgoPointSpend#getType()} — the permanent/temporary
     * distinction this hook exists for) and how many points actually left it, which may be
     * fewer than were asked for. An override should return 0 when {@code spend.getValue()} is
     * 0, since nothing was in fact used.
     *
     * <p><strong>Only a deliberate use triggers this, never a drain.</strong> It is resolved by
     * {@code EgoPointsService#useEgoPointsForEffect}, and deliberately <em>not</em> by {@code
     * CombatantSheet#spendEgoPoints} itself — which also serves {@code
     * org.aventyrs.core.effect.Primor} draining a victim's points against their will. Firing
     * there would mean a critical hit healing the character it just hit. "Usar pontos… para
     * qualquer efeito" is the holder choosing to spend, and that choice is exactly what the
     * separate service entry point represents.
     *
     * <p>Consulted only for the Vantagem held in the spend's <em>own</em> {@link EgoDomain} —
     * the same domain-scoping {@link #resolveExtraSessionEgoRecovery} uses, and what keeps
     * DETERMINACAO_HEROICA (an Autocontrole Vantagem) from reacting to a Sorte spend without
     * needing to check the domain itself.
     */
    default int resolveEgoSpendRecovery(EgoPointSpend spend, int rolledValue) {
        return 0;
    }

    /**
     * Every {@link Blessing} this Vantagem grants because its holder just deliberately used Ego
     * points for some effect — e.g. {@link SorteAdvantage#AS_NA_MANGA}'s 2UD of extra movement
     * "imediatamente após utilizar um Ponto de Sorte". Empty by default; only override on a
     * constant whose rules text grants a temporary bonus off the back of a spend.
     *
     * <p>The {@link EgoPointSpend} sibling of {@link #resolveInitiativeBlessings()}, and resolved
     * the same "resolve, don't mutate" way — {@code EgoPointsService#useEgoPointsForEffect} is
     * what actually grants what this returns. Unlike the initiative path, though, these are
     * granted <strong>directly to the spender</strong> rather than handed back for a caller to
     * route: who used the points is unambiguous, the same reasoning that lets {@code
     * AbstractSkillInteraction} apply {@code DESTINO_FAVORAVEL}'s Ego point on the spot. A
     * {@link org.aventyrs.core.sheet.TargetScope} other than {@code SELF} therefore has no
     * meaning here yet, and nothing routes one; don't return one expecting allies to receive it.
     *
     * <p>An override should return nothing when {@code spend.getValue()} is 0 — no points were
     * in fact used — and, like {@link #resolveEgoSpendRecovery}, it is consulted only for the
     * Vantagem held in the spend's own {@link EgoDomain}, so a constant needn't check the domain
     * itself. It fires on a deliberate use only, never on an involuntary drain such as {@code
     * org.aventyrs.core.effect.Primor}'s.
     */
    default List<Blessing> resolveEgoSpendBlessings(EgoPointSpend spend) {
        return List.of();
    }

    /**
     * RA (Redução Absoluta) this Vantagem grants right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s RA during a Cena
     * de Combate's first two Rounds. Summed by {@code
     * org.aventyrs.core.character.services.DamageService#getTotalAbsoluteDamageReduction(
     * Character, SceneContext)} across every held {@code EgoAdvantage}, alongside the
     * reflection-based {@code @Modifier(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)} sources that
     * method already scanned — this one exists because a Round/Cena-de-Combate-conditioned
     * bonus isn't reflection-discoverable via a no-arg {@code @Modifier} method, the same
     * reason {@link #resolveConditionalRollBonus} exists instead of a plain {@code @Modifier}
     * for {@code SKILL_ROLL_BONUS}. Zero by default; only override on a constant whose rules
     * text grants RA scoped to per-roll Scene facts like this.
     */
    default int resolveAbsoluteDamageReduction(final SceneContext sceneContext) {
        return 0;
    }

    /**
     * Whether this Vantagem halves damage taken right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s "dano causado a
     * você é reduzido à metade" once its holder has also won initiative. Mirrors {@link
     * #resolveAbsoluteDamageReduction}'s reasoning, but a plain {@code boolean} rather than a
     * summed value — same shape {@code DamageServiceImpl} already treats {@code
     * ModifierType#HALF_DAMAGE} as (any positive sum means "yes"), not a magnitude. {@code
     * false} by default; only override on a constant whose rules text halves damage taken
     * scoped to per-roll Scene facts like this.
     */
    default boolean resolveHalfDamage(final SceneContext sceneContext) {
        return false;
    }

    /**
     * A bonus toward a Perícia roll this Vantagem grants, but only for named skillType(s) —
     * e.g. {@link ResourcesAdvantage#MORAL_HERDADA}'s "+1 em rolagens de Artes e Persuasão,"
     * not every Perícia. {@link #resolveConditionalRollBonus} can't express this: it's summed
     * identically into every skill's own {@code AbstractSkillInteraction#applyTo} with no
     * {@link SkillType} to condition on at all — using it here would silently over-grant the
     * bonus to every Perícia instead of just the named ones, the same "don't silently narrow or
     * over-grant" restraint this codebase already applies to purpose-scoped Vantagem bonuses.
     * target is the CombatantSheet performing this roll (not a separate attack target — unlike
     * {@code SkillCompetencyAbility#resolveDamageBonus}'s {@code attackTarget}, this Vantagem's
     * own bonus depends on the roller's <em>own</em> state, e.g. its current Fama), passed
     * explicitly for the same reason {@code CombatantSheet} state generally is here: it isn't
     * reflection-discoverable via a no-arg {@code @Modifier} method. Empty by default; only
     * override on a constant whose rules text scopes a roll bonus to specific named skills.
     */
    default Optional<Integer> resolveSkillSpecificRollBonus(final SkillType skillType, final SceneContext sceneContext, final CombatantSheet target) {
        return Optional.empty();
    }

    /**
     * How many extra "números" this Vantagem widens skillType's Margem Crítica Menor by right
     * now, conditioned on {@link SceneContext} — e.g. {@link SorteAdvantage#ACE}'s +1/+3 bonus
     * depending on the roll's attack-skill/combat-scene status. Mirrors {@code
     * org.aventyrs.core.ability.AttributeAbility#resolveCriticalMarginIncrease}/{@code
     * org.aventyrs.core.skill.SkillCompetencyAbility#resolveCriticalMarginIncrease}'s identical
     * shape — see {@code org.aventyrs.core.skill.SkillRoll#getCriticalResult(int)} for how the
     * sum across all three is actually consumed. Zero by default; only override on a constant
     * whose rules text widens Margem Crítica Menor like this.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
        return 0;
    }
}
