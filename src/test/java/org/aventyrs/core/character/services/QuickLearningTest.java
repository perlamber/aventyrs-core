package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.GnomoFeat;
import org.aventyrs.core.feat.HumanoFeat;
import org.aventyrs.core.race.Gnomo;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Phase I: Aprendizado Rápido, and the Talentos that extend it. */
class QuickLearningTest {

    private final SkillGraduationService service = new SkillGraduationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** A character whose Ataque Corpo-a-Corpo sits at graduation, recorded for Aprendizado Rápido. */
    private static Character character(final Race race, final int graduation, final Feat... feats) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(graduation - skill.getGraduation().getGraduationValue());
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .quickLearningSkills(Set.of(SkillType.ATAQUE_CORPO_A_CORPO))
                .feats(new ArrayList<>(List.of(feats)))
                .build();
    }

    private BigDecimal cost(final Character character) {
        return service.getUpgradeCost(character, SkillType.ATAQUE_CORPO_A_CORPO);
    }

    @Test
    void theSecondAndThirdGraduacaoCostHalfAPointLess() {
        assertEquals(new BigDecimal("0.5"), cost(character(new Human(), 1)));
        assertEquals(new BigDecimal("1.0"), cost(character(new Human(), 2)));
        assertEquals(new BigDecimal("2"), cost(character(new Human(), 3)));
    }

    @Test
    void onlyARecordedPericiaOfAQuickLearningRaceIsDiscounted() {
        assertEquals(new BigDecimal("1"), cost(character(new Orc(), 1)));

        Character unrecorded = character(new Human(), 1).toBuilder().quickLearningSkills(Set.of()).build();
        assertEquals(new BigDecimal("1"), cost(unrecorded));
    }

    @Test
    void theUpgradeSpendsTheDiscountedCost() {
        Character human = character(new Human(), 1);
        CharacterSheet sheet = CharacterSheet.of(human, new Player());
        sheet.accumulateExperience(new BigDecimal("0.5"));

        service.upgradeGraduation(human, sheet, SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(2, human.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO).getGraduation().getGraduationValue());
    }

    @Test
    void aprendizadoRapidoEContinuoReachesTheFifthThenTheSeventh() {
        Character human = character(new Human(), 4, HumanoFeat.APRENDIZADO_RAPIDO_E_CONTINUO);
        assertEquals(new BigDecimal("2.0"), cost(human));
        assertEquals(new BigDecimal("3"), cost(character(new Human(), 5, HumanoFeat.APRENDIZADO_RAPIDO_E_CONTINUO)));

        Character awakened = character(new Human(), 6, HumanoFeat.APRENDIZADO_RAPIDO_E_CONTINUO);
        awakened.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertEquals(new BigDecimal("3.0"), cost(awakened));
    }

    @Test
    void sabichaoReachesTheSeventh() {
        assertEquals(new BigDecimal("3.0"), cost(character(new Gnomo(), 6, GnomoFeat.SABICHAO)));
        assertEquals(new BigDecimal("4"), cost(character(new Gnomo(), 7, GnomoFeat.SABICHAO)));
    }
}
