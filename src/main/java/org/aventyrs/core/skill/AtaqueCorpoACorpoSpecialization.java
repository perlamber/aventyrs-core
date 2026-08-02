package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Ataque Corpo-a-Corpo — it
 * defines which weapons/delivery methods that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoSpecialization {
    INFANTARIA_LEVE("Especialista em combates com armas cuja Categoria Base seja Leve ou " +
            "Média."),
    INFANTARIA_PESADA("Especialista em combates com armas cuja Categoria Base é Pesada."),
    ARCANISTA_DE_LINHA_DE_FRENTE("Domínio de técnicas de conjuração de Magias ofensivas de " +
            "Toque."),
    PRIMAL("Combatentes que utilizam Armas Naturais e de Fortalecimento."),
    ARMAS_TECNOLOGICAS("Ataques com armas de Xajah, Tesla, Vapor Elduriano e outras " +
            "tecnologias.");

    private final String description;
}
