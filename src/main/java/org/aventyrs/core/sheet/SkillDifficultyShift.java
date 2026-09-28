package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * A timed shift on the GD ladder a foe presents on some Perícias — "a Perícia escolhida tem o GD
 * aumentado em +1 nível. Duração 3 Rodadas", "GD das Rolagens de Perícias de Ataque e Defesas
 * aumentadas em +1 nível". A monster never rolls, so where a character would get a bonus it gets
 * steps; read by {@code MonsterRules}'s live GD path.
 */
@Getter
public class SkillDifficultyShift extends TemporaryEffect {

    @NonNull
    private final Set<SkillType> skills;

    private final int steps;

    private final String source;

    public SkillDifficultyShift(@NonNull final Set<SkillType> skills, final int steps, final Integer rounds,
                                final String source) {
        super(rounds);
        this.skills = Set.copyOf(skills);
        this.steps = steps;
        this.source = source;
    }

    /** The steps it grants skill right now — 0 once it lapses or for a Perícia it doesn't name. */
    public int stepsFor(final SkillType skill) {
        return !isExpired() && skills.contains(skill) ? steps : 0;
    }

    @Override
    Object stackingKey() {
        return source;
    }
}
