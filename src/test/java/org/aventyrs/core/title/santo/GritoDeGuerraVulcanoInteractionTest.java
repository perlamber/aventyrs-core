package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GritoDeGuerraVulcanoInteractionTest {

    private final GritoDeGuerraVulcanoInteraction interaction = new GritoDeGuerraVulcanoInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private boolean hasBlessing(final List<Blessing> blessings, final ModifierType modifierType, final int value, final int rounds) {
        return blessings.stream().anyMatch(blessing ->
                blessing.getModifierType() == modifierType
                        && blessing.getValue() == value
                        && blessing.getRounds() == rounds
                        && blessing.getScope() == TargetScope.SELF_AND_ALLIES);
    }

    @Test
    void reportsAllThreeBlessings() {
        CharacterSheet actor = newSheet();

        InteractionResult result = interaction.applyTo(actor);

        assertEquals(3, result.getBlessings().size());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
    }

    @Test
    void reportsTheRangedVantagemBlessing() {
        InteractionResult result = interaction.applyTo(newSheet());

        assertTrue(hasBlessing(result.getBlessings(), ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS, 2));
    }

    @Test
    void reportsTheMeleeVantagemBlessing() {
        InteractionResult result = interaction.applyTo(newSheet());

        assertTrue(hasBlessing(result.getBlessings(), ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS, 2));
    }

    @Test
    void reportsTheDefesasBlessingEvenThoughNothingConsumesItYet() {
        InteractionResult result = interaction.applyTo(newSheet());

        assertTrue(hasBlessing(result.getBlessings(), ModifierType.DEFESAS, 2, 2));
    }

    @Test
    void theTwoArgOverloadWithANullSceneContextStillReportsAllThreeBlessings() {
        InteractionResult result = interaction.applyTo(newSheet(), null);

        assertEquals(3, result.getBlessings().size());
    }

    @Test
    void everyBlessingReportsGritoDeGuerraVulcanoAsItsSource() {
        InteractionResult result = interaction.applyTo(newSheet());

        assertTrue(result.getBlessings().stream()
                .allMatch(blessing -> AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.name().equals(blessing.getSource())));
    }
}
