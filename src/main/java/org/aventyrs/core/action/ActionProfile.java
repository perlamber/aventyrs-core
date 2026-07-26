package org.aventyrs.core.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Perfil de Ação chosen once at character creation and never changed afterwards. It
 * shapes how a character's Pontos de Ação (PA) and free actions behave from Turn to Turn.
 * turnNumber is 0-based: 0 is the character's first Turn/Round.
 */
@Getter
@AllArgsConstructor
public enum ActionProfile {

    // TODO: first movement of each Turn (its first 2UD) and every movement on odd Turns
    // don't provoke Reações — no Reação/movement-tracking system exists yet.
    CONSCIENCIA_DEFENSIVA("Em seu primeiro movimento de cada Rodada, nos 2 primeiros UD " +
            "percorridos, você não provoca Reações. Em Rodadas ímpares nenhum de seus " +
            "movimentos provoca Reações."),

    // TODO: forbids Ações Livres on the character's first Turn and grants an extra Ação
    // Livre from the second Turn onward — no Ação Livre system exists yet.
    MOVIMENTO_PLANEJADO("Você não pode fazer Ações Livres em seu primeiro Turno; a partir " +
            "do segundo Turno você pode fazer uma Ação Livre adicional."),

    // TODO: grants an extra Reação every Turn — no Reação system exists yet.
    REFLEXOS_RAPIDOS("A cada Rodada você pode realizar uma Reação adicional."),

    IMPULSIVO("+2PA na primeira Rodada, -1PA na terceira e quarta Rodada.") {
        @Override
        public int adjustActionPoints(final int actionPoints, final int turnNumber) {
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
        public int adjustActionPoints(final int actionPoints, final int turnNumber) {
            if (turnNumber == 0) {
                return 0;
            }
            if (turnNumber >= 2) {
                return actionPoints + 1;
            }
            return actionPoints;
        }
    },

    // TODO: the -1PA in Cenas de Combate needs a Scene/CombatScene concept that doesn't
    // exist yet; the extra Ação Livre and Reação need those systems too.
    ESTRATEGISTA("Em cenas de Combate seus Pontos de Ação são reduzidos em -1, mas você " +
            "pode fazer uma Ação Livre e uma Reação adicional a cada Rodada.");

    private final String description;

    /**
     * Applies this profile's Turn-dependent adjustment on top of the character's already
     * modifier-resolved Action Points. The default is a no-op; profiles with a concrete
     * Turn-numbered PA effect (Impulsivo, Calculista) override it.
     */
    public int adjustActionPoints(final int actionPoints, final int turnNumber) {
        return actionPoints;
    }

    /**
     * Applies this profile's Turn-dependent adjustment on top of the already
     * modifier-resolved cost of a Perícia roll. The default is a no-op; none of the
     * current profiles vary a roll's cost by Turn, but the hook exists for one that does.
     */
    public int adjustSkillRollCost(final int skillRollCost, final int turnNumber) {
        return skillRollCost;
    }
}
