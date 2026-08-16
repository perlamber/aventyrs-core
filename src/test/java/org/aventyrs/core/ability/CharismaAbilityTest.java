package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.Artes;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagem;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharismaAbilityTest {

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
    void everyAbilityBelongsToCharisma() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertEquals(AttributeDomain.CHARISMA, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, CharismaAbility.values().length);
    }

    @Test
    void destinoFavoravelGrantsSorteAndAutocontroleOnMajorCriticalSuccess() {
        assertEquals(List.of(EgoDomain.SORTE, EgoDomain.AUTOCONTROLE),
                CharismaAbility.DESTINO_FAVORAVEL.resolveCriticalSuccessEgoGain(CriticalResult.ACERTO_CRITICO_MAIOR));
    }

    @Test
    void destinoFavoravelGrantsNothingOnAnyOtherCriticalResult() {
        for (CriticalResult criticalResult : CriticalResult.values()) {
            if (criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR) {
                continue;
            }
            assertTrue(CharismaAbility.DESTINO_FAVORAVEL.resolveCriticalSuccessEgoGain(criticalResult).isEmpty());
        }
    }

    @Test
    void noOtherAbilityGrantsAnEgoGainOnMajorCriticalSuccess() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            if (ability == CharismaAbility.DESTINO_FAVORAVEL) {
                continue;
            }
            assertTrue(ability.resolveCriticalSuccessEgoGain(CriticalResult.ACERTO_CRITICO_MAIOR).isEmpty());
        }
    }

    @Test
    void destinoFavoravelPermanentlyGrantsSorte() {
        assertEquals(Optional.of(EgoDomain.SORTE), CharismaAbility.DESTINO_FAVORAVEL.resolvePermanentEgoGain());
    }

    @Test
    void noOtherAbilityGrantsAPermanentEgoPoint() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            if (ability == CharismaAbility.DESTINO_FAVORAVEL) {
                continue;
            }
            assertTrue(ability.resolvePermanentEgoGain().isEmpty());
        }
    }

    @Test
    void charmeResolvesPendingChoicesForTrainedCarismaSkillsOnly() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO),
                Set.copyOf(CharismaAbility.CHARME.resolvePendingSkillTraitChoices(character)));
    }

    @Test
    void charmeResolvesEveryTrainedCarismaSkill() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new EmpatiaSelvagem());

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO, SkillType.EMPATIA_SELVAGEM),
                Set.copyOf(CharismaAbility.CHARME.resolvePendingSkillTraitChoices(character)));
    }

    @Test
    void charmeResolvesNothingForAnUntrainedCharacter() {
        Character character = characterTrainedIn();

        assertTrue(CharismaAbility.CHARME.resolvePendingSkillTraitChoices(character).isEmpty());
    }

    @Test
    void noOtherAbilityResolvesPendingSkillTraitChoices() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new EmpatiaSelvagem());

        for (CharismaAbility ability : CharismaAbility.values()) {
            if (ability == CharismaAbility.CHARME) {
                continue;
            }
            assertTrue(ability.resolvePendingSkillTraitChoices(character).isEmpty());
        }
    }
}
