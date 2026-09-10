package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Lâminas Pesadas catalog — one constant per {@link ItemCategory#HEAVY_BLADE} entry of
 * {@code docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Lâmina Pesada" table.
 * Same {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>All six are Adjacente, {@link SkillType#ATAQUE_CORPO_A_CORPO}. <b>Every Favor is prose
 * only</b>: they are "Dano Base muda para …" replacements (no Favor-driven Dano Base reader),
 * "Margem Crítica Menor muda para 16/15" shifts (no reader), and "Vantagem nas rolagens de
 * Danos Críticos" (crit dano unmodeled). {@link #getDamageBase()} and {@link
 * #getLesserCriticalMargin()} keep each weapon's authored table value.
 */
@Getter
public enum HeavyBladeItem implements ItemTemplate, Weapon {

    /**
     * Espada Bastarda ou Kodachi (Médio/Raro) — Preço 7. Dano 1d6+2, Tipo Corte, Efeito Crítico
     * Dilacerar (17), Alcance Adjacente, Requisito Nenhum. Favor Nenhum → {@code null}, Efeitos
     * Adicionais Nenhum.
     */
    ESPADA_BASTARDA_OU_KODACHI(
            "Espada Bastarda ou Kodachi",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            7,
            DamageBase.of(1, 2),
            CriticalEffectType.DILACERAR,
            17,
            null),

    /**
     * Espada Longa ou Katana (Pesada/Incomum) — Preço 10. Dano 1d6+3, Tipo Corte, Efeito
     * Crítico Desmembrar (17), Alcance Adjacente, Requisito Força 4.
     *
     * <p><b>Favor</b> "Dano Base muda para 2d6" — prose (no Favor-driven Dano Base reader).
     */
    ESPADA_LONGA_OU_KATANA(
            "Espada Longa ou Katana",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.UNCOMMON,
            10,
            DamageBase.of(1, 3),
            CriticalEffectType.DESMEMBRAR,
            17,
            ItemFavor.builder()
                    .description("Dano Base muda para 2d6.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .build()),

    /**
     * Foice de Batalha (Pesado/Incomum) — Preço 10. Dano 1d6+2, Tipo Corte, Efeito Crítico
     * Dilacerar (17), Alcance Adjacente, Requisito Destreza 3 ou Força 3.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16" — prose. <b>Efeitos Adicionais</b>
     * "Permite aumentar o Tempo de Ação de rolagens de Ataque Corpo-a-Corpo em +1PA para
     * aumentar a Margem Crítica em +1" — a PA-for-crit-margin trade with no reader.
     */
    FOICE_DE_BATALHA(
            "Foice de Batalha",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.UNCOMMON,
            10,
            DamageBase.of(1, 2),
            CriticalEffectType.DILACERAR,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3, AttributeDomain.STRENGTH))
                    .additionalEffects("Permite aumentar o Tempo de Ação de rolagens de Ataque "
                            + "Corpo-a-Corpo em +1PA para aumentar a Margem Crítica em +1.")
                    .build()),

    /**
     * Machado de Guerra (Pesado/Comum) — Preço 12. Dano 2d6, Tipo Corte, Efeito Crítico
     * Desmembrar (17), Alcance Adjacente, Requisito Força 4.
     *
     * <p><b>Favor</b> "Vantagem nas Rolagens de Danos Críticos" — prose (crit dano unmodeled).
     * <b>Efeitos Adicionais</b> "Pode ser usado como arma de Arremesso – Alvo Único Distância
     * Curta; quando arremessado o Dano Base muda para 1d6+3" — unmodeled thrown mode.
     */
    MACHADO_DE_GUERRA(
            "Machado de Guerra",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.COMMON,
            12,
            DamageBase.of(2, 0),
            CriticalEffectType.DESMEMBRAR,
            17,
            ItemFavor.builder()
                    .description("Vantagem nas Rolagens de Danos Críticos.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .additionalEffects("Pode ser usado como arma de Arremesso – Alvo Único "
                            + "Distância Curta; quando arremessado o Dano Base da Arma muda "
                            + "para 1d6+3.")
                    .build()),

    /**
     * Mata-Dragão (Pesado/Mítico) — Preço 20. Dano 3d6, Tipo Esmagamento, Efeito Crítico
     * Desmembrar (17), Alcance Adjacente, Requisito Força 6.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 15" — prose. <b>Efeitos Adicionais</b>
     * "Apenas personagens com Força 4 ou superior podem empunhá-la; exige uso de ambas as mãos
     * e o Tempo de Ação dos ataques é aumentado em +1PA".
     */
    MATA_DRAGAO(
            "Mata-Dragão",
            "Uma espada enorme, larga e maciça, de proporções surreais. Normalmente usada em "
                    + "rituais ou festivais por sua beleza e imponência, mas considerada "
                    + "impossível de empunhar em combate.",
            ItemWeightClass.HEAVY,
            ItemRarity.MYTHIC,
            20,
            DamageBase.of(3, 0),
            CriticalEffectType.DESMEMBRAR,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 15.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 6))
                    .additionalEffects("Apenas personagens com Força 4 ou superior podem "
                            + "empunhá-la. Exige uso de ambas as mãos para atacar e o Tempo de "
                            + "Ação dos ataques é aumentado em +1PA.")
                    .build()),

    /**
     * Montante ou Odachi (Pesado/Raro) — Preço 15. Dano 2d6, Tipo Corte, Efeito Crítico
     * Desmembrar (17), Alcance Adjacente, Requisito Força 4.
     *
     * <p><b>Favor</b> "Dano Base muda para 2d6+1" — prose (no Favor-driven Dano Base reader).
     */
    MONTANTE_OU_ODACHI(
            "Montante ou Odachi",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            15,
            DamageBase.of(2, 0),
            CriticalEffectType.DESMEMBRAR,
            17,
            ItemFavor.builder()
                    .description("Dano Base muda para 2d6+1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .build());

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final DamageBase damageBase;
    private final CriticalEffectType criticalEffect;
    private final int lesserCriticalMargin;
    private final ItemFavor favor;

    HeavyBladeItem(final String name, final String description, final ItemWeightClass weightClass,
                   final ItemRarity rarity, final int price, final DamageBase damageBase,
                   final CriticalEffectType criticalEffect, final int lesserCriticalMargin,
                   final ItemFavor favor) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.damageBase = damageBase;
        this.criticalEffect = criticalEffect;
        this.lesserCriticalMargin = lesserCriticalMargin;
        this.favor = favor;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.HEAVY_BLADE;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /** Every Lâmina Pesada is Adjacente — the default {@link Range} a corpo-a-corpo weapon carries. */
    @Override
    public Range getRange() {
        return Range.ADJACENTE;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a heavy blade grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a heavy blade grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a heavy blade carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
