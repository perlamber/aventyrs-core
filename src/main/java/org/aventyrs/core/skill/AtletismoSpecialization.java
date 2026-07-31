package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Atletismo — it defines
 * which athletic actions that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtletismoSpecialization {
    TRI_ATLETA("Especialista em Natação, Escalada e Corrida."),
    ACROBATA("Você tem grande afinidade com Saltos e Acrobacias."),
    LEVANTAMENTO_DE_PESO("Combinando força e técnica você pode levantar grandes pesos."),
    PULMAO_DE_ACO("Você tem grande resistência e capacidade para atividades duradouras."),
    ACADEMICO_ESPORTIVO("Conhecimentos acadêmicos sobre esportes no geral e a capacidade de " +
            "treinar outros.");

    private final String description;
}
