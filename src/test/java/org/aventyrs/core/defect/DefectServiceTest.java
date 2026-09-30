package org.aventyrs.core.defect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.DefectService;
import org.aventyrs.core.character.services.DefectServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.DEFECT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_DEFECT_SELECTION;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plan Phase 4 — a Defeito imposed during play, and Superar (3/5/7 EXP). */
class DefectServiceTest {

    private final DefectService defects = new DefectServiceImpl();
    private final CharacterCreationService creation = new CharacterCreationServiceImpl();
    private final HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character base() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Human())
                .skills(Map.of(SkillType.ATAQUE_CORPO_A_CORPO, CharacterSkill.builder()
                        .skill(SkillType.ATAQUE_CORPO_A_CORPO.newSkillInstance())
                        .graduation(SkillGraduation.builder().graduationValue(1).build()).build()))
                .build();
    }

    private static CharacterSheet funded(final Character character, final int experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(experience));
        return sheet;
    }

    private static IllegalOperationException refused(final org.junit.jupiter.api.function.Executable action) {
        return assertThrows(IllegalOperationException.class, action);
    }

    @Test
    void superarCostsThreeFiveAndSevenExp() {
        assertEquals(List.of(3, 5, 7), List.of(DefectSeverity.values()).stream()
                .map(severity -> defects.getOvercomeCost(severity).intValue()).toList());
    }

    // ---- Imposed during play ---------------------------------------------------------------------

    @Test
    void aDefeitoImposedDuringPlayTakesEffectWithNoSuperacao() {
        Character character = base();
        int before = hitPoints.getLifeMultiplier(character);
        int slots = creation.getStartingFeatSlots(character).size();

        HeldDefect held = defects.grantDefect(character, Defect.CORPO_FRAGIL, DefectSeverity.MODERADO, List.of());

        assertFalse(held.fromCreation());
        assertNull(held.superacao());
        assertEquals(before - 2, hitPoints.getLifeMultiplier(character));
        assertEquals(slots, creation.getStartingFeatSlots(character).size(), "no Benefício de Superação");
    }

    @Test
    void itsChoicesAreValidatedAsAtCreation() {
        Character character = base();
        IllegalOperationException ex = refused(() ->
                defects.grantDefect(character, Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, List.of()));
        assertEquals(INVALID_DEFECT_SELECTION, ex.getMessage());
        assertTrue(character.getDefects().isEmpty());
    }

    @Test
    void imposingAHeldDefeitoChangesItKeepingItsOriginAndSuperacao() {
        Character character = creation.applyDefectsAndQualities(base(), List.of(HeldDefect.atCreation(
                Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, List.of(Limb.BRACOS),
                SuperacaoBenefit.TREINAMENTO_ADICIONAL, List.of(SkillType.CONHECIMENTOS))), List.of());
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(sheet);

        defects.grantDefect(character, Defect.DEFICIENCIA_FISICA, DefectSeverity.MODERADO, List.of(Limb.BRACOS));

        assertEquals(1, character.getDefects().size());
        HeldDefect held = character.getDefects().get(0);
        assertEquals(DefectSeverity.MODERADO, held.severity());
        assertTrue(held.fromCreation());
        assertEquals(SuperacaoBenefit.TREINAMENTO_ADICIONAL, held.superacao());
        assertThrows(IllegalOperationException.class, () -> SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(sheet),
                "Membro Ausente now");
    }

    // ---- Superar ---------------------------------------------------------------------------------

    @Test
    void superarSpendsItsCostAndEndsTheEffectsButKeepsTheSuperacao() {
        Character character = creation.applyDefectsAndQualities(base(), List.of(HeldDefect.atCreation(
                Defect.CORPO_FRAGIL, DefectSeverity.MODERADO, List.of(), SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        int weakened = hitPoints.getLifeMultiplier(character);
        int slots = creation.getStartingFeatSlots(character).size();
        CharacterSheet sheet = funded(character, 10);

        HeldDefect overcome = defects.overcome(character, sheet, Defect.CORPO_FRAGIL);

        assertTrue(overcome.overcome());
        assertEquals(BigDecimal.valueOf(5), BigDecimal.TEN.subtract(sheet.getUnUsedExperience()).stripTrailingZeros());
        assertEquals(weakened + 2, hitPoints.getLifeMultiplier(character), "its effect is gone");
        assertEquals(List.of(overcome), character.getDefects(), "kept on record");
        assertEquals(slots, creation.getStartingFeatSlots(character).size(), "its Talento Geral slot stays");
    }

    @Test
    void superarRefusesADefeitoNotInForceAndAShortWalletChangingNothing() {
        Character character = base();
        defects.grantDefect(character, Defect.CORPO_FRAGIL, DefectSeverity.GRAVE, List.of());
        CharacterSheet poor = funded(character, 6);

        assertEquals(DEFECT_NOT_HELD, refused(() -> defects.overcome(character, poor, Defect.FOBIA)).getMessage());
        assertEquals(NOT_ENOUGH_EXPERIENCE, refused(() -> defects.overcome(character, poor, Defect.CORPO_FRAGIL)).getMessage());

        assertEquals(0, BigDecimal.valueOf(6).compareTo(poor.getUnUsedExperience()));
        assertTrue(character.getActiveDefect(Defect.CORPO_FRAGIL).isPresent());
        assertEquals(1, hitPoints.getLifeMultiplier(character));
    }

    @Test
    void anOvercomeDefeitoCanBeImposedAgainAsANewOne() {
        Character character = base();
        defects.grantDefect(character, Defect.CORPO_FRAGIL, DefectSeverity.LEVE, List.of());
        defects.overcome(character, funded(character, 3), Defect.CORPO_FRAGIL);
        int restored = hitPoints.getLifeMultiplier(character);

        defects.grantDefect(character, Defect.CORPO_FRAGIL, DefectSeverity.LEVE, List.of());

        assertEquals(2, character.getDefects().size());
        assertEquals(restored - 1, hitPoints.getLifeMultiplier(character));
    }
}
