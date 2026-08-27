package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

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
     * The 2UD is real: {@link #resolveEgoSpendBlessings} below grants a {@link
     * ModifierType#MOVEMENT} {@link Blessing} of {@value #MOVEMENT_BONUS}UD, which {@code
     * EgoPointsService#useEgoPointsForEffect} applies to the spender the moment a Ponto de Sorte
     * is deliberately used. Either pool counts — "utilizar um Ponto de Sorte" doesn't
     * distinguish permanent from temporary — so this doesn't inspect {@code
     * EgoPointSpend#getType()}, unlike {@code AutocontroleAdvantage#DETERMINACAO_HEROICA}.
     * Exactly the same granting shape as {@link InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s
     * own +2UD, off a different trigger.
     *
     * <p><strong>Both qualifiers are no-ops in this core today, not omissions.</strong> "Não
     * provoca Reações" would exempt this movement from a movement-triggers-Reação mechanism that
     * does not exist (the identical gap POSICIONAMENTO_ESTRATEGICO's own Reação-suppression half
     * is still TODO'd on), and "ignora terrenos difíceis" would exempt it from a per-movement
     * terreno difícil cost that does not exist either — {@code TerrainType} describes a whole
     * Scene, not a square. Being exempt from nothing costs nothing, so a plain +2UD is presently
     * an exact model of this clause; the two qualifiers become real the day either system lands,
     * and this constant will need revisiting then.
     *
     * <p>The grant lasts {@value #MOVEMENT_ROUNDS} Rodada — the shortest a {@code TemporaryBonus}
     * can express — where the rules text says "imediatamente". This core has no one-shot movement
     * allowance and never executes movement at all ({@code MovementService} computes Movimento
     * Base, it doesn't spend it), so the bonus is added to this Rodada's Movimento Base rather
     * than to a discrete immediate step.
     *
     * <p><strong>That approximation is wider than it looks, and deliberately kept anyway.</strong>
     * Movimento Base is a <em>per-Ponto-de-Ação</em> figure, so a {@code MOVEMENT}
     * {@code TemporaryBonus} of 2 is worth 2UD on every Ponto de Ação its holder spends moving
     * this Rodada, not the single 2UD step the rules text describes — unlike {@link
     * InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}, whose text raises Movimento Base itself
     * and so lands exactly. Expressing "one extra move of 2UD" needs a one-shot movement
     * allowance this core doesn't have, and {@code ModifierType} has no shape for a
     * non-per-point distance grant; a flat 2UD on the only distance stat that exists stays
     * closer to the clause than granting nothing. Revisit this the day a movement-execution
     * concept lands.
     */
    AS_NA_MANGA("Imediatamente após utilizar um Ponto de Sorte você pode se mover até 2UD, " +
            "este movimento não provoca Reações e ignora terrenos difíceis.") {
        @Override
        public List<Blessing> resolveEgoSpendBlessings(final EgoPointSpend spend) {
            if (spend.getValue() <= 0) {
                return List.of();
            }
            return List.of(new Blessing(ModifierType.MOVEMENT, MOVEMENT_BONUS, MOVEMENT_ROUNDS,
                    TargetScope.SELF, name()));
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

    /** AS_NA_MANGA's own movement grant, in UD, off a deliberate Ponto de Sorte spend. */
    private static final int MOVEMENT_BONUS = 2;

    /** How many Rodadas AS_NA_MANGA's movement grant lasts — see that constant's own javadoc. */
    private static final int MOVEMENT_ROUNDS = 1;

    /** DILETO_DE_TYKHE's own extra temporary Sorte point per game session. */
    private static final int EXTRA_SESSION_EGO_RECOVERY = 1;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.SORTE;
    }
}
