package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;

/**
 * A permanent enhancement that can be applied to a unique item instance. Each item may carry at
 * most one improvement, stored per-copy rather than in the catalog template.
 */
public interface Improvement {
    String getName();

    String getDescription();

    /**
     * This Aprimoramento's Raridade — the tier its install Grau de Dificuldade is read from
     * ({@code org.aventyrs.core.item.ItemRarity#getImprovementInstallDifficulty()}).
     */
    ItemRarity getRarity();

    default int getPhysicalDefenseBonus() {
        return 0;
    }

    default int getMagicDefenseBonus() {
        return 0;
    }

    default int getAttackBonus() {
        return 0;
    }

    default int getDamageBonus() {
        return 0;
    }

    default int getHardnessBonus() {
        return 0;
    }

    /**
     * How much damage aimed at the <em>fitted item itself</em> this improvement shrugs off. Not
     * to be confused with {@link #resolveDamageReduction(DamageDescriptor, Character)}, which is
     * the RD this improvement grants its <em>wearer</em> — two different victims.
     */
    default int getItemDamageReduction() {
        return 0;
    }

    /** Signed change to the item's weight category: negative is lighter, positive is heavier. */
    default int getWeightClassBonus() {
        return 0;
    }

    default int getCastingBonus() {
        return 0;
    }

    default int getPriceModifier() {
        return 0;
    }

    default int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character) {
        return getEffectiveDefenseBonus(defenseType, character, null, null);
    }

    /**
     * This improvement's current Defesa contribution. SceneContext is available for a fitted
     * improvement whose benefit is limited to specific combat Rounds.
     */
    default int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                         final org.aventyrs.core.scene.SceneContext sceneContext) {
        return getEffectiveDefenseBonus(defenseType, character, sceneContext, null);
    }

    /**
     * This improvement's current Defesa contribution for its fitted item. The item supplies
     * per-copy state for effects whose catalog rule is triggered during a Scene.
     */
    default int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                         final org.aventyrs.core.scene.SceneContext sceneContext,
                                         final Item item) {
        return defenseType == DefenseType.PHYSICAL ? getPhysicalDefenseBonus() : getMagicDefenseBonus();
    }

    /** Descriptor-aware form for a defense benefit conditional on the incoming attack's element. */
    default int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                         final org.aventyrs.core.scene.SceneContext sceneContext,
                                         final Item item, final DamageDescriptor damageDescriptor) {
        return getEffectiveDefenseBonus(defenseType, character, sceneContext, item);
    }

    /** Lets this catalog rule react when its fitted item wearer takes final damage. */
    default void onFinalDamageTaken(final Item item, final int finalDamage,
                                    final org.aventyrs.core.scene.SceneContext sceneContext) {
    }

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

    /** How many Dano Base scale-ups this improvement grants when weapon is the attack source. */
    default int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        return 0;
    }

    /** This improvement's damage reduction for one fully-classified incoming damage instance. */
    default int resolveDamageReduction(final DamageDescriptor damageDescriptor, final Character character) {
        return 0;
    }

    /** The number of Rodadas this improvement adds to the given Magia's resolved Duração. */
    default int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
        return 0;
    }
}
