package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_NOT_HELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MimetizedSpellCastingServiceImplTest {
    private final MimetizedSpellCastingService service = new MimetizedSpellCastingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void spendsPdForAnOwnedMimetizedSpellOnly() throws IllegalOperationException {
        MimetizedSpell spell = MimetizedSpell.builder().spell(new TestSpell()).determinationPointCost(2).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .mimetizedSpells(new ArrayList<>()).build();
        character.grantMimetizedSpell(spell);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        MimetizedSpellCastingResult result = service.cast(sheet, spell);

        assertEquals(2, sheet.getDeterminationSpent());
        assertEquals(spell, result.getMimetizedSpell());
    }

    @Test
    void refusesAnUnownedMimetizedSpellBeforeChargingPd() {
        MimetizedSpell spell = MimetizedSpell.builder().spell(new TestSpell()).determinationPointCost(2).build();
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> service.cast(sheet, spell));

        assertEquals(MIMETIZED_SPELL_NOT_HELD, exception.getMessage());
        assertEquals(0, sheet.getDeterminationSpent());
    }
}
