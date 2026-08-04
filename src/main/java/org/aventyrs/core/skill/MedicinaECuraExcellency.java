package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Medicina e Cura's automatic Excelência bonuses, granted once a character's Medicina e Cura
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum MedicinaECuraExcellency implements SkillExcellency {

    // TODO: automatic success on rolls to stanch bleeding while no enemies are within
    // Distância Curta. The enemy-proximity condition itself is now checkable for real —
    // see org.aventyrs.core.scene.SceneContext#hasEnemyWithin(Range.DISTANCIA_CURTA) — but
    // this still needs a roll-resolution-vs-DifficultyLevel engine to define "success" in
    // the first place (same gap as AtaqueADistanciaCompetencyAbility.DIRECAO_SEGURA-style
    // auto-success effects), which doesn't exist yet; nothing currently calls
    // AbstractSkillInteraction's SceneContext-accepting applyTo overload for this ability.
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
