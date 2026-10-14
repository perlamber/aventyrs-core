package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.util.DiceRoller;

/**
 * Dano de Asfixia (acoes-de-combate.txt) — "Após Vigor Rodada o personagem sofre 1d6 pontos de Dano Primordial, este
 * dano aumenta em +1d6 a cada instância de dano (máximo 3d6). Na quarta instância de danos adicionalmente aplica
 * Feridas Dolorosas até o próximo efeito de Descanso. Na sexta instância de danos o personagem sofre morte
 * instantânea por asfixia."
 *
 * <p>An instance lands every Vigor Rodadas without air (Vigor 4: the 4th, 8th, … Rodada). The state lives on the sheet
 * ({@code CombatantSheet#getAsphyxia}); the caller decides when breathing stops and starts (submerged, a gas) and
 * ticks it once per Rodada — at the start of the combatant's Turn in combat, or when the Narrador passes a Rodada
 * outside one (table ruling, 2026-10-14). Objects are not affected ("não afeta objetos").
 */
public interface AsphyxiaService {

    /** "máximo 3d6". */
    int MAXIMUM_DICE = 3;
    /** "Na quarta instância … Feridas Dolorosas". */
    int PAINFUL_WOUNDS_INSTANCE = 4;
    /** "Na sexta instância … morte instantânea". */
    int LETHAL_INSTANCE = 6;

    /** Breathing stops; a combatant already without air keeps its count. */
    void stopBreathing(CombatantSheet sheet);

    /** Breathing again — "encerrando assim o ciclo de asfixia". */
    void breathe(CombatantSheet sheet);

    /** Whether sheet is currently without air. */
    boolean isBreathless(CombatantSheet sheet);

    /** How many Rodadas pass between two instances for sheet — its Vigor, at least 1. */
    int getRoundsPerInstance(CombatantSheet sheet);

    /**
     * One more Rodada without air. Lands an instance when the count reaches a multiple of {@link
     * #getRoundsPerInstance}. A combatant breathing is left alone and reported as {@link AsphyxiaTick#NONE}.
     *
     * @param dice rolls the instance's d6s; may be {@code null} on a Rodada that lands nothing
     * @throws IllegalArgumentException if an instance needing dice lands and dice is {@code null}
     */
    AsphyxiaTick tick(CombatantSheet sheet, DiceRoller dice, SceneContext sceneContext);

    /**
     * One Rodada of asphyxia.
     *
     * @param breathlessRounds Rodadas without air after this one
     * @param instance         the instance that landed this Rodada (1-based), 0 when none did
     * @param dice             the d6s that instance rolled
     * @param damage           the Dano Primordial dealt
     * @param painfulWounds    whether this instance applied Feridas Dolorosas
     * @param lethal           whether this instance killed
     */
    record AsphyxiaTick(int breathlessRounds, int instance, int dice, int damage, boolean painfulWounds,
                        boolean lethal) {

        /** Nothing happened — the combatant breathes. */
        public static final AsphyxiaTick NONE = new AsphyxiaTick(0, 0, 0, 0, false, false);

        /** Whether an instance landed this Rodada. */
        public boolean landed() {
            return instance > 0;
        }
    }
}
