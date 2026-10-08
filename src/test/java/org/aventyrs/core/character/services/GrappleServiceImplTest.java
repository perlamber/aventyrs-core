package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Immobilization;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agarrar and its escapes (core 0.1.5): both directions against a foe, the captor's hand budget and
 * size cap, every way a hold ends, and the Talentos that favour a grapple.
 */
class GrappleServiceImplTest {

    private static final SkillRoll HIGH = new SkillRoll(List.of(6, 6, 5));
    private static final SkillRoll LOW = new SkillRoll(List.of(1, 2, 2));

    private final GrappleService service = new GrappleServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet hero(final Feat... feats) {
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        CharacterSkill defence = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .skill(SkillType.ESQUIVA_E_APARAR, defence)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static MonsterSheet brute() {
        return GenericMonster.BRUTAMONTES.spawn(new Player());
    }

    private static void assertRefused(final String key, final Runnable action) {
        assertEquals(key, assertThrows(IllegalOperationException.class, action::run).getMessage());
    }

    // ---------- Agarrar ----------

    @Test
    void aSuccessfulGrabLeavesTheFoeAgarradoByTheCaptorAndDealsNothing() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();

        GrappleResult result = service.grab(hero, brute, null, HIGH);

        assertTrue(result.succeeded());
        assertEquals(GrappleService.GRAB_COST, result.actionCost());
        assertTrue(brute.isHeldAgarradoBy(hero));
        assertTrue(brute.isMovementPrevented(null));
        assertTrue(brute.hasCondition(ConditionType.DESPREVENIDO, null));
        assertEquals(List.of(brute), hero.getGrappledTargets());
        assertEquals(0, brute.getDamageTaken());
    }

    @Test
    void aFailedGrabChangesNothing() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();

        assertFalse(service.grab(hero, brute, null, LOW).succeeded());

        assertFalse(brute.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(hero.getGrappledTargets().isEmpty());
    }

    @Test
    void eachHoldOccupiesAHandSoAThirdGrabIsRefused() {
        CharacterSheet hero = hero();
        service.grab(hero, brute(), null, HIGH);
        service.grab(hero, brute(), null, HIGH);

        assertRefused(TranslatableMessages.GRAPPLE_REQUIRES_FREE_HAND, () -> service.grab(hero, brute(), null, HIGH));
    }

    @Test
    void aTargetMoreThanTwoCategoriasLargerCannotBeGrabbed() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        brute.grantTemporaryBonus(ModifierType.SIZE_CATEGORY, 4, 1);

        assertFalse(service.canGrab(hero, brute, null));
        assertRefused(TranslatableMessages.GRAPPLE_TARGET_TOO_LARGE, () -> service.grab(hero, brute, null, HIGH));
    }

    @Test
    void aProneCaptorCannotGrab() {
        CharacterSheet hero = hero();
        hero.applyCondition(new Condition(ConditionType.CAIDO, null));

        assertRefused(TranslatableMessages.GRAPPLE_CAPTOR_INCAPACITATED, () -> service.grab(hero, brute(), null, HIGH));
    }

    /** Submissão: "Vantagem em suas rolagens de Ataque Corpo-a-Corpo para Agarrar". */
    @Test
    void submissaoFavoursTheGrabRoll() {
        int plain = service.grab(hero(), brute(), null, LOW).total();

        int favoured = service.grab(hero(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO), brute(), null, LOW).total();

        assertEquals(plain + Skill.ADVANTAGE_BONUS, favoured);
    }

    // ---------- a foe grabbing a player ----------

    @Test
    void aFailedDefenceLeavesThePlayerAgarradoByTheFoe() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();

        GrappleResult result = service.defendGrab(hero, brute, null, LOW);

        assertFalse(result.succeeded());
        assertTrue(hero.isHeldAgarradoBy(brute));
        assertEquals(List.of(hero), brute.getGrappledTargets());
    }

    @Test
    void aSuccessfulDefenceKeepsThePlayerFree() {
        CharacterSheet hero = hero();

        assertTrue(service.defendGrab(hero, brute(), null, HIGH).succeeded());

        assertFalse(hero.hasCondition(ConditionType.AGARRADO, null));
    }

    // ---------- escaping, and holding on ----------

    @Test
    void escapingAFoesHoldLeavesThePlayerPronto() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.defendGrab(hero, brute, null, LOW);

        GrappleResult result = service.escape(hero, null, SkillType.ATAQUE_CORPO_A_CORPO, HIGH);

        assertTrue(result.succeeded());
        assertEquals(GrappleService.ESCAPE_COST, result.actionCost());
        assertFalse(hero.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(brute.getGrappledTargets().isEmpty());
    }

    @Test
    void aFailedEscapeLeavesTheHoldInPlace() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.defendGrab(hero, brute, null, LOW);

        assertFalse(service.escape(hero, null, SkillType.ATAQUE_CORPO_A_CORPO, LOW).succeeded());

        assertTrue(hero.isHeldAgarradoBy(brute));
    }

    @Test
    void escapingWhenNotHeldIsRefused() {
        assertRefused(TranslatableMessages.NOT_HELD,
                () -> service.escape(hero(), null, SkillType.ATAQUE_CORPO_A_CORPO, HIGH));
    }

    /** Two player characters have no opposed roll to settle a hold between them. */
    @Test
    void anEscapeFromAPlayersHoldHasNoResolutionYet() {
        CharacterSheet held = hero();
        held.applyCondition(new Condition(ConditionType.AGARRADO, null, hero()));

        assertRefused(TranslatableMessages.OPPOSED_ROLL_UNSUPPORTED,
                () -> service.escape(held, null, SkillType.ATAQUE_CORPO_A_CORPO, HIGH));
    }

    @Test
    void aStrugglingFoeGoesFreeWhenItsCaptorFailsToHold() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.grab(hero, brute, null, HIGH);

        assertTrue(service.holdAgainst(hero, brute, null, HIGH).succeeded());
        assertTrue(brute.isHeldAgarradoBy(hero));

        assertFalse(service.holdAgainst(hero, brute, null, LOW).succeeded());
        assertFalse(brute.hasCondition(ConditionType.AGARRADO, null));
    }

    @Test
    void anImobilizadoHeldByAMaterialEscapesAgainstItsPresetGd() {
        CharacterSheet hero = hero();
        hero.applyCondition(Immobilization.byMaterial(10, null));

        GrappleResult result = service.escapeImmobilization(hero, null, HIGH);

        assertTrue(result.succeeded());
        assertEquals(10, result.required());
        assertFalse(hero.hasCondition(ConditionType.IMOBILIZADO, null));
    }

    // ---------- how a hold ends ----------

    @Test
    void movingTheCaptorReleasesEveryone() {
        CharacterSheet hero = hero();
        MonsterSheet first = brute();
        MonsterSheet second = brute();
        service.grab(hero, first, null, HIGH);
        service.grab(hero, second, null, HIGH);

        assertEquals(2, service.releaseAllHeldBy(hero).size());

        assertFalse(first.hasCondition(ConditionType.AGARRADO, null));
        assertFalse(second.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(hero.getGrappledTargets().isEmpty());
    }

    @Test
    void theCaptorMayLetGo() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.grab(hero, brute, null, HIGH);

        assertTrue(service.release(hero, brute));

        assertFalse(brute.hasCondition(ConditionType.AGARRADO, null));
    }

    /** "Caído, Imobilizado, Desacordado" — and the hold does not come back when the captor stands. */
    @Test
    void aCaptorFallingCaidoEndsTheHoldForGood() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.grab(hero, brute, null, HIGH);

        hero.applyCondition(new Condition(ConditionType.CAIDO, null));
        assertFalse(brute.hasCondition(ConditionType.AGARRADO, null));

        hero.removeCondition(ConditionType.CAIDO);
        assertFalse(brute.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(hero.getGrappledTargets().isEmpty());
    }

    /** Two combatants holding each other must not chase each other's Condições forever. */
    @Test
    void twoCombatantsMayHoldEachOther() {
        CharacterSheet hero = hero();
        MonsterSheet brute = brute();
        service.grab(hero, brute, null, HIGH);
        service.defendGrab(hero, brute, null, LOW);

        assertTrue(hero.isHeldAgarradoBy(brute));
        assertTrue(brute.isHeldAgarradoBy(hero));
        assertTrue(hero.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(brute.hasCondition(ConditionType.AGARRADO, null));
    }

    // ---------- a client holding only an identity copy of the foe ----------

    /** The foe's authored Defesa comes from the bestiary; its own Condições still count on top. */
    @Test
    void aGrabAgainstAGivenDefesaAddsTheFoesOwnCondicoes() {
        CharacterSheet hero = hero();
        CharacterSheet foeCopy = hero();

        int plain = service.grab(hero, foeCopy, 15, null, LOW).required();
        CharacterSheet proneCopy = hero();
        proneCopy.applyCondition(new Condition(ConditionType.CAIDO, null));

        assertEquals(15, plain);
        // Desprevenido -4 and Fraqueza -2.
        assertEquals(15 - 4 - 2, service.grab(hero(), proneCopy, 15, null, LOW).required());
    }

    @Test
    void escapingAGivenGdFreesTheHeldCopy() {
        CharacterSheet hero = hero();
        CharacterSheet foeCopy = hero();
        hero.applyCondition(new Condition(ConditionType.AGARRADO, null, foeCopy));
        foeCopy.startGrappling(hero);

        assertTrue(service.escape(hero, null, SkillType.ATAQUE_CORPO_A_CORPO, HIGH, 10).succeeded());

        assertFalse(hero.hasCondition(ConditionType.AGARRADO, null));
        assertTrue(foeCopy.getGrappledTargets().isEmpty());
    }
}
