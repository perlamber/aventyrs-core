package org.aventyrs.core.rest;

import org.aventyrs.core.ability.FocusAbility;
import org.aventyrs.core.ability.InstinctAbility;
import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.PendingEgoRecovery;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestServiceTest {

    private final RestService restService = new RestServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

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

    /**
     * A Descanso has to move the reported {@link CharacterStatus} tier back <em>up</em>. Nothing
     * in 0.0.17 could do that: {@code RestServiceImpl#applyRest} heals through {@link
     * CharacterSheet#heal}, which holds no {@code DamageService}, so the stored tier was left
     * exactly where the last hit put it — a character could be restored to near-full PV and
     * still read as {@code LOW_LIFE} indefinitely.
     *
     * <p>Vigor 3 gives 22 max PV and a Descanso Longo restoring 6. At 15 damage the character
     * sits on 7 PV, just under the one-third threshold; the rest carries them to 13, comfortably
     * inside {@code MEDIUM_LIFE}.
     */
    @Test
    void applyRestMovesTheDerivedStatusBackUpATier() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(15);
        assertEquals(CharacterStatus.LOW_LIFE, hitPointsService.getStatus(sheet));

        restService.applyRest(character, sheet, RestType.LONGO);

        assertEquals(9, sheet.getDamageTaken());
        assertEquals(CharacterStatus.MEDIUM_LIFE, hitPointsService.getStatus(sheet));
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
    void canalizadorDeManaGrantsTwoExtraMagicPointsOnLongoOrBetterRests() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(FocusAbility.CANALIZADOR_DE_MANA)
                .build();

        assertEquals(4, restService.getRecoveredMagicPoints(character, RestType.LONGO));
        assertEquals(5, restService.getRecoveredMagicPoints(character, RestType.TOTAL));
    }

    @Test
    void canalizadorDeManaGrantsNoBonusBelowLongoRests() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(FocusAbility.CANALIZADOR_DE_MANA)
                .build();

        assertEquals(1, restService.getRecoveredMagicPoints(character, RestType.CURTO));
        assertEquals(0, restService.getRecoveredMagicPoints(character, RestType.MINIMO));
    }

    @Test
    void metabolismoRapidoGrantsThreeExtraHitPointsOnLongoOrBetterRests() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(VigorAbility.METABOLISMO_RAPIDO)
                .build();

        assertEquals(9, restService.getRecoveredHitPoints(character, RestType.LONGO));
        assertEquals(12, restService.getRecoveredHitPoints(character, RestType.TOTAL));
    }

    @Test
    void metabolismoRapidoGrantsNoBonusBelowLongoRests() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(VigorAbility.METABOLISMO_RAPIDO)
                .build();

        assertEquals(3, restService.getRecoveredHitPoints(character, RestType.CURTO));
        assertEquals(1, restService.getRecoveredHitPoints(character, RestType.MINIMO));
    }

    @Test
    void supermotivadoGrantsOneExtraDeterminationPointOnAnyRest() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(InstinctAbility.SUPERMOTIVADO)
                .build();

        assertEquals(3, restService.getRecoveredDeterminationPoints(character, RestType.MINIMO));
        assertEquals(5, restService.getRecoveredDeterminationPoints(character, RestType.CURTO));
    }

    @Test
    void supermotivadoGrantsThreeExtraDeterminationPointsOnLongoOrBetterRests() {
        Character character = exampleCharacter().toBuilder()
                .attributeAbility(InstinctAbility.SUPERMOTIVADO)
                .build();

        assertEquals(11, restService.getRecoveredDeterminationPoints(character, RestType.LONGO));
        assertEquals(15, restService.getRecoveredDeterminationPoints(character, RestType.TOTAL));
    }

    // Both must spend first: a recovery restores previously-spent points, so against a full pool
    // it is a no-op and either assertion would pass for the wrong reason.

    @Test
    void applyRestResolvesAPendingEgoRecoveryOfSufficientTier() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int ceiling = sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE);
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        restService.applyRest(character, sheet, RestType.LONGO);

        assertEquals(ceiling, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void applyRestDoesNotResolveAPendingEgoRecoveryBelowItsRequiredTier() {
        Character character = exampleCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int ceiling = sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE);
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        restService.applyRest(character, sheet, RestType.CURTO);

        assertEquals(ceiling - 2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }
}
