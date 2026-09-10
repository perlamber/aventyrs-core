package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Perícia roll now knows the Grau de Dificuldade it was made against and reports whether it
 * beat it — {@code InteractionResult#getSucceeded()}. Three states, not two: {@code null} means
 * nobody said what the roll was against, which an ability gated on success must read as "cannot
 * tell" rather than "failed".
 *
 * <p>The comparison mirrors {@code org.aventyrs.core.combat.AttackReceiver#resolve}, which
 * already did this for the combat side — a tie succeeds, and a held {@code difficultyReduction}
 * eases the target by whole níveis before comparing.
 */
class RollOutcomeTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).build(), new Player());
    }

    /** Three 6s — the highest total this ruleset can roll, and never a Falha. */
    private static final List<Integer> MAX_ROLL = List.of(6, 6, 6);
    /** 1+1+2 — the lowest, and a Falha Crítica Menor. */
    private static final List<Integer> MIN_ROLL = List.of(1, 1, 2);

    private InteractionResult roll(final CharacterSheet target, final SkillRoll skillRoll) {
        return new AtletismoInteraction().applyTo(target, null, skillRoll);
    }

    // ---------- the three states ----------

    /** No target stated — "cannot tell", deliberately not "failed". */
    @Test
    void aRollAgainstNothingReportsNoVerdict() {
        InteractionResult result = roll(sheet(), new SkillRoll(MAX_ROLL));

        assertNull(result.getSucceeded());
        assertNull(result.getMargin());
    }

    /** No roll at all — a bonuses-only preview has nothing to judge either. */
    @Test
    void aBonusesOnlyPreviewReportsNoVerdict() {
        InteractionResult result = new AtletismoInteraction().applyTo(sheet());

        assertNull(result.getSucceeded());
        assertNull(result.getMargin());
    }

    @Test
    void beatingTheTargetSucceeds() {
        InteractionResult result = roll(sheet(), SkillRoll.against(MAX_ROLL, DifficultyLevel.EASY));

        assertTrue(result.getSucceeded());
        assertTrue(result.getMargin() > 0);
    }

    @Test
    void fallingShortOfTheTargetFails() {
        InteractionResult result = roll(sheet(), SkillRoll.against(MIN_ROLL, DifficultyLevel.HARD));

        assertFalse(result.getSucceeded());
        assertTrue(result.getMargin() < 0);
    }

    /**
     * The roll's own finished total — dice plus whatever the roller's Perícia bonus works out to.
     * Read back from a preview rather than assumed, so these assertions stay exact whatever a
     * blank Character's bonus happens to be.
     */
    private int totalOf(final CharacterSheet target, final List<Integer> dice) {
        return roll(target, new SkillRoll(dice)).getSkillRollBonus()
                + dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** A total exactly equal to the target beats it — the same reading AttackReceiver takes. */
    @Test
    void aTieSucceedsAndReportsAMarginOfZero() {
        CharacterSheet target = sheet();
        InteractionResult result = roll(target, new SkillRoll(MAX_ROLL, null, totalOf(target, MAX_ROLL)));

        assertTrue(result.getSucceeded());
        assertEquals(0, result.getMargin());
    }

    /** The margin is signed and exact — what a "se exceder a GD em N" clause would read. */
    @Test
    void theMarginIsTheSignedDistanceFromTheTarget() {
        CharacterSheet target = sheet();
        int total = totalOf(target, MAX_ROLL);

        assertEquals(3, roll(target, new SkillRoll(MAX_ROLL, null, total - 3)).getMargin());
        assertEquals(-4, roll(target, new SkillRoll(MAX_ROLL, null, total + 4)).getMargin());
    }

    /** The target is a plain number, so a computed GD with no tier ("GD 10+Vigor") works too. */
    @Test
    void aTargetNeedNotBeADifficultyLevelTier() {
        CharacterSheet target = sheet();
        int total = totalOf(target, MAX_ROLL);

        assertTrue(roll(target, new SkillRoll(MAX_ROLL, null, total - 1)).getSucceeded());
        assertFalse(roll(target, new SkillRoll(MAX_ROLL, null, total + 1)).getSucceeded());
    }

    // ---------- difficultyReduction eases the bar ----------

    /**
     * A Graduação-7 Atletismo unlocks PRODIGIO's -1 nível. That must move the threshold by a
     * whole tier, not by a flat point — which is why the reduction is applied by easing the
     * target rather than subtracting from it.
     */
    @Test
    void aHeldDifficultyReductionEasesTheTargetByAWholeNivel() {
        CharacterSkill atletismo = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismo.increaseGraduation(7);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismo).build();
        CharacterSheet prodigy = CharacterSheet.of(character, new Player());
        assertEquals(1, new AtletismoInteraction().applyTo(prodigy).getDifficultyReduction());

        // Rolling against MEDIUM, the -1 nível drops the real bar to EASY's base value.
        InteractionResult result = new AtletismoInteraction()
                .applyTo(prodigy, null, SkillRoll.against(MIN_ROLL, DifficultyLevel.MEDIUM));

        int bonus = result.getSkillRollBonus();
        int total = bonus + MIN_ROLL.stream().mapToInt(Integer::intValue).sum();
        assertEquals(total - DifficultyLevel.EASY.getBaseValue(), result.getMargin());
    }

    // ---------- automatic success ----------

    private static CharacterSheet perceptive() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .skillCompetencyAbility(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM)
                .build(), new Player());
    }

    /**
     * PERCEPCAO_DE_FOXM: "sempre bem-sucedido, dispensando rolagens… cuja GD seja igual ou
     * inferior à Média". The worst possible roll still succeeds under the ceiling.
     */
    @Test
    void automaticSuccessOverridesARollThatWouldOtherwiseFail() {
        InteractionResult plain = new AttentionInteraction()
                .applyTo(sheet(), null, SkillRoll.against(MIN_ROLL, DifficultyLevel.MEDIUM));
        assertFalse(plain.getSucceeded());

        InteractionResult automatic = new AttentionInteraction()
                .applyTo(perceptive(), null, SkillRoll.against(MIN_ROLL, DifficultyLevel.MEDIUM));

        assertTrue(automatic.getSucceeded());
        assertEquals(0, automatic.getMargin(), "no roll had to be beaten");
    }

    /** Above the stated ceiling it does not fire — the GD cap is the point of the parameter. */
    @Test
    void automaticSuccessDoesNotFireAboveItsStatedGdCeiling() {
        InteractionResult result = new AttentionInteraction()
                .applyTo(perceptive(), null, SkillRoll.against(MIN_ROLL, DifficultyLevel.HARD));

        assertFalse(result.getSucceeded());
    }

    /** Scoped to its own Perícia — it must not make every roll automatic. */
    @Test
    void automaticSuccessDoesNotLeakIntoAnotherPericia() {
        InteractionResult result = new AtletismoInteraction()
                .applyTo(perceptive(), null, SkillRoll.against(MIN_ROLL, DifficultyLevel.MEDIUM));

        assertFalse(result.getSucceeded());
    }

    /** With no target there is no GD to be automatically good enough for. */
    @Test
    void automaticSuccessNeedsAStatedTarget() {
        InteractionResult result = new AttentionInteraction()
                .applyTo(perceptive(), null, new SkillRoll(MIN_ROLL));

        assertNull(result.getSucceeded());
    }

    // ---------- granting on success ----------

    private static CharacterSheet feinter() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .skillCompetencyAbility(PersuasaoCompetencyAbility.FINTAR_APRIMORADO)
                .build(), new Player());
    }

    /**
     * FINTAR_APRIMORADO: "se for bem-sucedido sua próxima rolagem de Perícia de Ataque nesta
     * Rodada recebe Vantagem." Reported, not applied — the caller grants it.
     */
    @Test
    void aWonFintaReportsVantagemOnBothAtaquePericias() {
        InteractionResult result = new PersuasaoInteraction()
                .applyTo(feinter(), null, SkillRoll.against(MAX_ROLL, DifficultyLevel.EASY));

        assertTrue(result.getSucceeded());
        List<Blessing> blessings = result.getBlessings();
        assertEquals(2, blessings.size());
        assertTrue(blessings.stream().allMatch(b -> b.getValue() == Skill.ADVANTAGE_BONUS));
        assertTrue(blessings.stream().allMatch(b -> b.getScope() == TargetScope.SELF));
        assertTrue(blessings.stream().anyMatch(b -> b.getModifierType() == ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS));
        assertTrue(blessings.stream().anyMatch(b -> b.getModifierType() == ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS));
    }

    @Test
    void aLostFintaGrantsNothing() {
        InteractionResult result = new PersuasaoInteraction()
                .applyTo(feinter(), null, SkillRoll.against(MIN_ROLL, DifficultyLevel.VERY_HARD));

        assertFalse(result.getSucceeded());
        assertNull(result.getBlessings());
    }

    /** No stated GD is "cannot tell", so nothing is earned — never treated as a success. */
    @Test
    void aFintaWithNoStatedTargetGrantsNothing() {
        InteractionResult result = new PersuasaoInteraction()
                .applyTo(feinter(), null, new SkillRoll(MAX_ROLL));

        assertNull(result.getSucceeded());
        assertNull(result.getBlessings());
    }

    /** Scoped to Persuasão — winning an unrelated roll earns no Finta. */
    @Test
    void winningAnotherPericiaDoesNotEarnTheFinta() {
        InteractionResult result = new AtletismoInteraction()
                .applyTo(feinter(), null, SkillRoll.against(MAX_ROLL, DifficultyLevel.EASY));

        assertTrue(result.getSucceeded());
        assertNull(result.getBlessings());
    }
}
