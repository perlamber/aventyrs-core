package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Armas de Arremesso catalog — one constant per {@link ItemCategory#THROWABLE} entry of
 * {@code docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Arremesso" table.
 * Same {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>Every entry is de Ataque à Distância ({@link SkillType#ATAQUE_A_DISTANCIA}). Two of the
 * four Favores and every "Efeitos Adicionais" line stay prose — see each constant.
 *
 * <h2>Source-row defect: the Zarabatanas</h2>
 *
 * {@code ZARABATANA}/{@code ZARABATANA_DE_CACA} print {@code Projétil | Projétil | Projétil |
 * Médio} for their whole Dano row instead of numbers — the defect {@code
 * equipamentos-index.md} flags. Their real dano lives only in the prose: a Zarabatana fires
 * Dardos, whose own Dano Base ({@code DARDOS_E_SHUKENS}, 1d6+1) it raises by +1 / +2. So the
 * modeled {@link #getDamageBase()} is the resulting dart figure — 1d6+2 / 1d6+3 — and
 * {@link #getCriticalEffect()} is {@code null} ("Projétil" is not a {@link CriticalEffectType}).
 * This is a reconstruction from the prose, not a silent invention — it is called out here and
 * in each constant's javadoc.
 */
@Getter
public enum ThrowableItem implements ItemTemplate, Weapon {

    /**
     * Dardos e Shukens (Leve/Comum) — Preço 7. Dano 1d6+1, Tipo Perfurante, Efeito Crítico
     * Excruciante (17), Alcance Curto, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Primeiro arremesso em cada um de seus Turnos tem o Tempo de Ação
     * reduzido em -1PA" is prose: an action-cost reduction scoped to the first throw of a Turn
     * has no reader (this core reports PA, it does not budget them). <b>Efeitos Adicionais</b>
     * "Vendido em kits de 6 unidades" is inventory-packaging flavour.
     */
    DARDOS_E_SHUKENS(
            "Dardos e Shukens",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            7,
            DamageBase.of(1, 1),
            Range.DISTANCIA_CURTA,
            CriticalEffectType.EXCRUCIANTE,
            17,
            ItemFavor.builder()
                    .description("Primeiro arremesso em cada um de seus Turnos tem o Tempo de "
                            + "Ação reduzido em -1PA.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Vendido em kits de 6 unidades.")
                    .build()),

    /**
     * Pilum (Médio/Raro) — Preço 14. Dano 2d6, Tipo Perfurante, Efeito Crítico Estilhaçador
     * (17), Alcance Curto, Requisito Força 3 ou Destreza 3.
     *
     * <p><b>Favor</b> "+1d6 pontos de danos adicionais se o alvo for um objeto, construto ou
     * equipamento" is prose: this core rolls no dice and does not track what an attack's target
     * <em>is</em>. <b>Efeitos Adicionais</b> — usable corpo-a-corpo at a reduced 1d6+1 (one
     * Weapon holds one Dano Base and one Perícia, see {@code Weapon#getSkillType()}) and the
     * Corrente de Efeitos "Destruidor de Escudos" (no item-granted Corrente reader) — both stay
     * prose.
     */
    PILUM(
            "Pilum",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            14,
            DamageBase.of(2, 0),
            Range.DISTANCIA_CURTA,
            CriticalEffectType.ESTILHACADOR,
            17,
            ItemFavor.builder()
                    .description("Causa +1d6 pontos de danos adicionais se o alvo for um "
                            + "objeto, construto ou equipamento.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY))
                    .additionalEffects("Também pode ser usado como Arma Corpo-a-Corpo, dano "
                            + "reduzido para 1d6+1. Arremessos possuem a Corrente de Efeitos – "
                            + "Destruidor de Escudos: causa a mesma quantidade de danos ao "
                            + "Escudo do alvo.")
                    .build()),

    /**
     * Zarabatana (Leve/Comum) — Preço 6. Requisito Destreza 4. Its Dano row is the defective
     * {@code Projétil | Projétil | Projétil | Médio} (see this enum's javadoc); reconstructed
     * from the prose as a Dardo (1d6+1) raised +1 → 1d6+2, Alcance Média, Efeito Crítico
     * {@code null}.
     *
     * <p><b>Favor</b> "Dano Base do Dardo aumenta em +1 (para um total de +2)" restates that
     * reconstruction and is otherwise prose. <b>Efeitos Adicionais</b> "Usada apenas para
     * disparar Dardos … Distância Base muda para Longa. Tempo de Recarga 1PA" — the dual
     * dart-only mode, the Range replacement and the reload cost have no readers.
     */
    ZARABATANA(
            "Zarabatana",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            6,
            DamageBase.of(1, 2),
            Range.DISTANCIA_MEDIA,
            null,
            17,
            ItemFavor.builder()
                    .description("Dano Base do Dardo aumenta em +1 (para um total de +2).")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 4))
                    .additionalEffects("Usada apenas para disparar Dardos; o Dano Base dos "
                            + "dardos aumenta em +1 e a Distância Base muda para Longa. Tempo "
                            + "de Recarga 1PA.")
                    .build()),

    /**
     * Zarabatana de Caça (Médio/Comum) — Preço 9. Requisito Destreza 4. Defective Dano row (see
     * this enum's javadoc); reconstructed from the prose as a Dardo (1d6+1) raised +2 → 1d6+3,
     * Alcance Longa, Efeito Crítico {@code null}.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16 e recebe Ferida Profunda como um
     * Efeito Crítico adicional" is prose: no reader applies an item-granted Margem Crítica
     * shift or a bonus Efeito Crítico. <b>Efeitos Adicionais</b> "o Dano Base dos dardos
     * aumenta em +2 … Muito Longa … ambas as mãos … Tempo de Recarga 1PA" — same unread
     * mechanisms as {@link #ZARABATANA}.
     */
    ZARABATANA_DE_CACA(
            "Zarabatana de Caça",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            9,
            DamageBase.of(1, 3),
            Range.DISTANCIA_LONGA,
            null,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16 e recebe Ferida Profunda "
                            + "como um Efeito Crítico adicional.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 4))
                    .additionalEffects("Usada apenas para disparar Dardos; o Dano Base dos "
                            + "dardos aumenta em +2 e a Distância Base muda para Muito Longa. "
                            + "Exige o uso de ambas as mãos para atirar, Tempo de Recarga 1PA.")
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

    ThrowableItem(final String name, final String description, final ItemWeightClass weightClass,
                  final ItemRarity rarity, final int price, final DamageBase damageBase,
                  final Range range, final CriticalEffectType criticalEffect,
                  final int lesserCriticalMargin, final ItemFavor favor) {
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
        return ItemCategory.THROWABLE;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }

    /** Always 0 — the Armas section leaves every weapon's Dureza blank. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — a throwing weapon grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a throwing weapon grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a throwing weapon carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
