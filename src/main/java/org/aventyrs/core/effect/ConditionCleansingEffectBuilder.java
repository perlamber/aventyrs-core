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
        java.util.UUID casterId = context.caster() == null ? null : context.caster().getId();
        Optional<SpellWardEffect> ward = spell.getWard().map(kind -> new SpellWardEffect(spell, kind, casterId));
        if (spell.getCleansedConditions().isEmpty()) {
            // Procrastinar Ferimento: a ward and nothing else.
            return ward.map(SpellEffect.class::cast);
        }
        ConditionCleansingEffect cleansing = new ConditionCleansingEffect(spell, spell.getCleansedConditions());
        // Corpo Fechado: every Malefício removed, then none may land while the ward holds.
        ward.ifPresent(cleansing::chainInto);
        return Optional.of(cleansing);
    }
}
