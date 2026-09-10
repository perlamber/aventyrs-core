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
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.Artes;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    void dominioDoConhecimentoResolvesTheSameChoicesFromABareTrainedSkillList() {
        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO),
                Set.copyOf(GnoseAbility.DOMINIO_DO_CONHECIMENTO.resolvePendingSkillTraitChoices(
                        Set.of(SkillType.ARTES, SkillType.PERSUASAO))));
    }

    @Test
    void dominioDoConhecimentoCapsItsChoicesAtThree() {
        assertEquals(3, GnoseAbility.DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT);
        assertEquals(GnoseAbility.DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT,
                GnoseAbility.DOMINIO_DO_CONHECIMENTO.resolvePendingSkillTraitChoiceLimit().orElseThrow());
    }

    @Test
    void dominioDoConhecimentoOwesASpecializationAndNothingElse() {
        assertEquals(Set.of(SkillTraitKind.SPECIALIZATION),
                GnoseAbility.DOMINIO_DO_CONHECIMENTO.resolvePendingSkillTraitKinds());
    }

    @Test
    void genialidadeEEsforcoResolvesPendingChoicesForEveryKnownSkillRegardlessOfDomain() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO, SkillType.ATLETISMO),
                Set.copyOf(GnoseAbility.GENIALIDADE_E_ESFORCO.resolvePendingSkillTraitChoices(character)));
    }

    @Test
    void genialidadeEEsforcoCapsItsChoicesAtTwo() {
        assertEquals(2, GnoseAbility.GENIALIDADE_E_ESFORCO_CHOICE_LIMIT);
        assertEquals(GnoseAbility.GENIALIDADE_E_ESFORCO_CHOICE_LIMIT,
                GnoseAbility.GENIALIDADE_E_ESFORCO.resolvePendingSkillTraitChoiceLimit().orElseThrow());
    }

    @Test
    void genialidadeEEsforcoOwesACompetencyAbilityAndNothingElse() {
        assertEquals(Set.of(SkillTraitKind.COMPETENCY_ABILITY),
                GnoseAbility.GENIALIDADE_E_ESFORCO.resolvePendingSkillTraitKinds());
    }

    @Test
    void estabilidadeEmocionalGrantsAPermanentAutocontrolePoint() {
        assertEquals(Optional.of(EgoDomain.AUTOCONTROLE), GnoseAbility.ESTABILIDADE_EMOCIONAL.resolvePermanentEgoGain());
    }

    /** "1 ponto temporário neste Ego" — Autocontrole, and no other domain. */
    @Test
    void estabilidadeEmocionalOwesOneTemporaryAutocontrolePointOnDepletion() {
        assertEquals(GnoseAbility.DEPLETION_TEMPORARY_GRANT,
                GnoseAbility.ESTABILIDADE_EMOCIONAL.resolveEgoDepletionGrant(EgoDomain.AUTOCONTROLE));
        for (EgoDomain domain : EgoDomain.values()) {
            if (domain != EgoDomain.AUTOCONTROLE) {
                assertEquals(0, GnoseAbility.ESTABILIDADE_EMOCIONAL.resolveEgoDepletionGrant(domain));
            }
        }
    }

    @Test
    void noOtherAbilityReactsToEgoDepletion() {
        for (GnoseAbility ability : GnoseAbility.values()) {
            if (ability == GnoseAbility.ESTABILIDADE_EMOCIONAL) {
                continue;
            }
            for (EgoDomain domain : EgoDomain.values()) {
                assertEquals(0, ability.resolveEgoDepletionGrant(domain));
            }
        }
    }

    @Test
    void ratoDeBibliotecaGrantsTrainingInEveryUntrainedSkill() {
        Character character = characterTrainedIn(new Artes(), new Persuasao());

        List<SkillType> untrained = Arrays.stream(SkillType.values())
                .filter(skillType -> skillType != SkillType.ARTES && skillType != SkillType.PERSUASAO)
                .toList();

        assertEquals(Set.copyOf(untrained),
                Set.copyOf(GnoseAbility.RATO_DE_BIBLIOTECA.resolveGrantedSkillTraining(character)));
    }

    @Test
    void ratoDeBibliotecaGrantsNothingForAFullyTrainedCharacter() {
        Character character = characterTrainedIn(Arrays.stream(SkillType.values())
                .map(SkillType::newSkillInstance)
                .toArray(Skill[]::new));

        assertTrue(GnoseAbility.RATO_DE_BIBLIOTECA.resolveGrantedSkillTraining(character).isEmpty());
    }

    @Test
    void noOtherAbilityResolvesPendingSkillTraitChoices() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        for (GnoseAbility ability : GnoseAbility.values()) {
            if (ability == GnoseAbility.DOMINIO_DO_CONHECIMENTO || ability == GnoseAbility.GENIALIDADE_E_ESFORCO) {
                continue;
            }
            assertTrue(ability.resolvePendingSkillTraitChoices(character).isEmpty());
            assertTrue(ability.resolvePendingSkillTraitChoiceLimit().isEmpty());
            assertTrue(ability.resolvePendingSkillTraitKinds().isEmpty());
        }
    }

    @Test
    void noOtherAbilityGrantsAPermanentEgoPointOrSkillTraining() {
        Character character = characterTrainedIn(new Artes(), new Persuasao(), new Atletismo());

        for (GnoseAbility ability : GnoseAbility.values()) {
            if (ability != GnoseAbility.ESTABILIDADE_EMOCIONAL) {
                assertTrue(ability.resolvePermanentEgoGain().isEmpty());
            }
            if (ability != GnoseAbility.RATO_DE_BIBLIOTECA) {
                assertTrue(ability.resolveGrantedSkillTraining(character).isEmpty());
            }
        }
    }
}
