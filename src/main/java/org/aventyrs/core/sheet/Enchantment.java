package org.aventyrs.core.sheet;

/**
 * Marks a {@link TemporaryEffect} as an <b>Efeito de Encantamento</b> — the classification several
 * unrelated clauses reach for, and the reason it is an interface rather than a base class.
 *
 * <p>The producers have no common supertype: an Aura's forced targeting ({@link ForcedTargeting}),
 * a {@link Condition} some clause inflicts, and a Magia's own {@code MagicType#ENCANTAMENTO} are
 * three different shapes. A marker any {@code TemporaryEffect} can carry keeps them one
 * classification without forcing them into one hierarchy.
 *
 * <p><b>Everything an Encantamento's tag does is resolved by its recipient, never by whoever cast
 * it.</b> {@code CombatantSheet#applyEnchantment} is the single door: it consults the recipient's
 * own immunity and their own Duração modifiers, and the caster simply builds the effect and offers
 * it. That is why immunity is a {@code Race} question and the halving a {@code sheet.Ungido} one —
 * both facts about whoever is being enchanted.
 *
 * <p>An effect implementing this must also extend {@link TemporaryEffect}; {@code applyEnchantment}
 * enforces that pairing in its signature.
 */
public interface Enchantment {

    /**
     * Whoever cast it, or {@code null} for an Encantamento with no attributable caster. Kept
     * because most clauses that read one need to know whose it is — a forced target, or whether
     * the caster is still standing.
     */
    CombatantSheet getEnchanter();

    /**
     * Whether this is an <i>efeito nocivo</i> — what "a Duração de efeitos nocivos de Encantamentos
     * e Maldições são reduzidas pela metade" halves ({@code SantoAbility#PROTECAO_UNGIDA}).
     *
     * <p>Stated by the producer rather than derived: this core has no allegiance model at cast
     * time, so whether an effect harms its recipient is a fact about the clause, not something to
     * infer from who cast it.
     */
    boolean isHarmful();
}
