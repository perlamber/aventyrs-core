package org.aventyrs.core.character;

import lombok.NonNull;
import org.aventyrs.core.sheet.ActionCost;

/**
 * One way its holder can start an <b>Efeito de Voo</b> in a stressful Scene — "Iniciar uma ação de
 * voo em situações estressantes, como as Cenas de Combate, exige o uso de 3PD … A Duração do Efeito
 * de Voo é igual à 1d6+Metade do Vigor Rodadas" ({@code FeericoFeat#ASAS}). Each winged source states
 * its own price and Duração, so a holder with two pairs of wings (an Aviano who also took Asas) is
 * offered two of these and picks one.
 *
 * <p>Resolved for one holder at one moment: the per-Título cost reductions, the half-Atributo
 * Duração term and every other Talento's adjustment (Benção de Flora's −1PM and 2d6+½Foco, Coração
 * Alado's +1d6, Eterno Viajante's unlimited Duração) are already folded in by {@code
 * FlightService#getFlightActivations}. A value, compared by equality, so a caller hands back the one
 * it was offered.
 *
 * @param source                 the trait that gives the wings, as shown to the player
 * @param actionCost             the Tempo de Ativação — {@link ActionCost#NONE} where the text names
 *                               none (⚠️ a reading: Voar itself is free, only the resource is stated)
 * @param determinationPointCost PD spent on taking off, never negative
 * @param magicPointCost         PM spent on taking off, never negative
 * @param duration               the dice the caller rolls for the Duração in Rodadas, or {@code null}
 *                               for a flight with no limit ({@code AvianoFeat#ETERNO_VIAJANTE})
 */
public record FlightActivation(@NonNull String source, @NonNull ActionCost actionCost, int determinationPointCost,
                               int magicPointCost, Dice duration) {

    public FlightActivation {
        determinationPointCost = Math.max(0, determinationPointCost);
        magicPointCost = Math.max(0, magicPointCost);
    }

    public FlightActivation withMagicPointCost(final int cost) {
        return new FlightActivation(source, actionCost, determinationPointCost, cost, duration);
    }

    public FlightActivation withDuration(final Dice dice) {
        return new FlightActivation(source, actionCost, determinationPointCost, magicPointCost, dice);
    }

    /** Whether this flight lasts until its holder lands, with no Duração to roll. */
    public boolean isUnlimited() {
        return duration == null;
    }
}
