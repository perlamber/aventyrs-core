package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end feature test: a single Character at {@link SizeCategory#PLUS_TWO}, with every
 * attribute/skill left at its untrained default ({@link AttributeValue}'s own default {@code
 * base} of 1, plus {@link Skill#UNTRAINED_PENALTY} since no Perícia is trained here), proving
 * each of {@link AbstractSkillInteraction#sizeCategoryRollBonus}'s three groupings resolves
 * independently — the two Perícias de Ataque get {@link SizeCategory#getAttackAndDamageModifier()},
 * Atenção/Furtividade get {@link SizeCategory#getStealthAndAttentionModifier()}, Esquiva e
 * Aparar gets {@link SizeCategory#getDefenseModifier()} — without leaking into any other
 * Perícia's roll (including one, Persuasão, that isn't affected by size at all).
 */
class SizeCategoryRollBonusFeatureTest {

    private static final int UNTRAINED_BASELINE = 1 + Skill.UNTRAINED_PENALTY;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.PLUS_TWO)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void ataqueADistanciaGetsTheAttackAndDamageModifier() {
        InteractionResult result = new AtaqueADistanciaInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE + SizeCategory.PLUS_TWO.getAttackAndDamageModifier(), result.getSkillRollBonus());
    }

    @Test
    void ataqueCorpoACorpoGetsTheAttackAndDamageModifier() {
        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE + SizeCategory.PLUS_TWO.getAttackAndDamageModifier(), result.getSkillRollBonus());
    }

    @Test
    void attentionGetsTheStealthAndAttentionModifier() {
        InteractionResult result = new AttentionInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE + SizeCategory.PLUS_TWO.getStealthAndAttentionModifier(), result.getSkillRollBonus());
    }

    @Test
    void furtividadeGetsTheStealthAndAttentionModifier() {
        InteractionResult result = new FurtividadeInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE + SizeCategory.PLUS_TWO.getStealthAndAttentionModifier(), result.getSkillRollBonus());
    }

    @Test
    void esquivaEApararGetsTheDefenseModifier() {
        InteractionResult result = new EsquivaEApararInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE + SizeCategory.PLUS_TWO.getDefenseModifier(), result.getSkillRollBonus());
    }

    @Test
    void persuasaoIsUnaffectedBySizeCategory() {
        InteractionResult result = new PersuasaoInteraction().applyTo(sheet());

        assertEquals(UNTRAINED_BASELINE, result.getSkillRollBonus());
    }
}
