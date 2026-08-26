package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both directions of one exchange, on one pair of combatants — the end-to-end check that the
 * player-rolls-everything model actually closes. A foe attacks the player through {@link
 * AttackReceiver}; the player attacks it back through {@link AttackDelivery}; both produce a
 * chain the same drain loop walks.
 */
class AttackRoundTest {

    private final AttackReceiver attackReceiver = new AttackReceiver();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet hero() {
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        melee.increaseGraduation(6);
        CharacterSkill defence = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        defence.increaseGraduation(4);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .skill(SkillType.ESQUIVA_E_APARAR, defence)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** Drains an assembled chain the way the effect package-info documents. */
    private int drain(final CombatantSheet target, final Interaction<CombatantSheet> head, final int rawDamage) {
        InteractionResult result = assertInstanceOf(DamageInteraction.class, head)
                .applyTo(target, rawDamage, false);
        int stages = 1;
        while (result.getNextInteraction() != null) {
            result = target.receiveInteraction(result.getNextInteraction());
            stages++;
        }
        return stages;
    }

    @Test
    void theFoeAttacksThePlayerAndThePlayerAttacksBack() {
        CharacterSheet hero = hero();
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        UUID foes = UUID.randomUUID();
        scene.setCombatScene(true);
        scene.addParticipant(hero, 14, party);
        scene.addParticipant(brute, 9, foes);

        assertEquals(List.of(brute), scene.getEnemies(hero));

        // --- Inbound: the brute swings, the hero rolls Esquiva e Aparar against its GD. ---
        IncomingAttackResult inbound = attackReceiver.resolve(IncomingAttack.builder()
                .defender(hero)
                .attacker(brute)
                .difficultyLevel(brute.getAttackDifficulty())
                .attackBonus(brute.getAttackBonus())
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(1, 1, 3)))
                .build());

        assertFalse(inbound.getDefended());
        int inboundStages = drain(hero, inbound.getDefenseResult().getNextInteraction(), 9);
        assertEquals(1, inboundStages);
        assertEquals(9, hero.getDamageTaken());

        // --- Outbound: the hero swings back, rolled against the brute's flat DF. ---
        DeliveredAttackResult outbound = attackDelivery.resolve(
                DeliveredAttack.from(brute, DefenseType.PHYSICAL)
                        .attacker(hero)
                        .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                        .attackRoll(new SkillRoll(List.of(4, 4, 4)))
                        .build());

        assertEquals(brute.getPhysicalDefense(), outbound.getRequiredTotal());
        assertTrue(outbound.getHit());
        drain(brute, outbound.getAttackResult().getNextInteraction(), 7);
        assertEquals(7, brute.getDamageTaken());
    }

    /**
     * The lifted 4-arg overload's real payoff: ABATEDORES_DE_GIGANTES' rules text covers every
     * Perícia de Ataque, but before the overload moved up to AbstractSkillInteraction only
     * Ataque à Distância could see the target, so a Dwarf swinging a hammer at a giant silently
     * got nothing.
     */
    @Test
    void abatedoresDeGigantesNowAppliesToMeleeToo() {
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        melee.increaseGraduation(3);
        Character dwarf = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Anao())
                .sizeCategory(SizeCategory.MINUS_ONE)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .build();
        CharacterSheet dwarfSheet = CharacterSheet.of(dwarf, new Player());

        // PLUS_THREE vs MINUS_ONE is four categories apart — well past the 2 the ability needs.
        MonsterSheet giant = GenericMonster.ABERRACAO.spawn(new Player());
        MonsterSheet peer = GenericMonster.CAPANGA.spawn(new Player());

        int againstGiant = attackDelivery.resolve(DeliveredAttack.from(giant, DefenseType.PHYSICAL)
                .attacker(dwarfSheet).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).build()).getAttackTotal();
        int againstPeer = attackDelivery.resolve(DeliveredAttack.from(peer, DefenseType.PHYSICAL)
                .attacker(CharacterSheet.of(dwarf, new Player()))
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).build()).getAttackTotal();

        assertEquals(Skill.ADVANTAGE_BONUS, againstGiant - againstPeer);
    }
}
