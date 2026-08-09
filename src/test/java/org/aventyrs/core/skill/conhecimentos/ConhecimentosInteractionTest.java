package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConhecimentosInteractionTest {

    private final ConhecimentosInteraction conhecimentosInteraction = new ConhecimentosInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithGnoseAndSkill(int gnoseBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(gnoseBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.CONHECIMENTOS, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill conhecimentosSkill = CharacterSkillFixture.blank(CharacterSkillFixture.CONHECIMENTOS_1).build();
        conhecimentosSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, conhecimentosSkill);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(3, null);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill conhecimentosSkill = CharacterSkillFixture.blank(CharacterSkillFixture.CONHECIMENTOS_1).build();
        conhecimentosSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, conhecimentosSkill);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(conhecimentosInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    private CharacterSheet anaoSheetTrainedInNatureza() {
        CharacterSkill conhecimentosSkill = CharacterSkillFixture.blank(CharacterSkillFixture.CONHECIMENTOS_1)
                .specializations(List.of(ConhecimentosSpecialization.NATUREZA))
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Anao())
                .skill(SkillType.CONHECIMENTOS, conhecimentosSkill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /**
     * End-to-end proof that {@code AnoesRacialAbility#FILHOS_DA_MONTANHA} — a racial ability,
     * held via {@code character.getRace().getRacialAbilities()}, not acquired — reaches this
     * Interaction's real {@code skillRollBonus} sum via {@code AbstractSkillInteraction
     * #sumConditionalRollBonuses}, not just its own isolated {@code resolveConditionalRollBonus}
     * call (see {@code AnoesRacialAbilityTest} for that unit-level coverage).
     */
    @Test
    void applyToAddsFilhosDaMontanhaAdvantageWhenNaturezaIsRequestedInMountainTerrain() {
        CharacterSheet sheet = anaoSheetTrainedInNatureza();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.MOUNTAIN);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ConhecimentosSpecialization.NATUREZA);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet, sceneContext, skillRoll);

        // Gnose(1, untouched default) + Graduação(0) + FILHOS_DA_MONTANHA's Vantagem(2) = 3.
        assertEquals(3, result.getSkillRollBonus());
    }

    @Test
    void applyToOmitsFilhosDaMontanhaAdvantageOutsideMountainOrCaveTerrain() {
        CharacterSheet sheet = anaoSheetTrainedInNatureza();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.URBAN);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ConhecimentosSpecialization.NATUREZA);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet, sceneContext, skillRoll);

        assertEquals(1, result.getSkillRollBonus());
    }

    @Test
    void applyToOmitsFilhosDaMontanhaAdvantageWhenNaturezaWasNotRequested() {
        CharacterSheet sheet = anaoSheetTrainedInNatureza();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.MOUNTAIN);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet, sceneContext, null);

        assertEquals(1, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsTheAdvantageBonusWhenCharacterHasOProfessor() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(2).build())
                        .build())
                .skillCompetencyAbility(ConhecimentosCompetencyAbility.O_PROFESSOR)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }
}
