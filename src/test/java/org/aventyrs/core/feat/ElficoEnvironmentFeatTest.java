package org.aventyrs.core.feat;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Altitude;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.LightLevel;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElficoEnvironmentFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void corruptorRespondsToLightingOnly() {
        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.CORRUPTOR_SOMBRIO,
                new EnvironmentalState(LightLevel.DARK, Altitude.ORDINARY, false, false),
                SkillType.ARTES));
        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.CORRUPTOR_SOMBRIO,
                new EnvironmentalState(LightLevel.SHADOWED, Altitude.ORDINARY, false, false),
                SkillType.ARTES));
        assertEquals(Skill.DISADVANTAGE_MALUS, bonus(ElficoFeat.CORRUPTOR_SOMBRIO,
                new EnvironmentalState(LightLevel.BRIGHT, Altitude.ORDINARY, false, false),
                SkillType.ARTES));
        assertEquals(0, bonus(ElficoFeat.CORRUPTOR_SOMBRIO, EnvironmentalState.ORDINARY, SkillType.ARTES));
    }

    @Test
    void guardianDasNuvensActivatesAtHighAltitudeOrWhileFlying() {
        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.GUARDIAO_DAS_NUVENS,
                new EnvironmentalState(LightLevel.NORMAL, Altitude.HIGH, false, false),
                SkillType.FURTIVIDADE));
        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.GUARDIAO_DAS_NUVENS,
                new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, true, false),
                SkillType.EMPATIA_SELVAGEM));
        assertEquals(0, bonus(ElficoFeat.GUARDIAO_DAS_NUVENS, EnvironmentalState.ORDINARY,
                SkillType.FURTIVIDADE));
    }

    @Test
    void guardianDasProfundezasGrantsOnlyAttackAndFurtividadeWhenSubmerged() {
        EnvironmentalState submerged = new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, false, true);

        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS, submerged,
                SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(Skill.ADVANTAGE_BONUS, bonus(ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS, submerged,
                SkillType.FURTIVIDADE));
        assertEquals(0, bonus(ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS, submerged, SkillType.EMPATIA_SELVAGEM));
        assertEquals(0, bonus(ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS, EnvironmentalState.ORDINARY,
                SkillType.FURTIVIDADE));
    }

    private static int bonus(final ElficoFeat feat, final EnvironmentalState state, final SkillType skillType) {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false, null, null,
                state);
        return feat.resolveSkillRollBonus(skillType, context, null,
                CharacterFixture.blank(CharacterFixture.BLANK).build());
    }
}
