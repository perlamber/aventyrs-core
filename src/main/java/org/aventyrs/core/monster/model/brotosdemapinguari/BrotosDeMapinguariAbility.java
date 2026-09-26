package org.aventyrs.core.monster.model.brotosdemapinguari;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.effect.CriticalEffectType;
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
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.RecurringDice;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.sheet.TemporaryEffect;

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
                    + "Efeito Ativo – Regeneração Selvagem [1PA, 2PD]:  A recuperação de PV deste Monstro muda para 1d6+Metade do Instinto por 2 Rodadas. Resfriamento 3.\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Regeneração Selvagem – Resfriamento muda para 2 Rodadas\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Regeneração Selvagem – Recuperação de vida adicional aumenta para +2d6.\n"
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
                    .description("A recuperação de PV muda para " + dice + "d6+Metade do Instinto por " + duration + " Rodadas.")
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
                    + "“Carnivoria – Este ataque recebe Roubo de Vida +1d6, o Alvo é perde 1 ponto temporário de Autocontrole (efeito de Veneno). Personagens que tenham o Autocontrole reduzido à zero desta forma sofrem obrigatoriamente o efeito Torpor. Pontos de Autocontrole perdidos desta forma são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso.”\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Carnivoria – Margem Crítica Manor +3, recebe Oferenda Maldita como efeito Crítico adicional.\n"
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
                    .onRolled((sheet, total) -> sheet.applyEffect(new LifeSteal(total, Optional.of(duration))));
            if (context.isAtLeast(DEVIANTE)) {
                carnivoria.effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, duration, "Carnivoria"));
            }
            return List.of(carnivoria.build());
        }

        @Override
        public String getUnappliedNote() {
            return "The target's lost Autocontrole, Torpor and the Deviante's extra Oferenda Maldita: no Corrente reaches "
                    + "a target, and an Efeito Ativo cannot add a Crítico to a later attack. Immunity to Roubo de Vida "
                    + "from non-vegetal creatures: Roubo de Vida is healed by the stealer, never refused by the victim.";
        }
    },

    /** Categoria de Tamanho and Multiplicador de PV +1. Lançar Esporos lands on others. */
    PRIMAVERIL("Primaveril", DEVIANTE, 
            "Efeito Passivo: A Categoria de Tamanho e o Multiplicador de PV aumentam em +1.\n"
                    + "Efeito Ativo – Lançar Esporos: Personagens em Distância Curta sofrem redutor de -1 Multiplicador de PV e -1 Multiplicador de PM (efeito de Veneno) por 2 Rodadas. Ataques de monstros com a Habilidade Primaveril efetuados contra personagens afetados personagens afetados pelo Veneno Primaveril recebem Roubo de Mana 1 e Roubo de Determinação 1. Esta Habilidade afeta apenas personagens vivos que não sejam Feéricos ou outros Brotos de Mapinguari. Resfriamento 2.\n"
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
                    .name("Lançar Esporos")
                    .description("Personagens em Distância " + (context.isAtLeast(APEX) ? "Média" : "Curta")
                            + " sofrem -1 Multiplicador de PV e de PM (efeito de Veneno).")
                    .durationInRounds(context.isAtLeast(PREDADOR) ? 3 : 2)
                    .cooldownRounds(2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Lançar Esporos' poison on everyone in range (geometry), and Roubo de Mana and de Determinação (no "
                    + "such steal exists). No price is stated; 1PA is taken.";
        }
    },

    /** Categoria de Tamanho +1, +3 Defesas and RDS. Murchar lands on attackers. */
    OUTONAL("Outonal", DEVIANTE, 
            "Efeito Passivo: Categoria de Tamanho aumenta em +1, também recebe Bônus Racial de +3 em Defesas e RDS.\n"
                    + "Efeito Ativo – Murchar [3PA, 1PD]: Personagens que ataquem o Outonal sofrem redutor de -2 em Força (mínimo 1) e tem efeitos de cura reduzidos à zero (efeitos de Doença). Pontos de Vigor reduzidos por Murchar são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso. Estes efeitos não são cumulativos, personagens afetados por Murchar se tornam imunes a novas instancias deste efeito até passarem por algum efeito de Descanso. Duração de 1 Rodada e Resfriamento 1.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Murchar – Personagens em Distância Muito Curta são obrigados a desferir seu primeiro ataque no Monstro Outonal, este é um efeito de Encantamento e não afeta personagens Feéricos e outros Outonais.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Murchar – Personagens sofrem redutor adicional de -2 em Foco e Instinto.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return switch (type) {
                case SIZE_CATEGORY -> 1;
                case DEFESAS -> 3;
                case DAMAGE_REDUCTION -> MonstrousTraits.RDS_INSTANCE;
                default -> 0;
            };
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Murchar")
                    .description("Personagens que ataquem o Outonal sofrem -2 em Força e têm efeitos de cura reduzidos a zero.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(1)
                    .durationInRounds(1)
                    .cooldownRounds(1)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Murchar's effects on attackers and the Predador's forced first attack: they land on others, which "
                    + "an Efeito Ativo cannot reach. Only its price is applied.";
        }
    },

    /** Cannot be grabbed or knocked down; Raízes Longas' 2d6 per Rodada, RD and physical Meio-Dano. */
    RAIZES_LONGAS("Raízes Longas", PREDADOR, 
            "Efeito Passivo: Os pés desta criatura são enormes raízes que se espalham pelo terreno. A Área Circular Muito Curta ao redor deste Monstro é um Terreno Difícil. Este Monstro não pode ser agarrado, empurrado ou derrubado.\n"
                    + "Efeito Ativo – Raízes Longas [1PA, 3PD]: Este Monstro não pode se mover por Concentração +2 Rodadas, enquanto isso recupera 2d6PV por Rodada, recebe RD e reduz danos físicos à metade (efeito de Meio-Dano). Resfriamento 2.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Raízes Longas – Ao longo da Duração as raízes aplicam Veneno nos personagens em sua Área de Efeito, personagens envenenados pelas Raízes longas sofrem Redutor de -1PA. Os efeitos do veneno são encerrados após 2 Rodadas fora da Área de Efeito.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Raízes Longas – Personagens envenenados pelas Raízes Longas sofrem 1d6 pontos de Dano Elemental: Natural por Rodada.") {
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
                    .effect(bonus(ModifierType.DAMAGE_REDUCTION, MonstrousTraits.REDUCTION_INSTANCE, ROOTED_DURATION, "Raízes Longas"))
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
                return MonstrousTraits.REDUCTION_INSTANCE;
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
                    + "Efeito Ativo – Flagelo de Lacerto [4PA, 5PD]: Personagens na Área que não sejam Brotos de Mapinguari sofrem no início de cada Rodada 2d6 pontos de dano Elemental: Natural, então no fim da Rodada sofrem mais 2d6 pontos de dano Elemental: Gelo. Duração de 3 Rodadas, Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Flagelo de Lacerto – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                    + "• Ao derrotar outros Personagens (PV reduzidos à zero ou menos), Flagelos de Lacerto plantam uma de suas sementes no alvo. A cada Rodada a semente reduz 1 Multiplicador de PV do personagem afetado, personagens mortos desta forma se tornam Árvores Retorcidas. Um Flagelo de Lacerto derrotado pode transferir sua essência vital para uma Árvore Retorcida – Área de Efeito Ao Alcance dos Olhos –, então ressuscitar a partir dela em 3d6 dias.\n"
                    + "“Árvore Retorcida - Efeito de Possessão de corpo morto, corpos transformados em Árvores Retorcidas não podem ser ressuscitadas e emitem uma Aura com Área de Efeito Circular Longa que aplica Medo (condição Abalado) em todos os personagens em Distância Curta (efeito de Maldição).”") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Flagelo de Lacerto")
                    .description("Personagens na Área sofrem 2d6 de dano Elemental: Natural no início e 2d6 de Gelo no fim de cada Rodada.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(5)
                    .durationInRounds(3)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 1 : 2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The fear aura, the storm's damage to others, and the Abominação's seeds and resurrection: geometry, "
                    + "area damage and resurrection. Only the price is applied.";
        }
    },

    /** A Descanso Curto of PV every Rodada, immunity to fear and possession; Dádiva de Gaea's cleanse, Descanso Total and Roubo 1. */
    DADIVA_DE_GAEA("Dádiva de Gaea", APEX, 
            "Efeito Passivo: Monstros Dádiva de Gaea emitem uma aura (Área de Efeito Circular Média) que cura a si próprio e os seus aliados, permitindo que recuperem PV a cada Rodada como se passassem por um Descanso Curto, enquanto sob efeito da Aura também são imunes a efeitos de Medo e a Possessões.\n"
                    + "Efeito Ativo – Dádiva de Gaea [3PA, 4PD]: Encerra todos os efeitos de Maldição, Possessão e Doença sobre este Monstro e todos os seus aliados, a recuperação de PV da aura muda para Descanso Total e ignora o estado de Coma. O Monstro e seus aliados afetados recebem Roubo de Bônus Base 1. Duração de 3 Rodadas e Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Dádiva de Gaea – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                    + "• Aliados do Monstro que morram ou entrem em coma enquanto afetados pela Dádiva de Gaea se tornam Totens do Ciclo Natural. Ao morrer, um Dádiva de Gaea pode transferir sua essência vital para um Totem do Ciclo Natural – Área de Efeito Ao Alcance dos Olhos -, então ressuscitar a partir dele em 3d6 dias (efeito de Possessão).\n"
                    + "“Totem do Ciclo Natural – Efeito de Encantamento, corpos transformados em Totens do Ciclo Natural tem seus PV alterados para 1 após 4 Rodadas. Os Totens recuperam PV de qualquer personagem próximo a cada Rodada como se passassem por um Descanso Mínimo. Estes efeitos são aplicados apenas se o Monstro com a Dádiva de Gaea estiver vivo.”") {
        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            int shortRest = new RestServiceImpl().getRecoveredHitPoints(character, RestType.CURTO);
            return List.of(Regeneration.openEnded(shortRest, "Dádiva de Gaea"));
        }

        @Override
        public boolean isImmuneToCondition(final ConditionType conditionType, final AbilityContext context) {
            return conditionType == ConditionType.ABALADO || conditionType == ConditionType.ASSUSTADO
                    || conditionType == ConditionType.APAVORADO || conditionType == ConditionType.POSSESSAO;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Dádiva de Gaea")
                    .description("Encerra Maldição, Possessão e Doença; a recuperação da aura muda para Descanso Total "
                            + "e recebe Roubo de Bônus Base 1, por " + DADIVA_DURATION + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(4)
                    .durationInRounds(DADIVA_DURATION)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 1 : 2)
                    .effect(() -> new LifeSteal(1, Optional.of(DADIVA_DURATION)))
                    .onActivated(sheet -> {
                        sheet.removeCondition(ConditionType.AMALDICOADO);
                        sheet.removeCondition(ConditionType.POSSESSAO);
                        sheet.removeCondition(ConditionType.DOENTE);
                        RestServiceImpl rest = new RestServiceImpl();
                        int extra = rest.getRecoveredHitPoints(sheet.getCharacter(), RestType.TOTAL)
                                - rest.getRecoveredHitPoints(sheet.getCharacter(), RestType.CURTO);
                        sheet.applyEffect(new Regeneration(Math.max(0, extra), DADIVA_DURATION, null,
                                "Dádiva de Gaea (Descanso Total)", 1));
                    })
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Everything the aura does for allies, ignoring Coma, Roubo de Bônus Base's PM and PD half (only PV "
                    + "can be stolen), and the Abominação's Totens and resurrection.";
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
        return grantsAnatomy() && context.ownsSharedTraits() ? MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE : 0;
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
