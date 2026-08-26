package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ManaDrain;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.CriticalResult;

import java.util.Optional;

/**
 * ManaPurge (Purga-Mana) — the second concrete {@link CriticalEffect} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into), mirroring
 * {@link Sangramento}'s own shape but draining Magic Points instead of Hit Points.
 * Applied to the character receiving the attack — {@link #applyTo}'s {@code target},
 * never the attacker — an immediate PM loss, applied directly via {@link
 * CombatantSheet#spendMagicPoints(int)}. ManaPurge also leaves an ongoing {@link
 * ManaDrain} registered on the target (see {@link CombatantSheet#applyEffect}) — the
 * "1PM adicional por Rodada" half of its own rules text, advanced by a caller via
 * {@link CombatantSheet#tickTemporaryEffects()} once per subsequent Rodada, and
 * interrupted by {@link CombatantSheet#recoverMagicPoints(int)} per ManaPurge's own
 * "Efeitos de cura interrompem a perda de PM por rodada" clause.
 *
 * <p>Its severity is determined directly by the {@link CriticalResult} that triggered
 * it, not picked by hand — {@link CriticalResult#ACERTO_CRITICO_MAIOR} for ManaPurge
 * Maior, {@link CriticalResult#ACERTO_CRITICO_MENOR} for ManaPurge Menor, matching how a
 * caller already has this value in hand from the triggering roll's own {@link
 * InteractionResult#getCriticalResult()}. Any other {@code CriticalResult} — a plain
 * roll, or either Falha Crítica — is rejected at construction ({@link
 * IllegalOperationException}, via {@link CriticalEffect#validateCriticalHit}): ManaPurge
 * is only ever a *consequence* of a critical hit landing, never something a caller
 * applies on its own judgment.
 *
 * <p>ManaPurge Menor's duration (Rodadas equal to the target's own Foco) is fully
 * computable and implemented for real. ManaPurge Maior's own duration — "até o fim da
 * cena ou 1 minuto, o que for maior" — has the same unbuilt-duration gap {@link
 * Sangramento} documents (no scene-end trigger or minute-based time tracking exists yet),
 * so its {@link ManaDrain} is left open-ended (never expires from Rodada countdown
 * alone) the same way; only healing or a caller explicitly clearing it ends it early.
 */
public class ManaPurge extends AbstractEffect implements CriticalEffect {

    private static final int IMMEDIATE_DRAIN = 2;
    private static final int PER_ROUND_DRAIN = 1;

    private final CriticalResult criticalResult;

    public ManaPurge(final CriticalResult criticalResult) {
        CriticalEffect.validateCriticalHit(criticalResult);
        this.criticalResult = criticalResult;
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.PURGA_DE_MANA;
    }

    @Override
    public String getDescription() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                ? "O alvo perde 2PM, e 1PM adicional por Rodada (até o fim da cena ou 1 " +
                        "minuto, o que for maior). Efeitos de cura interrompem a perda de PM por rodada."
                : "O alvo perde 2PM, e 1PM adicional por Rodada por até um número de Rodadas " +
                        "igual ao Foco dele. Efeitos de cura interrompem a perda de PM por rodada.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Character affectedCharacter = target.getCharacter();
        target.spendMagicPoints(IMMEDIATE_DRAIN);

        Optional<Integer> remainingRounds = criticalResult == CriticalResult.ACERTO_CRITICO_MENOR
                ? Optional.of(affectedCharacter.getAttributes().getFocus().getTotal())
                : Optional.empty();
        target.applyEffect(new ManaDrain(PER_ROUND_DRAIN, remainingRounds));

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .resourceLossValue(IMMEDIATE_DRAIN)
                .resourceLossType(ResourceType.MAGIC_POINTS))
                .build();
    }
}
