package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

/**
 * A defence scoped to <b>one attacker</b> — "Bônus de +5 em suas Defesas para evitar ataques do
 * alvo" ({@code effect.CriticalEffectType#PREVENIR}), "Bônus de +2 em suas Defesas para resistir aos
 * ataques dele" and "imune aos ataques dele" ({@code DefensiveCriticalEffectType#IMPETO_DEFENSIVO}),
 * "Sua Margem Crítica Menor para resistir a este próximo ataque aumenta" ({@code #PROVOCAR}),
 * "+3 em suas Defesas para resistir aos ataques do inimigo que você se aproximou" (Rolamento
 * Ofensivo).
 *
 * <p>A {@link Blessing} cannot say this: it reaches every attack alike. This effect names the
 * attacker, and is read wherever the attacker is known — {@code DefenseServiceImpl} and {@code
 * CriticalServiceImpl} against the defence roll's {@code SceneContext#getOpposedCharacter()} (the
 * attacker, on a defence roll), and {@code AttackReceiver} against its {@code IncomingAttack}'s
 * attacker for the immunity. A caller building the defender's context without naming the attacker
 * as its opposed character gets no bonus — "cannot tell" withholds, as everywhere here.
 *
 * <p>Held by the defender. Several stack: each is its own grant, summed.
 */
@Getter
public class AttackerGuard extends TemporaryEffect {

    private final UUID attackerId;
    private final int defesasBonus;
    private final int criticalMarginIncrease;
    private final boolean immune;

    public AttackerGuard(@NonNull final UUID attackerId, final int defesasBonus, final int criticalMarginIncrease,
                         final boolean immune, final int rounds) {
        this(attackerId, defesasBonus, criticalMarginIncrease, immune, rounds, false);
    }

    private AttackerGuard(final UUID attackerId, final int defesasBonus, final int criticalMarginIncrease,
                          final boolean immune, final int rounds, final boolean countsDownAtTurnStart) {
        super(rounds, countsDownAtTurnStart);
        this.attackerId = attackerId;
        this.defesasBonus = defesasBonus;
        this.criticalMarginIncrease = criticalMarginIncrease;
        this.immune = immune;
    }

    /** A Defesas bonus against attacker for rounds Rodadas. */
    public static AttackerGuard defesas(final CombatantSheet attacker, final int bonus, final int rounds) {
        return new AttackerGuard(attacker.getId(), bonus, 0, false, rounds);
    }

    /**
     * A Defesas bonus against attacker taken on the holder's own Turn, lasting until their Turn
     * begins in the next Rodada — "por 1 Rodada" under the table ruling (Rolamento Ofensivo).
     */
    public static AttackerGuard defesasUntilNextTurn(final CombatantSheet attacker, final int bonus) {
        return new AttackerGuard(attacker.getId(), bonus, 0, false, 1, true);
    }

    /** Whether this guard is against attacker. {@code null} is nobody. */
    public boolean isAgainst(final CombatantSheet attacker) {
        return attacker != null && attackerId.equals(attacker.getId());
    }
}
