package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Rugido — "Alvo é empurrado 1UD para trás, se este ataque for um Acerto Crítico o dano causado
 * aumenta em +2." A Corrente de Efeitos, and (table ruling, 2026-09-27) the <b>Empurrão Violento</b>
 * {@code EscudeiroFeat#DOMINIO_DA_ARTE_DO_ESCUDO_ATACANTE} names.
 *
 * <p>The push is <b>reported, never applied</b> — {@link InteractionResult#getPushedBackUd()} — since
 * this core holds no positions (the same division of labour a Defensive critical's push has). The +2
 * on a critical lands on the target's PV as this stage runs, after the hit's own mitigation, which
 * already happened at the chain head: ⚠️ it rides the Corrente rather than the dano roll, so RD does
 * not reach it.
 */
public class Rugido extends AbstractEffect implements EffectChain {

    /** "empurrado 1UD para trás". */
    public static final int PUSH_UD = 1;

    /** "o dano causado aumenta em +2" on an Acerto Crítico. */
    public static final int CRITICAL_DAMAGE_BONUS = 2;

    private final boolean critical;

    /** @param critical whether the attack carrying it was an Acerto Crítico */
    public Rugido(final boolean critical) {
        this.critical = critical;
    }

    public boolean isCritical() {
        return critical;
    }

    @Override
    public String getDescription() {
        return "Alvo é empurrado 1UD para trás, se este ataque for um Acerto Crítico o dano causado aumenta em +2.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder().pushedBackUd(PUSH_UD);
        if (critical) {
            target.applyDamage(CRITICAL_DAMAGE_BONUS);
            result.resourceLossValue(CRITICAL_DAMAGE_BONUS)
                    .resourceLossType(org.aventyrs.core.sheet.ResourceType.HIT_POINTS);
        }
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }
}
