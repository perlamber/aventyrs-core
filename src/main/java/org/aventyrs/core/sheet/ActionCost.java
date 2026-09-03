package org.aventyrs.core.sheet;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_ACTION_COST;

/**
 * What one action a combatant takes <b>cost</b> — a Pontos de Ação amount, an Ação Livre, or a
 * Reação. Carried on a {@link org.aventyrs.core.skill.SkillRoll} as optional roll-metadata and
 * recorded on a {@link CombatantAction} in the per-Rodada action log, so a Talento gated on an
 * attack's cost ({@code AssassinoFeat#SAQUE_RELAMPAGO}: "utilizando apenas 1PA ou Ação Livre")
 * can see it.
 *
 * <p>This core does not run a Pontos de Ação economy — nothing spends PA or checks affordability
 * mid-exchange (see {@code ActionPointsService}, which only computes maximums). The caller
 * decides what an action cost and states it here; {@link #kind} plus {@link #actionPoints} is
 * exactly that statement, validated at construction because it is a system boundary.
 *
 * <p>{@link #actionPoints} is meaningful only for {@link Kind#ACTION_POINTS} and is then
 * {@code >= 1}; a Reação and an Ação Livre both carry 0, and {@link #spentActionPoints()}
 * reports 0 for them — so both satisfy a "1PA ou menos" gate.
 */
public record ActionCost(Kind kind, int actionPoints) {

    /** Whether an action was paid for with Pontos de Ação, taken as an Ação Livre, or as a Reação. */
    public enum Kind { ACTION_POINTS, FREE_ACTION, REACTION }

    /** An Ação Livre — spends no Pontos de Ação. */
    public static final ActionCost FREE_ACTION = new ActionCost(Kind.FREE_ACTION, 0);

    /** A Reação — taken on another combatant's Turn, spends no Pontos de Ação. */
    public static final ActionCost REACTION = new ActionCost(Kind.REACTION, 0);

    public ActionCost {
        boolean pointsMatchKind = (kind == Kind.ACTION_POINTS) == (actionPoints > 0);
        if (kind == null || actionPoints < 0 || !pointsMatchKind) {
            throw new IllegalOperationException(INVALID_ACTION_COST);
        }
    }

    /** An action paid for with actionPoints Pontos de Ação — {@code actionPoints} must be {@code >= 1}. */
    public static ActionCost ofActionPoints(final int actionPoints) {
        return new ActionCost(Kind.ACTION_POINTS, actionPoints);
    }

    /**
     * The Pontos de Ação this action actually spent — {@link #actionPoints} for a {@link
     * Kind#ACTION_POINTS} action, 0 for a Reação or an Ação Livre.
     */
    public int spentActionPoints() {
        return kind == Kind.ACTION_POINTS ? actionPoints : 0;
    }
}
