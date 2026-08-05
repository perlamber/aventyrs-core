package org.aventyrs.core.skill.dominiodomana;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Domínio do Mana's automatic Excelência bonuses, granted once a character's Domínio do Mana
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaExcellency implements SkillExcellency {

    // TODO: +1 to a Magia's Dano/Cura effects and +1 Rodada to its Duração — no Magia
    // damage/healing-formula or duration-tracking system exists yet.
    FOCADO(ExcellencyTier.FOCADO, "Efeitos de Danos e Curas aumentado em +1, Duração de " +
            "suas Magias é aumentada em +1 Rodada."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: once per Round, reduces the Magia's own Tempo de Conjuração by -1PA — needs both
    // a Magia-casting-time entity (this is the spell's own PA cost, not
    // DominioDoManaInteraction's SKILL_ROLL_COST) and a once-per-Round usage-limiting
    // mechanism, neither of which exist yet (every other ModifierType-based bonus in this
    // codebase is unconditionally always-on, not rate-limited per Round).
    LENDA(ExcellencyTier.LENDA, "Apenas uma vez por Rodada o Tempo de Conjuração de suas " +
            "Magias é reduzido em -1PA.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }
}
