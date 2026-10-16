package org.aventyrs.core.monster.model.brotosdemapinguari;

import org.aventyrs.core.character.Resistance;
import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.monstrous.AutocontroleLoss;
import org.aventyrs.core.effect.monstrous.RouboDeBonusBase;
import org.aventyrs.core.effect.monstrous.VenenoPrimaveril;
import org.aventyrs.core.monster.MonstrousAbilityGrant;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.AuraPulse;
import org.aventyrs.core.scene.AuraReach;
import org.aventyrs.core.scene.MonsterAura;
import org.aventyrs.core.scene.Range;
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
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.SourcedState;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.RecurringDice;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.aventyrs.core.monster.model.MonstrousTraits.bonus;

/**
 * The Habilidades Monstruosas of the Modelo Brotos de Mapinguari — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Anatomia Vegetal</b> — "Bônus Racial de +2 em Vigor, recebem Resistência à Críticos, … imunes
 * aos efeitos críticos Atordoante, Ferida Profunda e Sangramento … vulneráveis à dano Elemental:
 * Fogo, adicionalmente … Gelo ou Natural" — is granted by both Nascido Habilidades. It is a {@link
 * #sharedTraitKey()}, so a Broto holding both gets its Vigor and Resistência a Críticos once.
 * Vulnerabilidade is reported, not sized: no rules text gives it a number.
 */
@Getter
public enum BrotosDeMapinguariAbility implements MonstrousAbility {

    /** Instinto +2, Anatomia Vegetal, Metade do Instinto PV per Rodada; Regeneração Selvagem's extra 1d6 (Predador 2d6). */
    NASCIDO_DAS_LAGRIMAS_DE_FLORA("Nascido das Lágrimas de Flora", PRESA,
            "Efeito Passivo: Bônus Racial de +2 em Instinto, Anatomia Vegetal e recupera Metade do Instinto PV a cada Rodada.\n"
                    + "Efeito Ativo – Regeneração Selvagem [1PA, 2PD]:  A recuperação de PV deste Monstro aumenta em +1d6 por 2 Rodadas. Resfriamento 3.\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Regeneração Selvagem – Resfriamento muda para 2 Rodadas\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Regeneração Selvagem – Recuperação de vida aumenta em +1d6 (para um total de +2d6).\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Regeneração Selvagem – Custo de Ativação aumenta para 3PD, Duração aumenta para 4 Rodadas.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return anatomyWith(AttributeDomain.INSTINCT, context);
        }

        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            int halfInstinct = character.getEffectiveAttributeTotal(AttributeDomain.INSTINCT) / 2;
            return List.of(Regeneration.openEnded(halfInstinct, "Nascido das Lágrimas de Flora"));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            int duration = apex ? 4 : 2;
            int dice = context.isAtLeast(PREDADOR) ? 2 : 1;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Regeneração Selvagem")
                    .description("A recuperação de PV aumenta em +" + dice + "d6 por " + duration + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(apex ? 3 : 2)
                    .durationInRounds(duration)
                    .cooldownRounds(context.isAtLeast(DEVIANTE) ? 2 : 3)
                    .effect(() -> RecurringDice.healing(Dice.of(dice), duration, "Regeneração Selvagem"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Anatomia Vegetal's Vulnerabilidades are reported, not sized (no rules text gives one a number), and "
                    + "its immunity to Roubo de Vida from non-vegetal creatures is not applied (Roubo de Vida is healed "
                    + "by the stealer, never refused by the victim).";
        }
    },

    /** Força +2, Anatomia Vegetal, Roubo de Vida 2; Carnivoria's +1d6 Roubo de Vida (Deviante Margem +3). */
    NASCIDO_DO_SANGUE_DE_LACERTO("Nascido do Sangue de Lacerto", PRESA,
            "Efeito Passivo: Bônus Racial de +2 em Força, Anatomia Vegetal e recebe Roubo de Vida 2.\n"
                    + "Ativo – Carnivoria [1PA 3PD]: Ataques recebem a Corrente de Efeitos Monstruosa – Carnivoria por 1 Rodada. Resfriamento 1.\n"
                    + "“Carnivoria – Este ataque recebe Roubo de Vida +1d6, o Alvo perde 1 ponto temporário de Autocontrole (efeito de Veneno). Personagens que tenham o Autocontrole reduzido à zero desta forma sofrem obrigatoriamente o efeito Torpor. Pontos de Autocontrole perdidos desta forma são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso.”\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Carnivoria – Margem Crítica Menor +3, recebe Oferenda Maldita como efeito Crítico adicional.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Carnivoria – Apenas Descansos Verdadeiros recuperam Pontos de Autocontrole perdidos por Carnivoria.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Carnivoria – Tempo de Resfriamento aumenta para 2 Rodadas, Duração aumenta para 3 Rodadas.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return anatomyWith(AttributeDomain.STRENGTH, context);
        }

        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            return List.of(new LifeSteal(2, Optional.empty()));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            int duration = apex ? 3 : 1;
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder carnivoria = MonstrousActiveAbility.builder()
                    .name("Carnivoria")
                    .description("Por " + duration + " Rodada(s) os ataques recebem Roubo de Vida +1d6.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(3)
                    .durationInRounds(duration)
                    .cooldownRounds(apex ? 2 : 1)
                    .dice(Dice.of(1))
                    .onRolled((sheet, total) -> sheet.applyEffect(new LifeSteal(total, Optional.of(duration))))
                    .effect(() -> new SourcedState(CARNIVORIA, duration));
            if (context.isAtLeast(DEVIANTE)) {
                carnivoria.effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, duration, CARNIVORIA));
            }
            return List.of(carnivoria.build());
        }

        /** The Corrente Carnivoria's Autocontrole half while the active runs; Predador: only a Descanso Verdadeiro. */
        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(CARNIVORIA)
                    ? List.of(AutocontroleLoss.carnivoria(context.isAtLeast(PREDADOR)))
                    : List.of();
        }

        /** Deviante: "recebe Oferenda Maldita como efeito Crítico adicional". */
        @Override
        public List<CriticalEffectType> resolveAdditionalCriticalEffects(final SkillType attackSkill,
                                                                         final AttackSource attackSource,
                                                                         final CombatantSheet holder,
                                                                         final AbilityContext context) {
            return context.isAtLeast(DEVIANTE) && holder.hasEffectFrom(CARNIVORIA)
                    ? List.of(CriticalEffectType.OFERENDA_MALDITA)
                    : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Immunity to Roubo de Vida from non-vegetal creatures: Roubo de Vida is healed by the stealer, never "
                    + "refused by the victim. Carnivoria's +1d6 Roubo de Vida is rolled once at activation, not per "
                    + "attack.";
        }
    },

    /** Categoria de Tamanho and Multiplicador de PV +1. Lançar Esporos lands on others. */
    PRIMAVERIL("Primaveril", DEVIANTE,
            "Efeito Passivo: A Categoria de Tamanho e o Multiplicador de PV aumentam em +1.\n"
                    + "Efeito Ativo – Lançar Esporos: Ataque Mágico – Área Circular Curta, personagens na Área de Efeito são afetadas por Veneno Primaveril. Ataques de monstros com a Habilidade Primaveril efetuados contra personagens afetados pelo Veneno Primaveril recebem Roubo de Mana 1 e Roubo de Determinação 1. Resfriamento 2.\n"
                    + "“Veneno Primaveril – Efeito de veneno. Personagens afetados sofrem redutor de -1 Multiplicador de PV e -1 Multiplicador de PD por 2 Rodadas. Apenas personagens vivos que não sejam Feéricos ou outros Brotos de Mapinguari podem ser infectados pelo Veneno Primaveril.”\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Lançar Esporos - Duração do Veneno Primaveril aumenta para 3 Rodadas.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Lançar Esporos - Área de Efeito aumenta para Distância Média.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.SIZE_CATEGORY || type == ModifierType.LIFE_MULTIPLIER ? 1 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(LANCAR_ESPOROS)
                    .description("Ataque Mágico – Área Circular " + (context.isAtLeast(APEX) ? "Média" : "Curta")
                            + ": os personagens na área são afetados pelo Veneno Primaveril (-1 Multiplicador de PV e "
                            + "de PD por " + sporeRounds(context) + " Rodadas).")
                    .durationInRounds(1)
                    .cooldownRounds(2)
                    .effect(() -> new SourcedState(LANCAR_ESPOROS, 1))
                    .build());
        }

        /** Lançar Esporos' "Área Circular Curta" (Apex Média), for the attack made in its window. */
        @Override
        public AreaOfEffect resolveAttackArea(final SkillType attackSkill, final AttackSource attackSource,
                                              final CombatantSheet holder, final AbilityContext context) {
            return holder != null && holder.hasEffectFrom(LANCAR_ESPOROS)
                    ? AreaOfEffect.circle(context.isAtLeast(APEX) ? Range.DISTANCIA_MEDIA : Range.DISTANCIA_CURTA)
                    : null;
        }

        /**
         * The spore attack carries the Veneno Primaveril; every attack carries the Roubos, which land only on an
         * infected target ("Ataques … contra personagens afetados pelo Veneno Primaveril recebem Roubo de Mana 1 e
         * Roubo de Determinação 1").
         */
        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(LANCAR_ESPOROS)
                    ? List.of(new VenenoPrimaveril(sporeRounds(context)), VenenoPrimaveril.roubos(holder))
                    : List.of(VenenoPrimaveril.roubos(holder));
        }

        @Override
        public String getUnappliedNote() {
            return "No price is stated for Lançar Esporos; 1PA is taken. Its area attack is the one made in the "
                    + "active's 1-Rodada window (the caller names the circle's occupants).";
        }
    },

    /** Categoria de Tamanho +1, +3 Defesas and RDS. Murchar lands on attackers. */
    OUTONAL("Outonal", DEVIANTE,
            "Efeito Passivo: Categoria de Tamanho aumenta em +1, também recebe Bônus Racial de +3 em Defesas e RDS.\n"
                    + "Efeito Ativo – Murchar [3PA, 1PD]: Efeito de Doença. Personagens que ataquem o Outonal sofrem redutor de -2 em Força (mínimo 1) e recebem a Condição Feridas Dolorosas. Pontos de Vigor reduzidos por Murchar são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso. Estes efeitos não são cumulativos, personagens afetados por Murchar se tornam imunes a novas instancias deste efeito até passarem por algum efeito de Descanso. Duração de 1 Rodada e Resfriamento 1.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Murchar – Personagens em Distância Muito Curta são obrigados a desferir seu primeiro ataque da Rodada no Monstro Outonal, este é um efeito de Encantamento e não afeta personagens Feéricos e outros Outonais.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Murchar – Personagens sofrem redutor adicional de -2 em Foco e Instinto.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return switch (type) {
                case SIZE_CATEGORY -> 1;
                case DEFESAS -> 3;
                case DAMAGE_TAKEN_REDUCTION -> Resistance.RDS.instance();
                default -> 0;
            };
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Murchar")
                    .description("Efeito de Doença: personagens que ataquem o Outonal sofrem -2 em Força e recebem a "
                            + "Condição Feridas Dolorosas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(1)
                    .durationInRounds(1)
                    .cooldownRounds(1)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Murchar's -2 Força and Feridas Dolorosas on its attackers, and the Predador's forced first attack: "
                    + "a Habilidade Monstruosa has no retaliation hook (RetaliationResolver scans Talentos). Only its "
                    + "price is applied.";
        }
    },

    /** Cannot be grabbed or knocked down; Raízes Longas' 2d6 per Rodada, RD and physical Meio-Dano. */
    RAIZES_LONGAS("Raízes Longas", PREDADOR,
            "Efeito Passivo: Os pés desta criatura são enormes raízes que se espalham pelo terreno. A Área Circular Muito Curta ao redor deste Monstro é um Terreno Difícil. Este Monstro não pode ser agarrado, empurrado ou derrubado.\n"
                    + "Efeito Ativo – Raízes Longas [1PA, 3PD]: Este Monstro não pode se mover por Concentração +2 Rodadas, enquanto isso recupera 2d6PV por Rodada, recebe RD e reduz danos físicos à metade (efeito de Meio-Dano). Resfriamento 2.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Raízes Longas – Ao longo da Duração as raízes aplicam Veneno nos personagens em sua Área de Efeito, personagens envenenados pelas Raízes longas sofrem Redutor de -1PA. Os efeitos do veneno são encerrados após 2 Rodadas fora da Área de Efeito.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Raízes Longas – Personagens envenenados pelas Raízes Longas sofrem 2d6 pontos de Dano Elemental: Natural por Rodada.") {
        @Override
        public boolean isImmuneToCondition(final ConditionType conditionType, final AbilityContext context) {
            return conditionType == ConditionType.AGARRADO || conditionType == ConditionType.CAIDO;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Raízes Longas")
                    .description("Não pode se mover; recupera 2d6PV por Rodada, recebe RD e reduz danos físicos à metade.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(3)
                    .durationInRounds(ROOTED_DURATION)
                    .cooldownRounds(2)
                    .effect(() -> RecurringDice.healing(Dice.of(2), ROOTED_DURATION, "Raízes Longas"))
                    .effect(bonus(ModifierType.DAMAGE_REDUCTION, Resistance.RD.instance(), ROOTED_DURATION, "Raízes Longas"))
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.HALVES, DamageScope.PHYSICAL,
                            ROOTED_DURATION, "Raízes Longas"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The difficult terrain around it, \"não pode ser empurrado\", \"não pode se mover\" and the "
                    + "Apex/Abominação poison on others: geometry and movement are the caller's. Concentração is taken "
                    + "as 0, so Raízes Longas lasts 2 Rodadas.";
        }
    },

    /** RM and magic Meio-Dano; the Natural Magias up to Muda (Apex Emergente, Abominação Florescente); Conjuração +2/+5. */
    BRUMAS_DE_FLORA("Brumas de Flora", PREDADOR, 
            "Efeito Passivo: Este monstro é cercado por uma fina e constante camada de névoa, que lhe concede RM e reduz Danos Mágicos à metade (efeito de Metade).\n"
                    + "Efeito Ativo – Evocação das Brumas: Pode mimetizar Magias do tipo Muda ou inferior de todas as Árvores Elemental: Natural. Tempo e Custo de conjuração conforme a magia mimetizada.\n"
                    + "Aprimoramento Apex\n"
                    + "• Bônus de +2 em Conjuração\n"
                    + "• Evocação das Brumas – Pode mimetizar magias do tipo Emergente.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Bônus em Conjuração muda para +5\n"
                    + "• Evocação das Brumas – Pode mimetizar magias do tipo Florescente.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            if (type == ModifierType.MAGIC_REDUCTION) {
                return Resistance.RM.instance();
            }
            if (type == ModifierType.DOMINIO_DO_MANA_ROLL_BONUS) {
                return context.isAtLeast(ABOMINACAO) ? 5 : context.isAtLeast(APEX) ? 2 : 0;
            }
            return 0;
        }

        @Override
        public boolean halvesDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
            return DamageScope.MAGICAL.matches(type, descriptor);
        }

        @Override
        public List<Spell> resolveGrantedSpells(final AbilityContext context) {
            BranchLevel deepest = context.isAtLeast(ABOMINACAO) ? BranchLevel.FLORESCENTE
                    : context.isAtLeast(APEX) ? BranchLevel.EMERGENTE : BranchLevel.MUDA;
            return MonstrousTraits.spellsUpTo(MonstrousTraits::isNatural, deepest);
        }

        @Override
        public String getUnappliedNote() {
            return "\"Mimetizar\": the Natural Magias are learned and cast at their own time and cost, which is what "
                    + "the rules ask; they are not marked as mimetized.";
        }
    },

    /** Flagelo de Lacerto's price. The fear aura, the storm and the seeds all land on others. */
    FLAGELO_DE_LACERTO("Flagelo de Lacerto", APEX,
            "Efeito Passivo: De aspecto retorcido, a presença destes monstros perturba e assusta suas presas. Flagelos de Lacerto aplicam aura de medo (condição abalado) em todos os personagens em Distância Média. Personagens em Distância Curta que sofram danos do Flagelo de Lacerto recebem a condição Assustado por 2 Rodadas. Estes são efeitos de Maldição.\n"
                    + "Efeito Ativo – Flagelo de Lacerto [4PA, 4PD]: Personagens na Área que não sejam Brotos de Mapinguari sofrem no início de cada Rodada 2d6 pontos de dano Elemental: Natural, então no fim da Rodada sofrem mais 2d6 pontos de dano Elemental: Gelo. Duração de 3 Rodadas, Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Flagelo de Lacerto – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                    + "• Ao derrotar outros Personagens (PV reduzidos à zero ou menos), Flagelos de Lacerto plantam uma de suas sementes no alvo. A cada Rodada a semente reduz 1 Multiplicador de PV do personagem afetado, personagens mortos desta forma se tornam Árvores Retorcidas. Um Flagelo de Lacerto derrotado pode transferir sua essência vital para uma Árvore Retorcida – Área de Efeito Ao Alcance dos Olhos –, então ressuscitar a partir dela em 3d6 dias.\n"
                    + "“Árvore Retorcida - Efeito de Possessão de corpo morto, corpos transformados em Árvores Retorcidas não podem ser ressuscitadas e emitem uma Aura com Área de Efeito Circular Longa que aplica Medo (condição Abalado) em todos os personagens em Distância Curta (efeito de Maldição).”") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(FLAGELO)
                    .description("Personagens na Área que não sejam Brotos de Mapinguari sofrem 2d6 de dano Elemental: "
                            + "Natural no início e 2d6 de Gelo no fim de cada Rodada, por 3 Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(4)
                    .durationInRounds(3)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 1 : 2)
                    .effect(() -> new SourcedState(FLAGELO, 3))
                    .build());
        }

        /**
         * The fear aura — Abalado on everyone in Distância Média — and, while the Efeito Ativo runs, the storm: 2d6
         * Elemental: Natural to everyone there who is not a Broto de Mapinguari.
         */
        @Override
        public List<MonsterAura> resolveAuras(final CombatantSheet holder, final AbilityContext context) {
            List<MonsterAura> auras = new ArrayList<>();
            auras.add(MonsterAura.builder()
                    .source("Flagelo de Lacerto (medo)")
                    .radius(Range.DISTANCIA_MEDIA)
                    .reach(AuraReach.ALL_OTHERS)
                    .pulse(AuraPulse.builder().condition(ConditionType.ABALADO).conditionRounds(1).build())
                    .build());
            if (holder.hasEffectFrom(FLAGELO)) {
                auras.add(MonsterAura.builder()
                        .source(FLAGELO)
                        .radius(Range.DISTANCIA_MEDIA)
                        .reach(AuraReach.ALL_OTHERS)
                        .exempt(sheet -> MonstrousAbilityGrant.heldBy(sheet.getCharacter())
                                .anyMatch(grant -> grant.getAbility().getModel() == MonsterModel.BROTOS_DE_MAPINGUARI))
                        .pulse(AuraPulse.builder().dice(2)
                                .descriptor(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.NATURAL)).build())
                        .build());
            }
            return auras;
        }

        @Override
        public String getUnappliedNote() {
            return "Assustado for those in Distância Curta it damages, the storm's 2d6 Gelo at the end of each Rodada, "
                    + "the fear's Maldição classification (a curse-immune target still takes Abalado), and the "
                    + "Abominação's seeds, Árvores Retorcidas and resurrection.";
        }
    },

    /** A Descanso Curto of PV every Rodada, immunity to fear and possession; Dádiva de Gaea's cleanse, Descanso Total and Roubo 1. */
    DADIVA_DE_GAEA("Dádiva de Gaea", APEX,
            "Efeito Passivo: Emitem a Aura Dádiva de Gaea.\n"
                    + "“Dádiva de Gaea – Área Circular Média, O Monstros e seus aliados recuperam PV como se passassem por um Descanso Curto, enquanto sob efeito da Aura também são imunes a efeitos de Medo e a Possessões.”\n"
                    + "Efeito Ativo – Dádiva de Gaea Maior [3PA, 4PD]: Encerra todos os efeitos de Maldição, Veneno e Doença sobre este Monstro e seus aliados afetados por Dádiva de Gaea, a recuperação de PV da aura muda para Descanso Total e ignora a Condição Coma. O Monstro e seus aliados afetados recebem Roubo de Bônus Base 2. Duração de 3 Rodadas e Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Dádiva de Gaea Maior– Tempo de Resfriamento reduzido para 1 Rodada.\n"
                    + "• Aliados do Monstro que morram ou entrem em coma enquanto afetados pela Dádiva de Gaea se tornam Totens do Ciclo Natural. Ao morrer, um Dádiva de Gaea pode transferir sua essência vital para um Totem do Ciclo Natural – Área de Efeito Ao Alcance dos Olhos -, então ressuscitar a partir dele em 3d6 dias (efeito de Possessão).\n"
                    + "“Totem do Ciclo Natural – Efeito de Encantamento, corpos transformados em Totens do Ciclo Natural tem seus PV alterados para 1 após 4 Rodadas. Os Totens recuperam PV de qualquer personagem próximo a cada Rodada como se passassem por um Descanso Mínimo. Estes efeitos são aplicados apenas se o Monstro com a Dádiva de Gaea estiver vivo.”") {
        /**
         * The Aura Dádiva de Gaea — Área Circular Média over the monster and its monster allies (table ruling
         * 2026-10-15): a Descanso Curto of PV each Rodada (a Descanso Total while Dádiva de Gaea Maior runs) and
         * immunity to Medo and Possessão while inside.
         */
        @Override
        public List<MonsterAura> resolveAuras(final CombatantSheet holder, final AbilityContext context) {
            return List.of(MonsterAura.builder()
                    .source("Dádiva de Gaea")
                    .radius(Range.DISTANCIA_MEDIA)
                    .reach(AuraReach.SELF_AND_MONSTER_ALLIES)
                    .wardedCondition(ConditionType.ABALADO)
                    .wardedCondition(ConditionType.ASSUSTADO)
                    .wardedCondition(ConditionType.APAVORADO)
                    .wardedCondition(ConditionType.POSSESSAO)
                    .pulse(AuraPulse.builder()
                            .healAsRest(holder.hasEffectFrom(DADIVA_MAIOR) ? RestType.TOTAL : RestType.CURTO)
                            .build())
                    .build());
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(DADIVA_MAIOR)
                    .description("Encerra Maldição, Veneno e Doença; a recuperação da aura muda para Descanso Total "
                            + "e recebe Roubo de Bônus Base 2, por " + DADIVA_DURATION + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(4)
                    .durationInRounds(DADIVA_DURATION)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 1 : 2)
                    .effect(() -> new LifeSteal(DADIVA_STEAL, Optional.of(DADIVA_DURATION)))
                    .effect(() -> new SourcedState(DADIVA_MAIOR, DADIVA_DURATION))
                    .onActivated(sheet -> {
                        sheet.removeCondition(ConditionType.AMALDICOADO);
                        sheet.removeCondition(ConditionType.ENVENENADO);
                        sheet.removeCondition(ConditionType.DOENTE);
                    })
                    .build());
        }

        /** Roubo de Bônus Base 2's PM and PD halves — Roubos on the holder's attacks while Dádiva de Gaea Maior runs. */
        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(DADIVA_MAIOR)
                    ? List.of(new RouboDeBonusBase(holder, DADIVA_STEAL, DADIVA_STEAL))
                    : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Dádiva de Gaea Maior's cleanse reaches only the monster itself (allies in the aura are the caller's); "
                    + "ignoring the Condição Coma, and the Abominação's Totens and resurrection.";
        }
    };

    /** The pick each Nascido Habilidade makes for Anatomia Vegetal's second weakness. */
    public static final String WEAKNESS = "weakness";

    /** {@link #sharedTraitKey()} of the two Nascido Habilidades. */
    public static final String ANATOMIA_VEGETAL = "ANATOMIA_VEGETAL";

    private static final Set<CriticalEffectType> ANATOMY_IMMUNITIES =
            Set.of(CriticalEffectType.ATORDOANTE, CriticalEffectType.FERIDA_PROFUNDA, CriticalEffectType.SANGRAMENTO);

    private static final int ROOTED_DURATION = 2;
    private static final int DADIVA_DURATION = 3;

    /** Flagelo de Lacerto — the Efeito Ativo, its window and the storm aura's source. */
    private static final String FLAGELO = "Flagelo de Lacerto";

    /** Dádiva de Gaea Maior's "Roubo de Bônus Base 2" (20261015; was 1). */
    private static final int DADIVA_STEAL = 2;

    /** Dádiva de Gaea Maior — the Efeito Ativo and the window its Roubo de Bônus Base runs in. */
    private static final String DADIVA_MAIOR = "Dádiva de Gaea Maior";

    /** Carnivoria — the Efeito Ativo and the window its Corrente rides in. */
    private static final String CARNIVORIA = "Carnivoria";

    /** Lançar Esporos — the Efeito Ativo and the window its spore attack is made in. */
    private static final String LANCAR_ESPOROS = "Lançar Esporos";

    /** Veneno Primaveril's Duração: 2 Rodadas, Predador 3. */
    private static int sporeRounds(final AbilityContext context) {
        return context.isAtLeast(PREDADOR) ? 3 : 2;
    }

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    BrotosDeMapinguariAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.BROTOS_DE_MAPINGUARI;
    }

    /** APPLIED when every clause is, PARTIAL when {@link #getUnappliedNote()} names one that isn't. */
    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }

    // ---- Anatomia Vegetal — the two Nascido Habilidades ----------------------------------------

    private boolean grantsAnatomy() {
        return this == NASCIDO_DAS_LAGRIMAS_DE_FLORA || this == NASCIDO_DO_SANGUE_DE_LACERTO;
    }

    @Override
    public String sharedTraitKey() {
        return grantsAnatomy() ? ANATOMIA_VEGETAL : null;
    }

    @Override
    public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
        return grantsAnatomy() ? List.of(ChoiceSpec.one(WEAKNESS, List.of(ElementalType.GELO.name(), ElementalType.NATURAL.name())))
                : List.of();
    }

    @Override
    public int resolveCriticalResistance(final AbilityContext context) {
        return grantsAnatomy() && context.ownsSharedTraits() ? Resistance.RC.instance() : 0;
    }

    @Override
    public Set<CriticalEffectType> resolveCriticalEffectImmunities(final AbilityContext context) {
        return grantsAnatomy() ? ANATOMY_IMMUNITIES : Set.of();
    }

    @Override
    public boolean isVulnerableToDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
        if (!grantsAnatomy()) {
            return false;
        }
        return DamageScope.element(ElementalType.FOGO).matches(type, descriptor)
                || context.pick(WEAKNESS, ElementalType.class)
                        .map(weakness -> DamageScope.element(weakness).matches(type, descriptor))
                        .orElse(false);
    }

    /** own +2, and Anatomia Vegetal's Vigor +2 when this Habilidade is the one granting it. */
    private static Map<AttributeDomain, Integer> anatomyWith(final AttributeDomain own, final AbilityContext context) {
        return context.ownsSharedTraits() ? Map.of(own, 2, AttributeDomain.VIGOR, 2) : Map.of(own, 2);
    }
}
