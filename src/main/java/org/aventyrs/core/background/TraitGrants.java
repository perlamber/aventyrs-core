package org.aventyrs.core.background;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillTraitCatalog;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The building blocks every {@link Background#resolveTraitGrants} is written in. Each returns
 * {@code null} for "nothing owed here" (a condition not met, or every option already held), and
 * {@link #of} drops those — so a constant reads as its rules text, one call per clause.
 */
final class TraitGrants {

    private TraitGrants() {
    }

    static List<TraitGrant> of(final TraitGrant... grants) {
        return Arrays.stream(grants).filter(Objects::nonNull).toList();
    }

    /** Trained in skillType at all — holding any Graduação in it. */
    static boolean trained(final Character character, final SkillType skillType) {
        CharacterSkill skill = character.getSkills().get(skillType);
        return skill != null && skill.getGraduation().getGraduationValue() >= 1;
    }

    static boolean holds(final Character character, final SkillTrait trait) {
        if (trait instanceof SkillSpecialization specialization) {
            return character.getSpecializations(specialization.getSkillType()).contains(specialization);
        }
        return SkillCompetencyAbility.allFor(character).contains(trait);
    }

    static SkillTraitKind kindOf(final SkillTrait trait) {
        return trait instanceof SkillSpecialization ? SkillTraitKind.SPECIALIZATION : SkillTraitKind.COMPETENCY_ABILITY;
    }

    /** "Recebe X" — nothing when already held. */
    static TraitGrant fixed(final Character character, final SkillTrait trait) {
        return holds(character, trait) ? null : TraitGrant.fixed(kindOf(trait), trait);
    }

    /** "Se treinado em skillType recebe X". */
    static TraitGrant ifTrained(final Character character, final SkillType skillType, final SkillTrait trait) {
        return trained(character, skillType) ? fixed(character, trait) : null;
    }

    /** "{@code picks} escolhida(s) entre …". */
    static TraitGrant choose(final Character character, final int picks, final List<? extends SkillTrait> options) {
        List<SkillTrait> open = options.stream().map(SkillTrait.class::cast)
                .filter(trait -> !holds(character, trait)).toList();
        return open.isEmpty() ? null : TraitGrant.choose(kindOf(options.get(0)), picks, open);
    }

    /** "{@code picks} Especialização/Habilidade de Competência de" one of skills — any of them. */
    static TraitGrant anyOf(final Character character, final SkillTraitKind kind, final int picks,
                            final Collection<SkillType> skills) {
        List<SkillTrait> open = skills.stream()
                .flatMap(skill -> SkillTraitCatalog.traitsOf(skill, kind).stream())
                .filter(trait -> !holds(character, trait))
                .toList();
        return open.isEmpty() ? null : TraitGrant.choose(kind, picks, open);
    }

    /** {@link #anyOf} gated on being trained in skillType. */
    static TraitGrant anyOfIfTrained(final Character character, final SkillTraitKind kind, final SkillType skillType) {
        return trained(character, skillType) ? anyOf(character, kind, 1, List.of(skillType)) : null;
    }

    /** Only the attack Perícias among skills. */
    static List<SkillType> attackSkills(final Collection<SkillType> skills) {
        return skills.stream().filter(SkillType::isAttackSkill).toList();
    }
}
