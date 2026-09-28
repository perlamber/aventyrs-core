package org.aventyrs.core.character.services;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Olhar de Lacerto — the Górgona Característica "as Górgonas utilizam 2PA e 1PD para enfraquecer ou
 * petrificar seus alvos", made with {@link org.aventyrs.core.race.OlharDeLacerto} as its attack
 * source.
 *
 * <p><b>Caller-driven, in two steps around an ordinary attack</b>: {@link #declare} pays the PD and
 * reports what the gaze is — its PA, reach, dano and whether it petrifies — and the caller then
 * delivers a {@code DeliveredAttack} with that source against the target's DM; on a hit, {@link
 * #applyHit} lands the -2PA and, in Forma Monstruosa, the petrification. The dice are the caller's to
 * throw, as ever.
 */
public interface OlharDeLacertoService {

    /** "Utilizar 2PA" — reported, like every PA here. */
    int ACTION_POINT_COST = 2;

    /** "E 1PD." */
    int DETERMINATION_POINT_COST = 1;

    /** "Sofre redutor de 2PA por 2 Rodadas." */
    int ACTION_POINT_MALUS = -2;

    int MALUS_ROUNDS = 2;

    /** Olhar Petrificador: "petrificado por 2 Rodadas". */
    int PETRIFICATION_ROUNDS = 2;

    /**
     * One gaze: its PA, its reach ({@code GorgonaFeat#MARCA_DA_MALDICAO} widens it to Curta), its dano
     * as d6s plus metade do Carisma (+1d6 for the Olhar Petrificador), whether it carries the Olhar
     * Petrificador (Forma Monstruosa), and how many níveis that attempt's GD rises — "cada tentativa de
     * petrificar um mesmo alvo tem a GD aumentada em +1 nível", counted within the combat.
     */
    record Gaze(int actionPoints, Range range, int damageDice, int damageBonus, DamageDescriptor damage,
                boolean petrifying, int petrificationDifficultyIncrease) {
    }

    /** Whether sheet has the gaze — a Górgona whose innate racial traits no Forma suppresses. */
    boolean hasGaze(CombatantSheet sheet);

    /**
     * Pays the 1PD and reports the gaze at target.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code OLHAR_DE_LACERTO_NOT_HELD}, or
     *         {@code NOT_ENOUGH_DETERMINATION_POINTS}
     */
    Gaze declare(CombatantSheet gorgona, CombatantSheet target);

    /**
     * Lands a gaze that hit, after its damage was applied: the -2PA for 2 Rodadas (a second gaze
     * renews it rather than stacking), and — for a petrifying gaze — a {@code Petrification} for 2
     * Rodadas, permanent when the target is at 0 PV or below. Petrification is an Encantamento, so an
     * immune target is not petrified. Returns whether the target was petrified.
     */
    boolean applyHit(CombatantSheet gorgona, CombatantSheet target, Gaze gaze);
}
