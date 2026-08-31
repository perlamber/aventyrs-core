package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import lombok.Getter;

/** The catalog of Aprimoramentos that can be fitted to defensive items. */
@Getter
public enum DefensiveImprovement implements Improvement {
    OCULTA("Oculta", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Vantagem em rolagens de Furtividade.", "Item tem aparência de item comum.") {
        @Override
        public int resolveBonus(final ModifierType modifierType, final SkillType skillType,
                                final Character character) {
            return skillType == SkillType.FURTIVIDADE && modifierType == skillType.getRollBonusType()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    ENCANTADORA("Encantadora", ItemRarity.RARE, 0, 0, 0, 0, 1,
            "Duração de Encantamentos e Maldições +1 Rodada.", null) {
        @Override
        public int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
            return spell.getPrimaryType() == MagicType.ENCANTAMENTO
                    || spell.getSecondaryType() == MagicType.ENCANTAMENTO
                    || spell.getPrimaryType() == MagicType.MALDICAO
                    || spell.getSecondaryType() == MagicType.MALDICAO ? 1 : 0;
        }
    },
    RESISTENTE("Resistente", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            null, "PV deste Equipamento aumentam em +10, danos causados a este equipamento são reduzidos em -1.") {
        @Override
        public int getHardnessBonus() {
            return 10;
        }

        @Override
        public int getItemDamageReduction() {
            return 1;
        }
    },
    // The enabler for a Pedra do Poder — see PowerStone / PowerStoneType and
    // AbstractItem#setPowerStone, which gates the socket on this constant being fitted.
    ENCAIXE("Encaixe", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            null, "Apenas Armaduras e Escudos, permite encaixe de Pedra do Poder."),
    AJUSTADA("Ajustada", ItemRarity.COMMON, 1, 0, 0, 0, 0,
            null, "PV deste item é reduzido em -5, Categoria de Peso do item reduzida em -1 nível.") {
        @Override
        public int getHardnessBonus() {
            return -5;
        }

        @Override
        public int getWeightClassBonus() {
            return -1;
        }
    },
    BENCAO_DE_PROTECAO("Benção de Proteção", ItemRarity.UNCOMMON, 1, 1, 0, 0, 0,
            "Bônus de Defesas aumentam para +3 nas três primeiras Rodadas do combate. Se você sofrer Danos durante este efeito este Bônus aumenta para +5 até o fim da Rodada 5.", null) {
        @Override
        public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                             final org.aventyrs.core.scene.SceneContext sceneContext,
                                             final Item item) {
            if (sceneContext == null || !sceneContext.isCombatScene()) {
                return super.getEffectiveDefenseBonus(defenseType, character, sceneContext, item);
            }
            if (item != null && item.hasActiveImprovementEffect(sceneContext)) {
                return PROTECTION_BLESSING_EXTENDED_DEFENSE_BONUS;
            }
            return sceneContext.isWithinFirstCombatRounds(PROTECTION_BLESSING_INITIAL_LAST_ROUND)
                    ? PROTECTION_BLESSING_INITIAL_DEFENSE_BONUS
                    : super.getEffectiveDefenseBonus(defenseType, character, sceneContext, item);
        }

        @Override
        public void onFinalDamageTaken(final Item item, final int finalDamage,
                                       final org.aventyrs.core.scene.SceneContext sceneContext) {
            if (finalDamage > 0 && sceneContext != null
                    && sceneContext.isWithinFirstCombatRounds(PROTECTION_BLESSING_INITIAL_LAST_ROUND)) {
                item.activateImprovementEffect(sceneContext, PROTECTION_BLESSING_EXTENDED_LAST_ROUND);
            }
        }
    },
    BENCAO_SELVAGEM("Benção Selvagem", ItemRarity.RARE, 0, 0, 1, 0, 0,
            "Margem Crítica Menor de Ataques efetuados com Armas Naturais aumenta em +1 número.",
            "Dano Base de Ataques efetuados com Armas Naturais aumenta em +1.") {
        @Override
        public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
            return weapon.getCategory() == ItemCategory.NATURAL_WEAPON ? 1 : 0;
        }
    },
    BENCAO_ELEMENTAL("Benção Elemental", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Defesas +2 para resistir a ataques e magias do Elemento escolhido.",
            "Recebe RE para resistir a um Elemento (escolhido na criação).") {
        @Override
        public boolean requiresElementalTypeChoice() {
            return true;
        }

        @Override
        public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                             final org.aventyrs.core.scene.SceneContext sceneContext,
                                             final Item item, final DefenseType selectedDefense,
                                             final ElementalType selectedElementalType,
                                             final DamageDescriptor damageDescriptor) {
            return matchesElement(selectedElementalType, damageDescriptor) ? 2 : 0;
        }

        @Override
        public int resolveDamageReduction(final DamageDescriptor damageDescriptor, final Character character,
                                          final ElementalType selectedElementalType) {
            return matchesElement(selectedElementalType, damageDescriptor)
                    && (damageDescriptor.damageType() == org.aventyrs.core.character.DamageType.ELEMENTAL
                    || damageDescriptor.damageType() == org.aventyrs.core.character.DamageType.FISICO_ELEMENTAL)
                    ? 2 : 0;
        }
    },
    // TODO: hostile Enchantment/Curse duration and magic-damage-scoped mitigation do not exist.
    BENCAO_ELDURIANA("Benção Elduriana", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Duração de Encantamentos e Maldições hostis reduzidas em -1 Rodada.",
            "Danos Mágicos sofridos reduzidos em -1."),
    // TODO: damage-type-scoped mitigation does not exist.
    BENCAO_VULCANA("Benção Vulcana", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Danos Elementais sofridos reduzidos em -1.", "Danos Físicos sofridos reduzidos em -1."),
    // TODO: needs an after-being-attacked trigger and one-Rodada attack bonus state.
    BENCAO_YMIRIANA("Benção Ymiriana", ItemRarity.RARE, 0, 0, 0, 1, 0,
            "O Bônus em Danos aumenta para +2 por 1 Rodada após sofrer um ataque.", null),
    // TODO: needs a terrain choice and the SceneContext-aware item-bonus path.
    CAMUFLADA("Camuflada", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            "Vantagem adicional nas rolagens de Furtividade em tipo de ambiente definido na criação do item.",
            "Apenas equipamentos do tipo Armadura. Vantagem em rolagens de Furtividade.") {
        @Override
        public int resolveBonus(final ModifierType modifierType, final SkillType skillType,
                                final Character character) {
            return skillType == SkillType.FURTIVIDADE && modifierType == skillType.getRollBonusType()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    // TODO: retaliation damage and damage to attacking weapons are not modeled.
    ESPINHOSA("Espinhosa", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            "Dano dos espinhos aumentado para 2.", "Atacantes corpo-a-corpo e suas armas sofrem 1 pontos de Dano Físico ao te atacar."),
    CAMADA_DE_REFORCO("Camada de Reforço", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Defesas +1 (Apenas Escudos recebem este Favor).",
            "Bônus de +1 em DF ou DM, definido na criação. Equipamentos do tipo Escudo, em substituição aos efeitos anteriores recebem Defesas +1.") {
        @Override
        public boolean requiresDefenseChoice() {
            return true;
        }

        @Override
        public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                             final org.aventyrs.core.scene.SceneContext sceneContext,
                                             final Item item, final DefenseType selectedDefense) {
            return selectedDefense == null || selectedDefense == defenseType ? 1 : 0;
        }
    },
    // TODO: damage to weapons and projectile destruction are not modeled.
    RIGIDEZ_REFLETORA("Rigidez Refletora", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            null, "Armas de atacantes corpo-a-corpo sofrem 2 pontos de danos, projeteis são destruídos."),
    // TODO: needs living-character classification, reduced-to-zero trigger, range and life-steal resolution.
    SOLVE_VIDAS("Maldição: Solve-Vidas", ItemRarity.MYTHIC, 0, 0, 0, 1, 0,
            "Sempre que um Personagem inimigo vivo tiver seus PV reduzidos à 0 ou menos em até Distância Curta ele será imediatamente morto e você recupera uma quantidade de PV igual ao Vigor dele.", null),
    // TODO: needs a non-seed spell-effect trigger and recovery from magical Monster abilities.
    LADRA_DO_AETHER("Maldição: Ladra do AEther", ItemRarity.MYTHIC, 0, 0, 1, 1, 0,
            "Sempre que você for afetado por efeitos de Magias que não sejam sementes ou efeitos de habilidades mágicas de Monstros você recupera 1PM e 1PD.", null);

    public static final int PROTECTION_BLESSING_START_ROUND = 1;
    public static final int PROTECTION_BLESSING_INITIAL_LAST_ROUND = 3;
    public static final int PROTECTION_BLESSING_EXTENDED_LAST_ROUND = 5;
    public static final int PROTECTION_BLESSING_INITIAL_DEFENSE_BONUS = 3;
    public static final int PROTECTION_BLESSING_EXTENDED_DEFENSE_BONUS = 5;

    private final String name;
    private final ItemRarity rarity;
    private final int physicalDefenseBonus;
    private final int magicDefenseBonus;
    private final int attackBonus;
    private final int damageBonus;
    private final int castingBonus;
    private final String favorDescription;
    private final String additionalEffects;

    DefensiveImprovement(final String name, final ItemRarity rarity, final int physicalDefenseBonus,
                         final int magicDefenseBonus, final int attackBonus, final int damageBonus,
                         final int castingBonus, final String favorDescription, final String additionalEffects) {
        this.name = name;
        this.rarity = rarity;
        this.physicalDefenseBonus = physicalDefenseBonus;
        this.magicDefenseBonus = magicDefenseBonus;
        this.attackBonus = attackBonus;
        this.damageBonus = damageBonus;
        this.castingBonus = castingBonus;
        this.favorDescription = favorDescription;
        this.additionalEffects = additionalEffects;
    }

    @Override
    public String getDescription() {
        return favorDescription;
    }

    public boolean requiresDefenseChoice() {
        return false;
    }

    public boolean requiresElementalTypeChoice() {
        return false;
    }

    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final org.aventyrs.core.scene.SceneContext sceneContext,
                                        final Item item, final DefenseType selectedDefense) {
        return getEffectiveDefenseBonus(defenseType, character, sceneContext, item);
    }

    public int getEffectiveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final org.aventyrs.core.scene.SceneContext sceneContext,
                                        final Item item, final DefenseType selectedDefense,
                                        final ElementalType selectedElementalType,
                                        final DamageDescriptor damageDescriptor) {
        return getEffectiveDefenseBonus(defenseType, character, sceneContext, item, selectedDefense);
    }

    public int resolveDamageReduction(final DamageDescriptor damageDescriptor, final Character character,
                                      final ElementalType selectedElementalType) {
        return 0;
    }

    private static boolean matchesElement(final ElementalType selectedElementalType,
                                         final DamageDescriptor damageDescriptor) {
        return selectedElementalType != null && damageDescriptor != null
                && (selectedElementalType == ElementalType.TODOS
                || selectedElementalType == damageDescriptor.elementalType());
    }
}
