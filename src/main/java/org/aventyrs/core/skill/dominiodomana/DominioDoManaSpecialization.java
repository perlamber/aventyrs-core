package org.aventyrs.core.skill.dominiodomana;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

/**
 * The specialization a player must choose when a character trains Domínio do Mana — it
 * defines which kind of Magias that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaSpecialization implements SkillSpecialization {
    INVOCADOR("Você se especializou em Magias do tipo Invocação."),
    ENCANTADOR("Sua especialidade é o uso de Magias do tipo Encantamento ou Divinas."),
    FEITICARIA_OFENSIVA("Seu estilo de combate é baseado em Magias que afetem alvos " +
            "inimigos ou Profanas."),
    UTILIDADE_ARCANA("Você utiliza magias que não afetam personagens, apenas terrenos ou " +
            "objetos."),
    ARCANISMO_HEREDITARIO("Você se especializou em Mimetizar Magias ou suas Habilidades " +
            "Mágicas Raciais.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }
}
