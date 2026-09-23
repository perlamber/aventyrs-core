package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DeterminationDrain;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Excruciante — "O alvo perde 2PD, e 1PD adicional por rodada", the PD twin of {@link Sangramento}:
 * open-ended (Maior) or for as many Rodadas as the target's Instinto (Menor), interrupted by any heal
 * ({@code sheet.DeterminationDrain}).
 */
public class Excruciante extends AbstractCriticalEffect {

    static final int IMMEDIATE_LOSS = 2;
    static final int PER_ROUND_LOSS = 1;


    public Excruciante(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.EXCRUCIANTE;
    }

    @Override
    protected String majorDescription() {
        return "O alvo perde 2PD, e 1PD adicional por rodada. (até o fim da cena ou 1 minuto, o que for maior). Efeitos de cura interrompem a perda de PD por rodada.";
    }

    @Override
    protected String minorDescription() {
        return "O alvo perde 2PD, e 1PD adicional por rodada por até um número de rodadas igual ao Instinto dele. Efeitos de cura interrompem a perda de PD por rodada.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.spendDeterminationPoints(IMMEDIATE_LOSS);
        Integer rounds = getContext().isMajor() ? null
                : target.getCharacter().getEffectiveAttributeTotal(AttributeDomain.INSTINCT);
        target.applyEffect(new DeterminationDrain(PER_ROUND_LOSS, rounds));
        return InteractionResult.builder()
                .resourceLossValue(IMMEDIATE_LOSS)
                .resourceLossType(ResourceType.DETERMINATION_POINTS);
    }
}
