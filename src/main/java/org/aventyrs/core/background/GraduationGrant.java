package org.aventyrs.core.background;

import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * "+1 Graduação" in {@code picks} distinct Perícias among {@code options} — one clause of an
 * Antecedente's Perícias line. A fixed grant ("+1 Graduação em Atletismo") is one option picked
 * once; "+1 Graduação em 2 Perícias, escolhidas entre Artes, Conhecimentos ou Medicina e Cura" is
 * three options, two picks.
 *
 * <p>The Graduação <b>stacks</b> on whatever the character already has (a table ruling): a trained
 * Perícia goes from 1 to 2, an untrained one becomes trained at 1 without spending a creation
 * training slot.
 *
 * @param options the Perícias this grant may land on, in rules-text order
 * @param picks   how many of them receive the Graduação — exactly, not at most
 */
public record GraduationGrant(List<SkillType> options, int picks) {

    public GraduationGrant {
        options = List.copyOf(options);
        if (picks < 1 || picks > options.size()) {
            throw new IllegalArgumentException("picks must be within 1.." + options.size());
        }
    }

    /** "+1 Graduação em X". */
    public static GraduationGrant fixed(final SkillType skillType) {
        return new GraduationGrant(List.of(skillType), 1);
    }

    /** "+1 Graduação em {@code picks} Perícias, escolhidas entre …". */
    public static GraduationGrant choose(final int picks, final SkillType... options) {
        return new GraduationGrant(List.of(options), picks);
    }

    /** Whether the player picks anything here — false when every option is granted. */
    public boolean isChoice() {
        return options.size() > picks;
    }
}
