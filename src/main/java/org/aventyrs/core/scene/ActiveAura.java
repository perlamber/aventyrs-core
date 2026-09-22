package org.aventyrs.core.scene;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.title.AventyrTitleAbility;

/**
 * A provoking Aura centred on its holder and registered on a {@link Scene} — the shape of
 * {@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}. It is an <b>emitter</b>: while it lasts, a
 * combatant outside the holder's sub-group who comes within {@link #radius} is caught, and what
 * they are handed is an Encantamento of their own ({@code sheet.ForcedTargeting}) compelling them
 * to spend their first attack of each Rodada on the holder.
 *
 * <p><b>The Aura holds no per-target state.</b> Who is compelled, whether they have paid their
 * attack this Rodada, and how much longer it lasts for them are all facts about the <i>enemy</i>,
 * carried on their own sheet — which is what lets their immunity and their own Duração modifiers
 * decide what landed. This class knows only what it emits and how many it may still catch.
 *
 * <p>Deliberately concrete: it is the provocation Aura, not a generic one. What an Aura
 * <i>does</i> is not abstracted until a second one exists.
 *
 * <p>Catching is caller-driven ({@link Scene#refreshAura}), since this core does no geometry — a
 * foe entering through movement or a teleport is caught on the next refresh. A foe who then leaves
 * range keeps the Encantamento they were given: it is theirs now, and it counts down on their
 * sheet.
 *
 * <p>Two independent lifetimes, and both are real: {@link #remainingRounds} is how long the Aura
 * keeps catching people, while {@link #effectDurationInRounds} is how long each catch lasts for
 * whoever it caught. A foe caught late gets a full Duração rather than the Aura's remainder.
 */
@Getter
public class ActiveAura {

    private final CombatantSheet holder;
    private final AventyrTitleAbility source;
    private final Range radius;

    /** How many combatants this Aura may catch in total — what the PD spent bought. */
    private final int maxTargets;

    /** The Duração each caught foe's own Encantamento starts with. */
    private final int effectDurationInRounds;

    private int boundCount;

    private int remainingRounds;

    public ActiveAura(@NonNull final CombatantSheet holder, @NonNull final AventyrTitleAbility source,
                      @NonNull final Range radius, final int durationInRounds, final int maxTargets) {
        if (durationInRounds < 1) {
            throw new IllegalArgumentException("An Aura must last at least one Rodada: " + durationInRounds);
        }
        if (maxTargets < 1) {
            throw new IllegalArgumentException("An Aura must be able to catch at least one foe: " + maxTargets);
        }
        this.holder = holder;
        this.source = source;
        this.radius = radius;
        this.maxTargets = maxTargets;
        // The two lifetimes are the same figure today — Orgulho Elduriano's one Duração both
        // keeps the Aura catching and sets what each catch is worth. Kept apart because they
        // answer different questions and a foe caught late gets a *full* effect, not the Aura's
        // remainder; a clause stating them separately needs only a second constructor.
        this.effectDurationInRounds = durationInRounds;
        this.remainingRounds = durationInRounds;
    }

    /** Whether this Aura has already caught everyone the PD spent on it paid for. */
    boolean isFull() {
        return boundCount >= maxTargets;
    }

    /** Whether combatant is this Aura's holder, matched by sheet id. */
    public boolean isHeldBy(final CombatantSheet combatant) {
        return holder.getId().equals(combatant.getId());
    }

    /**
     * Notes that one more foe was really caught. Only a catch that <em>landed</em> is counted — an
     * immune foe costs the Aura nothing, since it never took hold of them.
     */
    void recordBinding() {
        boundCount++;
    }

    /** Advances this Aura by one Scene Rodada. */
    void tick() {
        remainingRounds--;
    }

    /** Whether this Aura's Duração has elapsed. */
    boolean isExpired() {
        return remainingRounds <= 0;
    }
}
