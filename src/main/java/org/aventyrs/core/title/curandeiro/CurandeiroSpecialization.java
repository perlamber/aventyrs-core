package org.aventyrs.core.title.curandeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Curandeiro's own catalog of Especializações — exactly two, a player may hold both, one, or
 * neither. <b>Both are passive</b>, so neither names an Interaction: {@link Curandeiro}'s hooks read
 * them.
 */
@Getter
@AllArgsConstructor
public enum CurandeiroSpecialization implements AventyrTitleSpecialization {

    // "Apenas 'Curandeiros' podem adquirir esta especialização" — enforced by
    // Curandeiro#grantSpecialization. Passive. Real through Curandeiro#resolveHealingBonus: every heal
    // the holder makes offers +2PV, before any halving or the Coma cap.
    MEDICO_DE_GUERRA(
            "Os efeitos de recuperação de PV aumentam em +2.",
            fixed(0), ActionCost.NONE, Optional.empty()),

    // "Apenas 'Curandeiros' podem adquirir esta especialização" — enforced by
    // Curandeiro#grantSpecialization. Passive. Real through Curandeiro#resolveCastingDifficultyReduction:
    // -1 Nível on a Magia that heals and does not target only its caster ("outros personagens").
    MARTIR_ALTRUISTA(
            "O GD de suas magias que permitem que outros personagens recuperarem PV é reduzido em -1 Nível.",
            fixed(0), ActionCost.NONE, Optional.empty());

    private final String description;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
