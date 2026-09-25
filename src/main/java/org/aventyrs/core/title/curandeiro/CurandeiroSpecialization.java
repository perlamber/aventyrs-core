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
 * neither. <b>Both are passive</b>, so neither names an Interaction.
 */
@Getter
@AllArgsConstructor
public enum CurandeiroSpecialization implements AventyrTitleSpecialization {

    // "Apenas 'Curandeiros' podem adquirir esta especialização" — enforced by
    // Curandeiro#grantSpecialization. Passive.
    // TODO: "Os efeitos de recuperação de PV aumentam em +2" — no heal-amount bonus hook exists: nothing
    //  asks the healer's Títulos how much a heal is worth. CombatantSheet#heal(int, HealingSource)
    //  now knows its healer, so the missing piece is the hook, not the healer.
    MEDICO_DE_GUERRA(
            "Os efeitos de recuperação de PV aumentam em +2.",
            fixed(0), ActionCost.NONE, Optional.empty()),

    // "Apenas 'Curandeiros' podem adquirir esta especialização" — enforced by
    // Curandeiro#grantSpecialization. Passive.
    // TODO: "O GD de suas magias que permitem que outros personagens recuperarem PV é reduzido em -1
    //  Nível" — no Título hook reaches a Magia's casting GD (SpellCastingServiceImpl's
    //  castingDifficultyReduction scans no Título).
    MARTIR_ALTRUISTA(
            "O GD de suas magias que permitem que outros personagens recuperarem PV é reduzido em -1 Nível.",
            fixed(0), ActionCost.NONE, Optional.empty());

    private final String description;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
