package org.aventyrs.core.item;

import lombok.NonNull;

/**
 * What Pedra do Poder to make: a {@link PowerStoneType} in a {@link PowerStoneQuality}, optionally an Obra-Prima and,
 * on an Obra-Prima, one Aprimoramento. The stone's {@link ItemSpecification}, used by a store purchase ({@code
 * ItemPurchaseService#purchase(ItemStore, PowerStoneSpecification, CombatantSheet)}) and by a Narrador's grant ({@link
 * #create()} straight into an inventory). Joalheria refining is a later follow-up (table ruling, 2026-10-12).
 *
 * <p>Validated on construction the way {@link PowerStone} is: a Jolda takes no Obra-Prima, and an Aprimoramento
 * needs one.
 */
public record PowerStoneSpecification(@NonNull PowerStoneType type, @NonNull PowerStoneQuality quality,
                                      PowerStoneMasterpiece masterpiece, PowerStoneImprovement improvement) {

    public PowerStoneSpecification {
        if (masterpiece != null && !quality.isMasterpieceAllowed()) {
            throw new IllegalArgumentException(quality + " cannot be an Obra-Prima.");
        }
        if (improvement != null && masterpiece == null) {
            throw new IllegalArgumentException("Only an Obra-Prima stone takes an Aprimoramento.");
        }
    }

    public static PowerStoneSpecification of(final PowerStoneType type, final PowerStoneQuality quality) {
        return new PowerStoneSpecification(type, quality, null, null);
    }

    /** A fresh stone, every Carga full. */
    public PowerStone create() {
        return PowerStone.restored(type, quality, masterpiece, improvement, Integer.MAX_VALUE, false);
    }

    /** Its market Preço: the refino's plus the Obra-Prima's and the Aprimoramento's. */
    public int getPrice() {
        return create().getPrice();
    }
}
