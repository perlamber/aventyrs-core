package org.aventyrs.core.item;

import java.util.List;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.PowerStoneBinding;

/**
 * Vinculando Pedras do Poder ({@code docs/rules/pedras-do-poder.txt}, V19; table rulings 2026-10-12). Vinculação
 * means socketing a carried stone into a worn Obra-Prima Arma, Armadura or Escudo. It happens <b>only in combat</b>
 * and costs 1PA, or an Ação Livre with Conexão Veloz. Each one:
 * <ul>
 *   <li>spends a Carga;</li>
 *   <li>deals the stone's Danos to the host, mitigated by the host's own item reduction and never to a Regalia;</li>
 *   <li>turns the stone's Base and the host's mode on for its Duração.</li>
 * </ul>
 * Then stone and host both cool down. Combat ending clears every binding and cooldown; spent Cargas stay spent.
 *
 * <p>The PA are <b>reported, never deducted</b>, like every action here.
 */
public interface PowerStoneService {

    /** What a successful {@link #bind} did, for the caller to show and pay. */
    record BindResult(PowerStoneBinding binding, ActionCost cost, int chargesSpent, int hostDamage,
                      int bearerDamage) {
    }

    /**
     * The carried stones holder could bind right now: usable (a Carga, or Última Carga's last use), not cooling, not
     * already vinculada, and with at least one {@linkplain #hostsFor host} ready. Empty outside combat — which is the
     * question "list the Vincular action?" asks.
     */
    List<PowerStone> readyStones(CombatantSheet holder);

    /** The worn items stone could be bound to right now: accepting it, not cooling, not already holding a stone. */
    List<Item> hostsFor(CombatantSheet holder, PowerStone stone);

    /** What binding stone costs: 1PA, or an Ação Livre with Conexão Veloz. */
    ActionCost getBindCost(PowerStone stone);

    /**
     * Binds stone to host. overcharge spends 3 Cargas under Mecanismo de Sobrecarga, doubling Duração and
     * Resfriamento.
     *
     * <p>An empty stone with Última Carga makes its last use: 2 Rodadas, Danos to the Equipamento doubled. Solve-Vidas
     * sends the Danos of the stone's first Vinculação each Cena to the bearer instead, locked until a Descanso
     * Verdadeiro. Dissipador de Calor takes 1 Rodada off the Resfriamento on the stone's first use each Cena.
     *
     * <p>Refuses, spending nothing:
     * <ul>
     *   <li>{@code POWER_STONE_NOT_IN_COMBAT} outside combat;</li>
     *   <li>{@code POWER_STONE_NOT_CARRIED} for a stone not in the inventory;</li>
     *   <li>{@code POWER_STONE_HOST_INVALID} for a host that is not worn, does not accept the stone, or already holds
     *       one;</li>
     *   <li>{@code POWER_STONE_COOLING_DOWN} while either is cooling or the stone is already bound;</li>
     *   <li>{@code POWER_STONE_NO_CHARGES} for an empty stone;</li>
     *   <li>{@code POWER_STONE_IMPROVEMENT_REQUIRED} for an overcharge the stone cannot make.</li>
     * </ul>
     */
    BindResult bind(CombatantSheet holder, PowerStone stone, Item host, boolean overcharge);

    /**
     * Removes the stone vinculada to host before its Duração ends. Both cool down, and a Relíquia shortens the host's
     * Resfriamento in proportion to the Duração left. {@code POWER_STONE_NOT_BOUND} when host holds none.
     */
    void unbind(CombatantSheet holder, Item host);

    /**
     * Vitalitas Immanis: spends charges Cargas of stone and heals holder twice that many PV. ⚠️ Read as an Ação Livre,
     * usable in or out of combat. Returns the PV restored. {@code POWER_STONE_IMPROVEMENT_REQUIRED} without the
     * Aprimoramento; {@code POWER_STONE_NO_CHARGES} for more Cargas than it holds.
     */
    int heal(CombatantSheet holder, PowerStone stone, int charges);

    /**
     * Draws charges Cargas of stone as a reserve of PM (Arcanum) or PD (Anima Selvagem), the Aprimoramento named by
     * purpose. ⚠️ Read as mixing freely with the bearer's own PM/PD: the caller pays the rest of the cost itself.
     * Returns the Cargas drawn. Refuses as {@link #heal} does.
     */
    int drawCharges(CombatantSheet holder, PowerStone stone, PowerStoneImprovement purpose, int charges);
}
