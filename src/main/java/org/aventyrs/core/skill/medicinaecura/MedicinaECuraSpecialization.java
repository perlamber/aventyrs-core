package org.aventyrs.core.skill.medicinaecura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

/**
 * The specialization a player must choose when a character trains Medicina e Cura — it
 * defines which treatments that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum MedicinaECuraSpecialization implements SkillSpecialization {
    ALQUIMIA("Criação de Venenos e Poções."),
    HERBANARIO("Você é especialista em lidar doenças e venenos mundanos e capaz de criar " +
            "Antídotos e Remédios."),
    XAMANISMO("Você sabe como identificar e interromper doenças mágicas e maldições."),
    HOSPITALARIO("Primeiros-socorros e curas de ferimentos, comuns e de batalha."),
    EXORCISTA("Habilidade de identificar e interromper possessões.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.MEDICINA_E_CURA;
    }
}
