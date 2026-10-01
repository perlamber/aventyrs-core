package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Oprimir — "Este ataque recebe Roubo de Determinação 1, se este ataque for um Acerto Crítico o Roubo de Determinação
 * muda para 2." A Corrente de Efeitos ({@code SobrevivenciaFeat#MESTRE_DE_CACA}, in the chosen terrain).
 *
 * <p>Roubo de Determinação, by table ruling (2026-10-01): the target loses the PD and the attacker recovers what was
 * actually taken — never more than the target still had. The PD mirror of a Roubo de Vida.
 */
public class Oprimir extends AbstractEffect implements EffectChain {

    /** "Roubo de Determinação 1". */
    public static final int STEAL = 1;

    /** "… muda para 2" on an Acerto Crítico. */
    public static final int CRITICAL_STEAL = 2;

    private static final DeterminationPointsService DETERMINATION = new DeterminationPointsServiceImpl();

    private final CombatantSheet attacker;
    private final boolean critical;

    /**
     * @param attacker who recovers the PD taken
     * @param critical whether the attack carrying it was an Acerto Crítico
     */
    public Oprimir(@NonNull final CombatantSheet attacker, final boolean critical) {
        this.attacker = attacker;
        this.critical = critical;
    }

    public boolean isCritical() {
        return critical;
    }

    /** How much PD it steals — 2 on an Acerto Crítico, 1 otherwise. */
    public int steal() {
        return critical ? CRITICAL_STEAL : STEAL;
    }

    @Override
    public String getDescription() {
        return "Este ataque recebe Roubo de Determinação 1, se este ataque for um Acerto Crítico o Roubo de "
                + "Determinação muda para 2.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int taken = Math.min(steal(),
                Math.max(0, DETERMINATION.getCurrentDeterminationPoints(target.getCharacter(), target)));
        if (taken > 0) {
            target.spendDeterminationPoints(taken);
            attacker.recoverDeterminationPoints(taken);
        }
        return reportChain(InteractionResult.builder()
                .resourceLossValue(taken)
                .resourceLossType(ResourceType.DETERMINATION_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
