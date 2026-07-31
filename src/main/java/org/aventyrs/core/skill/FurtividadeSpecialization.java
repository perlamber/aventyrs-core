package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Furtividade — it defines
 * which discreet actions that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum FurtividadeSpecialization {
    MAESTRIA_DA_OCULTACAO("Capacidade de se esconder e ocultar rastros."),
    TECNICAS_DE_LADINICE("Habilidade de furtar e técnicas de manipular objetos rapidamente " +
            "para ludibriar espectadores."),
    GOLPISTA("Capacidade de criar falsificações de objetos."),
    INFILTRADOR("Habilidades de disfarce e de se passar por outras pessoas."),
    ARMADILHEIRO("Habilidade de colocar Armadilhas Móveis e Estacionárias ocultas na cena " +
            "ou cenário.");

    private final String description;
}
