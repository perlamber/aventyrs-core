package org.aventyrs.core.magic.catalog;

/**
 * What a ramificação <em>is for</em>. The source document never gives a single Árvore's branches
 * names, but it does state what the two of them always are ({@code docs/rules/magias.txt} L30):
 *
 * <blockquote>
 * "Muitas Árvores de Magias se dividem em Ramos de Especialização. <b>Um deles aprofunda o efeito
 * principal da magia, enquanto outro foca na evolução dos Efeitos Alternativos.</b> Durante a
 * progressão do personagem, é necessário escolher um dos Ramos de Especialização, sem
 * possibilidade de adquirir magias do outro ramo."
 * </blockquote>
 *
 * So a ramificação's identity is a <b>role</b>, and it is the same pair of roles in all twelve
 * diverging trees. Naming them anything else would mean inventing 24 names the document does not
 * contain.
 *
 * <p><b>Which of a rung's two entries holds which role is a reading, not stated text.</b> The
 * document lists the pair in an order but never says the order means anything, so each {@link
 * MagicBranch} constant records on itself which {@code Efeito Alternativo} its branch was traced
 * back to. Where that trace is genuinely ambiguous the constant says so.
 */
public enum BranchRole {

    /** "aprofunda o efeito principal da magia". */
    PRINCIPAL("Ramo Principal"),

    /** "foca na evolução dos Efeitos Alternativos". */
    ALTERNATIVO("Ramo Alternativo");

    private final String displayName;

    BranchRole(final String displayName) {
        this.displayName = displayName;
    }

    /** How this role prints on a character sheet. */
    public String getDisplayName() {
        return displayName;
    }
}
