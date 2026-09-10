package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Everything one Magia invocation needs. A Combatant or position target is supplied only when
 * {@link SpellTargeting} for {@link #spell} permits it.
 */
@Getter
@Builder
public class SpellCastRequest {
    @NonNull
    private final CombatantSheet caster;
    @NonNull
    private final Spell spell;
    @NonNull
    private final SceneContext sceneContext;
    @NonNull
    private final Scene scene;
    private final CombatantSheet combatantTarget;
    private final GridPosition positionTarget;
}
