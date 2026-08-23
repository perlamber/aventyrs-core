package org.aventyrs.core.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.UUID;

/**
 * A CombatantSheet paired with the Iniciativa value it rolled for a Scene — Iniciativa's Ego
 * total plus permanent modifiers (see {@link org.aventyrs.core.character.services.InitiativeService
 * #getTotalInitiative}) plus whatever dice roll the caller applied; this project doesn't
 * compute that roll itself, so the combined total is supplied already-resolved — plus the
 * sub-group it belongs to within the Scene, used to resolve who counts as an ally (see
 * {@link Scene#getAllies}).
 *
 * <p>{@link #initiativeValue} itself never changes once set — what changes mid-Scene is a
 * Round-scoped {@code TemporaryBonus} targeting {@code ModifierType.INITIATIVE}, granted
 * directly to {@link #combatantSheet} the same way any other temporary bonus already is (e.g.
 * a bonus from another Character's action, an activated Ability, or a passive with a Round
 * trigger — see {@code CombatantSheet#grantTemporaryBonus}). {@link
 * #getEffectiveInitiativeValue()} is {@link #initiativeValue} plus whatever that currently sums
 * to on {@link #combatantSheet} — resolved fresh from {@link #combatantSheet}'s own state every
 * time it's called, not cached here or pushed in by whoever granted the bonus. This is
 * deliberate: {@link CombatantSheet} has no reference back to a {@link Scene} to notify it
 * through, and doesn't need one — a {@link Scene} already holds a reference to every
 * CombatantSheet it's tracking (right here, via {@link #combatantSheet}) and can just ask each
 * one directly whenever it needs their current standing, the same "recompute on demand from
 * data already in hand" shape {@link Scene#wonInitiative}/{@link Scene#buildContext} use.
 */
@Getter
@AllArgsConstructor
public class InitiativeEntry {
    private final CombatantSheet combatantSheet;
    private final int initiativeValue;
    private final UUID group;

    /**
     * {@link #initiativeValue} plus whatever {@link ModifierType#INITIATIVE} currently sums to
     * on {@link #combatantSheet} (see {@code CombatantSheet#getTemporaryBonus}) — this
     * CombatantSheet's actual current Iniciativa standing, including any Round-scoped bonus
     * still active right now. Used for point-in-time facts like {@link Scene#wonInitiative}; a
     * {@link Scene}'s own turn *order* only re-reads this at a Round boundary (see {@link
     * Scene#next()}), so it never reshuffles the Round currently in progress even though this
     * value itself can drift at any moment.
     */
    public int getEffectiveInitiativeValue() {
        return initiativeValue + combatantSheet.getTemporaryBonus(ModifierType.INITIATIVE);
    }
}
