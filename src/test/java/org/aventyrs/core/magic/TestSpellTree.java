package org.aventyrs.core.magic;

import java.util.List;

/**
 * Stub {@link SpellTree}s for tests, standing in for the real Árvores de Magia none of which are
 * authored yet.
 *
 * <ul>
 *   <li>{@link #DIVERGING} has the Aliados da Natureza shape — trunk at SEMENTE/BROTO, two
 *       ramificações from MUDA through EMERGENTE, converging back to trunk at FLORESCENTE.</li>
 *   <li>{@link #LINEAR} never diverges, so it has no ramificações and its branch gate can never
 *       reject anything. Its {@link MagicType} differs from {@code DIVERGING}'s deliberately, so
 *       a test can prove a {@code MagiaAlternativaAbility} for the wrong type doesn't exempt.</li>
 * </ul>
 *
 * <p>{@link #getBranches()} builds its list per call rather than holding it in a field: {@link
 * TestSpellBranch} names this enum right back, and a field would make the two enums' static
 * initialisers depend on each other's order.
 */
public enum TestSpellTree implements SpellTree {

    /** Diverges at MUDA into two ramificações and converges again at FLORESCENTE. */
    DIVERGING(MagicType.NATURAL) {
        @Override
        public List<SpellBranch> getBranches() {
            return TestSpellBranch.both();
        }
    },

    /** Runs straight through — the single-path case, with no ramificações at all. */
    LINEAR(MagicType.ELEMENTAL) {
        @Override
        public List<SpellBranch> getBranches() {
            return List.of();
        }
    };

    private final MagicType magicType;

    TestSpellTree(final MagicType magicType) {
        this.magicType = magicType;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public MagicType getMagicType() {
        return magicType;
    }
}
