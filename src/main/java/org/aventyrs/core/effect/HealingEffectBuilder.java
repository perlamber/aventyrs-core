package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;

import java.util.Optional;

/**
 * Builds a {@link SpellHealingEffect} from a Magia's {@code Spell#getHealing()} column —
 * {@link SpellEffectKind#HEALING}'s contributor.
 *
 * <p>It owns the {@link RestService} that turns "como se passasse por um Descanso Longo" into a
 * figure. That collaborator used to sit on {@code SpellCastingServiceImpl}, used by exactly one
 * line and forcing a constructor of its own; keeping it here is what lets the casting service hold
 * none of it.
 */
public class HealingEffectBuilder implements SpellEffectBuilder {

    private final RestService restService;

    public HealingEffectBuilder() {
        this(new RestServiceImpl());
    }

    public HealingEffectBuilder(final RestService restService) {
        this.restService = restService;
    }

    @Override
    public Optional<SpellEffect> build(final Spell spell, final SpellEffectContext context) {
        return spell.getHealing()
                .map(healing -> new SpellHealingEffect(spell, healing, context.hostileTarget(), restService,
                        context.caster()));
    }
}
