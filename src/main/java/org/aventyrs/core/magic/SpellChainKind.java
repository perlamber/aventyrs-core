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
    ESPREMER(SpellBodyChange.builder().sizeCategoryShift(-1).build()),

    /** Murcha-Corpo's "Murcha-Almas: Em substituição ao efeito anterior o alvo sofre Redutor de -2 em Força e Destreza" (core 0.1.0). */
    MURCHA_ALMAS(SpellBodyChange.builder().attributeChange(-2).build()),

    /**
     * Infla-Músculos's "Inflar o Ego: Em substituição ao efeito anterior o alvo recebe Bônus de +2 em Força ou
     * Destreza" (core 0.1.0) — the Atributo the caster picked for the {@code Efeito:}, raised to +2.
     */
    INFLAR_O_EGO(SpellBodyChange.builder().attributeChange(2).attributeChoice(true).build()),

    /** Dracônecer's wings, flight and scales — {@code effect.Draconato} (core 0.1.0). */
    DRACONATO(null),

    /** Enfadecer's "perde sua RD e RM, e efeitos de Cura … reduzidos à metade" — {@code effect.BonecaDePorcelana} (core 0.1.0). */
    BONECA_DE_PORCELANA(null),

    /**
     * Serra-Pernas's Corrente de Efeitos Alternativa — a Desvantagem on Força/Destreza rolls next Turn and Amaldiçoado
     * for the Duração, {@code effect.FraquezaMomentanea} (core 0.1.0).
     */
    FRAQUEZA_MOMENTANEA(null);

    private final SpellBodyChange bodyChange;

    SpellChainKind(final SpellBodyChange bodyChange) {
        this.bodyChange = bodyChange;
    }

    /** What this Corrente does to the target's body — empty for one that does something else. */
    public java.util.Optional<SpellBodyChange> getBodyChange() {
        return java.util.Optional.ofNullable(bodyChange);
    }
}
