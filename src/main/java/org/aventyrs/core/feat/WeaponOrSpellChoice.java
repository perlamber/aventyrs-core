package org.aventyrs.core.feat;

import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.skill.AttackSource;

/**
 * The coarse "Armas ou Magias" pick a Talento records at acquisition — used by exactly one
 * Talento today ({@code AssassinoFeat#SAQUE_RELAMPAGO}), which is why it is not the per-category
 * {@code org.aventyrs.core.item.AttackMethod} enum: that one answers "which <em>kind</em> of
 * weapon" (Arco / Lâmina Leve / …), whereas this is the whole-category weapons-vs-spells split.
 * A second Talento naming the identical coarse choice would be the point to reconsider.
 */
public enum WeaponOrSpellChoice {
    WEAPONS, SPELLS;

    /**
     * Whether an attack delivered with source matches this choice — a {@link Weapon} (an Arma
     * Natural is still a {@code Weapon}) for {@link #WEAPONS}, a {@link Spell} for {@link
     * #SPELLS}. {@code null} always reads as "no match", the same convention {@code
     * AttackMethod#matches} uses.
     */
    public boolean matches(final AttackSource source) {
        return switch (this) {
            case WEAPONS -> source instanceof Weapon;
            case SPELLS -> source instanceof Spell;
        };
    }
}
