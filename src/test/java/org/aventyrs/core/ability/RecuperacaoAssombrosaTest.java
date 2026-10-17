package org.aventyrs.core.ability;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link VigorAbility#RECUPERACAO_ASSOMBROSA}: a harmful effect landing with more than 3 counted Rodadas lasts 3;
 * an open-ended one, one scoped to a Descanso, and any beneficial one are untouched (table ruling, 2026-10-16).
 */
class RecuperacaoAssombrosaTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final boolean recovering) {
        var builder = CharacterFixture.blank(CharacterFixture.BLANK);
        if (recovering) {
            builder.attributeAbility(VigorAbility.RECUPERACAO_ASSOMBROSA);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void aLongHarmfulEffectEndsInThreeRodadas() {
        CharacterSheet holder = sheet(true);
        Condition silence = new Condition(ConditionType.SILENCIO, 6);
        TemporaryBonus malus = new TemporaryBonus(ModifierType.DEFESAS, -2, 5);
        Bleeding bleed = new Bleeding(2, Optional.of(8));

        holder.applyCondition(silence);
        holder.applyEffect(malus);
        holder.applyEffect(bleed);

        assertEquals(3, silence.getRemainingRounds());
        assertEquals(3, malus.getRemainingRounds());
        assertEquals(3, bleed.getRemainingRounds());
    }

    @Test
    void shortHarmfulBeneficialAndPermanentEffectsAreUntouched() {
        CharacterSheet holder = sheet(true);
        Condition brief = new Condition(ConditionType.SILENCIO, 2);
        TemporaryBonus blessing = new TemporaryBonus(ModifierType.DEFESAS, 2, 6);
        Condition fallen = new Condition(ConditionType.CAIDO, null);
        TemporaryBonus untilRest = new TemporaryBonus(ModifierType.DEFESAS, -2, 10);

        holder.applyCondition(brief);
        holder.applyEffect(blessing);
        holder.applyCondition(fallen);
        holder.applyEffectUntilRest(untilRest, RestType.LONGO);

        assertEquals(2, brief.getRemainingRounds());
        assertEquals(6, blessing.getRemainingRounds());
        assertNull(fallen.getRemainingRounds());
        assertEquals(10, untilRest.getRemainingRounds());
    }

    @Test
    void withoutTheHabilidadeNothingIsCut() {
        CharacterSheet holder = sheet(false);
        Condition silence = new Condition(ConditionType.SILENCIO, 6);

        holder.applyCondition(silence);

        assertEquals(6, silence.getRemainingRounds());
    }

    @Test
    void harmfulnessMirrorsTheStatusPanelSplit() {
        assertTrue(new Condition(ConditionType.RENDIDO, 1).isHarmful());
        assertFalse(new Condition(ConditionType.ESCONDIDO, null).isHarmful());
        assertTrue(TemporaryBonus.isHarmful(ModifierType.DEFESAS, -1));
        assertFalse(TemporaryBonus.isHarmful(ModifierType.DEFESAS, 1));
        assertTrue(TemporaryBonus.isHarmful(ModifierType.SKILL_ROLL_COST, 1));
        assertFalse(TemporaryBonus.isHarmful(ModifierType.SKILL_ROLL_COST, -1));
        assertTrue(new Bleeding(1, Optional.empty()).isHarmful());
    }
}
