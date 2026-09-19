package org.aventyrs.core.scene;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A provoking Aura centred on its holder and registered on a {@link Scene} — the shape of
 * {@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}: every combatant outside the holder's
 * sub-group that comes within {@link #radius} while it lasts becomes <i>bound</i>, must spend
 * its first attack of each Rodada on the holder, and takes {@code Skill#DISADVANTAGE_MALUS} on
 * any later attack that Rodada against someone else. Like {@link ActiveAreaSpellEffect}, it
 * lives on the Scene rather than on one sheet, and counts down at the Rodada boundary.
 *
 * <p>Deliberately concrete: it is the provocation Aura, not a generic one. What an Aura
 * <i>does</i> is not abstracted until a second one exists.
 *
 * <p>Binding is caller-driven ({@link Scene#refreshAura}), since this core does no geometry, and
 * a bound combatant stays bound for the Aura's whole duration even after leaving range. Bound
 * combatants are tracked by sheet id together with the last Rodada each attacked the holder
 * ({@link #NEVER}) — which is everything both halves of the rule read.
 */
@Getter
public class ActiveAura {

    /** The "has not attacked the holder yet" value in {@link #lastRoundAttackedHolder}. */
    static final int NEVER = -1;

    private final CombatantSheet holder;
    private final AventyrTitleAbility source;
    private final Range radius;
    private int remainingRounds;

    @Getter(lombok.AccessLevel.NONE)
    private final Map<UUID, Integer> lastRoundAttackedHolder = new LinkedHashMap<>();

    public ActiveAura(@NonNull final CombatantSheet holder, @NonNull final AventyrTitleAbility source,
                      @NonNull final Range radius, final int durationInRounds) {
        if (durationInRounds < 1) {
            throw new IllegalArgumentException("An Aura must last at least one Rodada: " + durationInRounds);
        }
        this.holder = holder;
        this.source = source;
        this.radius = radius;
        this.remainingRounds = durationInRounds;
    }

    /** Whether the combatant with this sheet id is bound by this Aura. */
    public boolean isBound(final UUID combatantId) {
        return lastRoundAttackedHolder.containsKey(combatantId);
    }

    /** Whether combatant is this Aura's holder, matched by sheet id. */
    public boolean isHeldBy(final CombatantSheet combatant) {
        return holder.getId().equals(combatant.getId());
    }

    /** Whether the bound combatant has already attacked the holder in round. */
    boolean hasAttackedHolderIn(final UUID combatantId, final int round) {
        return lastRoundAttackedHolder.getOrDefault(combatantId, NEVER) == round;
    }

    /** Binds the combatant with this sheet id; a no-op if already bound. */
    void bind(final UUID combatantId) {
        lastRoundAttackedHolder.putIfAbsent(combatantId, NEVER);
    }

    /** Notes that a bound attacker attacked the holder in round; any other attack is ignored. */
    void recordAttack(final UUID attackerId, final UUID defenderId, final int round) {
        if (isBound(attackerId) && holder.getId().equals(defenderId)) {
            lastRoundAttackedHolder.put(attackerId, round);
        }
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
