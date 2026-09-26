package org.aventyrs.core.monster.model.aspectohumanoide;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.DestinoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
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
import org.aventyrs.core.sheet.SkillDifficultyShift;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.aventyrs.core.monster.model.MonstrousTraits.bonus;

/**
 * The Habilidades Monstruosas of the Modelo Aspecto Humanoide — {@code criacao-de-monstros.txt}.
 *
 * <p>Built the way {@code CireneiaAbility} is. Most of this Modelo is about <i>becoming someone
 * else</i> — a race, a Título, another creature's Habilidade, a possessed body — and this core can
 * change none of those on a live sheet; each such clause is named in {@link #getUnappliedNote()},
 * and every number around it is applied.
 */
@Getter
public enum AspectoHumanoideAbility implements MonstrousAbility {

    /**
     * +1 Talento; Mimetizar Competência's GD +1 nível on the chosen Perícias. The Perícias are
     * picked when the Habilidade is taken rather than at each activation — an Efeito Ativo here has
     * no per-activation choice.
     */
    CORPO_HUMANOIDE("Corpo Humanoide", PRESA, 
            "Efeito Passivo: Recebe um Talento Geral ou Monstruoso adicional.\n"
                    + "Efeito Ativo - Mimetizar Competência [2PD, 2PA]: Escolha uma Perícia, este monstro imita perfeitamente como outros Humanoides executam certas tarefas. Escolha uma Perícia, a Perícia escolhida tem o GD aumentado em +1 nível. Duração 3 Rodadas, Resfriamento 2 Rodadas.\n"
                    + "Aprimoramento dos Deviante\n"
                    + "• Mimetizar Competência - Escolha uma segunda perícia para receber os Benefícios.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Mimetizar Competência – A Duração aumenta em +1 Rodada, o Tempo de Resfriamento é reduzido para 1 Rodada.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Mimetizar Competência - Escolha uma terceira perícia para receber os Benefícios.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            int count = category.isAtLeast(APEX) ? 3 : category.isAtLeast(DEVIANTE) ? 2 : 1;
            return List.of(ChoiceSpec.many(SKILLS, ChoiceSpec.names(SkillType.values()), count));
        }

        @Override
        public int resolveBonusFeatSlots(final AbilityContext context) {
            return 1;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean predador = context.isAtLeast(PREDADOR);
            int duration = predador ? 4 : 3;
            Set<SkillType> skills = EnumSet.noneOf(SkillType.class);
            skills.addAll(context.picks(SKILLS, SkillType.class));
            return List.of(MonstrousActiveAbility.builder()
                    .name("Mimetizar Competência")
                    .description("As Perícias escolhidas têm o GD aumentado em +1 nível por " + duration + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(2)
                    .durationInRounds(duration)
                    .cooldownRounds(predador ? 1 : 2)
                    .effect(() -> new SkillDifficultyShift(skills, 1, duration, "Mimetizar Competência"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The Perícias are picked when the Habilidade is taken, not at each activation.";
        }
    },

    /**
     * Carisma +2 and Aparência Inofensiva; from Deviante +2 on Carisma Perícias, Predador +3
     * Iniciativa, Apex GD +1 nível on them. Aparência Inofensiva's own effect ("nunca será alvo
     * primário") is a targeting rule this core does not have, so granting the Talento — and
     * restoring it with Mimetizar Inocência — changes nothing a rule reads.
     */
    MASCARA_SOCIAL("Máscara Social", PRESA, 
            "Efeito Passivo: Bônus Racial de Carisma +2. Monstros com essa Habilidade nunca estão sozinho, usando outros personagens ou monstros como escudos, possuem um corpo que aparente ser frágil, escondendo suas verdadeiras capacidades, Mimetizando os efeitos do Talento Aparência Inofensiva, mesmo que não cumpram com o pré-requisitos.\n"
                    + "Efeito Ativo - Mimetizar Inocência [1PA, 5PD]: Restauram os efeitos do Talento Aparência Inofensiva, como forma de enganar personagens que já descobriram a verdade. Este Habilidade é um Efeito de Encantamento e possui Resfriamento 3.\n"
                    + "“Aparência Inofensiva - A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas contra seus inimigos, você nunca será alvo primário de ataques, Magias ou Habilidades inimigas nas duas primeiras Rodadas de um combate.”\n"
                    + "Aprimoramento dos Deviante\n"
                    + "• Bônus de +2 em Perícias baseadas em Carisma.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Iniciativa +3.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Perícias baseadas em Carisma tem o GD aumentado em +1 nível.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return Map.of(AttributeDomain.CHARISMA, 2);
        }

        @Override
        public List<Feat> resolveGrantedFeats(final AbilityContext context) {
            return List.of(DestinoFeat.APARENCIA_INOFENSIVA);
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            if (type == ModifierType.INITIATIVE) {
                return context.isAtLeast(PREDADOR) ? 3 : 0;
            }
            boolean charismaSkill = MonstrousTraits.skillsGovernedBy(AttributeDomain.CHARISMA).stream()
                    .anyMatch(skill -> skill.getRollBonusType() == type);
            return charismaSkill && context.isAtLeast(DEVIANTE) ? 2 : 0;
        }

        @Override
        public int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                          final AbilityContext context) {
            return context.isAtLeast(APEX) && governingAttribute == AttributeDomain.CHARISMA ? 1 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Mimetizar Inocência")
                    .description("Restaura os efeitos do Talento Aparência Inofensiva.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(5)
                    .cooldownRounds(3)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Aparência Inofensiva's \"nunca será alvo primário\" and \"usando outros como escudos\": this core "
                    + "has no targeting step. The Talento is granted; its effect is run at the table.";
        }
    },

    /** From Predador, a Talento Racial of the chosen race. The race itself is not taken on. */
    MIMETIZAR_RACA("Mimetizar Raça", DEVIANTE, 
            "Efeito Passivo: Escolha uma raça, este monstro tem a aparência e as Características Raciais da raça escolhida.\n"
                    + "Efeito Ativo – Transição Racial [3PA, 5PD]: Permite trocar a raça escolhida, alterando a aparência, Caraterísticas Raciais e Talentos Raciais escolhidos.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Recebe um Talento Racial da raça escolhida.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Recebe um Talento Racial ou um Talento Racial Aventyr.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(RACE, racialCategories()));
        }

        @Override
        public int resolveBonusFeatSlots(final AbilityContext context) {
            return context.isAtLeast(PREDADOR) ? 1 : 0;
        }

        @Override
        public Set<FeatCategory> resolveAllowedFeatCategories(final AbilityContext context) {
            if (!context.isAtLeast(PREDADOR)) {
                return Set.of();
            }
            Set<FeatCategory> allowed = EnumSet.noneOf(FeatCategory.class);
            allowed.addAll(context.picks(RACE, FeatCategory.class));
            if (context.isAtLeast(APEX)) {
                allowed.add(FeatCategory.AVENTYR);
            }
            return allowed;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Transição Racial")
                    .description("Troca a raça escolhida, a aparência, as Características e os Talentos Raciais.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(5)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The race's appearance and Características Raciais, and Transição Racial: a Character's race cannot "
                    + "change once built. The racial Talento slot is applied.";
        }
    },

    /** Multiplicador de PD +1. The Título mimicry is not built. */
    MIMETIZAR_CENTELHA("Mimetizar Centelha", DEVIANTE, 
            "Efeito Passivo: O Multiplicador de PD aumenta em +1.\n"
                    + "Ativo – Mimetizar Títulos: Escolha um Título, permite mimetizar os efeitos do Título, 1 Especialização e 2 Habilidades da Especialização escolhida.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Mimetizar Centelha – permite mimetizar ambas as especializações do Título escolhido e 2 Habilidades de cada Especialização.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Mimetizar Centelha – Permite mimetizar uma Habilidade adicional e uma Suprema do Título escolhido.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DETERMINATION_MULTIPLIER ? 1 : 0;
        }

        @Override
        public String getUnappliedNote() {
            return "Mimetizar Títulos: granting a Título's effects without the Título has no mechanism.";
        }
    },

    /** Furtividade and Persuasão GD +1 nível. Mímico's copy of another's Habilidade is not built. */
    MIMICO("Mímico", DEVIANTE, 
            "Efeito Passivo: O GD das Perícias Furtividade e Persuasão aumentam em +1 nível.\n"
                    + "Efeito Ativo – Mímico [2PA, 4PD]: Mimetiza os efeitos de uma Habilidade ativada ou Magia conjurada na última Rodada. Supremas e Magias Florescente não podem ser mimetizadas desta forma.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Mímico – Permite mimetizar efeitos das últimas 3 Rodadas.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Mímico – Permite mimetizar os efeitos de Supremas e Magias Florescentes.") {
        @Override
        public int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                          final AbilityContext context) {
            return skill == SkillType.FURTIVIDADE || skill == SkillType.PERSUASAO ? 1 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Mímico")
                    .description("Mimetiza os efeitos de uma Habilidade ativada ou Magia conjurada recentemente.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(4)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Mímico's copy of another's Habilidade or Magia: nothing records what was activated in a Rodada "
                    + "in a form another combatant could replay. Only its price is applied.";
        }
    },

    /** Possession is not built; Possessão's price and, at Apex, Autocontrole +1 are. */
    POSSESSAO("Possessão", PREDADOR, 
            "Efeito Passivo: Ataques deste Monstro recebem a Corrente de Efeito Monstruosa – Enfraquecer o Espírito.\n"
                    + "“Enfraquecer o Espírito – Alvo perde 1 ponto temporário de Autocontrole (efeito cumulativo). Pontos de Autocontrole perdidos desta forma são recuperados cada após Descansos Longos.”\n"
                    + "Efeito Ativo – Possessão [3PA, 5PD]: Ataque à Distância – Alvo Único Distância Média, o Monstro toma o controle do personagem alvo por 2 Rodadas, apenas personagens com o Autocontrole inferior ao do Monstro pode ser alvo deste Efeito (efeito de Possessão). Alvos possuídos sofrem 1d6 pontos de Dano Mágico Primordial a cada Rodada. Resfriamento 3.\n"
                    + "Efeito Ativo Alternativo – Possessão Contínua [10PD]: Apenas fora de combate e apenas enquanto Possessão estiver ativo, a Duração da possessão deixe ter um limite. Alvos possuídos não mais sofrem danos contínuos por Rodada em decorrência da Possessão, ao invés disso perdem 1 Multiplicador de PV por dia, até que morram ou a possessão for encerrada. Caso a Possessão seja encerrada os Multiplicadores de PV perdidos são recuperados 1 a cada Descanso Longo.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Autocontrole +1.\n"
                    + "• Possessão – Duração dobrada contra alvos com Autocontrole zero.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Possessão – Apenas 1 vez por Cena, ao ser destruído (PV reduzido à zero ou menos), pode efetuar Possessão contra o Alvo com o menor Autocontrole em Distância Curta. Este Efeito ignora o Tempo de Resfriamento.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Possessão")
                    .description("Toma o controle do alvo por 2 Rodadas (efeito de Possessão).")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(5)
                    .durationInRounds(2)
                    .cooldownRounds(3)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Enfraquecer o Espírito and the Possessão itself: no Corrente de Efeitos reaches a target, and no "
                    + "combatant can take control of another. The Apex Autocontrole +1 is not applied either (a "
                    + "Habilidade cannot raise an Ego). Only the price is applied.";
        }
    },

    /** Torpor instead of death, and resurrection, have no mechanism. */
    TANATOSE("Tanatose", APEX, 
            "Efeito Passivo: Ao sofrer dano fatal, ao invés disso entra em um estado de torpor ou – quando possível – abandona o corpo em estado de possessão, evitando a morte derradeira.\n"
                    + "Efeito Ativo – Mimetizar Ressurreição: Encerra o estado de Torpor ou possui um novo alvo vulnerável em seu alcance. Este efeito só pode ser ativado após 2d6 dias. Após ativer este Efeito, o Monstro perde esta Habilidade por 2d6 meses.\n"
                    + "Aprimoramento das Abominação\n"
                    + "• Após ativar Mimetizar Ressurreição o monstro retorna mais forte e tomado por desejo de vingança. Suas perícias têm o GD aumentado em +2 níveis contra aqueles que o derrotaram no passado.") {
        @Override
        public ImplementationStatus getImplementationStatus() {
            return ImplementationStatus.TABLE_ONLY;
        }

        @Override
        public String getUnappliedNote() {
            return "Torpor instead of death, leaving the body and Mimetizar Ressurreição: no death-avoidance, possession "
                    + "or days-long timing exists.";
        }
    },

    /**
     * Forma Verdadeira's numbers, held while the monster keeps its true form (open-ended): +2 in
     * every Atributo, Conjuração and Resistência à Correntes, RD, RM, Resistência a Críticos, +2 on
     * the Margem Crítica Menor; at Abominação its attack and defence GDs one nível up.
     */
    FORMA_VERDADEIRA("Forma Verdadeira", APEX, 
            "Efeito Passivo: A Verdadeira Forma deste monstro não é Humanoide, sendo esta forma apenas uma casca conveniente. Ao alternar de Forma, temporariamente, este Monstro adquire um novo Modelo, então recebe uma Habilidade Presa e Deviante dele.\n"
                    + "Efeito Ativo – Forma Verdadeira: Ao abandonar sua forma Humanoide, este Monstro recebe Bônus Racial de +2 em todos os Atributos, rolagens de Conjuração e Resistência à Correntes de Efeitos, também recebem RD, RM e Resistência à Críticos. Os Ataques do Monstro enquanto em sua Forma Verdadeira tem a Margem Crítica Menor aumenta em +2 números, reduzem a Resistência à Correntes de Efeitos de seus alvos em -2, então recebem a Corrente de Efeitos Monstruosa – Terror da Forma Verdadeira.\n"
                    + "“Terror da Forma Verdadeira – Alvos deste Ataque recebem a Condição Abalado por 2 Rodadas, novas aplicações desta Corrente de Efeitos aumenta a Duração em +1 Rodada e podem progredir a Condição para Amedrontado e Apavorado.”\n"
                    + "Aprimoramento das Abominação\n"
                    + "• Forma Verdadeira - GD das Rolagens de Perícias de Ataque e Defesas aumentadas em +1 nível.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder builder = MonstrousActiveAbility.builder()
                    .name("Forma Verdadeira")
                    .description("Abandona a forma Humanoide: +2 em todos os Atributos e em Conjuração, RD, RM, "
                            + "Resistência a Críticos e Margem Crítica Menor +2.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .durationInRounds(0);
            for (AttributeDomain domain : AttributeDomain.values()) {
                builder.effect(openEnded(domain.getBonusModifierType(), 2));
            }
            builder.effect(openEnded(ModifierType.DOMINIO_DO_MANA_ROLL_BONUS, 2))
                    .effect(openEnded(ModifierType.DAMAGE_REDUCTION, MonstrousTraits.REDUCTION_INSTANCE))
                    .effect(openEnded(ModifierType.MAGIC_REDUCTION, MonstrousTraits.REDUCTION_INSTANCE))
                    .effect(openEnded(ModifierType.CRITICAL_RESISTANCE, MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE))
                    .effect(openEnded(ModifierType.LESSER_CRITICAL_MARGIN, 2));
            if (context.isAtLeast(ABOMINACAO)) {
                Set<SkillType> skills = MonstrousTraits.attackAndDefenseSkills();
                builder.effect(() -> new SkillDifficultyShift(skills, 1, null, FORMA_VERDADEIRA_SOURCE));
            }
            return List.of(builder.build());
        }

        @Override
        public String getUnappliedNote() {
            return "The second Modelo the true form takes (a Modelo is chosen at build, not mid-Cena), Terror da Forma "
                    + "Verdadeira and the -2 on targets' Resistência à Correntes (no Corrente reaches a target), and "
                    + "the +2 Resistência à Correntes (it has no number). No PA or PD price is stated; 1PA is taken. "
                    + "Returning to the humanoid form is the caller's removeEffectsFrom(\"Forma Verdadeira\").";
        }
    };

    /** {@link #CORPO_HUMANOIDE}'s picks: the Perícias Mimetizar Competência raises. */
    public static final String SKILLS = "skills";

    /** {@link #MIMETIZAR_RACA}'s pick: a race, as its Talentos Raciais' {@link FeatCategory}. */
    public static final String RACE = "race";

    /** The source every Forma Verdadeira effect carries — what returning to the humanoid form lifts. */
    public static final String FORMA_VERDADEIRA_SOURCE = "Forma Verdadeira";

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    AspectoHumanoideAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ASPECTO_HUMANOIDE;
    }

    /** APPLIED when every clause is, PARTIAL when {@link #getUnappliedNote()} names one that isn't. */
    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }

    private static Supplier<org.aventyrs.core.sheet.TemporaryEffect> openEnded(final ModifierType type, final int value) {
        return () -> org.aventyrs.core.sheet.TemporaryBonus.openEnded(type, value, FORMA_VERDADEIRA_SOURCE);
    }

    /** Every race whose Talentos Raciais a monster could mimic. */
    private static List<String> racialCategories() {
        List<String> names = new ArrayList<>();
        Arrays.stream(FeatCategory.values())
                .filter(category -> category.getType() == FeatCategory.Type.RACIAL && category != FeatCategory.MONSTRUOSO)
                .forEach(category -> names.add(category.name()));
        return names;
    }
}
