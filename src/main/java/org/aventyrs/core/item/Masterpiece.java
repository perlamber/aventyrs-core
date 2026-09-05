package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;

/**
 * A master-crafted quality applied to a unique item instance. {@link DefensiveMasterpiece}
 * contains the authored defensive catalog; {@link ItemMasterpiece} wraps an entry where an
 * individual forged copy needs a choice.
 */
public interface Masterpiece {
    String getName();

    String getDescription();

    default int getPhysicalDefenseBonus() {
        return 0;
    }

    default int getMagicDefenseBonus() {
        return 0;
    }

    default int getCastingBonus() {
        return 0;
    }

    /**
     * This masterpiece's current contribution to one Defesa. "Muda para" entries override their
     * own base column here rather than stacking a second bonus onto it.
     */
    default int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character) {
        return defenseType == DefenseType.PHYSICAL ? getPhysicalDefenseBonus() : getMagicDefenseBonus();
    }

    /** A conditional bonus the masterpiece grants its wearer once its requirements are met. */
    default int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character) {
        return 0;
    }

    /**
     * Whether this enhancement stops its weapon being knocked out of its wielder's hands —
     * the "Não pode ser desarmado" Característica Adicional (Manopla de Segurança, an
     * Aprimoramento de Obra-Prima Ofensiva). False by default.
     *
     * <p>No constant overrides it yet: the offensive Obra-Prima/Aprimoramento catalogues are not
     * authored (only the defensive ones are), so this is the hook {@code Weapon#isDisarmable()}
     * consults, waiting on the catalogue rather than on a mechanism.
     */
    default boolean preventsDisarming() {
        return false;
    }

    /** How many Dano Base scale-ups this masterpiece grants when weapon is the attack source. */
    default int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        return 0;
    }

    default int getHardnessBonus() {
        return 0;
    }

    /** How much damage aimed at the fitted item itself this masterpiece shrugs off. */
    default int getItemDamageReduction() {
        return 0;
    }

    /** Signed change to the item's weight category: negative is lighter, positive is heavier. */
    default int getWeightClassBonus() {
        return 0;
    }

    default int getPriceModifier() {
        return 0;
    }
}
