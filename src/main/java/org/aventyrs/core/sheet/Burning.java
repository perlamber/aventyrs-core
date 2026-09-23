package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Inflamar — "alvo sofre 2 pontos de dano de fogo por Rodada, até que o apague 3PA" (Maior) / "2PA"
 * (Menor). Open-ended: it burns until somebody puts it out, which is {@link
 * CombatantSheet#extinguish()} — the caller spends {@link #getExtinguishActionPoints()} PA on it,
 * reported, not charged, like every price in this core.
 *
 * <p>The per-Rodada damage is the bare PV loss, as {@link Bleeding}'s is: fire already on the
 * target's body is not an attack for RD to judge. Not cumulative — a second Inflamar relights the
 * same fire, keeping the harder one to put out.
 */
@Getter
public class Burning extends TemporaryEffect {

    private final int damagePerRound;
    private final int extinguishActionPoints;

    public Burning(final int damagePerRound, final int extinguishActionPoints) {
        super(null);
        this.damagePerRound = damagePerRound;
        this.extinguishActionPoints = extinguishActionPoints;
    }

    @Override
    boolean isCumulative() {
        return false;
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        sheet.applyDamage(damagePerRound);
    }
}
