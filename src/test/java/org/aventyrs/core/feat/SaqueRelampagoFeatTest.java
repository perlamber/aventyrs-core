package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link SaqueRelampagoFeat} tested at both layers: the {@code
 * resolveAttackCostDifficultyReduction} truth table, and the effect on a character who acquired
 * it legally — a Perícia de Ataque roll costing 1PA is one GD nível easier the first time each
 * Rodada.
 */
class SaqueRelampagoFeatTest {

    private final FeatService featService = new FeatServiceImpl();

    private static final AttackSource WEAPON = AbstractWeapon.builder()
            .name("Arco").category(ItemCategory.BOW)
            .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_A_DISTANCIA).build();
    private static final AttackSource SPELL = new TestSpell();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CombatantAction cheapWeaponAttack() {
        return new CombatantAction(SkillType.ATAQUE_A_DISTANCIA, AttributeDomain.DEXTERITY, WEAPON,
                ActionCost.ofActionPoints(1), 0, null);
    }

    private static CombatantAction cheapSpellAttack() {
        return new CombatantAction(SkillType.ATAQUE_A_DISTANCIA, AttributeDomain.DEXTERITY, SPELL,
                ActionCost.ofActionPoints(1), 0, null);
    }

    private static int reduction(final SaqueRelampagoFeat feat, final SkillType skill,
                                 final AttackSource source, final ActionCost cost,
                                 final List<CombatantAction> history) {
        return feat.resolveAttackCostDifficultyReduction(skill, null, null, source, cost, history);
    }

    // ---------- identity / choice ----------

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        assertSame(AssassinoFeat.SAQUE_RELAMPAGO, SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS).catalogEntry());
    }

    @Test
    void requiresAChosenMethod() {
        assertThrows(NullPointerException.class, () -> SaqueRelampagoFeat.of(null));
    }

    @Test
    void chosenByReadsTheHeldChoice() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        character.grantFeat(SaqueRelampagoFeat.of(WeaponOrSpellChoice.SPELLS));

        assertEquals(WeaponOrSpellChoice.SPELLS, SaqueRelampagoFeat.chosenBy(character).orElseThrow());
    }

    // ---------- the hook truth table ----------

    @Test
    void reducesGdForTheFirstCheapAttackOfTheChosenMethodEachRound() {
        SaqueRelampagoFeat weapons = SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS);

        assertEquals(1, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON, ActionCost.ofActionPoints(1), List.of()));
        assertEquals(1, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON, ActionCost.FREE_ACTION, List.of()));
        assertEquals(1, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON, ActionCost.REACTION, List.of()));
        assertEquals(0, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON, ActionCost.ofActionPoints(2), List.of()));
        assertEquals(0, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON, null, List.of()));
        assertEquals(0, reduction(weapons, SkillType.ATLETISMO, WEAPON, ActionCost.ofActionPoints(1), List.of()));
        assertEquals(0, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, SPELL, ActionCost.ofActionPoints(1), List.of()));
    }

    @Test
    void spendsTheRoundsOneReductionOnTheFirstQualifyingAttack() {
        SaqueRelampagoFeat weapons = SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS);

        assertEquals(0, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON,
                ActionCost.ofActionPoints(1), List.of(cheapWeaponAttack())));
    }

    @Test
    void aPriorCheapSpellAttackDoesNotSpendAWeaponChoicesReduction() {
        SaqueRelampagoFeat weapons = SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS);

        assertEquals(1, reduction(weapons, SkillType.ATAQUE_A_DISTANCIA, WEAPON,
                ActionCost.ofActionPoints(1), List.of(cheapSpellAttack())));
    }

    // ---------- the effect on a character who acquired it ----------

    private static Character.CharacterBuilder dexterousBuild() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build());
    }

    private Character assassin() throws IllegalOperationException {
        Character character = dexterousBuild().build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(50));
        featService.grantFeat(character, sheet, AssassinoFeat.SAQUE_RAPIDO);
        featService.grantFeat(character, sheet, SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));
        return character;
    }

    /** A 1PA weapon attack roll reports one more GD nível reduced once the Talento is held. */
    @Test
    void acquiringItEasesTheFirstCheapWeaponAttackRollEachRound() throws IllegalOperationException {
        Character before = dexterousBuild().build();
        Character after = assassin();

        SkillRoll roll = new SkillRoll(List.of(3, 3, 3), null, DifficultyLevel.MEDIUM.getBaseValue(),
                ActionCost.ofActionPoints(1));

        int baseline = new AtaqueADistanciaInteraction()
                .applyTo(CharacterSheet.of(before, new Player()), null, roll, null, WEAPON)
                .getDifficultyReduction();
        int withFeat = new AtaqueADistanciaInteraction()
                .applyTo(CharacterSheet.of(after, new Player()), null, roll, null, WEAPON)
                .getDifficultyReduction();

        assertEquals(baseline + 1, withFeat);
    }

    /** Once an action is recorded this Rodada, a second identical roll is back to the baseline. */
    @Test
    void theReductionIsNotRepeatedAfterTheFirstAttackIsRecorded() throws IllegalOperationException {
        Character character = assassin();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3), null, DifficultyLevel.MEDIUM.getBaseValue(),
                ActionCost.ofActionPoints(1));

        InteractionResult first = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll, null, WEAPON);
        sheet.recordAction(new CombatantAction(SkillType.ATAQUE_A_DISTANCIA,
                first.getGoverningAttributeDomain(), WEAPON, ActionCost.ofActionPoints(1), 0, null));
        InteractionResult second = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll, null, WEAPON);

        assertEquals(first.getDifficultyReduction() - 1, second.getDifficultyReduction());
    }

    @Test
    void aTwoPointAttackIsNeverEased() throws IllegalOperationException {
        Character character = assassin();
        SkillRoll onePoint = new SkillRoll(List.of(3, 3, 3), null, DifficultyLevel.MEDIUM.getBaseValue(),
                ActionCost.ofActionPoints(1));
        SkillRoll twoPoints = new SkillRoll(List.of(3, 3, 3), null, DifficultyLevel.MEDIUM.getBaseValue(),
                ActionCost.ofActionPoints(2));

        int eased = new AtaqueADistanciaInteraction()
                .applyTo(CharacterSheet.of(character, new Player()), null, onePoint, null, WEAPON)
                .getDifficultyReduction();
        int notEased = new AtaqueADistanciaInteraction()
                .applyTo(CharacterSheet.of(character, new Player()), null, twoPoints, null, WEAPON)
                .getDifficultyReduction();

        assertEquals(eased - 1, notEased);
    }

    @Test
    void everyOtherAssassinoConstantLeavesTheAttackCostReductionAtZero() {
        for (AssassinoFeat feat : AssassinoFeat.values()) {
            assertEquals(0, feat.resolveAttackCostDifficultyReduction(
                    SkillType.ATAQUE_A_DISTANCIA, null, null, WEAPON, ActionCost.ofActionPoints(1), List.of()),
                    feat.name());
        }
    }
}
