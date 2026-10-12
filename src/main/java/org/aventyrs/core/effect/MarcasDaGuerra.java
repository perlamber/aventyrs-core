package org.aventyrs.core.effect;

import org.aventyrs.core.item.CastingArtifacts;
import org.aventyrs.core.item.CastingArtifactItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Marcas da Guerra — the Cajado de Guerra's Corrente: "Os Bônus em Efeitos de danos e curas de suas Magias e em suas
 * Defesas mudam para +2."
 *
 * <p>Both bonuses it raises are the Cajado's own +1s. The damage/healing one already reached the Magia ({@code
 * SpellCastingResult}), so this stage adds the difference (+1) to the target; the Defesas one is replaced on the
 * <b>caster</b> with +2 under the shared {@link CastingArtifacts#CAST_DEFESAS_SOURCE}.
 */
public class MarcasDaGuerra extends AbstractEffect implements EffectChain {

    /** "mudam para +2". */
    public static final int BONUS = 2;

    private final CombatantSheet caster;
    private final boolean hostile;

    public MarcasDaGuerra(final CombatantSheet caster, final boolean hostile) {
        this.caster = caster;
        this.hostile = hostile;
    }

    @Override
    public String getDescription() {
        return "Os Bônus em Efeitos de danos e curas de suas Magias e em suas Defesas mudam para +2.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        caster.applyEffect(TemporaryBonus.untilTurnStarts(ModifierType.DEFESAS, BONUS, 1,
                CastingArtifacts.CAST_DEFESAS_SOURCE));
        int raise = BONUS - CastingArtifactItem.CAJADO_DE_GUERRA.getSpellEffectBonus();
        return reportChain(SpellEffectBonus.apply(target, hostile, raise).resultStatus(resolveStatus(target))).build();
    }
}
