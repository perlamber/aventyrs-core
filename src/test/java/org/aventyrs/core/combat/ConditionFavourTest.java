package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Estados reaching both directions of an exchange (core 0.1.5): a foe's own Condições reach the
 * GD and Defesa it presents, and the outward Favorecido of a Condição reaches whoever attacks its
 * holder — on the attack roll, and on Caído's/Cego's Esquiva e Aparar.
 */
class ConditionFavourTest {

    private final AttackReceiver attackReceiver = new AttackReceiver();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet hero() {
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
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private IncomingAttackResult swingAt(final CharacterSheet hero, final MonsterSheet brute) {
        return attackReceiver.resolve(IncomingAttack.builder()
                .defender(hero)
                .attacker(brute)
                .difficultyLevel(brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level())
                .attackBonus(brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).bonus())
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());
    }

    private DeliveredAttackResult swingAt(final MonsterSheet brute, final CharacterSheet hero) {
        return attackDelivery.resolve(DeliveredAttack.from(brute, DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());
    }

    // ---------- a foe's own Condições ----------

    @Test
    void aDesprevenidoFoePresentsFourLessDefesa() {
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        int before = brute.getPhysicalDefense();

        brute.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));

        assertEquals(before + ConditionType.DESPREVENIDO_DEFENSE_MALUS, brute.getPhysicalDefense());
    }

    @Test
    void aWeakenedFoesAttackGdAndDefesaBothDrop() {
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        int attackBefore = brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).getValue();
        int defenseBefore = brute.getPhysicalDefense();

        brute.applyCondition(new Condition(ConditionType.FRAQUEZA, 1));

        assertEquals(attackBefore + Skill.DISADVANTAGE_MALUS,
                brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).getValue());
        // Fraqueza reaches the defence too — a foe's Defesa stands in for its Esquiva e Aparar.
        assertEquals(defenseBefore + Skill.DISADVANTAGE_MALUS, brute.getPhysicalDefense());
    }

    // ---------- Favorecido em Perícias de Ataque ----------

    @Test
    void attackingAFlankedFoeIsFavorecidoOnTheAttackRoll() {
        CharacterSheet hero = hero();
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        int before = swingAt(brute, hero).getAttackTotal();

        brute.applyCondition(new Condition(ConditionType.FLANQUEADO, 1));

        assertEquals(before + Skill.ADVANTAGE_BONUS, swingAt(brute, hero).getAttackTotal());
    }

    @Test
    void aFoeAttackingACaidoHeroIsFavorecido() {
        CharacterSheet hero = hero();
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        int before = swingAt(hero, brute).getRequiredTotal();

        hero.applyCondition(new Condition(ConditionType.CAIDO, null));

        assertEquals(before + Skill.ADVANTAGE_BONUS, swingAt(hero, brute).getRequiredTotal());
    }

    // ---------- Favorecido em Esquiva e Aparar against a Caído/Cego attacker ----------

    @Test
    void aHeroWhoAttackedTheProneFoeDefendsAgainstItWithVantagem() {
        CharacterSheet hero = hero();
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        Scene scene = new Scene();
        scene.addParticipant(hero, 14, UUID.randomUUID());
        scene.addParticipant(brute, 9, UUID.randomUUID());
        brute.applyCondition(new Condition(ConditionType.CAIDO, null));
        int before = swingAt(hero, brute).getDefenseTotal();

        scene.recordAttack(hero, brute);

        assertEquals(before + Skill.ADVANTAGE_BONUS, swingAt(hero, brute).getDefenseTotal());
    }

    @Test
    void aFoeThatAttackedTheBlindHeroDefendsAgainstItWithVantagem() {
        CharacterSheet hero = hero();
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        hero.applyCondition(new Condition(ConditionType.CEGO, null));
        int before = swingAt(brute, hero).getRequiredTotal();

        hero.noteAttackedBy(brute);

        assertEquals(before + Skill.ADVANTAGE_BONUS, swingAt(brute, hero).getRequiredTotal());
    }
}
