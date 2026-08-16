package org.aventyrs.core.character.services;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * The result of {@link AttributeAbilityService#grantAttributeAbility} — the granted {@link
 * Character}, plus every {@link SkillType} the granted ability still owes the player a choice
 * for (see {@code AttributeAbility#resolvePendingSkillTraitChoices}).
 *
 * <p>A non-empty {@link #pendingSkillTraitChoices} means: for each {@link SkillType} listed,
 * both a {@code SkillCompetencyAbility} and a {@code SkillSpecialization} are still owed —
 * this is a caller's (an API/UI layer's) cue to prompt the player once per entry (e.g. open
 * one choice modal per {@link SkillType}), then resolve each pick via {@link
 * AttributeAbilityService#grantSkillTraitChoice}. Empty for every ability that doesn't defer
 * a choice this way — the common case.
 */
@Getter
@Builder
public class AttributeAbilityGrantResult {
    private final Character character;

    @Builder.Default
    private final List<SkillType> pendingSkillTraitChoices = List.of();
}
