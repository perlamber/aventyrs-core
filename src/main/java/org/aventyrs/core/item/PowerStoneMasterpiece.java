package org.aventyrs.core.item;

import lombok.Getter;

/**
 * An Obra-Prima de Pedra do Poder — one master-crafted refinement fitted to a single stone copy,
 * from the source "Obras-Primas de Pedras do Poder" list (L1345–1354). Held on {@link PowerStone}
 * alongside the optional {@link PowerStoneImprovement}, the same way {@link ItemMasterpiece} sits
 * on a forged item.
 *
 * <p>Every entry adjusts the stone's {@link PowerStoneQuality} charge economy — Resfriamento,
 * Duração, número de Cargas, or the Danos de Vinculação. {@link PowerStone}'s effective-stat
 * getters fold these deltas in. Like the Qualidade columns they modify, <b>nothing consumes the
 * result yet</b> (no activation or bind step exists); the figures are exact authored data.
 *
 * <p>A stone can only take one if its Qualidade allows it — {@link PowerStoneQuality#JOLDA} does
 * not ("Não pode ser Obra-Prima"), enforced by {@link PowerStone}.
 */
@Getter
public enum PowerStoneMasterpiece {

    /** Resfriamento Rápido (Incomum) — Tempo de Resfriamento reduzido em -1 Rodada. */
    RESFRIAMENTO_RAPIDO(ItemRarity.UNCOMMON, -1, 0, 0, 0, false),

    /**
     * Duração Estendida (Incomum) — Duração do Efeito +2 Rodadas, mas Tempo de Resfriamento +1
     * Rodada.
     */
    DURACAO_ESTENDIDA(ItemRarity.UNCOMMON, 1, 2, 0, 0, false),

    /** Carga Extra (Raro) — o número de Cargas é aumentado em +50%. */
    CARGA_EXTRA(ItemRarity.RARE, 0, 0, 50, 0, false),

    /**
     * Solve-Vidas (Épico) — durante a Vinculação inflige danos ao Personagem ao invés do
     * Equipamento.
     */
    SOLVE_VIDAS(ItemRarity.EPIC, 0, 0, 0, 0, true),

    /** Refinada (Épico) — Danos causados durante a Vinculação são reduzidos em -1. */
    REFINADA(ItemRarity.EPIC, 0, 0, 0, -1, false);

    private final ItemRarity rarity;
    private final int cooldownRoundsDelta;
    private final int effectDurationRoundsDelta;

    /** Percent increase to the stone's Cargas (+50 for Carga Extra), 0 otherwise. */
    private final int chargeMultiplierPercent;

    private final int bindingDamageDelta;

    /** Whether Vinculação damages the bearer rather than the host item (Solve-Vidas). */
    private final boolean bindsToBearer;

    PowerStoneMasterpiece(final ItemRarity rarity, final int cooldownRoundsDelta,
                          final int effectDurationRoundsDelta, final int chargeMultiplierPercent,
                          final int bindingDamageDelta, final boolean bindsToBearer) {
        this.rarity = rarity;
        this.cooldownRoundsDelta = cooldownRoundsDelta;
        this.effectDurationRoundsDelta = effectDurationRoundsDelta;
        this.chargeMultiplierPercent = chargeMultiplierPercent;
        this.bindingDamageDelta = bindingDamageDelta;
        this.bindsToBearer = bindsToBearer;
    }
}
