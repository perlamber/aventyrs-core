package org.aventyrs.core.sheet;

import lombok.NonNull;

/**
 * What a combatant is riding — "enquanto estiver montado ou dirigindo". Held on the rider's own sheet
 * ({@link CombatantSheet#getRiding()}) and set by {@code MountService#mount}. Nothing here moves
 * anybody: positions stay the caller's, and the rider and a {@link #steed()} share a hex only as far as
 * the caller places them.
 *
 * @param kind  a Montaria (an animal cavalgado) or a Veículo (dirigido)
 * @param steed the mount's own sheet, when it is a combatant with Pontos de Ação of its own — what
 *              {@code CavalariaFeat#MONTARIA_DE_COMBATE} spends; {@code null} for a vehicle, or a
 *              mount the table does not run as a combatant
 */
public record Riding(@NonNull Kind kind, CombatantSheet steed) {

    /** "Cavalgando um animal" or "dirigindo um veículo". */
    public enum Kind {
        MONTARIA,
        VEICULO
    }

    public static Riding montaria(final CombatantSheet steed) {
        return new Riding(Kind.MONTARIA, steed);
    }

    public static Riding veiculo() {
        return new Riding(Kind.VEICULO, null);
    }
}
