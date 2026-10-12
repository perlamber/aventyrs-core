package org.aventyrs.core.combat;

import static org.aventyrs.core.util.TranslatableMessages.AMMUNITION_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_AMMUNITION;
import static org.aventyrs.core.util.TranslatableMessages.PROJECTILE_INCOMPATIBLE;
import static org.aventyrs.core.util.TranslatableMessages.RELOAD_NOT_POSSIBLE;
import static org.aventyrs.core.util.TranslatableMessages.WEAPON_NOT_LOADED;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ProjectileItem;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

public class AmmunitionServiceImpl implements AmmunitionService {

    @Override
    public List<AmmunitionStack> availableFor(final CombatantSheet holder, final Weapon weapon) {
        if (weapon == null || weapon.getAmmunitionKind() == null) {
            return List.of();
        }
        Map<ProjectileItem, Integer> units = new EnumMap<>(ProjectileItem.class);
        for (Item item : holder.getInventory()) {
            ProjectileItem projectile = ProjectileItem.of(item);
            if (projectile != null && projectile.getAmmunitionKind() == weapon.getAmmunitionKind()
                    && item.getQuantity() > 0) {
                units.merge(projectile, item.getQuantity(), Integer::sum);
            }
        }
        return units.entrySet().stream().map(entry -> new AmmunitionStack(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public List<AmmunitionStack> firableFrom(final CombatantSheet holder, final Weapon weapon) {
        if (weapon == null || weapon.getAmmunitionKind() == null) {
            return List.of();
        }
        if (!weapon.requiresReload()) {
            return availableFor(holder, weapon);
        }
        Map<ProjectileItem, Integer> loaded = new LinkedHashMap<>();
        if (weapon instanceof AbstractWeapon copy) {
            copy.getLoadedProjectiles().forEach(projectile -> loaded.merge(projectile, 1, Integer::sum));
        }
        return loaded.entrySet().stream().map(entry -> new AmmunitionStack(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public void requireFirable(final CombatantSheet holder, final Weapon weapon, final ProjectileItem projectile,
                               final int shots) {
        if (weapon == null || weapon.getAmmunitionKind() == null) {
            return;
        }
        if (projectile == null) {
            throw new IllegalOperationException(AMMUNITION_REQUIRED);
        }
        if (projectile.getAmmunitionKind() != weapon.getAmmunitionKind()) {
            throw new IllegalOperationException(PROJECTILE_INCOMPATIBLE);
        }
        if (weapon.requiresReload()) {
            List<ProjectileItem> magazine = weapon instanceof AbstractWeapon copy ? copy.getLoadedProjectiles() : List.of();
            if (magazine.isEmpty() || magazine.get(0) != projectile) {
                throw new IllegalOperationException(WEAPON_NOT_LOADED);
            }
            // Every extra projectile of the shot comes off the magazine too, in order.
            if (magazine.size() < shots || magazine.subList(0, shots).stream().anyMatch(next -> next != projectile)) {
                throw new IllegalOperationException(NOT_ENOUGH_AMMUNITION);
            }
            return;
        }
        int carried = availableFor(holder, weapon).stream()
                .filter(stack -> stack.projectile() == projectile)
                .mapToInt(AmmunitionStack::units)
                .sum();
        if (carried == 0) {
            throw new IllegalOperationException(AMMUNITION_REQUIRED);
        }
        if (carried < shots) {
            throw new IllegalOperationException(NOT_ENOUGH_AMMUNITION);
        }
    }

    @Override
    public void consume(final CombatantSheet holder, final Weapon weapon, final ProjectileItem projectile,
                        final int shots) {
        requireFirable(holder, weapon, projectile, shots);
        if (weapon == null || weapon.getAmmunitionKind() == null || shots <= 0) {
            return;
        }
        if (weapon.requiresReload()) {
            AbstractWeapon copy = (AbstractWeapon) weapon;
            for (int i = 0; i < shots; i++) {
                copy.fireLoaded();
            }
        } else {
            takeFromBag(holder, projectile, shots);
        }
        if (holder.isInCombat()) {
            holder.recordFiredProjectiles(projectile, shots);
        }
    }

    @Override
    public ActionCost getReloadCost(final CombatantSheet holder, final Weapon weapon) {
        if (weapon == null || !weapon.requiresReload()) {
            return null;
        }
        if (!holder.isInCombat()) {
            return ActionCost.NONE;
        }
        // Besta de Repetição: "Tempo de Ação da recarga reduzido para 1PA" — its Favor, while its Requisitos hold.
        if (weapon.getFavoredReloadCost() != null && weapon.grantsFavorTo(holder.getCharacter())) {
            return weapon.getFavoredReloadCost();
        }
        return weapon.getReloadCost();
    }

    @Override
    public ActionCost reload(final CombatantSheet holder, final Weapon weapon, final ProjectileItem projectile) {
        if (!(weapon instanceof AbstractWeapon copy) || !weapon.requiresReload() || projectile == null) {
            throw new IllegalOperationException(RELOAD_NOT_POSSIBLE);
        }
        if (projectile.getAmmunitionKind() != weapon.getAmmunitionKind()) {
            throw new IllegalOperationException(PROJECTILE_INCOMPATIBLE);
        }
        int room = weapon.getEffectiveMagazineCapacity() - copy.getLoadedProjectiles().size();
        if (room <= 0) {
            throw new IllegalOperationException(RELOAD_NOT_POSSIBLE);
        }
        ActionCost cost = getReloadCost(holder, weapon);
        int taken = takeFromBag(holder, projectile, room);
        if (taken == 0) {
            throw new IllegalOperationException(RELOAD_NOT_POSSIBLE);
        }
        for (int i = 0; i < taken; i++) {
            copy.load(projectile);
        }
        return cost;
    }

    /** Takes up to units of projectile out of holder's bag, dropping each emptied stack; returns how many it took. */
    private static int takeFromBag(final CombatantSheet holder, final ProjectileItem projectile, final int units) {
        int taken = 0;
        List<Item> emptied = new ArrayList<>();
        Iterator<Item> bag = holder.getInventory().iterator();
        while (bag.hasNext() && taken < units) {
            Item item = bag.next();
            if (ProjectileItem.of(item) == projectile && item instanceof AbstractItem stack) {
                taken += stack.takeUnits(units - taken);
                if (stack.getQuantity() == 0) {
                    emptied.add(item);
                }
            }
        }
        emptied.forEach(holder::removeFromInventory);
        return taken;
    }
}
