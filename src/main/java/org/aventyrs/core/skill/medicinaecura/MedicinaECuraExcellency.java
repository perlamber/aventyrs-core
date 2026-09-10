package org.aventyrs.core.skill.medicinaecura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Medicina e Cura's automatic Excelência bonuses, granted once a character's Medicina e Cura
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum MedicinaECuraExcellency implements SkillExcellency {

    // TODO: both halves are individually real — the enemy-proximity condition is
    // SceneContext#hasEnemyWithin(Range.DISTANCIA_CURTA), and automatic success is
    // SkillCompetencyAbility#resolveAutomaticSuccess (see AttentionCompetencyAbility
    // .PERCEPCAO_DE_FOXM). What blocks this one is that it is a SkillExcellency, and that
    // interface has no resolve* hook at all: AbstractSkillInteraction scans it only for @Modifier
    // methods and getDifficultyReduction(), so it receives neither the SceneContext this needs
    // nor the target GD. That hook parity is the gap, not the engine.
    // TODO: "estancar sangramentos" additionally scopes this to one purpose of the Perícia, which
    // this core does not track (CLAUDE.md's "never tracks what a roll is for").
    FOCADO(ExcellencyTier.FOCADO, "Se não tiver inimigos próximos (Distância Curta) você é " +
            "sempre considerado bem-sucedido em rolagens para estancar sangramentos."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +3 to Cura (PV recovery) effects — this is an active-healing-effect value, not
    // the org.aventyrs.core.rest.RestService's passive Descanso recovery formula, and no
    // such active-healing-amount concept exists yet (same gap as
    // MedicinaECuraCompetencyAbility.MEDICINA_ALTERNATIVA/MILAGREIRO).
    LENDA(ExcellencyTier.LENDA, "Efeitos de Cura (que permitam a recuperação de PV de um " +
            "alvo) aumentam em +3.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.MEDICINA_E_CURA;
    }
}
