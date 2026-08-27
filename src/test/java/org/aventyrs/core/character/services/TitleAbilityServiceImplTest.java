package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.InstinctAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TitleAbilityServiceImplTest {

    private final TitleAbilityService titleAbilityService = new TitleAbilityServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Suprema with no acquisition prerequisite, isolating the extra-slot/CENTELHA_SUPERIOR
     *  mechanics under test from SantoAbility's own real "Requer..." prerequisites. */
    private static class TestSupremaAbility implements AventyrTitleAbility {
        @Override
        public String getDescription() {
            return "Test-only Suprema with no prerequisite.";
        }

        @Override
        public boolean isSupreme() {
            return true;
        }

        @Override
        public Optional<Class<? extends Interaction>> getInteractionClass() {
            return Optional.empty();
        }
    }

    private Character characterWithCentelhaSuperior() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(InstinctAbility.CENTELHA_SUPERIOR)
                .build();
    }

    private CharacterSheet sheetWithExperience(final Character character, final BigDecimal experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    @Test
    void getAvailableSupremaSlotsIsOneByDefaultWithoutCentelhaSuperior() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        AventyrTitle title = new Santo(List.of(), List.of());

        assertEquals(1, titleAbilityService.getAvailableSupremaSlots(character, title));
    }

    @Test
    void getAvailableSupremaSlotsIsTwoWhenCentelhaSuperiorIsHeldAndUnspent() {
        Character character = characterWithCentelhaSuperior();
        AventyrTitle title = new Santo(List.of(), List.of());

        assertEquals(2, titleAbilityService.getAvailableSupremaSlots(character, title));
    }

    @Test
    void getAvailableSupremaSlotsSubtractsSupremasTheTitleAlreadyHolds() {
        Character character = characterWithCentelhaSuperior();
        AventyrTitle title = new Santo(List.of(), List.of(new TestSupremaAbility()));

        assertEquals(1, titleAbilityService.getAvailableSupremaSlots(character, title));
    }

    @Test
    void getAvailableSupremaSlotsNeverGoesNegative() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        AventyrTitle title = new Santo(List.of(), List.of(new TestSupremaAbility(), new TestSupremaAbility()));

        assertEquals(0, titleAbilityService.getAvailableSupremaSlots(character, title));
    }

    @Test
    void getAvailableSupremaSlotsDropsToOneAcrossEveryTitleOnceTheExtraIsSpentAnywhere() throws IllegalOperationException {
        Character character = characterWithCentelhaSuperior();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);
        // Both titles already hold their base Suprema, so granting a second one to firstTitle
        // must go through CENTELHA_SUPERIOR's own extra-slot gate.
        AventyrTitle firstTitle = new Santo(List.of(), List.of(new TestSupremaAbility()));
        AventyrTitle secondTitle = new Santo(List.of(), List.of(new TestSupremaAbility()));
        character.grantTitle(firstTitle, TitleSlot.PRIMARY);
        character.grantTitle(secondTitle, TitleSlot.SECONDARY);

        titleAbilityService.grantTitleAbility(character, sheet, firstTitle, new TestSupremaAbility());

        assertEquals(0, titleAbilityService.getAvailableSupremaSlots(character, firstTitle));
        assertEquals(0, titleAbilityService.getAvailableSupremaSlots(character, secondTitle));
    }

    @Test
    void grantTitleAbilityRejectsWhenTitleIsNotHeldByTheCharacter() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ZERO);
        AventyrTitle unheldTitle = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertThrows(IllegalOperationException.class,
                () -> titleAbilityService.grantTitleAbility(character, sheet, unheldTitle, SantoAbility.PROTECAO_UNGIDA));
    }

    @Test
    void grantTitleAbilityRejectsAnAbilityWhosePrerequisiteIsntMetYet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ZERO);
        AventyrTitle title = new Santo(List.of(), List.of());
        character.grantTitle(title, TitleSlot.PRIMARY);

        // PROTECAO_UNGIDA requires 1 Especialização; title has none yet.
        assertThrows(IllegalOperationException.class,
                () -> titleAbilityService.grantTitleAbility(character, sheet, title, SantoAbility.PROTECAO_UNGIDA));
    }

    @Test
    void grantTitleAbilityGrantsAPlainHabilidadeForFreeOnceItsPrerequisiteIsMet() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ZERO);
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());
        character.grantTitle(title, TitleSlot.PRIMARY);

        titleAbilityService.grantTitleAbility(character, sheet, title, SantoAbility.PROTECAO_UNGIDA);

        assertEquals(List.of(SantoAbility.PROTECAO_UNGIDA), title.getAbilities());
        assertEquals(BigDecimal.ZERO, sheet.getUnUsedExperience());
    }

    @Test
    void grantTitleAbilityGrantsABaseSupremaForFreeWithNoCentelhaSuperiorRequired() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ZERO);
        AventyrTitle title = new Santo(List.of(), List.of());
        character.grantTitle(title, TitleSlot.PRIMARY);
        TestSupremaAbility suprema = new TestSupremaAbility();

        titleAbilityService.grantTitleAbility(character, sheet, title, suprema);

        assertEquals(List.of(suprema), title.getAbilities());
        assertEquals(BigDecimal.ZERO, sheet.getUnUsedExperience());
    }

    @Test
    void grantTitleAbilityRejectsAnExtraSupremaWhenCentelhaSuperiorIsNotHeld() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);
        AventyrTitle title = new Santo(List.of(), List.of(new TestSupremaAbility()));
        character.grantTitle(title, TitleSlot.PRIMARY);

        assertThrows(IllegalOperationException.class,
                () -> titleAbilityService.grantTitleAbility(character, sheet, title, new TestSupremaAbility()));
    }

    @Test
    void grantTitleAbilityRejectsAnExtraSupremaWhenNotEnoughExperience() {
        Character character = characterWithCentelhaSuperior();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.valueOf(2));
        AventyrTitle title = new Santo(List.of(), List.of(new TestSupremaAbility()));
        character.grantTitle(title, TitleSlot.PRIMARY);

        assertThrows(IllegalOperationException.class,
                () -> titleAbilityService.grantTitleAbility(character, sheet, title, new TestSupremaAbility()));
    }

    @Test
    void grantTitleAbilityRejectsASecondExtraSupremaUseOnADifferentTitle() throws IllegalOperationException {
        Character character = characterWithCentelhaSuperior();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);
        AventyrTitle firstTitle = new Santo(List.of(), List.of(new TestSupremaAbility()));
        AventyrTitle secondTitle = new Santo(List.of(), List.of(new TestSupremaAbility()));
        character.grantTitle(firstTitle, TitleSlot.PRIMARY);
        character.grantTitle(secondTitle, TitleSlot.SECONDARY);
        titleAbilityService.grantTitleAbility(character, sheet, firstTitle, new TestSupremaAbility());

        assertThrows(IllegalOperationException.class,
                () -> titleAbilityService.grantTitleAbility(character, sheet, secondTitle, new TestSupremaAbility()));
    }

    @Test
    void grantTitleAbilityKeepsSpecializationGatedCatalogsScopedIndependently() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ZERO);
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of(SantoAbility.PROTECAO_UNGIDA));
        character.grantTitle(title, TitleSlot.PRIMARY);

        // ORGULHO_ELDURIANO only requires holding the Especialização — the already-held
        // SantoAbility Habilidade belongs to a different catalog and must not interfere.
        titleAbilityService.grantTitleAbility(character, sheet, title, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO);

        assertEquals(List.of(SantoAbility.PROTECAO_UNGIDA, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO), title.getAbilities());
    }

    @Test
    void grantTitleAbilityAddsTheExtraSupremaAndSpendsExperience() throws IllegalOperationException {
        Character character = characterWithCentelhaSuperior();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);
        AventyrTitle title = new Santo(List.of(), List.of(new TestSupremaAbility()));
        character.grantTitle(title, TitleSlot.PRIMARY);
        TestSupremaAbility extraSuprema = new TestSupremaAbility();

        titleAbilityService.grantTitleAbility(character, sheet, title, extraSuprema);

        assertEquals(2, title.getAbilities().size());
        assertEquals(BigDecimal.valueOf(5), sheet.getUnUsedExperience());
    }
}
