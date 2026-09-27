package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.LacertoFerocity;

import java.util.Optional;

/**
 * Ferocidade de Lacerto — the Indômito Característica a Talento may also grant ({@code
 * GorgonaFeat#ACEITAR_A_SELVAGERIA}), veto ({@code IndomitoFeat#RENEGAR_A_LACERTO}) or mimic
 * ({@code BestialFeat#ACEITAR_A_LACERTO}). The state is a {@link LacertoFerocity} on the sheet;
 * this service decides when it starts and ends.
 *
 * <p><b>Caller-driven, like every trigger here.</b> Nothing enters the state on its own: the
 * caller calls {@link #refresh} for a combatant at the start of their Turn (the {@link SceneContext}
 * says which Rodada it is), and the Ferocidade then lasts until the combat ends — or until its
 * holder spends a temporary Autocontrole point ({@link #decline}).
 */
public interface LacertoFerocityService {

    /** Rodadas from which Aceitar a Lacerto may be used — "a partir da terceira Rodada". */
    int MIMIC_FROM_ROUND = 3;

    /**
     * The combat Rodada from which sheet enters the Ferocidade: the earliest of its Raça's (silenced
     * while a Forma suppresses innate racial traits) and its Talentos'. Empty for a combatant without
     * the Característica.
     */
    Optional<Integer> getFerocityRound(CombatantSheet sheet);

    /**
     * Enters the natural Ferocidade if it is due — a Cena de Combate at or past {@link
     * #getFerocityRound}, not declined this combat, and no held Talento preventing it given
     * holderContext (the holder's own snapshot) — and returns it. Already ferocious: returns the
     * running state unchanged. Otherwise empty. A {@code null} context enters nothing.
     */
    Optional<LacertoFerocity> refresh(CombatantSheet sheet, SceneContext holderContext);

    /**
     * "É possível gastar um ponto temporário de Autocontrole para evitar ou finalizar a Ferocidade":
     * spends the point, ends a running natural Ferocidade and keeps it from starting again this
     * combat. ⚠️ That one point covers the rest of the combat is a reading; the clause does not say
     * how long "evitar" lasts. A mimicked copy is not ended — it was entered by choice.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code LACERTO_FEROCITY_NOT_HELD}
     *         without the Característica; {@code NOT_ENOUGH_EGO_POINTS} with no temporary Autocontrole
     */
    void decline(CombatantSheet sheet);

    /**
     * {@code BestialFeat#ACEITAR_A_LACERTO}: "A partir da terceira Rodada de Cenas de Combate, como
     * uma Ação Livre, você pode gastar temporariamente 1 ponto de Autocontrole e Mimetizar os
     * Efeitos da Ferocidade de Lacerto por Instinto Rodadas … apenas uma vez a cada Cena de
     * Combate." Spends the point and starts a mimicked {@link LacertoFerocity}. The Ação Livre is the
     * caller's to account for.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code LACERTO_FEROCITY_MIMIC_NOT_GRANTED},
     *         {@code LACERTO_FEROCITY_MIMIC_TOO_EARLY}, {@code LACERTO_FEROCITY_MIMIC_ALREADY_USED}, or
     *         {@code NOT_ENOUGH_EGO_POINTS}
     */
    LacertoFerocity mimic(CombatantSheet sheet, SceneContext holderContext);
}
