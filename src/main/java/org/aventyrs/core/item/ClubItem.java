package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Clavas catalog — one constant per {@link ItemCategory#CLUB} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Clava" table. Same
 * {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>All five are Adjacente, {@link SkillType#ATAQUE_CORPO_A_CORPO}. Only {@link #BORDAO_OU_BO}'s
 * Favor is expressible ("Concede Bônus de +2 as Defesas do usuário" → {@link
 * ModifierType#DEFESAS} 2, which {@code DefenseService} sums from equipped items). The
 * Danos-Críticos Favores stay prose — a critical hit's dano is not modeled at all in this core
 * (CLAUDE.md's "A critical hit grants Vantagem em Danos" bullet), so neither a "+1d6" nor a
 * "Vantagem nas rolagens de Danos Críticos" has anything to attach to.
 */
@Getter
public enum ClubItem implements ItemTemplate, Weapon {

    /**
     * Bordão ou Bo (Médio/Comum) — Preço 7. Dano 1d6+2, Tipo Esmagamento, Efeito Crítico
     * Atordoante (17), Alcance Adjacente, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Concede Bônus de +2 as Defesas do usuário" is a real {@link
     * ModifierType#DEFESAS} 2. <b>Efeitos Adicionais</b> "Versões Obras-Primas podem receber
     * Aprimoramentos ofensivos ou defensivos. Exige o uso de ambas as mãos para atacar" — prose.
     */
    BORDAO_OU_BO(
            "Bordão ou Bo",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            7,
            DamageBase.of(1, 2),
            CriticalEffectType.ATORDOANTE,
            17,
            ItemFavor.builder()
                    .description("Concede Bônus de +2 as Defesas do usuário.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.DEFESAS, 2))
                    .additionalEffects("Versões Obras-Primas podem receber Aprimoramentos de "
                            + "Equipamentos ofensivos ou defensivos. Exige o uso de ambas as "
                            + "mãos para atacar.")
                    .build()),

    /**
     * Maça ou Mangual (Leve/Comum) — Preço 6. Dano 1d6+1, Tipo Esmagamento, Efeito Crítico
     * Atordoante (17), Alcance Adjacente, Requisito Força 3.
     *
     * <p><b>Favor</b> "Vantagem nas rolagens de Danos Críticos" — prose (crit dano unmodeled).
     */
    MACA_OU_MANGUAL(
            "Maça ou Mangual",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            6,
            DamageBase.of(1, 1),
            CriticalEffectType.ATORDOANTE,
            17,
            ItemFavor.builder()
                    .description("Vantagem nas rolagens de Danos Críticos.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .build()),

    /**
     * Maça de Guerra ou Mangual Pesado (Médio/Incomum) — Preço 11. Dano 1d6+3, Tipo
     * Esmagamento, Efeito Crítico Atordoante (17), Alcance Adjacente, Requisito Força 3.
     *
     * <p><b>Favor</b> "Danos Críticos +1d6" — prose (crit dano unmodeled).
     */
    MACA_DE_GUERRA_OU_MANGUAL_PESADO(
            "Maça de Guerra ou Mangual Pesado",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.UNCOMMON,
            11,
            DamageBase.of(1, 3),
            CriticalEffectType.ATORDOANTE,
            17,
            ItemFavor.builder()
                    .description("Danos Críticos +1d6.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .build()),

    /**
     * Martelo de Guerra ou Tetsubo (Pesado/Raro) — Preço 14. Dano 1d6+3, Tipo Esmagamento,
     * Efeito Crítico Estilhaçador (17), Alcance Adjacente, Requisito Força 4.
     *
     * <p><b>Favor</b> "Dano Base da Arma muda para 2d6+1" is prose: a Favor that <em>replaces</em>
     * the Dano Base column has no reader (the Obra-Prima "muda para" hooks are per-copy, not a
     * Favor). {@link #getDamageBase()} stays the table's 1d6+3. <b>Efeitos Adicionais</b>
     * "Personagens com Força 3 ou inferior precisam utilizar ambas as mãos para atacar".
     */
    MARTELO_DE_GUERRA_OU_TETSUBO(
            "Martelo de Guerra ou Tetsubo",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            14,
            DamageBase.of(1, 3),
            CriticalEffectType.ESTILHACADOR,
            17,
            ItemFavor.builder()
                    .description("Dano Base da Arma muda para 2d6+1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .additionalEffects("Personagens com Força 3 ou inferior precisam utilizar "
                            + "ambas as mãos para atacar.")
                    .build()),

    /**
     * Tonfa (Leve/Raro) — Preço 6. Dano 1d6+1, Tipo Esmagamento, Efeito Crítico Atordoante
     * (17), Alcance Adjacente, Requisito Destreza 4.
     *
     * <p><b>Favor</b> "Bônus cumulativo de +1 em DF para cada Tonfa empunhada" is prose: it
     * scales with how many copies are wielded, and this core keeps no per-hand tracking of
     * drawn weapons (CLAUDE.md's "no per-hand tracking" note), so a fixed {@link ItemBonus}
     * can't express it. <b>Efeitos Adicionais</b> "Versões Obras-Primas podem receber
     * Aprimoramentos ofensivos e defensivos".
     */
    TONFA(
            "Tonfa",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            6,
            DamageBase.of(1, 1),
            CriticalEffectType.ATORDOANTE,
            17,
            ItemFavor.builder()
                    .description("Bônus cumulativo de +1 em DF para cada Tonfa empunhada.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 4))
                    .additionalEffects("Versões Obras-Primas podem receber Aprimoramentos de "
                            + "Equipamentos ofensivos e defensivos.")
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

    ClubItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.CLUB;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /** Every Clava is Adjacente — the default {@link Range} a corpo-a-corpo weapon carries. */
    @Override
    public Range getRange() {
        return Range.ADJACENTE;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a clava grants no Defesa Física (Bordão's +2 is its Favor, not a column). */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a clava grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a clava carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
