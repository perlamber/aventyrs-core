package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * Empatia Selvagem's automatic Excelência bonuses, granted once a character's Empatia
 * Selvagem graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum EmpatiaSelvagemExcellency implements SkillExcellency {

    // Note: scoped to rolls made against neutral or favorable creatures specifically, but
    // this codebase doesn't track a roll's target disposition (same simplification as
    // EmpatiaSelvagemCompetencyAbility.AMAINAR_A_SELVAGERIA), so it's implemented as an
    // unconditional flat bonus rather than silently narrowed.
    FOCADO(ExcellencyTier.FOCADO, "Vantagem em rolagens para lidar com criaturas neutras ou " +
            "favoráveis.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: irrational creatures always treat the character as neutral or favorable — this
    // is an NPC-disposition override, not a roll bonus, and no NPC-disposition/reputation
    // system exists yet (same gap as ArtesCompetencyAbility.ESPALHAR_REPUTACAO).
    LENDA(ExcellencyTier.LENDA, "As criaturas irracionais não te veem como ameaças e são " +
            "sempre neutros ou favoráveis a sua presença.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.EMPATIA_SELVAGEM;
    }
}
