package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * The "you just dropped a foe" trigger. This core has no defeat observer — nothing reports back
 * to an attacker that its blow reduced a target to 0 PV — so, like {@code
 * CombatantSheet#recordAction} and session recovery, the <b>caller drives it</b>: after an
 * attack it has determined was fatal (its own {@code DeliveredAttackResult} / {@code
 * HitPointsService#getStatus} check), it calls {@link #applyDefeatBlessings}.
 *
 * <p>Scans {@code attacker}'s {@code Feat}s for {@code Feat#resolveDefeatBlessings} contributions
 * ({@code AssassinoFeat#SANGUE_QUENTE}, {@code VIOLENCIA_DESCOMUNAL}, {@code
 * ARCANISMO_AVASSALADOR}) and applies each as a {@code TemporaryBonus} on {@code attacker}
 * itself — every such Blessing is {@code TargetScope#SELF}.
 */
public interface DefeatBlessingService {

    /**
     * Grants {@code attacker} every {@link Blessing} its held Talentos produce for having just
     * defeated {@code defeated}, and returns them. {@code viaCriticalHit} distinguishes "eliminou
     * um alvo com um Acerto Crítico" clauses from a plain "derrotar um inimigo" one.
     */
    List<Blessing> applyDefeatBlessings(CombatantSheet attacker, CombatantSheet defeated, boolean viaCriticalHit);
}
