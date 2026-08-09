package org.aventyrs.core.skill.esquivaeaparar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

/**
 * The specialization a player must choose when a character trains Esquiva e Aparar — it
 * defines which defensive equipment that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum EsquivaEApararSpecialization implements SkillSpecialization {
    LUTADOR_LEVE("Especialista em combates com equipamentos Defensivos (não-tecnológicos) " +
            "cuja Categoria Natural seja Leve."),
    SOLDADO_DE_INFANTARIA("Especialista em combates com equipamentos Defensivos " +
            "(não-tecnológicos) cuja Categoria Natural seja Média."),
    PESO_PESADO("Especialista em combates com equipamentos Defensivos (não-tecnológicos) " +
            "cuja Categoria Natural seja Pesada."),
    PROTECAO_TECNOLOGICA("Especialista em combates com equipamentos Defensivos " +
            "Tecnológicos."),
    GUERREIRO_NATURAL("Especialista em combates usando Defesas Naturais.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ESQUIVA_E_APARAR;
    }
}
