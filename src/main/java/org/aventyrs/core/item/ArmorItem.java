package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Armaduras catalog — one constant per {@link ItemCategory#ARMOR} item, mirroring
 * {@code org.aventyrs.core.feat.ArtesMarciaisFeat}'s one-enum-per-category shape (and, like
 * it, holding the rules text as the single source of truth rather than duplicating it).
 * A sibling enum per {@link ItemCategory} follows the same shape as more items are supplied.
 */
@Getter
public enum ArmorItem implements Item {

    /**
     * Armadura Completa (Pesado/Raro). Its Conjuração column reads "Desvantagem" rather than a
     * number, which is exactly {@link Skill#DISADVANTAGE_MALUS} — this is the first real,
     * unconditional Desvantagem in the ruleset, and what motivated adding that constant.
     *
     * <p>Its Favor, at Força 3, is a real Redução de Dano 2 ({@link
     * ModifierType#DAMAGE_REDUCTION}, which {@code DamageService#getTotalDamageReduction}
     * already sums for real), not merely rules text. The text scopes it to Dano de Corte
     * specifically; this core has no damage-type-scoped mitigation ({@code DamageType} has no
     * Corte/Perfuração/Impacto breakdown under {@code FISICO}, and RD/RA are resolved with no
     * notion of damage type at all), so it's modeled as plain RD — the same explicit
     * "document the simplification rather than silently narrowing or over-granting" treatment
     * scoped Vantagem bonuses get elsewhere. "Efeitos Adicionais: Nenhum" is modeled as no
     * additional effects at all, not an empty string.
     */
    ARMADURA_COMPLETA(
            "Armadura Completa",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            20,
            5,
            2,
            32,
            Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Dano de Corte sofrido é reduzido em -2.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                    .build()),

    /**
     * Armadura de Justa (Pesado/Épico) — the first item whose Raridade is {@link
     * ItemRarity#EPIC}, which is what added that constant (the enum previously modeled only
     * the three tiers rules text had confirmed).
     *
     * <p>Its Favor, at Força 4, is two clauses, and only one of them is expressible today:
     * <ul>
     *   <li>"Dano de Corte e Perfuração sofrido é reduzido em -2" is a real {@link
     *   ModifierType#DAMAGE_REDUCTION} 2, carrying the identical damage-type simplification
     *   {@link #ARMADURA_COMPLETA}'s own Corte-scoped Favor already documents — {@code
     *   DamageType} has no Corte/Perfuração/Impacto breakdown, and RD is resolved with no
     *   notion of damage type at all, so it's modeled as plain RD rather than silently
     *   narrowed or over-granted.</li>
     *   <li>"Movimento Base reduzido à metade" contributes no {@link ItemBonus} at all and
     *   lives on in the Favor's own description text. Halving isn't a shape {@link ItemBonus}
     *   can express — it's a flat {@link ModifierType}-and-value pair, and {@code
     *   org.aventyrs.core.character.services.MovementService#getTotalMovement} sums {@code
     *   ModifierType#MOVEMENT} additively with no multiplicative step anywhere (unlike {@code
     *   DamageService}, which does have a real halving stage for {@code
     *   ModifierType#HALF_DAMAGE} to feed). Don't add a {@code MOVEMENT_HALVED} constant
     *   speculatively: unlike {@code HALF_DAMAGE}, there is no consumer to read it, so it
     *   would be inert in a way even {@code ModifierType#DEFESAS} (a real registry entry
     *   nothing reads *yet*) isn't — the mechanism itself is what's missing, not just the
     *   reader.</li>
     * </ul>
     *
     * <p>Its Conjuração column reads "Desvantagem", exactly as {@link #ARMADURA_COMPLETA}'s
     * does, and its Descrição and "Efeitos Adicionais: Nenhum" are both empty/absent.
     */
    ARMADURA_DE_JUSTA(
            "Armadura de Justa",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.EPIC,
            24,
            4,
            4,
            35,
            Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Dano de Corte e Perfuração sofrido é reduzido em -2, "
                            + "Movimento Base reduzido à metade.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                    .build()),

    /**
     * Armadura de Gladiador (Leve/Comum). "Favor: Nenhum" is exactly a {@code null} {@link
     * ItemFavor} — the item grants nothing conditional at all, so there is no Requisito to
     * meet either (the two always travel together: Requisitos exist only to gate a Favor).
     * {@link Item#grantsFavorTo(org.aventyrs.core.character.Character)} is permanently {@code
     * false} for it and {@link Item#resolveFavorBonus} permanently 0, with no null-checking
     * needed at any call site.
     *
     * <p>Its Conjuração column reads "-" rather than a number or "Desvantagem", which is 0:
     * the armour neither helps nor hinders conjuração. Contrast {@link #ARMADURA_COMPLETA},
     * where "Desvantagem" is a real {@link Skill#DISADVANTAGE_MALUS}.
     */
    ARMADURA_DE_GLADIADOR(
            "Armadura de Gladiador",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            5,
            2,
            1,
            24,
            0,
            null);

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final int physicalDefenseBonus;
    private final int magicDefenseBonus;
    private final int hardness;
    private final int castingBonus;
    private final ItemFavor favor;

    ArmorItem(final String name, final String description, final ItemWeightClass weightClass,
              final ItemRarity rarity, final int price, final int physicalDefenseBonus,
              final int magicDefenseBonus, final int hardness, final int castingBonus,
              final ItemFavor favor) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.physicalDefenseBonus = physicalDefenseBonus;
        this.magicDefenseBonus = magicDefenseBonus;
        this.hardness = hardness;
        this.castingBonus = castingBonus;
        this.favor = favor;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ARMOR;
    }
}
