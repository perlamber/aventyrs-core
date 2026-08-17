package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;

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

    // TODO: grants 2UD of movement, ignoring Reações and terreno difícil, immediately after
    // spending a Ponto de Sorte — blocked on three separate missing pieces: (1) no
    // "triggered the moment a resource is spent" hook exists anywhere in this codebase,
    // CharacterSheet#spendTemporaryEgoPoints is a plain mutator with nothing observing it
    // (see CLAUDE.md's Iniciativa section for why an observer/event mechanism was
    // deliberately rejected elsewhere in this codebase — the same reasoning applies here);
    // (2) no movement-triggers-Reação suppression mechanism exists, the identical gap
    // InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO's own Reação-suppression half is still
    // TODO'd on; (3) TerrainType models only what kind of place a whole Scene is in, not a
    // per-square/per-movement "terreno difícil" concept to ignore.
    AS_NA_MANGA("Imediatamente após utilizar um Ponto de Sorte você pode se mover até 2UD, " +
            "este movimento não provoca Reações e ignora terrenos difíceis."),

    // TODO: grants +1 additional temporary Sorte point recovered per game session — no
    // game-session tracking system exists yet, the identical gap
    // AutocontroleAdvantage#MOTIVACAO_DE_MOSES's own "por sessão de jogo" clause is still
    // TODO'd on.
    DILETO_DE_TYKHE("Você recupera 1 ponto de Sorte temporário adicional por sessão de jogo.");

    /** ACE's own Margem Crítica Menor bonus for a Perícia de Ataque during a Cena de Combate. */
    private static final int COMBAT_ATTACK_MARGIN_BONUS = 1;

    /** ACE's own Margem Crítica Menor bonus for a non-Ataque Perícia outside a Cena de Combate. */
    private static final int NON_COMBAT_NON_ATTACK_MARGIN_BONUS = 3;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.SORTE;
    }
}
