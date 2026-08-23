package org.aventyrs.core.skill.artes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Artes' automatic Excelência bonuses, granted once a character's Artes graduation reaches
 * each {@link ExcellencyTier}'s threshold. Focado and Lenda both raise the chosen Fama and
 * add up rather than Lenda replacing Focado's — +2 then +3 more, totaling +5 once both are
 * active (Lenda's rules text reads "muda para +5", but per this codebase's convention that's
 * the cumulative total across tiers, not a standalone value — see CLAUDE.md).
 */
@Getter
@AllArgsConstructor
public enum ArtesExcellency implements SkillExcellency {

    // TODO: +2 to Fama Positiva or Fama Negativa, whichever the player chooses once when
    // Artes graduation first reaches 3 — Fama is tracked
    // (CombatantSheet.increaseFamaPositiva/increaseFamaNegativa), and the permanent choice
    // itself could be persisted via org.aventyrs.core.ability.AcquiredChoice — but nothing
    // yet detects a graduation crossing a threshold to trigger this automatically.
    FOCADO(ExcellencyTier.FOCADO, "Fama Positiva ou Negativa (à escolha) +2."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +3 more to whichever Fama was chosen at Focado (totaling +5) — same missing
    // graduation-threshold trigger and choice-persistence gaps as Focado.
    LENDA(ExcellencyTier.LENDA, "Bônus na Fama escolhida muda para +5.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ARTES;
    }
}
