package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestServiceImpl;
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

    /**
     * A blank character sturdy enough that the damage these tests deal leaves it alive and out of
     * Coma — what is under test is the heal's arithmetic, not the limits on healing the fallen.
     */
    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).lifeMultiplier(200).build();
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

    /**
     * A Corrente is one heal effect with its Magia: for a target in Coma, Revigorar and its Sobrecura
     * together give 1PV. Built from the caster's sheet and its parent Magia, it shares the Magia's key.
     */
    @Test
    void inComaItSharesItsMagiasOnePv() {
        CharacterSheet target = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        CharacterSheet caster = newSheet();
        int max = new HitPointsServiceImpl().getMaxHitPoints(target.getCharacter(), target);
        target.applyDamage(2 * max - 1);

        target.receiveInteraction(new SpellHealingEffect(VidaSpell.REVIGORAR,
                VidaSpell.REVIGORAR.getHealing().orElseThrow(), false, new RestServiceImpl(), caster));
        target.receiveInteraction(new Sobrecura(caster, VidaSpell.REVIGORAR, 6));

        assertEquals(2 * max - 2, target.getDamageTaken());
    }
}
