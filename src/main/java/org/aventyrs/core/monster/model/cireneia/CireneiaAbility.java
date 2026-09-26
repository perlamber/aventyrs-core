package org.aventyrs.core.monster.model.cireneia;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;

/**
 * The Habilidades Monstruosas of the Modelo Abençoado de Cireneia — {@code
 * criacao-de-monstros.txt}. The one Modelo built end to end; the reference for the other five.
 *
 * <p>Every Aprimoramento is folded into the hook that answers for it, gated with {@link
 * MonsterCategory#isAtLeast}, so a held Habilidade grows with its holder's Categoria and nothing
 * is ever re-acquired.
 *
 * <p><b>Readings the text leaves open</b>, decided here:
 * <ul>
 *   <li>Movimento Aprimorado's "Iniciativa +2" (Deviante) and "Movimento Base +2, Inciativa +2"
 *   (Apex) name no Efeito Ativo, unlike every Aprimoramento that modifies one ("Impulso –",
 *   "Celeridade Mór –"), so they raise the passive, not Aceleração.</li>
 *   <li>Relampejante's Apex "+1 Reação ou +1 Ação Livre" repeats the original pick rather than
 *   asking again — the Habilidade carries one choice.</li>
 * </ul>
 *
 * <p>Two Habilidades are held but not applied ({@link #isImplemented()} is {@code false}):
 * {@link #OFUSCAR} is entirely a Corrente de Efeitos and a Resistência a Correntes de Efeitos,
 * neither of which this core can land on a target; {@link #PREFERIDO_DE_CIRENEIA} needs Efeitos
 * tagged Temporal or Planar, and no such tag exists.
 */
@Getter
public enum CireneiaAbility implements MonstrousAbility {

    ATRIBUTOS_APRIMORADOS("Atributos Aprimorados", PRESA,
            "Efeito Passivo: Bônus Racial de Destreza +2\n"
                    + "Efeito Ativo – Impulso [Ação Livre / 2PD]: Apenas uma vez por Cena, com Duração de 3 Rodadas, recebem Bônus Variável de +3 em Destreza.\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Impulso +2 Rodadas.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Bônus Racial de Destreza +2\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Impulso - Bônus de Destreza muda para +6.") {
        @Override
        public Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final MonsterCategory category,
                                                                            final String choice) {
            return Map.of(AttributeDomain.DEXTERITY, category.isAtLeast(PREDADOR) ? 4 : 2);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            int bonus = category.isAtLeast(APEX) ? 6 : 3;
            int duration = category.isAtLeast(DEVIANTE) ? 5 : 3;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Impulso")
                    .description("Apenas uma vez por Cena, com Duração de " + duration
                            + " Rodadas, recebe Bônus Variável de +" + bonus + " em Destreza.")
                    .actionPointCost(ActionCost.FREE_ACTION)
                    .determinationPointCost(2)
                    .durationInRounds(duration)
                    .cooldownRounds(MonstrousActiveAbility.ONCE_PER_SCENE)
                    .effect(() -> new TemporaryBonus(ModifierType.DEXTERITY_BONUS, bonus, duration, "Impulso"))
                    .build());
        }
    },

    MOVIMENTO_APRIMORADO("Movimento Aprimorado", PRESA,
            "Efeito Passivo: Movimento Base +3 e Vantagem nas Rolagens de Ataque e Danos durante Investidas.\n"
                    + "Efeito Ativo – Aceleração [1PA, 2PD]: Iniciativa +5 por 2 Rodadas, Resfriamento 2.\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Iniciativa +2.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Investidas sempre acertam o alvo.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Movimento Base +2, Inciativa +2.") {
        @Override
        public int resolveModifier(final ModifierType type, final MonsterCategory category, final String choice) {
            return switch (type) {
                case MOVEMENT -> category.isAtLeast(APEX) ? 5 : 3;
                case INITIATIVE -> (category.isAtLeast(DEVIANTE) ? 2 : 0) + (category.isAtLeast(APEX) ? 2 : 0);
                default -> 0;
            };
        }

        @Override
        public int resolveChargeAttackBonus(final MonsterCategory category) {
            return Skill.ADVANTAGE_BONUS;
        }

        @Override
        public int resolveChargeDamageBonus(final MonsterCategory category) {
            return Skill.ADVANTAGE_BONUS;
        }

        @Override
        public boolean chargesAlwaysHit(final MonsterCategory category) {
            return category.isAtLeast(PREDADOR);
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Aceleração")
                    .description("Iniciativa +5 por 2 Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(2)
                    .cooldownRounds(2)
                    .effect(() -> new TemporaryBonus(ModifierType.INITIATIVE, 5, 2, "Aceleração"))
                    .build());
        }
    },

    CELERIDADE("Celeridade", DEVIANTE,
            "Efeito Passivo: PA +1\n"
                    + "Efeito Ativo – Celeridade Mór [Ação Livre / 3PD]: PA +2 neste Turno – Resfriamento 2.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Celeridade Mór – Tempo de Resfriamento muda para 1.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• PA +1\n"
                    + "• Celeridade Mór – Custo muda para 2PD.") {
        @Override
        public int resolveModifier(final ModifierType type, final MonsterCategory category, final String choice) {
            if (type != ModifierType.ACTION_POINTS) {
                return 0;
            }
            return category.isAtLeast(APEX) ? 2 : 1;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            int cooldown = category.isAtLeast(PREDADOR) ? 1 : 2;
            int cost = category.isAtLeast(APEX) ? 2 : 3;
            return List.of(MonstrousActiveAbility.builder()
                    .name("Celeridade Mór")
                    .description("PA +2 neste Turno. Resfriamento " + cooldown + ".")
                    .actionPointCost(ActionCost.FREE_ACTION)
                    .determinationPointCost(cost)
                    .durationInRounds(1)
                    .cooldownRounds(cooldown)
                    .effect(() -> new TemporaryBonus(ModifierType.ACTION_POINTS, 2, 1, "Celeridade Mór"))
                    .build());
        }
    },

    RELAMPEJANTE("Relampejante", DEVIANTE,
            "Efeito Passivo: +1 Reação ou +1 Ação Livre\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• +1 Reação ou +1 Ação Livre, opção faltante do efeito Base e Relampejante.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• +1 Reação ou +1 Ação Livre.") {
        @Override
        public List<String> getChoiceOptions() {
            return List.of(REACTION_CHOICE, FREE_ACTION_CHOICE);
        }

        @Override
        public int resolveModifier(final ModifierType type, final MonsterCategory category, final String choice) {
            ModifierType chosen = FREE_ACTION_CHOICE.equals(choice) ? ModifierType.FREE_ACTIONS : ModifierType.REACTIONS;
            ModifierType other = chosen == ModifierType.REACTIONS ? ModifierType.FREE_ACTIONS : ModifierType.REACTIONS;
            int total = 0;
            if (type == chosen) {
                total += 1 + (category.isAtLeast(APEX) ? 1 : 0);
            }
            if (type == other && category.isAtLeast(PREDADOR)) {
                total += 1;
            }
            return total;
        }
    },

    LIBERDADE_SELVAGEM("Liberdade Selvagem", PREDADOR,
            "Efeito Passivo: Perícias Baseadas em Destreza GD +1\n"
                    + "Efeito Ativo – Sonido [+1PA, 2PD]: Deve ser ativado juntamente com outra ação, estação não permite Reações – Resfriamento 2.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Sonido – Tempo de resfriamento muda para 1.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Sonido – Tempo de Ativação muda para Desprezível, Custo muda para 0PD.") {
        @Override
        public int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                          final MonsterCategory category) {
            return governingAttribute == AttributeDomain.DEXTERITY ? 1 : 0;
        }

        /**
         * Sonido rides on another action, so its price is the "+1PA" on top of it. "Estação não
         * permite Reações" has no mechanism here — no action in this core records whether it may be
         * reacted to — so the active carries no effect, only its price and Resfriamento. At
         * Abominação it becomes "Desprezível" and free.
         */
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            boolean abomination = category.isAtLeast(MonsterCategory.ABOMINACAO);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Sonido")
                    .description("Deve ser ativado juntamente com outra ação; esta ação não permite Reações.")
                    .actionPointCost(abomination ? ActionCost.NONE : ActionCost.ofActionPoints(1))
                    .determinationPointCost(abomination ? 0 : 2)
                    .durationInRounds(1)
                    .cooldownRounds(category.isAtLeast(APEX) ? 1 : 2)
                    .build());
        }
    },

    /**
     * TODO: the whole Habilidade is a Corrente de Efeitos Monstruosa ("Ofuscar – Alvo sofre Redutor
     * de -1PA por 1 Rodada, este Monstro recebe Bônus de +1PA") riding the monster's attacks, plus
     * a -2 to enemies' Resistência à Correntes de Efeitos. {@code EffectChainService} computes a
     * margin but nothing applies a Corrente when an attack lands, and a monster's attack is resolved
     * on the defender's side ({@code AttackReceiver}), which has no hook for one. The active is kept
     * for its price and Resfriamento.
     */
    OFUSCAR("Ofuscar", PREDADOR,
            "Efeito Passivo: A Resistência à Correntes de Efeitos dos inimigos deste Monstro são reduzidas em -2. Resfriamento 1.\n"
                    + "Efeito Ativo – Ofuscar [+1PA, 2PD]: Neste Turno as Perícias de Ataques recebem a Corrente de Efeitos Monstruosa – Ofuscar.\n"
                    + "“Ofuscar – Alvo sofre Redutor de -1PA por 1 Rodada, este Monstro recebe Bônus de +1PA por 1 Rodada.”\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Ofuscar – Duração muda para 1 Rodada e adicionalmente afeta as Defesas contra os ataques Corpo-a-Corpo.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Ofuscar – Se torna um efeito Passivo, sempre ativo.") {
        @Override
        public boolean isImplemented() {
            return false;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            if (category.isAtLeast(MonsterCategory.ABOMINACAO)) {
                return List.of();
            }
            return List.of(MonstrousActiveAbility.builder()
                    .name("Ofuscar")
                    .description("Neste Turno as Perícias de Ataques recebem a Corrente de Efeitos Monstruosa – Ofuscar.")
                    .actionPointCost(ActionCost.ofActionPoints(1))
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .cooldownRounds(1)
                    .build());
        }
    },

    /**
     * The monster's own half — "+2PA" — is applied, for the Duração of Concentração + 2 Rodadas,
     * taken as 2 (this core has no Concentração ledger). TODO: the condition ("enquanto houver pelo
     * menos 1 personagem em Distância Curta") and the "-2PA" on everyone else in range are geometry,
     * which this core never does; the caller applies the latter to each character it finds in range.
     */
    AURA_DO_PARADOXO("Aura do Paradoxo", APEX,
            "Efeito Ativo [2PA, 3PD]: Enquanto houver pelo menos 1 personagem em Distância Curta que não possua Aura do Paradoxo, este monstro recebe Bônus de +2PA. Todos os outros personagens no Alcance recebem Redutor de -2PA.\n"
                    + "Pontos de Ação reduzidos pela Aura do Paradoxo são recuperados no início da Rodada seguinte, mas apenas após os personagens deixarem a Área de Efeito.\n"
                    + "A Duração da Aura do Paradoxo é de Concentração + 2 Rodadas, o efeito é encerrado automaticamente (mesmo com a Concentração mantida) se não houver outros personagens em sua Área de Efeito por 2 Rodadas\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Ofuscar – Se torna um efeito Passivo, sempre ativo.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Aura do Paradoxo")
                    .description("Enquanto houver pelo menos 1 personagem em Distância Curta sem Aura do Paradoxo, "
                            + "recebe Bônus de +2PA; todos os outros personagens no Alcance recebem Redutor de -2PA.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(3)
                    .durationInRounds(AURA_DURATION)
                    .effect(() -> new TemporaryBonus(ModifierType.ACTION_POINTS, 2, AURA_DURATION, "Aura do Paradoxo"))
                    .build());
        }
    },

    /**
     * TODO: "Efeitos Temporais e Planares em Distância Média consideram este monstro como ativador
     * ou conjurador" — no Efeito, Magia or Habilidade in this core is tagged Temporal or Planar, so
     * there is nothing to redirect. Held and counted, not applied.
     */
    PREFERIDO_DE_CIRENEIA("Preferido de Cireneia", APEX,
            "Efeito Passivo: Efeitos Temporais e Planares em Distância Média consideram este monstro como ativador ou conjurador.\n"
                    + "Esta Habilidade não é ativada quando o Efeito Temporal ou Planar é conjurado por outro personagem Preferido de Cireneia.\n"
                    + "Aprimoramentos das Abominações\n"
                    + "• Área de Efeito muda para Distância Longa e pode ser utilizado para pungar efeitos Temporais e Planares de personagens Apex, mas não de outra Abominações.") {
        @Override
        public boolean isImplemented() {
            return false;
        }
    };

    /** {@link #RELAMPEJANTE}'s pick: the extra Reação. */
    public static final String REACTION_CHOICE = "REACTIONS";

    /** {@link #RELAMPEJANTE}'s pick: the extra Ação Livre. */
    public static final String FREE_ACTION_CHOICE = "FREE_ACTIONS";

    /** Aura do Paradoxo's "Concentração + 2 Rodadas", with no Concentração tracked. */
    private static final int AURA_DURATION = 2;

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    CireneiaAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ABENCOADO_DE_CIRENEIA;
    }

    @Override
    public boolean isImplemented() {
        return true;
    }
}
