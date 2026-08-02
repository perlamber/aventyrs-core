package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AcquiredChoice;
import org.aventyrs.core.ability.GnoseAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.skill.ArtesCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityChoiceServiceImplTest {

    private final AbilityChoiceService abilityChoiceService = new AbilityChoiceServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void returnsEmptyWhenNoChoiceWasRecordedForTheAbility() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        Optional<SkillType> choice = abilityChoiceService.getChoiceFor(character, GnoseAbility.PERITO_TEORICO);

        assertTrue(choice.isEmpty());
    }

    @Test
    void returnsTheRecordedChoiceForTheAbility() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .abilityChoice(AcquiredChoice.of(GnoseAbility.PERITO_TEORICO, SkillType.ATLETISMO))
                .build();

        Optional<SkillType> choice = abilityChoiceService.getChoiceFor(character, GnoseAbility.PERITO_TEORICO);

        assertEquals(Optional.of(SkillType.ATLETISMO), choice);
    }

    @Test
    void distinguishesChoicesBetweenDifferentAbilities() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .abilityChoice(AcquiredChoice.of(GnoseAbility.PERITO_TEORICO, SkillType.ATLETISMO))
                .abilityChoice(AcquiredChoice.of(ArtesCompetencyAbility.APRIMORAR_COM_ARTE, SkillType.ESQUIVA_E_APARAR))
                .build();

        Optional<SkillType> peritoTeoricoChoice = abilityChoiceService.getChoiceFor(character, GnoseAbility.PERITO_TEORICO);
        Optional<SkillType> aprimorarComArteChoice = abilityChoiceService.getChoiceFor(character, ArtesCompetencyAbility.APRIMORAR_COM_ARTE);

        assertEquals(Optional.of(SkillType.ATLETISMO), peritoTeoricoChoice);
        assertEquals(Optional.of(SkillType.ESQUIVA_E_APARAR), aprimorarComArteChoice);
    }

    @Test
    void returnsEmptyForAnUnrelatedAbilityEvenWhenOtherChoicesExist() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .abilityChoice(AcquiredChoice.of(GnoseAbility.PERITO_TEORICO, SkillType.ATLETISMO))
                .build();

        Optional<SkillType> choice = abilityChoiceService.getChoiceFor(character, ArtesCompetencyAbility.APRIMORAR_COM_ARTE);

        assertTrue(choice.isEmpty());
    }
}
