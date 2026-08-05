package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
