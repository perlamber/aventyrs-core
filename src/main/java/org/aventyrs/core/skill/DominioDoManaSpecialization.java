package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Domínio do Mana — it
 * defines which kind of Magias that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaSpecialization {
    INVOCADOR("Você se especializou em Magias do tipo Invocação."),
    ENCANTADOR("Sua especialidade é o uso de Magias do tipo Encantamento."),
    FEITICARIA_OFENSIVA("Seu estilo de combate é baseado em Magias que afetem alvos " +
            "inimigos."),
    MEDICINA_ARCANA("Você domina a arte das Magias Divinas e Magias Naturais."),
    ARCANISMO_HEREDITARIO("Você se especializou em suas Habilidades Mágicas Raciais.");

    private final String description;
}
