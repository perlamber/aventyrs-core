package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;

import lombok.NonNull;

/** An Improvement fitted to one item, retaining any selection made while that item was created. */
public final class ItemImprovement implements Improvement {

    private final DefensiveImprovement definition;
    private final DefenseType selectedDefense;
    private final ElementalType selectedElementalType;

    private ItemImprovement(final DefensiveImprovement definition, final DefenseType selectedDefense,
                            final ElementalType selectedElementalType) {
        this.definition = definition;
        this.selectedDefense = selectedDefense;
        this.selectedElementalType = selectedElementalType;
    }

    public static ItemImprovement of(@NonNull final DefensiveImprovement definition) {
        if (definition.requiresDefenseChoice() || definition.requiresElementalTypeChoice()) {
            throw new IllegalArgumentException(definition.getName() + " requires its creation-time choice.");
        }
        return new ItemImprovement(definition, null, null);
    }

    public static ItemImprovement camadaDeReforco(@NonNull final DefenseType selectedDefense) {
        return new ItemImprovement(DefensiveImprovement.CAMADA_DE_REFORCO, selectedDefense, null);
    }

    public static ItemImprovement camadaDeReforcoParaEscudo() {
        return new ItemImprovement(DefensiveImprovement.CAMADA_DE_REFORCO, null, null);
    }

    public static ItemImprovement bencaoElemental(@NonNull final ElementalType selectedElementalType) {
        if (selectedElementalType == ElementalType.TODOS) {
            throw new IllegalArgumentException("Benção Elemental requires one concrete element.");
        }
        return new ItemImprovement(DefensiveImprovement.BENCAO_ELEMENTAL, null, selectedElementalType);
    }

    public DefensiveImprovement getDefinition() {
        return definition;
    }

    public DefenseType getSelectedDefense() {
        return selectedDefense;
    }

    public ElementalType getSelectedElementalType() {
        return selectedElementalType;
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
    public int getHardnessBonus() {
        return definition.getHardnessBonus();
    }

    @Override
    public int getWeightClassBonus() {
        return definition.getWeightClassBonus();
    }

    @Override
    public int getItemDamageReduction() {
        return definition.getItemDamageReduction();
    }

    @Override
    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character) {
        return getEffectiveDefenseBonus(defenseType, character, null);
    }

    @Override
    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext, final Item item,
                                        final DamageDescriptor damageDescriptor) {
        return definition.getEffectiveDefenseBonus(
                defenseType, character, sceneContext, item, selectedDefense, selectedElementalType, damageDescriptor);
    }

    @Override
    public void onFinalDamageTaken(final Item item, final int finalDamage, final SceneContext sceneContext) {
        definition.onFinalDamageTaken(item, finalDamage, sceneContext);
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character) {
        return definition.resolveBonus(modifierType, skillType, character);
    }

    @Override
    public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        return definition.resolveDamageBaseIncrease(weapon, character);
    }

    @Override
    public int resolveDamageReduction(final DamageDescriptor damageDescriptor, final Character character) {
        return definition.resolveDamageReduction(damageDescriptor, character, selectedElementalType);
    }

    @Override
    public int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
        return definition.resolveDurationIncreaseInRounds(spell, character);
    }
}
