package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.Definhar;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.Sangramento;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackDeliveryTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** A player attacker: Força + Graduação in Ataque Corpo a Corpo, nothing else. */
    private CharacterSheet attacker(final int strengthBase, final int graduation) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(graduation);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(strengthBase).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private DeliveredAttack.DeliveredAttackBuilder attackOn(final MonsterSheet foe, final CharacterSheet attacker) {
        return DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO);
    }

    @Test
    void anAttackTotalEqualToTheDefesaCountsAsAHit() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());  // DF 13
        CharacterSheet hero = attacker(4, 3);                   // 4 + 3 = 7 bonus

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, hero)
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))    // 7 + 6 = 13
                .build());

        assertEquals(capanga.getPhysicalDefense(), result.getRequiredTotal());
        assertEquals(result.getRequiredTotal(), result.getAttackTotal());
        assertEquals(0, result.getMargin());
        assertTrue(result.getHit());
    }

    @Test
    void oneShortOfTheDefesaIsAMiss() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        CharacterSheet hero = attacker(4, 3);

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, hero)
                .attackRoll(new SkillRoll(List.of(2, 2, 1)))
                .build());

        assertFalse(result.getHit());
        assertEquals(-1, result.getMargin());
        assertNull(result.getAttackResult().getNextInteraction());
    }

    @Test
    void dfAndDmAreDifferentTargetNumbers() {
        MonsterSheet conjurador = GenericMonster.CONJURADOR.spawn(new Player());  // DF 13, DM 19
        CharacterSheet hero = attacker(4, 3);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        DeliveredAttackResult physical = attackDelivery.resolve(
                DeliveredAttack.from(conjurador, DefenseType.PHYSICAL)
                        .attacker(hero).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).attackRoll(roll).build());
        DeliveredAttackResult magic = attackDelivery.resolve(
                DeliveredAttack.from(conjurador, DefenseType.MAGIC)
                        .attacker(attacker(4, 3)).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).attackRoll(roll).build());

        assertEquals(13, physical.getRequiredTotal());
        assertEquals(19, magic.getRequiredTotal());
        assertTrue(physical.getHit());
        assertFalse(magic.getHit());
    }

    @Test
    void aLandedAttackChainsADamageInteractionEvenWithNoEffects() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, attacker(6, 6))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertTrue(result.getHit());
        assertInstanceOf(DamageInteraction.class, result.getAttackResult().getNextInteraction());
    }

    /**
     * The inversion the defensive direction suffers from doesn't exist here: an attacker's
     * Acerto Crítico is exactly what CriticalEffect's own construction demands.
     */
    @Test
    void anAcertoCriticoFiresTheAttacksCriticalEffects() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        Sangramento bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, attacker(4, 3))
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .criticalEffect(bleed)
                .build());

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());

        DamageInteraction head = assertInstanceOf(DamageInteraction.class,
                result.getAttackResult().getNextInteraction());
        assertEquals(bleed, head.getNextInteraction());
    }

    @Test
    void anAttackClearingTheMarginFiresItsEffectChains() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());  // DF 13
        Definhar definhar = new Definhar();

        // 6 Força + 6 Graduação = 12 bonus, +6 dice = 18, a margin of 5 over DF 13.
        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, attacker(6, 6))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .effectChain(definhar)
                .build());

        assertEquals(EffectChainService.BASE_REQUIRED_MARGIN, result.getMargin());
        assertTrue(result.getEffectChainTriggered());
        assertFalse(result.getCriticalEffectTriggered());

        DamageInteraction head = assertInstanceOf(DamageInteraction.class,
                result.getAttackResult().getNextInteraction());
        assertEquals(definhar, head.getNextInteraction());
    }

    @Test
    void resolutoOnTheDefenderRaisesTheMarginAnEffectChainMustClear() {
        MonsterSheet plain = GenericMonster.CAPANGA.spawn(new Player());
        Character resoluteMonster = plain.getCharacter().toBuilder()
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO)
                .build();
        MonsterSheet resolute = MonsterSheet.of(resoluteMonster, plain.getPlayer(), plain.getPhysicalDefense(),
                plain.getMagicDefense(), plain.getAttackDifficulty(), plain.getAttackBonus());
        SkillRoll roll = new SkillRoll(List.of(2, 2, 2));

        assertTrue(attackDelivery.resolve(attackOn(plain, attacker(6, 6))
                .attackRoll(roll).effectChain(new Definhar()).build()).getEffectChainTriggered());
        assertFalse(attackDelivery.resolve(attackOn(resolute, attacker(6, 6))
                .attackRoll(roll).effectChain(new Definhar()).build()).getEffectChainTriggered());
    }

    @Test
    void withNoAttackRollTheOutcomeIsUndeterminedButTheThresholdIsStillReported() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, attacker(4, 3)).build());

        assertNull(result.getHit());
        assertNull(result.getMargin());
        assertNull(result.getCriticalResult());
        assertNull(result.getCriticalEffectTriggered());
        assertNull(result.getEffectChainTriggered());
        assertEquals(7, result.getAttackTotal());
        assertEquals(13, result.getRequiredTotal());
        assertNull(result.getAttackResult().getNextInteraction());
    }

    /** Report-only: resolve assembles the chain but applies none of it. */
    @Test
    void resolveNeverAppliesAnythingToTheDefender() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, attacker(6, 6))
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR))
                .build());

        assertTrue(result.getHit());
        assertEquals(0, capanga.getDamageTaken());
        assertEquals(0, capanga.getShieldPoints());
    }

    /**
     * The attacker's GD reduction is denominated in níveis and the foe's Defesa is a flat
     * number, so it's reported rather than applied — see AttackDelivery's javadoc.
     */
    @Test
    void theAttackersDifficultyReductionIsReportedButNotApplied() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        // Graduação 7 unlocks Ataque Corpo a Corpo's PRODIGIO (-1 nível).
        CharacterSheet hero = attacker(1, 7);

        DeliveredAttackResult result = attackDelivery.resolve(attackOn(capanga, hero)
                .attackRoll(new SkillRoll(List.of(1, 1, 3)))
                .build());

        assertEquals(1, result.getUnappliedDifficultyReduction());
        assertEquals(capanga.getPhysicalDefense(), result.getRequiredTotal());
    }

    @Test
    void aNonAttackSkillIsRejected() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        assertThrows(IllegalOperationException.class, () -> attackDelivery.resolve(
                DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                        .attacker(attacker(4, 3))
                        .attackSkill(SkillType.ATLETISMO)
                        .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                        .build()));
    }
}
