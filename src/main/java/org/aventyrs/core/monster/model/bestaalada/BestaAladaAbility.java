package org.aventyrs.core.monster.model.bestaalada;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.SkillDifficultyShift;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.aventyrs.core.monster.model.MonstrousTraits.bonus;
import static org.aventyrs.core.monster.model.MonstrousTraits.standing;

/**
 * The Habilidades Monstruosas of the Modelo Besta Alada — {@code criacao-de-monstros.txt}.
 *
 * <p>Built the way {@code CireneiaAbility} is, on {@code CombatantSheet#isFlying()}: whoever runs
 * the table takes the creature off and lands it ({@code setFlying}), Voo Permanente keeps it up for
 * good, and every "enquanto voando" clause is a {@code resolveWhileFlyingEffects} the sheet applies
 * on take-off and lifts on landing. There is still no separate flight <i>movement</i> — a flying
 * creature moves at its Movimento Base, raised by what these grant.
 */
@Getter
public enum BestaAladaAbility implements MonstrousAbility {

    /** Deviante Movimento +2; Mergulho Atroz (only in flight) +2d6, landing; Apex Margem +4. */
    PLANAR_E_PAIRAR("Planar e Pairar", PRESA, 
            "Efeito Passivo: Adquire Movimento Base de Voo por 2 Rodadas após correr ou saltar.\n"
                    + "Efeito Ativo – Mergulho Atroz [3PA, 2PD]: Apenas enquanto voando, este ataque é uma investida e inflige +2d6 pontos de danos. Esta habilidade encerra efeitos de voo atuais. Resfriamento 2.\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Movimento Base +2UD\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Mergulho Atroz – Corrente de Efeitos Monstruosa – Empurrão Atroz. Este ataque recebe Atordoante como um Efeito Crítico adicional.\n"
                    + "“Empurrão Atroz – Alvo é empurrado 2UD e então derrubado.”\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Mergulho Atroz – Margem Crítica Menor aumentada em +4 números, recebe Atordoante como Efeito Crítico Adicional.") {
        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.MOVEMENT && context.isAtLeast(DEVIANTE) ? 2 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder mergulho = MonstrousActiveAbility.builder()
                    .name("Mergulho Atroz")
                    .description("Apenas enquanto voando: uma Investida que inflige +2d6 de dano, e encerra o voo.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .cooldownRounds(2)
                    .usableWhen(CombatantSheet::isFlying)
                    .onActivated(sheet -> sheet.setFlying(false))
                    .effect(bonus(ModifierType.EXTRA_DAMAGE_DICE, 2, 1, "Mergulho Atroz"));
            if (context.isAtLeast(APEX)) {
                mergulho.effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 4, 1, "Mergulho Atroz"));
            }
            return List.of(mergulho.build());
        }

        @Override
        public String getUnappliedNote() {
            return "Taking flight for 2 Rodadas after running or jumping (the caller takes off and lands; nothing times "
                    + "a flight), Empurrão Atroz (no Corrente reaches a target) and the extra Atordoante (an Efeito "
                    + "Ativo cannot add a Crítico to a later attack). The +2d6 rides every attack that Turn.";
        }
    },

    /** Flight at Movimento +2; Investida Aérea's +1 GD, +1d6 (Apex +2d6), Deviante +3 Movimento, Predador Margem +4. */
    DESFILE_DE_SYLPH("Desfile de Sylph", PRESA, 
            "Efeito Passivo: Movimento Base de Voo e Movimento Base +2UD enquanto voando.\n"
                    + "Efeito Ativo – Investida Aérea [3PA, 2PD]: Investida Área é uma investida cujo ataque não interrompe o movimento, permitindo o deslocamento máximo enquanto inflige danos em um alvo em sua trajetória. O Grau de Dificuldade da Perícia de Ataque aumenta em +1 nível, este ataque recebe a corrente de Efeitos Monstruosa – Abduzir. Resfriamento 2.\n"
                    + "“Abduzir – Dano do ataque aumenta em +1d6, o alvo é agarrado e movido juntamente com o Monstro.”\n"
                    + "Aprimoramentos dos Deviantes\n"
                    + "• Investida Aérea – Movimento Base +3UD.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Investida Aérea – Margem Crítica Menor +4, Efeito Crítico adicional Guilhotina.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Investida Aérea – Dano +1d6, Tempo de Resfriamento reduzido para 1 Rodada.") {
        @Override
        public List<TemporaryEffect> resolveWhileFlyingEffects(final AbilityContext context) {
            return List.of(standing(ModifierType.MOVEMENT, 2, "Desfile de Sylph"));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            Set<SkillType> attack = Set.of(SkillType.ATAQUE_CORPO_A_CORPO);
            MonstrousActiveAbility.MonstrousActiveAbilityBuilder investida = MonstrousActiveAbility.builder()
                    .name("Investida Aérea")
                    .description("Uma Investida que não interrompe o movimento: GD do ataque +1 nível e dano +"
                            + (apex ? 2 : 1) + "d6.")
                    .actionPointCost(ActionCost.ofActionPoints(3))
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .cooldownRounds(apex ? 1 : 2)
                    .effect(() -> new SkillDifficultyShift(attack, 1, 1, "Investida Aérea"))
                    .effect(bonus(ModifierType.EXTRA_DAMAGE_DICE, apex ? 2 : 1, 1, "Investida Aérea"));
            if (context.isAtLeast(DEVIANTE)) {
                investida.effect(bonus(ModifierType.MOVEMENT, 3, 1, "Investida Aérea"));
            }
            if (context.isAtLeast(PREDADOR)) {
                investida.effect(bonus(ModifierType.LESSER_CRITICAL_MARGIN, 4, 1, "Investida Aérea"));
            }
            return List.of(investida.build());
        }

        @Override
        public String getUnappliedNote() {
            return "Abduzir's grab and carry, and the Predador's extra Guilhotina: no Corrente reaches a target and an "
                    + "Efeito Ativo cannot add a Crítico to a later attack. The GD, dice, Movimento and Margem are "
                    + "applied for the Turn.";
        }
    },

    /** +2 on Perícias Físicas while flying; Domínio dos Céus +2PA (Resfriamento 3, Predador 2); Apex +2PA for good. */
    DOMINIO_DOS_CEUS("Domínio dos Céus", DEVIANTE, 
            "Efeito Passivo: Perícias Físicas recebe Bônus de +2 enquanto voando.\n"
                    + "Efeito Ativo – Domínio dos Céus [Ação Livre, 3PD]: Recebe Bônus de +2PA neste Turno. Resfriamento 3.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Domínio dos Céus – Resfriamento muda para 2 Rodadas\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Domínio dos Céus – Se torna um Efeito Passivo, Sempre ativo.") {
        @Override
        public List<TemporaryEffect> resolveWhileFlyingEffects(final AbilityContext context) {
            List<TemporaryEffect> effects = new ArrayList<>();
            for (SkillType skill : MonstrousTraits.physicalSkills()) {
                effects.add(standing(skill.getRollBonusType(), 2, "Domínio dos Céus"));
            }
            return effects;
        }

        @Override
        public int resolveModifier(final ModifierType type, final AbilityContext context) {
            return type == ModifierType.ACTION_POINTS && context.isAtLeast(APEX) ? 2 : 0;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            if (context.isAtLeast(APEX)) {
                return List.of();
            }
            return List.of(MonstrousActiveAbility.builder()
                    .name("Domínio dos Céus")
                    .description("Recebe Bônus de +2PA neste Turno.")
                    .actionPointCost(ActionCost.FREE_ACTION)
                    .determinationPointCost(3)
                    .durationInRounds(1)
                    .cooldownRounds(context.isAtLeast(PREDADOR) ? 2 : 3)
                    .effect(bonus(ModifierType.ACTION_POINTS, 2, 1, "Domínio dos Céus"))
                    .build());
        }
    },

    /** +5 Defesas and RDS while flying; Defesa Aérea's Meio-Dano (a Reação), passive in flight at Apex. */
    DEFESA_AREA("Defesa Aérea", DEVIANTE, 
            "Efeito Passivo: Este Monstro recebe Bônus Racial de +5 em Defesas e RDS enquanto voando.\n"
                    + "Efeito Ativo – Defesa Área [Reação, 2PD]: Danos de inimigos que não estejam voando são Reduzidos à Metade (efeito de Meio-Dano). Duração 1 Rodada, Resfriamento 2.\n"
                    + "Aprimoramentos dos Predadores\n"
                    + "• Defesa Aérea – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                    + "Aprimoramentos dos Apex\n"
                    + "• Defesa Aérea – Enquanto o Monstro estiver voando esta Habilidade torna-se um Efeito Passivo, sempre ativo.") {
        @Override
        public List<TemporaryEffect> resolveWhileFlyingEffects(final AbilityContext context) {
            List<TemporaryEffect> effects = new ArrayList<>();
            effects.add(standing(ModifierType.DEFESAS, 5, "Defesa Aérea"));
            effects.add(standing(ModifierType.DAMAGE_REDUCTION, MonstrousTraits.RDS_INSTANCE, "Defesa Aérea"));
            if (context.isAtLeast(APEX)) {
                effects.add(new DamageScopeEffect(DamageScopeEffect.Kind.HALVES, DamageScope.ALL, null, "Defesa Aérea"));
            }
            return effects;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Defesa Aérea")
                    .description("Danos de inimigos que não estejam voando são reduzidos à Metade por 1 Rodada.")
                    .actionPointCost(ActionCost.REACTION)
                    .determinationPointCost(2)
                    .durationInRounds(1)
                    .cooldownRounds(context.isAtLeast(PREDADOR) ? 1 : 2)
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.HALVES, DamageScope.ALL, 1, "Defesa Aérea (Reação)"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "\"De inimigos que não estejam voando\": a hit carries no attacker, so the Meio-Dano halves every "
                    + "hit, flying attacker or not.";
        }
    },

    /** Always flying. Voo Livre's release from what grounds it is not modelled; its price is. */
    VOO_PERMANENTE("Voo Permanente", PREDADOR, 
            "Efeito Passivo: Este Monstro está sempre voando, não precisando retornar ao solo mesmo para ações comuns, como comer ou dormir. O Movimento Base de Voo está sempre ativo.\n"
                    + "Efeito Ativo – Voo Livre [3PA 5PD]: Se algum Efeito puder impedir este Monstro de voar, então o efeito é anulado. O monstro se torna imune a novas instancias deste mesmo efeito por 3 Rodadas. Resfriamento 1.\n"
                    + "Aprimoramento Apex\n"
                    + "• Voo Livre – tempo de ativação reduzido para 2PA, custo reduzido para 3PD.\n"
                    + "Aprimoramento abominação\n"
                    + "• Voo Livre – tempo de ativação reduzido para 1PA, Duração aumentada para 5 Rodadas.") {
        @Override
        public boolean keepsFlying(final AbilityContext context) {
            return true;
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean abominacao = context.isAtLeast(ABOMINACAO);
            boolean apex = context.isAtLeast(APEX);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Voo Livre")
                    .description("Anula um efeito que impediria o voo e fica imune a ele por " + (abominacao ? 5 : 3) + " Rodadas.")
                    .actionPointCost(ActionCost.ofActionPoints(abominacao ? 1 : apex ? 2 : 3))
                    .determinationPointCost(apex ? 3 : 5)
                    .durationInRounds(abominacao ? 5 : 3)
                    .cooldownRounds(1)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Voo Livre's release and immunity: no effect in this core grounds a flier. Only its price is applied.";
        }
    },

    /** +2PA while flying; Movimento Instantâneo (only in flight). */
    SUPERSONICO("Supersônico", PREDADOR, 
            "Efeito Passivo: Enquanto voando recebe Bônus de +2PA, então o Movimento Base de Voo aumenta em +2UD para cada ação de movimento realizada no mesmo Turno.\n"
                    + "Efeito Ativo – Movimento Instantâneo [1PA, 5PD]: enquanto voando, se move instantaneamente para um espaço dentro de seu alcance (Movimento Base de Voo). Esta ação não provoca reações, o local alvo deve ser uma linha reta, sem obstáculos ou barreiras. Resfriamento 2.\n"
                    + "Aprimoramento Apex\n"
                    + "• Movimento Instantâneo – Tempo de Ação muda para Ação Livre, Custo reduzido para 3PD\n"
                    + "Aprimoramento abominação\n"
                    + "• Movimento instantâneo – pode ser mover para qualquer ponto em seu raio de visão (limitado ao Movimento Base de Voo), mesmo que não seja uma linha reta, permitindo desviar de barreiras e obstáculos. Tempo de Resfriamento muda para 1 Rodada.") {
        @Override
        public List<TemporaryEffect> resolveWhileFlyingEffects(final AbilityContext context) {
            return List.of(standing(ModifierType.ACTION_POINTS, 2, "Supersônico"));
        }

        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean apex = context.isAtLeast(APEX);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Movimento Instantâneo")
                    .description("Enquanto voando, move-se instantaneamente até o alcance do seu Movimento Base de Voo.")
                    .actionPointCost(apex ? ActionCost.FREE_ACTION : ActionCost.ofActionPoints(1))
                    .determinationPointCost(apex ? 3 : 5)
                    .cooldownRounds(context.isAtLeast(ABOMINACAO) ? 1 : 2)
                    .usableWhen(CombatantSheet::isFlying)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "+2UD per movement action in the same Turn, and the instant move itself: the caller moves the token. "
                    + "The +2PA in flight and the move's price are applied.";
        }
    },

    /** Forma de Vento: immune to everything not Elemental or Primordial for 2 Rodadas. */
    PREDILETO_DE_SYLPH("Predileto de Sylph", APEX, 
            "Efeito Passivo: Sempre sob efeito de Furtividade enquanto voando. Ações ofensivas efetuadas pelo Monstro desativam a Furtividade, retornando ao estado furtivo após 2 Rodadas sem novas ações ofensivas.\n"
                    + "Efeito Ativo – Forma de Vento [3PA, 5PD]: Entra imediatamente em Furtividade. Adicionalmente o corpo do Monstro se torna incorpóreo, se tornando imune à efeitos e danos que não sejam Elementais ou Primordiais. O efeito adicional possui Duração de Concentração +2 Rodadas e Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Forma de Vento – Tempo de Ação reduzido para 1PA, Custo de Ativação reduzido para 3PD, Tempo de Resfriamento reduzido para 1 Rodada.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            boolean abominacao = context.isAtLeast(ABOMINACAO);
            return List.of(MonstrousActiveAbility.builder()
                    .name("Forma de Vento")
                    .description("Corpo incorpóreo: imune a efeitos e danos que não sejam Elementais ou Primordiais.")
                    .actionPointCost(ActionCost.ofActionPoints(abominacao ? 1 : 3))
                    .determinationPointCost(abominacao ? 3 : 5)
                    .durationInRounds(FORMA_DE_VENTO_DURATION)
                    .cooldownRounds(abominacao ? 1 : 2)
                    .effect(() -> new DamageScopeEffect(DamageScopeEffect.Kind.IMMUNE,
                            DamageScope.NON_ELEMENTAL_NON_PRIMORDIAL, FORMA_DE_VENTO_DURATION, "Forma de Vento"))
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Always hidden while flying and Forma de Vento's Furtividade: a monster's concealment is not rolled "
                    + "and HidingService takes a roll. Concentração is taken as 0, so the immunity lasts 2 Rodadas; "
                    + "\"efeitos\" other than damage are not refused.";
        }
    },

    /** Ranged physical immunity and the storm are not modelled; Invocar Tempestade's price is. */
    INVOCAR_DE_TEMPESTADES("Invocar de Tempestades", APEX, 
            "Efeito Passivo: Uma aura de Ventos Caóticos cerca este Monstro. A Área Circular Curta ao redor é sempre terreno difícil, o efeito aumento para Área Circular Média enquanto o monstro estiver voando. Este monstro é imune a Ataques à Distância físicos.\n"
                    + "Efeito Ativo - Invocar Tempestade [2PA 3PD]: No ato da Ativação e no início de cada Rodada, Personagens afetados pela aura de Ventos Caóticos são cegados e empurrados 2UD para trás a cada Rodada. Duração 3 Rodadas e Resfriamento 2.\n"
                    + "Aprimoramento das Abominações\n"
                    + "• Invocar Tempestade – Antes de serem empurrados, os personagens afetados pela aura de Ventos Caóticos sofrem 2d6 pontos de Dano Elemental: Vento. No fim de cada Rodada cada personagem na área sofre 2d6 pontos de Dano Elemental: Eletricidade. Danos causados por Invocar Tempestade são reduzidos em -1 para cada UD entre o personagem e o Monstro.") {
        @Override
        public List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
            return List.of(MonstrousActiveAbility.builder()
                    .name("Invocar Tempestade")
                    .description("Personagens na aura de Ventos Caóticos são cegados e empurrados 2UD a cada Rodada.")
                    .actionPointCost(ActionCost.ofActionPoints(2))
                    .determinationPointCost(3)
                    .durationInRounds(3)
                    .cooldownRounds(2)
                    .build());
        }

        @Override
        public String getUnappliedNote() {
            return "Immunity to ranged physical attacks (a hit carries no range), the difficult terrain of the aura and "
                    + "everything Invocar Tempestade does to others: geometry. Only the price is applied.";
        }
    };

    private static final int FORMA_DE_VENTO_DURATION = 2;

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    BestaAladaAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.BESTA_ALADA;
    }

    /** APPLIED when every clause is, PARTIAL when {@link #getUnappliedNote()} names one that isn't. */
    @Override
    public ImplementationStatus getImplementationStatus() {
        return getUnappliedNote() == null ? ImplementationStatus.APPLIED : ImplementationStatus.PARTIAL;
    }
}
