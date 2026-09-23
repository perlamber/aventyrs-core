package org.aventyrs.core.title.senhordabriga;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;
import static org.aventyrs.core.title.PDCost.variable;

/**
 * The Habilidades/Suprema gated on {@link SenhorDaBrigaSpecialization#FANTASMA_DO_RINGUE}
 * specifically. Each Habilidade carries a "Malícia de Valentão" upgrade that applies while its
 * holder also holds that Suprema — read through {@link SenhorDaBriga#holdsMaliciaDeValentao()}.
 */
@Getter
@AllArgsConstructor
public enum FantasmaDoRingueAbility implements AventyrTitleAbility {

    // Requer Especialização Fantasma do Ringue. A Reação on ReactionTrigger
    // .SELF_TARGETED_BY_MELEE_ATTACK, offered only when the Senhor da Briga is the sole target and
    // the attacker stands within reach (#isReactionAvailable). Real through CruzDeSangueInteraction,
    // all for 1 Rodada: Defesas reduced by half the holder's Destreza (a negative DEFESAS Blessing);
    // Margem Crítica Menor +2 on Defesas and on Arma Natural melee attacks (an activation window
    // SenhorDaBriga#resolveCriticalMarginIncrease reads — Margem Crítica has no ModifierType a
    // Blessing could carry); and the counter-attack's "+1d6" — a one-attack budget whose die
    // SenhorDaBriga#resolveAttackModifiers reports. Malícia de Valentão: RDS 2 for the same Rodada, a
    // DAMAGE_REDUCTION Blessing (RDS is RD).
    // "recebem Contra-atacante como um Efeito Crítico adicional" is real (0.0.49): while the window
    // is open, SenhorDaBriga#resolveAdditionalDefensiveCriticalEffects adds CONTRA_ATACANTE.
    // TODO the counter-attack itself is offered by the caller — this core has no "Defesa failed"
    // trigger, so it cannot tell when "sempre que falhar em uma rolagem de Defesas" has happened.
    CRUZ_DE_SANGUE(
            "Cruz de Sangue só pode ser ativada quando você for o alvo único de um ataque corpo-a-corpo " +
            "e apenas enquanto o alvo estiver dentro de seu alcance de ataque. Por 1 Rodada você sofre " +
            "redutor em suas Defesas igual à metade de sua Destreza, então aumenta sua Margem Crítica " +
            "Menor de Defesas e Ataque Corpo-a-Corpo com Armas naturais em +2. Suas rolagens de Defesas " +
            "recebem Contra-atacante como um Efeito Crítico adicional, sempre que falhar em uma rolagem " +
            "de Defesas para resistir a um ataque que cumpra os requisitos de Cruz de Sangue você poderá " +
            "desferir um contra-ataque contra o agressor, os danos deste ataque aumentam em +1d6. " +
            "Malícia de Valentão: Você recebe RDS2 para enquanto Cruz de Sangue estiver ativa.",
            false, fixed(2), ActionCost.REACTION, Optional.of(CruzDeSangueInteraction.class),
            Optional.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), 0) {
        @Override
        public ReactionTrigger getReactionTrigger() {
            return ReactionTrigger.SELF_TARGETED_BY_MELEE_ATTACK;
        }

        /**
         * "quando você for o alvo único de um ataque corpo-a-corpo e apenas enquanto o alvo estiver
         * dentro de seu alcance de ataque". The reach is read off the reactor's own SceneContext as
         * adjacency — the band every Arma Natural reaches, since none is authored with a longer
         * Alcance. A {@code null} context, attacker or Perícia is "cannot tell", which refuses.
         */
        @Override
        public boolean isReactionAvailable(final ReactionContext context) {
            CombatantSheet attacker = context.getAttacker();
            SceneContext reactorContext = context.getReactorContext();
            if (attacker == null || reactorContext == null || !context.isSoleTarget()
                    || context.getAttackSkill() != SkillType.ATAQUE_CORPO_A_CORPO) {
                return false;
            }
            Range distance = reactorContext.getDistanceTo(attacker);
            return distance != null && distance.isWithin(Range.ADJACENTE);
        }
    },

    // Requer Especialização Fantasma do Ringue. Real through FingirFraquezasInteraction:
    // "Variável" is PDCost.variable(1), and the amount must equal 1 + the enemies at Distância Muito
    // Curta ("o Custo de Ativação desta Habilidade é igual a este valor"); it is refused while any
    // non-natural weapon is drawn. Grants a DEFESAS Blessing of that value for 2 Rodadas (the
    // table's ruling, 2026-09-22 — the clause names no Duração of its own), and casts a 2-Rodada
    // sheet.ForcedTargeting on each of those enemies through CombatantSheet#applyEnchantment ("este é
    // um efeito de Encantamento"), marking each with CombatantSheet#markAffectedUntilRest so none can
    // be caught again "até passarem por um Descanso". Malícia de Valentão: Vantagem on attack and
    // dano rolls against a target bound to this holder (SenhorDaBriga#resolveAttackRollBonus /
    // #resolveDamageRollBonus read the target's own getForcedTargeting()).
    // TODO "Inimigos inteligentes": nothing classifies a creature's intelligence, so every enemy in
    // range is caught — a caller wanting the distinction passes a SceneContext without the beasts.
    FINGIR_FRAQUEZAS(
            "Esta Habilidade pode ser ativa apenas se você estiver desarmado (exceto armas naturais). Os " +
            "inimigos acreditam que, por estar desarmado, você também está desguarnecido e indefeso, este " +
            "é um efeito de Encantamento. Você recebe Bônus de 1+ Número de Inimigos em Distância Muito " +
            "Curta em suas Defesas, o Custo de Ativação desta Habilidade é igual a este valor. Inimigos " +
            "inteligentes afetados por Fingir Fraquezas vão sempre desferir o primeiro ataque das próximas " +
            "2 Rodadas contra você, após este período os alvos não poderão ser afetados por esta " +
            "Habilidade novamente até passarem por um Descanso. Malícia de Valentão: Você recebe " +
            "Vantagem em suas rolagens de Ataque e Danos contra alvos encantados por Fingir Fraquezas.",
            false, variable(1), ActionCost.ofActionPoints(1), Optional.of(FingirFraquezasInteraction.class),
            Optional.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), 0),

    // Requer Especialização Fantasma do Ringue. Passive.
    // TODO the whole clause needs occupancy, which this core does not model: trespassing enemy-held
    // spaces as Terreno Difícil (TerrainType is Scene-wide, never per-hex), sharing a space with a
    // larger character, and the per-Categoria Defesas bonus that sharing grants. Malícia de
    // Valentão's upgrade is blocked on the same.
    ENTRE_AS_PERNAS(
            "Você pode trespassar por espaços ocupados por inimigos, estes espaços contam como Terrenos " +
            "Difíceis. Você pode permanecer em um mesmo espaço ocupado por inimigo se a Categoria de " +
            "Tamanho dele for superior à sua. Você recebe Bônus de +1 em Defesas para cada ponto de " +
            "Categoria de Tamanho que o adversário tiver além do seu. Malícia de Valentão: Você pode " +
            "ocupar o mesmo espaço que qualquer outro personagem, independentemente de qual a Categoria " +
            "de Tamanho dele, enquanto o fizer sua Margem Crítica Menor Defensiva aumenta em +2.",
            false, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), 0),

    // Requer 2 Habilidades de Fantasma do Ringue — the Especialização requirement is an inference
    // from the class-level statement, as for PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS.
    // Passive: a flag, read through SenhorDaBriga#holdsMaliciaDeValentao by each Habilidade above.
    MALICIA_DE_VALENTAO(
            "Suas Habilidades de Fantasma do Ringue recebem o efeito adicional Malícia de Valentão.",
            true, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
