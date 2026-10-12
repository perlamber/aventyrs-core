package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * A ranged weapon swung corpo-a-corpo through the blade fitted to it — Machado Anexo's and Baioneta's "Armas de
 * Ataque à Distância são Machados / Lanças" (table ruling, 2026-10-12: the weapon also gains a melee mode). The
 * {@link org.aventyrs.core.skill.AttackSource} such an attack names, the way {@link ShieldAttack} is for a Escudo.
 *
 * <p>A <b>view over the ranged copy</b>, not an item of its own: it reads Ataque Corpo-a-Corpo at {@link
 * Range#ADJACENTE}, fires nothing, and counts as the fitted blade's category ({@link
 * OffensiveImprovement#getAdditionalCategory()}); everything else — the Dano Base, the Efeito Crítico and its
 * margin, the weight, the Obra-Prima, the Aprimoramentos, the Pedra, the damage the wood takes — is the copy's
 * ({@link #host()}). ⚠️ The ruling keeps the weapon's own Dano Base and Efeito Crítico rather than a Machado's or
 * a Lança's. Two views of one copy are {@link #equals equal}.
 */
public final class AttachedMeleeAttack implements Weapon {

    private final Weapon ranged;
    private final ItemCategory category;

    private AttachedMeleeAttack(final Weapon ranged, final ItemCategory category) {
        this.ranged = ranged;
        this.category = category;
    }

    /**
     * The melee mode of ranged, struck as the category its first fitted blade adds.
     *
     * @throws IllegalArgumentException when ranged is not an Ataque à Distância weapon, or carries no Aprimoramento
     *         granting a melee category
     */
    public static AttachedMeleeAttack of(final Weapon ranged) {
        ItemCategory category = meleeCategoryOf(ranged);
        if (category == null) {
            throw new IllegalArgumentException("No attached blade: " + ranged);
        }
        return new AttachedMeleeAttack(ranged, category);
    }

    /** Whether weapon can strike corpo-a-corpo through a fitted blade. */
    public static boolean isAvailableFor(final Weapon weapon) {
        return meleeCategoryOf(weapon) != null;
    }

    private static ItemCategory meleeCategoryOf(final Weapon weapon) {
        if (weapon == null || weapon instanceof AttachedMeleeAttack
                || weapon.getSkillType() != SkillType.ATAQUE_A_DISTANCIA) {
            return null;
        }
        return WeaponEnhancements.offensiveImprovements(weapon.host()).stream()
                .map(OffensiveImprovement::getAdditionalCategory)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The ranged weapon this view swings. */
    public Weapon getRanged() {
        return ranged;
    }

    @Override
    public Item host() {
        return ranged.host();
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    @Override
    public Range getRange() {
        return Range.ADJACENTE;
    }

    @Override
    public ItemCategory getCategory() {
        return category;
    }

    /** The blade's category, and the ranged weapon's own too. */
    @Override
    public boolean countsAs(final ItemCategory other) {
        return other != null && (other == category || ranged.countsAs(other));
    }

    @Override
    public DamageBase getDamageBase() {
        return ranged.getDamageBase();
    }

    @Override
    public DamageBase getEffectiveDamageBase() {
        return ranged.getEffectiveDamageBase();
    }

    @Override
    public CriticalEffectType getCriticalEffect() {
        return ranged.getCriticalEffect();
    }

    @Override
    public int getLesserCriticalMargin() {
        return ranged.getLesserCriticalMargin();
    }

    @Override
    public boolean isDisarmable() {
        return ranged.isDisarmable();
    }

    @Override
    public String getName() {
        return ranged.getName();
    }

    @Override
    public String getDescription() {
        return ranged.getDescription();
    }

    @Override
    public ItemRarity getRarity() {
        return ranged.getRarity();
    }

    @Override
    public ItemWeightClass getWeightClass() {
        return ranged.getWeightClass();
    }

    @Override
    public ItemWeightClass getEffectiveWeightClass() {
        return ranged.getEffectiveWeightClass();
    }

    @Override
    public int getPrice() {
        return ranged.getPrice();
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return ranged.getPhysicalDefenseBonus();
    }

    @Override
    public int getMagicDefenseBonus() {
        return ranged.getMagicDefenseBonus();
    }

    @Override
    public int getHardness() {
        return ranged.getHardness();
    }

    @Override
    public int getDamageTaken() {
        return ranged.getDamageTaken();
    }

    @Override
    public boolean isDestroyed() {
        return ranged.isDestroyed();
    }

    @Override
    public int applyDamage(final int rawDamage) {
        return ranged.applyDamage(rawDamage);
    }

    @Override
    public int repair(final int rawRecovery) {
        return ranged.repair(rawRecovery);
    }

    @Override
    public int getCastingBonus() {
        return ranged.getCastingBonus();
    }

    @Override
    public ItemFavor getFavor() {
        return ranged.getFavor();
    }

    @Override
    public Masterpiece getMasterpiece() {
        return ranged.getMasterpiece();
    }

    @Override
    public List<Improvement> getImprovements() {
        return ranged.getImprovements();
    }

    @Override
    public PowerStone getPowerStone() {
        return ranged.getPowerStone();
    }

    @Override
    public ItemActiveAbility getActiveAbility() {
        return ranged.getActiveAbility();
    }

    @Override
    public RegaliaGrade getRegaliaGrade() {
        return ranged.getRegaliaGrade();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof AttachedMeleeAttack that && this.ranged == that.ranged;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(ranged);
    }

    @Override
    public String toString() {
        return "AttachedMeleeAttack[" + getName() + " as " + category + "]";
    }
}
