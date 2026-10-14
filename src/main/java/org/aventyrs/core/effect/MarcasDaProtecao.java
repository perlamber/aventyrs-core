package org.aventyrs.core.effect;

import org.aventyrs.core.item.CastingArtifacts;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Marcas da Proteção — the Cajado do Amparo's Corrente: "Bônus nas Defesas concedido muda para +3. Esta Corrente de
 * Efeitos não desencadeia durante Conjuração de Magias do tipo Semente."
 *
 * <p>It acts on the <b>caster</b>, not the target: it replaces the Cajado's post-cast "+1 em suas Defesas por 1
 * Rodada" with +3, under the same source ({@link CastingArtifacts#CAST_DEFESAS_SOURCE}) so the two never stack. The
 * Semente exclusion is applied where the Corrente is built ({@code CastingArtifactItem#CAJADO_DO_AMPARO}).
 */
public class MarcasDaProtecao extends AbstractEffect implements EffectChain {

    /** "muda para +3". */
    public static final int DEFESAS_BONUS = 3;

    private final CombatantSheet caster;

    public MarcasDaProtecao(final CombatantSheet caster) {
        this.caster = caster;
    }

    @Override
    public String getDescription() {
        return "Bônus nas Defesas concedido muda para +3.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        caster.applyEffect(TemporaryBonus.untilTurnStarts(ModifierType.DEFESAS, DEFESAS_BONUS, 1,
                CastingArtifacts.CAST_DEFESAS_SOURCE));
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
