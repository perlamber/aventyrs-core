package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ConcentracaoProfundaActiveAbilityTest {

    private final ConcentracaoProfundaActiveAbility ability = new ConcentracaoProfundaActiveAbility();

    @Test
    void costsOnePontoDeAcao() {
        assertEquals(1, ability.getActionPointCost());
    }

    @Test
    void costsThreePontosDeMagia() {
        assertEquals(3, ability.getMagicPointCost());
    }

    @Test
    void lastsTwoRodadas() {
        assertEquals(2, ability.getDurationInRounds());
    }

    @Test
    void descriptionMatchesTheCatalogConstant() {
        assertEquals(FocusAbility.CONCENTRACAO_PROFUNDA.getDescription(), ability.getDescription());
        assertFalse(ability.getDescription().isBlank());
    }

    private Character characterWithFocusBase(final int base) {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(base).build())
                        .build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void resolveEffectGrantsHalfFocusAsASkillRollBonus() {
        Character character = characterWithFocusBase(5);

        TemporaryEffect effect = ability.resolveEffect(character);

        TemporaryBonus bonus = assertInstanceOf(TemporaryBonus.class, effect);
        assertEquals(ModifierType.SKILL_ROLL_BONUS, bonus.getType());
        assertEquals(2, bonus.getValue());
        assertEquals(2, bonus.getRemainingRounds());
    }

    @Test
    void resolveEffectRoundsHalfFocusDown() {
        Character character = characterWithFocusBase(3);

        TemporaryBonus bonus = (TemporaryBonus) ability.resolveEffect(character);

        assertEquals(1, bonus.getValue());
    }
}
