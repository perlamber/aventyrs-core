package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_TREE;

/**
 * An Árvore de Magia — the catalog a Magia belongs to, and the structure {@link
 * Spell#isEligible}'s acquisition gates are expressed against. A Magia's place in its tree is
 * three facts: this tree, its {@link BranchLevel} (how deep), and its {@link SpellBranch} (which
 * ramificação, if the tree has diverged by then).
 *
 * <pre>
 * SEMENTE ── BROTO ─┬─ MUDA(A) ── EMERGENTE(A) ─┬─ FLORESCENTE
 *                   └─ MUDA(B) ── EMERGENTE(B) ─┘
 * \_____ trunk ____/ \____ ramificações ______/  \___ trunk ___/
 * </pre>
 *
 * <h2>Convergence needs no mechanism</h2>
 *
 * A Magia after the branches rejoin simply reports no {@link SpellBranch} — it sits on the trunk,
 * so it is on every path, and {@code isEligible}'s branch gate can never reject it. Nothing
 * models "these two branches converge here"; being branchless <em>is</em> that.
 *
 * <h2>Zero or two ramificações, never one</h2>
 *
 * {@link #getBranches()} is empty for a tree that runs straight through — the single-path case —
 * or holds exactly two for one that diverges. A divergence into one path is meaningless, and
 * three is more than the rules allow ({@code FocusAbility#MAGIA_ALTERNATIVA} says "ambas as
 * ramificações", which only reads for two). {@link #validateBranches} rejects both, so an
 * authoring slip fails loudly rather than quietly disabling the branch gate for that tree.
 *
 * <p>An interface rather than a catalog enum for the same reason {@code
 * org.aventyrs.core.title.AventyrTitle} and {@code org.aventyrs.core.feat.Feat} are: trees are
 * authored per family, and a central enum would have to sit empty until the first one lands.
 */
public interface SpellTree {

    /** How many ramificações a diverging tree has — see the class javadoc. */
    int BRANCH_COUNT = 2;

    /** This tree's own name, e.g. "Aliados da Natureza" — matches its rules-text header exactly. */
    String getName();

    /**
     * The Tipo de Magia this whole tree belongs to. This is what {@code
     * org.aventyrs.core.ability.MagiaAlternativaAbility} matches against: holding that ability
     * for this type is what exempts a Conjurador from the branch gate on this tree.
     */
    MagicType getMagicType();

    /** This tree's ramificações — empty when it never diverges, otherwise exactly {@link #BRANCH_COUNT}. */
    List<SpellBranch> getBranches();

    /** Whether this tree splits at all. A tree that doesn't can never fail the branch gate. */
    default boolean divergesIntoBranches() {
        return !getBranches().isEmpty();
    }

    /**
     * Guards an authored tree's branch count — see the class javadoc for why one and three-plus
     * are both wrong. Throws {@link IllegalOperationException} ({@code INVALID_SPELL_TREE})
     * rather than returning a boolean, since a malformed tree is an authoring mistake, not a
     * value a caller should be deciding what to do about. Called from the branch gate, so no
     * tree can reach an acquisition decision unvalidated.
     */
    static void validateBranches(final SpellTree tree) {
        int count = tree.getBranches().size();
        if (count != 0 && count != BRANCH_COUNT) {
            throw new IllegalOperationException(INVALID_SPELL_TREE);
        }
    }
}
