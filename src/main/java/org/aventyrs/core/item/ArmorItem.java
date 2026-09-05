package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Armaduras catalog — one constant per {@link ItemCategory#ARMOR} item, mirroring
 * {@code org.aventyrs.core.feat.ArtesMarciaisFeat}'s one-enum-per-category shape (and, like
 * it, holding the rules text as the single source of truth rather than duplicating it).
 * A sibling enum per {@link ItemCategory} follows the same shape as more items are supplied.
 */
@Getter
public enum ArmorItem implements ItemTemplate {

    /**
     * Armadura Completa (Pesado/Raro). Its Conjuração column reads "Desvantagem" rather than a
     * number, which is exactly {@link Skill#DISADVANTAGE_MALUS} — this is the first real,
     * unconditional Desvantagem in the ruleset, and what motivated adding that constant.
     *
     * <p>Its Favor, at Força 3, is a real Redução de Dano 2 ({@link
     * ModifierType#DAMAGE_REDUCTION}, which {@code DamageService#getTotalDamageReduction}
     * already sums for real), not merely rules text. The text scopes it to Dano de Corte
     * specifically; this core has no damage-type-scoped mitigation ({@code DamageType} has no
     * Corte/Perfuração/Impacto breakdown under {@code FISICO}, and RD/RA are resolved with no
     * notion of damage type at all), so it's modeled as plain RD — the same explicit
     * "document the simplification rather than silently narrowing or over-granting" treatment
     * scoped Vantagem bonuses get elsewhere. "Efeitos Adicionais: Nenhum" is modeled as no
     * additional effects at all, not an empty string.
     */
    ARMADURA_COMPLETA(
            "Armadura Completa",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            20,
            5,
            2,
            32,
            Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Dano de Corte sofrido é reduzido em -2.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                    .build(),
            DefensiveCriticalEffectType.RETORNO_DE_DANOS),

    /**
     * Armadura de Gladiador (Leve/Comum). "Favor: Nenhum" is exactly a {@code null} {@link
     * ItemFavor} — the item grants nothing conditional at all, so there is no Requisito to
     * meet either (the two always travel together: Requisitos exist only to gate a Favor).
     * {@link Item#grantsFavorTo(org.aventyrs.core.character.Character)} is permanently {@code
     * false} for it and {@link Item#resolveFavorBonus} permanently 0, with no null-checking
     * needed at any call site.
     *
     * <p>Its Conjuração column reads "-" rather than a number or "Desvantagem", which is 0:
     * the armour neither helps nor hinders conjuração. Contrast {@link #ARMADURA_COMPLETA},
     * where "Desvantagem" is a real {@link Skill#DISADVANTAGE_MALUS}.
     */
    ARMADURA_DE_GLADIADOR(
            "Armadura de Gladiador",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            5,
            2,
            1,
            24,
            0,
            null,
            DefensiveCriticalEffectType.CONTRA_ATACANTE),

    /**
     * Armadura de Justa (Pesado/Épico) — the first item whose Raridade is {@link
     * ItemRarity#EPIC}, which is what added that constant (the enum previously modeled only
     * the three tiers rules text had confirmed).
     *
     * <p>Its Favor, at Força 4, is two clauses, and only one of them is expressible today:
     * <ul>
     *   <li>"Dano de Corte e Perfuração sofrido é reduzido em -2" is a real {@link
     *   ModifierType#DAMAGE_REDUCTION} 2, carrying the identical damage-type simplification
     *   {@link #ARMADURA_COMPLETA}'s own Corte-scoped Favor already documents — {@code
     *   DamageType} has no Corte/Perfuração/Impacto breakdown, and RD is resolved with no
     *   notion of damage type at all, so it's modeled as plain RD rather than silently
     *   narrowed or over-granted.</li>
     *   <li>"Movimento Base reduzido à metade" contributes no {@link ItemBonus} at all and
     *   lives on in the Favor's own description text. Halving isn't a shape {@link ItemBonus}
     *   can express — it's a flat {@link ModifierType}-and-value pair, and {@code
     *   org.aventyrs.core.character.services.MovementService#getMovementBase} sums {@code
     *   ModifierType#MOVEMENT} additively with no multiplicative step anywhere (unlike {@code
     *   DamageService}, which does have a real halving stage for {@code
     *   ModifierType#HALF_DAMAGE} to feed). Don't add a {@code MOVEMENT_HALVED} constant
     *   speculatively: unlike {@code HALF_DAMAGE}, there is no consumer to read it, so it
     *   would be inert in a way even {@code ModifierType#DEFESAS} (a real registry entry
     *   nothing reads *yet*) isn't — the mechanism itself is what's missing, not just the
     *   reader.</li>
     * </ul>
     *
     * <p>Its Conjuração column reads "Desvantagem", exactly as {@link #ARMADURA_COMPLETA}'s
     * does, and its Descrição and "Efeitos Adicionais: Nenhum" are both empty/absent.
     */
    ARMADURA_DE_JUSTA(
            "Armadura de Justa",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.EPIC,
            24,
            4,
            4,
            35,
            Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Dano de Corte e Perfuração sofrido é reduzido em -2, "
                            + "Movimento Base reduzido à metade.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 4))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                    .build(),
            DefensiveCriticalEffectType.PROVOCAR),

    /**
     * Couraça (Médio/Incomum). Its Favor's "Dano de Corte e Perfuração" scoping is the same
     * simplification {@link #ARMADURA_COMPLETA} documents — modeled as plain {@link
     * ModifierType#DAMAGE_REDUCTION}, here 1 rather than 2, since RD is resolved with no
     * notion of damage type at all. Its Conjuração column reads "-", which is 0.
     */
    COURACA(
            "Couraça",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.UNCOMMON,
            11,
            3,
            1,
            28,
            0,
            ItemFavor.builder()
                    .description("Dano de Corte e Perfuração sofrido é reduzido em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1))
                    .build(),
            DefensiveCriticalEffectType.RETORNO_DE_DANOS),

    /**
     * Meia Armadura (Médio/Comum) — a plain {@link ModifierType#DAMAGE_REDUCTION} 1 at Força
     * 3, its "de Corte" scoping simplified away exactly as {@link #ARMADURA_COMPLETA}'s is.
     */
    MEIA_ARMADURA(
            "Meia Armadura",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            13,
            3,
            3,
            28,
            Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Dano de Corte sofrido é reduzido em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1))
                    .build(),
            DefensiveCriticalEffectType.RETORNO_DE_DANOS),

    /**
     * Robe Cerimonial (Leve/Incomum) — the first item gated on an {@link AttributeDomain}
     * other than Força ({@link AttributeDomain#GNOSE} 3), and the first whose Conjuração
     * column is a real positive number rather than 0 or "Desvantagem".
     *
     * <p>Its Favor's "Dano Mágico" scoping is the same simplification the Corte/Perfuração
     * ones get, for a slightly different reason worth keeping straight: {@code
     * DamageType#MAGICO} genuinely exists as a classification tag (unlike Corte/Perfuração,
     * which don't exist anywhere), but {@code DamageService} resolves RD/RA with no notion of
     * damage type whatsoever — so the missing piece here is per-type mitigation, not the
     * type itself. Modeled as plain {@link ModifierType#DAMAGE_REDUCTION} 2 either way.
     */
    ROBE_CERIMONIAL(
            "Robe Cerimonial",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            9,
            1,
            3,
            20,
            1,
            ItemFavor.builder()
                    .description("Dano Mágico sofrido reduzido em -2.")
                    .requirements(new ItemRequirements(AttributeDomain.GNOSE, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                    .build(),
            DefensiveCriticalEffectType.SURTO_ARCANO),

    /**
     * Robe de Guerra (Médio/Raro) — the one cataloged Favor whose RD needs no simplification
     * at all: "Dano sofrido reduzido em -1" is unscoped in its own rules text, so plain {@link
     * ModifierType#DAMAGE_REDUCTION} 1 is exactly what it says, not an approximation of it.
     */
    ROBE_DE_GUERRA(
            "Robe de Guerra",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            18,
            2,
            5,
            28,
            2,
            ItemFavor.builder()
                    .description("Dano sofrido reduzido em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.GNOSE, 4))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1))
                    .build(),
            DefensiveCriticalEffectType.SURTO_ARCANO),

    /**
     * Roupa Pesada (Leve/Comum) — its Favor grants a flat +1 to DF and +1 to DM at once,
     * gated on Destreza 3. The rules text splits this into two clauses ("+1 na Defesa
     * faltante entre DF ou DM" as the base Favor, plus an Efeito Adicional of "+1 em DF ou
     * DM, definido na fabricação" for whichever of the two the base clause didn't already
     * cover), but the production-time choice always lands on the option the base clause
     * didn't pick — so the two clauses combined net out to an unconditional +1 DF/+1 DM for
     * anyone meeting the requirement, regardless of that per-copy choice. There's still no
     * separate DF/DM stat this core computes ({@link ModifierType#DEFESAS} is one
     * undifferentiated constant, see {@link Item}'s own javadoc), so the combined +2 is
     * modeled as a single {@code DEFESAS} {@link ItemBonus} rather than as two
     * DF-specific/DM-specific ones — unlike the old single-clause reading, this no longer
     * over-grants, since the net effect really is unconditional on both.
     * Its DF/DM columns are genuinely +0 — the armour's whole contribution is its Favor.
     */
    ROUPA_PESADA(
            "Roupa Pesada",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            3,
            0,
            0,
            20,
            0,
            ItemFavor.builder()
                    .description("Concede Bônus de +1 em DF e +1 em DM.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.DEFESAS, 2))
                    .build(),
            DefensiveCriticalEffectType.LIBERDADE_DE_ACAO);

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

    /**
     * The Efeito Crítico Defensivo this armour grants its wearer, from the source catalog's
     * "Atualizando os Equipamentos Defensivos" table — every Armadura has one, so this is
     * never {@code null}.
     *
     * <p>It lives here rather than on {@code Item} because "apenas Armaduras e Escudos recebem
     * Efeitos Críticos Defensivos" — a Capa or an Elmo grants none, and a defaulted column on
     * {@code Item} would make "a helmet" and "a breastplate" answer alike, the same mistake
     * keeping {@code getDamageBase()} off {@code Item} avoids. Promote it to a shared interface
     * when a {@code ShieldItem} lands and needs the identical shape, not before.
     *
     * <p><b>Nothing reads it yet</b> — a Defensive effect fires on an Acerto Crítico in the
     * wearer's own Defesa roll, and neither {@code AttackReceiver} nor {@code
     * EsquivaEApararInteraction} resolves that branch. The values are exact authored data all
     * the same, like this enum's own Preço and Dureza columns.
     */
    private final DefensiveCriticalEffectType defensiveCriticalEffect;

    ArmorItem(final String name, final String description, final ItemWeightClass weightClass,
              final ItemRarity rarity, final int price, final int physicalDefenseBonus,
              final int magicDefenseBonus, final int hardness, final int castingBonus,
              final ItemFavor favor, final DefensiveCriticalEffectType defensiveCriticalEffect) {
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
        this.defensiveCriticalEffect = defensiveCriticalEffect;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ARMOR;
    }
}
