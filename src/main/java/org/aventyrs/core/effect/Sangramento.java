package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT;

/**
 * Sangramento — the first concrete {@link CriticalEffect} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into). Applied to
 * the character receiving the attack — {@link #applyTo}'s {@code target}, never the
 * attacker — the same shape as {@link DamageInteraction}: an immediate PV loss, applied
 * directly via {@link CharacterSheet#applyDamage(int)}. Unlike DamageInteraction,
 * Sangramento also leaves an ongoing {@link Bleeding} registered on the target (see
 * {@link CharacterSheet#applyEffect}) — the "1PV adicional por Rodada" half of its own
 * rules text, advanced by a caller via {@link CharacterSheet#tickTemporaryEffects()} once
 * per subsequent Rodada, and interrupted by {@link CharacterSheet#heal(int)} per
 * Sangramento's own "Efeitos de cura interrompem a perda de PV por rodada" clause.
 *
 * <p>Its severity is determined directly by the {@link CriticalResult} that triggered
 * it, not picked by hand — {@link CriticalResult#ACERTO_CRITICO_MAIOR} for Sangramento
 * Maior, {@link CriticalResult#ACERTO_CRITICO_MENOR} for Sangramento Menor, matching how
 * a caller already has this value in hand from the triggering roll's own {@link
 * org.aventyrs.core.sheet.InteractionResult#getCriticalResult()} (set by {@code
 * AbstractSkillInteraction} whenever a {@code SkillRoll} was supplied). Any other {@code
 * CriticalResult} — a plain roll, or either Falha Crítica — is rejected at construction
 * ({@link IllegalOperationException}): Sangramento is only ever a *consequence* of a
 * critical hit landing, never something a caller applies on its own judgment, the same
 * "validate possession/legitimacy up front rather than compute a result for an
 * illegitimate case" discipline {@code AbstractSkillInteraction#validateRequestedTrait}
 * already applies to a requested Habilidade/Especialização.
 *
 * <p>Sangramento Menor's duration (Rodadas equal to the target's own Vigor) is fully
 * computable and implemented for real. Sangramento Maior's own duration — "até o fim da
 * cena ou 1 minuto, o que for maior" — has no fixed Rodada count this core can compute
 * (no scene-end trigger or minute-based time tracking exists yet), so its {@link
 * Bleeding} is left open-ended (never expires from Rodada countdown alone); only healing
 * or a caller explicitly clearing it ends it early.
 */
public class Sangramento implements CriticalEffect {

    private static final int IMMEDIATE_DAMAGE = 2;
    private static final int PER_ROUND_DAMAGE = 1;

    private final CriticalResult criticalResult;

    public Sangramento(final CriticalResult criticalResult) {
        if (criticalResult != CriticalResult.ACERTO_CRITICO_MAIOR && criticalResult != CriticalResult.ACERTO_CRITICO_MENOR) {
            throw new IllegalOperationException(CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT);
        }
        this.criticalResult = criticalResult;
    }

    @Override
    public String getDescription() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                ? "O alvo perde 2PV, e 1PV adicional por Rodada (até o fim da cena ou 1 " +
                        "minuto, o que for maior). Efeitos de cura interrompem a perda de PV por rodada."
                : "O alvo perde 2PV, e 1PV adicional por Rodada por até um número de Rodadas " +
                        "igual ao Vigor dele. Efeitos de cura interrompem a perda de PV por rodada.";
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character affectedCharacter = target.getCharacter();
        target.applyDamage(IMMEDIATE_DAMAGE);

        Optional<Integer> remainingRounds = criticalResult == CriticalResult.ACERTO_CRITICO_MENOR
                ? Optional.of(affectedCharacter.getAttributes().getVigor().getTotal())
                : Optional.empty();
        target.applyEffect(new Bleeding(PER_ROUND_DAMAGE, remainingRounds));

        return InteractionResult.builder()
                .resultStatus(affectedCharacter.getStatus())
                .finalDamage(IMMEDIATE_DAMAGE)
                .build();
    }
}
