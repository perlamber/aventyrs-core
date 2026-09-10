package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;

import java.util.List;

/**
 * A choice between {@link ActiveAbility}s that a Talento makes its holder pick at acquisition —
 * "Escolha 2 Formas Metamórficas" ({@code VampiricoFeat#METAMORFOSE_DRACULEA}).
 *
 * <p><b>This exists so a client does not have to know which Talentos are special.</b> A UI
 * offering the catalog asks every constant {@code Feat#resolveActiveAbilityChoice(Character)}; a
 * non-{@code null} answer means "this one cannot be taken plain — the player must pick {@link
 * #picks()} of {@link #options()} first". Without it the only way to discover the options was to
 * know that a particular acquired-form class existed and read a static off it, which is not
 * discovery at all.
 *
 * <p>{@code options} are the actual {@link ActiveAbility} instances, not identifiers — each one
 * already answers {@code getDescription()}, its Pontos de Ação/Magia/Vida cost, its Duração and
 * its Resfriamento, which is everything a UI needs to render the choice. They are also the very
 * instances the acquired Talento will hold, since {@code ActiveAbilityService#activate} matches by
 * {@code ==}: what the client is shown is what it picks, and what it picks is what gets granted.
 *
 * <p>{@code options} is already filtered for the holder — a Rakshasa is not offered Névoa — so a
 * client never has to reimplement a Talento's own eligibility rules to present its choice
 * correctly.
 *
 * <p><b>Scope.</b> This covers Talentos whose acquisition choice is between activatable abilities.
 * The catalog's other acquisition choices — a Perícia ({@code FocoEmPericiaFeat}), a terreno,
 * a weapon type, a {@code SkillTrait}, an {@code AttributeAbility} — are still undiscoverable in
 * the same way this one was, each recorded only on its own acquired-form class. A general
 * descriptor covering all of them is the obvious next step and is deliberately not invented here
 * ahead of a caller that needs it.
 *
 * @param picks   how many of {@code options} must be chosen — exactly, not at most
 * @param options every ability this holder may legally pick from
 */
public record ActiveAbilityChoice(int picks, List<ActiveAbility> options) {

    public ActiveAbilityChoice {
        options = List.copyOf(options);
    }
}
