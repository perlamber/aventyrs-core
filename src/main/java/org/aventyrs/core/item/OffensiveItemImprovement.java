package org.aventyrs.core.item;

import java.util.Objects;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;

import lombok.NonNull;

/**
 * An {@link OffensiveImprovement} fitted together with its creation-time choice — the offensive twin of
 * {@link ItemImprovement}. Only {@link OffensiveImprovement#BENCAO_ELEMENTAL} has one: "tipo escolhido na
 * fabricação" (core 0.1.6.9). Every column and clause is its definition's; the element is what this adds.
 */
public final class OffensiveItemImprovement implements Improvement {

    private final OffensiveImprovement definition;
    private final ElementalType selectedElementalType;

    private OffensiveItemImprovement(final OffensiveImprovement definition, final ElementalType selectedElementalType) {
        this.definition = definition;
        this.selectedElementalType = selectedElementalType;
    }

    /**
     * Benção Elemental with its chosen element.
     *
     * @throws IllegalArgumentException for {@link ElementalType#TODOS} — the choice is one element
     */
    public static OffensiveItemImprovement bencaoElemental(@NonNull final ElementalType selectedElementalType) {
        if (selectedElementalType == ElementalType.TODOS) {
            throw new IllegalArgumentException("Benção Elemental requires one concrete element.");
        }
        return new OffensiveItemImprovement(OffensiveImprovement.BENCAO_ELEMENTAL, selectedElementalType);
    }

    public OffensiveImprovement getDefinition() {
        return definition;
    }

    /** The element every hit with the host deals in addition to its own types. */
    public ElementalType getSelectedElementalType() {
        return selectedElementalType;
    }

    @Override
    public OffensiveImprovement offensiveDefinition() {
        return definition;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getDescription() {
        return definition.getDescription();
    }

    @Override
    public ItemRarity getRarity() {
        return definition.getRarity();
    }

    /** The choice changes what it does, never what it costs. */
    @Override
    public EnhancementPriceCategory getPriceCategory() {
        return definition.getPriceCategory();
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
    public int getAttackBonus() {
        return definition.getAttackBonus();
    }

    @Override
    public int getDamageBonus() {
        return definition.getDamageBonus();
    }

    @Override
    public int getCastingBonus() {
        return definition.getCastingBonus();
    }

    @Override
    public int getPriceModifier() {
        return definition.getPriceModifier();
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character) {
        return definition.resolveBonus(modifierType, skillType, character);
    }

    @Override
    public boolean preventsDisarming() {
        return definition.preventsDisarming();
    }

    @Override
    public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        return definition.resolveDamageBaseIncrease(weapon, character);
    }

    @Override
    public int resolveCriticalMarginIncrease(final Weapon weapon, final Character character) {
        return definition.resolveCriticalMarginIncrease(weapon, character);
    }

    @Override
    public CriticalDamage resolveCriticalDamage(final Weapon weapon, final Character character) {
        return definition.resolveCriticalDamage(weapon, character);
    }

    @Override
    public int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
        return definition.resolveDurationIncreaseInRounds(spell, character);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof OffensiveItemImprovement that
                && definition == that.definition && selectedElementalType == that.selectedElementalType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, selectedElementalType);
    }

    @Override
    public String toString() {
        return definition.name() + "(" + selectedElementalType + ")";
    }
}
