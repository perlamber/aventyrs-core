package org.aventyrs.core.background;

import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillTraitKind;

import java.util.List;

/**
 * An Especialização or Habilidade de Competência an Antecedente hands over — {@code picks} of
 * {@code options}. A fixed grant ("Recebe a Habilidade de Competência Anfíbio de Atletismo") has
 * one option; "uma Especialização escolhida entre Alfaiataria e Curtume, Metalurgia ou Joalheria"
 * has three and one pick.
 *
 * <p>Options are resolved per character by {@link Background#resolveTraitGrants}, already
 * <b>without the traits the character holds</b> — an Antecedente never hands over a second copy.
 * Should that leave fewer options than picks, {@code picks} shrinks to match rather than owing a
 * pick nobody can make; a fixed trait already held simply grants nothing new.
 *
 * @param kind    which trait kind this owes
 * @param options every trait this character may receive here
 * @param picks   how many of them are received — exactly, not at most
 */
public record TraitGrant(SkillTraitKind kind, List<SkillTrait> options, int picks) {

    public TraitGrant {
        options = List.copyOf(options);
        picks = Math.min(picks, options.size());
    }

    /** A trait granted outright. */
    public static TraitGrant fixed(final SkillTraitKind kind, final SkillTrait trait) {
        return new TraitGrant(kind, List.of(trait), 1);
    }

    /** {@code picks} of options. */
    public static TraitGrant choose(final SkillTraitKind kind, final int picks, final List<? extends SkillTrait> options) {
        return new TraitGrant(kind, List.copyOf(options), picks);
    }

    /** Whether the player picks anything here — false when every option is granted. */
    public boolean isChoice() {
        return options.size() > picks;
    }
}
