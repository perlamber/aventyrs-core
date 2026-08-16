package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
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

class GnoseAbilityTest {

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
    void everyAbilityBelongsToGnose() {
        for (GnoseAbility ability : GnoseAbility.values()) {
            assertEquals(AttributeDomain.GNOSE, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (GnoseAbility ability : GnoseAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, GnoseAbility.values().length);
    }

    @Test
    void dominioDoConhecimentoResolvesPendingChoicesForEveryKnownSkillRegardlessOfDomain() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO, SkillType.ATLETISMO),
                Set.copyOf(GnoseAbility.DOMINIO_DO_CONHECIMENTO.resolvePendingSkillTraitChoices(character)));
    }

    @Test
    void dominioDoConhecimentoResolvesNothingForAnUntrainedCharacter() {
        Character character = characterTrainedIn();

        assertTrue(GnoseAbility.DOMINIO_DO_CONHECIMENTO.resolvePendingSkillTraitChoices(character).isEmpty());
    }

    @Test
    void noOtherAbilityResolvesPendingSkillTraitChoices() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        for (GnoseAbility ability : GnoseAbility.values()) {
            if (ability == GnoseAbility.DOMINIO_DO_CONHECIMENTO) {
                continue;
            }
            assertTrue(ability.resolvePendingSkillTraitChoices(character).isEmpty());
        }
    }
}
