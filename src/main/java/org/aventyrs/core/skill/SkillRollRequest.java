package org.aventyrs.core.skill;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Everything needed to request one Perícia roll, bundled into a single value object for a
 * caller (typically an API layer receiving an incoming roll request) to build once and hand to
 * {@link SkillInteractionFactory#resolve(SkillRollRequest)} — instead of separately looking up
 * the right {@code <Skill>Interaction} class for {@code skillType} and calling its 3-arg
 * {@code applyTo} directly. {@code skillType} and {@code target} are required; {@code
 * sceneContext} and {@code skillRoll} are optional (either or both may be {@code null}, same as
 * every {@code applyTo} overload already accepts) — a request for a plain roll with no
 * proximity data and no dice yet supplies neither. {@code attackSource} is optional too, and
 * meaningful only on a Perícia de Ataque roll.
 *
 * <p>{@code attackTarget} is deliberately absent: it is a {@code CombatantSheet}, which this
 * value object has no way to resolve from a deserialized request, and an attack that needs one
 * belongs in {@code org.aventyrs.core.combat.DeliveredAttack} rather than here.
 */
@Getter
@Builder
public class SkillRollRequest {
    @NonNull
    private final SkillType skillType;
    @NonNull
    private final CombatantSheet target;
    private final SceneContext sceneContext;
    private final SkillRoll skillRoll;

    /**
     * What an attack is being delivered with, for a request that is one — the {@link
     * org.aventyrs.core.item.Weapon} or {@link org.aventyrs.core.magic.Spell} itself, since both
     * are {@link AttackSource}s. Optional like the two above; {@code null} on any non-attack
     * roll, and on an attack whose caller didn't say.
     */
    private final AttackSource attackSource;
}
