package org.aventyrs.core.feat;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerrenoPrediletoFeatTest {

    private static final SceneContext FOREST = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.FOREST);
    private static final SceneContext DESERT = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.DESERT);

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        TerrenoPrediletoFeat feat = TerrenoPrediletoFeat.of(TerrainType.FOREST);

        assertSame(SobrevivenciaFeat.TERRENO_PREDILETO, feat.catalogEntry());
        assertEquals(SobrevivenciaFeat.TERRENO_PREDILETO.getDescription(), feat.getDescription());
    }

    @Test
    void requiresAChosenTerrain() {
        assertThrows(NullPointerException.class, () -> TerrenoPrediletoFeat.of(null));
    }

    @Test
    void grantsVantagemOnFurtividadeOnlyInTheChosenTerrain() {
        TerrenoPrediletoFeat feat = TerrenoPrediletoFeat.of(TerrainType.FOREST);

        assertEquals(Skill.ADVANTAGE_BONUS,
                feat.resolveSkillRollBonus(SkillType.FURTIVIDADE, FOREST, null, null));
        assertEquals(0, feat.resolveSkillRollBonus(SkillType.FURTIVIDADE, DESERT, null, null));
        assertEquals(0, feat.resolveSkillRollBonus(SkillType.FURTIVIDADE, null, null, null));
    }

    @Test
    void grantsVantagemOnConhecimentosOnlyForNaturezaInTheChosenTerrain() {
        TerrenoPrediletoFeat feat = TerrenoPrediletoFeat.of(TerrainType.FOREST);

        assertEquals(Skill.ADVANTAGE_BONUS, feat.resolveSkillRollBonus(
                SkillType.CONHECIMENTOS, FOREST, ConhecimentosSpecialization.NATUREZA, null));
        assertEquals(0, feat.resolveSkillRollBonus(SkillType.CONHECIMENTOS, FOREST, null, null));
    }

    @Test
    void grantsPlusTwoToBothDefensesOnlyInTheChosenTerrain() {
        TerrenoPrediletoFeat feat = TerrenoPrediletoFeat.of(TerrainType.FOREST);

        assertEquals(2, feat.resolveDefenseBonus(DefenseType.PHYSICAL, null, FOREST));
        assertEquals(2, feat.resolveDefenseBonus(DefenseType.MAGIC, null, FOREST));
        assertEquals(0, feat.resolveDefenseBonus(DefenseType.PHYSICAL, null, DESERT));
        assertEquals(0, feat.resolveDefenseBonus(DefenseType.PHYSICAL, null, null));
    }
}
