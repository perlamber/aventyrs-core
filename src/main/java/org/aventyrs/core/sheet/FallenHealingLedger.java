package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The bookkeeping behind the fallen-character healing limits — which heal effects a character in
 * Coma has already used, whether that Coma began in this Cena, and how many Rodadas ago they died.
 *
 * <p><b>This is not a stored {@link CharacterStatus}.</b> The status stays derived ({@code
 * HitPointsService#getStatus}), and every reader goes through {@link #observe} with a freshly
 * derived one first. What is stored is <i>when transitions happened</i>, which a derivation from
 * the current PV cannot recover. A transition that happens without any PV changing (a Forma or a
 * Frenesi ending, which move the maximum) is only seen at the next damage, heal or Rodada boundary.
 *
 * <p>Package-private and owned by {@link AbstractCombatantSheet}, which already carries a great
 * deal of state; the sheet exposes only the read-only facts on {@link CombatantSheet}.
 */
class FallenHealingLedger {

    private CharacterStatus observed = CharacterStatus.CLEAN;

    /** Non-repeatable {@link HealingSource#key()}s used during the current Coma. */
    private final Set<Object> comaHealKeys = new HashSet<>();

    private boolean comaEnteredThisScene;

    /** Rodadas since death, or {@code null} while alive or once the window has closed. */
    private Integer roundsSinceDeath;

    private boolean beyondRevival;

    /** Rodadas since PV first reached 0 or below, or {@code null} while above it. */
    private Integer roundsSinceFallen;

    /**
     * Records now as the current status. Entering Coma (from above <i>or</i> from Dead, when a
     * revival lands) opens a fresh Coma with no heal effects used and marks it as begun in this
     * Cena; dying starts the death count at 0.
     */
    void observe(final CharacterStatus now) {
        if (now == CharacterStatus.COMMA) {
            if (observed != CharacterStatus.COMMA) {
                comaHealKeys.clear();
                comaEnteredThisScene = true;
            }
        } else {
            comaHealKeys.clear();
            comaEnteredThisScene = false;
        }
        boolean fallen = now == CharacterStatus.FALLEN || now == CharacterStatus.COMMA || now == CharacterStatus.DEAD;
        if (!fallen) {
            roundsSinceFallen = null;
        } else if (roundsSinceFallen == null) {
            roundsSinceFallen = 0;
        }
        if (now == CharacterStatus.DEAD) {
            if (observed != CharacterStatus.DEAD) {
                roundsSinceDeath = 0;
            }
        } else {
            roundsSinceDeath = null;
        }
        observed = now;
    }

    boolean hasUsedInComa(final Object key) {
        return comaHealKeys.contains(key);
    }

    void recordComaHeal(final Object key) {
        comaHealKeys.add(key);
    }

    boolean hasEnteredComaThisScene() {
        return comaEnteredThisScene;
    }

    OptionalInt getRoundsSinceDeath() {
        return roundsSinceDeath == null ? OptionalInt.empty() : OptionalInt.of(roundsSinceDeath);
    }

    /** One Rodada more since death — called after {@link #observe} at the Rodada boundary. */
    void tickRound() {
        if (roundsSinceDeath != null) {
            roundsSinceDeath++;
        }
        if (roundsSinceFallen != null) {
            roundsSinceFallen++;
        }
    }

    OptionalInt getRoundsSinceFallen() {
        return roundsSinceFallen == null ? OptionalInt.empty() : OptionalInt.of(roundsSinceFallen);
    }

    /**
     * A new Cena: whatever Coma is running no longer began "in this Cena", and a death from an
     * earlier Cena is out of every "morrido em até N Rodadas" window. The used heal keys survive —
     * the Coma cap is a total for as long as the Coma lasts.
     */
    void startNewScene() {
        comaEnteredThisScene = false;
        roundsSinceDeath = null;
    }

    boolean isBeyondRevival() {
        return beyondRevival;
    }

    void markBeyondRevival() {
        beyondRevival = true;
    }
}
