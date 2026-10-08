package org.aventyrs.core.character.services;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Libertar-se da Predação, for a devourer that is not a Bocarra (core 0.1.5). */
class DevouredEscapeTest {

    private final DevourService devourService = new DevourServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet hero() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
    }

    private static CharacterSheet swallowedBy(final MonsterSheet devourer) {
        CharacterSheet victim = hero();
        victim.applyCondition(new Condition(ConditionType.DEVORADO, null, devourer));
        devourer.addDevouredVictim(victim);
        return victim;
    }

    @Test
    void theEscapeGdIsTheDevourersDefesaTwoNiveisEasierButNeverBelowMedio() {
        MonsterSheet devourer = GenericMonster.BRUTAMONTES.spawn(new Player());
        DifficultyLevel reached = DifficultyLevel.reachedBy(devourer.getPhysicalDefense())
                .orElse(DifficultyLevel.VERY_EASY).easier(2);
        DifficultyLevel expected = reached.compareTo(DifficultyLevel.MEDIUM) < 0 ? DifficultyLevel.MEDIUM : reached;

        assertEquals(expected, devourService.getEscapeDifficulty(devourer));
        assertTrue(devourService.getEscapeDifficulty(devourer).compareTo(DifficultyLevel.MEDIUM) >= 0);
    }

    @Test
    void theEscapeAttackIsJudgedAgainstTheEscapeGd() {
        MonsterSheet devourer = GenericMonster.BRUTAMONTES.spawn(new Player());
        CharacterSheet victim = swallowedBy(devourer);

        var result = devourService.escapeAttack(victim, null, new SkillRoll(List.of(3, 3, 3)), null);

        assertEquals(devourService.getEscapeDifficulty(devourer).getBaseValue(), result.required());
    }

    @Test
    void aSingleHitOfTwiceTheDevourersVigorFreesTheVictimAbaladoAndDesprevenido() {
        MonsterSheet devourer = GenericMonster.BRUTAMONTES.spawn(new Player());
        CharacterSheet victim = swallowedBy(devourer);
        int threshold = devourService.getEscapeDamage(devourer.getCharacter());

        assertFalse(devourService.recordEscapeHit(victim, threshold - 1));
        assertTrue(victim.hasCondition(ConditionType.DEVORADO, null), "hits do not add up outside a Bocarra");

        assertTrue(devourService.recordEscapeHit(victim, threshold));
        assertFalse(victim.hasCondition(ConditionType.DEVORADO, null));
        assertTrue(devourer.getDevouredVictims().isEmpty());
        assertTrue(victim.hasCondition(ConditionType.ABALADO, null));
        assertTrue(victim.hasCondition(ConditionType.DESPREVENIDO, null));
        victim.tickTemporaryEffects();
        assertFalse(victim.hasCondition(ConditionType.DESPREVENIDO, null), "for 1 Rodada");
    }

    @Test
    void aDevouredCharacterCannotBeReachedFromOutside() {
        MonsterSheet devourer = GenericMonster.BRUTAMONTES.spawn(new Player());
        MonsterSheet outsider = GenericMonster.BRUTAMONTES.spawn(new Player());
        CharacterSheet victim = swallowedBy(devourer);

        assertTrue(victim.isShieldedFrom(outsider));
        assertFalse(victim.isShieldedFrom(devourer));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> new AttackReceiver().resolve(IncomingAttack.builder()
                        .defender(victim)
                        .attacker(outsider)
                        .difficultyLevel(DifficultyLevel.MEDIUM)
                        .defenseType(DefenseType.PHYSICAL)
                        .defenseRoll(new SkillRoll(List.of(3, 3, 3)))
                        .build()));
        assertEquals(TranslatableMessages.TARGET_INSIDE_DEVOURER, refused.getMessage());
    }

    /** "Danos causados ao interior da criatura são reduzidos à Metade." */
    @Test
    void damageDealtFromInsideIsHalved() {
        DamageService damageService = new DamageServiceImpl();
        MonsterSheet devourer = GenericMonster.BRUTAMONTES.spawn(new Player());
        CharacterSheet victim = swallowedBy(devourer);

        int fromOutside = damageService.calculateFinalDamage(devourer, null, DamageType.MAGICO, hero(), 20, true);
        int fromInside = damageService.calculateFinalDamage(devourer, null, DamageType.MAGICO, victim, 20, true);

        assertEquals(fromOutside / 2, fromInside);
    }
}
