package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Artes' automatic Excelência bonuses, granted once a character's Artes graduation reaches
 * each {@link ExcellencyTier}'s threshold. Focado and Lenda both raise Fama Positiva and add
 * up rather than Lenda replacing Focado's — +2 then +3 more, totaling +5 once both are active.
 */
@Getter
@AllArgsConstructor
public enum ArtesExcellency implements SkillExcellency {

    // TODO: +2 Fama Positiva, granted once when Artes graduation first reaches 3 — Fama is
    // now tracked (CharacterSheet.increaseFamaPositiva), but nothing yet detects a
    // graduation crossing a threshold to trigger this automatically.
    FOCADO(ExcellencyTier.FOCADO, "Fama Positiva +2."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +3 Fama Positiva adicional (soma com o +2 de Focado, totalizando +5) — mesma
    // ausência de gatilho de cruzamento de graduação que Focado.
    LENDA(ExcellencyTier.LENDA, "Fama Positiva +3 adicional (totalizando +5 com Focado).");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ARTES;
    }
}
