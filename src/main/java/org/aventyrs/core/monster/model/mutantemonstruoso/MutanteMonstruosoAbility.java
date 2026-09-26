package org.aventyrs.core.monster.model.mutantemonstruoso;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.magic.ElementalType;
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
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.RecurringDice;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.aventyrs.core.monster.model.MonstrousTraits.bonus;

/**
 * The Habilidades Monstruosas of the Modelo Mutante Monstruoso — {@code criacao-de-monstros.txt}.
 *
 * <p>Built the way {@code CireneiaAbility} is. RDS is carried as plain RD of 1 per instance ("Cada
 * instância reduz … em -1") — this core has no separate RDS stat, and the rules' only difference
 * (RDS also reaches Mágico damage) is lost to RD's own type-blindness here.
 */
@Getter
public enum MutanteMonstruosoAbility implements MonstrousAbility {

    /** RDS and Resistência a Críticos; Fisiologia Estranha's Meio-Dano (and Deviante +3 Defesas) while active. */
    FISIOLOGIA_ESTRANHA("Fisiologia Estranha", PRESA, 
            "Efeito Passivo: Recebe RDS e Resistência à Críticos, adicionalmente a Resistência à Correntes de Efeitos aumenta em +2.\n"
                    + "Efeito Ativo – Fisiologia Estranha [1PA, 3PD]: Danos sofridos reduzidos à Metade (efeito de Meio-Dano) por 2 Rodadas. Resfriamento 1.\n"
                    + "Aprimoramento dos Deviante\n"
                    + "• Fisiologia Estranha – Defesas +3.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Fisiologia Estranha – Duração aumentada em +1 Rodada.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Fisiologia Estranha – Imunidade à Críticos Menores e Correntes de Efeitos.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DAMAGE_REDUCTION ? MonstrousTraits.RDS_INSTANCE : 0;
        }

        @Override
        public int resolveCriticalResistance(final AbilityContext context) {
            return MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE;
        }

        /** Apex: "Imunidade à Críticos Menores" — applied as standing, see the note. */
        @Override
        public boolean ignoresMinorCriticalEffects(final AbilityContext context) {
            return context.isAtLeast(APEX);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            int duration = context.isAtLeast(PREDADOR) ? 3 : 2;
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder active = MonstrousActiveAbility.builder()
                    .name("Fisiologia Estranha")
                    .description("Danos sofridos reduzidos à Metade (Meio-Dano) por " + duration + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(3)
                    .durationInRounds(duration)
                    .cooldownRounds(1)
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.HALVES, DamageScope.ALL, duration,
                            "Fisiologia Estranha"));
            if (context.isAtLeast(DEVIANTE)) {
                active.effect(bonus(ModifierType.DEFESAS, 3, duration, "Fisiologia Estranha"));
            }
            return List.of(active.build());
        }

        @Override
        public String getUnappliedNote() {
            return "The +2 Resistência à Correntes de Efeitos (it has no number). The Apex immunity to Críticos Menores "
                    + "and Correntes is an Aprimoramento of the Efeito Ativo; it is applied as standing, since an "
                    + "immunity cannot be granted for a Duração, and the Correntes half has nothing to refuse.";
        }
    },

    /** Surto de Ação's +1PA per set of extra limbs. The limbs themselves are not modelled. */
    MEMBROS_MULTIPLOS("Membros Múltiplos", PRESA, 
            "Efeito Passivo - Escolha um membro do corpo, como braços, cabeças etc. Este Monstro recebe um novo conjunto do membro escolhido.\n"
                    + "Efeito Ativo – Surto de Ação [Ação Livre, 3PD]: Para cada conjunto de membros adicionais o Monstro recebe Bônus de +1PA neste Turno.\n"
                    + "Aprimoramento dos Deviante\n"
                    + "• Adquire um novo conjunto de Membros\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Adquire um novo conjunto de Membros\n"
                    + "• Surto de Ação – Ações com os membros extras tem o Tempo de Ação reduzidos em -1PA. Este efeito desencadeia uma vez para cada conjunto de membro, novas ações com aquele conjunto de Membro tem o Tempo de Ação padrão.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Dobra o número de Membros Extras\n"
                    + "• Efeito Ativo – Regenerar Membro [3PA, 1PD]: Regenera um membro Perdido.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(LIMB, LIMBS));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            int sets = extraLimbSets(context);
            MonstrousActiveAbility surto = MonstrousActiveAbility.builder()
                    .name("Surto de Ação")
                    .description("Recebe Bônus de +" + sets + "PA neste Turno — +1PA por conjunto de membros adicionais.")
                    .actionPointCost(ActionCost.FREE_ACTION)
                    .determinationPointCost(3)
                    .durationInRounds(1)
                    .effect(bonus(ModifierType.ACTION_POINTS, sets, 1, "Surto de Ação"))
                    .build();
            if (!context.isAtLeast(APEX)) {
                return List.of(surto);
            }
            return List.of(surto, MonstrousActiveAbility.builder()
                    .name("Regenerar Membro")
                    .description("Regenera um membro perdido.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(1)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "The limbs themselves, the Predador's cheaper actions with them and Regenerar Membro's effect: this "
                    + "core has no limbs or per-limb action cost. Surto de Ação's PA is applied.";
        }
    },

    /** Bônus Racial in the chosen Atributo; Adaptabilidade; Apex Multiplicador de PV +1 and RE to every element. */
    SELECAO_NATURAL_SUPERIOR("Seleção Natural Superior", DEVIANTE, 
            "Habilidade Passiva: Escolha um Atributo, este Monstro recebe Bônus Racial de +2 no Atributo escolhido.\n"
                    + "Habilidade Ativa – Adaptabilidade [3PA, 3PD]: RDS e Multiplicador de PV +3 por 3 Rodadas. Resfriamento 3.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Atributo escolhido recebe Bônus Racial de +2.\n"
                    + "• Adaptabilidade – Tempo de Resfriamento reduzido para 2 Rodadas.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Multiplicador de PV +1 e RE: Todos os Elementos.\n"
                    + "• Adaptabilidade – Multiplicador de PV +1 e Duração +2 Rodadas.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            return List.of(ChoiceSpec.one(ATTRIBUTE, AttributeDomain.values()));
        }

        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            return context.pick(ATTRIBUTE, AttributeDomain.class)
                    .map(domain -> Map.of(domain, context.isAtLeast(PREDADOR) ? 4 : 2))
                    .orElse(Map.of());
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.LIFE_MULTIPLIER && context.isAtLeast(APEX) ? 1 : 0;
        }

        @Override
        public int resolveElementalResistanceInstances(final ElementalType element, final AbilityContext context) {
            return context.isAtLeast(APEX) ? 1 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            int duration = apex ? 5 : 3;
            int lifeMultiplier = apex ? 4 : 3;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Adaptabilidade")
                    .description("RDS e Multiplicador de PV +" + lifeMultiplier + " por " + duration + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .durationInRounds(duration)
                    .cooldownRounds(context.isAtLeast(PREDADOR) ? 2 : 3)
                    .effect(bonus(ModifierType.DAMAGE_REDUCTION, MonstrousTraits.RDS_INSTANCE, duration, "Adaptabilidade"))
                    .effect(bonus(ModifierType.LIFE_MULTIPLIER, lifeMultiplier, duration, "Adaptabilidade"))
                    .build());
        }
    },

    /** Predador Defesas +2. The reach and Bote Inesperado's doubled reach are geometry. */
    ELASTICIDADE("Elasticidade", DEVIANTE, 
            "Efeito Passivo: Armas Naturais recebem Alcance como aprimoramento de Obra-Prima.\n"
                    + "Aprimoramento dos Predadores\n"
                    + "• Defesas +2\n"
                    + "• Efeito Ativo – Bote Inesperado [+1PA, 1PD]: Como parte da ativação desta Habilidade o Monstro deverá fazer um ataque corpo-a-corpo, o Alcance deste ataque é dobrado. Resfriamento 1.\n"
                    + "Aprimoramento dos Apex\n"
                    + "• Efeito Ativo – Bote Inesperado: Recebe a Corrente de Efeitos Monstruosa – Abdução.\n"
                    + "“Abdução – Alvo é agarrado, então realocado para um ponto adjacente ao Monstro. O Monstro recebe Bônus de +3 em Defesas por 1 Rodada.”") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DEFESAS && context.isAtLeast(PREDADOR) ? 2 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            if (!context.isAtLeast(PREDADOR)) {
                return List.of();
            }
            return List.of(MonstrousActiveAbility.builder()
                    .name("Bote Inesperado")
                    .description("Um ataque corpo-a-corpo com o Alcance dobrado.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(1)
                    .cooldownRounds(1)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Alcance on the Armas Naturais, Bote Inesperado's doubled reach and Abdução: reach and relocation "
                    + "are geometry, and no Corrente reaches a target. Defesas +2 and the active's price are applied.";
        }
    },

    /** Size ±1 for good, Tamanho Variável ±2 (Apex ±3) for 3 Rodadas (Abominação 5); Apex Força and PV; Abominação two Atributos. */
    TAMANHO_VARIAVEL("Tamanho Variável", PREDADOR, 
            "Efeito Passivo: A Categoria de Tamanho desse monstro é permanentemente aumentada ou reduzida em 1 nível.\n"
                    + "Efeito Ativo – Tamanho Variável [1PA, 1PD]: Pode temporariamente aumentar ou reduzir Categoria de Tamanho em 2 níveis. Duração de 3 Rodadas, Resfriamento 2.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Bônus Racial de Força +4 e Multiplicador de PV +1.\n"
                    + "• Efeito Ativo – Tamanho Variável: Aumento ou redução da Categoria de Tamanho muda para 3 níveis.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Bônus Racial de +2 em dois Atributo à escolha, exceto Força.\n"
                    + "• Efeito Ativo – Tamanho Variável: Duração aumenta para 5 Rodadas.") {
        @Override
        public List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
            if (category.isAtLeast(ABOMINACAO)) {
                return List.of(ChoiceSpec.one(DIRECTION, List.of(GROW, SHRINK)),
                        ChoiceSpec.many(ATTRIBUTES, abominationAttributeOptions(), 2));
            }
            return List.of(ChoiceSpec.one(DIRECTION, List.of(GROW, SHRINK)));
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return switch (type) {
                case SIZE_CATEGORY -> SHRINK.equals(context.pick(DIRECTION)) ? -1 : 1;
                case LIFE_MULTIPLIER -> context.isAtLeast(APEX) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
            Map<AttributeDomain, Integer> bonuses = new EnumMap<>(AttributeDomain.class);
            if (context.isAtLeast(APEX)) {
                bonuses.put(AttributeDomain.STRENGTH, 4);
            }
            if (context.isAtLeast(ABOMINACAO)) {
                context.picks(ATTRIBUTES, AttributeDomain.class).forEach(domain -> bonuses.merge(domain, 2, Integer::sum));
            }
            return bonuses;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            int steps = context.isAtLeast(APEX) ? 3 : 2;
            int duration = context.isAtLeast(ABOMINACAO) ? 5 : 3;
            return List.of(resize("Tamanho Variável (aumentar)", steps, duration),
                    resize("Tamanho Variável (reduzir)", -steps, duration));
        }

        @Override
        public String getUnappliedNote() {
            return "Growing and shrinking are two Efeitos Ativos, each with its own Resfriamento, where the rules have "
                    + "one — an Efeito Ativo here has no per-activation choice.";
        }
    },

    /** Furtividade GD +1 nível (Abominação +2), Apex +4 on it. Moving and attacking while hidden are not gated here. */
    CAMUFLAGEM_PERFEITA("Camuflagem Perfeita", PREDADOR, 
            "Efeito Passivo: GD de Furtividade aumenta em +1 nível.\n"
                    + "Efeito Ativo – Camuflagem Perfeita [3PA, 3PD]: Pode efetuar Furtividade mesmo à vista de outros personagens. Resfriamento 2.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Bônus de +4 em Furtividade.\n"
                    + "• Camuflagem Perfeita - Pode se mover durante a Furtividade, GD reduzida em -1 nível após se mover.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• GD de Furtividade aumenta em +1 nível\n"
                    + "• Camuflagem Perfeita - Pode atacar durante a Furtividade, GD reduzida em -2 níveis após atacar.") {
        @Override
        public int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                          final AbilityContext context) {
            if (skill != SkillType.FURTIVIDADE) {
                return 0;
            }
            return context.isAtLeast(ABOMINACAO) ? 2 : 1;
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.FURTIVIDADE_ROLL_BONUS && context.isAtLeast(APEX) ? 4 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Camuflagem Perfeita")
                    .description("Pode efetuar Furtividade mesmo à vista de outros personagens.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(3)
                    .cooldownRounds(2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "\"Mesmo à vista\", moving and attacking while hidden, and the GD dropping after each: HidingService "
                    + "never checks whether the hider is observed, and a monster's Furtividade is a GD it presents, "
                    + "not a concealment it holds. The GD, the +4 and the active's price are applied.";
        }
    },

    /** RDS; 1d6 over 2 Rodadas after being hurt (Abominação stacking to 3); Adaptação Milagrosa's 3d6 heal (Abominação RA and RC). */
    ADAPTACAO_MILAGROSA("Adaptação Milagrosa", APEX, 
            "Efeito Passiva: RDS. Após sofrer danos recupera 1d6PV durante 2 Rodadas (não cumulativo).\n"
                    + "Efeito Ativo – Adaptação Milagrosa [3PA, 6PD]: Recupera 3d6PV. Após sofrer danos se torna imune a mesma fonte de dano ao longo da Duração da Adaptação Milagrosa. Resfriamento 4, Duração 4 Rodadas.\n"
                    + "“São fontes de danos as Armas, Armadilhas, Doenças, Habilidades, Magias, Maldições, Venenos, além de efeitos mundanos, como quedas, esmagamentos etc.”\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Recuperação de vida pós danos se torna cumulativa, até 3 vezes, mas a Duração não se renova até o fim do Efeito Primário.\n"
                    + "• Adaptação Milagrosa – recebe RA, Resistência à Críticos e Resistência à Corrente de Efeitos +2 enquanto ativo.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.DAMAGE_REDUCTION ? MonstrousTraits.RDS_INSTANCE : 0;
        }

        @Override
        public List<TemporaryEffect> resolveDamageTakenEffects(final int finalDamage, final AbilityContext context) {
            int limit = context.isAtLeast(ABOMINACAO) ? 3 : 1;
            return List.of(RecurringDice.healing(Dice.of(1), 2, "Adaptação Milagrosa", limit));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder active = MonstrousActiveAbility.builder()
                    .name("Adaptação Milagrosa")
                    .description("Recupera 3d6PV.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(6)
                    .durationInRounds(ADAPTACAO_DURATION)
                    .cooldownRounds(4)
                    .dice(Dice.of(3))
                    .onRolled((sheet, total) -> sheet.heal(total));
            if (context.isAtLeast(ABOMINACAO)) {
                active.effect(bonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION, MonstrousTraits.REDUCTION_INSTANCE,
                                ADAPTACAO_DURATION, "Adaptação Milagrosa"))
                        .effect(bonus(ModifierType.CRITICAL_RESISTANCE, MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE,
                                ADAPTACAO_DURATION, "Adaptação Milagrosa"));
            }
            return List.of(active.build());
        }

        @Override
        public String getUnappliedNote() {
            return "Immunity to a damage source already suffered: a hit carries no source (Arma, Magia, Veneno …) to "
                    + "compare against. At Abominação the stacked regeneration renews its Duração, and the +2 "
                    + "Resistência à Correntes has no number.";
        }
    },

    /** Vigor PV per Rodada (Abominação +1d6). Regrowing two limbs for one is not modelled. */
    REGENERACAO_MULTIPLICATIVA("Regeneração Multiplicativa", APEX, 
            "Efeito Passivo: Recupera Vigor PV por Rodada\n"
                    + "Efeito Ativo – Regeneração Multiplicativa: Apenas após perder um membro, 2 novos membros do mesmo tipo podem surgir no local. Recebe bônus de +1PA para cada par de membro regenerado desta forma. O novo membro extra, apenas o adicional, definha e cai após 3 Rodadas.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Recuperação de Vida por Rodada aumenta em +1d6.\n"
                    + "• Regeneração Multiplicativa – Membros adicionais definham apenas após 5 Rodadas.") {
        @Override
        public List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
            int vigor = character.getEffectiveAttributeTotal(AttributeDomain.VIGOR);
            TemporaryEffect regeneration = Regeneration.openEnded(vigor, "Regeneração Multiplicativa");
            if (!context.isAtLeast(ABOMINACAO)) {
                return List.of(regeneration);
            }
            return List.of(regeneration, RecurringDice.healing(Dice.of(1), null, "Regeneração Multiplicativa (1d6)"));
        }

        @Override
        public String getUnappliedNote() {
            return "Regrowing two limbs for each one lost, and the +1PA per pair: this core has no limbs. The Vigor PV "
                    + "per Rodada (and the Abominação's +1d6) are applied.";
        }
    };

    /** {@link #MEMBROS_MULTIPLOS}'s pick. */
    public static final String LIMB = "limb";

    /** The limbs a Membros Múltiplos may multiply — "como braços, cabeças etc.". */
    public static final List<String> LIMBS = List.of("BRACOS", "CABECAS", "PERNAS", "CAUDAS", "ASAS", "TENTACULOS");

    /** {@link #SELECAO_NATURAL_SUPERIOR}'s pick. */
    public static final String ATTRIBUTE = "attribute";

    /** {@link #TAMANHO_VARIAVEL}'s picks. */
    public static final String DIRECTION = "direction";
    public static final String ATTRIBUTES = "attributes";
    public static final String GROW = "GROW";
    public static final String SHRINK = "SHRINK";

    private static final int ADAPTACAO_DURATION = 4;

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    MutanteMonstruosoAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.MUTANTE_MONSTRUOSO;
    }

    /** APPLIED when every clause is, PARTIAL when {@link #getUnappliedNote()} names one that isn't. */
    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }

    /** Sets of extra limbs: one, a second at Deviante, a third at Predador, doubled at Apex. */
    private static int extraLimbSets(final AbilityContext context) {
        int sets = 1 + (context.isAtLeast(DEVIANTE) ? 1 : 0) + (context.isAtLeast(PREDADOR) ? 1 : 0);
        return context.isAtLeast(APEX) ? sets * 2 : sets;
    }

    private static MonstrousActiveAbility resize(final String name, final int steps, final int duration) {
        return MonstrousActiveAbility.builder()
                .name(name)
                .description("Categoria de Tamanho " + (steps > 0 ? "+" : "") + steps + " por " + duration + " Rodadas.")
                .actionPointCost(ActionCost.ofActionPoints(1))
                .determinationPointCost(1)
                .durationInRounds(duration)
                .cooldownRounds(2)
                .effect(bonus(ModifierType.SIZE_CATEGORY, steps, duration, "Tamanho Variável"))
                .build();
    }

    /** "Bônus Racial de +2 em dois Atributo à escolha, exceto Força". */
    private static List<String> abominationAttributeOptions() {
        return Arrays.stream(AttributeDomain.values()).filter(domain -> domain != AttributeDomain.STRENGTH)
                .map(Enum::name).toList();
    }
}
