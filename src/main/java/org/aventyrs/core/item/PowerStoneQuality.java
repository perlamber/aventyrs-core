package org.aventyrs.core.item;

import lombok.Getter;

/**
 * The Qualidade (refino) of a Pedra do Poder — the second axis of a {@link PowerStone}, orthogonal to its {@link
 * PowerStoneType}. It decides the stone's Raridade, Preço and charge economy, from {@code
 * docs/rules/pedras-do-poder.txt} (V19), "Qualidades, Raridades e Preço":
 * <ul>
 *   <li>Cargas — how many Vinculações it holds;</li>
 *   <li>Danos — what each Vinculação deals its host;</li>
 *   <li>Resfriamento — the Rodadas both stone and host cool down after a binding ends;</li>
 *   <li>Duração do Efeito — how long one binding lasts.</li>
 * </ul>
 * All four are read by {@code PowerStoneService}.
 *
 * <p>{@link #isMasterpieceAllowed()} is {@code false} only for {@link #JOLDA} ("Não pode ser Obra-Prima"), and
 * {@link #reducesCooldownOnEarlyRemoval()} is {@code true} only for {@link #RELIQUIA}: "Remover antecipadamente reduz
 * tempo de Resfriamento do Equipamento em quantidade proporcional a Duração restante". {@link #AETHERNUM}'s recharge
 * by consuming other stones is part of the Joalheria follow-up and not built.
 */
@Getter
public enum PowerStoneQuality {

    /** Jolda (Incomum) — Preço 5, 6 Cargas, Danos 3, Resfriamento 5, Duração 2. Cannot be an Obra-Prima. */
    JOLDA("Jolda", ItemRarity.UNCOMMON, 5, 6, 3, 5, 2, false, false),

    /** Joia (Raro) — Preço 12, 10 Cargas, Danos 2, Resfriamento 4, Duração 3. */
    JOIA("Joia", ItemRarity.RARE, 12, 10, 2, 4, 3, true, false),

    /** Relíquia (Épico) — Preço 20, 20 Cargas, Danos 1, Resfriamento 3, Duração 3. Early removal shortens cooling. */
    RELIQUIA("Relíquia", ItemRarity.EPIC, 20, 20, 1, 3, 3, true, true),

    /** AEthernum (Mítico) — Preço 30, 14 Cargas, Danos 3, Resfriamento 2, Duração 4. */
    AETHERNUM("AEthernum", ItemRarity.MYTHIC, 30, 14, 3, 2, 4, true, false);

    private final String name;
    private final ItemRarity rarity;
    private final int price;
    private final int charges;

    /** Danos de Vinculação — what each binding deals its host Equipamento (its bearer under Solve-Vidas). */
    private final int bindingDamage;

    /** Tempo de Resfriamento, in Rodadas, of both stone and host once a binding ends. */
    private final int cooldownRounds;

    /** Duração do Efeito, in Rodadas, of one binding. */
    private final int effectDurationRounds;

    private final boolean masterpieceAllowed;

    /** Whether unbinding early shortens the host's Resfriamento in proportion to the Duração left (Relíquia). */
    private final boolean reducesCooldownOnEarlyRemoval;

    PowerStoneQuality(final String name, final ItemRarity rarity, final int price, final int charges,
                      final int bindingDamage, final int cooldownRounds, final int effectDurationRounds,
                      final boolean masterpieceAllowed, final boolean reducesCooldownOnEarlyRemoval) {
        this.name = name;
        this.rarity = rarity;
        this.price = price;
        this.charges = charges;
        this.bindingDamage = bindingDamage;
        this.cooldownRounds = cooldownRounds;
        this.effectDurationRounds = effectDurationRounds;
        this.masterpieceAllowed = masterpieceAllowed;
        this.reducesCooldownOnEarlyRemoval = reducesCooldownOnEarlyRemoval;
    }

    /** Whether unbinding early shortens the host's Resfriamento (Relíquia). */
    public boolean reducesCooldownOnEarlyRemoval() {
        return reducesCooldownOnEarlyRemoval;
    }
}
