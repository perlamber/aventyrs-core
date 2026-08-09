package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnoesRacialAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetWithSizeCategory(SizeCategory sizeCategory) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(sizeCategory)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void everyAbilityHasADescription() {
        for (AnoesRacialAbility ability : AnoesRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void abatedoresDeGigantesBelongsToAtaqueADistancia() {
        assertEquals(SkillType.ATAQUE_A_DISTANCIA, AnoesRacialAbility.ABATEDORES_DE_GIGANTES.getSkillType());
    }

    @Test
    void abatedoresDeGigantesMatchesBothAttackSkillTypes() {
        assertTrue(AnoesRacialAbility.ABATEDORES_DE_GIGANTES.matchesSkillType(SkillType.ATAQUE_A_DISTANCIA));
        assertTrue(AnoesRacialAbility.ABATEDORES_DE_GIGANTES.matchesSkillType(SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void abatedoresDeGigantesDoesNotMatchANonAttackSkillType() {
        assertFalse(AnoesRacialAbility.ABATEDORES_DE_GIGANTES.matchesSkillType(SkillType.ATLETISMO));
        assertFalse(AnoesRacialAbility.ABATEDORES_DE_GIGANTES.matchesSkillType(null));
    }

    @Test
    void abatedoresDeGigantesGrantsAdvantageAgainstATargetTwoSizeCategoriesLarger() {
        CharacterSheet actor = sheetWithSizeCategory(SizeCategory.MINUS_ONE);
        CharacterSheet attackTarget = sheetWithSizeCategory(SizeCategory.PLUS_ONE);

        Optional<Integer> bonus = AnoesRacialAbility.ABATEDORES_DE_GIGANTES.resolveAttackRollBonus(actor, attackTarget);

        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), bonus);
    }

    @Test
    void abatedoresDeGigantesGrantsNoBonusWhenTheDifferenceIsOnlyOneCategory() {
        CharacterSheet actor = sheetWithSizeCategory(SizeCategory.ZERO);
        CharacterSheet attackTarget = sheetWithSizeCategory(SizeCategory.PLUS_ONE);

        assertEquals(Optional.empty(), AnoesRacialAbility.ABATEDORES_DE_GIGANTES.resolveAttackRollBonus(actor, attackTarget));
    }

    @Test
    void abatedoresDeGigantesGrantsNoBonusWhenTheTargetIsSmaller() {
        CharacterSheet actor = sheetWithSizeCategory(SizeCategory.PLUS_TWO);
        CharacterSheet attackTarget = sheetWithSizeCategory(SizeCategory.ZERO);

        assertEquals(Optional.empty(), AnoesRacialAbility.ABATEDORES_DE_GIGANTES.resolveAttackRollBonus(actor, attackTarget));
    }

    @Test
    void abatedoresDeGigantesGrantsNoBonusWithoutAnActorOrAttackTarget() {
        CharacterSheet sheet = sheetWithSizeCategory(SizeCategory.ZERO);

        assertEquals(Optional.empty(), AnoesRacialAbility.ABATEDORES_DE_GIGANTES.resolveAttackRollBonus(null, sheet));
        assertEquals(Optional.empty(), AnoesRacialAbility.ABATEDORES_DE_GIGANTES.resolveAttackRollBonus(sheet, null));
    }

    @Test
    void filhosDaMontanhaBelongsToConhecimentos() {
        assertEquals(SkillType.CONHECIMENTOS, AnoesRacialAbility.FILHOS_DA_MONTANHA.getSkillType());
    }

    private static SceneContext contextWithTerrain(TerrainType terrainType) {
        return new SceneContext(List.of(), List.of(), Map.of(), terrainType);
    }

    @Test
    void filhosDaMontanhaGrantsAdvantageForNaturezaInMountainOrCaveTerrain() {
        Optional<Integer> mountainBonus = AnoesRacialAbility.FILHOS_DA_MONTANHA
                .resolveConditionalRollBonus(contextWithTerrain(TerrainType.MOUNTAIN), ConhecimentosSpecialization.NATUREZA);
        Optional<Integer> caveBonus = AnoesRacialAbility.FILHOS_DA_MONTANHA
                .resolveConditionalRollBonus(contextWithTerrain(TerrainType.CAVE), ConhecimentosSpecialization.NATUREZA);

        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), mountainBonus);
        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), caveBonus);
    }

    @Test
    void filhosDaMontanhaGrantsNoBonusOutsideMountainOrCaveTerrain() {
        Optional<Integer> bonus = AnoesRacialAbility.FILHOS_DA_MONTANHA
                .resolveConditionalRollBonus(contextWithTerrain(TerrainType.URBAN), ConhecimentosSpecialization.NATUREZA);

        assertEquals(Optional.empty(), bonus);
    }

    @Test
    void filhosDaMontanhaGrantsNoBonusWithoutASceneContext() {
        assertEquals(Optional.empty(),
                AnoesRacialAbility.FILHOS_DA_MONTANHA.resolveConditionalRollBonus(null, ConhecimentosSpecialization.NATUREZA));
    }

    @Test
    void filhosDaMontanhaGrantsNoBonusWhenTheRollDidNotRequestNatureza() {
        SceneContext mountain = contextWithTerrain(TerrainType.MOUNTAIN);

        assertEquals(Optional.empty(), AnoesRacialAbility.FILHOS_DA_MONTANHA.resolveConditionalRollBonus(mountain, null));
        assertEquals(Optional.empty(), AnoesRacialAbility.FILHOS_DA_MONTANHA
                .resolveConditionalRollBonus(mountain, ConhecimentosSpecialization.GEO_HISTORIA));
    }

    @Test
    void onlyFilhosDaMontanhaEverResolvesAConditionalRollBonus() {
        SceneContext mountain = contextWithTerrain(TerrainType.MOUNTAIN);

        for (AnoesRacialAbility ability : AnoesRacialAbility.values()) {
            if (ability != AnoesRacialAbility.FILHOS_DA_MONTANHA) {
                assertEquals(Optional.empty(),
                        ability.resolveConditionalRollBonus(mountain, ConhecimentosSpecialization.NATUREZA));
            }
        }
    }
}
