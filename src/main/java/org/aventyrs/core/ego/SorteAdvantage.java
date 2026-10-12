package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Vantagem de Sorte chosen once at character creation — available only to characters
 * whose Sorte base reached {@value
 * org.aventyrs.core.character.services.CharacterCreationService#EGO_ADVANTAGE_MIN_BASE}
 * through the creation-time point distribution (see {@link
 * org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}).
 * Reaching that base any other way (Talentos, Títulos Aventyrs, other Habilidades) never
 * grants access to this choice, and it is never lost if Sorte later drops below it.
 */
@Getter
@AllArgsConstructor
public enum SorteAdvantage implements EgoAdvantage {

    /**
     * {@link #resolveCriticalMarginIncrease} is real, tested data, and — since {@link
     * EgoAdvantage} carries this hook now (summed generically alongside {@code
     * AttributeAbility}/{@code SkillCompetencyAbility}'s own identical methods) — actually
     * consulted: {@code AbstractSkillInteraction} sums it into {@code
     * org.aventyrs.core.skill.SkillRoll#getCriticalResult(int)}'s margin parameter for every
     * roll. See {@code org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA} for
     * the other real source of this same widening.
     */
    ACE("Em Cenas de Combate Sua Margem Crítica Menor em rolagens de Perícia de Ataque é " +
            "aumentada em +1 número. Em Cenas não combativas sua Margem Crítica Menor é " +
            "aumentada em +3 números para rolagens de Perícias que não sejam de Ataque.") {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
            if (sceneContext == null) {
                return 0;
            }
            if (sceneContext.isCombatScene() && skillType.isAttackSkill()) {
                return COMBAT_ATTACK_MARGIN_BONUS;
            }
            if (!sceneContext.isCombatScene() && !skillType.isAttackSkill()) {
                return NON_COMBAT_NON_ATTACK_MARGIN_BONUS;
            }
            return 0;
        }
    },

    /**
     * Fully wired (core 0.1.6.4): {@link #resolveEgoSpendMove} grants a {@link GrantedMove} of {@value
     * #MOVE_DISTANCE}UD that ignores Terreno Difícil and provokes no Reações, reported by {@code
     * EgoPointsService#getSpendMoves} for any deliberate Ponto de Sorte use. Either pool counts — "utilizar um Ponto
     * de Sorte" doesn't distinguish permanent from temporary — so this doesn't inspect {@code
     * EgoPointSpend#getType()}, unlike {@code AutocontroleAdvantage#DETERMINACAO_HEROICA}.
     *
     * <p>One move of 2UD in total, its own movement rather than a Movimento Base bump: it claims no movement of the
     * Rodada and costs no Ponto de Ação. The caller paths it over {@code StepRules#ignoringDifficultTerrainCost()}
     * and asks no reactors. <i>(Before 0.1.6.4 it was a 1-Rodada {@code MOVEMENT} bonus, worth 2UD on every Ponto de
     * Ação spent moving that Rodada, and neither qualifier had anything to apply to.)</i>
     */
    AS_NA_MANGA("Imediatamente após utilizar um Ponto de Sorte você pode se mover até 2UD, " +
            "este movimento não provoca Reações e ignora terrenos difíceis.") {
        @Override
        public Optional<GrantedMove> resolveEgoSpendMove(final EgoPointSpend spend) {
            if (spend.getValue() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new GrantedMove(MOVE_DISTANCE, true, false, name()));
        }
    },

    // The amount is real, tested data — resolveExtraSessionEgoRecovery below, read by
    // EgoPointsService#getExtraSessionRecovery and applied by #applySessionRecovery.
    // The trigger is deliberately outside this core: a Narrador ends a session by pressing a
    // button, which the consumer routes to EgoPointsService#applySessionRecovery(Map). No core
    // boundary exists by design. Same shape as AutocontroleAdvantage#MOTIVACAO_DE_MOSES's own
    // "por sessão de jogo" clause.
    DILETO_DE_TYKHE("Você recupera 1 ponto de Sorte temporário adicional por sessão de jogo.") {
        @Override
        public int resolveExtraSessionEgoRecovery() {
            return EXTRA_SESSION_EGO_RECOVERY;
        }
    };

    /** ACE's own Margem Crítica Menor bonus for a Perícia de Ataque during a Cena de Combate. */
    private static final int COMBAT_ATTACK_MARGIN_BONUS = 1;

    /** ACE's own Margem Crítica Menor bonus for a non-Ataque Perícia outside a Cena de Combate. */
    private static final int NON_COMBAT_NON_ATTACK_MARGIN_BONUS = 3;

    /** AS_NA_MANGA's own move, in UD, off a deliberate Ponto de Sorte spend. */
    private static final int MOVE_DISTANCE = 2;

    /** DILETO_DE_TYKHE's own extra temporary Sorte point per game session. */
    private static final int EXTRA_SESSION_EGO_RECOVERY = 1;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.SORTE;
    }
}
