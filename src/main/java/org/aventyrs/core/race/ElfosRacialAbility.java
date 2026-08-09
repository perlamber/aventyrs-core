package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Habilidades Raciais granted to every Elfo — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a
 * separate type.
 */
@Getter
@AllArgsConstructor
public enum ElfosRacialAbility implements SkillCompetencyAbility {

    // Unconditional Vantagem on every Atenção roll — unlike AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
    // this doesn't depend on any per-roll target data, so it's granted the same flat-Vantagem
    // way any ordinary SkillCompetencyAbility is (see CLAUDE.md's "Vantagem is a flat +2
    // bonus" section) — a @Modifier method, now picked up automatically by
    // AbstractSkillInteraction for every Character whose Race grants it, same as if it had
    // been acquired directly.
    SENTIDOS_ABSOLUTOS("Vantagem em todas as suas rolagens de Atenção.") {
        @Modifier(ModifierType.ATTENTION_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATTENTION;
    }
}
