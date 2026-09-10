package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Escudos catalog — one constant per {@link ItemCategory#SHIELD} item in {@code
 * docs/rules/equipamentos.txt}, mirroring {@link ArmorItem}'s one-enum-per-category shape.
 *
 * <p>Escudos <b>are</b> {@link CriticallyDefensiveItem} — the second implementor after {@code
 * ArmorItem}, which is what moved {@code getDefensiveCriticalEffect()} onto that shared
 * interface. The source "Atualizando os Equipamentos Defensivos" table assigns one to each.
 *
 * <p>Every Escudo's real Favor mechanic is gated on "não realizou nenhuma ação ofensiva nesta
 * Rodada" — a {@link FavorCondition#NO_OFFENSIVE_ACTION_THIS_ROUND} on the {@link ItemBonus},
 * resolved against the wielder's {@link org.aventyrs.core.sheet.CombatantSheet#hasActedOffensivelyThisRound()}.
 * The Favores that instead <i>rewrite where</i> another bonus applies, or gate on an activated
 * effect this core has no activation path for, stay as description / {@code additionalEffects}
 * text.
 */
@Getter
public enum ShieldItem implements CriticallyDefensiveItem {

    /**
     * Braçadeiras (Leve/Incomum). The Favor rewrites the Efeito Adicional's target ("afeta
     * Defesas ao invés de DM") — a redirect with no expression — so it stays as text. The
     * Efeito Adicional itself is a real {@link ModifierType#MAGIC_DEFENSE} 1 while the wielder
     * has taken no offensive action this Rodada.
     */
    BRACADEIRAS(
            "Braçadeiras",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            5, 0, 1, 20, 0,
            ItemFavor.builder()
                    .description("Bônus defensivos desencadeados quando não fizer ações ofensivas "
                            + "afetam Defesas ao invés de DM.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.MAGIC_DEFENSE, 1,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .build(),
            DefensiveCriticalEffectType.FAISCA_DE_DETERMINACAO),

    /**
     * Bracelete Arcano (Leve/Raro). Both clauses need an activated-effect path this core lacks:
     * the Favor scales "Bônus em Defesas do efeito ativo", and the Efeito Adicional is a 1PA /
     * 2PM activation ({@code ItemActiveAbility} only exists on a Regalia, and nothing consumes
     * one). Both stay as text; its DM +1 and Conjuração +1 columns are real.
     */
    BRACELETE_ARCANO(
            "Bracelete Arcano",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.RARE,
            11, 0, 1, 20, 1,
            ItemFavor.builder()
                    .description("Bônus em Defesas do efeito ativo muda para +2.")
                    .requirements(new ItemRequirements(AttributeDomain.FOCUS, 3))
                    .additionalEffects("Com o Tempo de Ação de 1PA, o usuário pode utilizar 2PM "
                            + "para aumentar os Bônus nas Defesas em +1 por 3 Rodadas (efeito não "
                            + "cumulativo). Apenas personagens capazes de Conjurar ou Mimetizar "
                            + "magias podem ativar este efeito.")
                    .build(),
            DefensiveCriticalEffectType.CHOQUE_DE_AETHER),

    /**
     * Broquel (Leve/Incomum) — the DF twin of {@link #BRACADEIRAS}: the Favor rewrites the
     * Efeito Adicional's target ("afeta Defesas ao invés de DF"), and the Efeito Adicional is a
     * real {@link ModifierType#PHYSICAL_DEFENSE} 1 while the wielder has taken no offensive
     * action this Rodada.
     */
    BROQUEL(
            "Broquel",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            4, 1, 0, 20, 0,
            ItemFavor.builder()
                    .description("Bônus defensivos desencadeados quando não fizer ações ofensivas "
                            + "afetam Defesas ao invés de DF.")
                    .requirements(new ItemRequirements(AttributeDomain.DEXTERITY, 3))
                    .bonus(new ItemBonus(ModifierType.PHYSICAL_DEFENSE, 1,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .build(),
            DefensiveCriticalEffectType.CONTRA_ATACANTE),

    /**
     * Escudo Médio (Médio/Comum). Both clauses land, both gated on no offensive action this
     * Rodada: the Favor's "danos sofridos são reduzidos em -1" is a {@link
     * ModifierType#DAMAGE_REDUCTION} 1 (read as conditional on the same trigger as the Efeito
     * Adicional, per the rules-text reading agreed with the author), and the Efeito Adicional is
     * a {@link ModifierType#DEFESAS} 1.
     */
    ESCUDO_MEDIO(
            "Escudo Médio",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.COMMON,
            5, 2, 1, 20, 0,
            ItemFavor.builder()
                    .description("Adicionalmente aos bônus defensivos desencadeados quando não "
                            + "fizer ações ofensivas, danos sofridos são reduzidos em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .bonus(new ItemBonus(ModifierType.DEFESAS, 1,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .build(),
            DefensiveCriticalEffectType.REPELIR_E_SUPRIMIR),

    /**
     * Escudo de Corpo (Pesado/Raro) — the heavier {@link #ESCUDO_MEDIO}: same conditional
     * {@link ModifierType#DAMAGE_REDUCTION} 1 Favor, a {@link ModifierType#DEFESAS} 2 Efeito
     * Adicional, and its Conjuração column reads "Desvantagem" — a flat {@link
     * Skill#DISADVANTAGE_MALUS}.
     */
    ESCUDO_DE_CORPO(
            "Escudo de Corpo",
            "",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            9, 2, 2, 20, Skill.DISADVANTAGE_MALUS,
            ItemFavor.builder()
                    .description("Adicionalmente aos bônus defensivos desencadeados quando não "
                            + "fizer ações ofensivas, danos sofridos são reduzidos em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
                    .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .bonus(new ItemBonus(ModifierType.DEFESAS, 2,
                            FavorCondition.NO_OFFENSIVE_ACTION_THIS_ROUND))
                    .build(),
            DefensiveCriticalEffectType.PROVOCAR),

    /**
     * Repulsor (Médio/Épico). The Favor gates its -1 dano on an activated effect, and the
     * Efeito Adicional is a 1PA / 2PM activation — no activation path exists for a non-Regalia
     * item, so both stay as text. Its DF/DM +1 and Conjuração +2 columns are real.
     */
    REPULSOR(
            "Repulsor",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.EPIC,
            14, 1, 1, 20, 2,
            ItemFavor.builder()
                    .description("Enquanto efeito estiver ativo reduz danos sofridos em -1.")
                    .requirements(new ItemRequirements(AttributeDomain.FOCUS, 3))
                    .additionalEffects("Com o Tempo de Ação de 1PA, o usuário pode utilizar 2PM "
                            + "para aumentar os Bônus nas Defesas em +2 por 3 Rodadas (efeito não "
                            + "cumulativo). Apenas personagens capazes de Conjurar ou Mimetizar "
                            + "magias podem ativar este efeito.")
                    .build(),
            DefensiveCriticalEffectType.CHOQUE_DE_AETHER);

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
    private final DefensiveCriticalEffectType defensiveCriticalEffect;

    ShieldItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.SHIELD;
    }
}
