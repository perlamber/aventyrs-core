package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.BeforeEach;
import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrimorTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void rejectsAnyCriticalResultThatIsNotAnAcertoCritico() {
        assertThrows(IllegalOperationException.class, () -> new Primor(CriticalResult.NONE, EgoDomain.SORTE));
        assertThrows(IllegalOperationException.class, () -> new Primor(CriticalResult.FALHA_CRITICA_MENOR, EgoDomain.SORTE));
        assertThrows(IllegalOperationException.class, () -> new Primor(CriticalResult.FALHA_CRITICA_MAIOR, EgoDomain.SORTE));
    }

    @Test
    void rejectsAnyEgoDomainThatIsNotSorteOrAutocontrole() {
        assertThrows(IllegalOperationException.class,
                () -> new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.RECURSOS));
        assertThrows(IllegalOperationException.class,
                () -> new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.INICIATIVA));
    }

    @Test
    void acertoCriticoMenorSpendsOneTemporaryPointFromTheChosenDomain() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.SORTE).applyTo(sheet);

        assertEquals(1, result.getEgoLossValue());
        assertEquals(EgoDomain.SORTE, result.getEgoLossDomain());
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
    }

    @Test
    void acertoCriticoMaiorSpendsTwoTemporaryPointsFromTheChosenDomain() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.AUTOCONTROLE).applyTo(sheet);

        assertEquals(2, result.getEgoLossValue());
        assertEquals(EgoDomain.AUTOCONTROLE, result.getEgoLossDomain());
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void menorLossIsRecoveredByAnyRestTier() {
        CharacterSheet sheet = newSheet();
        new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.SORTE).applyTo(sheet);

        sheet.applyPendingEgoRecoveries(RestType.MINIMO);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void maiorLossIsNotRecoveredByAShortOrMinimumRest() {
        CharacterSheet sheet = newSheet();
        new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE).applyTo(sheet);

        sheet.applyPendingEgoRecoveries(RestType.MINIMO);
        sheet.applyPendingEgoRecoveries(RestType.CURTO);

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void maiorLossIsRecoveredByALongRest() {
        CharacterSheet sheet = newSheet();
        new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE).applyTo(sheet);

        sheet.applyPendingEgoRecoveries(RestType.LONGO);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /**
     * The refund is registered for what was <em>actually</em> spent, not what Primor asked for —
     * otherwise a Rest would hand back a point the target had spent itself.
     */
    @Test
    void aTargetWithFewerPointsThanPrimorDrainsIsNotRefundedMoreThanItLost() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);

        InteractionResult result = new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE).applyTo(sheet);
        assertEquals(1, result.getEgoLossValue());

        sheet.applyPendingEgoRecoveries(RestType.LONGO);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = sheet.receiveInteraction(new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.SORTE));

        assertEquals(1, result.getEgoLossValue());
    }

    @Test
    void bothAcertoCriticoTiersHaveANonBlankDescription() {
        assertFalse(new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE).getDescription().isBlank());
        assertFalse(new Primor(CriticalResult.ACERTO_CRITICO_MENOR, EgoDomain.SORTE).getDescription().isBlank());
    }
}
