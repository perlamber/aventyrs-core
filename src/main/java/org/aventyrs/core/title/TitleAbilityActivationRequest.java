package org.aventyrs.core.title;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import org.aventyrs.core.util.DiceRoller;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything one Título ability activation needs, bundled — the request half of {@link
 * AbstractTitleAbilityInteraction#activate} and {@link AventyrTitle#activateAbility}, in the
 * same "one required field plus optional ones" shape as {@code SkillRollRequest} and {@code
 * SpellCastRequest}. Each ability reads only what its rules text needs; an ability that needs a
 * field the caller left out refuses in its own {@code validate}.
 */
@Getter
@Builder
public class TitleAbilityActivationRequest {

    /** Who activates the ability — and pays its PD. */
    @NonNull
    private final CombatantSheet activator;

    /**
     * Who the ability acts on, for a targeted one (Abençoado pela Luz's touch). {@code null}
     * means the activator — see {@link #getEffectiveTarget()}.
     */
    private final CombatantSheet target;

    /** The activator's snapshot of nearby allies/enemies, or {@code null} outside an encounter. */
    private final SceneContext sceneContext;

    /** The live Scene, for an ability that registers something on it; {@code null} otherwise. */
    private final Scene scene;

    /**
     * PD the player chooses to spend. Required for a {@link PDCost.Variable} cost; for a {@link
     * PDCost.Fixed} one, {@code null} means "the stated cost", and anything else must equal it.
     */
    private final Integer determinationPoints;

    /**
     * The player's pick between an ability's branches, typed by the ability itself (e.g. {@code
     * AbencoadoPelaLuzInteraction.Branch}) — read through {@link #getChoice(Class)}, so one field
     * serves every ability without growing a new one per option.
     */
    @Getter(lombok.AccessLevel.NONE)
    private final Object choice;

    /**
     * Several picks at once, for an ability whose rules text lets the player choose any number of
     * options — Cataclismo Elemental's elements, the Especializações activated "em conjunto" with a
     * Frenesi. Read through {@link #getChoices(Class)}; {@link #choice} stays the single-pick field.
     */
    @Getter(lombok.AccessLevel.NONE)
    private final Collection<?> choices;

    /**
     * The caller's dice, for an activation that rolls — a Grito Espinhoso's "1d6", the d6 a
     * Vantagem de Ego reacting to an Ego cost needs. {@code null} when the caller supplies none;
     * this core never rolls on its own.
     */
    private final DiceRoller diceRoller;

    /** {@link #target}, or the activator when none was named. */
    public CombatantSheet getEffectiveTarget() {
        return target == null ? activator : target;
    }

    /** The {@link #choice}, if one was made and it is of type. */
    public <T> Optional<T> getChoice(final Class<T> type) {
        return type.isInstance(choice) ? Optional.of(type.cast(choice)) : Optional.empty();
    }

    /** Every one of {@link #choices} that is of type — empty when none were made. */
    public <T> Set<T> getChoices(final Class<T> type) {
        if (choices == null) {
            return Set.of();
        }
        return choices.stream().filter(type::isInstance).map(type::cast)
                .collect(Collectors.toUnmodifiableSet());
    }
}
