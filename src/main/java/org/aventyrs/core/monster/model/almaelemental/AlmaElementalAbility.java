package org.aventyrs.core.monster.model.almaelemental;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ChoiceSpec;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.RecurringDice;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.aventyrs.core.monster.model.MonstrousTraits.ELEMENT;
import static org.aventyrs.core.monster.model.MonstrousTraits.bonus;

/**
 * The Habilidades Monstruosas of the Modelo Alma Elemental — {@code criacao-de-monstros.txt}.
 *
 * <p>Built the way {@code CireneiaAbility} is: every Aprimoramento folded into the hook that answers
 * for it and gated on the Categoria. A Habilidade whose clause needs a system this core lacks keeps
 * the rest applied and names the gap in {@link #getUnappliedNote()}.
 *
 * <p><b>An element-scoped clause only reaches a hit that names its element</b> — one resolved
 * through a {@code DamageDescriptor}; the ordinary {@code DamageInteraction} path carries a bare
 * {@code DamageType}. That is RE's own limit, not this Modelo's.
 */
@Getter
public enum AlmaElementalAbility implements MonstrousAbility {

    /** Anatomia Elemental: RE, Resistência a Críticos and Instinto; then Meio-Dano, then immunity, to the chosen element. */
    SANGUE_ELEMENTAL("Sangue Elemental", PRESA, 
            "Efeito Passivo – Anatomia Elemental.\n"
                    + "“Anatomia Elemental – Escolha um Elemento, este Monstro recebe RE, Resistência a Críticos, Bônus Racial de +2 em Instinto”\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Danos do Elemento escolhido são reduzidos à metade (efeito de Meio-Dano).\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Bônus Racial de Instinto +2\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Imunidade ao Elemento escolhido.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(ELEMENT, MonstrousTraits.elementOptions()));
        }

        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return Map.of(AttributeDomain.INSTINCT, context.isAtLeast(PREDADOR) ? 4 : 2);
        }

        @Override
        public int resolveElementalResistanceInstances(final ElementalType element, final AbilityContext context) {
            return chosenElement(context).filter(element::equals).map(chosen -> 1).orElse(0);
        }

        @Override
        public int resolveCriticalResistance(final AbilityContext context) {
            return MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE;
        }

        @Override
        public boolean halvesDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
            return context.isAtLeast(DEVIANTE) && chosenScope(context).map(scope -> scope.matches(type, descriptor)).orElse(false);
        }

        @Override
        public boolean isImmuneToDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
            return context.isAtLeast(APEX) && chosenScope(context).map(scope -> scope.matches(type, descriptor)).orElse(false);
        }
    },

    /**
     * The chosen Arma Natural, and from Deviante +2 on its attacks. Fúria Elemental's element typing
     * and its Cataclismo are not applied: the Efeito lands on the holder, while "a Arma Natural causa
     * dano Físico Elemental" retypes a future attack's damage, which no self effect can reach.
     */
    ARMAMENTO_ELEMENTAL("Armamento Elemental", PRESA, 
            "Efeito Passivo – Escolha uma Arma Natural, este Monstro possui a Arma Natural escolhida.\n"
                    + "Efeito Ativo – Fúria Elemental [1PA, 2PD]: Escolha um Elemento, durante 3 Rodadas a Arma Natural causa dano Físico Elemental e recebe Cataclismo como Efeito Crítico adicional. Resfriamento 3.\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Rolagens de Ataques e Danos com a Arma Natural recebem Bônus de +2.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Fúria Elemental – ataques infligem Danos Mágicos e recebem a Corrente de Efeitos Monstruosa – Marca Elemental.\n"
                    + "“Marca Elemental – Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas (Efeito de Maldição)”\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Ataques da Arma natural tem Margem Crítica Menor aumentada em +3, também reduzem a Resistência a Correntes de Efeitos do alvo em -2.\n"
                    + "• Fúria Elemental – Duração aumentada em +2 Rodadas, Resfriamento reduzido para 2 Rodadas.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(WEAPON, weaponOptions()),
                    ChoiceSpec.one(ELEMENT, MonstrousTraits.elementOptions()));
        }

        @Override
        public List<NaturalWeapon> resolveNaturalWeapons(final AbilityContext context) {
            return context.pick(WEAPON, NaturalWeapon.class).map(List::of).orElse(List.of());
        }

        /** "Rolagens de Ataques … com a Arma Natural recebem Bônus de +2" — the GD its attacks present. */
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            if (!context.isAtLeast(DEVIANTE)) {
                return 0;
            }
            return context.pick(WEAPON, NaturalWeapon.class)
                    .filter(weapon -> weapon.getSkillType().getRollBonusType() == type)
                    .map(weapon -> ARMAMENTO_ATTACK_BONUS)
                    .orElse(0);
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skill, final AbilityContext context) {
            if (!context.isAtLeast(APEX)) {
                return 0;
            }
            return context.pick(WEAPON, NaturalWeapon.class)
                    .filter(weapon -> weapon.getSkillType() == skill)
                    .map(weapon -> 3)
                    .orElse(0);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Fúria Elemental")
                    .description("Durante " + (apex ? 5 : 3) + " Rodadas a Arma Natural causa dano Físico Elemental "
                            + "e recebe Cataclismo como Efeito Crítico adicional.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(apex ? 5 : 3)
                    .cooldownRounds(apex ? 2 : 3)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Fúria Elemental's element and extra Cataclismo, its Marca Elemental, the +2 on the weapon's "
                    + "damage and the -2 Resistência à Correntes de Efeitos: an Efeito Ativo lands on its holder and "
                    + "cannot retype a later attack's damage. The +2 on attacks covers every attack with the weapon's "
                    + "Perícia, not the weapon alone.";
        }
    },

    /**
     * Retribuição Elemental and the Aura Elemental both land on <i>others</i> — the attacker, every
     * character in the area — so only the Aura's price and Resfriamento are carried.
     */
    SANGUE_ELEMENTAL_DEVIANTE("Sangue Elemental", DEVIANTE, 
            "Efeito Passivo: Escolha um Elemento; Retribuição Elemental.\n"
                    + "“Retribuição Elemental - Atacantes corpo-a-corpo que infligirem danos ao Elemental sofrem 3 pontos de Dano Elemental.”\n"
                    + "Efeito Ativo – Aura Elemental: Cria uma Aura Elemental ao redor do Monstro com Área de Efeito Circular Curta. Outros personagens na área sofrem 1d6 pontos de Dano Físico Elemental. Esta Habilidade só pode ser ativada se o Monstro estiver ferido e tem Duração de 2 Rodadas. Resfriamento 2.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Retribuição Elemental – Dano muda para 1d6 e se torna Mágico Elemental.\n"
                    + "• Explosão Pós-Mortis.\n"
                    + "“Explosão Pós Mortis – Este Monstro explode quando morre e não pode ser revivido. A explosão tem Área de Efeito Circular Média e inflige 2d6+Instinto pontos de Dano Mágico Elemental.”\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Aura Elemental – Alcance muda para Área de Efeito Circular Média e passa a infligir 2d6 pontos de Dano Mágico Elemental.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(ELEMENT, MonstrousTraits.elementOptions()));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Aura Elemental")
                    .description("Outros personagens na área sofrem " + (apex ? "2d6 pontos de Dano Mágico" : "1d6 pontos de Dano Físico")
                            + " Elemental. Apenas se o Monstro estiver ferido.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .durationInRounds(2)
                    .cooldownRounds(2)
                    .usableWhen(sheet -> sheet.getDamageTaken() > 0)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Retribuição Elemental (RETALIATION_DAMAGE is fixed to Natural damage and Envenenado), the Aura's "
                    + "damage to others in its area (geometry) and Explosão Pós-Mortis. Only the Aura's price, "
                    + "Resfriamento and \"apenas se ferido\" are applied.";
        }
    },

    /** Foco +2 and the chosen Árvores de Magia, up to Muda, then Emergente, then Florescente. */
    CONJURACAO_ELEMENTAL("Conjuração Elemental", DEVIANTE, 
            "Efeito Passivo: Foco +2.\n"
                    + "Efeito Ativo – Conjuração Elemental: Aprende 2 Árvores de Magia Elemental é capaz de conjurar Sementes, Brotos e Mudas destas Árvores.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Conjuração Elemental – Aprende uma Árvore de Magia adicional Elemental, Divina ou Profana. É se torna capaz de conjurar Magias Emergentes.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Conjuração Elemental – Aprende uma Árvore de Magia adicional Elemental, Divina ou Profana. É se torna capaz de conjurar Magias Florescentes.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            List<ChoiceSpec> specs = new ArrayList<>();
            specs.add(ChoiceSpec.many(TREES, MonstrousTraits.treeOptions(MonstrousTraits::isElemental), 2));
            if (category.isAtLeast(PREDADOR)) {
                specs.add(ChoiceSpec.one(PREDADOR_TREE, MonstrousTraits.treeOptions(MonstrousTraits::isElementalDivineOrProfane)));
            }
            if (category.isAtLeast(APEX)) {
                specs.add(ChoiceSpec.one(APEX_TREE, MonstrousTraits.treeOptions(MonstrousTraits::isElementalDivineOrProfane)));
            }
            return specs;
        }

        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return Map.of(AttributeDomain.FOCUS, 2);
        }

        @Override
        public List<Spell> resolveGrantedSpells(final AbilityContext context) {
            BranchLevel deepest = context.isAtLeast(APEX) ? BranchLevel.FLORESCENTE
                    : context.isAtLeast(PREDADOR) ? BranchLevel.EMERGENTE : BranchLevel.MUDA;
            List<String> trees = new ArrayList<>(context.picks(TREES));
            trees.addAll(context.picks(PREDADOR_TREE));
            trees.addAll(context.picks(APEX_TREE));
            return MonstrousTraits.spellsUpTo(trees, deepest);
        }

        @Override
        public ImplementationStatus getImplementationStatus() {
            return ImplementationStatus.APPLIED;
        }
    },

    /** The Arma de Sopro, and Sopro Cataclísmico's +2d6 and +3 Margem Crítica Menor for the Turn. */
    SOPRO_ELEMENTAL("Sopro Elemental", PREDADOR, 
            "Efeito Passivo - Adquire a Arma de Sopro.\n"
                    + "Efeito Ativo – Sopro Cataclísmico [+1PA, 2PD]: Dano da Arma de Sopro aumenta em +2d6 neste Turno e Margem Crítica Menor aumenta em +3.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Sopro Cataclísmico –Alcance muda para Cone Longo e o dano não é reduzido pela distância percorrida.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Sopro Cataclísmico – Alcance muda para Cone Muito Longo e aplica a Corrente de Efeito Monstruosa – Sopro Apocalíptico.\n"
                    + "“Sopro Apocalíptico – Aplica a Corrente de Efeitos Menor Cataclismo em cada alvo.”") {
        @Override
        public List<NaturalWeapon> resolveNaturalWeapons(final AbilityContext context) {
            return List.of(NaturalWeapon.ARMA_DE_SOPRO);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Sopro Cataclísmico")
                    .description("Dano da Arma de Sopro aumenta em +2d6 neste Turno e Margem Crítica Menor aumenta em +3.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .effect(bonus(ModifierType.EXTRA_DAMAGE_DICE, 2, 1, "Sopro Cataclísmico"))
                    .effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, 1, "Sopro Cataclísmico"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Cone ranges and \"não é reduzido pela distância\" (geometry), and the Abominação's Sopro "
                    + "Apocalíptico (no Corrente de Efeitos reaches a target). The +2d6 and +3 ride every attack "
                    + "this Turn, not the Sopro alone.";
        }
    },

    /** Summons are not built: only Invocar Elemental's price reaches the sheet. */
    INVOCAR_ELEMENTAL("Invocar Elemental", PREDADOR, 
            "Efeito Passivo: Este monstro possui um Subordinado que lhe serve ou auxilia.\n"
                    + "Efeito Ativo – Invocar Elemental [3PA, 4PD]: Invoca dois Monstros Elementais do tipo Presa para ajudar.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Invocar Elemental – o tipo dos Monstros invocados é Deviante.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Possui um Subordinado adicional\n"
                    + "• Invocar Elemental – o tipo dos Monstros invocados é Predador, alternativamente pode optar por invocar um único Monstro do tipo Apex.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonsterCategory summoned = context.isAtLeast(ABOMINACAO) ? PREDADOR : context.isAtLeast(APEX) ? DEVIANTE : PRESA;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Invocar Elemental")
                    .description("Invoca dois Monstros Elementais do tipo " + summoned.name() + ".")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(4)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Subordinado and the summoned Elementais: nothing spawns a creature from an Efeito Ativo — "
                    + "the Mestre places them. Only the price is applied.";
        }
    },

    /** Multiplicador de PD +1; Tormenta Cataclísmica's own 1d6 regeneration and Roubo de Vida 2. */
    TORMENTA_CATACLISMICA("Tormenta Cataclísmica", APEX, 
            "Efeito Passivo – Multiplicador de PD +1.\n"
                    + "Efeito Ativo – Tormenta Cataclísmica [4PA, 5PD]: Altera o terreno por 5 Rodadas, criando uma Tormenta Cataclísmica. Enquanto estiver no terreno transformado em Tormenta Cataclísmica recupera 1d6PV por Rodada e seus ataques recebem Roubo de Vida 2.\n"
                    + "“Tormenta Cataclísmica – Área Circular Muito Longa, o terreno é tomado por energia elemental, infligindo 1d6+Instinto pontos de Dano Físico Elemental a todos os personagens, Equipamentos e objetos em seu interior.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Tormenta Cataclísmica – Os danos da Tormenta Cataclísmica mudam para Mágico Elemental.\n"
                    + "• Tormenta Cataclísmica – Uma tempestade de energia elemental cai sobre o terreno, causando 1d6 Pontos de Dano Mágico Elemental a todos os personagens.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DETERMINATION_MULTIPLIER ? 1 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Tormenta Cataclísmica")
                    .description("Por 5 Rodadas: recupera 1d6PV por Rodada e seus ataques recebem Roubo de Vida 2.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(5)
                    .durationInRounds(TORMENTA_DURATION)
                    .effect(() -> RecurringDice.healing(Dice.of(1), TORMENTA_DURATION, "Tormenta Cataclísmica"))
                    .effect(() -> new LifeSteal(2, Optional.of(TORMENTA_DURATION)))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The terrain it transforms and the damage the storm deals everyone in it: effect-created terrain "
                    + "and area damage need geometry.";
        }
    },

    /** Fisiologia Abissal or Celestial: immunities, Conjuração +2, Roubo de Vida or 1d6 regeneration. */
    ASCENCAO("Ascenção", APEX, 
            "Efeito Passivo – Adquire Fisiologia Abissal ou Fisiologia Celestial.\n"
                    + "“Fisiologia Abissal – Imu4nidade a Doenças e Maldições, Conjuração +2 e Ataques recebem Roubo de Vida 2 e a Corrente de Efeito Monstruosa – Murchar. Danos Elementais se tornam Profanos em substituição aos seus tipos e ignoram RM.”\n"
                    + "“Murchar – Alvo sofre Redutor de -2 Multiplicador de PV por 2 Rodadas (efeito de Maldição).\n"
                    + "“Fisiologia Celestial – Imunidade a Doenças e Encantamentos Nocivos, Conjuração +2, recuperam 1d6PV a cada Rodada e seus ataques recebem a Corrente de Efeitos Monstruosa - Expurgo. Danos Elementais se tornam Sagrados em substituição aos seus tipos e ignoram RM.”\n"
                    + "“Expurgo – Desativa Habilidades Ativas e Encantamentos do alvo.”\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Imunidade a Efeitos Críticos e Correntes de Efeitos que não sejam de fontes Abissais, Celestiais ou Primordiais.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(ASPECT, List.of(ABISSAL, CELESTIAL)));
        }

        @Override
        public boolean isImmuneToCondition(final ConditionType conditionType, final AbilityContext context) {
            // Doenças for both; Maldições for the Abissal. The Celestial's "Encantamentos Nocivos" has no
            // Condição of its own to refuse.
            return conditionType == ConditionType.DOENTE
                    || (conditionType == ConditionType.AMALDICOADO && ABISSAL.equals(context.pick(ASPECT)));
        }

        /** "Conjuração +2" — the Domínio do Mana GD it presents. */
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DOMINIO_DO_MANA_ROLL_BONUS ? 2 : 0;
        }

        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            return CELESTIAL.equals(context.pick(ASPECT))
                    ? List.of(RecurringDice.healing(Dice.of(1), null, "Fisiologia Celestial"))
                    : List.of(new LifeSteal(2, Optional.empty()));
        }

        /** Abominação: "Imunidade a Efeitos Críticos … que não sejam de fontes Abissais, Celestiais ou Primordiais". */
        @Override
        public Set<CriticalEffectType> resolveCriticalEffectImmunities(final AbilityContext context) {
            return context.isAtLeast(ABOMINACAO) ? EnumSet.allOf(CriticalEffectType.class) : Set.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Murchar and Expurgo (no Corrente de Efeitos reaches a target), elemental damage turned Profano or "
                    + "Sagrado and ignoring RM (no damage type for either), immunity to Encantamentos Nocivos, and the "
                    + "Abominação's exception for Abissal, Celestial and Primordial sources (critical effects carry "
                    + "no source).";
        }
    };

    /** {@link #ARMAMENTO_ELEMENTAL}'s pick: the Arma Natural. */
    public static final String WEAPON = "weapon";

    /** {@link #CONJURACAO_ELEMENTAL}'s picks: the two Árvores learned at Deviante. */
    public static final String TREES = "trees";

    /** {@link #CONJURACAO_ELEMENTAL}'s Predador tree. */
    public static final String PREDADOR_TREE = "predadorTree";

    /** {@link #CONJURACAO_ELEMENTAL}'s Apex tree. */
    public static final String APEX_TREE = "apexTree";

    /** {@link #ASCENCAO}'s pick. */
    public static final String ASPECT = "aspect";
    public static final String ABISSAL = "ABISSAL";
    public static final String CELESTIAL = "CELESTIAL";

    private static final int ARMAMENTO_ATTACK_BONUS = 2;
    private static final int TORMENTA_DURATION = 5;

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    AlmaElementalAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ALMA_ELEMENTAL;
    }

    /** APPLIED when every clause is, PARTIAL when {@link #getUnappliedNote()} names one that isn't. */
    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }

    private static Optional<ElementalType> chosenElement(final AbilityContext context) {
        return context.pick(ELEMENT, ElementalType.class);
    }

    private static Optional<DamageScope> chosenScope(final AbilityContext context) {
        return chosenElement(context).map(DamageScope::element);
    }

    /** Every Arma Natural but the unarmed strike, which every creature already has. */
    private static List<String> weaponOptions() {
        return Arrays.stream(NaturalWeapon.values()).filter(weapon -> weapon != NaturalWeapon.ATAQUE_DESARMADO)
                .map(Enum::name).toList();
    }
}
