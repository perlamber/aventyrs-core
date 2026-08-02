package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DominioDoManaInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SpellCastingServiceImplTest {

    private static class FixedResultInteraction implements Interaction<CharacterSheet> {
        private final InteractionResult result;

        private FixedResultInteraction(final InteractionResult result) {
            this.result = result;
        }

        @Override
        public InteractionResult applyTo(final CharacterSheet target) {
            return result;
        }
    }

    private CharacterSheet sheet;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        sheet = CharacterSheet.of(character, new Player());
    }

    @Test
    void castSpellRollsTheDeliveryInteractionThenDominioDoMana() {
        InteractionResult deliveryResult = InteractionResult.builder().skillRollBonus(3).build();
        InteractionResult dominioDoManaResult = InteractionResult.builder().skillRollBonus(5).build();
        SpellCastingService spellCastingService = new SpellCastingServiceImpl(new FixedResultInteraction(dominioDoManaResult));

        SpellCastingResult result = spellCastingService.castSpell(sheet, new FixedResultInteraction(deliveryResult));

        assertSame(deliveryResult, result.getDeliveryResult());
        assertSame(dominioDoManaResult, result.getDominioDoManaResult());
    }

    @Test
    void defaultConstructorRollsARealDominioDoManaInteraction() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        InteractionResult deliveryResult = InteractionResult.builder().skillRollBonus(3).build();
        int expectedDominioDoManaBonus = new DominioDoManaInteraction().applyTo(sheet).getSkillRollBonus();

        SpellCastingResult result = spellCastingService.castSpell(sheet, new FixedResultInteraction(deliveryResult));

        assertSame(deliveryResult, result.getDeliveryResult());
        assertEquals(expectedDominioDoManaBonus, result.getDominioDoManaResult().getSkillRollBonus());
    }
}
