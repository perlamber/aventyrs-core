package org.aventyrs.core.effect;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.ForcedTargeting;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every Efeito Crítico Ofensivo built from its catalog identity, applied to a real sheet. */
class CriticalEffectsTest {

    private static final CriticalResult MAIOR = CriticalResult.ACERTO_CRITICO_MAIOR;
    private static final CriticalResult MENOR = CriticalResult.ACERTO_CRITICO_MENOR;

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private CharacterSheet attacker;
    private CharacterSheet target;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        attacker = sheet();
        target = sheet();
    }

    private static CharacterSheet sheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CriticalEffect effect(final CriticalEffectType type, final CriticalResult result, final Integer... dice) {
        CriticalEffectContext context = CriticalEffectContext.of(attacker, NaturalWeapon.GARRAS_AFIADAS, result,
                dice.length == 0 ? null : DiceRoller.fixed(dice));
        return CriticalEffects.create(type, context).orElseThrow();
    }

    private int defense(final CombatantSheet sheet) {
        return defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);
    }

    @Test
    void everyIdentityButDesmembrarIsBuiltAndReportsItsOwnType() {
        for (CriticalEffectType type : CriticalEffectType.values()) {
            CriticalEffectContext context = CriticalEffectContext.of(attacker, null, MAIOR, DiceRoller.fixed(3));
            if (type == CriticalEffectType.DESMEMBRAR) {
                assertTrue(CriticalEffects.create(type, context).isEmpty());
            } else {
                assertEquals(type, CriticalEffects.create(type, context).orElseThrow().getType(), type.name());
            }
        }
    }

    @Test
    void aDiceBearingEffectIsNotBuiltWithoutDice() {
        CriticalEffectContext noDice = CriticalEffectContext.of(attacker, null, MENOR, null);

        assertTrue(CriticalEffects.create(CriticalEffectType.AMALDICOAR, noDice).isEmpty());
        assertTrue(CriticalEffects.create(CriticalEffectType.DILACERAR, noDice).isEmpty());
        assertTrue(CriticalEffects.create(CriticalEffectType.SANGRAMENTO, noDice).isPresent());
        // Dilacerar's Maior hits both Atributos and throws nothing.
        assertTrue(CriticalEffects.create(CriticalEffectType.DILACERAR,
                CriticalEffectContext.of(attacker, null, MAIOR, null)).isPresent());
    }

    @Test
    void aDefendersFalhaCriticaReadsAsTheAcertoOfTheSameSeverity() {
        assertTrue(CriticalEffectContext.of(attacker, null, CriticalResult.FALHA_CRITICA_MAIOR, null).isMajor());
        assertFalse(CriticalEffectContext.of(attacker, null, CriticalResult.FALHA_CRITICA_MENOR, null).isMajor());
    }

    @Test
    void amaldicoarCursesForADieOfRodadasAndCostsDefesas() {
        int before = defense(target);

        effect(CriticalEffectType.AMALDICOAR, MAIOR, 4).applyTo(target);

        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(before - 5, defense(target));
    }

    @Test
    void imunizarWardsOffAmaldicoarAndHarmfulEnchantments() {
        effect(CriticalEffectType.IMUNIZAR, MENOR).applyTo(target);
        int before = defense(target);

        effect(CriticalEffectType.AMALDICOAR, MENOR, 4).applyTo(target);

        assertFalse(target.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(before, defense(target));
        assertFalse(target.applyEnchantment(new ForcedTargeting(attacker, 2)));
    }

    @Test
    void amenizarGrantsOneInstanceOfRa() {
        effect(CriticalEffectType.AMENIZAR, MENOR).applyTo(target);

        assertEquals(Amenizar.ABSOLUTE_REDUCTION, target.getTemporaryBonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION));
    }

    @Test
    void atordoanteMaiorLeavesNoActionAtAll() {
        ActionPointsService actionPoints = new ActionPointsServiceImpl();
        ReactionsService reactions = new ReactionsServiceImpl();

        effect(CriticalEffectType.ATORDOANTE, MAIOR).applyTo(target);

        assertEquals(0, actionPoints.getMaxActionPoints(target, 0, null));
        assertEquals(0, reactions.getTotalReactions(target, 0, null));
    }

    @Test
    void atordoanteMenorTakesReactionsAndConfuses() {
        ReactionsService reactions = new ReactionsServiceImpl();

        effect(CriticalEffectType.ATORDOANTE, MENOR).applyTo(target);

        assertEquals(0, reactions.getTotalReactions(target, 0, null));
        assertTrue(target.hasCondition(ConditionType.CONFUSO, null));
    }

    @Test
    void cataclismoRepeatsTheElementalDamageNextRodada() {
        target.recordDamageReceived(new DamageReceipt(8, DamageType.FISICO_ELEMENTAL, attacker));
        int before = target.getDamageTaken();

        effect(CriticalEffectType.CATACLISMO, MENOR).applyTo(target);
        assertEquals(before, target.getDamageTaken());
        target.startNewRound();

        assertEquals(before + 4, target.getDamageTaken());
    }

    @Test
    void cataclismoHasNothingToRepeatAfterAnUntypedHit() {
        target.recordDamageReceived(new DamageReceipt(8, null, attacker));
        int before = target.getDamageTaken();

        effect(CriticalEffectType.CATACLISMO, MAIOR).applyTo(target);
        target.startNewRound();

        assertEquals(before, target.getDamageTaken());
    }

    @Test
    void dilacerarCostsATemporaryAttributePointUntilARest() {
        effect(CriticalEffectType.DILACERAR, MAIOR).applyTo(target);

        assertEquals(-1, target.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertEquals(-1, target.getTemporaryBonus(ModifierType.DEXTERITY_BONUS));

        target.clearRestCooldowns(RestType.MINIMO);
        assertEquals(0, target.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
    }

    @Test
    void dilacerarMenorPicksOneByTheDie() {
        effect(CriticalEffectType.DILACERAR, MENOR, 5).applyTo(target);

        assertEquals(0, target.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertEquals(-1, target.getTemporaryBonus(ModifierType.DEXTERITY_BONUS));
    }

    @Test
    void empalarLeavesTheWeaponStuckAndPullingItOutHurts() {
        effect(CriticalEffectType.EMPALAR, MENOR).applyTo(target);
        int before = target.getDamageTaken();

        assertTrue(target.getImpalement().isPresent());
        assertEquals(NaturalWeapon.GARRAS_AFIADAS, target.getImpalement().get().getWeapon());
        assertEquals(7, target.removeImpalement(DiceRoller.fixed(3, 4)));
        assertEquals(before + 7, target.getDamageTaken());
        assertTrue(target.getImpalement().isEmpty());
    }

    @Test
    void estilhacadorMaiorDamagesEveryItemByHalfTheHit() {
        Item armour = AbstractItem.builder().name("Armadura").category(ItemCategory.ARMOR).hardness(20).build();
        Item helmet = AbstractItem.builder().name("Elmo").category(ItemCategory.HELMET).hardness(20).build();
        target.getCharacter().equip(armour);
        target.getCharacter().equip(helmet);
        target.recordDamageReceived(new DamageReceipt(9, null, attacker));

        effect(CriticalEffectType.ESTILHACADOR, MAIOR).applyTo(target);

        assertEquals(4, armour.getDamageTaken());
        assertEquals(4, helmet.getDamageTaken());
    }

    @Test
    void estilhacadorMenorRerollsUntilItFindsAWornKind() {
        Item helmet = AbstractItem.builder().name("Elmo").category(ItemCategory.HELMET).hardness(20).build();
        target.getCharacter().equip(helmet);
        target.recordDamageReceived(new DamageReceipt(6, null, attacker));

        // 2 (Armadura) and 6 (Núcleo) aren't worn; 5 is the Elmo.
        effect(CriticalEffectType.ESTILHACADOR, MENOR, 2, 6, 5).applyTo(target);

        assertEquals(3, helmet.getDamageTaken());
    }

    @Test
    void excrucianteDrainsPdUntilAHeal() {
        effect(CriticalEffectType.EXCRUCIANTE, MENOR).applyTo(target);
        assertEquals(2, target.getDeterminationSpent());

        target.finishTurn();
        assertEquals(3, target.getDeterminationSpent());

        target.applyDamage(1);
        target.heal(1);
        target.finishTurn();
        assertEquals(3, target.getDeterminationSpent());
    }

    @Test
    void feridaProfundaMaiorCostsMultipliersAndBlocksHealingUntilCombatEnds() {
        int multiplier = hitPointsService.getLifeMultiplier(target.getCharacter(), target);

        effect(CriticalEffectType.FERIDA_PROFUNDA, MAIOR).applyTo(target);

        assertEquals(Math.max(1, multiplier - 3), hitPointsService.getLifeMultiplier(target.getCharacter(), target));
        target.applyDamage(5);
        target.heal(5);
        assertEquals(5, target.getDamageTaken());

        target.endCombat();
        assertEquals(multiplier, hitPointsService.getLifeMultiplier(target.getCharacter(), target));
        target.heal(5);
        assertEquals(0, target.getDamageTaken());
    }

    @Test
    void feridaProfundaMenorHalvesHealing() {
        effect(CriticalEffectType.FERIDA_PROFUNDA, MENOR).applyTo(target);
        target.applyDamage(8);

        target.heal(8);

        assertEquals(4, target.getDamageTaken());
    }

    @Test
    void fortalecerGrantsRdAndRm() {
        effect(CriticalEffectType.FORTALECER, MENOR).applyTo(target);

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, target.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
        assertEquals(Fortalecer.MAGIC_REDUCTION, target.getTemporaryBonus(ModifierType.MAGIC_REDUCTION));
    }

    @Test
    void guilhotinaRewardsTheAttackerAndItsMarginStacks() {
        effect(CriticalEffectType.GUILHOTINA, MAIOR).applyTo(target);
        effect(CriticalEffectType.GUILHOTINA, MENOR).applyTo(target);

        assertEquals(Skill.ADVANTAGE_BONUS, attacker.getTemporaryBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS));
        assertEquals(Skill.ADVANTAGE_BONUS, attacker.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
        assertEquals(3, attacker.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
        assertEquals(0, target.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
    }

    @Test
    void inflamarBurnsEachRodadaUntilPutOut() {
        effect(CriticalEffectType.INFLAMAR, MAIOR).applyTo(target);
        assertEquals(3, target.getBurning().orElseThrow().getExtinguishActionPoints());

        target.finishTurn();
        assertEquals(2, target.getDamageTaken());

        target.extinguish();
        target.finishTurn();
        assertEquals(2, target.getDamageTaken());
    }

    @Test
    void oferendaMalditaStealsNoMoreThanTheHitDealt() {
        attacker.applyDamage(10);
        target.recordDamageReceived(new DamageReceipt(3, null, attacker));

        effect(CriticalEffectType.OFERENDA_MALDITA, MAIOR, 6, 6).applyTo(target);

        assertEquals(7, attacker.getDamageTaken());
    }

    @Test
    void potencializarThrowsItsUnits() {
        Potencializar effect = (Potencializar) effect(CriticalEffectType.POTENCIALIZAR, MAIOR, 2, 5);

        assertEquals(7, effect.getDurationIncrease());
    }

    @Test
    void prevenirGuardsTheAttackerAgainstTheTarget() {
        effect(CriticalEffectType.PREVENIR, MAIOR).applyTo(target);
        SceneContext againstTarget = new SceneContext(List.of(), List.of(target), Map.of(), null, true, 0, false,
                target);

        assertEquals(defense(attacker) + 5,
                defenseService.getTotalDefense(attacker, DefenseType.PHYSICAL, againstTarget));
    }

    @Test
    void toqueDoAetherSilences() {
        effect(CriticalEffectType.TOQUE_DO_AETHER, MAIOR).applyTo(target);

        assertTrue(target.isSpellCastingPrevented(null));
        assertTrue(target.isAbilityActivationPrevented(null));
    }

    @Test
    void aBuiltEffectIsAnAbstractCriticalEffectDescribingItsTier() {
        CriticalEffect maior = effect(CriticalEffectType.IMUNIZAR, MAIOR);
        CriticalEffect menor = effect(CriticalEffectType.IMUNIZAR, MENOR);

        assertInstanceOf(AbstractCriticalEffect.class, maior);
        assertTrue(maior.getDescription().contains("5 Rodadas"));
        assertTrue(menor.getDescription().contains("2 Rodadas"));
    }
}
