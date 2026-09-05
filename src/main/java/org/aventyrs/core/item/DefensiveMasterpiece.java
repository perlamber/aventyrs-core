package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import lombok.Getter;

/**
 * The catalog of Obra-Prima options that can be fitted to a defensive item. The enum holds the
 * shared rules entry; {@link ItemMasterpiece} holds choices made for one forged copy.
 */
@Getter
public enum DefensiveMasterpiece implements Masterpiece {
    // TODO: needs a magic-only, minor-critical-margin hook carried through SpellCastingService.
    CONJURADORA("Conjuradora", ItemRarity.UNCOMMON, 0, 0, 1, requirements(AttributeDomain.FOCUS, 3),
            "Margem Crítica Menor de Magias aumentada em -1.", null),
    REFORCADA("Reforçada", ItemRarity.COMMON, 1, 0, 0, requirements(AttributeDomain.DEXTERITY, 3),
            "Bônus em DF muda para +2.", null),
    RUNICA("Rúnica", ItemRarity.COMMON, 0, 1, 0, requirements(AttributeDomain.STRENGTH, 3),
            "Bônus em DM muda para +2.", null),
    EQUILIBRADA("Equilibrada", ItemRarity.UNCOMMON, 1, 1, 0,
            requirements(AttributeDomain.STRENGTH, 3, AttributeDomain.GNOSE, 3),
            "Danos sofridos reduzidos em -1.", null),
    MAGISTRAL("Magistral", ItemRarity.UNCOMMON, 0, 0, 0,
            requirements(AttributeDomain.DEXTERITY, 3, AttributeDomain.GNOSE, 3),
            "Bônus na Defesa escolhida muda para +3.",
            "Concede Bônus de +2 em DF ou DM, definido na criação deste equipamento."),
    SOB_MEDIDA("Sob Medida", ItemRarity.UNCOMMON, 0, 0, 0, requirements(AttributeDomain.FOCUS, 3),
            "Concede uma Ação Livre ou uma Reação adicional, definido na criação deste equipamento.",
            "Pode receber 2 tipos de Aprimoramento de Obra-Prima adicionais."),
    // TODO: needs per-Round first-monster-attack/effect tracking and spell-target classification.
    BANHADA_EM_PRATA("Banhada em Prata", ItemRarity.UNCOMMON, 1, 1, 1, requirements(AttributeDomain.GNOSE, 3),
            "Bônus Defensivos mudam para +3 para resistir ao primeiro ataque ou efeito de um monstro a cada Rodada.",
            "Benefícios desta Obra-Prima são aplicados apenas para resistir a ataques e efeitos de Monstros e para Conjurar Magias em monstros alvos."),
    // TODO: magic damage/healing effects are authored prose; Spell has no numeric effect to increase.
    BANHADA_EM_OURO("Banhada em Ouro", ItemRarity.RARE, 0, 1, 1, requirements(AttributeDomain.FOCUS, 3),
            "Efeitos de Danos e Curas Mágicas de suas magias aumentam em +2.", "Vantagem em rolagens de Persuasão."),
    // TODO: RM and special-material classification for attack sources do not exist.
    DYOSPIROS("Material Especial - Dyospiros", ItemRarity.UNCOMMON, 0, 2, 0, requirements(AttributeDomain.FOCUS, 3),
            "Concede RM, exceto para resistir à efeitos de Armas de Dyospiros.",
            "Apenas equipamentos do Escudo Médios ou Pesados podem ser feitas deste Material."),
    // TODO: needs a Monster Ability model and an activated masterpiece-Favor mechanism.
    COURO_DE_MONSTRO("Material Especial - Couro de Monstro", ItemRarity.RARE, 1, 1, 1, requirements(AttributeDomain.GNOSE, 3),
            "Fornece uma Habilidade Monstruosa Deviante ou Predador, definida aleatoriamente na construção do item.",
            "Ativar o Favor 1PA e 3PM, Duração 2 Rodadas."),
    // TODO: needs retaliation damage, melee-source and special-material classifications.
    GELO_VERDADEIRO("Material Especial - Gelo Verdadeiro", ItemRarity.RARE, 2, 0, 0, requirements(AttributeDomain.STRENGTH, 3),
            "Atacantes Corpo-a-Corpo sofrem 3 pontos de danos Físico Elemental: Gelo.",
            "Dano sofrido reduzido em -1, exceto para resistir a danos de armas de Gelo Verdadeiro."),
    // TODO: needs critical resistance, first-magic-effect Scene tracking and RM.
    MITRAL("Material Especial - Mitral", ItemRarity.EPIC, 1, 2, 1, requirements(AttributeDomain.DEXTERITY, 3),
            "Resistência à Críticos. Bônus em DM aumenta para +6 para resistir ao primeiro efeito mágico da Cena.",
            "Concede RM ao usuário. Equipamento base é considerado uma Categoria de Peso inferior."),
    // TODO: needs elemental resistance, dragon-element typing and shield-as-weapon resolution.
    COURO_DE_DRAGAO("Material Especial - Couro de Dragão", ItemRarity.MYTHIC, 2, 2, 1, requirements(AttributeDomain.VIGOR, 3),
            "RE muda para Todos os Elementos. Dano do Elemento do Dragão é reduzido à metade (efeito de Meio-Dano).",
            "Usuário ganha RE para resistir a efeitos do Elemento do Dragão. Escudos feitos de Couro de Dragão, quando usados para atacar, causam +1d6 pontos de danos e o tipo de dano muda para Físico Elemental."),
    // TODO: needs per-item incoming-damage state, round parity and physical-damage classification.
    ADAMANTINA("Material Especial - Adamantina", ItemRarity.EPIC, 2, 1, 0, requirements(AttributeDomain.VIGOR, 3),
            "Primeiro dano sofrido em Rodadas Ímpares é reduzido à metade (efeito de Meio-Dano). Bônus em DF aumenta para +6 para resistir ao primeiro ataque que inflija danos físicos da Cena.",
            "Concede RD ao usuário. Equipamento base é considerado uma Categoria de Peso Superior."),
    // TODO: needs item agreement state, a subordinate model and agreement-scoped RA/critical-margin resolution.
    ESPIRITO_UMBRAL("Material Especial - Espírito Umbral", ItemRarity.MYTHIC, 1, 1, 0, requirements(AttributeDomain.CHARISMA, 5),
            "Apenas quando em acordo, a armadura conta como um Subordinado de tipo determinado em sua confecção.",
            "Este item está vivo e é senciente, pode se comunicar telepaticamente com seu usuário e costuma ajudar seu portador quando seus objetivos estão alinhados.");

    private final String name;
    private final ItemRarity rarity;
    private final int physicalDefenseBonus;
    private final int magicDefenseBonus;
    private final int castingBonus;
    private final MasterpieceRequirements requirements;
    private final String favorDescription;
    private final String additionalEffects;

    DefensiveMasterpiece(final String name, final ItemRarity rarity, final int physicalDefenseBonus,
                         final int magicDefenseBonus, final int castingBonus,
                         final MasterpieceRequirements requirements, final String favorDescription,
                         final String additionalEffects) {
        this.name = name;
        this.rarity = rarity;
        this.physicalDefenseBonus = physicalDefenseBonus;
        this.magicDefenseBonus = magicDefenseBonus;
        this.castingBonus = castingBonus;
        this.requirements = requirements;
        this.favorDescription = favorDescription;
        this.additionalEffects = additionalEffects;
    }

    @Override
    public String getDescription() {
        return favorDescription;
    }

    @Override
    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character) {
        if (this == REFORCADA && defenseType == DefenseType.PHYSICAL && requirements.isMetBy(character)) {
            return 2;
        }
        if (this == RUNICA && defenseType == DefenseType.MAGIC && requirements.isMetBy(character)) {
            return 2;
        }
        return Masterpiece.super.getEffectiveDefenseBonus(defenseType, character);
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character) {
        if (!requirements.isMetBy(character)) {
            return 0;
        }

        if (this == EQUILIBRADA && modifierType == ModifierType.DAMAGE_REDUCTION) {
            return 1;
        }
        if (this == ADAMANTINA && modifierType == ModifierType.DAMAGE_REDUCTION) {
            return 2;
        }
        if (this == BANHADA_EM_OURO && modifierType == SkillType.PERSUASAO.getRollBonusType()
                && skillType == SkillType.PERSUASAO) {
            return Skill.ADVANTAGE_BONUS;
        }
        return 0;
    }

    @Override
    public int getWeightClassBonus() {
        return switch (this) {
            case MITRAL -> -1;
            case ADAMANTINA -> 1;
            default -> 0;
        };
    }

    private static MasterpieceRequirements requirements(final AttributeDomain domain, final int value) {
        return requirements(domain, value, null, 0);
    }

    private static MasterpieceRequirements requirements(final AttributeDomain firstDomain, final int firstValue,
                                                         final AttributeDomain secondDomain, final int secondValue) {
        List<ItemRequirements> requirements = secondDomain == null
                ? List.of(new ItemRequirements(firstDomain, firstValue))
                : List.of(new ItemRequirements(firstDomain, firstValue), new ItemRequirements(secondDomain, secondValue));
        return new MasterpieceRequirements(requirements);
    }
}
