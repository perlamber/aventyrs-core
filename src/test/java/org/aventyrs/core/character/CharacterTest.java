package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
