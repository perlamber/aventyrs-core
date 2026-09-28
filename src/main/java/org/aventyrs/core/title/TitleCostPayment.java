package org.aventyrs.core.title;

/**
 * How an activation pays its PD, passed among {@code TitleAbilityActivationRequest#getChoices}.
 * Omitted means {@link #DETERMINATION_POINTS}, the ordinary payment.
 */
public enum TitleCostPayment {

    /** The ordinary payment: PD from the activator's own pool. */
    DETERMINATION_POINTS,

    /**
     * The PD paid in PV instead, locked until a Descanso Verdadeiro ({@code
     * CombatantSheet#payWithVitality}) — only where a held Título permits it ({@link
     * AventyrTitle#permitsHitPointPayment}), Mártir Altruísta's Transferir Vitalidade.
     */
    HIT_POINTS
}
