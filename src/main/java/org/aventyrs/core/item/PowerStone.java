package org.aventyrs.core.item;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A Pedra do Poder: a {@link PowerStoneType} (what it does) paired with a {@link PowerStoneQuality} (its refino:
 * Raridade, Preço and charge economy), plus an optional Obra-Prima ({@link PowerStoneMasterpiece}) and, only on an
 * Obra-Prima, one Aprimoramento ({@link PowerStoneImprovement}). Rules: {@code docs/rules/pedras-do-poder.txt} (V19).
 *
 * <p><b>A stone is a carried item in its own right</b> ({@link ItemCategory#POWER_STONE}), bought or granted into the
 * inventory like any other. It is <b>not</b> fitted to anything permanently. In combat its bearer <em>vincula</em> it
 * to an Obra-Prima Arma, Armadura or Escudo they wear ({@code PowerStoneService#bind}). That spends a Carga and damages
 * the host, and while the Duração runs the host's {@link Item#getPowerStone()} is this stone, which is what makes its
 * effects reach the existing item aggregation. When the Duração ends, both stone and host cool down.
 *
 * <p>Per-copy state is {@link #getRemainingCharges()} and whether Última Carga's last use is spent. Both persist; the
 * combat-scoped binding and cooldowns live on the sheet. A stone is identified by identity, never by value: two Joias
 * of Hematita are two stones with their own Cargas.
 */
@Getter
public final class PowerStone implements Item {

    /** How long Última Carga's last use lasts: "uma última vez por 2 Rodadas". */
    public static final int LAST_CHANCE_DURATION_ROUNDS = 2;

    /** Cargas Mecanismo de Sobrecarga spends instead of 1. */
    public static final int OVERCHARGE_CHARGES = 3;

    @Getter(lombok.AccessLevel.NONE)
    private final PowerStoneType type;
    private final PowerStoneQuality quality;
    @Getter(lombok.AccessLevel.NONE)
    private final PowerStoneMasterpiece masterpiece;
    @Getter(lombok.AccessLevel.NONE)
    private final PowerStoneImprovement improvement;
    private int remainingCharges;
    private boolean lastChanceUsed;

    private PowerStone(final PowerStoneType type, final PowerStoneQuality quality,
                       final PowerStoneMasterpiece masterpiece, final PowerStoneImprovement improvement) {
        if (masterpiece != null && !quality.isMasterpieceAllowed()) {
            throw new IllegalArgumentException(quality + " cannot be an Obra-Prima.");
        }
        // "Após criar uma Obra-Prima, o joalheiro pode tentar aprimorar a Pedra do Poder".
        if (improvement != null && masterpiece == null) {
            throw new IllegalArgumentException("Only an Obra-Prima stone takes an Aprimoramento.");
        }
        this.type = type;
        this.quality = quality;
        this.masterpiece = masterpiece;
        this.improvement = improvement;
        this.remainingCharges = getEffectiveCharges();
    }

    public static PowerStone of(@NonNull final PowerStoneType type, @NonNull final PowerStoneQuality quality) {
        return new PowerStone(type, quality, null, null);
    }

    public static PowerStone withMasterpiece(@NonNull final PowerStoneType type,
                                             @NonNull final PowerStoneQuality quality,
                                             @NonNull final PowerStoneMasterpiece masterpiece) {
        return new PowerStone(type, quality, masterpiece, null);
    }

    public static PowerStone withMasterpieceAndImprovement(@NonNull final PowerStoneType type,
                                                           @NonNull final PowerStoneQuality quality,
                                                           @NonNull final PowerStoneMasterpiece masterpiece,
                                                           @NonNull final PowerStoneImprovement improvement) {
        return new PowerStone(type, quality, masterpiece, improvement);
    }

    /**
     * A stone with any refinements and its saved state: Cargas left (clamped to 0..{@link #getEffectiveCharges()})
     * and whether Última Carga's last use is spent. This is the restore path for a persisted copy; masterpiece and
     * improvement may be {@code null}.
     */
    public static PowerStone restored(@NonNull final PowerStoneType type, @NonNull final PowerStoneQuality quality,
                                      final PowerStoneMasterpiece masterpiece, final PowerStoneImprovement improvement,
                                      final int remainingCharges, final boolean lastChanceUsed) {
        PowerStone stone = new PowerStone(type, quality, masterpiece, improvement);
        stone.remainingCharges = Math.max(0, Math.min(remainingCharges, stone.getEffectiveCharges()));
        stone.lastChanceUsed = lastChanceUsed;
        return stone;
    }

    /**
     * What this stone does. Named apart from {@link Item#getType()}, which a stone answers as {@link ItemType#UTILITY}
     * like any carried item.
     */
    public PowerStoneType getStoneType() {
        return type;
    }

    /** This stone's Obra-Prima, or {@code null}. Named apart from {@link Item#getMasterpiece()}, which is an item's. */
    public PowerStoneMasterpiece getStoneMasterpiece() {
        return masterpiece;
    }

    /** This stone's Aprimoramento, or {@code null}. Named apart from {@link Item#getImprovement()}. */
    public PowerStoneImprovement getStoneImprovement() {
        return improvement;
    }

    // --- the stone's own effects, read through its host ---------------------------------------

    /** @see PowerStoneType#resolveBonus(ModifierType, ItemType) */
    public int resolveBonus(final ModifierType modifierType, final ItemType hostType) {
        return type.resolveBonus(modifierType, hostType);
    }

    /** @see PowerStoneType#resolveDamageBaseIncrease(Weapon, ItemType) */
    public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
        return type.resolveDamageBaseIncrease(weapon, hostType);
    }

    // --- charge economy -----------------------------------------------------------------------

    /** Cargas after the Obra-Prima's {@code +N%} (Carga Extra), floored. The stone's full load. */
    public int getEffectiveCharges() {
        int percent = masterpiece == null ? 0 : masterpiece.getChargeMultiplierPercent();
        return Math.max(0, quality.getCharges() + quality.getCharges() * percent / 100);
    }

    /** Tempo de Resfriamento in Rodadas after the Obra-Prima's delta, floored at 0. */
    public int getEffectiveCooldownRounds() {
        int delta = masterpiece == null ? 0 : masterpiece.getCooldownRoundsDelta();
        return Math.max(0, quality.getCooldownRounds() + delta);
    }

    /** Duração do Efeito in Rodadas after the Obra-Prima's delta, floored at 0. */
    public int getEffectiveEffectDurationRounds() {
        int delta = masterpiece == null ? 0 : masterpiece.getEffectDurationRoundsDelta();
        return Math.max(0, quality.getEffectDurationRounds() + delta);
    }

    /** Danos de Vinculação after the Obra-Prima's delta. Refinada's −1 stops at "mínimo 1". */
    public int getEffectiveBindingDamage() {
        int base = quality.getBindingDamage();
        int delta = masterpiece == null ? 0 : masterpiece.getBindingDamageDelta();
        return delta < 0 ? Math.max(1, base + delta) : Math.max(0, base + delta);
    }

    /** Whether the first Vinculação of a Cena damages the bearer instead of the host (Solve-Vidas). */
    public boolean bindsToBearerInsteadOfItem() {
        return masterpiece != null && masterpiece.isBindsToBearer();
    }

    /** Whether the Tempo de Vinculação is an Ação Livre (Conexão Veloz). */
    public boolean bindsAsFreeAction() {
        return improvement == PowerStoneImprovement.CONEXAO_VELOZ;
    }

    /** Whether this stone may spend {@link #OVERCHARGE_CHARGES} at once (Mecanismo de Sobrecarga). */
    public boolean canOvercharge() {
        return improvement == PowerStoneImprovement.MECANISMO_DE_SOBRECARGA && remainingCharges >= OVERCHARGE_CHARGES;
    }

    /** Whether this stone, out of Cargas, still has Última Carga's last use. */
    public boolean hasLastChance() {
        return improvement == PowerStoneImprovement.ULTIMA_CARGA && remainingCharges == 0 && !lastChanceUsed;
    }

    /** Whether this stone can be bound at all: a Carga left, or Última Carga's last use. */
    public boolean isUsable() {
        return remainingCharges > 0 || hasLastChance();
    }

    /**
     * Spends charges Cargas, or Última Carga's last use when charges exceed what is left and the stone is empty.
     * A plain mutator: {@code PowerStoneService} judges first. Returns how many Cargas were actually spent.
     */
    public int spendCharges(final int charges) {
        if (remainingCharges == 0 && hasLastChance()) {
            lastChanceUsed = true;
            return 0;
        }
        int spent = Math.max(0, Math.min(charges, remainingCharges));
        remainingCharges -= spent;
        return spent;
    }

    // --- the stone as a carried item ----------------------------------------------------------

    /** "Hematita do Vendaval (Joia)" — the Tipo and the refino. */
    @Override
    public String getName() {
        return type.getName() + " (" + quality.getName() + ")";
    }

    @Override
    public String getDescription() {
        return type.getBaseEffect();
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.POWER_STONE;
    }

    @Override
    public ItemRarity getRarity() {
        return quality.getRarity();
    }

    @Override
    public ItemWeightClass getWeightClass() {
        return ItemWeightClass.LIGHT;
    }

    /** The refino's Preço plus the Obra-Prima's and the Aprimoramento's, as V19 prints them. */
    @Override
    public int getPrice() {
        return quality.getPrice()
                + (masterpiece == null ? 0 : masterpiece.getPriceModifier())
                + (improvement == null ? 0 : improvement.getPriceModifier());
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    @Override
    public int getHardness() {
        return 0;
    }

    @Override
    public int getCastingBonus() {
        return 0;
    }

    @Override
    public ItemFavor getFavor() {
        return null;
    }

    @Override
    public ItemActiveAbility getActiveAbility() {
        return null;
    }

    @Override
    public RegaliaGrade getRegaliaGrade() {
        return null;
    }
}
