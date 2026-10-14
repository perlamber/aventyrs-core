package org.aventyrs.core.monster.summon;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.escolhidodemacaria.EscolhidoDeMacariaAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The generated <b>Carrasco Moribundo</b> a Habilidade Monstruosa invokes — Senhor dos Mortos' Procissão dos
 * Desencarnados, Cuspidor de Pragas, Praga Zumbi's risen (core 0.1.6.26). The text names only a Categoria ("um Carrasco
 * Moribundo Presa"), so by table ruling (2026-10-14) the stats are a stock blueprint built the manual's way.
 *
 * <p>⚠️ Every number below is authored here, not read off the rules: GP at the Categoria's floor ({@link
 * MonsterCategory#getMinimumPowerDegree()}), the spell Zumbi's profile within the 10 Atributo points (Força 4, Vigor 3,
 * Destreza 2, Instinto 2, the rest 1), Ataque Corpo-a-Corpo, Esquiva e Aparar and Atenção trained, Garras Afiadas, and
 * the Modelo Escolhido de Macária with Carrasco Morimbundo alone.
 */
public final class CarrascoMorimbundo {

    private CarrascoMorimbundo() {
    }

    /** The stock Carrasco Moribundo of category. */
    public static MonsterBlueprint stock(@NonNull final MonsterCategory category) {
        return MonsterBlueprint.builder()
                .name("Carrasco Moribundo (" + displayName(category) + ")")
                .powerDegree(category.getMinimumPowerDegree())
                .sizeCategory(SizeCategory.ZERO)
                .attributeBase(AttributeDomain.STRENGTH, 4)
                .attributeBase(AttributeDomain.VIGOR, 3)
                .attributeBase(AttributeDomain.DEXTERITY, 2)
                .attributeBase(AttributeDomain.INSTINCT, 2)
                .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .trainedSkill(SkillType.ESQUIVA_E_APARAR)
                .trainedSkill(SkillType.ATTENTION)
                .equipmentItem(NaturalWeapon.GARRAS_AFIADAS)
                .model(MonsterModel.ESCOLHIDO_DE_MACARIA)
                .ability(MonstrousAbilitySelection.of(EscolhidoDeMacariaAbility.CARRASCO_MORIMBUNDO))
                .build();
    }

    private static String displayName(final MonsterCategory category) {
        String name = category.name();
        return name.charAt(0) + name.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
