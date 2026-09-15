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

    /**
     * This Obra-Prima's Raridade — the tier its fabrication Grau de Dificuldade, its crafter's
     * minimum Graduação ({@code ItemRarity#getMinimumMasterpieceGraduation()}) and its Preço
     * ({@link #getPriceModifier()}) are all read from.
     */
    ItemRarity getRarity();

    /**
     * Which column of the "Preços de Obras-Primas" table this one is priced from — Armas for an
     * Obra-Prima Ofensiva, Armaduras for a Defensiva. Abstract rather than defaulted on purpose:
     * a silent default would price the unauthored offensive catalog off the armour column, and
     * the two differ at every tier but Incomum and Mítico.
     */
    EnhancementPriceCategory getPriceCategory();

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
     * The Ataque column of the "Obras-Primas Ofensivas" table. Absent from the Defensivas table
     * entirely — {@link DefensiveMasterpiece} authors {@code DF | DM | Conjuração | Requisitos}
     * and nothing else — so this is 0 for every defensive entry by construction rather than by
     * coincidence.
     *
     * <p><b>Read by nothing yet</b>, exactly like {@link Improvement#getAttackBonus()}: applying it
     * needs a roll pass scoped to the weapon the attack was actually delivered with, and {@code
     * AbstractSkillInteraction#sumEquipmentRollBonuses} scans the whole loadout indiscriminately.
     * Exact, authored data in the meantime, per CLAUDE.md's "can't apply it yet doesn't mean can't
     * compute it yet".
     */
    default int getAttackBonus() {
        return 0;
    }

    /** The Danos column of the "Obras-Primas Ofensivas" table — see {@link #getAttackBonus()}. */
    default int getDamageBonus() {
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
     * the "Não pode ser desarmado" Característica Adicional. False by default.
     *
     * <p>No <em>Obra-Prima</em> states it; the one constant that does is an Aprimoramento
     * ({@link OffensiveImprovement#MANOPLA_DE_SEGURANCA}, through {@link
     * Improvement#preventsDisarming()}). Declared on both halves so {@code Weapon#isDisarmable()}
     * asks one question of whatever is fitted, rather than knowing which layer the clause lives in.
     */
    default boolean preventsDisarming() {
        return false;
    }

    /**
     * How many Dano Base scale-ups this masterpiece grants when weapon is the attack source.
     *
     * <p><b>An offensive entry is only ever asked about its own host.</b> "Dano Base da Arma
     * aumenta em +1" means the weapon the Obra-Prima is fitted to, and {@code
     * Item#resolveEnhancementDamageBaseIncrease} enforces that before delegating here — so an
     * override need not (and cannot) compare the two itself. A defensive entry is asked about every
     * weapon its wearer swings, which is what {@link DefensiveImprovement#BENCAO_SELVAGEM}'s Armas
     * Naturais clause needs.
     */
    default int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        return 0;
    }

    /**
     * The number of Rodadas this masterpiece adds to the given Magia's resolved Duração — the twin
     * of {@link Improvement#resolveDurationIncreaseInRounds}, added for {@link
     * OffensiveMasterpiece#PODEROSA}'s "Magias tem … Duração +1". Read by {@code
     * org.aventyrs.core.magic.SpellDurationService}, through {@code
     * Item#resolveEnhancementDurationIncreaseInRounds}.
     */
    default int resolveDurationIncreaseInRounds(final org.aventyrs.core.magic.Spell spell,
                                                final Character character) {
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

    /**
     * This Obra-Prima's Preço in PE, from the "Preços de Obras-Primas" table — its {@link
     * #getRarity()} read down this Obra-Prima's own {@link #getPriceCategory()} column. Summed
     * into a forge's worth by {@code ItemForgery#getTotalValue()}.
     */
    default int getPriceModifier() {
        return EnhancementPricing.masterpiecePrice(getRarity(), getPriceCategory());
    }
}
