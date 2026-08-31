package org.aventyrs.core.item;

import java.util.Objects;

import org.aventyrs.core.modifier.ModifierType;

import lombok.Getter;
import lombok.NonNull;

/**
 * A Pedra do Poder fitted to one item copy — a {@link PowerStoneType} (what it does) paired with
 * a {@link PowerStoneQuality} (its Preço and charge economy), plus the optional Obra-Prima
 * ({@link PowerStoneMasterpiece}) and Aprimoramento ({@link PowerStoneImprovement}) refinements.
 *
 * <p>The per-copy analogue of {@link ItemMasterpiece}/{@link ItemImprovement}: held on the
 * forged {@link AbstractItem}, never on a catalog {@link ItemTemplate}, and requiring the
 * Encaixe Aprimoramento on its host — see {@link AbstractItem#setPowerStone(PowerStone)}.
 *
 * <p>Its passive mode effects reach real machinery through {@link Item}'s enhancement
 * aggregation (which selects the Defensivo or Ofensivo mode from the host's {@link
 * Item#getType()}); its charge/cooldown/Vinculação figures — folded here with the Obra-Prima's
 * deltas — have no consumer yet, per {@link PowerStoneQuality}'s own javadoc.
 */
@Getter
public final class PowerStone {

    private final PowerStoneType type;
    private final PowerStoneQuality quality;
    private final PowerStoneMasterpiece masterpiece;
    private final PowerStoneImprovement improvement;

    private PowerStone(final PowerStoneType type, final PowerStoneQuality quality,
                       final PowerStoneMasterpiece masterpiece, final PowerStoneImprovement improvement) {
        if (masterpiece != null && !quality.isMasterpieceAllowed()) {
            throw new IllegalArgumentException(quality + " cannot be an Obra-Prima.");
        }
        this.type = type;
        this.quality = quality;
        this.masterpiece = masterpiece;
        this.improvement = improvement;
    }

    public static PowerStone of(@NonNull final PowerStoneType type, @NonNull final PowerStoneQuality quality) {
        return new PowerStone(type, quality, null, null);
    }

    public static PowerStone withMasterpiece(@NonNull final PowerStoneType type,
                                             @NonNull final PowerStoneQuality quality,
                                             @NonNull final PowerStoneMasterpiece masterpiece) {
        return new PowerStone(type, quality, masterpiece, null);
    }

    public static PowerStone withImprovement(@NonNull final PowerStoneType type,
                                             @NonNull final PowerStoneQuality quality,
                                             @NonNull final PowerStoneImprovement improvement) {
        return new PowerStone(type, quality, null, improvement);
    }

    public static PowerStone withMasterpieceAndImprovement(@NonNull final PowerStoneType type,
                                                           @NonNull final PowerStoneQuality quality,
                                                           @NonNull final PowerStoneMasterpiece masterpiece,
                                                           @NonNull final PowerStoneImprovement improvement) {
        return new PowerStone(type, quality, masterpiece, improvement);
    }

    /** @see PowerStoneType#resolveBonus(ModifierType, ItemType) */
    public int resolveBonus(final ModifierType modifierType, final ItemType hostType) {
        return type.resolveBonus(modifierType, hostType);
    }

    /** @see PowerStoneType#resolveDamageBaseIncrease(Weapon, ItemType) */
    public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
        return type.resolveDamageBaseIncrease(weapon, hostType);
    }

    /** Cargas after the fitted Obra-Prima's {@code +N%} adjustment, floored at 0. */
    public int getEffectiveCharges() {
        int percent = masterpiece == null ? 0 : masterpiece.getChargeMultiplierPercent();
        return Math.max(0, quality.getCharges() + quality.getCharges() * percent / 100);
    }

    /** Tempo de Resfriamento in Rodadas after the fitted Obra-Prima's delta, floored at 0. */
    public int getEffectiveCooldownRounds() {
        int delta = masterpiece == null ? 0 : masterpiece.getCooldownRoundsDelta();
        return Math.max(0, quality.getCooldownRounds() + delta);
    }

    /** Duração do Efeito in Rodadas after the fitted Obra-Prima's delta, floored at 0. */
    public int getEffectiveEffectDurationRounds() {
        int delta = masterpiece == null ? 0 : masterpiece.getEffectDurationRoundsDelta();
        return Math.max(0, quality.getEffectDurationRounds() + delta);
    }

    /** Danos de Vinculação after the fitted Obra-Prima's delta (Refinada -1), floored at 0. */
    public int getEffectiveBindingDamage() {
        int delta = masterpiece == null ? 0 : masterpiece.getBindingDamageDelta();
        return Math.max(0, quality.getBindingDamage() + delta);
    }

    /** Whether Vinculação damages the bearer instead of the host item ({@code SOLVE_VIDAS}). */
    public boolean bindsToBearerInsteadOfItem() {
        return masterpiece != null && masterpiece.isBindsToBearer();
    }

    /** Whether the Tempo de Vinculação is an Ação Livre ({@code CONEXAO_VELOZ}). */
    public boolean bindsAsFreeAction() {
        return improvement == PowerStoneImprovement.CONEXAO_VELOZ;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PowerStone stone)) {
            return false;
        }
        return type == stone.type && quality == stone.quality
                && masterpiece == stone.masterpiece && improvement == stone.improvement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, quality, masterpiece, improvement);
    }
}
