package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;

import java.util.Optional;

/**
 * Builds a {@link ConditionCleansingEffect} from a Magia's {@code Spell#getCleansedConditions()}
 * column — {@link SpellEffectKind#DEFENSIVE}'s contributor.
 *
 * <p>Needs no collaborator: lifting a Condição is {@code CombatantSheet#removeCondition}, which
 * the effect reaches through the target it is applied to.
 *
 * <p>An empty set means the Magia lifts nothing, which is the overwhelming majority — only Vida's
 * alternativo branch authors this column today.
 */
public class ConditionCleansingEffectBuilder implements SpellEffectBuilder {

    @Override
    public Optional<SpellEffect> build(final Spell spell, final SpellEffectContext context) {
        if (spell.getCleansedConditions().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConditionCleansingEffect(spell, spell.getCleansedConditions()));
    }
}
