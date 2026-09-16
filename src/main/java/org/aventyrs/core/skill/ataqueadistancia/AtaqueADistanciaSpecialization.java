package org.aventyrs.core.skill.ataqueadistancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * The specialization a player must choose when a character trains Ataque à Distância — it
 * defines which weapons/delivery methods that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaSpecialization implements SkillSpecialization {
    TECNICAS_DE_ARREMESSO("Conhecimento de técnicas de arremessos de armas diversas.", Set.of()),
    ARMAS_TECNOLOGICAS("Disparos com armas de Xajah, Tesla, Vapor Elduriano e outras " +
            "tecnologias.", Set.of()),
    // "Natural" isn't an ItemWeightClass — in equipamentos.txt it fills the rarity slot of a
    // heading ("Leve/Natural") — so only the Leve half of the clause is expressed here.
    ARTILHARIA_LEVE("Especialista no uso de Armas à Distância (não tecnológicas) de " +
            "Categoria Base Natural ou Leve.", Set.of(ItemWeightClass.LIGHT)),
    ARTILHARIA_PESADA("Especialista no uso de Armas à Distância (não tecnológicas) de " +
            "Categoria Base Média ou Pesada.", Set.of(ItemWeightClass.MEDIUM, ItemWeightClass.HEAVY)),
    CONJURADOR_DE_LINHA_DE_TRAS("Domínio de técnicas de conjuração de Magias ofensivas de " +
            "longo alcance.", Set.of());

    private final String description;

    /**
     * The weapon Categorias Base this specialization covers, as {@link ItemWeightClass}es. Empty
     * when the specialization is defined by something other than weight (a delivery method, or
     * a kind of weapon of any weight). Read by {@link org.aventyrs.core.skill.AttackSpecializations}
     * to pick the Especialização an ordinary weapon is rolled with.
     */
    private final Set<ItemWeightClass> weightClasses;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
