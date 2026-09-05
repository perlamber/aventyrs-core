package org.aventyrs.core.magic;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.MagiaAlternativaAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three acquisition gates on {@link Spell#isEligible}, each exercised in isolation and in
 * both directions. Every character here is built with whatever the gate under test needs and
 * nothing else, so a failure names which gate broke.
 */
class SpellEligibilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static final Spell TRUNK_SEMENTE =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.SEMENTE, null);
    private static final Spell TRUNK_BROTO =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.BROTO, null);
    private static final Spell MUDA_A =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.MUDA, TestSpellBranch.BRANCH_A);
    private static final Spell MUDA_B =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.MUDA, TestSpellBranch.BRANCH_B);
    private static final Spell EMERGENTE_A =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.EMERGENTE, TestSpellBranch.BRANCH_A);
    private static final Spell EMERGENTE_B =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.EMERGENTE, TestSpellBranch.BRANCH_B);
    /** The convergence: back on the trunk, so it belongs to neither ramificação. */
    private static final Spell TRUNK_FLORESCENTE =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.FLORESCENTE, null);
    private static final Spell OTHER_TREE_BROTO =
            TestSpell.at(TestSpellTree.LINEAR, BranchLevel.BROTO, null);

    private static Character holding(final Spell... spells) {
        return holding(List.of(), spells);
    }

    private static Character holding(final List<AttributeAbility> abilities, final Spell... spells) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .spells(new ArrayList<>(Arrays.asList(spells)))
                .attributeAbilities(abilities)
                .build();
    }

    // ---------- Gate 1: the general BranchLevel cap ----------

    @Test
    void aSpellDeeperThanTheCapIsRefused() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertFalse(EMERGENTE_A.isEligible(character, BranchLevel.MUDA));
    }

    @Test
    void aSpellAtTheCapIsAllowed() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertTrue(EMERGENTE_A.isEligible(character, BranchLevel.EMERGENTE));
    }

    @Test
    void aSpellShallowerThanTheCapIsAllowed() {
        Character character = holding(TRUNK_SEMENTE);

        assertTrue(TRUNK_BROTO.isEligible(character, BranchLevel.FLORESCENTE));
    }

    // ---------- Gate 2: the in-tree climb ----------

    @Test
    void aSementeSpellNeedsNoFoothold() {
        assertTrue(TRUNK_SEMENTE.isEligible(holding(), BranchLevel.SEMENTE));
    }

    @Test
    void aMudaSpellIsRefusedWithNoFootholdAtAll() {
        assertFalse(MUDA_A.isEligible(holding(), BranchLevel.FLORESCENTE));
    }

    @Test
    void aMudaSpellIsRefusedHoldingOnlyTheSementeRung() {
        Character character = holding(TRUNK_SEMENTE);

        assertFalse(MUDA_A.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void aMudaSpellIsAllowedHoldingTheBrotoRungOfTheSameTree() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO);

        assertTrue(MUDA_A.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void aFootholdInADifferentTreeDoesNotCount() {
        Character character = holding(OTHER_TREE_BROTO);

        assertFalse(MUDA_A.isEligible(character, BranchLevel.FLORESCENTE));
    }

    // ---------- Gate 3: the branch lock ----------

    @Test
    void theOppositeRamificacaoIsRefusedOnceOneIsCommittedTo() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertFalse(MUDA_B.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void theCommittedRamificacaoKeepsGoing() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertTrue(EMERGENTE_A.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void eitherRamificacaoIsOpenBeforeOneIsCommittedTo() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO);

        assertTrue(MUDA_A.isEligible(character, BranchLevel.FLORESCENTE));
        assertTrue(MUDA_B.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void magiaAlternativaForThisTreesTypeUnlocksTheOppositeRamificacao() {
        Character character = holding(
                List.of(MagiaAlternativaAbility.NATURAL),
                TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertTrue(MUDA_B.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void magiaAlternativaForAnotherTypeDoesNotUnlockIt() {
        Character character = holding(
                List.of(MagiaAlternativaAbility.ELEMENTAL),
                TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertFalse(MUDA_B.isEligible(character, BranchLevel.FLORESCENTE));
    }

    @Test
    void magiaAlternativaLoosensNeitherTheCapNorTheClimb() {
        Character character = holding(List.of(MagiaAlternativaAbility.NATURAL), TRUNK_SEMENTE);

        assertFalse(MUDA_A.isEligible(character, BranchLevel.MUDA), "climb still applies");
        assertFalse(EMERGENTE_A.isEligible(character, BranchLevel.BROTO), "cap still applies");
    }

    @Test
    void aTreeThatNeverDivergesCanNeverFailTheBranchGate() {
        Character character = holding(TestSpell.at(TestSpellTree.LINEAR, BranchLevel.SEMENTE, null));

        assertTrue(OTHER_TREE_BROTO.isEligible(character, BranchLevel.FLORESCENTE));
    }

    // ---------- Convergence ----------

    @Test
    void theTrunkFlorescenteSpellIsReachableFromEitherRamificacao() {
        Character viaBranchA = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A, EMERGENTE_A);
        Character viaBranchB = holding(TRUNK_SEMENTE, TRUNK_BROTO, MUDA_B, EMERGENTE_B);

        assertTrue(TRUNK_FLORESCENTE.isEligible(viaBranchA, BranchLevel.FLORESCENTE));
        assertTrue(TRUNK_FLORESCENTE.isEligible(viaBranchB, BranchLevel.FLORESCENTE));
    }

    @Test
    void aTrunkSpellCommitsTheConjuradorToNeitherRamificacao() {
        Character character = holding(TRUNK_SEMENTE, TRUNK_BROTO);

        assertTrue(MUDA_A.isEligible(character, BranchLevel.FLORESCENTE));
        assertTrue(MUDA_B.isEligible(character, BranchLevel.FLORESCENTE));
    }
}
