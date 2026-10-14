package org.aventyrs.core.item;

import org.aventyrs.core.character.Resistance;
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
            return spell != null && (spell.getPrimaryType() == MagicType.ENCANTAMENTO
                    || spell.getSecondaryType() == MagicType.ENCANTAMENTO
                    || spell.getPrimaryType() == MagicType.MALDICAO
                    || spell.getSecondaryType() == MagicType.MALDICAO) ? 1 : 0;
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
    // Encaixe (the Pedra do Poder socket) was removed by table ruling, 2026-10-12: any Obra-Prima
    // Armadura or Escudo now takes a stone — see Item#acceptsPowerStone.
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
        /**
         * Asks the wielder, not the weapon: a Talento can make an ordinary blade count as an Arma
         * Natural for its holder (see {@code Feat#reclassifiesAsNaturalWeapon}), and this Favor's
         * "Ataques efetuados com Armas Naturais" follows that. Falls back to the weapon's own
         * category when no character is supplied.
         */
        @Override
        public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
            return naturalWeapon(weapon, character) ? 1 : 0;
        }

        /** Its Favor: "Margem Crítica Menor de Ataques efetuados com Armas Naturais aumenta em +1 número". */
        @Override
        public int resolveCriticalMarginIncrease(final Weapon weapon, final Character character) {
            return naturalWeapon(weapon, character) ? 1 : 0;
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
            // "Recebe RE" — one instance. The element names the hit whatever its nature (Elemental or Mágico
            // Elemental) — matchesElement suffices.
            return matchesElement(selectedElementalType, damageDescriptor) ? Resistance.RE.instance() : 0;
        }
    },
    /**
     * Both halves read by {@link DefensiveEnhancements} (core 0.1.6.11): the hostile Duração by {@code
     * SpellCastingService} when a hostile Encantamento/Maldição lands, the -1 by {@code DamageService} on any Mágico
     * hit, whatever its qualifier (table ruling, 2026-10-12).
     */
    BENCAO_ELDURIANA("Benção Elduriana", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Duração de Encantamentos e Maldições hostis reduzidas em -1 Rodada.",
            "Danos Mágicos sofridos reduzidos em -1."),
    /**
     * Read by {@link DefensiveEnhancements#damageTakenReduction} (core 0.1.6.11): -1 on an Elemental or element-naming
     * hit, -1 on a Físico one.
     */
    BENCAO_VULCANA("Benção Vulcana", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Danos Elementais sofridos reduzidos em -1.", "Danos Físicos sofridos reduzidos em -1."),
    /**
     * Its Danos column reaches every attack ({@link DefensiveEnhancements#damageRollBonus}); being attacked opens the
     * +2 window until the end of the wearer's next Turn ({@link DefensiveEnhancements#onAttacked}, core 0.1.6.11).
     */
    BENCAO_YMIRIANA("Benção Ymiriana", ItemRarity.RARE, 0, 0, 0, 1, 0,
            "O Bônus em Danos aumenta para +2 por 1 Rodada após sofrer um ataque.", null),
    /**
     * Vantagem em Furtividade always; its Favor's "Vantagem adicional" (+2, the Perito ruling) while the Scene's
     * terrain is the one chosen at creation ({@link ItemImprovement#camuflada}, core 0.1.6.11).
     */
    CAMUFLADA("Camuflada", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            "Vantagem adicional nas rolagens de Furtividade em tipo de ambiente definido na criação do item.",
            "Apenas equipamentos do tipo Armadura. Vantagem em rolagens de Furtividade.") {
        @Override
        public int resolveBonus(final ModifierType modifierType, final SkillType skillType,
                                final Character character) {
            return skillType == SkillType.FURTIVIDADE && modifierType == skillType.getRollBonusType()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public boolean requiresTerrainChoice() {
            return true;
        }

        @Override
        public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character,
                                final org.aventyrs.core.scene.SceneContext sceneContext,
                                final org.aventyrs.core.scene.TerrainType selectedTerrain) {
            int base = resolveBonus(modifierType, skillType, character);
            boolean chosenTerrain = base > 0 && selectedTerrain != null && sceneContext != null
                    && sceneContext.isTerrain(selectedTerrain);
            return base + (chosenTerrain ? Skill.ADVANTAGE_BONUS : 0);
        }
    },
    /**
     * Reported by {@link DefensiveEnhancements#retaliationAgainst} (core 0.1.6.11): its Favor's 2 Dano Físico to a
     * melee attacker and 2 to their weapon, hit or miss, on the primary target only (table ruling, 2026-10-12).
     */
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
    /** Reported by {@link DefensiveEnhancements#retaliationAgainst} like {@link #ESPINHOSA} (core 0.1.6.11). */
    RIGIDEZ_REFLETORA("Rigidez Refletora", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            null, "Armas de atacantes corpo-a-corpo sofrem 2 pontos de danos, projeteis são destruídos."),
    /**
     * {@link DefensiveEnhancements#reapSouls}, a caller-driven sweep after damage (core 0.1.6.11); its Danos column
     * reaches every attack.
     */
    SOLVE_VIDAS("Maldição: Solve-Vidas", ItemRarity.MYTHIC, 0, 0, 0, 1, 0,
            "Sempre que um Personagem inimigo vivo tiver seus PV reduzidos à 0 ou menos em até Distância Curta ele será imediatamente morto e você recupera uma quantidade de PV igual ao Vigor dele.", null),
    /**
     * {@link DefensiveEnhancements#onAffectedBySpell}, caller-driven once per Magia landing (core 0.1.6.11). TODO: the
     * "efeitos de habilidades mágicas de Monstros" half — no Habilidade Monstruosa is classified as magical.
     */
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

    /** Every entry here is fitted to a defensive item, so all of them price off the Armaduras column. */
    @Override
    public EnhancementPriceCategory getPriceCategory() {
        return EnhancementPriceCategory.ARMOR;
    }

    public boolean requiresDefenseChoice() {
        return false;
    }

    public boolean requiresElementalTypeChoice() {
        return false;
    }

    /**
     * Whether a creation-time terrain is asked for — {@link #CAMUFLADA}. Unlike the other two choices it is optional
     * ({@link ItemImprovement#of} accepts the bare form), so an older copy without one still loads.
     */
    public boolean requiresTerrainChoice() {
        return false;
    }

    /** The terrain-aware form {@link ItemImprovement} calls; defaults to the three-argument {@code resolveBonus}. */
    public int resolveBonus(final ModifierType modifierType, final SkillType skillType, final Character character,
                            final org.aventyrs.core.scene.SceneContext sceneContext,
                            final org.aventyrs.core.scene.TerrainType selectedTerrain) {
        return resolveBonus(modifierType, skillType, character);
    }

    /** Whether weapon counts as an Arma Natural for character — asking the wielder, as every such clause does. */
    private static boolean naturalWeapon(final Weapon weapon, final Character character) {
        if (weapon == null) {
            return false;
        }
        return character == null
                ? weapon.getCategory() == ItemCategory.NATURAL_WEAPON
                : character.treatsAsNaturalWeapon(weapon);
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
