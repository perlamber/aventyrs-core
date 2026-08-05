package org.aventyrs.core.skill.conhecimentos;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Conhecimentos — it defines
 * which field of study that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum ConhecimentosSpecialization {
    METAMAGICO("Conhecimentos sobre magias e tradições mágicas, runas e símbolos arcanos, " +
            "construtos mágicos, mistérios sobrenaturais."),
    GEO_HISTORIA("Conhecimentos sobre terrenos, climas, povos, linhagens, leis, datas " +
            "importantes, lendas, tradições."),
    NATUREZA("Conhecimentos sobre as criaturas e monstros selvagens, fauna e flora, e " +
            "sobre as criaturas feéricas."),
    COSMOLOGIA("Estudo sobre os Deuses, os Planos, Mundo dos Sonhos, e da constituição do " +
            "Multiverso."),
    CIENCIAS_DIVERSAS("Você é conhecedor de estudos ligados à Antropologia, Astronomia, " +
            "Filosofia, Física e Matemática.");

    private final String description;
}
