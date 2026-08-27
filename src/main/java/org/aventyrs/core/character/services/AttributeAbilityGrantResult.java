package org.aventyrs.core.character.services;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Set;

/**
 * The result of {@link AttributeAbilityService#grantAttributeAbility} — the granted {@link
 * Character}, plus every {@link SkillType} the granted ability still owes the player a choice
 * for (see {@code AttributeAbility#resolvePendingSkillTraitChoices}).
 *
 * <p>A non-empty {@link #pendingSkillTraitChoices} is a caller's (an API/UI layer's) cue to
 * prompt the player, and the other two fields say how: {@link #pendingSkillTraitChoiceLimit}
 * is how many of those candidates they may actually take (already {@code min}-ed against the
 * candidate list — e.g. 3 for {@code GnoseAbility#DOMINIO_DO_CONHECIMENTO}'s "até 3", the whole
 * list for {@code CharismaAbility#CHARME}, which owes one per candidate), and {@link
 * #pendingSkillTraitKinds} is which trait(s) each pick owes — {@code
 * SkillTraitKind#SPECIALIZATION} alone for a specialization-only ability, both kinds for a
 * dual-trait one like CHARME. Resolve each pick via {@link
 * AttributeAbilityService#grantCompetencyAbilityChoice} and/or {@link
 * AttributeAbilityService#grantSpecializationChoice} accordingly. All three are empty/zero for
 * every ability that doesn't defer a choice this way — the common case.
 */
@Getter
@Builder
public class AttributeAbilityGrantResult {
    private final Character character;

    @Builder.Default
    private final List<SkillType> pendingSkillTraitChoices = List.of();

    /** How many of {@link #pendingSkillTraitChoices} the player may actually resolve — never
     * larger than that list, and 0 whenever it's empty. */
    @Builder.Default
    private final int pendingSkillTraitChoiceLimit = 0;

    @Builder.Default
    private final Set<SkillTraitKind> pendingSkillTraitKinds = Set.of();
}
