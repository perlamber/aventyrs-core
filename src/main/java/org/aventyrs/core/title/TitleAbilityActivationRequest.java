package org.aventyrs.core.title;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Optional;

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

    /** {@link #target}, or the activator when none was named. */
    public CombatantSheet getEffectiveTarget() {
        return target == null ? activator : target;
    }

    /** The {@link #choice}, if one was made and it is of type. */
    public <T> Optional<T> getChoice(final Class<T> type) {
        return type.isInstance(choice) ? Optional.of(type.cast(choice)) : Optional.empty();
    }
}
