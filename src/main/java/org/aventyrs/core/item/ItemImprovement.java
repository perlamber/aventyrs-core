package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.SkillType;

import java.util.Objects;

import lombok.NonNull;

/** An Improvement fitted to one item, retaining any selection made while that item was created. */
public final class ItemImprovement implements Improvement {

    private final DefensiveImprovement definition;
    private final DefenseType selectedDefense;
    private final ElementalType selectedElementalType;
    /** Camuflada's "tipo de ambiente definido na criação do item" (core 0.1.6.11), or {@code null}. */
    private final TerrainType selectedTerrain;

    private ItemImprovement(final DefensiveImprovement definition, final DefenseType selectedDefense,
                            final ElementalType selectedElementalType) {
        this(definition, selectedDefense, selectedElementalType, null);
    }

    private ItemImprovement(final DefensiveImprovement definition, final DefenseType selectedDefense,
                            final ElementalType selectedElementalType, final TerrainType selectedTerrain) {
        this.definition = definition;
        this.selectedDefense = selectedDefense;
        this.selectedElementalType = selectedElementalType;
        this.selectedTerrain = selectedTerrain;
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

    /**
     * Camuflada with its environment — the extra Vantagem in rolagens de Furtividade there. {@link #of} still accepts
     * a bare Camuflada (a copy made before the choice was recorded): it keeps the Furtividade Vantagem and nothing more.
     */
    public static ItemImprovement camuflada(@NonNull final TerrainType selectedTerrain) {
        return new ItemImprovement(DefensiveImprovement.CAMUFLADA, null, null, selectedTerrain);
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

    public TerrainType getSelectedTerrain() {
        return selectedTerrain;
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

    /**
     * The fitted copy is priced exactly as its catalog entry is — a creation-time choice
     * (Camada de Reforço's Defesa, Benção Elemental's element) changes what the Aprimoramento
     * does, not what it costs.
     */
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
        return resolveBonus(modifierType, skillType, character, null);
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character,
                            final SceneContext sceneContext) {
        return definition.resolveBonus(modifierType, skillType, character, sceneContext, selectedTerrain);
    }

    @Override
    public int resolveCriticalMarginIncrease(final Weapon weapon, final Character character) {
        return definition.resolveCriticalMarginIncrease(weapon, character);
    }

    @Override
    public org.aventyrs.core.character.CriticalDamage resolveCriticalDamage(final Weapon weapon,
                                                                           final Character character) {
        return definition.resolveCriticalDamage(weapon, character);
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
    public int resolveDamageReduction(final DamageDescriptor damageDescriptor, final Character character) {
        return definition.resolveDamageReduction(damageDescriptor, character, selectedElementalType);
    }

    @Override
    public int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
        return definition.resolveDurationIncreaseInRounds(spell, character);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ItemImprovement improvement)) {
            return false;
        }
        return definition == improvement.definition && selectedDefense == improvement.selectedDefense
                && selectedElementalType == improvement.selectedElementalType
                && selectedTerrain == improvement.selectedTerrain;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, selectedDefense, selectedElementalType, selectedTerrain);
    }
}
