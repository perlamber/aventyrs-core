package org.aventyrs.core.skill.esquivaeaparar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * The specialization a player must choose when a character trains Esquiva e Aparar — it
 * defines which defensive equipment that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum EsquivaEApararSpecialization implements SkillSpecialization {
    LUTADOR_LEVE("Especialista em combates com equipamentos Defensivos (não-tecnológicos) " +
            "cuja Categoria Natural seja Leve.", Set.of(ItemWeightClass.LIGHT)),
    SOLDADO_DE_INFANTARIA("Especialista em combates com equipamentos Defensivos " +
            "(não-tecnológicos) cuja Categoria Natural seja Média.", Set.of(ItemWeightClass.MEDIUM)),
    PESO_PESADO("Especialista em combates com equipamentos Defensivos (não-tecnológicos) " +
            "cuja Categoria Natural seja Pesada.", Set.of(ItemWeightClass.HEAVY)),
    PROTECAO_TECNOLOGICA("Especialista em combates com equipamentos Defensivos " +
            "Tecnológicos.", Set.of()),
    GUERREIRO_NATURAL("Especialista em combates usando Defesas Naturais.", Set.of());

    private final String description;

    /**
     * The defensive equipment Categorias Naturais this specialization covers, as {@link
     * ItemWeightClass}es. Empty when the specialization is defined by something other than
     * weight (technological or natural defenses of any weight). Nothing reads it yet.
     */
    private final Set<ItemWeightClass> weightClasses;

    @Override
    public SkillType getSkillType() {
        return SkillType.ESQUIVA_E_APARAR;
    }
}
