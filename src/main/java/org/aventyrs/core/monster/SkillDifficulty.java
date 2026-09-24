package org.aventyrs.core.monster;

import lombok.NonNull;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * One Grau de Dificuldade a foe presents — a {@link DifficultyLevel} plus a flat modifier on top of
 * its threshold, the same tier-and-bonus pair {@code org.aventyrs.core.sheet.Hidden} holds and
 * {@code org.aventyrs.core.combat.IncomingAttack} carries to a defender.
 *
 * <p>A foe never rolls, so every Perícia it uses is an authored number rather than dice: its
 * attacks present one of these to a defender's Esquiva e Aparar roll, its Atenção is one of these
 * flattened ({@link #getValue()}) against a hider's Furtividade, and its Furtividade is one of these
 * a watcher's Atenção roll has to reach. See {@link MonsterTemplate#getSkillDifficulty}.
 *
 * @param level the tier, never {@code null}
 * @param bonus the flat modifier on top of {@link DifficultyLevel#getBaseValue()} — may be negative
 */
public record SkillDifficulty(@NonNull DifficultyLevel level, int bonus) {

    /** What a foe whose stat block names no GD presents — Médio, no modifier. */
    public static final SkillDifficulty DEFAULT = new SkillDifficulty(DifficultyLevel.MEDIUM, 0);

    public static SkillDifficulty of(@NonNull final DifficultyLevel level, final int bonus) {
        return new SkillDifficulty(level, bonus);
    }

    /**
     * The GD flattened to a number the way this core flattens every GD — the tier's base threshold
     * plus the bonus, the same arithmetic {@code AttackReceiver#resolve} and {@code
     * Hidden#getConcealmentValue()} do. Médio +2 is 20.
     */
    public int getValue() {
        return level.getBaseValue() + bonus;
    }
}
