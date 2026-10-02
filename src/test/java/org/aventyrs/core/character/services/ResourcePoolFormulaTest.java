package org.aventyrs.core.character.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.effect.SpellEffectContext;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.PolimorfismoSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PV, PM and PD follow one rule (core 0.1.0): the Atributo and the Multiplicador are each read through the sheet, so a
 * timed bonus moves every pool the same way, and {@link CombatantStatsService#snapshot} is what notices it.
 */
class ResourcePoolFormulaTest {

    private final HitPointsService hitPoints = new HitPointsServiceImpl();
    private final MagicPointsService magicPoints = new MagicPointsServiceImpl();
    private final DeterminationPointsService determinationPoints = new DeterminationPointsServiceImpl();
    private final CombatantStatsService stats = new CombatantStatsService();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Vigor, Foco and Instinto 3. */
    private CharacterSheet sheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.ZERO)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(3).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static void grant(final CharacterSheet sheet, final ModifierType type, final int value) {
        sheet.grantBlessing(new Blessing(type, value, 3, TargetScope.SELF, "test-" + type));
    }

    @Test
    void aTimedFocoBonusRaisesMaxPmAsATimedVigorBonusRaisesMaxPv() {
        CharacterSheet sheet = sheet();
        Character character = sheet.getCharacter();
        int pm = magicPoints.getMaxMagicPoints(character, sheet);
        int pv = hitPoints.getMaxHitPoints(character, sheet);

        grant(sheet, ModifierType.FOCUS_BONUS, 2);
        grant(sheet, ModifierType.VIGOR_BONUS, 2);

        assertEquals(pm + 2 * magicPoints.getManaMultiplier(character, sheet),
                magicPoints.getMaxMagicPoints(character, sheet));
        assertEquals(pv + 2 * hitPoints.getLifeMultiplier(character, sheet),
                hitPoints.getMaxHitPoints(character, sheet));
        assertEquals(pm, magicPoints.getMaxMagicPoints(character), "no sheet, no timed bonus");
    }

    @Test
    void aTimedInstintoBonusRaisesMaxPd() {
        CharacterSheet sheet = sheet();
        Character character = sheet.getCharacter();
        int pd = determinationPoints.getMaxDeterminationPoints(character, sheet);

        grant(sheet, ModifierType.INSTINCT_BONUS, 1);

        assertEquals(pd + determinationPoints.getDeterminationMultiplier(character, sheet),
                determinationPoints.getMaxDeterminationPoints(character, sheet));
    }

    @Test
    void aTimedLossNeverTakesAMultiplierBelowOne() {
        CharacterSheet sheet = sheet();
        grant(sheet, ModifierType.MANA_MULTIPLIER, -20);
        grant(sheet, ModifierType.LIFE_MULTIPLIER, -20);
        grant(sheet, ModifierType.DETERMINATION_MULTIPLIER, -20);

        assertEquals(1, magicPoints.getManaMultiplier(sheet.getCharacter(), sheet));
        assertEquals(1, hitPoints.getLifeMultiplier(sheet.getCharacter(), sheet));
        assertEquals(1, determinationPoints.getDeterminationMultiplier(sheet.getCharacter(), sheet));
    }

    /** Damage stays fixed while the maximum moves (table ruling, 2026-10-02): current rises with it, then falls back. */
    @Test
    void currentPointsFollowAMaximumThatMovesAndFallBackWhenItLapses() {
        CharacterSheet sheet = sheet();
        Character character = sheet.getCharacter();
        sheet.applyDamage(5);
        int current = hitPoints.getCurrentHitPoints(character, sheet);

        SpellEffect titanecer = new SpellCastingServiceImpl()
                .resolveEffect(PolimorfismoSpell.TITANECER, SpellEffectContext.FRIENDLY).orElseThrow();
        sheet.receiveInteraction(titanecer);
        int grown = hitPoints.getCurrentHitPoints(character, sheet);
        assertEquals(current + 2 * sheet.getAttributeTotal(AttributeDomain.VIGOR), grown, "+2 multiplier × Vigor");
        assertEquals(5, sheet.getDamageTaken());

        sheet.finishTurn();
        sheet.finishTurn();
        sheet.finishTurn();
        assertEquals(current, hitPoints.getCurrentHitPoints(character, sheet));
    }

    @Test
    void aSnapshotChangesWhenATimedEffectTakesHoldAndAgainWhenItLapses() {
        CharacterSheet sheet = sheet();
        CombatantStats before = stats.snapshot(sheet);
        assertEquals(before, stats.snapshot(sheet), "nothing moved, nothing differs");

        sheet.receiveInteraction(new SpellCastingServiceImpl()
                .resolveEffect(PolimorfismoSpell.TITANECER, SpellEffectContext.FRIENDLY).orElseThrow());
        CombatantStats grown = stats.snapshot(sheet);

        assertNotEquals(before, grown);
        assertTrue(grown.poolsDifferFrom(before));
        assertTrue(grown.sizeDiffersFrom(before));
        assertEquals(SizeCategory.PLUS_TWO, grown.sizeCategory());
        assertEquals(before.attributes().get(AttributeDomain.STRENGTH) + 2,
                grown.attributes().get(AttributeDomain.STRENGTH));

        sheet.finishTurn();
        sheet.finishTurn();
        sheet.finishTurn();
        CombatantStats lapsed = stats.snapshot(sheet);
        assertEquals(before, lapsed);
        assertFalse(lapsed.sizeDiffersFrom(before));
    }
}
