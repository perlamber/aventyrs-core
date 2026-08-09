package org.aventyrs.core.skill.ataqueadistancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

/**
 * The specialization a player must choose when a character trains Ataque à Distância — it
 * defines which weapons/delivery methods that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaSpecialization implements SkillSpecialization {
    TECNICAS_DE_ARREMESSO("Conhecimento de técnicas de arremessos de armas diversas."),
    ARMAS_TECNOLOGICAS("Disparos com armas de Xajah, Tesla, Vapor Elduriano e outras " +
            "tecnologias."),
    ARTILHARIA_LEVE("Especialista no uso de Armas à Distância (não tecnológicas) de " +
            "Categoria Base Natural ou Leve."),
    ARTILHARIA_PESADA("Especialista no uso de Armas à Distância (não tecnológicas) de " +
            "Categoria Base Média ou Pesada."),
    CONJURADOR_DE_LINHA_DE_TRAS("Domínio de técnicas de conjuração de Magias ofensivas de " +
            "longo alcance.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
