package org.aventyrs.core.title.bruxo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Bruxo's own catalog of Especializações — exactly two, a player may hold both, one, or neither.
 * <b>Both are passive</b>, and both teach a Misticismo "à sua escolha".
 *
 * <p>Table ruling (2026-10-05): an Especialização does <b>not</b> count among the "Habilidades de
 * Bruxo" — see {@link Bruxo#getBruxoAbilityCount()}.
 */
@Getter
@AllArgsConstructor
public enum BruxoSpecialization implements AventyrTitleSpecialization, MisticismoTeacher {

    // "Apenas 'Bruxos' podem adquirir esta especialização" — enforced by Bruxo#grantSpecialization.
    // Passive. The Misticismo is real (Bruxo#getGrantedMimetizedSpells). The GD clause is read off the
    // summoner by Bruxo#resolveSummonEnhancement.
    ILUMINADO(
            "Você aprende um novo Misticismo à sua escolha. As Rolagens de Perícias de Ataque de suas invocações " +
            "têm a GD reduzida em -1 nível.",
            fixed(0), ActionCost.NONE, Optional.empty(), Optional.of(MisticismoFilter.ANY)),

    // "Apenas 'Bruxos' podem adquirir esta especialização" — enforced by Bruxo#grantSpecialization.
    // Passive. The Misticismo is real (Bruxo#getGrantedMimetizedSpells). The GD clause is read off the
    // summoner by Bruxo#resolveSummonEnhancement.
    ORACULO_ABISSAL(
            "Você aprende um novo Misticismo à sua escolha. A GD das rolagens de Defesas de suas Invocações é " +
            "reduzida em -1 nível.",
            fixed(0), ActionCost.NONE, Optional.empty(), Optional.of(MisticismoFilter.ANY));

    private final String description;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<MisticismoFilter> misticismoFilter;
}
