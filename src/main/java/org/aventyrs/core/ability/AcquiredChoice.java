package org.aventyrs.core.ability;

import lombok.Getter;

/**
 * Records the value a character chose when acquiring an ability whose rules require picking
 * one — e.g. which Perícia {@link GnoseAbility#PERITO_TEORICO} or
 * {@code ArtesCompetencyAbility#APRIMORAR_COM_ARTE} applies to, or a future ability that lets
 * the player pick one of several fixed effects instead. {@code C} is the type of the chosen
 * value (a {@code SkillType} for a Perícia choice, an ability-specific enum for an
 * effect choice, etc.); {@code ability} is the specific enum constant the choice belongs to
 * (an {@link AttributeAbility}, {@code SkillCompetencyAbility}, or similar).
 *
 * <p>This doesn't validate that the choice is legal (e.g. that a chosen Perícia is actually
 * trained) — same restraint this codebase already applies to unenforced prerequisites like
 * "Requer N Graduações"; see {@code org.aventyrs.core.character.services.AbilityChoiceService}
 * for how a recorded choice is looked back up.
 */
@Getter
public class AcquiredChoice<C> {
    private final Object ability;
    private final C value;

    private AcquiredChoice(final Object ability, final C value) {
        this.ability = ability;
        this.value = value;
    }

    public static <C> AcquiredChoice<C> of(final Object ability, final C value) {
        return new AcquiredChoice<>(ability, value);
    }
}
