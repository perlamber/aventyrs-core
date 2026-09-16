package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;

/**
 * A permanent enhancement that can be applied to a unique item instance, stored per-copy rather
 * than in the catalog template. A copy carries up to {@code
 * ItemWeightClass#getMaximumImprovements()} of them ({@code Item#getImprovements()}).
 *
 * <p>Two authored catalogs, one per Aprimoramento list in {@code docs/rules/equipamentos.txt}:
 * {@link DefensiveImprovement} (fitted through the {@link ItemImprovement} wrapper, which holds
 * its creation-time choices) and {@link OffensiveImprovement} (fitted bare — no offensive entry
 * has a choice this core can read).
 */
public interface Improvement {
    String getName();

    String getDescription();

    /**
     * This Aprimoramento's Raridade — the tier its install Grau de Dificuldade is read from
     * ({@code org.aventyrs.core.item.ItemRarity#getImprovementInstallDifficulty()}).
     */
    ItemRarity getRarity();

    /**
     * Which column of the "Preços de Aprimoramentos" table this one is priced from — Armas for
     * an Aprimoramento Ofensivo ({@link OffensiveImprovement}), Armaduras for a Defensivo ({@link
     * DefensiveImprovement}). Abstract rather than defaulted on purpose: a silent default would
     * have priced one catalog off the other's column, and the two differ at Comum, Raro and Épico.
     */
    EnhancementPriceCategory getPriceCategory();

    default int getPhysicalDefenseBonus() {
        return 0;
    }

    default int getMagicDefenseBonus() {
        return 0;
    }

    /**
     * The Ataque column both Aprimoramento tables print. <b>Read by nothing yet</b> — applying it
     * needs a roll pass scoped to the weapon the attack was actually delivered with, and {@code
     * AbstractSkillInteraction#sumEquipmentRollBonuses} scans the whole loadout indiscriminately,
     * so routing it through {@code ModifierType.ATAQUE_*_ROLL_BONUS} would let a sheathed weapon's
     * Obra-Prima sharpen a swing made with something else. Exact, authored data meanwhile.
     */
    default int getAttackBonus() {
        return 0;
    }

    /** The Danos column both Aprimoramento tables print — see {@link #getAttackBonus()}. */
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

    /**
     * This Aprimoramento's Preço in PE, from the "Preços de Aprimoramentos" table — its own
     * {@link #getRarity()} (never its host Obra-Prima's) read down this Aprimoramento's {@link
     * #getPriceCategory()} column. Summed into a forge's worth by {@code
     * ItemForgery#getTotalValue()}.
     */
    default int getPriceModifier() {
        return EnhancementPricing.improvementPrice(getRarity(), getPriceCategory());
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
     * the "Não pode ser desarmado" Característica Adicional of {@link
     * OffensiveImprovement#MANOPLA_DE_SEGURANCA}, the one constant that states it. False by
     * default, and what {@code Weapon#isDisarmable()} consults.
     */
    default boolean preventsDisarming() {
        return false;
    }

    /**
     * How many Dano Base scale-ups this improvement grants when weapon is the attack source.
     *
     * <p><b>An offensive entry is only ever asked about its own host.</b> "Dano Base da Arma
     * aumenta em +1" means the weapon the Aprimoramento is fitted to, and {@code
     * Item#resolveEnhancementDamageBaseIncrease} enforces that before delegating here. A defensive
     * entry is asked about every weapon its wearer swings, which is what {@link
     * DefensiveImprovement#BENCAO_SELVAGEM}'s Armas Naturais clause needs.
     */
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
