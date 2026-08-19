package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void tendenciaDefaultsToOneWhenNotSet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertEquals(1, character.getTendencia());
    }

    @Test
    void sexoIsNullByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getSexo());
    }

    @Test
    void builderAssignsSexoAndTendencia() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sexo(Character.Sexo.FEMININO)
                .tendencia(8)
                .build();

        assertEquals(Character.Sexo.FEMININO, character.getSexo());
        assertEquals(8, character.getTendencia());
    }

    @Test
    void deityIsNullByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getDeity());
    }

    @Test
    void builderAssignsDeity() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .deity(Deity.LUZ_PRIMORDIAL)
                .build();

        assertEquals(Deity.LUZ_PRIMORDIAL, character.getDeity());
    }

    @Test
    void hasNoTitlesByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getPrimaryTitle());
        assertNull(character.getSecondaryTitle());
        assertNull(character.getTertiaryTitle());
        assertTrue(character.getAllTitles().isEmpty());
    }

    @Test
    void grantTitleFillsTheRequestedSlot() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo santo = new Santo(List.of(), List.of());

        character.grantTitle(santo, TitleSlot.SECONDARY);

        assertEquals(santo, character.getSecondaryTitle());
        assertNull(character.getPrimaryTitle());
        assertNull(character.getTertiaryTitle());
    }

    @Test
    void grantTitleOverwritesWhateverAlreadyOccupiedTheSlot() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo first = new Santo(List.of(), List.of());
        Santo second = new Santo(List.of(), List.of());
        character.grantTitle(first, TitleSlot.PRIMARY);

        character.grantTitle(second, TitleSlot.PRIMARY);

        assertEquals(second, character.getPrimaryTitle());
    }

    @Test
    void getAllTitlesListsOnlyTheFilledSlotsPrimaryFirst() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo primary = new Santo(List.of(), List.of());
        Santo tertiary = new Santo(List.of(), List.of());
        character.grantTitle(tertiary, TitleSlot.TERTIARY);
        character.grantTitle(primary, TitleSlot.PRIMARY);

        assertEquals(List.of(primary, tertiary), character.getAllTitles());
    }
}
