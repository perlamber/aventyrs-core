package org.aventyrs.core.item;

import lombok.Getter;

/**
 * The Qualidade of a Pedra do Poder — the second axis of a {@link PowerStone}, orthogonal to its
 * {@link PowerStoneType}. Where the Tipo decides <em>what</em> the stone does, the Qualidade
 * decides its Preço and its charge economy: how many Cargas it holds, how much damage binding it
 * (Vinculação) inflicts, its Resfriamento between uses, and how long its activated effect lasts.
 * From the source "Qualidades, Raridades e Preço" table (L1324–1343).
 *
 * <p><b>The charge economy is authored data with no consumer yet.</b> This core has no
 * activation/charge-spend mechanism and no forge/bind step, so {@link #getCharges()} /
 * {@link #getBindingDamage()} / {@link #getCooldownRounds()} / {@link #getEffectDurationRounds()}
 * are exact figures nothing reads — the same "can't apply it yet doesn't mean can't compute it
 * yet" treatment {@link Item#getPrice()} and the unread Improvement/Masterpiece columns get. The
 * stone's <em>passive</em> mode effects (see {@link PowerStoneType}) do reach real machinery.
 *
 * <p>{@link #isMasterpieceAllowed()} is {@code false} only for {@link #JOLDA} — its "Outras
 * Informações" line reads "Não pode ser Obra-Prima". {@link PowerStone} enforces it.
 */
@Getter
public enum PowerStoneQuality {

    /** Jolda (Comum) — the cheap synthetic grade; the only one that cannot be an Obra-Prima. */
    JOLDA(ItemRarity.COMMON, 5, 5, 3, 5, 2, false),

    /** Joia (Incomum). Removing it early refunds Resfriamento proportional to the Duração left. */
    JOIA(ItemRarity.UNCOMMON, 10, 10, 2, 4, 3, true),

    /** Relíquia (Raro). */
    RELIQUIA(ItemRarity.RARE, 20, 20, 2, 3, 3, true),

    /** AEthernum (Mítico) — can be recharged by consuming other Pedras do Poder. */
    AETHERNUM(ItemRarity.MYTHIC, 50, 20, 3, 2, 4, true);

    private final ItemRarity rarity;
    private final int price;
    private final int charges;

    /**
     * Danos de Vinculação — the damage binding a stone of this Qualidade deals, normally to the
     * host Equipamento (to the bearer instead if the {@link PowerStoneMasterpiece#SOLVE_VIDAS}
     * Obra-Prima is fitted). No forge/bind step exists to apply it.
     */
    private final int bindingDamage;

    /** Tempo de Resfriamento, in Rodadas, between activations of the stone's effect. */
    private final int cooldownRounds;

    /** Duração do Efeito, in Rodadas, of one activation. */
    private final int effectDurationRounds;

    private final boolean masterpieceAllowed;

    PowerStoneQuality(final ItemRarity rarity, final int price, final int charges,
                      final int bindingDamage, final int cooldownRounds, final int effectDurationRounds,
                      final boolean masterpieceAllowed) {
        this.rarity = rarity;
        this.price = price;
        this.charges = charges;
        this.bindingDamage = bindingDamage;
        this.cooldownRounds = cooldownRounds;
        this.effectDurationRounds = effectDurationRounds;
        this.masterpieceAllowed = masterpieceAllowed;
    }
}
