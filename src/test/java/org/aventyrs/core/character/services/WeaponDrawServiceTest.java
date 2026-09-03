package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sacar uma arma as an action: what it costs, whether it is allowed, and what it changes.
 *
 * <p>The plain {@code Character#drawWeapon} mutator stays unvalidating and builder-bypassable by
 * design — this service is the entry point that prices and gates it, the same split
 * {@code ActiveAbilityService#activate} draws against {@code Character}'s own mutators.
 */
class WeaponDrawServiceTest {

    private final WeaponDrawService weaponDrawService = new WeaponDrawServiceImpl();
    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon dagger() {
        return AbstractWeapon.builder()
                .name("Adaga").category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static CharacterSheet carrying(final Weapon... weapons) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>(List.of(weapons)))
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    // ---------- cost ----------

    /** No rules text states sacar's cost; 1PA is the inference SAQUE_RAPIDO's existence forces. */
    @Test
    void drawingCostsOneActionPointByDefault() {
        ActionCost cost = weaponDrawService.getDrawCost(carrying(dagger()).getCharacter());

        assertEquals(ActionCost.Kind.ACTION_POINTS, cost.kind());
        assertEquals(1, cost.actionPoints());
        assertEquals(WeaponDrawService.DEFAULT_DRAW_COST, cost);
    }

    @Test
    void saqueRapidoMakesDrawingAnAcaoLivre() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(sheet.getCharacter(), sheet, AssassinoFeat.SAQUE_RAPIDO);

        assertEquals(ActionCost.FREE_ACTION, weaponDrawService.getDrawCost(sheet.getCharacter()));
        assertEquals(ActionCost.FREE_ACTION, weaponDrawService.draw(sheet, dagger));
    }

    /** Asking the price must not draw anything. */
    @Test
    void askingTheCostDrawsNothing() {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);

        weaponDrawService.getDrawCost(sheet.getCharacter());

        assertFalse(sheet.getCharacter().isDrawn(dagger));
    }

    // ---------- permission ----------

    @Test
    void drawingPutsTheWeaponInHandAndMarksTheTurn() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        sheet.startTurn(0);
        assertFalse(sheet.hasDrawnWeaponThisTurn());

        ActionCost cost = weaponDrawService.draw(sheet, dagger);

        assertEquals(WeaponDrawService.DEFAULT_DRAW_COST, cost);
        assertTrue(sheet.getCharacter().isDrawn(dagger));
        assertTrue(sheet.getCharacter().isWieldingAWeapon());
        assertTrue(sheet.hasDrawnWeaponThisTurn(), "SAQUE_RAPIDO's Desvantagem reads this marker");
    }

    @Test
    void aWeaponNotCarriedCannotBeDrawn() {
        CharacterSheet sheet = carrying();
        Weapon notCarried = dagger();

        assertFalse(weaponDrawService.canDraw(sheet, notCarried));
        assertThrows(IllegalOperationException.class, () -> weaponDrawService.draw(sheet, notCarried));
    }

    @Test
    void aWeaponAlreadyInHandCannotBeDrawnAgain() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        weaponDrawService.draw(sheet, dagger);

        assertFalse(weaponDrawService.canDraw(sheet, dagger));
        assertThrows(IllegalOperationException.class, () -> weaponDrawService.draw(sheet, dagger));
    }

    /** Devorado: inside a creature you can reach neither your sheath nor what you dropped. */
    @Test
    void aDevoredCharacterCannotDraw() {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        sheet.applyCondition(new Condition(ConditionType.DEVORADO, null));

        assertFalse(weaponDrawService.canDraw(sheet, dagger));
        assertThrows(IllegalOperationException.class, () -> weaponDrawService.draw(sheet, dagger));
        assertFalse(sheet.getCharacter().isDrawn(dagger), "a refused draw leaves the sheet untouched");
    }

    @Test
    void drawingWorksAgainOnceDevoradoIsLifted() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        sheet.applyCondition(new Condition(ConditionType.DEVORADO, null));

        sheet.removeCondition(ConditionType.DEVORADO);

        assertTrue(weaponDrawService.canDraw(sheet, dagger));
        weaponDrawService.draw(sheet, dagger);
        assertTrue(sheet.getCharacter().isDrawn(dagger));
    }

    /** Being merely Desarmado does not stop you arming yourself again — that is how it ends. */
    @Test
    void beingDesarmadoDoesNotStopDrawing() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        sheet.applyCondition(new Condition(ConditionType.DESARMADO, null));

        assertTrue(weaponDrawService.canDraw(sheet, dagger));
        weaponDrawService.draw(sheet, dagger);
        assertTrue(sheet.getCharacter().isDrawn(dagger));
    }

    // ---------- sheathing ----------

    @Test
    void sheathingFreesTheHandButKeepsTheWeapon() throws IllegalOperationException {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);
        weaponDrawService.draw(sheet, dagger);

        assertTrue(weaponDrawService.sheathe(sheet, dagger));

        assertFalse(sheet.getCharacter().isDrawn(dagger));
        assertTrue(sheet.getCharacter().getEquipment().contains(dagger), "still carried, just put away");
    }

    /** Putting away an empty hand is a no-op, not a failure. */
    @Test
    void sheathingAWeaponThatWasNotDrawnIsANoOp() {
        Weapon dagger = dagger();
        CharacterSheet sheet = carrying(dagger);

        assertFalse(weaponDrawService.sheathe(sheet, dagger));
    }
}
