package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.WEAPON_ALREADY_DRAWN;
import static org.aventyrs.core.util.TranslatableMessages.WEAPON_DRAW_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.WEAPON_NOT_CARRIED;

public class WeaponDrawServiceImpl implements WeaponDrawService {

    /**
     * The cheapest answer across every held Talento — one saying "as an Ação Livre" is enough,
     * and two saying it are not cheaper than one. Talentos sit outside every {@code
     * ModifierResolver} scan, so this is an explicit pass, the same shape {@code
     * DefenseServiceImpl}/{@code MovementServiceImpl} already use for their own {@code Feat} hooks.
     */
    @Override
    public ActionCost getDrawCost(final Character character) {
        boolean free = character.getFeats().stream().anyMatch(feat -> feat.drawsWeaponAsFreeAction(character));
        return free ? ActionCost.FREE_ACTION : DEFAULT_DRAW_COST;
    }

    @Override
    public boolean canDraw(final CombatantSheet sheet, final Weapon weapon) {
        return refusalFor(sheet, weapon) == null;
    }

    @Override
    public ActionCost draw(final CombatantSheet sheet, final Weapon weapon) throws IllegalOperationException {
        String refusal = refusalFor(sheet, weapon);
        if (refusal != null) {
            throw new IllegalOperationException(refusal);
        }
        // Through the sheet, not the Character, so the per-Turn draw marker SAQUE_RAPIDO reads is set.
        sheet.drawWeapon(weapon);
        return getDrawCost(sheet.getCharacter());
    }

    @Override
    public boolean sheathe(final CombatantSheet sheet, final Weapon weapon) {
        return sheet.getCharacter().sheatheWeapon(weapon);
    }

    /**
     * The message key for why this draw would be refused, or {@code null} when it is allowed —
     * one place so {@link #canDraw} and {@link #draw} can never disagree about what is legal.
     */
    private String refusalFor(final CombatantSheet sheet, final Weapon weapon) {
        if (weapon == null || !sheet.getCharacter().getEquipment().contains(weapon)) {
            return WEAPON_NOT_CARRIED;
        }
        if (sheet.getCharacter().isDrawn(weapon)) {
            return WEAPON_ALREADY_DRAWN;
        }
        // Devorado: you are inside something, and can reach neither your sheath nor what you
        // dropped. Checked with no SceneContext — no condition scopes this gate by proximity.
        boolean prevented = sheet.getActiveConditions(null).stream().anyMatch(ConditionType::preventsArming);
        return prevented ? WEAPON_DRAW_PREVENTED : null;
    }
}
