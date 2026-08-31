package org.aventyrs.core.item;

import java.util.Objects;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;

import lombok.NonNull;

/**
 * An Obra-Prima fitted to one item copy, including the choices its catalog definition requires.
 */
public final class ItemMasterpiece implements Masterpiece {

    private final DefensiveMasterpiece definition;
    private final DefenseType selectedDefense;
    private final ModifierType selectedActionBonus;

    private ItemMasterpiece(final DefensiveMasterpiece definition, final DefenseType selectedDefense,
                            final ModifierType selectedActionBonus) {
        this.definition = definition;
        this.selectedDefense = selectedDefense;
        this.selectedActionBonus = selectedActionBonus;
    }

    public static ItemMasterpiece of(@NonNull final DefensiveMasterpiece definition) {
        if (definition == DefensiveMasterpiece.MAGISTRAL || definition == DefensiveMasterpiece.SOB_MEDIDA) {
            throw new IllegalArgumentException(definition.getName() + " requires a creation choice.");
        }
        return new ItemMasterpiece(definition, null, null);
    }

    public static ItemMasterpiece magistral(@NonNull final DefenseType selectedDefense) {
        return new ItemMasterpiece(DefensiveMasterpiece.MAGISTRAL, selectedDefense, null);
    }

    public static ItemMasterpiece sobMedida(@NonNull final ModifierType selectedActionBonus) {
        if (selectedActionBonus != ModifierType.REACTIONS && selectedActionBonus != ModifierType.FREE_ACTIONS) {
            throw new IllegalArgumentException("Sob Medida must select REACTIONS or FREE_ACTIONS.");
        }
        return new ItemMasterpiece(DefensiveMasterpiece.SOB_MEDIDA, null, selectedActionBonus);
    }

    public DefensiveMasterpiece getDefinition() {
        return definition;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getDescription() {
        return definition.getFavorDescription();
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return definition.getPhysicalDefenseBonus();
    }

    @Override
    public int getMagicDefenseBonus() {
        return definition.getMagicDefenseBonus();
    }

    @Override
    public int getCastingBonus() {
        return definition.getCastingBonus();
    }

    @Override
    public int getWeightClassBonus() {
        return definition.getWeightClassBonus();
    }

    @Override
    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character) {
        if (definition == DefensiveMasterpiece.MAGISTRAL && definition.getRequirements().isMetBy(character)) {
            return defenseType == selectedDefense ? 3 : 0;
        }
        return definition.getEffectiveDefenseBonus(defenseType, character);
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character) {
        if (definition == DefensiveMasterpiece.SOB_MEDIDA && modifierType == selectedActionBonus
                && definition.getRequirements().isMetBy(character)) {
            return 1;
        }
        return definition.resolveBonus(modifierType, skillType, character);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ItemMasterpiece masterpiece)) {
            return false;
        }
        return definition == masterpiece.definition && selectedDefense == masterpiece.selectedDefense
                && selectedActionBonus == masterpiece.selectedActionBonus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, selectedDefense, selectedActionBonus);
    }
}
