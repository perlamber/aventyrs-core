package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Botas catalog — one constant per {@link ItemCategory#BOOTS} item in {@code
 * docs/rules/equipamentos.txt}, mirroring {@link ArmorItem}'s one-enum-per-category shape.
 *
 * <p>Botas are <b>not</b> {@link CriticallyDefensiveItem}: "apenas Armaduras e Escudos recebem
 * Efeitos Críticos Defensivos".
 *
 * <p>Most Botas Favores name a movement axis this core does not model separately — vertical /
 * climbing / swimming distance, the Reposicionar manoeuvre, or "which Rodada is this". Those
 * clauses stay in the Favor's {@code description} / {@code additionalEffects}. What <i>is</i>
 * expressible is a flat {@link ModifierType#MOVEMENT} bump (per Ponto de Ação, per CLAUDE.md's
 * "every movement figure is per Ponto de Ação"), now that {@code MovementServiceImpl} scans an
 * item's Favor for it, and a Vantagem on a whole named Perícia.
 */
@Getter
public enum BootsItem implements ItemTemplate {

    /**
     * Botas do Aventureiro (Leve/Comum). The Favor line ("Bônus em Movimento Base muda para
     * +2UD na primeira Rodada") <i>replaces</i> the Efeito Adicional's per-Rodada increase on
     * the first movement of the Rodada — a per-movement-index rewrite with no expression here
     * ({@code MovementService} has the per-movement axis but nothing rewrites an existing bonus
     * on it), so it stays as description text. The Efeito Adicional itself ("Movimento Base
     * todos +1") is a plain {@link ModifierType#MOVEMENT} 1, granted for real.
     */
    BOTAS_DO_AVENTUREIRO(
            "Botas do Aventureiro",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            2, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Bônus em Movimento Base muda para +2UD na primeira Rodada.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.MOVEMENT, 1))
                    .build()),

    /**
     * Botinas de Escalada (Leve/Incomum). Neither clause is expressible: the Favor's Movimento
     * Base Vertical / de escalada is a movement axis this core does not separate from ground
     * movement, and the Efeito Adicional's "Vantagem nas rolagens de Atletismo para Escalar" is
     * purpose-scoped ("para Escalar"), which CLAUDE.md's "never tracks what a roll is for" rules
     * out — a Vantagem on all of Atletismo would over-grant.
     */
    BOTINAS_DE_ESCALADA(
            "Botinas de Escalada",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            4, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Movimento Base Vertical e de escalada aumentam em +2UD.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .additionalEffects("Vantagem nas rolagens de Atletismo para Escalar.")
                    .build()),

    /**
     * Grevas dos Anões (Pesada/Épico). Both clauses are gated on "enquanto em movimento", a
     * per-Rodada state this core does not track (see CLAUDE.md's Movimento Base gap: a
     * movement's distance and direction are not recorded), so the +1 → +2 Defesa while moving
     * stays as text. Its flat DF/DM +1 columns are real.
     */
    GREVAS_DOS_ANOES(
            "Grevas dos Anões",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.EPIC,
            12, 1, 1, 24, 0,
            ItemFavor.builder()
                    .description("Bônus nas Defesas enquanto em movimento muda para +2.")
                    .requirements(new ItemRequirements(AttributeDomain.VIGOR, 3))
                    .additionalEffects("Bônus de +1 nas Defesas enquanto em movimento.")
                    .build()),

    /**
     * Nadadeiras Deciembranas (Leve/Raro). Movimento Base de Natação is a movement axis this
     * core does not model, and "Vantagem nas rolagens de Atletismo para Natação" is
     * purpose-scoped — both stay as text.
     */
    NADADEIRAS_DECIEMBRANAS(
            "Nadadeiras Deciembranas",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            6, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Movimento Base de Natação +2UD.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .additionalEffects("Vantagem nas rolagens de Atletismo para Natação.")
                    .build()),

    /**
     * Sandálhas do Corredor (Leve/Incomum). The Favor's "Distância da ação Reposicionar-se
     * aumenta em +1UD" has no expression — Reposicionar is an unmodelled manoeuvre (CLAUDE.md
     * "Forced movement / positioning"). The Efeito Adicional's "Movimento Base (terrestre) +2UD"
     * is a plain {@link ModifierType#MOVEMENT} 2: this core's Movimento Base is already the
     * ground figure, with no per-medium split, so "terrestre" adds nothing to narrow.
     */
    SANDALHAS_DO_CORREDOR(
            "Sandálhas do Corredor",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            6, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Distância da ação Reposicionar-se aumenta em +1UD.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.MOVEMENT, 2))
                    .build()),

    /**
     * Sandálhas dos Pequeninos (Leve/Raro) — both halves land: the Favor's "Movimento Base
     * (todos) +1" is a plain {@link ModifierType#MOVEMENT} 1, and the Efeito Adicional's
     * "Vantagem nas rolagens de Furtividade" is a Vantagem on the whole Perícia (not
     * purpose-scoped), a flat {@link Skill#ADVANTAGE_BONUS} on {@link
     * ModifierType#FURTIVIDADE_ROLL_BONUS}.
     */
    SANDALHAS_DOS_PEQUENINOS(
            "Sandálhas dos Pequeninos",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            8, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Movimento Base (todos) aumenta em +1.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.MOVEMENT, 1))
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

    BootsItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.BOOTS;
    }
}
