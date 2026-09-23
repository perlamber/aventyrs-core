package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.DefensiveCriticalEffect;
import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.aventyrs.core.effect.DefensiveCriticalOutcome;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.senhordabriga.FantasmaDoRingueAbility;
import org.aventyrs.core.title.senhordabriga.SenhorDaBriga;
import org.aventyrs.core.title.senhordabriga.SenhorDaBrigaAbility;
import org.aventyrs.core.title.senhordabriga.SenhorDaBrigaSpecialization;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Efeitos Críticos end to end: an attack source's own, a Título's additions and Finalização's
 * repetitions through {@link AttackDelivery}; Efeitos Críticos Defensivos through {@link
 * AttackReceiver}; and the Margem Crítica Maior deciding which tier fires.
 */
class CriticalEffectsInCombatTest {

    private static final Weapon CLAWS = NaturalWeapon.GARRAS_AFIADAS;  // Dilacerar

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final AttackReceiver attackReceiver = new AttackReceiver();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet fighter(final SenhorDaBriga title) {
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        melee.increaseGraduation(3);
        CharacterSkill dodge = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        dodge.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .skill(SkillType.ESQUIVA_E_APARAR, dodge)
                .build();
        if (title != null) {
            character.grantTitle(title, TitleSlot.PRIMARY);
        }
        return CharacterSheet.of(character, new Player());
    }

    private static SenhorDaBriga senhor(final List<SenhorDaBrigaSpecialization> specializations,
                                        final AventyrTitleAbility... abilities) {
        return new SenhorDaBriga(specializations, List.of(abilities));
    }

    private DeliveredAttackResult strike(final CombatantSheet attacker, final MonsterSheet foe, final List<Integer> dice,
                                         final DiceRoller roller) {
        return attackDelivery.resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(CLAWS)
                .attackRoll(new SkillRoll(dice))
                .diceRoller(roller)
                .build());
    }

    /** Runs the chain the way a caller does, with enough raw damage to get past any RD. */
    private static void drain(final CombatantSheet target, final Interaction<CombatantSheet> head) {
        if (!(head instanceof DamageInteraction damage)) {
            return;
        }
        InteractionResult result = damage.applyTo(target, 30, false);
        while (result.getNextInteraction() != null) {
            result = target.receiveInteraction(result.getNextInteraction());
        }
    }

    @Test
    void aNaturalWeaponsOwnEfeitoCriticoLandsOnACriticalHit() {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        DeliveredAttackResult result = strike(fighter(null), foe, List.of(6, 6, 6), null);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
        drain(foe, result.getAttackResult().getNextInteraction());

        // Dilacerar Maior: a point of Força and of Destreza, no dice needed.
        assertEquals(-1, foe.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertEquals(-1, foe.getTemporaryBonus(ModifierType.DEXTERITY_BONUS));
        assertTrue(result.getUnappliedCriticalEffects().isEmpty());
    }

    @Test
    void aDiceBearingEffectWithNoRollerIsReportedNotInvented() {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        DeliveredAttackResult result = strike(fighter(null), foe, List.of(6, 6, 5), null);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
        assertEquals(List.of(CriticalEffectType.DILACERAR), result.getUnappliedCriticalEffects());
    }

    @Test
    void campeaoDaTavernasMaiorMarginTurnsSeventeenIntoAMaior() {
        SenhorDaBriga campeao = senhor(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE),
                SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = strike(fighter(campeao), foe, List.of(6, 6, 5), DiceRoller.fixed(3));

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
    }

    @Test
    void resistenciaACriticosPushesTheMaiorMarginBack() {
        SenhorDaBriga campeao = senhor(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE),
                SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        foe.applyEffect(new TemporaryBonus(ModifierType.CRITICAL_RESISTANCE, 2, 3));

        DeliveredAttackResult result = strike(fighter(campeao), foe, List.of(6, 6, 5), DiceRoller.fixed(3));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
    }

    @Test
    void punhoInigualavelAddsGuilhotinaToTheAttacker() {
        CharacterSheet attacker = fighter(senhor(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL)));
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());

        drain(foe, strike(attacker, foe, List.of(6, 6, 6), null).getAttackResult().getNextInteraction());

        assertEquals(2, attacker.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
    }

    @Test
    void finalizacaoAppliesTheNaturalEffectsMenorOnAPlainHit() {
        SenhorDaBriga title = senhor(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                SenhorDaBrigaAbility.FINALIZACAO);
        CharacterSheet attacker = fighter(title);
        attacker.openActivationWindow(SenhorDaBrigaAbility.FINALIZACAO, 1);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = strike(attacker, foe, List.of(4, 4, 4), DiceRoller.fixed(2));
        assertTrue(result.getHit());
        assertEquals(CriticalResult.NONE, result.getCriticalResult());
        drain(foe, result.getAttackResult().getNextInteraction());

        // Dilacerar Menor, the die (2) picking Força.
        assertEquals(-1, foe.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertEquals(0, foe.getTemporaryBonus(ModifierType.DEXTERITY_BONUS));
    }

    @Test
    void withoutFinalizacaoAPlainHitAppliesNoEfeitoCritico() {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());

        drain(foe, strike(fighter(null), foe, List.of(4, 4, 4), DiceRoller.fixed(2))
                .getAttackResult().getNextInteraction());

        assertEquals(0, foe.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
    }

    private IncomingAttackResult defend(final CombatantSheet defender, final CombatantSheet attacker,
                                        final List<Integer> dice, final DiceRoller roller) {
        return defend(defender, attacker, dice, roller, 0);
    }

    private IncomingAttackResult defend(final CombatantSheet defender, final CombatantSheet attacker,
                                        final List<Integer> dice, final DiceRoller roller, final int attackBonus) {
        return attackReceiver.resolve(IncomingAttack.builder()
                .attackBonus(attackBonus)
                .defender(defender)
                .attacker(attacker)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(CLAWS)
                .defenseRoll(new SkillRoll(dice))
                .diceRoller(roller)
                .build());
    }

    private static List<DefensiveCriticalEffectType> types(final IncomingAttackResult result) {
        return result.getDefensiveCriticalEffects().stream().map(DefensiveCriticalEffect::getType).toList();
    }

    @Test
    void anUnarmouredDefendersCriticalDefenceGrantsLiberdadeDeAcao() {
        IncomingAttackResult result = defend(fighter(null), fighter(null), List.of(6, 6, 6), null);

        assertTrue(result.getDefended());
        assertEquals(List.of(DefensiveCriticalEffectType.LIBERDADE_DE_ACAO), types(result));
        assertEquals(2, result.getDefensiveCriticalEffects().get(0).apply().defenderMayMoveUd());
    }

    @Test
    void armaduraDeJustaProvokesTheAttacker() {
        CharacterSheet defender = fighter(null);
        defender.getCharacter().equip(AbstractItem.fromTemplate(ArmorItem.ARMADURA_DE_JUSTA));
        CharacterSheet attacker = fighter(null);

        IncomingAttackResult result = defend(defender, attacker, List.of(6, 6, 6), null);
        assertEquals(List.of(DefensiveCriticalEffectType.PROVOCAR), types(result));
        DefensiveCriticalOutcome outcome = result.getDefensiveCriticalEffects().get(0).apply();

        assertEquals(3, outcome.damageToAttacker());
        assertEquals(3, attacker.getDamageTaken());
        assertEquals(defender, attacker.getForcedTargeting().orElseThrow().getEnchanter());
    }

    @Test
    void retornoDeDanosWithoutDiceIsReported() {
        CharacterSheet defender = fighter(null);
        defender.getCharacter().equip(AbstractItem.fromTemplate(ArmorItem.ARMADURA_COMPLETA));

        IncomingAttackResult result = defend(defender, fighter(null), List.of(6, 6, 6), null);

        assertTrue(result.getDefensiveCriticalEffects().isEmpty());
        assertEquals(List.of(DefensiveCriticalEffectType.RETORNO_DE_DANOS), result.getUnappliedCriticalEffects());
    }

    @Test
    void retornoDeDanosHurtsAMeleeAttacker() {
        CharacterSheet defender = fighter(null);
        defender.getCharacter().equip(AbstractItem.fromTemplate(ArmorItem.ARMADURA_COMPLETA));
        CharacterSheet attacker = fighter(null);

        IncomingAttackResult result = defend(defender, attacker, List.of(6, 6, 6), DiceRoller.fixed(2, 2, 2));
        DefensiveCriticalOutcome outcome = result.getDefensiveCriticalEffects().get(0).apply();

        // Maior: 3d6 (6) + the defender's Vigor (3).
        assertEquals(9, outcome.damageToAttacker());
    }

    @Test
    void fantasmaDoRinguesImpetoDefensivoMaiorMakesTheDefenderImmuneToThatAttacker() {
        CharacterSheet defender = fighter(senhor(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE)));
        CharacterSheet attacker = fighter(null);

        IncomingAttackResult critical = defend(defender, attacker, List.of(6, 6, 6), null);
        assertTrue(types(critical).contains(DefensiveCriticalEffectType.IMPETO_DEFENSIVO));
        critical.getDefensiveCriticalEffects().forEach(DefensiveCriticalEffect::apply);

        // An attack no Defesa could hold: only the immunity explains holding it.
        assertTrue(defend(defender, attacker, List.of(1, 2, 2), null, 30).getDefended());
        assertFalse(defend(defender, fighter(null), List.of(1, 2, 2), null, 30).getDefended());
    }

    @Test
    void cruzDeSangueAddsContraAtacanteWhileItsRodadaLasts() {
        CharacterSheet defender = fighter(senhor(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE),
                FantasmaDoRingueAbility.CRUZ_DE_SANGUE));
        defender.openActivationWindow(FantasmaDoRingueAbility.CRUZ_DE_SANGUE, 1);

        IncomingAttackResult result = defend(defender, fighter(null), List.of(6, 6, 6), null);
        DefensiveCriticalEffect counter = result.getDefensiveCriticalEffects().stream()
                .filter(effect -> effect.getType() == DefensiveCriticalEffectType.CONTRA_ATACANTE)
                .findFirst().orElseThrow();

        DefensiveCriticalOutcome outcome = counter.apply();
        assertTrue(outcome.counterAttack());
        assertTrue(outcome.counterAttackMinorCritical());
    }

    @Test
    void anAttackersEfeitoCriticoLandsOnTheDefendersFalhaCritica() {
        CharacterSheet defender = fighter(null);

        IncomingAttackResult result = defend(defender, fighter(null), List.of(1, 1, 1), null);
        assertFalse(result.getDefended());
        drain(defender, result.getDefenseResult().getNextInteraction());

        assertEquals(-1, defender.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertEquals(-1, defender.getTemporaryBonus(ModifierType.DEXTERITY_BONUS));
    }

    @Test
    void aForgedCopyKeepsItsCatalogEntrysCriticalColumns() {
        assertEquals(DefensiveCriticalEffectType.PROVOCAR,
                AbstractItem.fromTemplate(ArmorItem.ARMADURA_DE_JUSTA).getDefensiveCriticalEffect());
    }
}
