package org.aventyrs.core.magic;

/**
 * One ramificação of an {@link SpellTree} — the named path a Conjurador commits to when their
 * Árvore de Magia diverges. A tree that diverges declares its two branches as its own enum
 * implementing this, the same per-family shape {@code
 * org.aventyrs.core.title.santo.SantoSpecialization} has for a Título's two Especializações.
 *
 * <p>Deliberately <b>not</b> one shared {@code PRIMEIRA}/{@code SEGUNDA} enum reused by every
 * tree: a ramificação is a named thing a character sheet should be able to print, and two trees'
 * branches are no more interchangeable than two Títulos' Especializações are.
 *
 * <p>A Magia on the <b>trunk</b> — before the divergence, or after the branches converge again —
 * belongs to no branch at all and reports {@link java.util.Optional#empty()} from {@link
 * Spell#getBranch()}. That is the whole convergence mechanism; see {@link SpellTree}.
 */
public interface SpellBranch {

    /** This ramificação's own name, matching its rules-text header exactly. */
    String getName();

    /** The Árvore de Magia this ramificação belongs to. */
    SpellTree getTree();
}
