package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

/**
 * "Física e mentalmente exausto" — Uno com a Ira's price for ending a Frenesi early: "Desvantagem
 * em rolagens de Perícias e Danos até que passe por um Descanso Curto Verdadeiro". Open-ended (no
 * Rodada count): only a true Descanso of at least Curto lifts it, through {@link
 * CombatantSheet#applyEffectUntilTrueRest}.
 *
 * <p>Its Desvantagem is the flat −{@value Skill#ADVANTAGE_BONUS} every Desvantagem is here, and it
 * reaches the rolls through {@link CombatantSheet#getTemporaryBonus}, beside every other timed malus.
 */
@Getter
public class Exhaustion extends TemporaryEffect {

    private final Object source;

    public Exhaustion(@NonNull final Object source) {
        super(null);
        this.source = source;
    }

    /** Desvantagem on Perícia rolls and on Danos; nothing else. */
    public int bonusFor(final ModifierType type) {
        return type == ModifierType.SKILL_ROLL_BONUS || type == ModifierType.DAMAGE_ROLL_BONUS
                ? -Skill.ADVANTAGE_BONUS : 0;
    }

    /** Being exhausted twice is still one Desvantagem. */
    @Override
    boolean isCumulative() {
        return false;
    }
}
