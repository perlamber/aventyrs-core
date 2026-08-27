package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * The report of a completed {@link CombatantSheet#spendEgoPoints} — which {@link EgoDomain}
 * was drawn from, from which of that domain's two pools, and how many points actually left
 * it. Purely a report: the spend has already happened by the time one of these exists.
 *
 * <p>{@code value} is the amount <em>actually</em> spent after clamping, never the amount
 * requested — a spend floors at 0 rather than throwing (see {@link
 * CombatantSheet#spendEgoPoints}), so asking for 3 points from a pool holding 1 reports 1.
 * Callers that promise the points back — {@code org.aventyrs.core.effect.Primor}, via {@link
 * PendingEgoRecovery} — must register this figure, not what they asked for, or a later
 * recovery hands back points the target had spent themselves.
 *
 * <p>{@link #getType()} is the field {@code
 * org.aventyrs.core.ego.AutocontroleAdvantage#DETERMINACAO_HEROICA} needs, whose recovery is
 * doubled "se o ponto for permanente" — that distinction is unresolvable from the sheet after
 * the fact (both pools are just counters), so it is reported here at the moment of the spend.
 */
@Getter
public class EgoPointSpend {
    private final EgoDomain domain;
    private final EgoPointType type;
    private final int value;

    public EgoPointSpend(final EgoDomain domain, final EgoPointType type, final int value) {
        this.domain = domain;
        this.type = type;
        this.value = value;
    }
}
