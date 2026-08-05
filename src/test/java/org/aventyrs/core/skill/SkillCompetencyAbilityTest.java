package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillCompetencyAbilityTest {

    @Test
    void resolveAttributeDomainReturnsTheDefaultWhenNoAbilityTargetsThisSkill() {
        AttributeDomain resolved = SkillCompetencyAbility.resolveAttributeDomain(
                List.of(AtaqueCorpoACorpoCompetencyAbility.ATAQUE_PRECISO), SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH);

        assertEquals(AttributeDomain.STRENGTH, resolved);
    }

    @Test
    void resolveAttributeDomainReturnsTheSubstitutionWhenAMatchingAbilityGrantsOne() {
        AttributeDomain resolved = SkillCompetencyAbility.resolveAttributeDomain(
                List.of(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE), SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH);

        assertEquals(AttributeDomain.DEXTERITY, resolved);
    }

    @Test
    void resolveAttributeDomainIgnoresASubstitutingAbilityForADifferentSkill() {
        AttributeDomain resolved = SkillCompetencyAbility.resolveAttributeDomain(
                List.of(AtletismoCompetencyAbility.ACROBATA), SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH);

        assertEquals(AttributeDomain.STRENGTH, resolved);
    }
}
