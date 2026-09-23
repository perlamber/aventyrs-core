package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Dilacerar — "O alvo perde 1 ponto temporário de Força e Destreza" (Maior) / "de Força ou
 * Destreza, definido aleatoriamente" (Menor: a d6, 1–3 Força, 4–6 Destreza). Each lost point is an
 * open-ended {@code <ATTR>_BONUS} −1 that ends at the target's next Descanso of any tier — "ponto
 * temporário" names no Duração, and a Descanso is where this ruleset returns temporary points. Unsourced,
 * so a second Dilacerar costs a second point.
 *
 * <p>Reach is the {@code <ATTR>_BONUS} path's own: a Perícia roll governed by that Atributo, not
 * PV/PM/PD (see CLAUDE.md's Atributo-bonus row).
 */
public class Dilacerar extends AbstractCriticalEffect {


    public Dilacerar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.DILACERAR;
    }

    @Override
    protected String majorDescription() {
        return "O alvo perde 1 ponto temporário de Força e Destreza.";
    }

    @Override
    protected String minorDescription() {
        return "O alvo perde 1 ponto temporário de Força ou Destreza, definido aleatoriamente.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        if (getContext().isMajor()) {
            lose(target, ModifierType.STRENGTH_BONUS);
            lose(target, ModifierType.DEXTERITY_BONUS);
        } else {
            lose(target, roll(1) <= 3 ? ModifierType.STRENGTH_BONUS : ModifierType.DEXTERITY_BONUS);
        }
        return InteractionResult.builder();
    }

    private static void lose(final CombatantSheet target, final ModifierType attribute) {
        target.applyEffectUntilRest(new TemporaryBonus(attribute, -1, null, null, Integer.MAX_VALUE), RestType.MINIMO);
    }
}
