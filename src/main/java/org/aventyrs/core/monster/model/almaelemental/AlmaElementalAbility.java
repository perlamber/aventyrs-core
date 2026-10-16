package org.aventyrs.core.monster.model.almaelemental;

import org.aventyrs.core.character.Resistance;
import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.monstrous.MarcaElemental;
import org.aventyrs.core.effect.monstrous.MurcharAbissal;
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
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.AuraPulse;
import org.aventyrs.core.scene.AuraReach;
import org.aventyrs.core.scene.MonsterAura;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.RecurringDice;
import org.aventyrs.core.sheet.SourcedState;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.AttackSource;
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
                    + "“Anatomia Elemental – Escolha um Elemento, este Monstro recebe RE, Resistência a Críticos, Bônus Racial de +2 em Instinto. Danos causados por este Monstro são Elementais em substituição aos seus tipos.”\n"
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
            return Resistance.RC.instance();
        }

        @Override
        public boolean halvesDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
            return context.isAtLeast(DEVIANTE) && chosenScope(context).map(scope -> scope.matches(type, descriptor)).orElse(false);
        }

        @Override
        public boolean isImmuneToDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
            return context.isAtLeast(APEX) && chosenScope(context).map(scope -> scope.matches(type, descriptor)).orElse(false);
        }

        /**
         * "Danos causados por este Monstro são Elementais em substituição aos seus tipos" — Elemental of the chosen
         * element (the 0.1.6.10 taxonomy). A running Fúria Elemental names its own element, and wins.
         */
        @Override
        public DamageDescriptor resolveAttackDamageRetype(final SkillType attackSkill, final AttackSource attackSource,
                                                          final CombatantSheet holder, final AbilityContext context) {
            if (holder.hasEffectFrom(FURIA_ELEMENTAL)) {
                return null;
            }
            return chosenElement(context).map(element -> new DamageDescriptor(DamageType.ELEMENTAL, element)).orElse(null);
        }
    },

    /**
     * The chosen Arma Natural; from Deviante every attack an Explosão and +3 Dano; from Apex the weapon's Margem +3
     * and -2 on its targets' Resistência. Fúria Elemental runs as a {@link SourcedState}: while it does the holder's
     * attacks are Elementais (Mágicos from Predador, with Marca Elemental), Margem +3 and Cataclismo (core 0.1.7.3).
     */
    ARMAMENTO_ELEMENTAL("Armamento Elemental", PRESA,
            "Efeito Passivo – Escolha uma Arma Natural, este Monstro possui a Arma Natural escolhida.\n"
                    + "Efeito Ativo – Fúria Elemental [1PA, 2PD]: Escolha um Elemento, ataques deste Monstro são Elementais em substituição aos seus tipos, tem a Margem Crítica Menor aumentada em +3 números e recebem Cataclismo como Efeito Crítico adicional. Resfriamento 3.\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Ataques recebem Área de Efeito – Explosão e infligem +3 pontos de Danos.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Fúria Elemental – Ataques infligem Danos Mágicos e recebem a Corrente de Efeitos Monstruosa – Marca Elemental.\n"
                    + "“Marca Elemental – Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas (Efeito de Maldição, não cumulativo)”\n"
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

        /** Deviante: "Ataques recebem Área de Efeito – Explosão". */
        @Override
        public AreaOfEffect resolveAttackArea(final SkillType attackSkill, final AttackSource attackSource,
                                              final CombatantSheet holder, final AbilityContext context) {
            return context.isAtLeast(DEVIANTE) ? AreaOfEffect.ATTACK_EXPLOSION : null;
        }

        /** Deviante: "… e infligem +3 pontos de Danos" — standing, on every attack. */
        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            return context.isAtLeast(DEVIANTE)
                    ? List.of(MonstrousTraits.standing(ModifierType.DAMAGE_ROLL_BONUS, ARMAMENTO_DAMAGE_BONUS,
                    "Armamento Elemental"))
                    : List.of();
        }

        /** Apex: "também reduzem a Resistência a Correntes de Efeitos do alvo em -2". */
        @Override
        public int resolveTargetEffectChainResistanceReduction(final CombatantSheet holder,
                                                               final AbilityContext context) {
            return context.isAtLeast(APEX) ? 2 : 0;
        }

        @Override
        public DamageDescriptor resolveAttackDamageRetype(final SkillType attackSkill, final AttackSource attackSource,
                                                          final CombatantSheet holder, final AbilityContext context) {
            if (!holder.hasEffectFrom(FURIA_ELEMENTAL)) {
                return null;
            }
            // Predador: "Ataques infligem Danos Mágicos" — Mágico of the element; before it, Elemental.
            DamageType type = context.isAtLeast(PREDADOR) ? DamageType.MAGICO : DamageType.ELEMENTAL;
            return chosenElement(context).map(element -> new DamageDescriptor(type, element)).orElse(null);
        }

        @Override
        public List<CriticalEffectType> resolveAdditionalCriticalEffects(final SkillType attackSkill,
                                                                         final AttackSource attackSource,
                                                                         final CombatantSheet holder,
                                                                         final AbilityContext context) {
            return holder.hasEffectFrom(FURIA_ELEMENTAL) ? List.of(CriticalEffectType.CATACLISMO) : List.of();
        }

        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            if (!context.isAtLeast(PREDADOR) || !holder.hasEffectFrom(FURIA_ELEMENTAL)) {
                return List.of();
            }
            return chosenElement(context).<List<EffectChain>>map(element -> List.of(new MarcaElemental(element)))
                    .orElse(List.of());
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
            int duration = apex ? FURIA_DURATION + 2 : FURIA_DURATION;
            return List.of(MonstrousActiveAbility.builder()
                    .name(FURIA_ELEMENTAL)
                    .description("Durante " + duration + " Rodadas os ataques são "
                            + (context.isAtLeast(PREDADOR) ? "Mágicos Elementais e recebem a Marca Elemental" : "Elementais")
                            + ", com Margem Crítica Menor +3 e Cataclismo como Efeito Crítico adicional.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(duration)
                    .cooldownRounds(apex ? 2 : 3)
                    .effect(() -> new SourcedState(FURIA_ELEMENTAL, duration))
                    .effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, duration, FURIA_ELEMENTAL))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Fúria Elemental's \"Escolha um Elemento\" is the element picked with the Habilidade, not one per "
                    + "activation; its Duração (3, Apex 5) is the previous revision's, the new text naming none but "
                    + "the Apex's +2. The Apex Margem +3 and -2 Resistência reach the Arma Natural's Perícia and every "
                    + "Corrente the holder lands, not the weapon alone.";
        }
    },

    /**
     * Retribuição Elemental and the Aura Elemental both land on <i>others</i> — the attacker, every
     * character in the area — so only the Aura's price and Resfriamento are carried.
     */
    SANGUE_ELEMENTAL_DEVIANTE("Sangue Elemental", DEVIANTE,
            "Efeito Passivo: Escolha um Elemento; Retribuição Elemental.\n"
                    + "“Retribuição Elemental - Atacantes corpo-a-corpo que infligirem danos ao Elemental sofrem 3 pontos de Dano Elemental.”\n"
                    + "Efeito Ativo – Aura Elemental [2PA, 2PD]: Invoca a Aura Elemental.\n"
                    + "“Aura Elemental - Área de Efeito Circular Curta. Outros personagens na área sofrem 1d6 pontos de Dano Físico Elemental. Esta Habilidade só pode ser ativada se o Monstro estiver ferido, tem Duração de 2 Rodadas e Resfriamento 2.”\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Retribuição Elemental – Dano aumenta em +1d6 e se torna Mágico Elemental.\n"
                    + "• Explosão Pós-Mortis.\n"
                    + "“Explosão Pós Mortis – Este Monstro explode quando morre e não pode ser revivido. A explosão tem Área de Efeito Circular Média e inflige 2d6+Instinto pontos de Dano Mágico Elemental.”\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Aura Elemental – Alcance muda para Área de Efeito Circular Média e dano muda para 2d6 pontos de Dano Mágico Elemental.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(ELEMENT, MonstrousTraits.elementOptions()));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            return List.of(MonstrousActiveAbility.builder()
                    .name(AURA_ELEMENTAL)
                    .description("Invoca a Aura Elemental (Circular " + (apex ? "Média" : "Curta") + ", 2 Rodadas): "
                            + "outros personagens na área sofrem " + (apex ? "2d6 pontos de Dano Mágico" : "1d6 pontos de Dano")
                            + " Elemental por Rodada. Apenas se o Monstro estiver ferido.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(2)
                    .durationInRounds(2)
                    .cooldownRounds(2)
                    .usableWhen(sheet -> sheet.getDamageTaken() > 0)
                    .effect(() -> new SourcedState(AURA_ELEMENTAL, 2))
                    .build());
        }

        /** The Aura Elemental while it runs: 1d6 Elemental (Apex 2d6 Mágico Elemental, Média) to everyone else inside. */
        @Override
        public List<MonsterAura> resolveAuras(final CombatantSheet holder, final AbilityContext context) {
            if (!holder.hasEffectFrom(AURA_ELEMENTAL) || chosenElement(context).isEmpty()) {
                return List.of();
            }
            boolean apex = context.isAtLeast(APEX);
            ElementalType element = chosenElement(context).get();
            return List.of(MonsterAura.builder()
                    .source(AURA_ELEMENTAL)
                    .radius(apex ? Range.DISTANCIA_MEDIA : Range.DISTANCIA_CURTA)
                    .reach(AuraReach.ALL_OTHERS)
                    .pulse(AuraPulse.builder()
                            .dice(apex ? 2 : 1)
                            .descriptor(new DamageDescriptor(apex ? DamageType.MAGICO : DamageType.ELEMENTAL, element))
                            .build())
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Retribuição Elemental (a Habilidade Monstruosa has no retaliation hook; RetaliationResolver scans "
                    + "Talentos) and Explosão Pós-Mortis (no death trigger). The Aura Elemental pulses at the holder's "
                    + "Turn start, for 2 Rodadas.";
        }
    },

    /**
     * Bônus Racial +2 Foco and two Árvores Elementais up to Muda; Predador a Divina or Profana third and Emergentes;
     * Apex Florescentes of the same trees (the 20261015 text dropped the Apex's extra tree).
     */
    CONJURACAO_ELEMENTAL("Conjuração Elemental", DEVIANTE,
            "Efeito Passivo: Bônus Racial de +2 em Foco.\n"
                    + "Efeito Ativo – Conjuração Elemental: Aprende 2 Árvores de Magia Elemental. É capaz de conjurar Sementes, Brotos e Mudas destas Árvores.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Conjuração Elemental – Aprende uma Árvore de Magia adicional Divina ou Profana.\n"
                    + "• Conjuração Elemental - Se torna capaz de conjurar Magias Emergentes das Árvores conhecidas.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Conjuração Elemental – Se torna capaz de conjurar Magias Florescentes das Árvores conhecidas.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            List<ChoiceSpec> specs = new ArrayList<>();
            specs.add(ChoiceSpec.many(TREES, MonstrousTraits.treeOptions(MonstrousTraits::isElemental), 2));
            if (category.isAtLeast(PREDADOR)) {
                specs.add(ChoiceSpec.one(PREDADOR_TREE, MonstrousTraits.treeOptions(MonstrousTraits::isDivineOrProfane)));
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
            if (context.isAtLeast(PREDADOR)) {
                trees.addAll(context.picks(PREDADOR_TREE));
            }
            return MonstrousTraits.spellsUpTo(trees, deepest);
        }

        @Override
        public ImplementationStatus getImplementationStatus() {
            return ImplementationStatus.APPLIED;
        }
    },

    /** The Arma de Sopro, and Sopro Cataclísmico's +2d6, +3 Margem Crítica Menor and Cataclismo for the Turn. */
    SOPRO_ELEMENTAL("Sopro Elemental", PREDADOR,
            "Efeito Passivo - Adquire a Arma de Sopro.\n"
                    + "Efeito Ativo – Sopro Cataclísmico [+1PA, 2PD]: Apenas neste Turno, dano da Arma de Sopro aumenta em +2d6, Margem Crítica Menor aumenta em +3 e recebe Cataclismo como Efeito Crítico adicional.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Sopro Cataclísmico –Alcance muda para Cone Longo e o dano não é reduzido pela distância percorrida.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Sopro Cataclísmico – Alcance muda para Cone Muito Longo e aplica a Corrente de Efeito Monstruosa – Sopro Apocalíptico.\n"
                    + "“Sopro Apocalíptico – Aplica o Efeito Crítico Menor Cataclismo em cada alvo.”") {
        @Override
        public List<NaturalWeapon> resolveNaturalWeapons(final AbilityContext context) {
            return List.of(NaturalWeapon.ARMA_DE_SOPRO);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(SOPRO_CATACLISMICO)
                    .description("Apenas neste Turno, dano da Arma de Sopro aumenta em +2d6, Margem Crítica Menor "
                            + "aumenta em +3 e recebe Cataclismo como Efeito Crítico adicional.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .effect(bonus(ModifierType.EXTRA_DAMAGE_DICE, 2, 1, SOPRO_CATACLISMICO))
                    .effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, 1, SOPRO_CATACLISMICO))
                    .effect(() -> new SourcedState(SOPRO_CATACLISMICO, 1))
                    .build());
        }

        /** Sopro Cataclísmico's "recebe Cataclismo como Efeito Crítico adicional" — the Sopro's own crits only. */
        @Override
        public List<CriticalEffectType> resolveAdditionalCriticalEffects(final SkillType attackSkill,
                                                                         final AttackSource attackSource,
                                                                         final CombatantSheet holder,
                                                                         final AbilityContext context) {
            return attackSource == NaturalWeapon.ARMA_DE_SOPRO && holder.hasEffectFrom(SOPRO_CATACLISMICO)
                    ? List.of(CriticalEffectType.CATACLISMO)
                    : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "The Cone ranges and \"não é reduzido pela distância\" (geometry), and the Abominação's Sopro "
                    + "Apocalíptico (a Corrente applying an Efeito Crítico needs the critical-effect context and "
                    + "dice). The +2d6 and +3 ride every attack this Turn, not the Sopro alone.";
        }
    },

    /** Summons are not built: only Invocar Elemental's price reaches the sheet. */
    INVOCAR_ELEMENTAL("Invocar Elemental", PREDADOR,
            "Efeito Passivo: Este monstro possui um Subordinado que lhe serve ou auxilia.\n"
                    + "Efeito Ativo – Invocar Elemental [3PA, 4PD]: Invoca dois Monstros com o Modelo Alma Elemental do tipo Presa para ajudar.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Invocar Elemental – o tipo dos Monstros invocados é Deviante.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Possui um Subordinado adicional\n"
                    + "• Invocar Elemental – o tipo dos Monstros invocados é Predador\n"
                    + "• Invocar Elemental – Efeito Alternativo, invoca um único Monstro do tipo Apex.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonsterCategory summoned = context.isAtLeast(ABOMINACAO) ? PREDADOR : context.isAtLeast(APEX) ? DEVIANTE : PRESA;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Invocar Elemental")
                    .description("Invoca dois Monstros com o Modelo Alma Elemental do tipo " + summoned.name()
                            + (context.isAtLeast(ABOMINACAO) ? " (ou, alternativamente, um único do tipo APEX)." : "."))
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
                    + "Efeito Ativo – Tormenta Cataclísmica [4PA, 5PD]: Apenas uma vez por Cena, altera o terreno por 5 Rodadas, criando uma Tormenta Cataclísmica. Enquanto estiver na Área de Efeito da Tormenta Cataclísmica recupera 1d6PV por Rodada e seus ataques recebem Roubo de Vida 2.\n"
                    + "“Tormenta Cataclísmica – Área Circular Muito Longa, o terreno é tomado por energia elemental, infligindo 1d6+Instinto pontos de Dano Elemental a todos os personagens, Equipamentos e objetos em seu interior.\n"
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
                    .description("Apenas uma vez por Cena, por 5 Rodadas: recupera 1d6PV por Rodada e seus ataques "
                            + "recebem Roubo de Vida 2.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(5)
                    .durationInRounds(TORMENTA_DURATION)
                    .cooldownRounds(MonstrousActiveAbility.ONCE_PER_SCENE)
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

    /** Ascenção Abissal or Celestial: immunities, Conjuração +2, Roubo de Vida or 1d6 regeneration. */
    ASCENCAO("Ascenção", APEX,
            "Efeito Passivo – Adquire Ascenção Abissal ou Ascenção Celestial.\n"
                    + "“Ascenção Abissal – Imunidade a Doenças e Maldições, Conjuração +2 e Ataques recebem Roubo de Vida 2 e a Corrente de Efeito Monstruosa – Murchar. Danos Elementais se tornam Profanos em substituição aos seus tipos e ignoram RM.”\n"
                    + "“Murchar – Alvo sofre Redutor de -2 Multiplicador de PV por 2 Rodadas (efeito de Maldição).\n"
                    + "“Ascenção Celestial – Imunidade a Doenças e Encantamentos Nocivos, Conjuração +2, recuperam 1d6PV a cada Rodada e seus ataques recebem a Corrente de Efeitos Monstruosa - Expurgo. Danos Elementais se tornam Sagrados em substituição aos seus tipos e ignoram RM.”\n"
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
                    ? List.of(RecurringDice.healing(Dice.of(1), null, "Ascenção Celestial"))
                    : List.of(new LifeSteal(2, Optional.empty()));
        }

        /** Abominação: "Imunidade a Efeitos Críticos … que não sejam de fontes Abissais, Celestiais ou Primordiais". */
        @Override
        public Set<CriticalEffectType> resolveCriticalEffectImmunities(final AbilityContext context) {
            return context.isAtLeast(ABOMINACAO) ? EnumSet.allOf(CriticalEffectType.class) : Set.of();
        }

        /** Ascenção Abissal: "Ataques recebem … a Corrente de Efeito Monstruosa – Murchar". */
        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return ABISSAL.equals(context.pick(ASPECT)) ? List.of(new MurcharAbissal()) : List.of();
        }

        /** "Danos Elementais se tornam Profanos / Sagrados em substituição aos seus tipos". */
        @Override
        public DamageSanctity resolveElementalDamageSanctity(final AbilityContext context) {
            return ABISSAL.equals(context.pick(ASPECT)) ? DamageSanctity.PROFANO
                    : CELESTIAL.equals(context.pick(ASPECT)) ? DamageSanctity.SAGRADO : null;
        }

        @Override
        public String getUnappliedNote() {
            return "Expurgo (\"Desativa Habilidades Ativas e Encantamentos do alvo\" names nothing this core can switch "
                    + "off), \"ignoram RM\" on the Profano/Sagrado damage (only a Habilidade's own Elemental retype is "
                    + "turned, and nothing bypasses RM for it), immunity to Encantamentos Nocivos, and the "
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

    /**
     * {@link #CONJURACAO_ELEMENTAL}'s former Apex tree — dropped by the 20261015 text, kept only so a saved pick still
     * names a known key. Nothing reads it.
     */
    public static final String APEX_TREE = "apexTree";

    /** {@link #ASCENCAO}'s pick. */
    public static final String ASPECT = "aspect";
    public static final String ABISSAL = "ABISSAL";
    public static final String CELESTIAL = "CELESTIAL";

    /** Armamento Elemental's Deviante "+3 pontos de Danos". */
    private static final int ARMAMENTO_DAMAGE_BONUS = 3;
    private static final int TORMENTA_DURATION = 5;

    /** Fúria Elemental — the Efeito Ativo and the {@link SourcedState} it runs as. */
    private static final String FURIA_ELEMENTAL = "Fúria Elemental";

    /** Fúria Elemental's Duração before the Apex's +2 — the previous revision's 3; the new text names no base. */
    private static final int FURIA_DURATION = 3;

    /** Aura Elemental — the Efeito Ativo, its window and the aura's source. */
    private static final String AURA_ELEMENTAL = "Aura Elemental";

    /** Sopro Cataclísmico — the Efeito Ativo and its one-Turn window. */
    private static final String SOPRO_CATACLISMICO = "Sopro Cataclísmico";

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
