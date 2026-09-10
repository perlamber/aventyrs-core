package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Arcos catalog — one constant per {@link ItemCategory#BOW} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Arco" table, mirroring
 * {@link NaturalWeapon}'s {@code implements ItemTemplate, Weapon} shape and {@link ArmorItem}'s
 * one-enum-per-{@link ItemCategory} layout.
 *
 * <p>Every Arco is de Ataque à Distância ({@link SkillType#ATAQUE_A_DISTANCIA}), carries the
 * "Exige o uso de ambas as mãos para atirar" line as prose (this core models no hand budget for
 * drawn weapons — see CLAUDE.md), and prints "Projétil" in its Efeito Crítico column. "Projétil"
 * is not a {@link CriticalEffectType} and appears nowhere in {@code
 * docs/rules/efeitos-criticos.txt}, so {@link #getCriticalEffect()} is {@code null} for all
 * three — the same source-row defect {@code equipamentos-index.md} flags for the Zarabatanas.
 *
 * <h2>What is not modeled, and why</h2>
 * <ul>
 *   <li><b>Dureza / DF / DM / Conjuração are 0.</b> The Armas section prints Dureza inline on
 *   the title line and leaves it blank for every weapon; a bow has no Defesa or Conjuração
 *   column at all.</li>
 *   <li><b>Every Favor here is prose only.</b> None maps to an {@link ItemBonus}: Arco
 *   Composto's "Metade da Força aos Danos" is an Atributo-scaled dano term ({@code ItemBonus}
 *   holds a fixed value), Arco Curto's "Margem Crítica Menor muda para 16" a conditional crit
 *   shift with no reader, Arco Longo's "Alcance Base muda para Muito Longa" a Range replacement
 *   with no reader. Each stays on the Favor's {@code description}.</li>
 *   <li><b>The disjunctive Requisitos</b> ("For 3 ou Des 3"-style) some weapons carry are
 *   handled by {@link ItemRequirements}' {@code alternativeDomain}; the Arcos each name a
 *   single Atributo, so none needs it.</li>
 * </ul>
 */
@Getter
public enum BowItem implements ItemTemplate, Weapon {

    /**
     * Arco Composto (Médio/Raro) — Preço 17. Dano 1d6+2, Tipo Projétil, Efeito Crítico
     * "Projétil (17)", Alcance Longa, Requisito Força 3.
     *
     * <p><b>Favor</b> "Adiciona Metade da Força aos Danos Causados" is unmodeled: a half-Força
     * dano term is not a fixed {@link ItemBonus}, and the melee ½-Força term {@code
     * AbstractSkillInteraction#resolveMeleeStrengthDamage} grants is melee-only by design (an
     * Ataque à Distância adds nothing — CLAUDE.md). An item-granted derived-Atributo dano bonus
     * has no hook.
     */
    ARCO_COMPOSTO(
            "Arco Composto",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            17,
            DamageBase.of(1, 2),
            Range.DISTANCIA_LONGA,
            null,
            17,
            ItemFavor.builder()
                    .description("Adiciona Metade da Força aos Danos Causados.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .additionalEffects("Exige o uso de ambas as mãos para atirar.")
                    .build()),

    /**
     * Arco Curto (Leve/Comum) — Preço 9. Dano 1d6+1, Tipo Projétil, Efeito Crítico
     * "Projétil (17)", Alcance Média, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Margem Crítica Menor muda para 16" is unmodeled — no reader applies an
     * item-granted Margem Crítica shift (contrast {@code Feat#resolveCriticalMarginIncrease},
     * a Talento hook). {@link #getLesserCriticalMargin()} stays the table's 17.
     */
    ARCO_CURTO(
            "Arco Curto",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            9,
            DamageBase.of(1, 1),
            Range.DISTANCIA_MEDIA,
            null,
            17,
            ItemFavor.builder()
                    .description("Margem Crítica Menor muda para 16.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Exige o uso de ambas as mãos para atirar.")
                    .build()),

    /**
     * Arco Longo (Pesado/Incomum) — Preço 14. Dano 1d6+3, Tipo Projétil, Efeito Crítico
     * "Projétil (17)", Alcance Longa, Requisito Destreza 3.
     *
     * <p><b>Favor</b> "Alcance Base muda para Distância Muito Longa" is unmodeled: a Favor that
     * <em>replaces</em> the Alcance column has no reader (unlike {@code
     * Feat#resolveAttackRangeIncrease}, which adds steps). {@link #getRange()} stays the table's
     * Longa.
     */
    ARCO_LONGO(
            "Arco Longo",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.UNCOMMON,
            14,
            DamageBase.of(1, 3),
            Range.DISTANCIA_LONGA,
            null,
            17,
            ItemFavor.builder()
                    .description("Alcance Base muda para Distância Muito Longa.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .additionalEffects("Exige o uso de ambas as mãos para atirar.")
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

    BowItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.BOW;
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

    /** Always 0 — a bow grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a bow grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a bow carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
