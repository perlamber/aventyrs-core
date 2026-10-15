package org.aventyrs.core.monster.model.escolhidodemacaria;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Resistance;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.monstrous.MarcaDaEscuridao;
import org.aventyrs.core.effect.monstrous.MonstrousCondition;
import org.aventyrs.core.effect.monstrous.SangriaProfana;
import org.aventyrs.core.effect.monstrous.Sanguenomancia;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ActivationTrigger;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonsterSummoning;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.scene.AuraPulse;
import org.aventyrs.core.scene.AuraReach;
import org.aventyrs.core.scene.MonsterAura;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.SourcedState;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.sheet.SkillDifficultyShift;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

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
 * The Habilidades Monstruosas of the Modelo Escolhido de Macária — {@code criacao-de-monstros.txt}, manual v19
 * 20261013 (core 0.1.6.26).
 *
 * <p><b>Anatomia de Morto-Vivo</b> is granted by every one of them ({@link #grantsUndeadAnatomy()}, table ruling
 * 2026-10-14) and lives on the sheet ({@code monster.UndeadAnatomy}), so holding several grants it once.
 *
 * <p>Mestre das Marionetes carries no tier tag in the text; it is read as Apex (table ruling, 2026-10-14) — it has only
 * an Abominação Aprimoramento, as Cuspidor de Pragas [Apex] does.
 *
 * <p>The monster-side summons are reported: Procissão dos Desencarnados as {@link MonstrousActiveAbility#getSummonOptions()},
 * Cuspidor de Pragas's spawning through {@code UndeadInvocationService#startPlagueSpitting}; both invoke generated
 * Carrascos Moribundos ({@code monster.summon.CarrascoMorimbundo}, table ruling 2026-10-14).
 */
@Getter
public enum EscolhidoDeMacariaAbility implements MonstrousAbility {

    /**
     * Renascido Monstruoso (20261015): Força and Instinto +2, RDS and Roubo de Vida 2 for 3 Rodadas after any Macária
     * Efeito Ativo or Título Habilidade, renewing; Sanguenomancia Inata's melee Mágico attack takes Vigor (-1/-2/-3)
     * from the target and gives the monster +1 (Apex +2).
     */
    RENASCIDO("Renascido Monstruoso", PRESA,
            "Efeito Passivo: Anatomia de Morto-vivo. Você é um morto-vivo senciente, consciente de sua condição e sua Centelha ainda brilha. Após ativar qualquer Habilidade Monstruosa do Modelo Escolhido de Macária ou Habilidade de Título Aventyr recebe Bônus Racial de +2 em Força e Instinto, RDS e Roubo de Vida 2 por 3 Rodadas (Efeitos não cumulativos, novas instâncias renovam a Duração do Efeito).\n"
                    + "Efeito Ativo – Sanguenomancia Inata [3PA, 3PD]: Este é um Ataque (Mágico) desferido Corpo-a-Corpo utilizado para roubar a vitalidade dos vivos e se alimentar. Este ataque não inflige danos, mas o alvo sofre Redutor Temporário de -1 em Vigor e o Monstro recebe Bônus Temporário de +1 em Vigor (Efeito de Doença, não cumulativo), pontos de Vigor perdidos desta forma são recuperados cada após 1 Descanso. Resfriamento 3.\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Desperta um Título Aventyr e uma Especialização\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Sanguenomancia Inata – O Redutor em Vigor aumenta para -2 (mínimo Vigor 1).\n"
                    + "• Desperta 2 Habilidades de Título.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Sanguenomancia Inata – Redutores em Vigor aumenta para -3 (mínimo Vigor 1), Bônus em Vigor aumentam para +2.\n"
                    + "• Desperta uma 1 Habilidade de Título e 1 Suprema.") {
        @Override
        public List<TemporaryEffect> resolveActivationTriggeredEffects(final ActivationTrigger trigger,
                                                                       final AbilityContext context) {
            if (!trigger.titleAbility() && trigger.model() != MonsterModel.ESCOLHIDO_DE_MACARIA) {
                return List.of();
            }
            return List.of(
                    new TemporaryBonus(ModifierType.STRENGTH_BONUS, 2, RENASCIDO_ROUNDS, RENASCIDO_SOURCE),
                    new TemporaryBonus(ModifierType.INSTINCT_BONUS, 2, RENASCIDO_ROUNDS, RENASCIDO_SOURCE),
                    new TemporaryBonus(ModifierType.DAMAGE_TAKEN_REDUCTION, Resistance.RDS.instance(),
                            RENASCIDO_ROUNDS, RENASCIDO_SOURCE),
                    new LifeSteal(2, Optional.of(RENASCIDO_ROUNDS), RENASCIDO_SOURCE));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            int gain = context.isAtLeast(APEX) ? 2 : 1;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Sanguenomancia Inata")
                    .description("Ataque (Mágico) Corpo-a-Corpo sem dano: o alvo sofre -" + sanguenomanciaLoss(context)
                            + " em Vigor (Doença, até 1 Descanso) e o Monstro recebe +" + gain + " em Vigor.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .durationInRounds(SANGUENOMANCIA_ROUNDS)
                    .cooldownRounds(3)
                    .effect(bonus(ModifierType.VIGOR_BONUS, gain, SANGUENOMANCIA_ROUNDS, "Sanguenomancia Inata"))
                    .effect(() -> new SourcedState(SANGUENOMANCIA_WINDOW, 1))
                    .build());
        }

        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(SANGUENOMANCIA_WINDOW)
                    ? List.of(new Sanguenomancia(sanguenomanciaLoss(context)))
                    : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Sanguenomancia Inata's \"não inflige danos\" is the caller's (its hit carries the Corrente; the "
                    + "caller deals no damage); the monster's own +Vigor is read as lasting 3 Rodadas, the text naming "
                    + "none. The Deviante/Predador/Apex Despertares (Título, Especialização, Habilidades, Suprema) are "
                    + "the Narrador's to give the sheet.";
        }
    },

    /** Vigor +2, Roubo de Vida 1/3/4; Instinto Predatório Mortis' +2 Iniciativa and PA (Apex Margem +3). */
    CARRASCO_MORIMBUNDO("Carrasco Morimbundo", PRESA,
            "Efeito Passivo: Esqueletos, Zumbis, Carniçais e outras formas de mortos sem vontade própria são tipos de Carrascos Morimbundos, são resistentes e vorazes, recebem Bônus Racial de +2 em Vigor, Autocontrole 0 (não sofrem efeitos negativos por autocontrole zero) e recebem Roubo de Vida 1.\n"
                    + "Efeito Ativo – Instinto Predatório Mortis [1PA e 2PD]: Bônus de +2 em Iniciativa e Pontos de Ação por 2 Rodadas. Esta Habilidade é utilizada apenas para atacar e devorar inimigos vivos, enquanto ativa o Carrasco Morimbundo ignora alvos que possuam Anatomia de Morto-Vivo e Anatomia de Objeto. Refrigeração 2.\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Roubo de Vida aumenta para 3.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Instinto Predatório – Duração aumenta em +1 Rodada para cada personagem vivo em Distância Longa no ato da ativação.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Roubo de Vida aumenta para 4 enquanto houver ao menos um personagem ferido em Distância Curta.\n"
                    + "• Instinto Predatório – Enquanto ativo a Margem Crítica Menor aumenta em +3 e recebe Oferenda Maldita como Efeito Crítico adicional.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return Map.of(AttributeDomain.VIGOR, 2);
        }

        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            return List.of(new LifeSteal(carrascoLifeSteal(context), Optional.empty()));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder instinct = MonstrousActiveAbility.builder()
                    .name("Instinto Predatório Mortis")
                    .description("Bônus de +2 em Iniciativa e Pontos de Ação por " + INSTINTO_ROUNDS
                            + " Rodadas, apenas para atacar e devorar inimigos vivos.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(INSTINTO_ROUNDS)
                    .cooldownRounds(2)
                    .effect(bonus(ModifierType.INITIATIVE, 2, INSTINTO_ROUNDS, "Instinto Predatório Mortis"))
                    .effect(bonus(ModifierType.ACTION_POINTS, 2, INSTINTO_ROUNDS, "Instinto Predatório Mortis"));
            if (context.isAtLeast(APEX)) {
                instinct.effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 3, INSTINTO_ROUNDS,
                        "Instinto Predatório Mortis"));
            }
            return List.of(instinct.build());
        }

        @Override
        public String getUnappliedNote() {
            return "Autocontrole 0 is the Narrador's Ego entry (its \"sem efeitos negativos\" means no Ego setback is "
                    + "applied for it). Instinto Predatório's targeting (only the living), its Predador +1 Rodada per "
                    + "living character in Distância Longa and its Apex Oferenda Maldita, and the Apex Roubo de Vida 4 "
                    + "\"enquanto houver ao menos um personagem ferido em Distância Curta\" (held at 3 — proximity is the "
                    + "caller's) are not applied.";
        }
    },

    /** Procissão dos Desencarnados invokes Carrascos Moribundos (reported); Predador Carisma +2. */
    SENHOR_DOS_MORTOS("Senhor dos Mortos", DEVIANTE,
            "Efeito Passivo: Pode se comunicar, comandar e criar mortos-vivos que não esteja sobre o comando de outro Senhor dos Mortos ou de um Necromante (Título Aventyr). Este é um efeito de Aura e afeta quaisquer mortos-vivos em Área de Efeito Circular Curta. Apenas Mortos-Vivos que sejam de categoria inferior ao do Senhor dos Mortos e que não sejam Renascidos são afetados por este efeito (Efeito de Encantamento). A quantidade de mortos-vivos controlados ou invocados pelo Senhor dos Mortos não pode superar seu Valor de Carisma.\n"
                    + "Efeito Ativo – Procissão dos Desencarnados [3PA, 4PD]: Invoca um Carrasco Moribundo Presa para auxiliar. Duração igual à Concentração + 5 Rodadas, Refrigeração 1.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Carisma +2\n"
                    + "• Procissão dos Desencarnados - invoca Carrascos Moribundos Deviantes.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Aura do Senhor dos Mortos permite controlar quaisquer Mortos-Vivos de categoria inferior.\n"
                    + "• Procissão dos Desencarnados – invoca 2 Carrascos Moribundos Deviantes ou 1 Carrasco Moribundo Predador.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return context.isAtLeast(PREDADOR) ? Map.of(AttributeDomain.CHARISMA, 2) : Map.of();
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            List<MonsterSummoning> options = new ArrayList<>();
            if (context.isAtLeast(APEX)) {
                options.add(procession(DEVIANTE, 2));
                options.add(procession(PREDADOR, 1));
            } else {
                options.add(procession(context.isAtLeast(PREDADOR) ? DEVIANTE : PRESA, 1));
            }
            return List.of(MonstrousActiveAbility.builder()
                    .name("Procissão dos Desencarnados")
                    .description("Invoca Carrascos Moribundos para auxiliar por Concentração + "
                            + PROCESSION_TRAILING_ROUNDS + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(4)
                    .cooldownRounds(1)
                    .summonOptions(options)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Aura that commands other undead (an Encantamento over every lesser undead in Área Circular "
                    + "Curta — area and control are the caller's). Procissão's Carrascos are reported "
                    + "(MonstrousActiveAbility#getSummonOptions) and placed by UndeadInvocationService#invokeCarrascos; "
                    + "its cap is its Carisma (\"não pode superar seu Valor de Carisma\").";
        }
    },

    /** Incorpóreo, Margem Crítica +1, Predador flight; Garras Espectrais' price (Apex 5PD, 4 Rodadas). */
    ESPECTRAL("Espectral", DEVIANTE,
            "Efeito Passivo: Incorpóreo, seus ataques são mágicos e tem a Margem Crítica Menor aumentada em +1 número.\n"
                    + "“Incorpóreo – Este monstro não possui um corpo, estando preso a plano material por uma forte força de vontade ou magias profanas. A ausência de um corpo torna imune a danos, exceto quando causados por Armas de Prata, fontes Primordiais, Elementais e Mágicas. Pode ignorar barreiras e terrenos difíceis em seus movimentos.”\n"
                    + "Efeito Ativo – Garras Espectrais [2PA, 4PD]: Dano aumentado em +1d6, recebem Sangramento como Efeito Crítico adicional e recebe a Corrente de Efeitos Monstruosa – Sangria Profana. Este efeito tem Duração de 3 Rodadas e Resfriamento 2.\n"
                    + "“Sangria Profana – Alvo perde 2PV e 1PD a cada Turno por Vigor Rodadas, este é um efeito de Sangramento. Espectrais recebem Roubo de Determinação 2 contra alvos afetados pela Sangria Profana.”\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Recebe Movimento Base de Voo e pode trespassar espaços ocupados por outros personagens\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Garras Espectrais – Custo de Ativação aumentado para 5PD, Duração aumentada para 5 Rodadas.") {
        @Override
        public boolean isImmuneToDamage(final DamageType type, final DamageDescriptor descriptor,
                                        final AbilityContext context) {
            // "imune a danos, exceto quando causados por … fontes Primordiais, Elementais e Mágicas".
            DamageType nature = descriptor != null ? descriptor.damageType() : type;
            return nature == DamageType.FISICO;
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skill, final AbilityContext context) {
            return skill != null && skill.isAttackSkill() ? 1 : 0;
        }

        @Override
        public boolean keepsFlying(final AbilityContext context) {
            return context.isAtLeast(PREDADOR);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            int duration = apex ? 5 : 3;
            return List.of(MonstrousActiveAbility.builder()
                    .name(GARRAS_ESPECTRAIS)
                    .description("Dano +1d6, Sangramento como Efeito Crítico adicional e a Corrente Sangria Profana por "
                            + duration + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(apex ? 5 : 4)
                    .durationInRounds(duration)
                    .cooldownRounds(2)
                    .effect(bonus(ModifierType.EXTRA_DAMAGE_DICE, 1, duration, GARRAS_ESPECTRAIS))
                    .effect(() -> new SourcedState(GARRAS_ESPECTRAIS, duration))
                    .build());
        }

        /** Sangria Profana while Garras Espectrais runs; the Roubo de Determinação 2 against the bleeding, always. */
        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(GARRAS_ESPECTRAIS)
                    ? List.of(new SangriaProfana(), SangriaProfana.roubo(holder))
                    : List.of(SangriaProfana.roubo(holder));
        }

        @Override
        public List<CriticalEffectType> resolveAdditionalCriticalEffects(final SkillType attackSkill,
                                                                         final AttackSource attackSource,
                                                                         final CombatantSheet holder,
                                                                         final AbilityContext context) {
            return holder.hasEffectFrom(GARRAS_ESPECTRAIS) ? List.of(CriticalEffectType.SANGRAMENTO) : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Armas de Prata piercing Incorpóreo (the immunity hook cannot see the weapon), \"seus ataques são "
                    + "mágicos\" (no element or nature to retype them to), ignoring barriers and terrain, and "
                    + "trespassing others' spaces. Garras Espectrais' +1d6 rides every attack while it runs.";
        }
    },

    /** Proteger a Filactéria's +1 nível; Apex RA and Metade do Vigor PV per Rodada; Abominação Meio-Dano. */
    FILACTERIA("Filactéria", PREDADOR, 
            "“Filactéria - A alma deste Monstro está selada em um objeto e destruir seu corpo ou essência apenas o coloca em Torpor por 1d6 Rodadas. Este morto-vivo é destruído definitivamente apenas quando a Filactéria é destruída. Os PV deste monstro são reduzidos à metade, a outra metade se torna a Dureza da Filactéria.”\n"
                    + "Efeito Passivo: Caso a Filactéria sofra danos o Monstro entra em um estado de fúria incontrolável, recebendo Bônus Variável de +6 em Força e Instinto (não cumulativo), este Bônus decai em 1 unidade a cada Rodada e priorizando atacar aqueles que infligiram danos ou que interagiram com a Filactéria.\n"
                    + "Efeito Ativo – Proteger a Filactéria [4PA, 5PD]: Esta Habilidade só pode ser ativada enquanto houver inimigos em Distância Curta da Filactéria e apenas se a Filactéria estiver danificada. O GD das Perícias de Ataque, Atenção e Domínio do Mana aumentam em +1 nível. Este efeito dura por toda a Cena\n"
                    + "Aprimoramento dos Apex\n"
                    + "• O Monstro e a Filactéria recebem RA e recuperam Metade do Vigor PV a cada Rodada\n"
                    + "Aprimoramento das Abominações\n"
                    + "• O Monstro e a Filactéria reduzem danos à metade, exceto danos causados por fontes Sagradas, Primordiais e Elemental: Fogo.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.ABSOLUTE_DAMAGE_REDUCTION && context.isAtLeast(APEX)
                    ? Resistance.RA.instance() : 0;
        }

        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            if (!context.isAtLeast(APEX)) {
                return List.of();
            }
            int halfVigor = character.getEffectiveAttributeTotal(AttributeDomain.VIGOR) / 2;
            return List.of(Regeneration.openEnded(halfVigor, "Filactéria"));
        }

        @Override
        public boolean halvesDamage(final DamageType type, final DamageDescriptor descriptor,
                                    final AbilityContext context) {
            if (!context.isAtLeast(ABOMINACAO)) {
                return false;
            }
            // "exceto danos causados por fontes Sagradas, Primordiais e Elemental: Fogo".
            DamageType nature = descriptor != null ? descriptor.damageType() : type;
            boolean sacred = descriptor != null && descriptor.sanctity() == DamageSanctity.SAGRADO;
            boolean fire = DamageScope.element(ElementalType.FOGO).matches(type, descriptor);
            return !(sacred || fire || nature != null && nature.isPrimordial());
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            Set<SkillType> skills = Set.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ATAQUE_A_DISTANCIA,
                    SkillType.ATTENTION, SkillType.DOMINIO_DO_MANA);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Proteger a Filactéria")
                    .description("O GD das Perícias de Ataque, Atenção e Domínio do Mana aumentam em +1 nível por "
                            + "toda a Cena. Apenas com a Filactéria danificada e inimigos em Distância Curta dela.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(5)
                    .cooldownRounds(MonstrousActiveAbility.ONCE_PER_SCENE)
                    .effect(() -> new SkillDifficultyShift(skills, 1, null, "Proteger a Filactéria"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Filactéria itself (an object holding half its PV as Dureza, Torpor for 1d6 Rodadas instead of "
                    + "dying, destroyed only with it), the fury when it is damaged (+6 Força and Instinto decaying 1 a "
                    + "Rodada), Proteger a Filactéria's two conditions (the caller's), and the Apex RA and regeneration "
                    + "reaching the Filactéria too.";
        }
    },

    /** RA, RD and RM; Marca da Escuridão's +1 nível on attacks. */
    ANUNCIADOR_DA_ESCURIDAO("Anunciador da Escuridão", PREDADOR,
            "Efeito Passivo: Recebe RA, RD e RM, propaga a aura Presença do Ceifeiro.\n"
                    + "“Presença do Ceifeiro – Inimigos vivos em Distância Curta sofrem 1d6 pontos de Dano Mágico Profano, o Monstro recupera uma quantidade de PV igual ao dano causado (efeito de Roubo de Vida).”\n"
                    + "Efeito Ativo – Marca da Escuridão [3PA, 4PD]: Ataque Corpo-a-Corpo (Mágico), possui GD aumentado em + 1 nível e recebe a Corrente de Efeitos Monstruosa – Marca da Escuridão. Refrigeração 2.\n"
                    + "“Marca da Escuridão - Alvo é Amaldiçoado e sofre 3d6 pontos de Dano Mágico Profano no início da próxima Rodada, se este ataque reduzir o PV do alvo à zero ou menos ele morrerá imediatamente, o Monstro recebe Roubo de Vida adicional igual ao Vigor do alvo por 2 Rodadas. Efeito não cumulativo.”\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Marca da Escuridão - Alvos mortos pela Marca da Escuridão se levantam como Renascidos Deviantes ao controle do Monstro após 2 Rodadas. Efeito de Possessão com Duração de 2 Rodadas.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Marca da Escuridão – Permite efetuar 2 ataques Corpo-a-Corpo.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return switch (type) {
                case ABSOLUTE_DAMAGE_REDUCTION -> Resistance.RA.instance();
                case DAMAGE_REDUCTION -> Resistance.RD.instance();
                case MAGIC_REDUCTION -> Resistance.RM.instance();
                default -> 0;
            };
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(MARCA_DA_ESCURIDAO)
                    .description("Ataque Corpo-a-Corpo (Mágico) com GD +1 nível que recebe a Corrente Marca da Escuridão"
                            + (context.isAtLeast(ABOMINACAO) ? "; permite efetuar 2 ataques." : "."))
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(4)
                    .durationInRounds(MARCA_ROUNDS)
                    .cooldownRounds(2)
                    .effect(() -> new SkillDifficultyShift(Set.of(SkillType.ATAQUE_CORPO_A_CORPO), 1, MARCA_ROUNDS,
                            MARCA_DA_ESCURIDAO))
                    .effect(() -> new SourcedState(MARCA_DA_ESCURIDAO, MARCA_ROUNDS))
                    .build());
        }

        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(MARCA_DA_ESCURIDAO) ? List.of(new MarcaDaEscuridao(holder)) : List.of();
        }

        /** Presença do Ceifeiro: 1d6 Mágico Profano to the living enemies in Distância Curta, healing the monster. */
        @Override
        public List<MonsterAura> resolveAuras(final CombatantSheet holder, final AbilityContext context) {
            return List.of(MonsterAura.builder()
                    .source("Presença do Ceifeiro")
                    .radius(Range.DISTANCIA_CURTA)
                    .reach(AuraReach.LIVING_ENEMIES)
                    .pulse(AuraPulse.builder()
                            .dice(1)
                            .descriptor(new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO))
                            .lifeSteal(true)
                            .build())
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Marca da Escuridão's \"se este ataque "
                    + "reduzir o PV do alvo à zero ou menos ele morrerá imediatamente\", the Apex raising of the dead as "
                    + "Renascidos Deviantes, and the Abominação's 2 attacks (reported) are the caller's.";
        }
    },

    /** Cuspidor de Pragas: 2 Deviante Carrascos every odd Rodada in combat (UndeadInvocationService); Praga Zumbi's price. */
    CUSPIDOR_DE_PRAGAS("Cuspidor de Pragas", APEX,
            "Efeito Passivo: Enquanto em combate, a Cada Rodada Ímpar dois Carrascos Moribundos do tipo Deviante são invocados para auxiliar o Monstro. Quando a Cena de Combate termina ou o Cuspidor de Pragas é destruído os mortos invocados são destruídos.\n"
                    + "Efeito Ativo – Praga Zumbi [3PA, 3PD]: Ataque à Distância (Mágico), Alvo Único Distante – Distância Muito Curta. Alvos afetados recebem a Doença Praga Zumbi.\n"
                    + "“Praga Zumbi – Alvo recebe o Estado Doente e sofre Redutor de 1 ponto de Vigor a cada Rodada, esta Doença pode ser Propagar. Personagens que tenham o PV reduzido à zero ou menos morrem imediatamente, 2 Rodadas depois levantam-se como Carrascos Moribundos do tipo Deviante sob o controle do Cuspidor de Pragas. Duração 3 Rodadas e Resfriamento 2.”\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Praga Zumbi – Alvos recebem a Condição Silêncio.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(PRAGA_ZUMBI)
                    .description("Ataque à Distância (Mágico), Alvo Único em Distância Muito Curta: o alvo recebe a "
                            + "Praga Zumbi por 3 Rodadas" + (context.isAtLeast(ABOMINACAO) ? " e a Condição Silêncio." : "."))
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .durationInRounds(1)
                    .cooldownRounds(2)
                    .effect(() -> new SourcedState(PRAGA_ZUMBI, 1))
                    .build());
        }

        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(PRAGA_ZUMBI)
                    ? List.of(MonstrousCondition.pragaZumbi(holder, context.isAtLeast(ABOMINACAO)))
                    : List.of();
        }

        @Override
        public String getUnappliedNote() {
            return "Its spawning is placed by UndeadInvocationService#startPlagueSpitting (the caller starts it when "
                    + "combat begins). Praga Zumbi's Doença and Silêncio land through the attack made in its window; its "
                    + "-1 Vigor a Rodada and the dead rising as Deviante Carrascos after 2 Rodadas are the caller's.";
        }
    },

    /** Títere's own immunity to damage for 1 Rodada. */
    MESTRE_DAS_MARIONETES("Mestre das Marionetes", APEX,
            "Efeito Passivo- Emite a Aura de Mestre das Marionetes.\n"
                    + "“Mestre das Marionetes – Área de Efeito - Circular Curta. Personagem vivos em sua Área de Efeito perdem 1 ponto Temporário de Autocontrole a cada 2 Rodadas. Personagens com Autocontrole zero no interior da Aura são Possuídos por 2 Rodadas. Após a possessão entram em Torpor. Pontos de Autocontrole perdidos por Mestre das Marionetes são recuperados individualmente a cada Efeito de Descanso.”\n"
                    + "Efeito Ativo – Títere [4PA, 4PD]: O Monstro se torna imune a danos por 1 Rodada, então desfere um Ataque à Distância (Mágico), Alvo Único Distante – Distância Muito Curta, personagens afetados são Possuídos por 2 Rodada. Resfriamento 3.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Mestre das Marionetes – Área de Efeito aumenta para Distância Média.\n"
                    + "• Títere – Alcance aumenta para Distância Curta. Resfriamento muda para 2 Rodadas.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name(TITERE)
                    .description("O Monstro se torna imune a danos por 1 Rodada, então desfere um Ataque à Distância "
                            + "(Mágico) em Distância " + (context.isAtLeast(ABOMINACAO) ? "Curta" : "Muito Curta")
                            + ": o alvo é Possuído por 2 Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(4)
                    .durationInRounds(1)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 2 : 3)
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.IMMUNE, DamageScope.ALL, 1, TITERE))
                    .effect(() -> new SourcedState(TITERE, 1))
                    .build());
        }

        @Override
        public List<EffectChain> resolveAttackEffectChains(final SkillType attackSkill, final AttackSource attackSource,
                                                           final CombatantSheet holder, final AbilityContext context) {
            return holder.hasEffectFrom(TITERE) ? List.of(MonstrousCondition.titere(holder)) : List.of();
        }

        /**
         * The Aura de Mestre das Marionetes — Circular Curta (Abominação Média): the living inside lose 1 temporary
         * Autocontrole every 2 Rodadas, recovered by a Descanso, and one at zero is Possuído for 2 Rodadas.
         */
        @Override
        public List<MonsterAura> resolveAuras(final CombatantSheet holder, final AbilityContext context) {
            return List.of(MonsterAura.builder()
                    .source("Mestre das Marionetes")
                    .radius(context.isAtLeast(ABOMINACAO) ? Range.DISTANCIA_MEDIA : Range.DISTANCIA_CURTA)
                    .reach(AuraReach.LIVING_OTHERS)
                    .pulse(AuraPulse.builder()
                            .autocontroleLoss(1)
                            .everyNRounds(2)
                            .possessAtZeroAutocontrole(true)
                            .conditionRounds(2)
                            .build())
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Control of the possessed and the Torpor after the possession are the caller's; a lost Autocontrole "
                    + "point comes back at any Descanso (all at once, not one per Descanso). Títere's Possessão lands "
                    + "through the attack made in its window.";
        }
    };

    /** How long Sanguenomancia Inata's own +Vigor lasts — ⚠️ the text names no Duração; the previous revision's 3. */
    public static final int SANGUENOMANCIA_ROUNDS = 3;

    /** Renascido Monstruoso's "por 3 Rodadas", and the source its buff renews by. */
    public static final int RENASCIDO_ROUNDS = 3;
    public static final String RENASCIDO_SOURCE = "Renascido Monstruoso";

    private static final String SANGUENOMANCIA_WINDOW = "Sanguenomancia Inata (ataque)";
    private static final String GARRAS_ESPECTRAIS = "Garras Espectrais";
    private static final String MARCA_DA_ESCURIDAO = "Marca da Escuridão";
    private static final String PRAGA_ZUMBI = "Praga Zumbi";
    private static final String TITERE = "Títere";

    /** Sanguenomancia Inata's "Redutor Temporário de -1 em Vigor" — Predador -2, Apex -3. */
    static int sanguenomanciaLoss(final AbilityContext context) {
        return context.isAtLeast(APEX) ? 3 : context.isAtLeast(PREDADOR) ? 2 : 1;
    }

    /** Instinto Predatório Mortis' "por 2 Rodadas". */
    public static final int INSTINTO_ROUNDS = 2;

    /** Procissão dos Desencarnados' "Concentração + 5 Rodadas". */
    public static final int PROCESSION_TRAILING_ROUNDS = 5;

    /** Cuspidor de Pragas: "dois Carrascos Moribundos do tipo Deviante" each odd Rodada. */
    public static final int PLAGUE_SPAWN_COUNT = 2;

    /** ⚠️ Marca da Escuridão states no Duração for its +1 nível. */
    public static final int MARCA_ROUNDS = 1;

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    EscolhidoDeMacariaAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ESCOLHIDO_DE_MACARIA;
    }

    /** Every Escolhido de Macária is a Morto-Vivo with the Anatomia (table ruling, 2026-10-14). */
    @Override
    public boolean grantsUndeadAnatomy() {
        return true;
    }

    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }

    /** Carrasco Morimbundo's Roubo de Vida: 1, 3 from Deviante (⚠️ the Apex 4 is proximity-gated; held at 3). */
    static int carrascoLifeSteal(final AbilityContext context) {
        return context.isAtLeast(DEVIANTE) ? 3 : 1;
    }

    private static MonsterSummoning procession(final MonsterCategory category, final int count) {
        return new MonsterSummoning(category, count, PROCESSION_TRAILING_ROUNDS, true);
    }
}
