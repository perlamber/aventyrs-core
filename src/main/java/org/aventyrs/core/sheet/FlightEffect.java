package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.services.HitPointsServiceImpl;

/**
 * The countdown behind an <b>Efeito de Voo</b> bought in a stressful Scene ({@code
 * FlightService#takeOff}). Like {@link FormEffect} it holds no bonus: being airborne lives on
 * {@link CombatantSheet#isFlying()}, and this exists so that something lapses and lands its holder.
 *
 * <p><b>Every flight ends in a safe landing</b> (table ruling, 2026-10-16) — Pixie's and the Aviano's
 * "quando encerrada a duração retornam ao solo lentamente, de forma segura", extended to every source:
 * {@link CombatantSheet#tickTemporaryEffects()} calls {@code setFlying(false)} as this expires, and no
 * Dano de Queda is dealt. Landing early ({@code setFlying(false)}) removes it.
 *
 * <p>{@code remainingRounds} is {@code null} for an unlimited flight ({@code
 * AvianoFeat#ETERNO_VIAJANTE}'s "enquanto estiver consciente … sem limites de Duração"), which still
 * lands its holder the Rodada they fall unconscious (Desacordado, or Caído at 0 PV or worse).
 *
 * <p><b>Not cumulative</b> — one body, one flight.
 */
@Getter
public class FlightEffect extends TemporaryEffect {

    private final String source;

    public FlightEffect(final String source, final Integer remainingRounds) {
        super(remainingRounds);
        this.source = source;
    }

    @Override
    boolean isCumulative() {
        return false;
    }

    /** An unlimited flight holds only "enquanto estiver consciente". */
    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        if (getRemainingRounds() == null && !isConscious(sheet)) {
            sheet.setFlying(false);
        }
    }

    private static boolean isConscious(final CombatantSheet sheet) {
        if (sheet.hasCondition(ConditionType.DESACORDADO, null)) {
            return false;
        }
        CharacterStatus status = new HitPointsServiceImpl().getStatus(sheet);
        return status.ordinal() < CharacterStatus.FALLEN.ordinal();
    }
}
