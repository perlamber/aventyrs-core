package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;

import java.util.Arrays;
import java.util.List;

/**
 * Every authored Magia — the single place to ask "what Magias exist?", which no per-tree enum can
 * answer on its own. 145 of them, across {@link MagicTree}'s twenty Árvores.
 *
 * <h2>No reflection at all, unlike {@code FeatCatalog}</h2>
 *
 * The Talento catalog has to seal {@code Feat} and read its {@code permits} clause, because
 * nothing else enumerates the trees. Here the trees <em>are</em> an enum, and each one knows its
 * own Magias ({@link SpellTree#getSpells()}), so this class is a flat-map over {@code
 * MagicTree.values()} — compiler-enforced completeness with no {@code getPermittedSubclasses()},
 * no classpath walk and no I/O. {@code Spell} is deliberately left unsealed as a result: a
 * consumer's own Magia is a first-class {@code Spell} that simply never appears here, which is
 * correct, since this is the authored ruleset rather than every {@code Spell} constructible.
 *
 * <p>Wiring a tree's enum into its {@link MagicTree} constant is the one step a new Árvore could
 * silently forget; {@code SpellCatalogTest} pins the per-tree counts against the source document
 * so it cannot.
 */
public final class SpellCatalog {

    /** Every authored Magia, tree by tree in declaration order, shallowest rung first within each. */
    private static final List<Spell> ALL = Arrays.stream(MagicTree.values())
            .flatMap(tree -> tree.getSpells().stream())
            .toList();

    private SpellCatalog() {
    }

    /** Every authored Magia across every Árvore. */
    public static List<Spell> all() {
        return ALL;
    }

    /** Every Magia of one Árvore — empty for a tree whose Magias are not authored. */
    public static List<Spell> in(final SpellTree tree) {
        return tree.getSpells();
    }

    /**
     * Every Magia at one rung, across every Árvore — what a Conjurador who has just raised their
     * cap is choosing between, before {@code Spell#isEligible}'s climb and branch gates narrow it.
     */
    public static List<Spell> at(final BranchLevel branchLevel) {
        return ALL.stream().filter(spell -> spell.getBranchLevel() == branchLevel).toList();
    }

    /**
     * Every Magia whose Árvore carries magicType as either half of its category tag — the
     * population a {@code MagiaAlternativaAbility} for that type exempts from the branch gate.
     */
    public static List<Spell> ofType(final MagicType magicType) {
        return ALL.stream().filter(spell -> spell.getTree().hasMagicType(magicType)).toList();
    }
}
