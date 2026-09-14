package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A grantable temporary bonus, computed by whatever mechanism resolved it but not yet applied
 * to a recipient — the "what" (a {@link ModifierType}, a value, a duration in Rodadas) and the
 * "who kind" ({@link TargetScope}), with the concrete recipient list still left to whoever
 * applies it. Two real mechanisms produce these today, on the same shape:
 *
 * <ul>
 *   <li>{@code org.aventyrs.core.character.services.InitiativeBlessingService#resolveBlessings}
 *   scans a Character's Vantagens de Ego/Habilidades/Habilidades de Competência for ones that
 *   grant a bonus the moment their holder wins initiative for their group — e.g. {@code
 *   org.aventyrs.core.ego.InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s "o seu Movimento
 *   Base e o de seus aliados aumentam em +2UD." {@code org.aventyrs.core.scene.Scene
 *   #applyInitiativeBlessings} is the one that actually applies these, as a {@link
 *   TemporaryBonus} on every intended recipient's {@code CombatantSheet}.
 *   <li>An {@link Interaction}'s own {@link InteractionResult#getBlessings()} — e.g. {@code
 *   ArtesCompetencyAbility#DOM_BARDICO} or {@code
 *   org.aventyrs.core.title.santo.GritoDeGuerraVulcanoInteraction} — reports one or more of
 *   these as the outcome of a directly-activated roll/ability; a caller resolves the concrete
 *   recipients (via {@code Scene#getAllies}/{@code #getEnemies}, or the actor itself for {@link
 *   TargetScope#SELF}/{@link TargetScope#SELF_AND_ALLIES}) and calls {@code CombatantSheet
 *   #grantTemporaryBonus} on each. This core doesn't apply these itself — same "compute what,
 *   caller applies who" restraint the initiative-win mechanism above already established.
 * </ul>
 *
 * A {@code Blessing} isn't necessarily consumed by anything yet — e.g. one typed {@code
 * ModifierType#ABSOLUTE_DAMAGE_REDUCTION} is still grantable-but-inert, since {@code
 * DamageServiceImpl#getTotalAbsoluteDamageReduction} only ever sums continuously-scanned passive
 * hooks and never reads {@code CombatantSheet#getTemporaryBonus}. It's real, grantable data all
 * the same (see CLAUDE.md's "can't apply it yet doesn't mean can't compute it yet" discipline),
 * and that keeps paying off. Two of these have since gone live with no change at any granting
 * site: a {@code ModifierType#DEFESAS}-typed Blessing — e.g. the one {@code
 * org.aventyrs.core.title.santo.GritoDeGuerraVulcanoInteraction} grants — is now summed for real
 * by {@code org.aventyrs.core.character.services.DefenseService}, and a {@code
 * ModifierType#ACTION_POINTS}/{@code #REACTIONS}/{@code #FREE_ACTIONS}-typed one is now read by
 * the {@code CombatantSheet}-taking overloads of {@code
 * org.aventyrs.core.action.ActionPointsService#getMaxActionPoints}/{@code
 * org.aventyrs.core.character.services.ReactionsService#getTotalReactions}/{@code
 * org.aventyrs.core.character.services.FreeActionsService#getTotalFreeActions}, and a {@code
 * ModifierType#DAMAGE_REDUCTION}-typed one — {@code AnaoFeat#VIGOR_DO_INVERNO}'s combat-start
 * grant — by the {@code CombatantSheet}-taking overload of {@code
 * org.aventyrs.core.character.services.DamageService#getTotalDamageReduction}. Note the
 * qualifier on those: the {@code Character}-only overloads structurally cannot see a
 * sheet's {@code TemporaryBonus}, so a caller holding only a {@code Character} still reads the
 * unblessed total. A {@code ModifierType#CRITICAL_RESISTANCE}-typed one (same grant) is read by
 * {@code AbstractSkillInteraction} off the <em>attack target</em>'s sheet.
 *
 * <p>{@code source} identifies which trait granted this — e.g. {@code "DOM_BARDICO"}/
 * {@code "GRITO_DE_GUERRA_VULCANO"} — so a caller aggregating several {@code Blessing}s from
 * different traits at once (e.g. {@code InitiativeBlessingService#resolveBlessings}, which
 * concatenates every Vantagem de Ego/Habilidade/Habilidade de Competência a Character holds
 * into one flat list) can still tell which one granted which, for logging/display or targeted
 * revocation. Where the granting site already has the real ability enum constant in hand (the
 * common case), pass its own {@code name()} rather than a hand-duplicated string literal, so
 * the two can never drift apart — see {@code ArtesInteraction}/{@code
 * GritoDeGuerraVulcanoInteraction} for the pattern. This core still doesn't track *who* (which
 * Character) granted a bonus, only *what trait* did — the same restraint {@code
 * CombatantSheet#grantTemporaryBonus} itself already applies.
 */
@Getter
public class Blessing {

    /**
     * What {@link #getMaximumSimultaneous()} is for every Blessing whose own rules text says
     * nothing about stacking — one, which is the ordinary "a trait re-triggering renews its own
     * effect rather than adding a second". Named rather than inlined so the meaning the older
     * constructors hand their callers is greppable.
     */
    public static final int DEFAULT_MAXIMUM_SIMULTANEOUS = 1;

    private final ModifierType modifierType;
    private final int value;
    private final int rounds;
    private final TargetScope scope;
    private final String source;

    /**
     * A ceiling on everything this Blessing may deliver over its whole life, or {@code null} for
     * the usual Blessing — which simply presents its {@link #getValue()} every Rodada until the
     * count runs out, with no running total to bound.
     *
     * <p>Only a Blessing that <em>acts</em> each Rodada rather than contributing to a stat can
     * need one, and Regeneração Reativa is the case: "a quantidade de PV recuperados desta forma
     * não pode superar os danos sofridos" caps the <em>sum</em> of its healing at the damage that
     * triggered it, which neither {@link #getValue()} nor {@link #getRounds()} can express (2PV a
     * Rodada over 3 Rodadas must still stop at 3 total when 3 was the damage). Enforced by
     * {@link Regeneration}, not by whoever grants the Blessing.
     */
    private final Integer totalLimit;

    /**
     * How many instances of this grant its recipient may hold at once — {@link
     * #DEFAULT_MAXIMUM_SIMULTANEOUS} for every Blessing but one, since {@link
     * org.aventyrs.core.sheet.TemporaryBonus} already refuses to accumulate with another from the
     * same source and type, replacing it and thereby renewing its duration. Regeneração Reativa's
     * "Efeito não cumulativo" is exactly that default; {@code
     * TrollFeat#REGENERACAO_REATIVA_SUPERIOR}'s "se tornam cumulativos, podendo somar uma
     * quantidade de efeitos simultâneos igual 1+ número de Títulos Aventyr Despertos" is what
     * raises it.
     *
     * <p><b>A column here, resolved by whoever grants the Blessing.</b> This used to be a
     * parameter of {@code CombatantSheet#grantBlessing} instead, on the grounds that the trait
     * lifting the ceiling is not the trait that granted the Blessing — which conflated <i>who
     * resolves the number</i> with <i>what the number describes</i>. The ability granting a
     * Blessing is precisely the one that knows which traits may lift its own ceiling, so it scans
     * for them and states the answer here (see {@code TrollsRacialAbility#REGENERACAO_REATIVA});
     * a generic consumer guessing on its behalf would hand a regeneration-specific figure to every
     * unrelated Blessing it happened to be applying in the same pass.
     *
     * <p>Unlike {@link #getTotalLimit()}, which is structurally meaningful only for a Blessing that
     * <i>acts</i> each Rodada, this column is honoured for every {@link ModifierType}.
     */
    private final int maximumSimultaneous;

    public Blessing(final ModifierType modifierType, final int value, final int rounds, final TargetScope scope, final String source) {
        this(modifierType, value, rounds, scope, source, null);
    }

    public Blessing(final ModifierType modifierType, final int value, final int rounds, final TargetScope scope,
                    final String source, final Integer totalLimit) {
        this(modifierType, value, rounds, scope, source, totalLimit, DEFAULT_MAXIMUM_SIMULTANEOUS);
    }

    public Blessing(final ModifierType modifierType, final int value, final int rounds, final TargetScope scope,
                    final String source, final Integer totalLimit, final int maximumSimultaneous) {
        this.modifierType = modifierType;
        this.value = value;
        this.rounds = rounds;
        this.scope = scope;
        this.source = source;
        this.totalLimit = totalLimit;
        this.maximumSimultaneous = maximumSimultaneous;
    }
}
