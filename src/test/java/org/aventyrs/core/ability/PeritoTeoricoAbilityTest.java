package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PeritoTeoricoAbilityTest {

    @Test
    void everyConstantBelongsToGnose() {
        for (PeritoTeoricoAbility ability : PeritoTeoricoAbility.values()) {
            assertEquals(AttributeDomain.GNOSE, ability.getAttributeDomain());
        }
    }

    @Test
    void everyConstantSharesPeritoTeoricosRulesText() {
        for (PeritoTeoricoAbility ability : PeritoTeoricoAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
            assertEquals(GnoseAbility.PERITO_TEORICO.getDescription(), ability.getDescription());
        }
    }

    @Test
    void thereIsExactlyOneConstantPerSkillType() {
        Set<SkillType> coveredSkillTypes = Arrays.stream(PeritoTeoricoAbility.values())
                .map(PeritoTeoricoAbility::getSkillType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SkillType.class)));

        assertEquals(SkillType.values().length, PeritoTeoricoAbility.values().length);
        assertEquals(EnumSet.allOf(SkillType.class), coveredSkillTypes);
    }

    @Test
    void resolveAttributeDomainReturnsGnoseWhenTheListContainsTheMatchingConstant() {
        List<AttributeAbility> attributeAbilities = List.of(PeritoTeoricoAbility.FURTIVIDADE);

        AttributeDomain resolved = PeritoTeoricoAbility.resolveAttributeDomain(
                attributeAbilities, SkillType.FURTIVIDADE, AttributeDomain.INSTINCT);

        assertEquals(AttributeDomain.GNOSE, resolved);
    }

    @Test
    void resolveAttributeDomainReturnsTheDefaultWhenTheListContainsADifferentConstant() {
        List<AttributeAbility> attributeAbilities = List.of(PeritoTeoricoAbility.FURTIVIDADE);

        AttributeDomain resolved = PeritoTeoricoAbility.resolveAttributeDomain(
                attributeAbilities, SkillType.PERSUASAO, AttributeDomain.CHARISMA);

        assertEquals(AttributeDomain.CHARISMA, resolved);
    }

    @Test
    void resolveAttributeDomainReturnsTheDefaultWhenTheListIsEmpty() {
        AttributeDomain resolved = PeritoTeoricoAbility.resolveAttributeDomain(
                List.of(), SkillType.FURTIVIDADE, AttributeDomain.INSTINCT);

        assertEquals(AttributeDomain.INSTINCT, resolved);
    }
}
