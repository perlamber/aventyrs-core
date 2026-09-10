package org.aventyrs.core.magic;

import java.util.List;

/**
 * The two ramificações of {@link TestSpellTree#DIVERGING}, standing in for a real tree's own
 * branch enum (Aliados da Natureza's two, say). Lombok is main-source-only in this project, so
 * this is hand-written like {@link TestSpell}.
 */
public enum TestSpellBranch implements SpellBranch {

    BRANCH_A,
    BRANCH_B;

    /** Both ramificações, in declaration order — what {@link TestSpellTree#DIVERGING} reports. */
    static List<SpellBranch> both() {
        return List.of(BRANCH_A, BRANCH_B);
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public SpellTree getTree() {
        return TestSpellTree.DIVERGING;
    }
}
