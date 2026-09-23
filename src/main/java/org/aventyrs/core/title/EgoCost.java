package org.aventyrs.core.title;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;

/**
 * A Título trait's Custo de Ativação paid in <b>temporary Ego points</b> rather than PD — Gigante
 * Enfurecido's "Custo de Ativação: 1 Ponto Temporário de Autocontrole", and the "+1 ponto temporário
 * de Autocontrole" each of its Especializações and Supremas adds on top.
 *
 * <p>The PD twin is {@link PDCost}; a trait may carry both, and {@code
 * AbstractTitleAbilityInteraction#activate} checks and spends each. Only the temporary pool pays:
 * every clause that names this cost says "temporário".
 *
 * @param domain which Ego pays
 * @param points how many temporary points; 0 for no Ego cost at all
 */
public record EgoCost(@NonNull EgoDomain domain, int points) {

    /** No Ego cost — every trait but Gigante Enfurecido's. */
    public static final EgoCost NONE = new EgoCost(EgoDomain.AUTOCONTROLE, 0);

    public EgoCost {
        if (points < 0) {
            throw new IllegalArgumentException("An Ego cost cannot be negative: " + points);
        }
    }

    /** "N Pontos Temporários de Autocontrole". */
    public static EgoCost autocontrole(final int points) {
        return points == 0 ? NONE : new EgoCost(EgoDomain.AUTOCONTROLE, points);
    }

    /** This cost plus extra points of the same Ego — a mode adding "+1 ponto temporário". */
    public EgoCost plus(final int extra) {
        return new EgoCost(domain, points + extra);
    }

    public boolean isFree() {
        return points == 0;
    }
}
