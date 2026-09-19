package org.aventyrs.core.effect;

import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.ConditionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two contributors, tested without a casting service anywhere in sight — which is itself half
 * the point of splitting them out of {@code SpellCastingServiceImpl}.
 */
class SpellEffectBuilderTest {

    private final SpellEffectBuilder healing = new HealingEffectBuilder();
    private final SpellEffectBuilder cleansing = new ConditionCleansingEffectBuilder();

    @Test
    void theHealingBuilderAnswersOnlyForAMagiaAuthoringThatColumn() {
        assertTrue(healing.build(VidaSpell.REVIGORAR, SpellEffectContext.FRIENDLY).isPresent());
        assertTrue(healing.build(VidaSpell.EXORCIZAR, SpellEffectContext.FRIENDLY).isEmpty());
        assertTrue(healing.build(new TestSpell(), SpellEffectContext.FRIENDLY).isEmpty());
    }

    @Test
    void theCleansingBuilderAnswersOnlyForAMagiaAuthoringThatColumn() {
        assertTrue(cleansing.build(VidaSpell.EXORCIZAR, SpellEffectContext.FRIENDLY).isPresent());
        assertTrue(cleansing.build(VidaSpell.REVIGORAR, SpellEffectContext.FRIENDLY).isEmpty());
        assertTrue(cleansing.build(new TestSpell(), SpellEffectContext.FRIENDLY).isEmpty());
    }

    @Test
    void theHealingBuilderPassesTheAuthoredColumnAndTheContextThrough() {
        SpellHealingEffect effect = (SpellHealingEffect) healing
                .build(VidaSpell.REVIGORAR_MAIOR, SpellEffectContext.HOSTILE).orElseThrow();

        assertSame(VidaSpell.REVIGORAR_MAIOR, effect.getSpell());
        assertEquals(RestType.TOTAL, effect.getHealing().restEquivalent());
        assertTrue(effect.isHostileTarget());
    }

    @Test
    void theCleansingBuilderPassesTheAuthoredConditionsThrough() {
        ConditionCleansingEffect effect = (ConditionCleansingEffect) cleansing
                .build(VidaSpell.TOQUE_CURATIVO, SpellEffectContext.FRIENDLY).orElseThrow();

        assertEquals(VidaSpell.TOQUE_CURATIVO.getCleansedConditions(), effect.getCleansedConditions());
        assertTrue(effect.getCleansedConditions().contains(ConditionType.DOENTE));
    }

    @Test
    void eachBuilderProducesItsOwnCategory() {
        assertInstanceOf(HealingEffect.class,
                healing.build(VidaSpell.BENCAO_DA_LUZ, SpellEffectContext.FRIENDLY).orElseThrow());
        assertInstanceOf(DefensiveEffect.class,
                cleansing.build(VidaSpell.CORPO_FECHADO, SpellEffectContext.FRIENDLY).orElseThrow());
    }

    @Test
    void theContextIsOnlyReadByABuilderWhoseEffectUsesIt() {
        // Cleansing ignores hostility — lifting a Malefício is the same act either way.
        ConditionCleansingEffect friendly = (ConditionCleansingEffect) cleansing
                .build(VidaSpell.EXORCIZAR, SpellEffectContext.FRIENDLY).orElseThrow();
        ConditionCleansingEffect hostile = (ConditionCleansingEffect) cleansing
                .build(VidaSpell.EXORCIZAR, SpellEffectContext.HOSTILE).orElseThrow();

        assertEquals(friendly.getCleansedConditions(), hostile.getCleansedConditions());
    }

    @Test
    void theContextFactoryMapsToItsTwoConstants() {
        assertSame(SpellEffectContext.HOSTILE, SpellEffectContext.of(true));
        assertSame(SpellEffectContext.FRIENDLY, SpellEffectContext.of(false));
        assertTrue(SpellEffectContext.HOSTILE.hostileTarget());
        assertFalse(SpellEffectContext.FRIENDLY.hostileTarget());
    }
}
