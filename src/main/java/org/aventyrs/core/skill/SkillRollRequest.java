package org.aventyrs.core.skill;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;

/**
 * Everything needed to request one Perícia roll, bundled into a single value object for a
 * caller (typically an API layer receiving an incoming roll request) to build once and hand to
 * {@link SkillInteractionFactory#resolve(SkillRollRequest)} — instead of separately looking up
 * the right {@code <Skill>Interaction} class for {@code skillType} and calling its 3-arg
 * {@code applyTo} directly. {@code skillType} and {@code target} are required; {@code
 * sceneContext} and {@code skillRoll} are optional (either or both may be {@code null}, same as
 * every {@code applyTo} overload already accepts) — a request for a plain roll with no
 * proximity data and no dice yet supplies neither.
 */
@Getter
@Builder
public class SkillRollRequest {
    @NonNull
    private final SkillType skillType;
    @NonNull
    private final CharacterSheet target;
    private final SceneContext sceneContext;
    private final SkillRoll skillRoll;
}
