package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellTreeTest {

    /** An arbitrarily-branched tree, to exercise the authoring guard's rejections. */
    private static SpellTree treeWith(final int branchCount) {
        return new SpellTree() {
            @Override
            public String getName() {
                return "Malformed";
            }

            @Override
            public MagicType getMagicType() {
                return MagicType.NATURAL;
            }

            @Override
            public List<SpellBranch> getBranches() {
                return java.util.Collections.nCopies(branchCount, TestSpellBranch.BRANCH_A);
            }
        };
    }

    @Test
    void aTreeWithTwoRamificacoesIsValid() {
        assertDoesNotThrow(() -> SpellTree.validateBranches(TestSpellTree.DIVERGING));
    }

    @Test
    void aTreeThatNeverDivergesIsValid() {
        assertDoesNotThrow(() -> SpellTree.validateBranches(TestSpellTree.LINEAR));
    }

    @Test
    void aDivergenceIntoASinglePathIsRejected() {
        assertThrows(IllegalOperationException.class, () -> SpellTree.validateBranches(treeWith(1)));
    }

    @Test
    void moreThanTwoRamificacoesIsRejected() {
        assertThrows(IllegalOperationException.class, () -> SpellTree.validateBranches(treeWith(3)));
    }

    @Test
    void divergesIntoBranchesReportsWhetherTheTreeSplits() {
        assertTrue(TestSpellTree.DIVERGING.divergesIntoBranches());
        assertFalse(TestSpellTree.LINEAR.divergesIntoBranches());
    }

    @Test
    void aBranchNamesTheTreeItBelongsTo() {
        assertTrue(TestSpellTree.DIVERGING.getBranches().stream()
                .allMatch(branch -> branch.getTree() == TestSpellTree.DIVERGING));
    }
}
