package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Capas catalog — one constant per {@link ItemCategory#CLOAK} item in {@code
 * docs/rules/equipamentos.txt}, mirroring {@link ArmorItem}'s one-enum-per-category shape.
 *
 * <p>Capas are <b>not</b> {@link CriticallyDefensiveItem}.
 *
 * <p>Three Capas Favores are blocked on Resistência Elemental, for which no {@link ModifierType}
 * exists (nothing grants a plain RE that isn't scoped to one {@code ElementalType} — see
 * {@link ModifierType#MAGIC_REDUCTION}'s javadoc), and two more on a magic-damage/heal effect
 * bonus this core has no stat for; those stay as description text. What lands: a plain RD
 * ({@link #MANTO_NORTENHO}), an RM ({@link #SOBRETUDO_DO_INQUISIDOR}'s Efeito Adicional), and a
 * Vantagem on a whole named Perícia.
 */
@Getter
public enum CloakItem implements ItemTemplate {

    /**
     * Capa do Viajante (Leve/Comum). "Resistência Elemental: Fogo e Frio" has no {@link
     * ModifierType} — RE is unmodelled — and "efeitos climáticos" is a weather system this core
     * lacks entirely, so both clauses stay as text.
     */
    CAPA_DO_VIAJANTE(
            "Capa do Viajante",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            2, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Resistência Elemental: Fogo e Frio.")
                    .requirements(new ItemRequirements(AttributeDomain.VIGOR, 3))
                    .additionalEffects("Protege o personagem de efeitos climáticos variáveis, "
                            + "mas não extremos.")
                    .build()),

    /**
     * Capa Esvoaçante (Leve/Incomum) — "Concede Vantagem em rolagens de Persuasão" is a Vantagem
     * on the whole Perícia (no purpose scope), a flat {@link Skill#ADVANTAGE_BONUS} on {@link
     * ModifierType#PERSUASAO_ROLL_BONUS}, now that {@code AbstractSkillInteraction} scans an
     * item's Favor for a Perícia roll bonus.
     */
    CAPA_ESVOACANTE(
            "Capa Esvoaçante",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            8, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Concede Vantagem em rolagens de Persuasão.")
                    .requirements(new ItemRequirements(AttributeDomain.CHARISMA, 3))
                    .bonus(new ItemBonus(ModifierType.PERSUASAO_ROLL_BONUS, Skill.ADVANTAGE_BONUS))
                    .build()),

    /**
     * Gabardina Élfica (Leve/Raro). Both clauses scale magic-damage and magic-healing effect
     * bonuses, a stat this core does not model (no {@link ModifierType} for it), so both stay as
     * text. Its Conjuração +1 column is real.
     */
    GABARDINA_ELFICA(
            "Gabardina Élfica",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            11, 0, 0, 20, 1,
            ItemFavor.builder()
                    .description("Bônus em danos e curas mágicas mudam para +2.")
                    .requirements(new ItemRequirements(AttributeDomain.FOCUS, 3))
                    .additionalEffects("Efeitos de danos e curas mágicas aumentam em +1.")
                    .build()),

    /**
     * Manto Nortenho (Médio/Raro) — "Dano Físico sofrido é reduzido em -1" is a real {@link
     * ModifierType#DAMAGE_REDUCTION} 1, carrying the same damage-type simplification {@link
     * ArmorItem#ARMADURA_COMPLETA}'s "de Corte" Favor documents: {@code DamageType} has no
     * Físico/Mágico breakdown that RD resolution reads, so it's modeled as plain RD.
     */
    MANTO_NORTENHO(
            "Manto Nortenho",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            10, 1, 0, 24, 0,
            ItemFavor.builder()
                    .description("Dano Físico sofrido é reduzido em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.VIGOR, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1))
                    .build()),

    /**
     * Poncho do Aventureiro (Leve/Comum). "Resistência Elemental: Todas" has no {@link
     * ModifierType} — RE is unmodelled — so the Favor stays as text; its DF +1 column is real.
     */
    PONCHO_DO_AVENTUREIRO(
            "Poncho do Aventureiro",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            6, 1, 0, 20, 0,
            ItemFavor.builder()
                    .description("Resistência Elemental: Todas.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .build()),

    /**
     * Sobretudo do Inquisidor (Leve/Incomum). The Favor line reduces the Duração of hostile
     * Encantamentos/Maldições — no duration-reduction mechanism exists (only {@code
     * SpellDurationService} extension), so it stays as text. The Efeito Adicional's "Dano
     * Mágico sofrido reduzido em -1" is a real {@link ModifierType#MAGIC_REDUCTION} 1 (see that
     * constant's javadoc — RM applies against Dano Mágico não-PRIMORDIAL).
     */
    SOBRETUDO_DO_INQUISIDOR(
            "Sobretudo do Inquisidor",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            7, 0, 1, 20, 1,
            ItemFavor.builder()
                    .description("Duração de Encantamentos e Maldições reduzidas em -1 Rodada, "
                            + "não reduz duração de Magias Elementais: Fogo, Sagradas e Primordiais.")
                    .requirements(new ItemRequirements(AttributeDomain.FOCUS, 3))
                    .bonus(new ItemBonus(ModifierType.MAGIC_REDUCTION, 1))
                    .build()),

    /**
     * Veste Sombria (Leve/Incomum). "Vantagem nas rolagens de Ladinice" has no expression —
     * Ladinice is not a {@code SkillType} in this core — so the Favor line stays as text. The
     * Efeito Adicional's "Vantagem nas rolagens de Furtividade" is a flat {@link
     * Skill#ADVANTAGE_BONUS} on {@link ModifierType#FURTIVIDADE_ROLL_BONUS}.
     */
    VESTE_SOMBRIA(
            "Veste Sombria",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            9, 0, 0, 20, 1,
            ItemFavor.builder()
                    .description("Concede ao usuário Vantagem nas rolagens de Ladinice.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.FURTIVIDADE_ROLL_BONUS, Skill.ADVANTAGE_BONUS))
                    .build());

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

    CloakItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.CLOAK;
    }
}
