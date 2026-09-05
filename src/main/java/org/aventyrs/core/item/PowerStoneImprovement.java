package org.aventyrs.core.item;

import lombok.Getter;

/**
 * An Aprimoramento fitted to a Pedra do Poder's Obra-Prima, from the source "Aprimoramentos de
 * Obras-Primas" list under Pedras do Poder (L1356–1358).
 *
 * <p>The ruleset confirms exactly one so far, so this enum has one constant — modeled like
 * {@link ItemRarity}, which likewise carries only the tiers real text has confirmed. Add a
 * constant when its rules text exists rather than guessing the rest of the list.
 */
@Getter
public enum PowerStoneImprovement {

    /** Conexão Veloz (Raro) — Tempo de Vinculação reduzido para Ação Livre. */
    CONEXAO_VELOZ(ItemRarity.RARE);

    private final ItemRarity rarity;

    PowerStoneImprovement(final ItemRarity rarity) {
        this.rarity = rarity;
    }
}
