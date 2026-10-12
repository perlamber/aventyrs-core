package org.aventyrs.core.item;

import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_COOLING_DOWN;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_HOST_INVALID;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_IMPROVEMENT_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_NOT_BOUND;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_NOT_CARRIED;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_NOT_IN_COMBAT;
import static org.aventyrs.core.util.TranslatableMessages.POWER_STONE_NO_CHARGES;

import java.util.List;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PowerStoneBinding;

public class PowerStoneServiceImpl implements PowerStoneService {

    /** "Vincular tem o Tempo de Ação igual à 1PA". */
    public static final ActionCost BIND_COST = ActionCost.ofActionPoints(1);

    /** Vitalitas Immanis: "recuperando PV igual ao dobro da quantidade de Carga utilizada". */
    public static final int PV_PER_CHARGE = 2;

    @Override
    public List<PowerStone> readyStones(final CombatantSheet holder) {
        if (!holder.isInCombat()) {
            return List.of();
        }
        return holder.getInventory().stream()
                .filter(PowerStone.class::isInstance)
                .map(PowerStone.class::cast)
                .filter(stone -> stone.isUsable() && holder.getRemainingItemCooldown(stone) == 0 && !isBound(holder, stone))
                .filter(stone -> !hostsFor(holder, stone).isEmpty())
                .toList();
    }

    @Override
    public List<Item> hostsFor(final CombatantSheet holder, final PowerStone stone) {
        return holder.getCharacter().getEquipment().stream()
                .filter(item -> item instanceof AbstractItem && item.getPowerStone() == null)
                .filter(item -> item.acceptsPowerStone(stone) && holder.getRemainingItemCooldown(item) == 0)
                .toList();
    }

    @Override
    public ActionCost getBindCost(final PowerStone stone) {
        return stone.bindsAsFreeAction() ? ActionCost.FREE_ACTION : BIND_COST;
    }

    @Override
    public BindResult bind(final CombatantSheet holder, final PowerStone stone, final Item host,
                           final boolean overcharge) {
        if (!holder.isInCombat()) {
            throw new IllegalOperationException(POWER_STONE_NOT_IN_COMBAT);
        }
        if (holder.getInventory().stream().noneMatch(item -> item == stone)) {
            throw new IllegalOperationException(POWER_STONE_NOT_CARRIED);
        }
        if (!(host instanceof AbstractItem copy) || copy.getPowerStone() != null || !copy.acceptsPowerStone(stone)
                || holder.getCharacter().getEquipment().stream().noneMatch(item -> item == host)) {
            throw new IllegalOperationException(POWER_STONE_HOST_INVALID);
        }
        if (isBound(holder, stone) || holder.getRemainingItemCooldown(stone) > 0
                || holder.getRemainingItemCooldown(host) > 0) {
            throw new IllegalOperationException(POWER_STONE_COOLING_DOWN);
        }
        if (!stone.isUsable()) {
            throw new IllegalOperationException(POWER_STONE_NO_CHARGES);
        }
        if (overcharge && !stone.canOvercharge()) {
            throw new IllegalOperationException(POWER_STONE_IMPROVEMENT_REQUIRED);
        }

        boolean lastChance = stone.getRemainingCharges() == 0;
        int duration = lastChance ? PowerStone.LAST_CHANCE_DURATION_ROUNDS : stone.getEffectiveEffectDurationRounds();
        int cooldown = stone.getEffectiveCooldownRounds();
        int damage = stone.getEffectiveBindingDamage();
        if (overcharge) {
            duration *= 2;
            cooldown *= 2;
        }
        if (lastChance) {
            // Última Carga: "os Danos causados aos Equipamentos por esta Pedra do Poder são dobrados".
            damage *= 2;
        }
        boolean firstThisCena = holder.claimFirstPowerStoneUseThisCena(stone);
        if (firstThisCena && stone.getStoneImprovement() == PowerStoneImprovement.DISSIPADOR_DE_CALOR) {
            cooldown = Math.max(0, cooldown - 1);
        }
        int chargesSpent = stone.spendCharges(overcharge ? PowerStone.OVERCHARGE_CHARGES : 1);

        int hostDamage = 0;
        int bearerDamage = 0;
        // "O poder monstruoso é intenso demais para Equipamentos que não sejam Regalias" — a Regalia takes nothing.
        // ⚠️ Solve-Vidas is read as redirecting damage that would land, so a Regalia host costs its bearer nothing too.
        if (!copy.isRegalia()) {
            if (firstThisCena && stone.bindsToBearerInsteadOfItem()) {
                holder.payWithVitality(damage);
                bearerDamage = damage;
            } else {
                hostDamage = copy.applyDamage(damage);
            }
        }

        PowerStoneBinding binding = new PowerStoneBinding(copy, stone, duration, cooldown);
        holder.startPowerStoneBinding(binding);
        return new BindResult(binding, getBindCost(stone), chargesSpent, hostDamage, bearerDamage);
    }

    @Override
    public void unbind(final CombatantSheet holder, final Item host) {
        PowerStoneBinding binding = holder.getPowerStoneBindings().stream()
                .filter(running -> running.getHost() == host)
                .findFirst()
                .orElseThrow(() -> new IllegalOperationException(POWER_STONE_NOT_BOUND));
        holder.endPowerStoneBinding(binding, true);
    }

    @Override
    public int heal(final CombatantSheet holder, final PowerStone stone, final int charges) {
        int drawn = drawCharges(holder, stone, PowerStoneImprovement.VITALITAS_IMMANIS, charges);
        int before = holder.getDamageTaken();
        holder.heal(drawn * PV_PER_CHARGE, new HealingSource(stone, false, holder, null, null));
        return before - holder.getDamageTaken();
    }

    @Override
    public int drawCharges(final CombatantSheet holder, final PowerStone stone, final PowerStoneImprovement purpose,
                           final int charges) {
        if (holder.getInventory().stream().noneMatch(item -> item == stone)) {
            throw new IllegalOperationException(POWER_STONE_NOT_CARRIED);
        }
        if (stone.getStoneImprovement() != purpose) {
            throw new IllegalOperationException(POWER_STONE_IMPROVEMENT_REQUIRED);
        }
        if (charges <= 0 || charges > stone.getRemainingCharges()) {
            throw new IllegalOperationException(POWER_STONE_NO_CHARGES);
        }
        return stone.spendCharges(charges);
    }

    private static boolean isBound(final CombatantSheet holder, final PowerStone stone) {
        return holder.getPowerStoneBindings().stream().anyMatch(binding -> binding.getStone() == stone);
    }
}
