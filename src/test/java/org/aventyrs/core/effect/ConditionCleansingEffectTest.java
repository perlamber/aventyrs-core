package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vida's alternativo branch, rung by rung — one class again, differing only in which {@link
 * ConditionType}s the Magia names.
 */
class ConditionCleansingEffectTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private InteractionResult cast(final CharacterSheet target, final Spell spell) {
        return target.receiveInteraction(
                new ConditionCleansingEffect(spell, spell.getCleansedConditions()));
    }

    @Test
    void toqueCurativoLiftsDiseaseAndPoisonAndNothingElse() {
        CharacterSheet sheet = newSheet();
        sheet.applyCondition(new Condition(ConditionType.DOENTE, 5));
        sheet.applyCondition(new Condition(ConditionType.ENVENENADO, 5));
        sheet.applyCondition(new Condition(ConditionType.AMALDICOADO, 5));

        InteractionResult result = cast(sheet, VidaSpell.TOQUE_CURATIVO);

        assertFalse(sheet.hasCondition(ConditionType.DOENTE, null));
        assertFalse(sheet.hasCondition(ConditionType.ENVENENADO, null));
        assertTrue(sheet.hasCondition(ConditionType.AMALDICOADO, null),
                "a curse is beyond this rung of the branch");
        assertEquals(Set.of(ConditionType.DOENTE, ConditionType.ENVENENADO),
                result.getLiftedConditions());
    }

    @Test
    void removerMaldicaoLiftsOnlyTheCurse() {
        CharacterSheet sheet = newSheet();
        sheet.applyCondition(new Condition(ConditionType.AMALDICOADO, 5));
        sheet.applyCondition(new Condition(ConditionType.DOENTE, 5));

        cast(sheet, VidaSpell.REMOVER_MALDICAO);

        assertFalse(sheet.hasCondition(ConditionType.AMALDICOADO, null));
        assertTrue(sheet.hasCondition(ConditionType.DOENTE, null));
    }

    @Test
    void exorcizarLiftsAllThreeItNamesIncludingPossession() {
        CharacterSheet sheet = newSheet();
        sheet.applyCondition(new Condition(ConditionType.AMALDICOADO, 5));
        sheet.applyCondition(new Condition(ConditionType.DOENTE, 5));
        sheet.applyCondition(new Condition(ConditionType.POSSESSAO, 2));

        InteractionResult result = cast(sheet, VidaSpell.EXORCIZAR);

        assertFalse(sheet.hasCondition(ConditionType.POSSESSAO, null));
        assertEquals(Set.of(ConditionType.AMALDICOADO, ConditionType.DOENTE,
                ConditionType.POSSESSAO), result.getLiftedConditions());
    }

    @Test
    void corpoFechadoLiftsEveryMaleficioButLeavesEscondidoAlone() {
        CharacterSheet sheet = newSheet();
        sheet.applyCondition(new Condition(ConditionType.AMALDICOADO, 5));
        sheet.applyCondition(new Condition(ConditionType.CONFUSO, 5));
        sheet.applyCondition(new Condition(ConditionType.SILENCIO, 5));

        cast(sheet, VidaSpell.CORPO_FECHADO);

        assertTrue(sheet.getActiveConditions(null).isEmpty());
        assertFalse(VidaSpell.CORPO_FECHADO.getCleansedConditions()
                        .contains(ConditionType.ESCONDIDO),
                "Escondido is a Condição its holder chose, not a Malefício");
    }

    @Test
    void aMagiaThatFindsNothingToLiftReportsAnEmptySetRatherThanFailing() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = cast(sheet, VidaSpell.EXORCIZAR);

        assertEquals(Set.of(), result.getLiftedConditions());
    }

    @Test
    void onlyWhatWasActuallyHeldIsReported() {
        CharacterSheet sheet = newSheet();
        sheet.applyCondition(new Condition(ConditionType.DOENTE, 5));

        InteractionResult result = cast(sheet, VidaSpell.EXORCIZAR);

        assertEquals(Set.of(ConditionType.DOENTE), result.getLiftedConditions());
    }

    @Test
    void theDescriptionIsTheMagiasOwnEfeitoLine() {
        ConditionCleansingEffect effect = new ConditionCleansingEffect(VidaSpell.EXORCIZAR,
                VidaSpell.EXORCIZAR.getCleansedConditions());

        assertEquals(VidaSpell.EXORCIZAR.getPrimaryEffectDescription(), effect.getDescription());
    }
}
