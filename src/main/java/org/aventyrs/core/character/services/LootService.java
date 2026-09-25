package org.aventyrs.core.character.services;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Saquear: taking what a defeated foe carries.
 *
 * <p><b>Authored by the table (2026-09-24), not by {@code docs/rules/}</b>, which say nothing about
 * looting. The rule has three parts:
 * <ul>
 *   <li>In combat, on their own Turn, a character may Saquear one defeated foe. It costs
 *       {@link #LOOT_COST} (2PA). Outside combat it costs nothing ({@link ActionCost#NONE}).</li>
 *   <li>Everything the foe <b>carries</b> (its inventory, never its equipment) goes into the
 *       Campanha's shared bag. From there, any participant may claim an item at once and for
 *       free. Equipping it mid-combat is priced separately ({@code
 *       CharacterSheet#getEquipCost}).</li>
 *   <li>When combat ends, whatever the defeated foes still carry goes into the bag as well, for
 *       free. That sweep is the caller's: it does not come through this service, so it has no
 *       Turn to spend.</li>
 * </ul>
 *
 * <p><b>This service gates and prices the action. It moves nothing.</b> The looter's client
 * holds only a stand-in for the foe's sheet, not its real inventory, and the Campanha's bag lives
 * in persistence, not in core. So the caller asks here whether the Saquear is allowed and what it
 * costs, and then has persistence do the move. Like {@link WeaponDrawService}, the Pontos de Ação
 * are <b>reported, not deducted</b>: this core keeps no spent-this-Turn pool.
 *
 * <p><b>"Defeated" is a {@link CharacterStatus}, not a live PV reading.</b> A client knows a
 * remote foe's state only through that foe's broadcast status tier. {@code
 * CombatantSheet#isAtOrBelowZeroHitPoints} would read a stand-in's default PV. See
 * {@link #isDefeated(CharacterStatus)}.
 */
public interface LootService {

    /** What a Saquear costs in combat: 2 Pontos de Ação, as the table ruled. */
    ActionCost LOOT_COST = ActionCost.ofActionPoints(2);

    /**
     * Whether a combatant in this status tier may be looted: PV at zero or below, which is {@code
     * FALLEN}, {@code COMMA} or {@code DEAD} (see {@code HitPointsService#getStatus}). A {@code
     * null} status is unknown and never counts.
     */
    static boolean isDefeated(final CharacterStatus status) {
        return status == CharacterStatus.FALLEN || status == CharacterStatus.COMMA || status == CharacterStatus.DEAD;
    }

    /**
     * What a Saquear costs in context: {@link #LOOT_COST} in a Cena de Combate, {@link
     * ActionCost#NONE} otherwise. A {@code null} context is outside combat.
     */
    ActionCost getLootCost(SceneContext context);

    /**
     * The non-throwing form of {@link #requireLootable}, for a UI deciding whether to offer the
     * action.
     */
    boolean canLoot(SceneContext looterContext, CombatantSheet target, CharacterStatus targetStatus);

    /**
     * Checks that the looter, whose view of the Cena is {@code looterContext}, may Saquear target
     * right now, and returns what it costs (per {@link #getLootCost}).
     *
     * <p>Refuses, in order: a target that is not among the looter's enemies ({@code
     * LOOT_TARGET_NOT_AN_ENEMY}; a fallen ally is carried to safety, not stripped), and one whose
     * {@code targetStatus} is not {@link #isDefeated defeated} ({@code LOOT_TARGET_NOT_DEFEATED}).
     * Whether the target actually carries anything is for persistence to answer, since only it
     * holds the real inventory.
     *
     * @throws IllegalOperationException with the refusal's message key
     */
    ActionCost requireLootable(SceneContext looterContext, CombatantSheet target, CharacterStatus targetStatus)
            throws IllegalOperationException;
}
