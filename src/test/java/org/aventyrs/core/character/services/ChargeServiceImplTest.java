package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Investida as an action: what it costs, how far it carries, who may declare one, and what a
 * miss costs the charger.
 *
 * <p>No document under {@code docs/rules/} defines the manoeuvre, so the constants under test are
 * labelled on {@link ChargeService} with their provenance — 3PA authored, the ×2 allowance read off
 * {@code DexterityAbility#IMPLACAVEL}'s "o triplo … ao invés do dobro".
 */
class ChargeServiceImplTest {

    private final ChargeService chargeService = new ChargeServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon sword() {
        return AbstractWeapon.builder()
                .name("Espada Longa").category(ItemCategory.HEAVY_BLADE)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    private static Weapon bow() {
        return AbstractWeapon.builder()
                .name("Arco Curto").category(ItemCategory.BOW)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_MEDIA)
                .build();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>());
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    /** Carried and in hand — the state an Investida requires. */
    private static CharacterSheet wielding(final Weapon weapon) {
        CharacterSheet sheet = sheetOf(character().build());
        sheet.getCharacter().equip(weapon);
        sheet.getCharacter().drawWeapon(weapon);
        return sheet;
    }

    // ---------- cost ----------

    @Test
    void anInvestidaCostsThreeActionPointsByDefault() {
        ActionCost cost = chargeService.getActionPointCost(character().build());

        assertEquals(ActionCost.Kind.ACTION_POINTS, cost.kind());
        assertEquals(ChargeService.BASE_ACTION_POINT_COST, cost.actionPoints());
        assertEquals(3, cost.actionPoints());
    }

    /**
     * {@code MobilidadeFeat#INVESTIDA_AQUATICA} — "o Tempo de Ação de investidas sempre reduzidos
     * em -1PA". Granted through {@link FeatService#grantFeat} so its own Pré-requisito (4
     * Graduações em Atletismo) is actually met and its XP actually paid: the reduction is being
     * read off a Talento the character legally holds, not off a hook called directly.
     */
    @Test
    void investidaAquaticaShortensTheManoeuvreByOneActionPoint() {
        Character swimmer = character()
                .skill(SkillType.ATLETISMO, CharacterSkill.builder()
                        .skill(new Atletismo())
                        .graduation(SkillGraduation.builder().graduationValue(4).build())
                        .build())
                .build();
        CharacterSheet sheet = sheetOf(swimmer);
        sheet.accumulateExperience(BigDecimal.valueOf(100));

        featService.grantFeat(swimmer, sheet, MobilidadeFeat.INVESTIDA_AQUATICA);

        assertEquals(2, chargeService.getActionPointCost(swimmer).actionPoints());
    }

    /**
     * {@code ActionCost} refuses a Pontos-de-Ação cost of 0, so the reduction floors at 1 rather
     * than letting a stack of reductions construct an impossible cost.
     */
    @Test
    void theCostNeverFallsBelowOneActionPoint() {
        Character character = character().build();
        character.grantFeat(new org.aventyrs.core.feat.AbstractFeat(
                FeatCategory.MOBILIDADE, "Reduz muito", null) {
            @Override
            public int resolveChargeActionPointReduction(final Character holder) {
                return 99;
            }
        });

        assertEquals(ChargeService.MINIMUM_ACTION_POINT_COST,
                chargeService.getActionPointCost(character).actionPoints());
    }

    // ---------- movement allowance ----------

    /**
     * "O dobro do seu Movimento Base" — a <b>total</b> distance for the whole manoeuvre, the one
     * deliberate exception to {@code MovementService}'s per-Ponto-de-Ação rule.
     */
    @Test
    void anInvestidaCarriesTwiceTheMovimentoBase() {
        CharacterSheet sheet = wielding(sword());

        assertEquals(2 * movementService.getMovementBase(sheet),
                chargeService.getMovementAllowance(sheet));
    }

    /** IMPLACAVEL: "o triplo do seu Movimento Base, ao invés do dobro". */
    @Test
    void implacavelCarriesThreeTimesTheMovimentoBase() {
        CharacterSheet sheet = sheetOf(character()
                .attributeAbility(DexterityAbility.IMPLACAVEL)
                .build());

        assertEquals(3 * movementService.getMovementBase(sheet),
                chargeService.getMovementAllowance(sheet));
    }

    /** Movimento Base is 0 while a Condição forbids moving, so the allowance is too. */
    @Test
    void aCombatantWhoCannotMoveCarriesNoDistance() {
        CharacterSheet sheet = wielding(sword());
        sheet.applyCondition(new Condition(ConditionType.IMOBILIZADO, 2));

        assertEquals(0, chargeService.getMovementAllowance(sheet));
    }

    // ---------- the gates ----------

    @Test
    void aDrawnMeleeWeaponMayCharge() {
        Weapon sword = sword();

        assertTrue(chargeService.canCharge(wielding(sword), sword));
    }

    /**
     * The weapon must be in hand <b>beforehand</b>: an Investida bundles a movement with an attack
     * and prices neither a draw nor a moment to make one.
     */
    @Test
    void aCarriedButSheathedWeaponCannotCharge() {
        CharacterSheet sheet = sheetOf(character().build());
        Weapon sword = sword();
        sheet.getCharacter().equip(sword);

        assertFalse(chargeService.canCharge(sheet, sword));
        assertThrows(IllegalOperationException.class, () -> chargeService.begin(sheet, sword, null));
    }

    @Test
    void aWeaponTheCombatantDoesNotHoldAtAllCannotCharge() {
        assertFalse(chargeService.canCharge(sheetOf(character().build()), sword()));
    }

    /** Melee is the Perícia, not the ItemCategory — and a bow is not swung as one. */
    @Test
    void aRangedWeaponCannotCharge() {
        Weapon bow = bow();

        assertFalse(chargeService.canCharge(wielding(bow), bow));
    }

    /**
     * An Arma Natural charges without being drawn — it is not equipment, so there is nothing to
     * draw. Guampo's Chifres Majestosos ("Investidas usando os chifres…") is the clause that
     * requires this to work.
     */
    @Test
    void anArmaNaturalMayChargeWithoutBeingDrawn() {
        Character character = character().build();
        character.grantFeat(new org.aventyrs.core.feat.AbstractFeat(
                FeatCategory.MOBILIDADE, "Chifres", null) {
            @Override
            public java.util.List<NaturalWeapon> getGrantedNaturalWeapons(final Character holder) {
                return java.util.List.of(NaturalWeapon.CHIFRES_PODEROSOS);
            }
        });

        assertTrue(chargeService.canCharge(sheetOf(character), NaturalWeapon.CHIFRES_PODEROSOS));
    }

    /** An Ataque Desarmado is a legitimate charge — no weapon to draw, nothing to classify. */
    @Test
    void anUnarmedCombatantMayCharge() {
        assertTrue(chargeService.canCharge(sheetOf(character().build()), null));
    }

    @Test
    void aCombatantWhoCannotMoveCannotCharge() {
        Weapon sword = sword();
        CharacterSheet sheet = wielding(sword);
        assertTrue(chargeService.canCharge(sheet, sword));

        sheet.applyCondition(new Condition(ConditionType.AGARRADO, 2));

        assertFalse(chargeService.canCharge(sheet, sword));
    }

    /** A Forma that suppresses weapons takes the sword out of play without unequipping it. */
    @Test
    void aFormaSuppressingWeaponsRefusesTheCharge() {
        CharacterSheet sheet = sheetOf(character().build());
        Weapon sword = sword();
        sheet.getCharacter().equip(sword);
        sheet.getCharacter().drawWeapon(sword);
        sheet.enterForm(FormType.NEVOA);

        assertFalse(chargeService.canCharge(sheet, sword));
    }

    // ---------- begin ----------

    /** {@code begin} claims the movement — the counter a per-movement clause resolves against. */
    @Test
    void beginningAChargeClaimsTheRoundsMovement() {
        Weapon sword = sword();
        CharacterSheet sheet = wielding(sword);
        assertEquals(0, sheet.getMovementsTakenThisRound());

        ChargeResult result = chargeService.begin(sheet, sword, null);

        assertEquals(1, sheet.getMovementsTakenThisRound());
        assertEquals(ChargeService.BASE_ACTION_POINT_COST, result.actionCost().actionPoints());
        assertEquals(2 * movementService.getMovementBase(sheet),
                result.movementAllowanceInUnidadesDeDistancia());
        assertTrue(result.provokedReactors().isEmpty());
    }

    // ---------- movement Redução de Danos ----------

    @Test
    void noTalentoGrantsMovementDamageReductionByDefault() {
        assertEquals(0, chargeService.getMovementDamageReduction(character().build()));
    }

    /**
     * {@code MobilidadeFeat#INVESTIDA_SELVAGEM} — "Redução de Danos Sofridos igual ao número de
     * Títulos Aventyrs que você possuir". Reported on the result, never granted onto the sheet:
     * "durante o movimento da investida" is shorter than the shortest {@code TemporaryBonus}.
     */
    @Test
    void investidaSelvagemReportsOnePointPerTituloHeld() {
        Character character = character().build();
        character.grantFeat(MobilidadeFeat.INVESTIDA_SELVAGEM);

        assertEquals(character.getAllTitles().size(),
                chargeService.getMovementDamageReduction(character));
    }

    // ---------- the aftermath ----------

    @Test
    void aLandedInvestidaCostsItsChargerNothing() {
        CharacterSheet sheet = wielding(sword());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        assertNull(chargeService.applyOutcome(sheet, true));
        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void aMissedInvestidaCostsTwoDefesasForOneRodada() {
        CharacterSheet sheet = wielding(sword());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        assertNotNull(chargeService.applyOutcome(sheet, false));

        assertEquals(before + ChargeService.MISS_DEFENSE_MALUS,
                defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        sheet.tickTemporaryEffects();
        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    /**
     * The penalty is granted as a sourced {@code Blessing}, so a second failed Investida
     * <b>renews</b> the window rather than deepening the Redutor to -4. The rules say nothing
     * about two failed charges and renewal is the conservative reading.
     */
    @Test
    void asecondMissRenewsTheRedutorRatherThanDeepeningIt() {
        CharacterSheet sheet = wielding(sword());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        chargeService.applyOutcome(sheet, false);
        chargeService.applyOutcome(sheet, false);

        assertEquals(before + ChargeService.MISS_DEFENSE_MALUS,
                defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    /** IMPLACAVEL: "quando malsucedido, você não recebe o redutor padrão de -2 em suas Defesas". */
    @Test
    void implacavelIsSparedTheMissPenalty() {
        CharacterSheet sheet = sheetOf(character()
                .attributeAbility(DexterityAbility.IMPLACAVEL)
                .build());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        assertNull(chargeService.applyOutcome(sheet, false));
        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }
}
