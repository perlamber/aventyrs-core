package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.CharismaAbility;
import org.aventyrs.core.ability.ConcentracaoProfundaActiveAbility;
import org.aventyrs.core.ability.FocusAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.artes.ArtesSpecialization;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeAbilityServiceTest {

    private final AttributeAbilityService abilityService = new AttributeAbilityServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private Character characterWithCharismaBase(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(base).build())
                        .build())
                .build();
    }

    private Character characterWithFocusBase(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(base).build())
                        .build())
                .build();
    }

    @Test
    void noSlotsUnlockedBelowBaseThree() {
        assertEquals(0, abilityService.getUnlockedAbilitySlots(2));
    }

    @Test
    void oneSlotUnlockedFromBaseThree() {
        assertEquals(1, abilityService.getUnlockedAbilitySlots(3));
        assertEquals(1, abilityService.getUnlockedAbilitySlots(4));
    }

    @Test
    void twoSlotsUnlockedFromBaseFive() {
        assertEquals(2, abilityService.getUnlockedAbilitySlots(5));
        assertEquals(2, abilityService.getUnlockedAbilitySlots(6));
    }

    @Test
    void allowsFirstChoiceAtBaseThree() {
        assertDoesNotThrow(() -> abilityService.validateChoice(3, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoiceWithNoUnlockedSlot() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(2, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoosingTheSameAbilityTwice() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsSecondChoiceUntilBaseFive() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(4, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }

    @Test
    void allowsSecondDistinctChoiceAtBaseFive() {
        assertDoesNotThrow(() -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }

    @Test
    void grantAttributeAbilityAddsItToAttributeAbilities() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO).getCharacter();

        assertTrue(granted.getAttributeAbilities().contains(CharismaAbility.VOZ_DE_OURO));
    }

    @Test
    void grantAttributeAbilityAppliesDestinoFavoravelsPermanentSortePoint() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.DESTINO_FAVORAVEL).getCharacter();

        assertEquals(1, granted.getEgos().getSorte().getVariable());
        assertEquals(0, granted.getEgos().getAutocontrole().getVariable());
        assertEquals(0, granted.getEgos().getRecursos().getVariable());
        assertEquals(0, granted.getEgos().getIniciativa().getVariable());
    }

    @Test
    void grantAttributeAbilityLeavesEgosUnchangedForAnAbilityWithNoPermanentGain() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO).getCharacter();

        assertEquals(character.getEgos().getSorte().getVariable(), granted.getEgos().getSorte().getVariable());
    }

    @Test
    void grantAttributeAbilityRejectsAnAlreadyChosenAbility() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .attributeAbility(CharismaAbility.VOZ_DE_OURO)
                .build();

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO));
    }

    @Test
    void grantAttributeAbilityRejectsWhenNoSlotIsFree() {
        Character character = characterWithCharismaBase(2);

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO));
    }

    @Test
    void grantAttributeAbilityAddsConcentracaoProfundasActiveAbility() {
        Character character = characterWithFocusBase(3);

        Character granted = abilityService.grantAttributeAbility(character, FocusAbility.CONCENTRACAO_PROFUNDA).getCharacter();

        assertEquals(1, granted.getActiveAbilities().size());
        assertTrue(granted.getActiveAbilities().get(0) instanceof ConcentracaoProfundaActiveAbility);
    }

    @Test
    void grantAttributeAbilityLeavesActiveAbilitiesEmptyForAnAbilityWithNoneToGrant() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO).getCharacter();

        assertTrue(granted.getActiveAbilities().isEmpty());
    }

    @Test
    void grantAttributeAbilityForCharmeReturnsPendingChoicesForTrainedCarismaSkillsOnly() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .skills(Map.of(
                        SkillType.ARTES, CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build(),
                        SkillType.PERSUASAO, CharacterSkillFixture.blank(CharacterSkillFixture.PERSUASAO_1).build(),
                        SkillType.ATLETISMO, CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build()))
                .build();

        AttributeAbilityGrantResult result = abilityService.grantAttributeAbility(character, CharismaAbility.CHARME);

        assertEquals(Set.of(SkillType.ARTES, SkillType.PERSUASAO), Set.copyOf(result.getPendingSkillTraitChoices()));
    }

    @Test
    void grantAttributeAbilityReturnsEmptyPendingChoicesForAnAbilityThatDoesntNeedThem() {
        Character character = characterWithCharismaBase(3);

        AttributeAbilityGrantResult result = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO);

        assertTrue(result.getPendingSkillTraitChoices().isEmpty());
    }

    @Test
    void grantSkillTraitChoiceAddsBothTraitsToTheTrainedSkill() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .skills(Map.of(SkillType.ARTES, CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build()))
                .build();

        Character granted = abilityService.grantSkillTraitChoice(character, SkillType.ARTES,
                ArtesCompetencyAbility.DOM_BARDICO, ArtesSpecialization.MUSICA);

        assertTrue(granted.getSkillCompetencyAbilities().contains(ArtesCompetencyAbility.DOM_BARDICO));
        assertTrue(granted.getSkills().get(SkillType.ARTES).getSpecializations().contains(ArtesSpecialization.MUSICA));
    }

    @Test
    void grantSkillTraitChoiceRejectsAnUntrainedSkillType() {
        Character character = characterWithCharismaBase(3);

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantSkillTraitChoice(character, SkillType.ARTES,
                        ArtesCompetencyAbility.DOM_BARDICO, ArtesSpecialization.MUSICA));
    }

    @Test
    void grantSkillTraitChoiceRejectsACompetencyAbilityFromAnotherSkillType() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .skills(Map.of(SkillType.ARTES, CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build()))
                .build();

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantSkillTraitChoice(character, SkillType.ARTES,
                        PersuasaoCompetencyAbility.SEDUTOR, ArtesSpecialization.MUSICA));
    }

    @Test
    void grantSkillTraitChoiceRejectsASpecializationFromAnotherSkillType() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .skills(Map.of(SkillType.ARTES, CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build()))
                .build();

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantSkillTraitChoice(character, SkillType.ARTES,
                        ArtesCompetencyAbility.DOM_BARDICO, PersuasaoSpecialization.NEGOCIACAO));
    }
}
