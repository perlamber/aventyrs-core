package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

/**
 * Sabotar — the fourth concrete {@link CriticalEffect} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into), and
 * deliberately a placeholder: unlike {@link Sangramento}/{@link ManaPurge}/{@link
 * Primor}, its own effect can't be computed for real yet, because it targets
 * *equipment*, not a CharacterSheet resource pool — {@link CriticalResult
 * #ACERTO_CRITICO_MAIOR} destroys every "item tecnológico" the target is carrying;
 * {@link CriticalResult#ACERTO_CRITICO_MENOR} deals 3d6 damage to them instead.
 *
 * <p>None of that is expressible today: this core has no Item/Equipamento entity at all
 * (see {@code org.aventyrs.core.item.ItemInteraction}, an unrelated pre-existing stub —
 * same gap {@code ProfissaoCompetencyAbility}'s own class javadoc already documents), so
 * there's nothing to classify as "tecnológico", no per-CharacterSheet inventory to find
 * such items on, and no "destroyed"/damage-taken state for one to mutate. The 3d6 damage
 * roll itself is also out of scope for this core regardless (it never rolls dice — see
 * {@code org.aventyrs.core.skill} package-info's "What this library computes" section),
 * matching how {@link Sangramento}/{@link ManaPurge} are handed an already-resolved
 * {@link CriticalResult} rather than rolling their own dice; a caller would need to
 * supply an already-rolled 3d6 total the same way, once there's an item to apply it to.
 *
 * <p>What *is* real here, matching every other concrete {@code CriticalEffect}: severity
 * is still resolved directly from the triggering {@link CriticalResult}, not picked by
 * hand, and any {@code CriticalResult} that isn't {@link CriticalResult#ACERTO_CRITICO_MAIOR}/
 * {@link CriticalResult#ACERTO_CRITICO_MENOR} is rejected at construction ({@link
 * IllegalOperationException}, via {@link CriticalEffect#validateCriticalHit}) — same
 * "only ever a consequence of a critical hit landing" discipline as the rest of this
 * package. {@link #applyTo} itself is the one piece left undone: it computes nothing
 * item-related (there's nothing to compute against) and returns only {@code
 * resultStatus}, the one field every Interaction in this core can always report.
 */
public class Sabotage extends AbstractEffect implements CriticalEffect {

    private final CriticalResult criticalResult;

    public Sabotage(final CriticalResult criticalResult) {
        CriticalEffect.validateCriticalHit(criticalResult);
        this.criticalResult = criticalResult;
    }

    @Override
    public String getDescription() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                ? "Itens tecnológicos afetados são destruídos"
                : "Itens tecnológicos sofrem 3d6 pontos de dano";
    }

    // TODO: destroy (Maior) or deal an already-rolled 3d6 total of damage to (Menor)
    // every "item tecnológico" the target is carrying — blocked on the Item/Equipamento
    // entity itself (no such type, no "tecnológico" classification, and no
    // per-CharacterSheet inventory to search) not existing anywhere in this core yet; see
    // this class's own javadoc for the full gap. Nothing here should be guessed at ahead
    // of that entity actually being modeled.
    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character affectedCharacter = target.getCharacter();
        return reportChain(InteractionResult.builder()
                .resultStatus(affectedCharacter.getStatus()))
                .build();
    }
}
