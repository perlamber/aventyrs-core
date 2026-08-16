package org.aventyrs.core.effect;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DamageInteractionTest {

    private final DamageInteraction damageInteraction = new DamageInteraction();

    private static class DamageReductionAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.VIGOR;
        }

        @Override
        public String getDescription() {
            return "Test-only +3 RD source.";
        }

        @Modifier(ModifierType.DAMAGE_REDUCTION)
        public int bonus() {
            return 3;
        }
    }

    private static class StubInteraction implements Interaction<CharacterSheet> {
        @Override
        public InteractionResult applyTo(final CharacterSheet target) {
            return InteractionResult.builder().build();
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void applyToAppliesTheMitigatedDamageAndReportsIt() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = damageInteraction.applyTo(sheet, 10, false);

        assertEquals(7, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(7, sheet.getDamageTaken());
        assertEquals(character.getStatus(), result.getResultStatus());
    }

    @Test
    void applyToIgnoringDamageReductionSkipsRd() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = damageInteraction.applyTo(sheet, 10, true);

        assertEquals(10, result.getResourceLossValue());
        assertEquals(10, sheet.getDamageTaken());
    }

    @Test
    void oneArgApplyToIsANoOpThatNeverChains() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = damageInteraction.applyTo(sheet);

        assertEquals(0, result.getResourceLossValue());
        assertEquals(0, sheet.getDamageTaken());
        assertNull(result.getNextInteraction());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = sheet.receiveInteraction(damageInteraction);

        assertEquals(0, result.getResourceLossValue());
    }

    @Test
    void applyToForwardsToNextInteractionWhenDamageIsDealt() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        StubInteraction next = new StubInteraction();

        InteractionResult result = damageInteraction.applyTo(sheet, 5, false, next);

        assertEquals(5, result.getResourceLossValue());
        assertSame(next, result.getNextInteraction());
    }

    @Test
    void applyToDoesNotForwardWhenDamageIsFullyMitigated() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        StubInteraction next = new StubInteraction();

        InteractionResult result = damageInteraction.applyTo(sheet, 2, false, next);

        assertEquals(0, result.getResourceLossValue());
        assertNull(result.getNextInteraction());
    }

    private SceneContext combatContext(final int currentRound, final boolean wonInitiative) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, currentRound, wonInitiative);
    }

    @Test
    void applyToWithSceneContextAppliesTheEgoAdvantagesOwnMitigation() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.TORRE_EM_MOVIMENTO)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        // RA(2) subtracted from 10 leaves 8, then halved (initiative won) to 4.
        InteractionResult result = damageInteraction.applyTo(sheet, combatContext(1, true), 10, false);

        assertEquals(4, result.getResourceLossValue());
        assertEquals(4, sheet.getDamageTaken());
    }

    @Test
    void applyToWithNullSceneContextMatchesTheNoContextOverload() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        CharacterSheet withoutContext = CharacterSheet.of(character, new Player());
        CharacterSheet withNullContext = CharacterSheet.of(character, new Player());

        InteractionResult resultWithoutContext = damageInteraction.applyTo(withoutContext, 10, false);
        InteractionResult resultWithNullContext = damageInteraction.applyTo(withNullContext, null, 10, false);

        assertEquals(resultWithoutContext.getResourceLossValue(), resultWithNullContext.getResourceLossValue());
    }

    @Test
    void fiveArgApplyToForwardsToNextInteractionWhenDamageIsDealt() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        StubInteraction next = new StubInteraction();

        InteractionResult result = damageInteraction.applyTo(sheet, combatContext(1, false), 5, false, next);

        assertEquals(5, result.getResourceLossValue());
        assertSame(next, result.getNextInteraction());
    }
}
