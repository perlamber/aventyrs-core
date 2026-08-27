package org.aventyrs.core.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.scene.SceneContext;

/**
 * The Perfil de Ação chosen once at character creation and never changed afterwards. It
 * shapes how a character's Pontos de Ação (PA), Reações and Ações Livres behave from Turn to
 * Turn. turnNumber is 0-based: 0 is the character's first Turn/Round.
 *
 * <h2>The three adjustment hooks</h2>
 *
 * Each of {@link #adjustActionPoints}/{@link #adjustReactions}/{@link #adjustFreeActions} takes
 * an already-fully-resolved permanent total and returns this profile's Turn-scoped view of it.
 * They're deliberately the <em>last</em> stage of each stat's computation — every ability,
 * excellency and {@code TemporaryBonus} contribution is already summed into the value handed in,
 * so a profile that zeroes a stat (Calculista's first-Round PA, Movimento Planejado's first-Turn
 * Ações Livres) genuinely zeroes it rather than being out-summed by a bonus applied afterwards.
 *
 * <p>Each hook follows this codebase's cascading-overload convention: the shorter form delegates
 * down with a {@code null} {@link SceneContext}, and the longest holds all the real logic — so a
 * constant body always overrides the <em>longest</em> one, even when it ignores the context
 * itself. A {@code null} context reads as "not a Cena de Combate", which is what
 * {@link #ESTRATEGISTA} needs it to mean: outside a Scene, none of its combat-scoped clauses
 * apply.
 */
@Getter
@AllArgsConstructor
public enum ActionProfile {

    // TODO: first movement of each Turn (its first 2UD) and every movement on odd Turns
    // don't provoke Reações — this core has no movement-triggers-Reação mechanism (the same
    // gap EgoAdvantage's POSICIONAMENTO_ESTRATEGICO cites), so this profile's Reação *count*
    // is untouched: it never grants or removes one, it exempts specific movements from
    // provoking them. adjustReactions is the wrong hook for it, not a missing one.
    CONSCIENCIA_DEFENSIVA("Em seu primeiro movimento de cada Rodada, nos 2 primeiros UD " +
            "percorridos, você não provoca Reações. Em Rodadas ímpares nenhum de seus " +
            "movimentos provoca Reações."),

    MOVIMENTO_PLANEJADO("Você não pode fazer Ações Livres em seu primeiro Turno; a partir " +
            "do segundo Turno você pode fazer uma Ação Livre adicional.") {
        /**
         * Exactly 0 on the first Turn — "não pode fazer Ações Livres" is a hard denial, not a
         * -1 malus, so it stands even for a character whose permanent total is 2 or more — and
         * one extra from the second Turn onward.
         */
        @Override
        public int adjustFreeActions(final int freeActions, final int turnNumber, final SceneContext sceneContext) {
            if (turnNumber == 0) {
                return 0;
            }
            return freeActions + 1;
        }
    },

    REFLEXOS_RAPIDOS("A cada Rodada você pode realizar uma Reação adicional.") {
        /** Every Rodada, unconditionally — unlike {@link #ESTRATEGISTA}, no Cena de Combate needed. */
        @Override
        public int adjustReactions(final int reactions, final int turnNumber, final SceneContext sceneContext) {
            return reactions + 1;
        }
    },

    IMPULSIVO("+2PA na primeira Rodada, -1PA na terceira e quarta Rodada.") {
        @Override
        public int adjustActionPoints(final int actionPoints, final int turnNumber, final SceneContext sceneContext) {
            if (turnNumber == 0) {
                return actionPoints + 2;
            }
            if (turnNumber == 2 || turnNumber == 3) {
                return actionPoints - 1;
            }
            return actionPoints;
        }
    },

    CALCULISTA("Você tem 0PA na primeira Rodada, recupera seus pontos na segunda Rodada, " +
            "então recebe +1PA na terceira Rodada até o final da Cena.") {
        @Override
        public int adjustActionPoints(final int actionPoints, final int turnNumber, final SceneContext sceneContext) {
            if (turnNumber == 0) {
                return 0;
            }
            if (turnNumber >= 2) {
                return actionPoints + 1;
            }
            return actionPoints;
        }
    },

    ESTRATEGISTA("Em cenas de Combate seus Pontos de Ação são reduzidos em -1, mas você " +
            "pode fazer uma Ação Livre e uma Reação adicional a cada Rodada.") {
        @Override
        public int adjustActionPoints(final int actionPoints, final int turnNumber, final SceneContext sceneContext) {
            return isCombatScene(sceneContext) ? actionPoints - 1 : actionPoints;
        }

        @Override
        public int adjustReactions(final int reactions, final int turnNumber, final SceneContext sceneContext) {
            return isCombatScene(sceneContext) ? reactions + 1 : reactions;
        }

        @Override
        public int adjustFreeActions(final int freeActions, final int turnNumber, final SceneContext sceneContext) {
            return isCombatScene(sceneContext) ? freeActions + 1 : freeActions;
        }
    };

    private final String description;

    /**
     * Applies this profile's Turn-dependent adjustment on top of the character's already
     * modifier-resolved Action Points, with no Scene in hand — delegates down with a
     * {@code null} {@link SceneContext}.
     */
    public int adjustActionPoints(final int actionPoints, final int turnNumber) {
        return adjustActionPoints(actionPoints, turnNumber, null);
    }

    /**
     * Applies this profile's Turn-dependent adjustment on top of the character's already
     * modifier-resolved Action Points. The default is a no-op; profiles with a concrete PA
     * effect ({@link #IMPULSIVO}, {@link #CALCULISTA}, {@link #ESTRATEGISTA}) override it.
     */
    public int adjustActionPoints(final int actionPoints, final int turnNumber, final SceneContext sceneContext) {
        return actionPoints;
    }

    /** {@link #adjustReactions(int, int, SceneContext)} with no Scene in hand. */
    public int adjustReactions(final int reactions, final int turnNumber) {
        return adjustReactions(reactions, turnNumber, null);
    }

    /**
     * Applies this profile's Turn-dependent adjustment on top of the character's already
     * modifier-resolved Reação count. The default is a no-op; {@link #REFLEXOS_RAPIDOS} and
     * {@link #ESTRATEGISTA} override it.
     */
    public int adjustReactions(final int reactions, final int turnNumber, final SceneContext sceneContext) {
        return reactions;
    }

    /** {@link #adjustFreeActions(int, int, SceneContext)} with no Scene in hand. */
    public int adjustFreeActions(final int freeActions, final int turnNumber) {
        return adjustFreeActions(freeActions, turnNumber, null);
    }

    /**
     * Applies this profile's Turn-dependent adjustment on top of the character's already
     * modifier-resolved Ação Livre count. The default is a no-op; {@link #MOVIMENTO_PLANEJADO}
     * and {@link #ESTRATEGISTA} override it.
     */
    public int adjustFreeActions(final int freeActions, final int turnNumber, final SceneContext sceneContext) {
        return freeActions;
    }

    /**
     * Applies this profile's Turn-dependent adjustment on top of the already
     * modifier-resolved cost of a Perícia roll. The default is a no-op; none of the
     * current profiles vary a roll's cost by Turn, but the hook exists for one that does.
     *
     * <p>Deliberately has no {@link SceneContext} overload, unlike its three siblings above:
     * no profile conditions a roll's cost on the Scene, so widening it would be building for a
     * hypothetical consumer. Add one the day a profile needs it.
     */
    public int adjustSkillRollCost(final int skillRollCost, final int turnNumber) {
        return skillRollCost;
    }

    /**
     * Whether sceneContext describes a Cena de Combate — {@code false} for a {@code null}
     * context, which is what every caller without a Scene in hand passes. Shared by
     * {@link #ESTRATEGISTA}'s three overrides.
     */
    static boolean isCombatScene(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isCombatScene();
    }
}
