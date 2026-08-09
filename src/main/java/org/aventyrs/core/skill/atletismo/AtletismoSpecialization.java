package org.aventyrs.core.skill.atletismo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;

/**
 * The specialization a player must choose when a character trains Atletismo — it defines
 * which athletic actions that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtletismoSpecialization implements SkillSpecialization {
    TRI_ATLETA("Especialista em Natação, Escalada e Corrida."),
    ACROBATA("Você tem grande afinidade com Saltos e Acrobacias."),
    LEVANTAMENTO_DE_PESO("Combinando força e técnica você pode levantar grandes pesos."),
    PULMAO_DE_ACO("Você tem grande resistência e capacidade para atividades duradouras."),
    ESPORTISTA("Conhecimentos sobre os esportes diversos de Tellus, suas práticas e regras.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
