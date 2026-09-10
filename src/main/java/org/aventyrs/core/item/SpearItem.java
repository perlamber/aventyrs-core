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
 * The Lanças catalog — one constant per {@link ItemCategory#SPEAR} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Lança" table. Same
 * {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>All five are swung with {@link SkillType#ATAQUE_CORPO_A_CORPO}; their reach varies
 * (Adjacente to Curta), so each carries its own {@link Range}. Only {@link #JAVELIN}'s Favor is
 * expressible ("Vantagem nas rolagens de Ataque à Distância" → {@link
 * ModifierType#ATAQUE_A_DISTANCIA_ROLL_BONUS}, {@link Skill#ADVANTAGE_BONUS}, for when it is
 * thrown); the rest are "Alcance/Margem Crítica muda para …" shifts or a Corrente de Efeitos,
 * all without readers. The disjunctive Requisitos ("For 4 ou Des 4") use {@link
 * ItemRequirements}' {@code alternativeDomain}.
 */
@Getter
public enum SpearItem implements ItemTemplate, Weapon {

    /**
     * Alabarda ou Naginata (Média/Rara) — Preço 12. Dano 1d6+2, Tipo Corte, Efeito Crítico
     * Ferida Profunda (17), Alcance Adjacente, Requisito Força 4 ou Destreza 4.
     *
     * <p><b>Favor</b> "Alcance Base muda para Muito Curto e Margem Crítica Menor muda para 16"
     * — prose (neither an item-granted Range nor crit-margin shift has a reader). <b>Efeitos
     * Adicionais</b> "Dano Base muda para 2d6 quando utilizada enquanto montando um animal ou
     * dirigindo" — no montaria concept.
     */
    ALABARDA_OU_NAGINATA(
            "Alabarda ou Naginata",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            12,
            DamageBase.of(1, 2),
            Range.ADJACENTE,
            CriticalEffectType.FERIDA_PROFUNDA,
            17,
            ItemFavor.builder()
                    .description("Alcance Base muda para Muito Curto e Margem Crítica Menor "
                            + "muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4, AttributeDomain.DEXTERITY))
                    .additionalEffects("Dano Base muda para 2d6 quando utilizada enquanto "
                            + "montando um animal ou dirigindo.")
                    .build()),

    /**
     * Javelin (Média/Comum) — Preço 9. Dano 1d6+1, Tipo Perfurante, Efeito Crítico Ferida
     * Profunda (17), Alcance Muito Curta, Requisito Força 3 ou Destreza 3.
     *
     * <p><b>Favor</b> "Vantagem nas rolagens de Ataque à Distância" is a real flat {@link
     * Skill#ADVANTAGE_BONUS} on the {@link SkillType#ATAQUE_A_DISTANCIA} Perícia — it applies
     * when the Javelin is thrown, which is a different Perícia's roll from its own melee
     * {@link #getSkillType()}. <b>Efeitos Adicionais</b> "Pode ser utilizado como Arma de
     * Arremesso – Alvo Único Distância Longa; arremessos bem-sucedidos recebem Vantagem nas
     * rolagens de Danos" — the thrown mode itself is unmodeled.
     */
    JAVELIN(
            "Javelin",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            9,
            DamageBase.of(1, 1),
            Range.DISTANCIA_MUITO_CURTA,
            CriticalEffectType.FERIDA_PROFUNDA,
            17,
            ItemFavor.builder()
                    .description("Vantagem nas rolagens de Ataque à Distância.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY))
                    .bonus(new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS))
                    .additionalEffects("Pode ser utilizado como uma Arma de Arremesso – Alvo "
                            + "Único Distância Longa; arremessos bem-sucedidos recebem "
                            + "Vantagem nas rolagens de Danos.")
                    .build()),

    /**
     * Lança (Média/Comum) — Preço 8. Dano 1d6+3, Tipo Perfurante, Efeito Crítico Empalar (17),
     * Alcance Muito Curta, Requisito Força 3 ou Destreza 3.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16" — prose. <b>Efeitos Adicionais</b>
     * "Pode ser utilizado como uma Arma de Arremesso – Alvo Único Distância Média".
     */
    LANCA(
            "Lança",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            8,
            DamageBase.of(1, 3),
            Range.DISTANCIA_MUITO_CURTA,
            CriticalEffectType.EMPALAR,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY))
                    .additionalEffects("Pode ser utilizado como uma Arma de Arremesso – Alvo "
                            + "Único Distância Média.")
                    .build()),

    /**
     * Lança de Justa (Pesada/Raro) — Preço 15. Dano 1d6+1, Tipo Perfurante, Efeito Crítico
     * Desmembrar (17), Alcance Muito Curta, Requisito Força 4.
     *
     * <p><b>Favor</b> Corrente de Efeitos "Derrubar" (alvos atingidos recebem a Condição Caído)
     * — prose: no reader applies an item-granted Corrente, and nothing applies a {@code
     * ConditionType} automatically from an attack (CLAUDE.md's Malefício row). <b>Efeitos
     * Adicionais</b> "Dano Base muda para 2d6+2 quando montando ou dirigindo".
     */
    LANCA_DE_JUSTA(
            "Lança de Justa",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            15,
            DamageBase.of(1, 1),
            Range.DISTANCIA_MUITO_CURTA,
            CriticalEffectType.DESMEMBRAR,
            17,
            ItemFavor.builder()
                    .description("Recebe a Corrente de Efeitos – Derrubar: alvos atingidos são "
                            + "derrubados, recebendo a Condição Caído.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .additionalEffects("Dano Base muda para 2d6+2 quando utilizada enquanto "
                            + "montando um animal ou dirigindo.")
                    .build()),

    /**
     * Pique (Pesada/Incomum) — Preço 12. Dano 1d6+2, Tipo Perfurante, Efeito Crítico Empalar
     * (17), Alcance Curta, Requisito Força 4 ou Destreza 4.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16 e não mais aumenta o Tempo de Ação dos
     * Ataques" — prose (crit-margin shift and PA relief both unread). <b>Efeitos Adicionais</b>
     * "Exige uso de ambas as mãos para atacar e o Tempo de Ação dos ataques é aumentado em
     * +1PA".
     */
    PIQUE(
            "Pique",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.UNCOMMON,
            12,
            DamageBase.of(1, 2),
            Range.DISTANCIA_CURTA,
            CriticalEffectType.EMPALAR,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16 e não mais aumenta o Tempo "
                            + "de Ação dos Ataques.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4, AttributeDomain.DEXTERITY))
                    .additionalEffects("Exige uso de ambas as mãos para atacar e o Tempo de "
                            + "Ação dos ataques é aumentado em +1PA.")
                    .build());

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final DamageBase damageBase;
    private final Range range;
    private final CriticalEffectType criticalEffect;
    private final int lesserCriticalMargin;
    private final ItemFavor favor;

    SpearItem(final String name, final String description, final ItemWeightClass weightClass,
              final ItemRarity rarity, final int price, final DamageBase damageBase, final Range range,
              final CriticalEffectType criticalEffect, final int lesserCriticalMargin,
              final ItemFavor favor) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.damageBase = damageBase;
        this.range = range;
        this.criticalEffect = criticalEffect;
        this.lesserCriticalMargin = lesserCriticalMargin;
        this.favor = favor;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.SPEAR;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a spear grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a spear grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a spear carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
