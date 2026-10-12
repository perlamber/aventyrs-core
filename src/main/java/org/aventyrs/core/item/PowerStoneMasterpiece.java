package org.aventyrs.core.item;

import lombok.Getter;

/**
 * An Obra-Prima de Pedra do Poder — one master-crafted refinement of a single stone, from {@code
 * docs/rules/pedras-do-poder.txt} (V19), "Obras-Primas de Pedras do Poder", each with its own Preço in PE. Every
 * entry adjusts the stone's {@link PowerStoneQuality} charge economy; {@link PowerStone}'s effective getters fold the
 * deltas in, and {@code PowerStoneService} reads the result. A {@link PowerStoneQuality#JOLDA} cannot take one.
 */
@Getter
public enum PowerStoneMasterpiece {

    /** Carga Extra [+9PE] — o número de Cargas é aumentado em +50%. */
    CARGA_EXTRA("Carga Extra", 9, 0, 0, 50, 0),

    /** Duração Estendida [+6PE] — Duração do Efeito +2 Rodadas, Tempo de Resfriamento +1 Rodada. */
    DURACAO_ESTENDIDA("Duração Estendida", 6, 1, 2, 0, 0),

    /** Refinada [+12PE] — Danos causados durante a Vinculação reduzidos em -1 (mínimo 1). */
    REFINADA("Refinada", 12, 0, 0, 0, -1),

    /** Resfriamento Rápido [+6PE] — Tempo de Resfriamento reduzido em -1 Rodada. */
    RESFRIAMENTO_RAPIDO("Resfriamento Rápido", 6, -1, 0, 0, 0),

    /**
     * Solve-Vidas [+8PE] — "A primeira vez que for Vinculada a cada Cena inflige danos ao Personagem, ao invés do
     * Equipamento. Danos causados desta forma são recuperados apenas após Descansos Verdadeiros."
     */
    SOLVE_VIDAS("Solve-Vidas", 8, 0, 0, 0, 0);

    private final String name;
    /** This Obra-Prima's Preço in PE, as V19 prints it beside its name. */
    private final int priceModifier;
    private final int cooldownRoundsDelta;
    private final int effectDurationRoundsDelta;

    /** Percent increase to the stone's Cargas (+50 for Carga Extra), 0 otherwise. */
    private final int chargeMultiplierPercent;

    private final int bindingDamageDelta;

    PowerStoneMasterpiece(final String name, final int priceModifier, final int cooldownRoundsDelta,
                          final int effectDurationRoundsDelta, final int chargeMultiplierPercent,
                          final int bindingDamageDelta) {
        this.name = name;
        this.priceModifier = priceModifier;
        this.cooldownRoundsDelta = cooldownRoundsDelta;
        this.effectDurationRoundsDelta = effectDurationRoundsDelta;
        this.chargeMultiplierPercent = chargeMultiplierPercent;
        this.bindingDamageDelta = bindingDamageDelta;
    }

    /** Whether the first Vinculação of a Cena damages the bearer rather than the host (Solve-Vidas). */
    public boolean isBindsToBearer() {
        return this == SOLVE_VIDAS;
    }
}
