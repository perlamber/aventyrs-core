package org.aventyrs.core.item;

/**
 * What a ranged {@link Weapon} is loaded with — {@link Weapon#getAmmunitionKind()}, matched against a {@link
 * ProjectileItem#getAmmunitionKind()}. Table ruling (2026-10-12): an Arco fires Flechas and a Besta fires Virotes,
 * and every one of {@code equipamentos.txt}'s six Projéteis exists in both forms.
 */
public enum AmmunitionKind {
    /** Flechas — fired by every {@link BowItem}. */
    ARROW,
    /** Virotes — fired by every {@link CrossbowItem}. */
    BOLT,
    /**
     * Dardos — what a Zarabatana fires. TODO: no Dardo projectile is authored yet (its Preço is being remade, table
     * ruling 2026-10-12), so no {@link ThrowableItem} declares this kind and a Zarabatana still fires unconstrained.
     */
    DART
}
