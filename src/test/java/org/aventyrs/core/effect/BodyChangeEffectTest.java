package org.aventyrs.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellBodyChange;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.PolimorfismoSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What casting a growing or shrinking Polimorfismo Magia does to its target, driven the way a client lands a cast: the
 * effect {@link SpellCastingService#resolveEffect} builds, with the Corrente {@link
 * SpellCastingService#resolveEffectChain} builds chained on when it fired.
 */
class BodyChangeEffectTest {

    private final SpellCastingService spellCastingService = new SpellCastingServiceImpl();
    private final CharacterSizeService sizeService = new CharacterSizeServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Categoria 0 character with Força 4 and Destreza 3. */
    private CharacterSheet target() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.ZERO)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private InteractionResult land(final CharacterSheet target, final Spell spell, final SpellEffectContext context,
                                   final boolean chainFires) {
        SpellEffect effect = spellCastingService.resolveEffect(spell, context).orElseThrow();
        if (chainFires) {
            spellCastingService.resolveEffectChain(spell, target, DiceRoller.fixed(1))
                    .ifPresent(((AbstractEffect) effect)::chainInto);
        }
        InteractionResult result = target.receiveInteraction(effect);
        while (result.getNextInteraction() != null) {
            result = target.receiveInteraction(result.getNextInteraction());
        }
        return result;
    }

    private InteractionResult land(final CharacterSheet target, final Spell spell) {
        return land(target, spell, SpellEffectContext.FRIENDLY, false);
    }

    private SizeCategory size(final CharacterSheet sheet) {
        return sizeService.getEffectiveSizeCategory(sheet);
    }

    private int strength(final CharacterSheet sheet) {
        return sheet.getTemporaryBonus(ModifierType.STRENGTH_BONUS);
    }

    private int dexterity(final CharacterSheet sheet) {
        return sheet.getTemporaryBonus(ModifierType.DEXTERITY_BONUS);
    }

    @Test
    void titanecerGrowsItsTargetTwoCategoriesAndRaisesStrengthDexterityAndLifeMultiplier() {
        CharacterSheet target = target();
        int lifeMultiplier = hitPointsService.getLifeMultiplier(target.getCharacter(), target);

        InteractionResult result = land(target, PolimorfismoSpell.TITANECER);

        assertEquals(SizeCategory.PLUS_TWO, size(target));
        assertEquals(2, strength(target));
        assertEquals(2, dexterity(target));
        assertEquals(lifeMultiplier + 2, hitPointsService.getLifeMultiplier(target.getCharacter(), target));
        assertEquals(4, result.getBlessings().size(), "size, PV multiplier, Força and Destreza");
    }

    @Test
    void theTargetIsItsOwnSizeAgainOnceTheDuracaoLapses() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.TITANECER);

        target.finishTurn();
        target.finishTurn();
        assertEquals(SizeCategory.PLUS_TWO, size(target), "3 Rodadas — still grown after two");

        target.finishTurn();
        assertEquals(SizeCategory.ZERO, size(target));
        assertEquals(0, strength(target));
    }

    @Test
    void castingItAgainRenewsTheDuracaoRatherThanGrowingFurther() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.TITANECER);
        target.finishTurn();

        land(target, PolimorfismoSpell.TITANECER);

        assertEquals(SizeCategory.PLUS_TWO, size(target));
        assertEquals(2, strength(target));
        target.finishTurn();
        target.finishTurn();
        assertEquals(SizeCategory.PLUS_TWO, size(target), "the second cast's 3 Rodadas, counted from itself");
    }

    @Test
    void draconecerGrantsThreeToStrengthAndDexterityAndTwoCategories() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.DRACONECER);

        assertEquals(SizeCategory.PLUS_TWO, size(target));
        assertEquals(3, strength(target));
        assertEquals(3, dexterity(target));
    }

    @Test
    void toqueDeNanicolinaShrinksTwoCategoriesAndNeverTakesAnAtributoBelowOne() {
        CharacterSheet target = target();
        InteractionResult result = land(target, PolimorfismoSpell.TOQUE_DE_NANICOLINA, SpellEffectContext.HOSTILE,
                false);

        assertEquals(SizeCategory.MINUS_TWO, size(target));
        assertEquals(-3, strength(target), "Força 4 − 3 = 1");
        assertEquals(-2, dexterity(target), "Destreza 3 stops at 1, not 0");
        assertNotNull(result.getBlessings());
    }

    @Test
    void enfadecerSetsStrengthAndDexterityToOneAndShrinksThreeCategoriesForOneRodada() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.ENFADECER, SpellEffectContext.HOSTILE, false);

        assertEquals(SizeCategory.MINUS_THREE, size(target));
        assertEquals(-3, strength(target), "Força 4 reduced to 1");
        assertEquals(-2, dexterity(target), "Destreza 3 reduced to 1");

        target.finishTurn();
        assertEquals(SizeCategory.ZERO, size(target));
    }

    @Test
    void ogrificarRaisesOnlyTheAtributoTheCasterPicked() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.OGRIFICAR,
                SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.DEXTERITY), false);

        assertEquals(0, strength(target));
        assertEquals(2, dexterity(target));
        assertEquals(SizeCategory.ZERO, size(target), "only its Corrente changes size");
    }

    @Test
    void ogrificarWithNoPickMovesNoAtributo() {
        CharacterSheet target = target();
        InteractionResult result = land(target, PolimorfismoSpell.OGRIFICAR);

        assertEquals(0, strength(target));
        assertEquals(0, dexterity(target));
        assertEquals(null, result.getBlessings());
    }

    @Test
    void gigantecerReplacesOgrificarsPickWithBothAtributosAndOneCategory() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.OGRIFICAR,
                SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.STRENGTH), true);

        assertEquals(2, strength(target), "replaced, not doubled");
        assertEquals(2, dexterity(target));
        assertEquals(SizeCategory.PLUS_ONE, size(target));
    }

    @Test
    void serraPernasWeakensAndItsEspremerShrinksOneCategoryOnTop() {
        CharacterSheet weakened = target();
        land(weakened, PolimorfismoSpell.SERRA_PERNAS, SpellEffectContext.HOSTILE, false);
        assertEquals(-2, strength(weakened));
        assertEquals(-2, dexterity(weakened));
        assertEquals(SizeCategory.ZERO, size(weakened));

        CharacterSheet squeezed = target();
        land(squeezed, PolimorfismoSpell.SERRA_PERNAS, SpellEffectContext.HOSTILE, true);
        assertEquals(-2, strength(squeezed), "in addition to, not instead of");
        assertEquals(SizeCategory.MINUS_ONE, size(squeezed));
    }

    @Test
    void aRecastMalusIsJudgedWithoutTheOneItReplaces() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.TOQUE_DE_NANICOLINA, SpellEffectContext.HOSTILE, false);
        land(target, PolimorfismoSpell.TOQUE_DE_NANICOLINA, SpellEffectContext.HOSTILE, false);

        assertEquals(-3, strength(target), "still Força 1, not pinned at its already-reduced total");
    }

    @Test
    void auraDoEncolhimentoShrinksItsTargetsWithoutTouchingTheirAtributos() {
        Spell aura = PolimorfismoSpell.TOQUE_DE_NANICOLINA.getAlternateVersion().orElseThrow();
        assertEquals(2, aura.getMaxAdditionalTargets(), "the caster and up to two adjacent allies");

        CharacterSheet ally = target();
        land(ally, aura);

        assertEquals(SizeCategory.MINUS_TWO, size(ally));
        assertEquals(0, strength(ally));
    }

    @Test
    void aGrowingChangeIsDefensiveAndAShrinkingOneOffensive() {
        assertInstanceOf(DefensiveEffect.class,
                spellCastingService.resolveEffect(PolimorfismoSpell.TITANECER, SpellEffectContext.FRIENDLY)
                        .orElseThrow());
        assertInstanceOf(OffensiveEffect.class,
                spellCastingService.resolveEffect(PolimorfismoSpell.ENFADECER, SpellEffectContext.HOSTILE)
                        .orElseThrow());
    }

    @Test
    void theSixSizeChangingMagiasAuthorABodyChange() {
        for (PolimorfismoSpell spell : new PolimorfismoSpell[]{PolimorfismoSpell.SERRA_PERNAS,
                PolimorfismoSpell.OGRIFICAR, PolimorfismoSpell.TOQUE_DE_NANICOLINA, PolimorfismoSpell.TITANECER,
                PolimorfismoSpell.ENFADECER, PolimorfismoSpell.DRACONECER}) {
            assertTrue(spell.getBodyChange().isPresent(), spell.name());
        }
    }

    @Test
    void aMeaninglessBodyChangeIsRefused() {
        assertThrows(IllegalOperationException.class, () -> SpellBodyChange.builder().build());
        assertThrows(IllegalOperationException.class,
                () -> SpellBodyChange.builder().sizeCategoryShift(2).attributeChange(-1).build());
        assertThrows(IllegalOperationException.class,
                () -> SpellBodyChange.builder().attributesSetTo(1).attributeChange(-1).build());
        assertThrows(IllegalOperationException.class,
                () -> SpellBodyChange.builder().attributeChoice(true).sizeCategoryShift(1).build());
    }
}
