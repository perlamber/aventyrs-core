package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

/**
 * The Balestras catalog — one constant per {@link ItemCategory#CROSSBOW} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Balestra" table. Same
 * {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>Every Besta is de Ataque à Distância ({@link SkillType#ATAQUE_A_DISTANCIA}) and prints
 * "Projétil (16)" in its Efeito Crítico column — "Projétil" is not a {@link CriticalEffectType}
 * (see {@link BowItem}), so {@link #getCriticalEffect()} is {@code null} and only the
 * {@link #getLesserCriticalMargin()} of 16 survives from that column. The reload cost every
 * Besta carries ("Tempo de Ação da recarga 1PA") stays prose — this core has no reload model.
 *
 * <p>Only {@link #BESTA_PESADA}'s Favor is expressible: "Vantagem nas Rolagens de Ataque à
 * Distância" is a flat {@link Skill#ADVANTAGE_BONUS} on that whole Perícia, which {@code
 * AbstractSkillInteraction} sums from equipped items' Favores.
 */
@Getter
public enum CrossbowItem implements ItemTemplate, Weapon {

    /**
     * Besta de Mão (Leve/Raro) — Preço 17. Dano 1d6+3, Tipo Projétil, Efeito Crítico
     * "Projétil (16)", Alcance Médio, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Uma vez por Cena pode disparar como Ação Livre" is prose: a
     * once-per-Cena action-cost waiver has no reader. <b>Efeitos Adicionais</b> "Precisa ser
     * recarregada após cada disparo, Tempo de Ação da recarga 1PA" is the reload rule.
     */
    BESTA_DE_MAO(
            "Besta de Mão",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            17,
            DamageBase.of(1, 3),
            Range.DISTANCIA_MEDIA,
            16,
            ItemFavor.builder()
                    .description("Uma vez por Cena pode disparar como Ação Livre.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Precisa ser recarregada após cada disparo, Tempo de "
                            + "Ação da recarga 1PA.")
                    .build()),

    /**
     * Besta de Repetição (Médio/Mítico) — Preço 22. Dano 1d6+2, Tipo Projétil, Efeito Crítico
     * "Projétil (16)", Alcance Médio, Requisito Destreza 4.
     *
     * <p><b>Favor</b> "Tempo de Ação da recarga reduzido para 1PA" is prose (no reload model).
     * <b>Efeitos Adicionais</b> "Exige o uso de ambas as mãos … recarregada após 5 disparos,
     * Tempo de Ação da recarga 2PA".
     */
    BESTA_DE_REPETICAO(
            "Besta de Repetição",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.MYTHIC,
            22,
            DamageBase.of(1, 2),
            Range.DISTANCIA_MEDIA,
            16,
            ItemFavor.builder()
                    .description("Tempo de Ação da recarga reduzido para 1PA.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 4))
                    .additionalEffects("Exige o uso de ambas as mãos para atirar. Precisa ser "
                            + "recarregada após 5 disparos, Tempo de Ação da recarga 2PA.")
                    .build()),

    /**
     * Besta Pesada (Pesado/Incomum) — Preço 14. Dano 2d6+1, Tipo Projétil, Efeito Crítico
     * "Projétil (16)", Alcance Longo, Requisito Destreza 3 ou Força 3.
     *
     * <p><b>Favor</b> "Vantagem nas Rolagens de Ataque à Distância" is a real flat {@link
     * Skill#ADVANTAGE_BONUS} on the whole {@link SkillType#ATAQUE_A_DISTANCIA} Perícia — {@link
     * ModifierType#ATAQUE_A_DISTANCIA_ROLL_BONUS}, which {@code
     * AbstractSkillInteraction.sumEquipmentRollBonuses} already scans equipped Favores for.
     * <b>Efeitos Adicionais</b> "Exige o uso de ambas as mãos … recarregada após cada disparo,
     * Tempo de Ação da recarga 1PA".
     */
    BESTA_PESADA(
            "Besta Pesada",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.UNCOMMON,
            14,
            DamageBase.of(2, 1),
            Range.DISTANCIA_LONGA,
            16,
            ItemFavor.builder()
                    .description("Vantagem nas Rolagens de Ataque à Distância.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3, AttributeDomain.STRENGTH))
                    .bonus(new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS))
                    .additionalEffects("Exige o uso de ambas as mãos para atirar. Precisa ser "
                            + "recarregada após cada disparo, Tempo de Ação da recarga 1PA.")
                    .build());

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final DamageBase damageBase;
    private final Range range;
    private final int lesserCriticalMargin;
    private final ItemFavor favor;

    CrossbowItem(final String name, final String description, final ItemWeightClass weightClass,
                 final ItemRarity rarity, final int price, final DamageBase damageBase,
                 final Range range, final int lesserCriticalMargin, final ItemFavor favor) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.damageBase = damageBase;
        this.range = range;
        this.lesserCriticalMargin = lesserCriticalMargin;
        this.favor = favor;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.CROSSBOW;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }

    /** {@code null} for every Besta — the "Projétil" column is not a catalogued Efeito Crítico. */
    @Override
    public CriticalEffectType getCriticalEffect() {
        return null;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a crossbow grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a crossbow grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a crossbow carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
