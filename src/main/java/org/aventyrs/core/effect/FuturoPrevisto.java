package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

/**
 * Futuro Previsto — the Bola de Crista's Corrente: "Esta Magia aplica Efeitos Críticos Menores aos alvos."
 *
 * <p>Applies the Magia's own Efeito Crítico ({@code Spell#getCriticalEffectType()}) at Menor to this target, built as
 * {@code SpellCastingService#resolveCastingCriticalEffect} builds one. A Magia naming none applies nothing, and a
 * dice-bearing Menor (this stage has no roller) is reported on {@code unappliedCriticalEffects} rather than invented.
 */
public class FuturoPrevisto extends AbstractEffect implements EffectChain {

    private final CombatantSheet caster;
    private final Spell spell;

    public FuturoPrevisto(final CombatantSheet caster, final Spell spell) {
        this.caster = caster;
        this.spell = spell;
    }

    @Override
    public String getDescription() {
        return "Esta Magia aplica Efeitos Críticos Menores aos alvos.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder();
        CriticalEffectType type = spell.getCriticalEffectType();
        if (type != null) {
            CriticalEffectContext context = CriticalEffectContext.of(caster, spell, CriticalResult.ACERTO_CRITICO_MENOR,
                    null);
            if (CriticalEffects.canCreate(type, context)) {
                CriticalEffects.create(type, context).ifPresent(effect -> target.receiveInteraction(effect));
            }
        }
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }
}
