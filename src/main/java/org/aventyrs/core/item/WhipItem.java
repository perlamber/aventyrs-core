package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Chicotes catalog — one constant per {@link ItemCategory#WHIP} entry of {@code
 * docs/rules/equipamentos.txt}'s "Equipamentos Ofensivos → Armas → Chicote" table. Same
 * {@code implements ItemTemplate, Weapon} shape as {@link NaturalWeapon}.
 *
 * <p>All four are swung with {@link SkillType#ATAQUE_CORPO_A_CORPO} (a Chicote is a reach melee
 * weapon — its Alcance band exceeds Adjacente but the Perícia is the melee one). <b>Every Favor
 * is prose only</b>: each is a Corrente de Efeitos (Prender e Puxar, Esmaga e Mói, Marcar
 * Território), and no reader routes an item-granted Corrente into a roll — CLAUDE.md's "Efeito
 * Crítico / Corrente de Efeitos grants from an item" gap.
 */
@Getter
public enum WhipItem implements ItemTemplate, Weapon {

    /**
     * Chicote (Leve/Incomum) — Preço 11. Dano 1d6+1, Tipo Corte, Efeito Crítico Atordoante
     * (16), Alcance Curta, Requisito Nenhum.
     *
     * <p><b>Favor</b> Corrente de Efeitos "Prender e Puxar" (inutiliza uma arma/escudo do alvo
     * até 1PA para soltar; puxa um objeto solto por +1PA) — prose.
     */
    CHICOTE(
            "Chicote",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.UNCOMMON,
            11,
            DamageBase.of(1, 1),
            Range.DISTANCIA_CURTA,
            CriticalEffectType.ATORDOANTE,
            16,
            ItemFavor.builder()
                    .description("Possui a Corrente de Efeitos – Prender e Puxar: inutiliza um "
                            + "Equipamento do alvo (arma ou escudo) até que ele gaste 1PA para "
                            + "soltá-lo; um objeto solto pode ser puxado até você (+1PA).")
                    .build()),

    /**
     * Chicote Ferrão (Médio/Raro) — Preço 14. Dano 1d6+2, Tipo Corte, Efeito Crítico Atordoante
     * (16), Alcance Curta, Requisito Nenhum. Same Corrente "Prender e Puxar" Favor as {@link
     * #CHICOTE}, one Dano Base rung heavier.
     */
    CHICOTE_FERRAO(
            "Chicote Ferrão",
            "",
            ItemWeightClass.MEDIUM,
            ItemRarity.RARE,
            14,
            DamageBase.of(1, 2),
            Range.DISTANCIA_CURTA,
            CriticalEffectType.ATORDOANTE,
            16,
            ItemFavor.builder()
                    .description("Possui a Corrente de Efeitos – Prender e Puxar: inutiliza um "
                            + "Equipamento do alvo (arma ou escudo) até que ele gaste 1PA para "
                            + "soltá-lo; um objeto solto pode ser puxado até você (+1PA).")
                    .build()),

    /**
     * Corrente Espinhosa (Pesado/Raro) — Preço 15. Dano 2d6, Tipo Esmagamento, Efeito Crítico
     * Desmembrar (16), Alcance Muito Curta, Requisito Nenhum.
     *
     * <p><b>Favor</b> Corrente de Efeitos "Esmaga e Mói" (tipo de dano vira Perfurante e danos
     * +1d6) — prose. <b>Efeitos Adicionais</b> "exige uso de ambas as mãos … Tempo de Ação dos
     * ataques aumentado em +1PA".
     */
    CORRENTE_ESPINHOSA(
            "Corrente Espinhosa",
            "Uma exótica e robusta corrente de metal, com espinhos e lâminas entre seus pesados "
                    + "e desajeitados elos.",
            ItemWeightClass.HEAVY,
            ItemRarity.RARE,
            15,
            DamageBase.of(2, 0),
            Range.DISTANCIA_MUITO_CURTA,
            CriticalEffectType.DESMEMBRAR,
            16,
            ItemFavor.builder()
                    .description("Possui a Corrente de Efeitos – Esmaga e Mói: adicionalmente o "
                            + "tipo de dano causado é Perfurante e os danos aumentam em +1d6.")
                    .additionalEffects("Grande, lenta e extremamente pesada: exige uso de "
                            + "ambas as mãos para atacar e o Tempo de Ação dos ataques é "
                            + "aumentado em +1PA.")
                    .build()),

    /**
     * Espada Chicote (Leve/Mítico) — Preço 26. Dano 1d6+1, Tipo Esmagamento, Efeito Crítico
     * Guilhotina (17), Alcance Adjacente, Requisito Nenhum.
     *
     * <p><b>Favor</b> Corrente de Efeitos "Marcar Território" (o ataque atinge um alvo
     * adicional adjacente ao primário por até 1d6 de dano) — prose. Multi-target attacks have a
     * hook ({@code Feat#resolveAdditionalTargets}) but an item-granted extra target is not
     * wired. <b>Efeitos Adicionais</b> "recebe o Aprimoramento Alcance Estendido mesmo se não
     * for Obra-Prima; atacar alvos não adjacentes reduz o Dano Base para 1d6".
     */
    ESPADA_CHICOTE(
            "Espada Chicote",
            "",
            ItemWeightClass.LIGHT,
            ItemRarity.MYTHIC,
            26,
            DamageBase.of(1, 1),
            Range.ADJACENTE,
            CriticalEffectType.GUILHOTINA,
            17,
            ItemFavor.builder()
                    .description("Possui a Corrente de Efeitos – Marcar Território: este "
                            + "ataque atinge um personagem adicional, que sofre até 1d6 pontos "
                            + "de dano, apenas se estiver adjacente ao alvo primário e se suas "
                            + "Defesas forem superadas.")
                    .additionalEffects("Recebe o Aprimoramento Alcance Estendido mesmo se não "
                            + "for uma Obra-Prima; atacar alvos que não estejam adjacentes "
                            + "reduz o Dano Base desta Arma para 1d6.")
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

    WhipItem(final String name, final String description, final ItemWeightClass weightClass,
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
        return ItemCategory.WHIP;
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

    /** Always 0 — a whip grants no Defesa Física. */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — a whip grants no Defesa Mágica. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a whip carries no Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }
}
