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
    SOBRECURA,

    /** Procrastinar Ferimento's "Estancar" — {@code effect.Estancar}. */
    ESTANCAR
}
