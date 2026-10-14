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
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonsterSummoning;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.sheet.SkillDifficultyShift;
import org.aventyrs.core.sheet.TemporaryEffect;
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

    /** Força and Instinto +2, RDS; Sanguenomancia Inata's Roubo de Vida 2 and +1 Vigor for 3 Rodadas. */
    RENASCIDO("Renascido", PRESA, 
            "Efeito Passivo: Anatomia de Morto-vivo. Você é um morto-vivo senciente, consciente de sua condição e sua Centelha ainda brilha. Recebe Bônus Racial de +2 em Força e Instinto e RDS.\n"
                    + "Efeito Ativo – Sanguenomancia Inata [3PA, 3PD]: Este é um Ataque Mágico desferido Corpo-a-Corpo utilizado para roubar a vitalidade dos vivos e se alimentar. Este ataque não inflige danos, mas o alvo sofre Redutor Temporário de -1 em Vigor e o Monstro recebe Bônus Temporário de +1 em Vigor (Efeito de Doença, não cumulativo), então recebe Roubo de Vida 2 por 3 Rodadas. Pontos de Vigor perdidos desta forma são recuperados após 1 Descanso Verdadeiro. Resfriamento 3.\n"
                    + "Aprimoramento dos Deviantes\n"
                    + "• Desperta um Título Aventyr e uma Especialização\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Desperta 3 Habilidades de Título\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Desperta uma Especialização, 1 Habilidade de Título e 1 Suprema") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.INSTINCT, 2);
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DAMAGE_TAKEN_REDUCTION ? Resistance.RDS.instance() : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Sanguenomancia Inata")
                    .description("Rouba a vitalidade do alvo: Bônus Temporário de +1 em Vigor e Roubo de Vida 2 por "
                            + SANGUENOMANCIA_ROUNDS + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .durationInRounds(SANGUENOMANCIA_ROUNDS)
                    .cooldownRounds(3)
                    .effect(bonus(ModifierType.VIGOR_BONUS, 1, SANGUENOMANCIA_ROUNDS, "Sanguenomancia Inata"))
                    .effect(() -> new LifeSteal(2, Optional.of(SANGUENOMANCIA_ROUNDS)))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Sanguenomancia Inata's Ataque Mágico and the target's -1 Vigor (a Doença on someone else, recovered "
                    + "by a Descanso Verdadeiro) are the caller's; its own +1 Vigor is read as lasting the 3 Rodadas of "
                    + "its Roubo de Vida. The Deviante/Predador/Apex Despertares (Título, Especialização, Habilidades, "
                    + "Suprema) are the Narrador's to give the sheet.";
        }
    },

    /** Vigor +2, Roubo de Vida 1/3/4; Instinto Predatório Mortis' +2 Iniciativa and PA (Apex Margem +3). */
    CARRASCO_MORIMBUNDO("Carrasco Morimbundo", PRESA, 
            "Efeito Passivo: Esqueletos, Zumbis, Carniçais e outras formas de mortos sem vontade própria são tipos de Carrascos Morimbundos, são resistentes e vorazes, recebem Bônus Racial de +2 em Vigor, Autocontrole 0 (não sofrem efeitos negativos por autocontrole zero) e recebem Roubo de Vida 1.\n"
                    + "Efeito Ativo – Instinto Predatório Mortis [1PA e 2PD]: Bônus de +2 em Iniciativa e Pontos de Ação por 2 Rodadas. Esta Habilidade é utilizada apenas para atacar e devorar inimigos vivos, enquanto ativa o Carrasco Morimbundo ignora alvos que possuam Anatomia de Morto-Vivo e Anatomia de Objeto.\n"
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
                    + "Efeito Ativo – Procissão dos Desencarnados [3PA, 4PD]: Invoca um Carrasco Moribundo Presa para auxiliar por Concentração + 5 Rodadas. Refrigeração 1.\n"
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
                    + "Efeito Ativo – Garras Espectrais [2PA, 4PD]: Dano aumentado em +1d6, recebem Sangramento como Efeito Crítico adicional e recebe a Corrente de Efeitos Monstruosa – Sangria Profana. Este efeito tem Duração de 2 Rodadas e Resfriamento 2.\n"
                    + "“Sangria Profana – Alvo perde 2PV e 1PD a cada Turno por Vigor Rodadas, este é um efeito de Sangramento. Espectrais recebem Roubo de Determinação 2 contra alvos afetados pela Sangria Profana.”\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Recebe Movimento Base de Voo e pode trespassar espaços ocupados por outros personagens\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Garras Espectrais – Custo de Ativação aumentado para 5PD, Duração aumentada para 4 Rodadas.") {
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
            return List.of(MonstrousActiveAbility.builder()
                    .name("Garras Espectrais")
                    .description("Dano +1d6, Sangramento como Efeito Crítico adicional e a Corrente Sangria Profana por "
                            + (apex ? 4 : 2) + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(apex ? 5 : 4)
                    .durationInRounds(apex ? 4 : 2)
                    .cooldownRounds(2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Armas de Prata piercing Incorpóreo (no silver weapons), \"seus ataques são mágicos\" (no attack "
                    + "retyping for a foe), ignoring barriers and terrain, trespassing others' spaces, and Garras "
                    + "Espectrais' +1d6, Sangramento and Sangria Profana (an Efeito Ativo lands on its holder). Only "
                    + "Garras' price, Duração and Resfriamento are carried.";
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
                    + "Efeito Ativo – Marca da Escuridão [2PA, 4PD]: Ataques tem o GD aumentado em + 1 nível e recebem a Corrente de Efeitos Monstruosa – Marca da Escuridão.\n"
                    + "“Marca da Escuridão - Alvo é Amaldiçoado e sofre 3d6 pontos de Dano Mágico Profano no início da próxima Rodada, se este ataque reduzir o PV do alvo à zero ou menos ele morrerá imediatamente, o Monstro recebe Roubo de Vida adicional igual ao Vigor do alvo por 2 Rodadas. Efeito não cumulativo.”\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Marca da Escuridão - Alvos mortos pela Marca da Escuridão se levantam como Renascidos ao controle do Monstro após 2 Rodadas. Efeito de Possessão com Duração de 2 Rodadas.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Marca da Escuridão – Duração indeterminada. Efeitos que removem Maldições ou Possessão removem a marca da Escuridão.") {
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
                    .name("Marca da Escuridão")
                    .description("Ataques têm o GD aumentado em +1 nível e recebem a Corrente Marca da Escuridão.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(4)
                    .durationInRounds(MARCA_ROUNDS)
                    .effect(() -> new SkillDifficultyShift(MonstrousTraits.attackAndDefenseSkills().stream()
                            .filter(SkillType::isAttackSkill).collect(java.util.stream.Collectors.toSet()),
                            1, MARCA_ROUNDS, "Marca da Escuridão"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Presença do Ceifeiro aura (1d6 Profano to the living in Distância Curta, healing the monster — "
                    + "area damage is the caller's), Marca da Escuridão's Corrente (Amaldiçoado, 3d6 Profano next Rodada, "
                    + "death and Roubo de Vida; Apex rising as Renascidos; Abominação indefinite). No Duração is stated "
                    + "for the +1 nível; 1 Rodada is read.";
        }
    },

    /** Cuspidor de Pragas: 2 Deviante Carrascos every odd Rodada in combat (UndeadInvocationService); Praga Zumbi's price. */
    CUSPIDOR_DE_PRAGAS("Cuspidor de Pragas", APEX, 
            "Efeito Passivo: Enquanto em combate, a Cada Rodada Ímpar dois Carrascos Moribundos do tipo Deviante são invocados para auxiliar o Monstro. Quando a Cena de Combate termina ou o Cuspidor de Pragas é destruído os mortos invocados são destruídos.\n"
                    + "Efeito Ativo – Praga Zumbi [3PA, 3PD]: Um Alvo Único Distante – Distância Muito Curta é afetado pela Praga Zumbi.\n"
                    + "“Praga Zumbi – Alvo recebe o Estado Doente e sofre Redutor de 1 ponto de Vigor a cada Rodada, esta Doença pode ser Propagar. Personagens que tenham o PV reduzido à zero ou menos morrem imediatamente, 2 Rodadas depois levantam-se como Carrascos Moribundos do tipo Deviante sob o controle do Cuspidor de Pragas. Duração 3 Rodadas e Resfriamento 2.”\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Praga Zumbi – Alvos recebem a Condição Silêncio.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Praga Zumbi")
                    .description("Um Alvo Único em Distância Muito Curta recebe a Praga Zumbi por 3 Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .durationInRounds(3)
                    .cooldownRounds(2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Its spawning is placed by UndeadInvocationService#startPlagueSpitting (the caller starts it when "
                    + "combat begins). Praga Zumbi lands on its target (Doente, -1 Vigor a Rodada, propagation; the dead "
                    + "rising as Deviante Carrascos after 2 Rodadas; Abominação Silêncio) — the caller's.";
        }
    },

    /** Títere's own immunity to damage for 1 Rodada. */
    MESTRE_DAS_MARIONETES("Mestre das Marionetes", APEX, 
            "Efeito Passivo- Emite a Aura de Mestre das Marionetes.\n"
                    + "“Mestre das Marionetes – Emite uma Aura com Área de Efeito - Circular Curta. Personagem vivos em seu interior perdem 1 ponto Temporário de Autocontrole a cada 2 Rodadas. Personagens que tenham Autocontrole zero no interior da Aura são Possuídos.\n"
                    + "Efeito Ativo – Títere [4PA, 4PD]: O Monstro se torna imune a danos e um Alvo Único Distante – Distância Muito Curta é Possuído por 1 Rodada. Resfriamento 3.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Mestre das Marionetes – Área de Efeito aumenta para Distância Média.\n"
                    + "• Títere – Alcance aumenta para Distância Curta.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Títere")
                    .description("O Monstro se torna imune a danos e um Alvo Único em Distância "
                            + (context.isAtLeast(ABOMINACAO) ? "Curta" : "Muito Curta") + " é Possuído por 1 Rodada.")
                    .actionPointCost(ActionCost.ofActionPoints(4))
                    .determinationPointCost(4)
                    .durationInRounds(1)
                    .cooldownRounds(3)
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.IMMUNE, DamageScope.ALL, 1, "Títere"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Aura (living characters losing 1 Autocontrole every 2 Rodadas, possessed at zero — area and "
                    + "Ego loss on others) and Títere's possession of its target. Títere's own immunity is read as "
                    + "lasting the possession's 1 Rodada.";
        }
    };

    /** Sanguenomancia Inata's "Roubo de Vida 2 por 3 Rodadas". */
    public static final int SANGUENOMANCIA_ROUNDS = 3;

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
