package org.aventyrs.core.magic;

/**
 * The Corrente de Efeitos a Magia carries, as something a cast can build rather than prose (core 0.0.93) — {@link
 * Spell#getEffectChainKind()}. {@code SpellCastingService#resolveEffectChain} builds it once {@code
 * SpellCastingService#isEffectChainTriggered} says the Conjuração cleared the target's margin.
 *
 * <p>Only the Correntes a cast can apply on its own are here; the rest stay {@code effectChainDescription} prose.
 */
public enum SpellChainKind {

    /** "O alvo desta magia adicionalmente recupera +1d6+Metade do Foco PV" — {@code effect.Sobrecura}. */
    SOBRECURA(null),

    /** Procrastinar Ferimento's "Estancar" — {@code effect.Estancar}. */
    ESTANCAR(null),

    /**
     * Ogrificar's "Gigantecer: Em substituição ao efeito anterior o alvo recebe Bônus de +2 em Força e Destreza, a
     * Categoria de Tamanho do alvo aumenta em +1" — {@code effect.BodyChangeChain} (core 0.0.99).
     */
    GIGANTECER(SpellBodyChange.builder().sizeCategoryShift(1).attributeChange(2).build()),

    /**
     * Serra-Pernas's "Espremer: Em adicional aos efeitos anteriores, o alvo desta magia tem sua Categoria de Tamanho
     * reduzida em 1 número" — {@code effect.BodyChangeChain} (core 0.0.99).
     */
    ESPREMER(SpellBodyChange.builder().sizeCategoryShift(-1).build());

    private final SpellBodyChange bodyChange;

    SpellChainKind(final SpellBodyChange bodyChange) {
        this.bodyChange = bodyChange;
    }

    /** What this Corrente does to the target's body — empty for one that does something else. */
    public java.util.Optional<SpellBodyChange> getBodyChange() {
        return java.util.Optional.ofNullable(bodyChange);
    }
}
