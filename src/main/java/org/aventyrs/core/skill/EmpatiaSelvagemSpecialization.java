package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Empatia Selvagem — it
 * defines which creatures that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum EmpatiaSelvagemSpecialization {
    ANIMAIS_MUNDANOS("Interação com mamíferos, répteis, anfíbios, aves e peixes modernos e " +
            "comuns."),
    MENTALIDADE_ARTROPODE("Você compreende a cultura dos insetos, aracnídeos, crustáceos, " +
            "quilópodes, dentre outros."),
    DRACO_SAUROIDES("Você é especialista em criaturas selvagens pré-históricas ou " +
            "aparentadas aos dragões (não aplicável às criaturas inteligentes e " +
            "civilizadas)."),
    EMPATIA_MONSTRUOSA("Comunicação com monstros irracionais."),
    CONEXAO_EXTRAPLANAR("Comunicação com criaturas selvagens de naturezas Abissais, " +
            "Celestiais, Elementais ou Umbrais.");

    private final String description;
}
