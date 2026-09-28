package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.OlharDeLacerto;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.OLHAR_DE_LACERTO_NOT_HELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase I: the Górgona's Olhar de Lacerto, Cabelo Serpentino and Marca da Maldição's reach. */
class OlharDeLacertoServiceTest {

    private final OlharDeLacertoService service = new OlharDeLacertoServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetOf(final Race race, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void theGazeCostsOnePdAndReachesMuitoCurta() {
        CharacterSheet gorgona = sheetOf(new Gorgona());
        int spentBefore = gorgona.getDeterminationSpent();

        OlharDeLacertoService.Gaze gaze = service.declare(gorgona, sheetOf(new Human()));

        assertEquals(spentBefore + 1, gorgona.getDeterminationSpent());
        assertEquals(2, gaze.actionPoints());
        assertEquals(Range.DISTANCIA_MUITO_CURTA, gaze.range());
        assertEquals(1, gaze.damageDice());
        assertEquals(OlharDeLacerto.DAMAGE, gaze.damage());
        assertFalse(gaze.petrifying());
    }

    @Test
    void onlyAGorgonaHasTheGaze() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.declare(sheetOf(new Human()), sheetOf(new Human())));
        assertEquals(OLHAR_DE_LACERTO_NOT_HELD, refused.getMessage());
    }

    @Test
    void marcaDaMaldicaoWidensItToCurta() {
        OlharDeLacertoService.Gaze gaze = service.declare(sheetOf(new Gorgona(), GorgonaFeat.MARCA_DA_MALDICAO),
                sheetOf(new Human()));

        assertEquals(Range.DISTANCIA_CURTA, gaze.range());
    }

    @Test
    void aHitCostsTheTargetTwoPaForTwoRodadasWithoutStacking() {
        CharacterSheet gorgona = sheetOf(new Gorgona());
        CharacterSheet target = sheetOf(new Human());
        OlharDeLacertoService.Gaze gaze = service.declare(gorgona, target);

        assertFalse(service.applyHit(gorgona, target, gaze));
        service.applyHit(gorgona, target, gaze);

        assertEquals(-2, target.getTemporaryBonus(ModifierType.ACTION_POINTS));
    }

    @Test
    void inFormaMonstruosaItPetrifiesAndEachAttemptIsHarder() {
        CharacterSheet gorgona = sheetOf(new Gorgona());
        gorgona.enterForm(FormType.MONSTRUOSA);
        CharacterSheet target = sheetOf(new Human());

        OlharDeLacertoService.Gaze first = service.declare(gorgona, target);
        assertTrue(first.petrifying());
        assertEquals(2, first.damageDice());
        assertEquals(0, first.petrificationDifficultyIncrease());
        assertEquals(1, service.declare(gorgona, target).petrificationDifficultyIncrease());

        assertTrue(service.applyHit(gorgona, target, first));
        assertTrue(target.isMovementPrevented(null));
        assertFalse(ConditionType.PETRIFICADO.isMaleficio());
    }

    @Test
    void aFallenTargetIsPetrifiedForGoodAndAnImmuneOneNotAtAll() {
        CharacterSheet gorgona = sheetOf(new Gorgona());
        gorgona.enterForm(FormType.MONSTRUOSA);
        CharacterSheet target = sheetOf(new Human());
        target.applyDamage(hitPointsService.getMaxHitPoints(target.getCharacter(), target));

        service.applyHit(gorgona, target, service.declare(gorgona, target));
        for (int turn = 0; turn < 5; turn++) {
            target.finishTurn();
        }
        assertTrue(target.getActiveConditions(null).contains(ConditionType.PETRIFICADO));

        CharacterSheet otherGorgona = sheetOf(new Gorgona());
        assertFalse(service.applyHit(gorgona, otherGorgona, service.declare(gorgona, otherGorgona)));
    }

    @Test
    void cabeloSerpentinoTradesPersuasaoForTheGaze() {
        Character character = sheetOf(new Gorgona()).getCharacter();

        assertEquals(Skill.DISADVANTAGE_MALUS, GorgonaFeat.CABELO_SERPENTINO.resolveSkillRollBonus(
                SkillType.PERSUASAO, null, null, character, null));
        assertEquals(Skill.ADVANTAGE_BONUS, GorgonaFeat.CABELO_SERPENTINO.resolveSkillRollBonus(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, character, OlharDeLacerto.INSTANCE));
        assertEquals(0, GorgonaFeat.CABELO_SERPENTINO.resolveSkillRollBonus(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, character, null));
        assertEquals(Skill.ADVANTAGE_BONUS, GorgonaFeat.CABELO_SERPENTINO.resolveDamageBonus(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, character, OlharDeLacerto.INSTANCE)
                .orElseThrow().getValue());
    }
}
