package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.Burning;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Inflamar — "alvo sofre 2 pontos de dano de fogo por Rodada, até que o apague" with 3PA (Maior) /
 * 2PA (Menor): a {@code sheet.Burning} ticking each Rodada until {@code CombatantSheet#extinguish}.
 */
public class Inflamar extends AbstractCriticalEffect {

    static final int DAMAGE_PER_ROUND = 2;


    public Inflamar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.INFLAMAR;
    }

    @Override
    protected String majorDescription() {
        return "alvo sofre 2 pontos de dano de fogo por Rodada, até que o apague 3PA.";
    }

    @Override
    protected String minorDescription() {
        return "alvo sofre 2 pontos de dano de fogo por Rodada, até que o apague 2PA.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.applyEffect(new Burning(DAMAGE_PER_ROUND, pick(3, 2)));
        return InteractionResult.builder();
    }
}
