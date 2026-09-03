package org.aventyrs.core.magic;

import org.aventyrs.core.ability.FocusAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.catalog.IraDeVulcanoSpell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ---------- resolvePrimaryDamage ----------

    private final SpellCastingService damageService = new SpellCastingServiceImpl();

    /** A ranged Magia dealing "<dice>d6 + Metade do Foco" Elemental damage. */
    private static Spell halfFocusSpell(final int diceCount) {
        return new TestSpell() {
            @Override
            public Optional<SpellDamage> getPrimaryDamage() {
                return Optional.of(SpellDamage.halfFocusElemental(diceCount, ElementalType.FOGO));
            }
        };
    }

    private static CharacterSheet casterWithFocus(final int focus, final FocusAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focus).build())
                        .build());
        for (FocusAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void resolvePrimaryDamageIsEmptyForAMagiaThatAuthorsNone() {
        assertTrue(damageService.resolvePrimaryDamage(new TestSpell(), casterWithFocus(6)).isEmpty());
    }

    @Test
    void resolvePrimaryDamageUsesHalfFocusAndReportsTheDiceTypeAndElement() {
        ResolvedSpellDamage resolved = damageService.resolvePrimaryDamage(halfFocusSpell(2), casterWithFocus(7)).orElseThrow();

        assertEquals(3, resolved.deterministicAmount()); // 7 / 2
        assertEquals(2, resolved.diceCount());
        assertEquals(DamageType.ELEMENTAL, resolved.damageType());
        assertEquals(ElementalType.FOGO, resolved.elementalType());
        assertFalse(resolved.focusFullyApplied());
    }

    @Test
    void magiaPoderosaUpgradesTheFirstMagiaOfTheRodadaToFullFocus() {
        ResolvedSpellDamage resolved = damageService
                .resolvePrimaryDamage(halfFocusSpell(0), casterWithFocus(6, FocusAbility.MAGIA_PODEROSA)).orElseThrow();

        assertEquals(6, resolved.deterministicAmount());
        assertTrue(resolved.focusFullyApplied());
    }

    @Test
    void magiaPoderosaDoesNotUpgradeOnceAMagiaHasAlreadyBeenCastThisRodada() {
        CharacterSheet caster = casterWithFocus(6, FocusAbility.MAGIA_PODEROSA);
        caster.recordAction(new CombatantAction(SkillType.ATAQUE_A_DISTANCIA, AttributeDomain.FOCUS,
                new TestSpell(), null, 0, null));

        ResolvedSpellDamage resolved = damageService.resolvePrimaryDamage(halfFocusSpell(0), caster).orElseThrow();

        assertEquals(3, resolved.deterministicAmount()); // back to 6 / 2
        assertFalse(resolved.focusFullyApplied());
    }

    @Test
    void withoutMagiaPoderosaTheFirstMagiaStillOnlyGetsHalfFocus() {
        ResolvedSpellDamage resolved = damageService.resolvePrimaryDamage(halfFocusSpell(0), casterWithFocus(6)).orElseThrow();

        assertEquals(3, resolved.deterministicAmount());
        assertFalse(resolved.focusFullyApplied());
    }

    @Test
    void anAuthoredCatalogMagiaResolvesThroughTheSamePath() {
        ResolvedSpellDamage resolved = damageService
                .resolvePrimaryDamage(IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR, casterWithFocus(8)).orElseThrow();

        assertEquals(4, resolved.deterministicAmount()); // 8 / 2, no dice
        assertEquals(0, resolved.diceCount());
        assertEquals(ElementalType.MAGMA, resolved.elementalType());
    }

    @Test
    void castSpellReportsTheResolvedPrimaryDamage() {
        Scene scene = new Scene();
        CharacterSheet caster = casterWithFocus(6);
        scene.addParticipant(caster, 1);
        CharacterSheet target = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene.addParticipant(target, 0);
        Spell spell = halfFocusSpell(0);

        SpellCastingResult result = damageService.castSpell(SpellCastRequest.builder()
                .caster(caster)
                .spell(spell)
                .scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of(target, Range.DISTANCIA_MEDIA), target))
                .combatantTarget(target)
                .build());

        assertEquals(3, result.getPrimaryDamage().deterministicAmount());
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
