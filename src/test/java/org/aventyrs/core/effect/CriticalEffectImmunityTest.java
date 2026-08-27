package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.Zumbi;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A target's Efeito Crítico immunities, driven through <b>both</b> directions of an exchange.
 *
 * <p>Both directions matter and neither is redundant: a summoned creature acts on its summoner's
 * roll, so a player can be the one rolling on either side of an attack involving a foe whose
 * anatomy resists part of what lands. That is why the filter lives on {@link CriticalEffect}
 * rather than in {@code AttackDelivery} or {@code AttackReceiver} — these tests pin that it
 * behaves identically from both.
 */
class CriticalEffectImmunityTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final AttackReceiver attackReceiver = new AttackReceiver();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

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

    /** Every stage behind the head DamageInteraction, in order. */
    private List<Interaction<CombatantSheet>> chainBehind(final Interaction<CombatantSheet> head) {
        List<Interaction<CombatantSheet>> stages = new ArrayList<>();
        Interaction<CombatantSheet> next = assertInstanceOf(DamageInteraction.class, head).getNextInteraction();
        while (next != null) {
            stages.add(next);
            next = next instanceof AbstractEffect effect ? effect.getNextInteraction() : null;
        }
        return stages;
    }

    // --- The filter itself ----------------------------------------------------------------

    @Test
    void everyEfeitoCriticoReportsItsOwnType() {
        assertEquals(CriticalEffectType.SANGRAMENTO,
                new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR).getType());
        assertEquals(CriticalEffectType.PURGA_DE_MANA,
                new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR).getType());
        assertEquals(CriticalEffectType.SABOTAGEM,
                new Sabotage(CriticalResult.ACERTO_CRITICO_MAIOR).getType());
        assertEquals(CriticalEffectType.EXECUCAO_REAL,
                new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR).getType());
    }

    @Test
    void applicableToLeavesAnOrdinaryTargetsEffectsAlone() {
        CharacterSheet hero = attacker(4, 3);
        List<CriticalEffect> effects = List.of(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR),
                new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR));

        assertEquals(effects, CriticalEffect.applicableTo(hero, effects));
    }

    @Test
    void applicableToDropsOnlyWhatTheTargetResists() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);
        CriticalEffect purge = new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR);

        List<CriticalEffect> survivors = CriticalEffect.applicableTo(zumbi, List.of(bleed, purge));

        assertEquals(List.of(purge), survivors);
    }

    @Test
    void anEmptyOrNullListIsHandledWithoutIncident() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());

        assertEquals(List.of(), CriticalEffect.applicableTo(zumbi, List.of()));
        assertEquals(List.of(), CriticalEffect.applicableTo(zumbi, null));
        assertEquals(List.of(), CriticalEffect.applicableTo(null, null));
    }

    // --- Player attacks the foe: AttackDelivery -------------------------------------------

    @Test
    void anImmuneDefenderDropsThatEffectFromADeliveredAttacksChain() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());   // DF 11, immune to Sangramento
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);
        CriticalEffect purge = new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR);

        DeliveredAttackResult result = attackDelivery.resolve(
                DeliveredAttack.from(zumbi, DefenseType.PHYSICAL)
                        .attacker(attacker(4, 3))
                        .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                        .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                        .criticalEffect(bleed)
                        .criticalEffect(purge)
                        .build());

        // The crit still happened — immunity shortens the chain, it doesn't un-crit the attack.
        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());

        List<Interaction<CombatantSheet>> stages = chainBehind(result.getAttackResult().getNextInteraction());
        assertEquals(1, stages.size());
        assertSame(purge, stages.get(0));
        assertFalse(stages.contains(bleed));
    }

    @Test
    void aVulnerableDefenderKeepsTheSameEffect() {
        // The control: identical attack, ordinary anatomy, and the Sangramento survives.
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);

        DeliveredAttackResult result = attackDelivery.resolve(
                DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                        .attacker(attacker(4, 3))
                        .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                        .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                        .criticalEffect(bleed)
                        .build());

        assertEquals(List.of(bleed), chainBehind(result.getAttackResult().getNextInteraction()));
    }

    // --- Foe attacks the player: AttackReceiver -------------------------------------------

    @Test
    void anImmuneDefenderDropsThatEffectFromAnIncomingAttacksChain() {
        // A Zumbi defending against something that attacks it — the mirrored direction, where
        // the defender rolls and its own Falha Crítica is the attacker's critical hit.
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);
        CriticalEffect purge = new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR);

        IncomingAttackResult result = attackReceiver.resolve(IncomingAttack.builder()
                .defender(zumbi)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(1, 1, 1)))
                .criticalEffect(bleed)
                .criticalEffect(purge)
                .build());

        assertEquals(CriticalResult.FALHA_CRITICA_MAIOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());

        List<Interaction<CombatantSheet>> stages = chainBehind(result.getDefenseResult().getNextInteraction());
        assertEquals(1, stages.size());
        assertSame(purge, stages.get(0));
    }

    @Test
    void aVulnerableDefenderKeepsTheSameEffectWhenAttacked() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);

        IncomingAttackResult result = attackReceiver.resolve(IncomingAttack.builder()
                .defender(capanga)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(1, 1, 1)))
                .criticalEffect(bleed)
                .build());

        assertEquals(List.of(bleed), chainBehind(result.getDefenseResult().getNextInteraction()));
    }

    @Test
    void aPlayerCharacterIsImmuneToNothingByDefault() {
        assertEquals(java.util.Set.of(), attacker(4, 3).getCriticalEffectImmunities());
        assertEquals(java.util.Set.of(), GenericMonster.CAPANGA.spawn(new Player()).getCriticalEffectImmunities());
    }
}
