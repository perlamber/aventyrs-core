package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.EgoPointType;

/**
 * Autocontrole spent against what an attack is about to do to its defender (2.5 Ego › Autocontrole; core 0.0.81).
 * Carried on the defence roll ({@code SkillRoll#hasAutocontrole}), paid by {@code AutocontroleEgoService#applyDefence},
 * applied by {@code AttackReceiver} when it builds what lands. A temporary point never buys a permanent effect.
 */
@Getter
@AllArgsConstructor
public enum AutocontroleDefence {
    /** "Evitar uma Corrente de Efeitos" — the Corrente's stages don't land. */
    AVOID_CHAIN(EgoPointType.TEMPORARY),
    /** "Ignorar os Efeitos de um Acerto Crítico Menor que você tenha sofrido" — a Menor critical's effects don't land. */
    IGNORE_MINOR_CRITICAL(EgoPointType.TEMPORARY),
    /**
     * "Evitar uma Corrente de Efeitos … e tornar-se imune ao longo da Cena" — the stages don't land, and each stage's
     * kind is refused for the rest of the Cena ({@code CombatantSheet#grantCenaImmunity}).
     */
    AVOID_CHAIN_WITH_IMMUNITY(EgoPointType.PERMANENT),
    /** "Evitar … Efeito Crítico e tornar-se imune ao longo da Cena" — any critical's effects, and their types. */
    AVOID_CRITICAL_WITH_IMMUNITY(EgoPointType.PERMANENT);

    private final EgoPointType pointType;
}
