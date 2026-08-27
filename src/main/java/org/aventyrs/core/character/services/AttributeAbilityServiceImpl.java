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
        ability.resolveGrantedSkillTraining(character).forEach(skillType ->
                builder.skill(skillType, new CharacterSkill(skillType.newSkillInstance())));
        Character granted = builder.build();

        List<SkillType> pendingChoices = ability.resolvePendingSkillTraitChoices(granted);
        return AttributeAbilityGrantResult.builder()
                .character(granted)
                .pendingSkillTraitChoices(pendingChoices)
                .pendingSkillTraitChoiceLimit(Math.min(
                        ability.resolvePendingSkillTraitChoiceLimit().orElse(pendingChoices.size()),
                        pendingChoices.size()))
                .pendingSkillTraitKinds(ability.resolvePendingSkillTraitKinds())
                .build();
    }

    @Override
    public Character grantCompetencyAbilityChoice(final Character character, final SkillType skillType, final SkillCompetencyAbility competencyAbility) throws IllegalOperationException {
        lookupTrainedSkill(character, skillType);
        if (competencyAbility.getSkillType() != skillType) {
            throw new IllegalOperationException(SKILL_TRAIT_SKILL_TYPE_MISMATCH);
        }

        return character.toBuilder()
                .skillCompetencyAbility(competencyAbility)
                .build();
    }

    @Override
    public Character grantSpecializationChoice(final Character character, final SkillType skillType, final SkillSpecialization specialization) throws IllegalOperationException {
        CharacterSkill trained = lookupTrainedSkill(character, skillType);
        if (specialization.getSkillType() != skillType) {
            throw new IllegalOperationException(SKILL_TRAIT_SKILL_TYPE_MISMATCH);
        }

        return character.toBuilder()
                .skill(skillType, withSpecialization(trained, specialization))
                .build();
    }

    private CharacterSkill lookupTrainedSkill(final Character character, final SkillType skillType) throws IllegalOperationException {
        CharacterSkill trained = character.getSkills().get(skillType);
        if (trained == null) {
            throw new IllegalOperationException(SKILL_NOT_TRAINED);
        }
        return trained;
    }

    private CharacterSkill withSpecialization(final CharacterSkill trained, final SkillSpecialization specialization) {
        return trained.toBuilder()
                .specializations(Stream.concat(trained.getSpecializations().stream(), Stream.of(specialization)).toList())
                .build();
    }
}
