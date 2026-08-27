package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.Artes;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConhecimentosCompetencyAbilityTest {

    private Character characterTrainedIn(final Skill... skills) {
        Map<SkillType, CharacterSkill> trained = Stream.of(skills)
                .collect(Collectors.toMap(Skill::getSkillType, skill -> CharacterSkill.builder().skill(skill).build()));
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .skills(trained)
                .build();
    }

    @Test
    void everyAbilityBelongsToConhecimentos() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertEquals(SkillType.CONHECIMENTOS, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, ConhecimentosCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyOProfessorGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            int expected = ability == ConhecimentosCompetencyAbility.O_PROFESSOR ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    @Test
    void generalistaResolvesPendingChoicesForEveryKnownSkillRegardlessOfDomain() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO, SkillType.ATLETISMO),
                Set.copyOf(ConhecimentosCompetencyAbility.GENERALISTA.resolvePendingSpecializationChoices(character)));
    }

    @Test
    void generalistaResolvesNothingForAnUntrainedCharacter() {
        Character character = characterTrainedIn();

        assertTrue(ConhecimentosCompetencyAbility.GENERALISTA.resolvePendingSpecializationChoices(character).isEmpty());
    }

    @Test
    void noOtherAbilityResolvesPendingSpecializationChoices() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            if (ability == ConhecimentosCompetencyAbility.GENERALISTA) {
                continue;
            }
            assertTrue(ability.resolvePendingSpecializationChoices(character).isEmpty());
        }
    }
}
