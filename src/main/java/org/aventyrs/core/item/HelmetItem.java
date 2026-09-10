package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Protetores de Cabeça catalog — one constant per {@link ItemCategory#HELMET} item in {@code
 * docs/rules/equipamentos.txt} ("Capacetes e Elmos", "Chapéus", "Máscaras", "Tiaras"),
 * mirroring {@link ArmorItem}'s one-enum-per-category shape.
 *
 * <p>Protetores de Cabeça are <b>not</b> {@link CriticallyDefensiveItem}.
 *
 * <p>Most helmet Favores are blocked on systems this core lacks — Encantamento duration
 * reduction, a per-copy "+1 DF ou DM definido na fabricação" production choice, purpose-scoped
 * or target-scoped Vantagem — and stay as description / {@code additionalEffects} text. Two
 * land: a Vantagem on a whole named Perícia ({@link #MASCARA_PRIMAL}'s Empatia Selvagem).
 */
@Getter
public enum HelmetItem implements ItemTemplate {

    /**
     * Capacetes e Elmos Fechados (Médio/Comum). The Favor reduces hostile Encantamento Duração
     * — no duration-reduction mechanism exists — and the Efeito Adicional is a per-copy "+1 DF
     * <i>ou</i> DM" choice made at fabrication, which this core has no production-choice data
     * for (and a {@link ModifierType#DEFESAS} 1 would wrongly grant both). Both stay as text;
     * its flat DF/DM +1 columns are real.
     */
    CAPACETES_E_ELMOS_FECHADOS(
            "Capacetes e Elmos Fechados",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            7, 1, 1, 20, 0,
            ItemFavor.builder()
                    .description("Duração de Encantamentos hostis reduzida em 1 Rodada.")
                    .requirements(new ItemRequirements(AttributeDomain.VIGOR, 3))
                    .additionalEffects("Concede ao seu usuário Bônus de +1 em DF ou DM, definido "
                            + "na fabricação.")
                    .build()),

    /**
     * Chapéu Galante (Leve/Incomum). Both clauses are purpose-scoped Persuasão Vantagem ("para
     * Diplomacia", and the Favor widening it "para qualquer finalidade") — CLAUDE.md's "never
     * tracks what a roll is for" rules a purpose scope out, so both stay as text.
     */
    CHAPEU_GALANTE(
            "Chapéu Galante",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            8, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Vantagem concedida em rolagens de Persuasão muda de apenas "
                            + "Diplomacias para qualquer finalidade.")
                    .requirements(new ItemRequirements(AttributeDomain.CHARISMA, 3))
                    .additionalEffects("Vantagem em rolagens de Persuasão para Diplomacia.")
                    .build()),

    /**
     * Charme do Artesão (Médio/Raro). Its Requisitos read "Car 3/Gno 3" — either Atributo, the
     * {@code alternativeDomain} half of {@link ItemRequirements}. Both clauses are blocked: the
     * Favor's "-1PA on the first Artes/Conhecimentos roll each Rodada" needs both an
     * acquisition-time Perícia choice and a per-Rodada action-cost reduction, and the Efeito
     * Adicional is a fabrication-time Vantagem choice — neither has a carrier here.
     */
    CHARME_DO_ARTESAO(
            "Charme do Artesão",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            8, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Primeira rolagem de Artes ou Conhecimentos, conforme Perícia "
                            + "definida na criação, efetuada em cada Rodada tem o Tempo de Ação "
                            + "reduzido em -1PA.")
                    .requirements(new ItemRequirements(AttributeDomain.CHARISMA, 3, AttributeDomain.GNOSE))
                    .additionalEffects("Vantagem em rolagens de Artes ou Conhecimentos, benefício "
                            + "definido durante a fabricação.")
                    .build()),

    /**
     * Face do Bufão (Leve/Raro). "Vantagem em rolagens de Ladinice" has no expression (Ladinice
     * is not a {@code SkillType}), and the Efeito Adicional is a purpose-scoped, fabrication-time
     * Persuasão choice — both stay as text. Its Conjuração +1 column is real.
     */
    FACE_DO_BUFAO(
            "Face do Bufão",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            3, 0, 0, 20, 1,
            ItemFavor.builder()
                    .description("Vantagem em rolagens de Ladinice.")
                    .requirements(new ItemRequirements(AttributeDomain.CHARISMA, 3))
                    .additionalEffects("Vantagem em rolagens de Persuasão para Blefar ou Intimidar, "
                            + "benefício definido durante a fabricação.")
                    .build()),

    /**
     * Gorro do Mago (Leve/Épico). The Favor extends conjured Encantamento Duração — {@code
     * SpellDurationService} has an <i>enhancement</i> extension hook but no Favor one — and the
     * Efeito Adicional scales magic-damage/heal effects, a stat with no {@link ModifierType}.
     * Both stay as text; its DM +1 and Conjuração +1 columns are real.
     */
    GORRO_DO_MAGO(
            "Gorro do Mago",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.EPIC,
            8, 0, 1, 20, 1,
            ItemFavor.builder()
                    .description("Duração dos Encantamentos conjurados aumentam em +1 Rodada.")
                    .requirements(new ItemRequirements(AttributeDomain.FOCUS, 4))
                    .additionalEffects("Efeitos de danos e curas mágicas aumentam em +1.")
                    .build()),

    /**
     * Máscara Primal (Médio/Raro). The Favor's Persuasão Vantagem is target-scoped ("contra
     * Monstros") and stays as text, but the Efeito Adicional's "Vantagem nas rolagens de
     * Empatia Selvagem" is a Vantagem on the whole Perícia — a flat {@link Skill#ADVANTAGE_BONUS}
     * on {@link ModifierType#EMPATIA_SELVAGEM_ROLL_BONUS}.
     */
    MASCARA_PRIMAL(
            "Máscara Primal",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            4, 0, 0, 20, 1,
            ItemFavor.builder()
                    .description("Concede Vantagem nas rolagens de Persuasão efetuada contra Monstros.")
                    .requirements(new ItemRequirements(AttributeDomain.CHARISMA, 3))
                    .bonus(new ItemBonus(ModifierType.EMPATIA_SELVAGEM_ROLL_BONUS, Skill.ADVANTAGE_BONUS))
                    .build()),

    /**
     * Tiaras e Bandanas (Leve/Incomum) — "Favor: Nenhum", "Requisitos: Nenhum". The Efeito
     * Adicional ("+1 DF ou DM definido na fabricação") is a per-copy production choice with no
     * carrier here, so it stays as text. Modeled as a {@link ItemFavor} with {@code null}
     * requirements (an Efeito Adicional with no Favor to gate it applies unconditionally) and no
     * {@link ItemBonus}es — the one cataloged item whose {@code favor} exists purely to carry an
     * {@code additionalEffects} line.
     */
    TIARAS_E_BANDANAS(
            "Tiaras e Bandanas",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            3, 0, 0, 20, 0,
            ItemFavor.builder()
                    .description("Nenhum.")
                    .additionalEffects("Concede Bônus de +1 em DF ou DM, definido na fabricação.")
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

    HelmetItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.HELMET;
    }
}
