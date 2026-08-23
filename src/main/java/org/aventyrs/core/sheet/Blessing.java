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
 * ModifierType#ACTION_POINTS} is still grantable-but-inert, since {@code ActionPointsServiceImpl}
 * reads only {@code Character#getTemporaryActionPointsBonus()}. It's real, grantable data all
 * the same (see CLAUDE.md's "can't apply it yet doesn't mean can't compute it yet" discipline),
 * and that pays off: a {@code ModifierType#DEFESAS}-typed Blessing — e.g. the one {@code
 * org.aventyrs.core.title.santo.GritoDeGuerraVulcanoInteraction} grants — was inert for several
 * revisions and is now summed for real by {@code
 * org.aventyrs.core.character.services.DefenseService}, with no change needed at the granting
 * site.
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
    private final ModifierType modifierType;
    private final int value;
    private final int rounds;
    private final TargetScope scope;
    private final String source;

    public Blessing(final ModifierType modifierType, final int value, final int rounds, final TargetScope scope, final String source) {
        this.modifierType = modifierType;
        this.value = value;
        this.rounds = rounds;
        this.scope = scope;
        this.source = source;
    }
}
