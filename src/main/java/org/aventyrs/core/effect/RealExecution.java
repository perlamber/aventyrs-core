package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

/**
 * Execução Real — the fifth concrete {@link CriticalEffect} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into). Applied to
 * the character receiving the attack — {@link #applyTo}'s {@code target}, never the
 * attacker.
 *
 * <p>{@link CriticalResult#ACERTO_CRITICO_MAIOR} destroys the target unconditionally.
 * {@link CriticalResult#ACERTO_CRITICO_MENOR} only destroys the target if their current
 * Hit Points — after whatever damage the triggering attack already dealt earlier in the
 * Skill -&gt; Damage -&gt; CriticalEffect pipeline (see {@link
 * HitPointsService#getCurrentHitPoints}) — are already at or below double their own
 * Vigor total; otherwise this Interaction does nothing at all beyond reporting {@code
 * resultStatus} as-is.
 *
 * <p>"Destroyed" is modeled as guaranteeing {@link
 * org.aventyrs.core.character.CharacterStatus#DEAD} (per {@link HitPointsService
 * #getStatus}'s own threshold — current Hit Points at or below negative max Hit Points)
 * — the closest thing this core computes to "imediatamente destruído" — achieved by
 * dealing twice the target's max Hit Points as curse damage (see {@link
 * CombatantSheet#applyCurseDamage}), which is always enough regardless of however much
 * damage the target had already taken. Curse damage is used deliberately — it bypasses
 * Shield points — since an execution-flavored effect reads as absolute; this is an
 * inference from the rules text's tone, not something the text states explicitly (same
 * "flagged, not confirmed" discipline as e.g. {@code ArtesInteraction}'s own
 * UNIMAGINABLE bonus inference). A caller checks whether the execution actually landed
 * via {@link InteractionResult#getResultStatus()} being {@link
 * org.aventyrs.core.character.CharacterStatus#DEAD} — there's no separate boolean flag
 * for this; {@code resultStatus} already carries the outcome every other Interaction in
 * this package reports through, so a dedicated field would just duplicate it. {@link
 * #applyTo} here reports it through {@link AbstractEffect#resolveStatus}, the same
 * derivation every Effect in this package uses — which matters more here than most, since
 * this Interaction applies its damage straight to the {@link CombatantSheet}, bypassing
 * {@code DamageService} entirely (an execution ignores mitigation), and the tier is
 * resolved from the sheet's own post-{@code applyCurseDamage} damage rather than from
 * anything stored.
 *
 * <p>The other half of its own rules text — "não poderá ser ressuscitado" — has nothing
 * in this core to attach to: there is no resurrection mechanic anywhere in this codebase
 * to block (confirmed by search). Unlike {@link Sabotage}'s gap (a whole missing entity
 * to eventually build against), there's no unbuilt system *here* to even cite a TODO
 * against — permanence is simply documented intent for a caller/Narrador to honor, not
 * something {@link #applyTo} can enforce today; a CombatantSheet reported as {@code DEAD}
 * here is still, mechanically, just a CombatantSheet at that status, indistinguishable
 * from any other route to {@code DEAD} — the caller/Narrador is expected to know this
 * Interaction is the one that requires honoring the permanence.
 *
 * <p>Its severity is determined directly by the {@link CriticalResult} that triggered
 * it, not picked by hand; any other {@code CriticalResult} — a plain roll, or either
 * Falha Crítica — is rejected at construction ({@link IllegalOperationException}, via
 * {@link CriticalEffect#validateCriticalHit}), same "only ever a consequence of a
 * critical hit landing" discipline as every other concrete CriticalEffect in this
 * package.
 */
public class RealExecution extends AbstractEffect implements CriticalEffect {

    private static final int LETHAL_DAMAGE_MULTIPLIER = 2;
    private static final int MENOR_VIGOR_MULTIPLIER = 2;

    private final CriticalResult criticalResult;

    public RealExecution(final CriticalResult criticalResult) {
        CriticalEffect.validateCriticalHit(criticalResult);
        this.criticalResult = criticalResult;
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.EXECUCAO_REAL;
    }

    @Override
    public String getDescription() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                ? "O alvo é imediatamente destruído e não poderá ser ressuscitado."
                : "Se o alvo deste ataque tiver seus PV reduzidos à uma quantidade igual " +
                        "ou inferior ao dobro do Vigor dele ele será imediatamente " +
                        "destruído e não poderá ser ressuscitado.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Character affectedCharacter = target.getCharacter();
        int maxHitPoints = hitPointsService.getMaxHitPoints(affectedCharacter);

        boolean destroyed;
        if (criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR) {
            destroyed = true;
        } else {
            int currentHitPoints = hitPointsService.getCurrentHitPoints(affectedCharacter, target);
            int vigorTotal = affectedCharacter.getAttributes().getVigor().getTotal();
            destroyed = currentHitPoints <= MENOR_VIGOR_MULTIPLIER * vigorTotal;
        }

        if (destroyed) {
            target.applyCurseDamage(LETHAL_DAMAGE_MULTIPLIER * maxHitPoints);
        }

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
