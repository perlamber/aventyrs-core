package org.aventyrs.core.effect;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealExecutionTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private Character characterWithVigor(final int vigorBase) {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
    }

    private boolean isPermanentlyDead(final Character character, final CharacterSheet sheet) {
        int maxHitPoints = hitPointsService.getMaxHitPoints(character);
        int unclampedCurrentHitPoints = maxHitPoints - sheet.getDamageTaken();
        return hitPointsService.getStatus(unclampedCurrentHitPoints, maxHitPoints) == CharacterStatus.DEAD;
    }

    @Test
    void rejectsAnyCriticalResultThatIsNotAnAcertoCritico() {
        assertThrows(IllegalOperationException.class, () -> new RealExecution(CriticalResult.NONE));
        assertThrows(IllegalOperationException.class, () -> new RealExecution(CriticalResult.FALHA_CRITICA_MENOR));
        assertThrows(IllegalOperationException.class, () -> new RealExecution(CriticalResult.FALHA_CRITICA_MAIOR));
    }

    @Test
    void acertoCriticoMaiorDestroysUnconditionallyEvenAtFullHealth() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR).applyTo(sheet);

        assertEquals(CharacterStatus.DEAD, result.getResultStatus());
        assertTrue(isPermanentlyDead(character, sheet));
    }

    @Test
    void acertoCriticoMenorDestroysWhenCurrentHitPointsAreExactlyDoubleVigor() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int maxHitPoints = hitPointsService.getMaxHitPoints(character);
        sheet.applyDamage(maxHitPoints - 6);

        InteractionResult result = new RealExecution(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);

        assertEquals(CharacterStatus.DEAD, result.getResultStatus());
        assertTrue(isPermanentlyDead(character, sheet));
    }

    @Test
    void acertoCriticoMenorDoesNotDestroyAboveDoubleVigor() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int maxHitPoints = hitPointsService.getMaxHitPoints(character);
        sheet.applyDamage(maxHitPoints - 7);
        int damageTakenBefore = sheet.getDamageTaken();

        InteractionResult result = new RealExecution(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);

        assertNotEquals(CharacterStatus.DEAD, result.getResultStatus());
        assertFalse(isPermanentlyDead(character, sheet));
        assertEquals(damageTakenBefore, sheet.getDamageTaken());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = sheet.receiveInteraction(new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR));

        assertEquals(CharacterStatus.DEAD, result.getResultStatus());
    }

    @Test
    void descriptionsMatchTheRulesTextPerSeverity() {
        assertEquals("O alvo é imediatamente destruído e não poderá ser ressuscitado.",
                new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR).getDescription());
        assertEquals("Se o alvo deste ataque tiver seus PV reduzidos à uma quantidade igual " +
                        "ou inferior ao dobro do Vigor dele ele será imediatamente " +
                        "destruído e não poderá ser ressuscitado.",
                new RealExecution(CriticalResult.ACERTO_CRITICO_MENOR).getDescription());
    }
}
