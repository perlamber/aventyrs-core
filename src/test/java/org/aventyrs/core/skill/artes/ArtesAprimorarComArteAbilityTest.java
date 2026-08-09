package org.aventyrs.core.skill.artes;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtesAprimorarComArteAbilityTest {

    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void belongsToArtesAndSharesTheCatalogDescription() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ATLETISMO);

        assertEquals(SkillType.ARTES, ability.getSkillType());
        assertEquals(ArtesCompetencyAbility.APRIMORAR_COM_ARTE.getDescription(), ability.getDescription());
        assertFalse(ability.getDescription().isBlank());
    }

    @Test
    void requiresAChosenSkill() {
        assertThrows(NullPointerException.class, () -> new ArtesAprimorarComArteAbility(null));
    }

    @Test
    void choosingEsquivaEApararGrantsTheDamageReductionBranch() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new ArtesAprimorarComArteAbility(SkillType.ESQUIVA_E_APARAR))
                .build();

        assertEquals(ArtesAprimorarComArteAbility.BENEFIT_BONUS, damageService.getTotalDamageReduction(character));
    }

    @Test
    void choosingAnyOtherSkillGrantsNoDamageReduction() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new ArtesAprimorarComArteAbility(SkillType.ATAQUE_CORPO_A_CORPO))
                .build();

        assertEquals(0, damageService.getTotalDamageReduction(character));
    }

    @Test
    void choosingAnAttackSkillGrantsItsBaseDamageBonus() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(ArtesAprimorarComArteAbility.BENEFIT_BONUS, ability.getBaseDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void baseDamageBonusDoesNotApplyToTheOtherAttackSkill() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(0, ability.getBaseDamageBonus(SkillType.ATAQUE_A_DISTANCIA));
    }

    @Test
    void choosingANonAttackSkillGrantsNoBaseDamageBonus() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ESQUIVA_E_APARAR);

        assertEquals(0, ability.getBaseDamageBonus(SkillType.ESQUIVA_E_APARAR));
        assertEquals(0, ability.getBaseDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void choosingAnOtherPericiaGrantsItsCriticalMarginReduction() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ATLETISMO);

        assertEquals(ArtesAprimorarComArteAbility.BENEFIT_BONUS, ability.getCriticalMarginReduction(SkillType.ATLETISMO));
    }

    @Test
    void criticalMarginReductionDoesNotApplyToADifferentSkillThanChosen() {
        ArtesAprimorarComArteAbility ability = new ArtesAprimorarComArteAbility(SkillType.ATLETISMO);

        assertEquals(0, ability.getCriticalMarginReduction(SkillType.PERSUASAO));
    }

    @Test
    void choosingAnAttackSkillOrEsquivaEApararGrantsNoCriticalMarginReduction() {
        ArtesAprimorarComArteAbility attackAbility = new ArtesAprimorarComArteAbility(SkillType.ATAQUE_A_DISTANCIA);
        ArtesAprimorarComArteAbility esquivaAbility = new ArtesAprimorarComArteAbility(SkillType.ESQUIVA_E_APARAR);

        assertEquals(0, attackAbility.getCriticalMarginReduction(SkillType.ATAQUE_A_DISTANCIA));
        assertEquals(0, esquivaAbility.getCriticalMarginReduction(SkillType.ESQUIVA_E_APARAR));
    }
}
