package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Enchantment;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.sheet.Withering;

import java.util.List;

/**
 * Excomungar — "Remove uma das Maldições ou Encantamentos Naturais presentes no alvo." A Corrente de Efeitos
 * ({@code Acólito da Luz Primordial}'s Fundamentalista rung, a Devoto Talento not yet in this core).
 *
 * <p>Table ruling (2026-10-01): any running Maldição or Encantamento counts — a {@link Withering} (Definhar's "Efeito
 * de Maldição"), the {@link ConditionType#AMALDICOADO} Condição, or any {@link Enchantment} — and the caster picks
 * which ({@link #Excomungar(TemporaryEffect)}); with none named, or the one named no longer running, the most recently
 * applied goes. ⚠️ "Naturais" is not checked: a running effect does not record the Magia type it came from.
 */
public class Excomungar extends AbstractEffect implements EffectChain {

    private final TemporaryEffect chosen;

    /** The most recent Maldição or Encantamento goes. */
    public Excomungar() {
        this(null);
    }

    /** @param chosen the Maldição or Encantamento the caster removes, or {@code null} for the most recent */
    public Excomungar(final TemporaryEffect chosen) {
        this.chosen = chosen;
    }

    /** Whether effect is one Excomungar may remove — a Maldição or an Encantamento. */
    public static boolean isRemovable(final TemporaryEffect effect) {
        return effect instanceof Withering || effect instanceof Enchantment
                || effect instanceof Condition condition && condition.getType() == ConditionType.AMALDICOADO;
    }

    /** What Excomungar could remove from target right now, oldest first. */
    public static List<TemporaryEffect> removableFrom(final CombatantSheet target) {
        return target.getRunningEffects().stream().filter(Excomungar::isRemovable).toList();
    }

    @Override
    public String getDescription() {
        return "Remove uma das Maldições ou Encantamentos Naturais presentes no alvo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        List<TemporaryEffect> removable = removableFrom(target);
        TemporaryEffect removed = chosen != null && removable.contains(chosen) ? chosen
                : removable.isEmpty() ? null : removable.get(removable.size() - 1);
        if (removed != null) {
            target.removeEffect(removed);
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
