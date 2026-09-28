package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.DevorarInteiro;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.OgricoFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.Goblin;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Indomito;
import org.aventyrs.core.race.Ogro;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.BOCARRA_NOT_HELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase I: Bocarra's Devorar Inteiro and the Talentos Ôgricos. */
class DevourServiceTest {

    private final DevourService service = new DevourServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetOf(final Race race, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .sizeCategory(race.getBaseSizeCategory())
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet ogre(final Feat... feats) {
        return sheetOf(new Ogro(Ogro.Aptidao.values()[0]), feats);
    }

    private static int vigor(final CharacterSheet sheet) {
        return sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR);
    }

    @Test
    void ogrosBiteWithPresasLongasAndOthersHaveNoBocarra() {
        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS), new Ogro(Ogro.Aptidao.values()[0]).getGrantedNaturalWeapons());
        assertEquals(List.of(NaturalWeapon.GARRAS_AFIADAS), new Indomito(Indomito.Tribo.BASTET).getGrantedNaturalWeapons());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.declareBite(sheetOf(new Human())));
        assertEquals(BOCARRA_NOT_HELD, refused.getMessage());
    }

    @Test
    void aBocarraBiteCostsOnePdAndGainsVantagemAndTheCorrente() {
        CharacterSheet ogre = ogre();
        int spentBefore = ogre.getDeterminationSpent();

        DevourService.BocarraBite bite = service.declareBite(ogre);

        assertEquals(spentBefore + 1, ogre.getDeterminationSpent());
        assertEquals(1, bite.extraActionPoints());
        assertEquals(Skill.ADVANTAGE_BONUS, bite.attackRollBonus());
        assertEquals(0, bite.lifeSteal());
        assertInstanceOf(DevorarInteiro.class, bite.devorarInteiro());
    }

    @Test
    void onlySmallerCharactersFitAndTheyFillTheStomachBySize() {
        CharacterSheet ogre = ogre();

        assertEquals(OptionalInt.of(2), service.getVigorOccupied(ogre, sheetOf(new Human())));
        assertEquals(OptionalInt.of(1), service.getVigorOccupied(ogre, sheetOf(new Goblin())));
        assertTrue(service.getVigorOccupied(ogre, ogre()).isEmpty());
    }

    @Test
    void mandibulaDesarticuladaAndPoderosoGlutaoWidenTheLimits() {
        CharacterSheet ogre = ogre(OgricoFeat.MANDIBULA_DESARTICULADA);
        assertEquals(OptionalInt.of(3), service.getVigorOccupied(ogre, ogre()));

        CharacterSheet glutao = ogre(OgricoFeat.MANDIBULA_DESARTICULADA, OgricoFeat.PODEROSO_GLUTAO);
        assertEquals(OptionalInt.of(2), service.getVigorOccupied(glutao, ogre()));
        assertEquals(OptionalInt.of(1), service.getVigorOccupied(glutao, sheetOf(new Goblin())));
    }

    @Test
    void doisEstomagosWidensOnlyTheStomach() {
        CharacterSheet ogre = ogre(OgricoFeat.DOIS_ESTOMAGOS);

        assertEquals(vigor(ogre) + 2, service.getStomachCapacity(ogre.getCharacter()));
        assertEquals(vigor(ogre(OgricoFeat.DOIS_ESTOMAGOS)), vigor(ogre()));
    }

    @Test
    void theCorrenteSwallowsUntilTheStomachIsFull() {
        CharacterSheet ogre = ogre();
        int capacity = service.getStomachCapacity(ogre.getCharacter());
        DevorarInteiro chain = (DevorarInteiro) service.declareBite(ogre).devorarInteiro();

        List<CharacterSheet> goblins = new ArrayList<>();
        for (int i = 0; i <= capacity; i++) {
            CharacterSheet goblin = sheetOf(new Goblin());
            goblins.add(goblin);
            chain.applyTo(goblin);
        }

        assertEquals(capacity, ogre.getDevouredVictims().size());
        assertTrue(goblins.get(0).getDevoured().isPresent());
        assertFalse(goblins.get(capacity).getDevoured().isPresent());
        assertEquals(capacity, service.getVigorInUse(ogre));
    }

    @Test
    void digestionDamagesEveryoneInsideAndBreakingOutFreesThem() {
        CharacterSheet ogre = ogre(OgricoFeat.DOIS_ESTOMAGOS);
        CharacterSheet human = sheetOf(new Human());
        assertTrue(service.swallow(ogre, human));

        List<DevourService.Digestion> digestions = service.digest(ogre);
        assertEquals(1, digestions.size());
        assertEquals(1 + vigor(ogre) / 2, human.getDamageTaken());

        assertFalse(service.recordDamageFromInside(ogre, human, 2 * vigor(ogre) - 1));
        assertTrue(service.recordDamageFromInside(ogre, human, 1));
        assertFalse(human.getDevoured().isPresent());
        assertTrue(ogre.getDevouredVictims().isEmpty());
    }

    @Test
    void devoratrizStealsLifeAndDigestsTheFallenOutright() {
        CharacterSheet ogre = ogre(OgricoFeat.DEVORATRIZ);
        ogre.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        ogre.applyDamage(5);
        CharacterSheet goblin = sheetOf(new Goblin());
        assertTrue(service.swallow(ogre, goblin));
        int maxHitPoints = hitPointsService.getMaxHitPoints(goblin.getCharacter(), goblin);
        goblin.applyDamage(maxHitPoints);

        DevourService.Digestion digestion = service.digest(ogre).get(0);

        assertTrue(digestion.killed());
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(goblin));
        assertEquals(5 - ogre.getDamageTaken(), digestion.hitPointsRecovered());
        assertTrue(digestion.hitPointsRecovered() >= 1);
        assertTrue(ogre.getDevouredVictims().isEmpty());
        assertEquals(1, service.declareBite(ogre).lifeSteal());
    }

    @Test
    void digestionTakesHoursPoderosoGlutaoHalves() {
        CharacterSheet ogre = ogre();
        CharacterSheet human = sheetOf(new Human());
        assertEquals(4, service.getDigestionHours(ogre, human));
        assertEquals(1, service.getDigestionHours(ogre(OgricoFeat.PODEROSO_GLUTAO), human));
        assertEquals(3, service.getRegurgitationActionPoints(3));
        assertEquals(1, service.getRegurgitationActionPoints(0));
    }
}
