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
import org.aventyrs.core.scene.ActiveAura;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.FORCED_ATTACK_TARGET_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A provoking Aura ({@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}) as the two attack
 * orchestrators see it: a bound attacker's first attack each Rodada must go to the holder, and
 * its later attacks that Rodada against anyone else take Desvantagem.
 */
class ProvokingAuraAttackTest {

    private final AttackReceiver attackReceiver = new AttackReceiver();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    private Scene scene;
    private CharacterSheet holder;
    private CharacterSheet ally;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
        scene = new Scene();
        holder = hero();
        ally = hero();
        UUID party = UUID.randomUUID();
        scene.addParticipant(holder, 20, party);
        scene.addParticipant(ally, 15, party);
    }

    /** A player character who can both dodge and swing: Destreza/Força 3, Graduação 3 in each. */
    private CharacterSheet hero() {
        CharacterSkill dodge = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        dodge.increaseGraduation(3);
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        melee.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .skill(SkillType.ESQUIVA_E_APARAR, dodge)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** Joins attacker to its own hostile group and binds it to the holder's Aura. */
    private void bind(final CombatantSheet attacker) {
        scene.addParticipant(attacker, 10, UUID.randomUUID());
        scene.addAura(new ActiveAura(holder, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, Range.DISTANCIA_CURTA, 3));
        scene.refreshAura(holder, scene.buildContext(holder, Map.of(attacker, Range.ADJACENTE)));
    }

    private IncomingAttack.IncomingAttackBuilder monsterAttack(final MonsterSheet foe, final CombatantSheet defender) {
        return IncomingAttack.builder()
                .defender(defender)
                .attacker(foe)
                .scene(scene)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(3, 3, 3)));
    }

    private DeliveredAttack.DeliveredAttackBuilder heroAttack(final CombatantSheet attacker, final CombatantSheet defender) {
        return DeliveredAttack.builder()
                .attacker(attacker)
                .defender(defender)
                .scene(scene)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(13)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)));
    }

    @Test
    void aBoundMonstersFirstAttackOnSomeoneElseIsRefused() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        bind(capanga);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> attackReceiver.resolve(monsterAttack(capanga, ally).build()));
        assertEquals(FORCED_ATTACK_TARGET_REQUIRED, refused.getMessage());
    }

    @Test
    void aBoundMonstersFirstAttackOnTheHolderIsUnpenalized() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        bind(capanga);

        IncomingAttackResult result = attackReceiver.resolve(monsterAttack(capanga, holder).build());

        assertEquals(0, result.getAuraPenalty());
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(), result.getRequiredTotal());
    }

    @Test
    void theCallerMayDeclareTheHolderNotAValidTarget() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        bind(capanga);

        IncomingAttackResult result = attackReceiver.resolve(monsterAttack(capanga, ally)
                .forcedTargetUnavailable(true)
                .build());

        assertEquals(0, result.getAuraPenalty());
    }

    @Test
    void afterAttackingTheHolderAMonstersGdAgainstOthersDropsByTwo() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        bind(capanga);
        scene.recordAttack(capanga, holder);

        IncomingAttackResult result = attackReceiver.resolve(monsterAttack(capanga, ally).build());

        assertEquals(Skill.DISADVANTAGE_MALUS, result.getAuraPenalty());
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue() + Skill.DISADVANTAGE_MALUS, result.getRequiredTotal());
    }

    @Test
    void anUnboundOrSceneLessAttackIsUntouched() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        scene.addParticipant(capanga, 10, UUID.randomUUID());
        scene.addAura(new ActiveAura(holder, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, Range.DISTANCIA_CURTA, 3));

        assertEquals(0, attackReceiver.resolve(monsterAttack(capanga, ally).build()).getAuraPenalty());
        bind(capanga);
        assertEquals(0, attackReceiver.resolve(monsterAttack(capanga, ally).scene(null).build()).getAuraPenalty());
        assertEquals(0, attackReceiver.resolve(monsterAttack(capanga, ally).attacker(null).build()).getAuraPenalty());
    }

    @Test
    void aBoundCharactersFirstAttackOnSomeoneElseIsRefused() {
        CharacterSheet rival = hero();
        bind(rival);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> attackDelivery.resolve(heroAttack(rival, ally).build()));
        assertEquals(FORCED_ATTACK_TARGET_REQUIRED, refused.getMessage());
    }

    @Test
    void afterAttackingTheHolderACharactersRollAgainstOthersTakesDesvantagem() {
        CharacterSheet rival = hero();
        bind(rival);
        int unpenalizedTotal = attackDelivery.resolve(heroAttack(rival, holder).build()).getAttackTotal();
        scene.recordAttack(rival, holder);

        DeliveredAttackResult result = attackDelivery.resolve(heroAttack(rival, ally).build());

        assertEquals(Skill.DISADVANTAGE_MALUS, result.getAuraPenalty());
        assertEquals(unpenalizedTotal + Skill.DISADVANTAGE_MALUS, result.getAttackTotal());
    }
}
