package org.aventyrs.core.skill.attention;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Atenção — it defines
 * which perceptive actions that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AttentionSpecialization {
    SENTIDOS_APURADOS("Sua visão, audição e olfato são acima da média, te permitindo uma " +
            "percepção mais ampla do ambiente."),
    INVESTIGAR("Você sabe como procurar objetos escondidos, pistas, evidências e " +
            "Armadilhas (Portáteis e Estacionárias) no cenário."),
    RASTREAR("Domínio de técnicas para localizar ou rastrear alvos, também pode ser " +
            "utilizada para identificar Armadilhas Portáteis no caminho."),
    DISCERNIR_MOTIVACAO("A capacidade de perceber a intenção alheia."),
    IDENTIFICAR_PADROES("Habilidade inata de perceber padrões — e quebras de padrões — " +
            "rapidamente, em objetos, construções e ações. Também pode ser utilizada para " +
            "identificar Armadilhas Estacionárias.");

    private final String description;
}
