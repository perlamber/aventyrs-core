package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Riding;

/**
 * Montar, desmontar and what a mount spends — the {@code CavalariaFeat} tree's shared ground. The
 * riding state itself is {@link Riding} on the rider's sheet; what it changes on a roll is read there
 * ({@code CombatantSheet#isSkillUsePrevented}, {@code DirigirECavalgarCompetencyAbility#GINETE}).
 *
 * <p><b>PA are reported, not deducted</b>, as everywhere in this core; positions stay the caller's.
 */
public interface MountService {

    /**
     * What montar or desmontar costs without a Talento — ⚠️ an inference, like {@code
     * WeaponDrawService#DEFAULT_DRAW_COST}: no rules text states it, and {@code
     * CavalariaFeat#MONTAR_E_DESMONTAR_INSTANTANEO} making it an Ação Livre implies it is not one.
     */
    ActionCost DEFAULT_MOUNT_COST = ActionCost.ofActionPoints(1);

    /** Montaria de Combate: "Os movimentos com sua montaria consomem apenas 2PA delas". */
    int COMBAT_MOUNT_MOVEMENT_COST = 2;

    /**
     * Mounts rider on riding and returns the action it cost: an Ação Livre when {@code
     * CavalariaFeat#MONTAR_E_DESMONTAR_INSTANTANEO} is held and unused this Turn, else {@link
     * #DEFAULT_MOUNT_COST}.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code ALREADY_RIDING}
     */
    ActionCost mount(CombatantSheet rider, Riding riding);

    /**
     * Dismounts rider. asReaction is the Talento's "Você também pode Desmontar de sua Montaria como
     * Reação", which spends a Reação ({@code CombatantSheet#spendReaction}).
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NOT_RIDING}, or {@code
     *         MOUNT_REACTION_NOT_PERMITTED} for a Reação without the Talento (or with it already used)
     */
    ActionCost dismount(CombatantSheet rider, boolean asReaction);

    /**
     * The mount's Pontos de Ação a movement with it consumes: {@value #COMBAT_MOUNT_MOVEMENT_COST} with
     * {@code CavalariaFeat#MONTARIA_DE_COMBATE}, else all of them (⚠️ read from "consomem apenas 2PA").
     * 0 when rider is not riding a mount with a sheet.
     */
    int getMountMovementActionPointCost(CombatantSheet rider, int turnNumber);

    /**
     * How many other (non-movement) actions the mount may take this Turn — Montaria de Combate's
     * "igual à quantidade de Títulos Aventyr Despertos que você possuir, mas sempre limitados a
     * quantidade de Pontos de Ação da montaria" left after its movement. 0 without the Talento.
     */
    int getMountActionAllowance(CombatantSheet rider, int turnNumber);

    /**
     * {@code CavalariaFeat#DIRECAO_CAOTICA}: whether rider may interrupt the mount's movement for one
     * action and then continue it. A permission — the movement is the caller's to split.
     */
    boolean mayInterruptMountMovement(CombatantSheet rider);
}
