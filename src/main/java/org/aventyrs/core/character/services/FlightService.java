package org.aventyrs.core.character.services;

import org.aventyrs.core.character.FlightActivation;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * Taking off as an <b>Efeito de Voo</b> (core 0.1.7.8) — the timed flight every winged source prices
 * for "situações estressantes, como as Cenas de Combate": Asas, Asas de Dragão, Pixie, the Aviano's
 * Braços Alados and the Bestial's Herança Aviana, adjusted by Benção de Flora, Coração Alado and
 * Eterno Viajante.
 *
 * <p><b>Stressful = a Cena de Combate</b> ({@link SceneContext#isCombatScene()}). Outside one, taking
 * off is free and has no Duração, exactly as {@link CombatantSheet#setFlying} always was. In one, a
 * holder whose only wings are priced must buy a flight: {@link #takeOff} spends its PD/PM, puts the
 * holder in the air and applies a {@link org.aventyrs.core.sheet.FlightEffect} that lands them —
 * safely, whatever the source (table ruling, 2026-10-16) — as its Duração lapses. Flight that costs
 * nothing (a Forma's, a timed Blessing's, a Habilidade that {@code keepsFlying()}) never needs one.
 *
 * <p>Like every action price here, the PA/Ação Livre is <b>checked and reported, not deducted</b>;
 * this core never rolls the Duração either — the caller rolls {@code duration().count()} d6.
 */
public interface FlightService {

    /** Every Efeito de Voo the holder could start, each with its price and Duração resolved for them now. */
    List<FlightActivation> getFlightActivations(CombatantSheet sheet);

    /** Whether the Scene is a stressful one, where a priced flight must be bought. */
    boolean isStressful(SceneContext sceneContext);

    /**
     * Whether taking off right now must go through {@link #takeOff} — a stressful Scene, the holder on
     * the ground, and wings that all carry a price.
     */
    boolean requiresActivation(CombatantSheet sheet, SceneContext sceneContext);

    /**
     * Takes off at no cost — outside a stressful Scene, or with wings that carry no price.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code CANNOT_FLY} for a holder with no
     *         Voo at all, {@code FLIGHT_ACTIVATION_REQUIRED} when {@link #requiresActivation} says so
     */
    void takeOffFreely(CombatantSheet sheet, SceneContext sceneContext);

    /**
     * Buys activation: checks it is one {@link #getFlightActivations} offers, the Tempo de Ativação is
     * affordable and the PD/PM are there; spends the PD/PM, takes off and starts the countdown.
     *
     * @param durationFaces the {@code duration().count()} d6 the caller rolled; ignored for an unlimited one
     * @return the Duração in Rodadas, or {@code null} for an unlimited flight
     */
    Integer takeOff(CombatantSheet sheet, FlightActivation activation, List<Integer> durationFaces,
                    int turnNumber);
}
