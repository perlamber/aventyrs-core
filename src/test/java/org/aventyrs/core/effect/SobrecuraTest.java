package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SobrecuraTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void recoversTheRolledDiePlusHalfTheCastersFocus() {
        Character caster = newSheet().getCharacter();
        int focus = caster.getEffectiveAttributeTotal(AttributeDomain.FOCUS);

        assertEquals(4 + focus / 2, new Sobrecura(caster, 4).getRecovery());
    }

    @Test
    void theFocusHalfIsFlooredLikeEveryHalfAttributeTerm() {
        Character caster = newSheet().getCharacter();
        int focus = caster.getEffectiveAttributeTotal(AttributeDomain.FOCUS);

        // Integer division, never rounded up — the same floor the melee half-Força term takes.
        assertEquals(focus / 2, new Sobrecura(caster, 0).getRecovery());
    }

    @Test
    void appliesItsRecoveryToTheTargetAndReportsWhatLanded() {
        CharacterSheet target = newSheet();
        Character caster = newSheet().getCharacter();
        Sobrecura sobrecura = new Sobrecura(caster, 3);
        target.applyDamage(50);

        InteractionResult result = target.receiveInteraction(sobrecura);

        assertEquals(50 - sobrecura.getRecovery(), target.getDamageTaken());
        assertEquals(sobrecura.getRecovery(), result.getResourceGainValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceGainType());
    }

    @Test
    void reportsWhatWasRecoveredRatherThanWhatWasOffered() {
        CharacterSheet target = newSheet();
        Character caster = newSheet().getCharacter();
        target.applyDamage(1);

        InteractionResult result = target.receiveInteraction(new Sobrecura(caster, 6));

        assertEquals(0, target.getDamageTaken());
        assertEquals(1, result.getResourceGainValue());
    }

    @Test
    void descriptionMatchesTheRulesText() {
        Character caster = newSheet().getCharacter();

        assertEquals("Sobrecura: O alvo desta magia adicionalmente recupera +1d6+Metade do Foco PV.",
                new Sobrecura(caster, 1).getDescription());
    }
}
