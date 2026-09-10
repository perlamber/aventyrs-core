package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Lâminas Leves catalog — one constant per {@link ItemCategory#LIGHT_BLADE} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Lâmina Leve" table. Same
 * {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>All six are Adjacente, {@link SkillType#ATAQUE_CORPO_A_CORPO}. Only {@link
 * #ESPADA_GANCHO_OU_SAI}'s "DF +1" Efeito Adicional maps to an {@link ItemBonus} ({@link
 * ModifierType#PHYSICAL_DEFENSE} 1); the Margem-Crítica-Menor Favores ("muda para 16") stay
 * prose — no reader applies an item-granted crit-margin shift, so {@link
 * #getLesserCriticalMargin()} keeps each weapon's authored table value.
 */
@Getter
public enum LightBladeItem implements ItemTemplate, Weapon {

    /**
     * Adaga, Kunai ou Seax (Leve/Comum) — Preço 3. Dano 1d6+1, Tipo Perfurante, Efeito Crítico
     * Sangramento (17), Alcance Adjacente, Requisito Nenhum. Favor Nenhum → {@code null}.
     *
     * <p><b>Efeitos Adicionais</b> "Pode ser utilizado como uma Arma de Arremesso – Alvo Único
     * Distância Curta" is unmodeled — one {@link Weapon} carries one Perícia and one Dano Base
     * (see {@code Weapon#getSkillType()}); the thrown mode has no expression.
     */
    ADAGA_KUNAI_OU_SEAX(
            "Adaga, Kunai ou Seax",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            3,
            DamageBase.of(1, 1),
            CriticalEffectType.SANGRAMENTO,
            17,
            null),

    /**
     * Espada Curta ou Wakizashi (Leve/Comum) — Preço 5. Dano 1d6+2, Tipo Corte, Efeito Crítico
     * Dilacerar (17), Alcance Adjacente, Requisito Nenhum. Favor Nenhum → {@code null}, Efeitos
     * Adicionais Nenhum.
     */
    ESPADA_CURTA_OU_WAKIZASHI(
            "Espada Curta ou Wakizashi",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            5,
            DamageBase.of(1, 2),
            CriticalEffectType.DILACERAR,
            17,
            null),

    /**
     * Espada Gancho ou Sai (Leve/Raro) — Preço 13. Dano 1d6+1, Tipo Perfurante, Efeito Crítico
     * Sangramento (17), Alcance Adjacente, Requisito Nenhum.
     *
     * <p><b>Favor</b> Corrente de Efeitos "Prender a Arma" (alvo sofre Desvantagem nas rolagens
     * de Ataque e Danos por 1 Rodada; Armas Naturais são imunes) is prose — no item-granted
     * Corrente reader. <b>Efeitos Adicionais</b> "DF +1" is a real {@link
     * ModifierType#PHYSICAL_DEFENSE} 1, applied unconditionally (no Requisito on this weapon),
     * so it is modeled as a Favor with {@code requirements = null}.
     */
    ESPADA_GANCHO_OU_SAI(
            "Espada Gancho ou Sai",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            13,
            DamageBase.of(1, 1),
            CriticalEffectType.SANGRAMENTO,
            17,
            ItemFavor.builder()
                    .description("Ataques recebem a Corrente de Efeitos – Prender a Arma: o "
                            + "alvo sofre Desvantagem nas Rolagens de Perícia de Ataque e "
                            + "Danos por 1 Rodada (Armas Naturais são imunes). Efeito "
                            + "Adicional: DF +1.")
                    .bonus(new ItemBonus(ModifierType.PHYSICAL_DEFENSE, 1))
                    .build()),

    /**
     * Florete ou Sabre (Leve/Incomum) — Preço 11. Dano 1d6+2, Tipo Perfurante, Efeito Crítico
     * Sangramento (17), Alcance Adjacente, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16" — prose (no crit-margin-shift
     * reader). <b>Efeitos Adicionais</b> "Pode receber o Aprimoramento Guarda Mãos mesmo se não
     * for Obra-Prima …".
     */
    FLORETE_OU_SABRE(
            "Florete ou Sabre",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            11,
            DamageBase.of(1, 2),
            CriticalEffectType.SANGRAMENTO,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Pode receber o Aprimoramento Guarda Mãos mesmo se não "
                            + "for uma Obra-Prima (o preço do Aprimoramento é pago "
                            + "normalmente); versões Obras-Primas sempre o recebem.")
                    .build()),

    /**
     * Foice de Mão (Leve/Incomum) — Preço 8. Dano 1d6+1, Tipo Corte, Efeito Crítico Dilacerar
     * (17), Alcance Adjacente, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16" — prose. <b>Efeitos Adicionais</b>
     * "Pode receber o Aprimoramento Corrente com Peso mesmo se não for Obra-Prima …".
     */
    FOICE_DE_MAO(
            "Foice de Mão",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            8,
            DamageBase.of(1, 1),
            CriticalEffectType.DILACERAR,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Pode receber o Aprimoramento Corrente com Peso mesmo se "
                            + "não for uma Obra-Prima (o preço do Aprimoramento é pago "
                            + "normalmente).")
                    .build()),

    /**
     * Machado de Mão (Leve/Comum) — Preço 6. Dano 1d6+2, Tipo Corte, Efeito Crítico Dilacerar
     * (17), Alcance Adjacente, Requisito Nenhum. Favor Nenhum → {@code null}.
     *
     * <p><b>Efeitos Adicionais</b> "Pode ser usado como arma de Arremesso – Alvo Único
     * Distância Curta; quando arremessado o Dano Base muda para 1d6+1" — unmodeled thrown mode.
     */
    MACHADO_DE_MAO(
            "Machado de Mão",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            6,
            DamageBase.of(1, 2),
            CriticalEffectType.DILACERAR,
            17,
            null);

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final DamageBase damageBase;
    private final CriticalEffectType criticalEffect;
    private final int lesserCriticalMargin;
    private final ItemFavor favor;

    LightBladeItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.LIGHT_BLADE;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /** Every Lâmina Leve is Adjacente — the default {@link Range} a corpo-a-corpo weapon carries. */
    @Override
    public Range getRange() {
        return Range.ADJACENTE;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a light blade grants no Defesa Física column (Espada Gancho's +1 is its Favor). */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a light blade grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a light blade carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
