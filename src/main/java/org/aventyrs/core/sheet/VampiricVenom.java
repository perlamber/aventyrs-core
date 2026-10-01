package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Veneno Vampírico's ongoing half — "então o alvo sofre 2 Pontos de Dano Mágico Profano por Rodada … possui Roubo de
 * Vida 1" ({@code effect.VenenoVampirico}). Each Rodada its holder takes {@link #getDamagePerRound()} and the one
 * who poisoned them recovers {@link #getLifeSteal()} of what landed. Unlike a {@link Bleeding}, a heal does not stop
 * it — its clause says nothing of cures.
 */
@Getter
public class VampiricVenom extends TemporaryEffect {

    private final CombatantSheet source;
    private final int damagePerRound;
    private final int lifeSteal;

    public VampiricVenom(final CombatantSheet source, final int damagePerRound, final int lifeSteal, final int rounds) {
        super(rounds);
        this.source = source;
        this.damagePerRound = damagePerRound;
        this.lifeSteal = lifeSteal;
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        strike(sheet, source, damagePerRound, lifeSteal);
    }

    /**
     * Deals damage to target and gives source back up to lifeSteal of what landed, as a Roubo de Vida — returns what
     * landed. source may be {@code null} (nobody to recover it).
     */
    public static int strike(final CombatantSheet target, final CombatantSheet source, final int damage,
                             final int lifeSteal) {
        int before = target.getDamageTaken();
        // "Dano Mágico Profano" (core 0.0.89).
        target.applyDamage(org.aventyrs.core.character.services.SanctityMitigation.apply(target,
                org.aventyrs.core.character.DamageType.MAGICO, org.aventyrs.core.character.DamageSanctity.PROFANO,
                damage));
        int landed = target.getDamageTaken() - before;
        if (source != null && landed > 0 && lifeSteal > 0
                && org.aventyrs.core.character.services.LifeStealGuard.allows(target, source)) {
            source.healFromLifeSteal(Math.min(lifeSteal, landed));
        }
        return landed;
    }
}
