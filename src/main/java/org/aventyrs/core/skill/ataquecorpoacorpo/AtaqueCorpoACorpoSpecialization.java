package org.aventyrs.core.skill.ataquecorpoacorpo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * The specialization a player must choose when a character trains Ataque Corpo-a-Corpo — it
 * defines which weapons/delivery methods that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoSpecialization implements SkillSpecialization {
    INFANTARIA_LEVE("Especialista em combates com armas cuja Categoria Base seja Leve ou " +
            "Média.", Set.of(ItemWeightClass.LIGHT, ItemWeightClass.MEDIUM)),
    INFANTARIA_PESADA("Especialista em combates com armas cuja Categoria Base é Pesada.",
            Set.of(ItemWeightClass.HEAVY)),
    ARCANISTA_DE_LINHA_DE_FRENTE("Domínio de técnicas de conjuração de Magias ofensivas de " +
            "Toque.", Set.of()),
    PRIMAL("Combatentes que utilizam Armas Naturais e de Fortalecimento.", Set.of()),
    ARMAS_TECNOLOGICAS("Ataques com armas de Xajah, Tesla, Vapor Elduriano e outras " +
            "tecnologias.", Set.of());

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
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }
}
