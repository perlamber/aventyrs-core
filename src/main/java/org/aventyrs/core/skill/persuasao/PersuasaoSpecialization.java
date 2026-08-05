package org.aventyrs.core.skill.persuasao;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Persuasão — it defines
 * which social approaches that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum PersuasaoSpecialization {
    OBTER_INFORMACOES("Técnicas de comunicação e interrogatórios para conseguir " +
            "informações especificas."),
    INTIMIDACAO("Você sabe ser assustador quando precisa, impondo sua vontade através do " +
            "medo."),
    MENTIR_OU_OMITIR("Você sabe como blefar, enganar ou ocultar informações de outras " +
            "pessoas."),
    NEGOCIACAO("A arte da diplomacia e das barganhas."),
    COMUNICACAO("Você domina técnicas de discursos, comunicações formais, manhas urbanas e " +
            "tribais.");

    private final String description;
}
