package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.List;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;

/**
 * Requests a Furtividade Perícia test. Only meaningful for a {@link CharacterSheet} target,
 * since only a Character carries attributes and skills — this Interaction computes the roll
 * bonus itself, looking up the target's own trained Furtividade CharacterSkill (or
 * defaulting to untrained, which carries {@link Skill#UNTRAINED_PENALTY}). Which of
 * Furtividade's specializations the roll is for doesn't change the bonus — see
 * {@link FurtividadeSpecialization} — so it isn't tracked here. The rules text also has an
 * opposed Atenção roll identify a hidden character; that resolution is left to a layer above
 * this core, same as every other cross-character contest.
 */
public class FurtividadeInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;

    public FurtividadeInteraction() {
        this(new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    public FurtividadeInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character character = target.getCharacter();
        CharacterSkill furtividadeSkill = findCharacterSkill(character);
        int graduationValue = furtividadeSkill.getGraduation().getGraduationValue();

        int bonus = characterSkillService.getValueForRoll(furtividadeSkill, character.getAttributes(), character.getRace());
        bonus += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_BONUS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.SKILL_ROLL_BONUS);
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(FurtividadeExcellency.class, graduationValue);
        bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.SKILL_ROLL_BONUS);
        bonus += target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS);

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(FurtividadeExcellency.class, graduationValue);
        difficultyReduction += character.getSkillCompetencyAbilities().stream()
                .mapToInt(SkillCompetencyAbility::getDifficultyReduction)
                .sum();

        return InteractionResult.builder()
                .resultStatus(character.getStatus())
                .skillRollBonus(bonus)
                .difficultyReduction(difficultyReduction)
                .build();
    }

    /**
     * The Character's own CharacterSkill for Furtividade, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.FURTIVIDADE);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new Furtividade())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
