package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellCastingServiceImplTest {

    private static class FixedResultInteraction implements Interaction<CombatantSheet> {
        private final InteractionResult result;

        private FixedResultInteraction(final InteractionResult result) {
            this.result = result;
        }

        @Override
        public InteractionResult applyTo(final CombatantSheet target) {
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

    @Test
    void castRequestRegistersALastingAreaSpellEffectOnTheScene() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        Scene scene = sceneWithCaster();
        Spell spell = areaSpell(SpellDuration.rodadas(2));

        SpellCastingResult result = spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(sheet)
                .spell(spell)
                .scene(scene)
                .sceneContext(scene.buildContext(sheet, Map.of()))
                .positionTarget(new org.aventyrs.core.scene.grid.GridPosition(4, 4))
                .build());

        assertEquals(2, result.getDurationInRounds());
        assertSame(result.getAreaSpellEffect(), scene.getActiveAreaSpellEffects().get(0));
        assertSame(spell, result.getAreaSpellEffect().getSpell());
    }

    @Test
    void concentrationAreaSpellRemainsOnTheSceneUntilItsFutureBreakTransition() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        Scene scene = sceneWithCaster();

        SpellCastingResult result = spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(sheet)
                .spell(areaSpell(SpellDuration.CONCENTRACAO))
                .scene(scene)
                .sceneContext(scene.buildContext(sheet, Map.of()))
                .positionTarget(new org.aventyrs.core.scene.grid.GridPosition(4, 4))
                .build());

        assertEquals(0, result.getDurationInRounds());
        assertEquals(1, scene.getActiveAreaSpellEffects().size());
        assertNull(result.getAreaSpellEffect().getRemainingRounds());
    }

    @Test
    void areaSpellEffectExpiresAfterItsSceneRoundDuration() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        Scene scene = sceneWithCaster();
        spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(sheet)
                .spell(areaSpell(SpellDuration.rodadas(2)))
                .scene(scene)
                .sceneContext(scene.buildContext(sheet, Map.of()))
                .positionTarget(new org.aventyrs.core.scene.grid.GridPosition(4, 4))
                .build());

        scene.next();
        scene.next();
        assertEquals(1, scene.getActiveAreaSpellEffects().size());

        scene.next();
        assertTrue(scene.getActiveAreaSpellEffects().isEmpty());
    }

    @Test
    void requestRejectsATargetPositionForASingleTargetSpell() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        Scene scene = sceneWithCaster();

        assertThrows(org.aventyrs.core.sheet.IllegalOperationException.class,
                () -> spellCastingService.castSpell(SpellCastRequest.builder()
                        .caster(sheet)
                        .spell(new TestSpell())
                        .scene(scene)
                        .sceneContext(scene.buildContext(sheet, Map.of()))
                        .positionTarget(new org.aventyrs.core.scene.grid.GridPosition(4, 4))
                        .build()));
    }

    @Test
    void castRequestSupportsAnInSceneCombatantTargetForASingleTargetSpell() {
        SpellCastingService spellCastingService = new SpellCastingServiceImpl();
        Scene scene = sceneWithCaster();
        CharacterSheet target = CharacterSheet.of(
                CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene.addParticipant(target, 0);

        SpellCastingResult result = spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(sheet)
                .spell(new TestSpell())
                .scene(scene)
                .sceneContext(scene.buildContext(sheet, Map.of(target, Range.DISTANCIA_MEDIA), target))
                .combatantTarget(target)
                .build());

        assertNull(result.getAreaSpellEffect());
    }

    private Scene sceneWithCaster() {
        Scene scene = new Scene();
        scene.addParticipant(sheet, 1);
        return scene;
    }

    private Spell areaSpell(final SpellDuration duration) {
        return new TestSpell() {
            @Override
            public SpellDuration getDuration() {
                return duration;
            }

            @Override
            public SpellTargeting getTargeting() {
                return SpellTargeting.areaDeEfeito(Range.DISTANCIA_CURTA,
                        AreaOfEffect.circle(Range.DISTANCIA_CURTA));
            }
        };
    }
}
