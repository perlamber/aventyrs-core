package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.ATTRIBUTE_ABILITY_ALREADY_CHOSEN;
import static org.aventyrs.core.util.TranslatableMessages.NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE;
import static org.aventyrs.core.util.TranslatableMessages.SKILL_NOT_TRAINED;
import static org.aventyrs.core.util.TranslatableMessages.SKILL_TRAIT_SKILL_TYPE_MISMATCH;

public class AttributeAbilityServiceImpl implements AttributeAbilityService {

    @Override
    public int getUnlockedAbilitySlots(final int attributeBase) {
        if (attributeBase >= SECOND_ABILITY_ATTRIBUTE_BASE) {
            return 2;
        }
        if (attributeBase >= FIRST_ABILITY_ATTRIBUTE_BASE) {
            return 1;
        }
        return 0;
    }

    @Override
    public void validateChoice(final int attributeBase, final List<AttributeAbility> alreadyChosen, final AttributeAbility choice) throws IllegalOperationException {
        if (alreadyChosen.contains(choice)) {
            throw new IllegalOperationException(ATTRIBUTE_ABILITY_ALREADY_CHOSEN);
        }
        long chosenOfSameDomain = alreadyChosen.stream()
                .filter(ability -> ability.getAttributeDomain() == choice.getAttributeDomain())
                .count();
        if (chosenOfSameDomain >= getUnlockedAbilitySlots(attributeBase)) {
            throw new IllegalOperationException(NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE);
        }
    }

    @Override
    public AttributeAbilityGrantResult grantAttributeAbility(final Character character, final AttributeAbility ability) throws IllegalOperationException {
        int attributeBase = character.getAttributes().getAttribute(ability.getAttributeDomain()).getBase();
        validateChoice(attributeBase, character.getAttributeAbilities(), ability);

        Character.CharacterBuilder builder = character.toBuilder().attributeAbility(ability);
        ability.resolvePermanentEgoGain().ifPresent(domain ->
                builder.egos(character.getEgos().withVariableBonus(domain, 1)));
        ability.resolveActiveAbility().ifPresent(builder::activeAbility);
        Character granted = builder.build();

        return AttributeAbilityGrantResult.builder()
                .character(granted)
                .pendingSkillTraitChoices(ability.resolvePendingSkillTraitChoices(granted))
                .build();
    }

    @Override
    public Character grantSkillTraitChoice(final Character character, final SkillType skillType, final SkillCompetencyAbility competencyAbility, final SkillSpecialization specialization) throws IllegalOperationException {
        CharacterSkill trained = character.getSkills().get(skillType);
        if (trained == null) {
            throw new IllegalOperationException(SKILL_NOT_TRAINED);
        }
        if (competencyAbility.getSkillType() != skillType || specialization.getSkillType() != skillType) {
            throw new IllegalOperationException(SKILL_TRAIT_SKILL_TYPE_MISMATCH);
        }

        CharacterSkill updated = trained.toBuilder()
                .specializations(Stream.concat(trained.getSpecializations().stream(), Stream.of(specialization)).toList())
                .build();

        return character.toBuilder()
                .skill(skillType, updated)
                .skillCompetencyAbility(competencyAbility)
                .build();
    }
}
