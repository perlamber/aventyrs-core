package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Ignora Resistências — Benção Vulcana's Corrente: "Ignora RE e RM." ⚠️ It names the hit's own resistances, so it
 * acts on <em>that</em> hit rather than after it: {@code AttackDelivery} reads it among the triggered Correntes and
 * marks the chain head {@code DamageInteraction#bypassing} RE and RM before the damage is mitigated — the same way
 * a triggered {@link ToqueSombrio} retypes the head. As a stage of its own it changes nothing; it is kept in the
 * chain so the caller sees it was triggered. RA is never ignored.
 */
public class IgnoraResistencias extends AbstractEffect implements EffectChain {

    @Override
    public String getDescription() {
        return "Ignora Resistências: Ignora RE e RM.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
