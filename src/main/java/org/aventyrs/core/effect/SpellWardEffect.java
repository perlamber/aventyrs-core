package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellWard;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.MaleficioWard;
import org.aventyrs.core.sheet.PostponedWoundWard;

import java.util.UUID;

/**
 * Leaves a Magia's {@link SpellWard} on its target (core 0.0.94). Corpo Fechado's ward is sustained by the caster's
 * Concentração and lasts its Duração's trailing Rodadas after; Procrastinar Ferimento's waits for the next hit.
 */
public class SpellWardEffect extends AbstractEffect implements DefensiveEffect {

    private final Spell spell;
    private final SpellWard ward;
    private final UUID casterId;

    /** @param casterId whoever concentrates on a sustained ward — {@code null} when the caster is unknown */
    public SpellWardEffect(final Spell spell, final SpellWard ward, final UUID casterId) {
        this.spell = spell;
        this.ward = ward;
        this.casterId = casterId;
    }

    @Override
    public Spell getSpell() {
        return spell;
    }

    @Override
    public String getDescription() {
        return spell.getPrimaryEffectDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        switch (ward) {
            case MALEFICIOS -> target.applyEffect(new MaleficioWard(casterId == null ? target.getId() : casterId,
                    spell.getDuration() == null ? 0 : spell.getDuration().count()));
            case POSTPONED_WOUND -> target.applyEffect(new PostponedWoundWard());
        }
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
