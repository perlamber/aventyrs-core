package org.aventyrs.core.effect;

import lombok.Getter;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HalvedHealing;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.StrippedReductions;

/**
 * Enfadecer's Corrente (core 0.1.0): "Adicionalmente aos efeitos anteriores, o alvo desta magia perde sua RD e RM, e
 * efeitos de Cura que ele receberia são reduzidos à metade" — for the Magia's Duração, a {@link StrippedReductions}
 * and one {@link HalvedHealing}.
 */
@Getter
public class BonecaDePorcelana extends AbstractEffect implements EffectChain {

    private final Spell spell;
    private final int rounds;

    public BonecaDePorcelana(final Spell spell, final int rounds) {
        this.spell = spell;
        this.rounds = rounds;
    }

    @Override
    public String getDescription() {
        return spell.getEffectChainDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (rounds > 0) {
            target.applyEffect(new StrippedReductions(rounds));
            target.applyEffect(new HalvedHealing(rounds));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
