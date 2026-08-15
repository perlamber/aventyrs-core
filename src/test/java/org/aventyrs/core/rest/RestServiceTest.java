package org.aventyrs.core.rest;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.PendingEgoRecovery;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestServiceTest {

    private final RestService restService = new RestServiceImpl();

    private Character exampleCharacter() {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(4).build())
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(1).build())
                        .build())
                .build();
    }

    @Test
    void matchesTheDescribedExampleForALongRest() {
        Character character = exampleCharacter();
        assertEquals(6, restService.getRecoveredHitPoints(character, RestType.LONGO));
        assertEquals(8, restService.getRecoveredDeterminationPoints(character, RestType.LONGO));
        assertEquals(2, restService.getRecoveredMagicPoints(character, RestType.LONGO));
    }

    @Test
    void shortRestRecoversExactlyTheAttributeValue() {
        Character character = exampleCharacter();
        assertEquals(3, restService.getRecoveredHitPoints(character, RestType.CURTO));
    }

    @Test
    void totalRestRecoversTripleTheAttributeValue() {
        Character character = exampleCharacter();
        assertEquals(9, restService.getRecoveredHitPoints(character, RestType.TOTAL));
    }

    @Test
    void minimumRestRoundsDownAnOddAttributeHalved() {
        Character character = exampleCharacter();
        assertEquals(1, restService.getRecoveredHitPoints(character, RestType.MINIMO));
        assertEquals(2, restService.getRecoveredDeterminationPoints(character, RestType.MINIMO));
    }

    @Test
    void applyRestRecoversAllThreePoolsAtOnce() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(6);
        sheet.spendMagicPoints(2);
        sheet.spendDeterminationPoints(8);

        restService.applyRest(character, sheet, RestType.LONGO);

        assertEquals(0, sheet.getDamageTaken());
        assertEquals(0, sheet.getManaSpent());
        assertEquals(0, sheet.getDeterminationSpent());
    }

    @Test
    void applyRestNeverRecoversMoreThanWasSpent() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(2);

        restService.applyRest(character, sheet, RestType.LONGO);

        assertEquals(0, sheet.getDamageTaken());
    }

    @Test
    void applyRestResolvesAPendingEgoRecoveryOfSufficientTier() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        restService.applyRest(character, sheet, RestType.LONGO);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void applyRestDoesNotResolveAPendingEgoRecoveryBelowItsRequiredTier() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        restService.applyRest(character, sheet, RestType.CURTO);

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }
}
