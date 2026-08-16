package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.ability.ConcentracaoProfundaActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveAbilityServiceImplTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final ActiveAbility ability = new ConcentracaoProfundaActiveAbility();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private Character characterWithFocusBaseHoldingAbility(final int focusBase) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focusBase).build())
                        .build())
                .activeAbility(ability)
                .build();
    }

    @Test
    void activateGrantsTheAbilitysResolvedEffect() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, ability, 0);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void activateSpendsTheAbilitysMagicPointCost() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, ability, 0);

        assertEquals(3, sheet.getManaSpent());
    }

    @Test
    void activateRejectsAnAbilityTheCharacterDoesNotHold() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    @Test
    void activateRejectsWhenNotEnoughActionPointsThisTurn() {
        Character character = characterWithFocusBaseHoldingAbility(5).toBuilder()
                .actionPoints(0)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    @Test
    void activateRejectsWhenNotEnoughCurrentMagicPoints() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int maxMagicPoints = new MagicPointsServiceImpl().getMaxMagicPoints(character);
        sheet.spendMagicPoints(maxMagicPoints - 2);

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    @Test
    void activateLeavesCharacterSheetUntouchedWhenRejected() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));

        assertEquals(0, sheet.getManaSpent());
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }
}
