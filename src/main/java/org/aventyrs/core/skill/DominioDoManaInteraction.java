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
 * Requests a Domínio do Mana Perícia test. Only meaningful for a {@link CharacterSheet}
 * target, since only a Character carries attributes and skills — this Interaction computes
 * the roll bonus itself, looking up the target's own trained Domínio do Mana CharacterSkill
 * (or defaulting to untrained, which carries {@link Skill#UNTRAINED_PENALTY}). Domínio do
 * Mana rolls only ever happen after an already-separate, already-successful Magia casting
 * roll; this Interaction only ever computes Domínio do Mana's own bonus/difficultyReduction,
 * never that separate casting roll, which this core doesn't model yet (no {@code Magia}
 * entity or casting-resolution engine exists).
 */
public class DominioDoManaInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;

    public DominioDoManaInteraction() {
        this(new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    public DominioDoManaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character character = target.getCharacter();
        CharacterSkill dominioDoManaSkill = findCharacterSkill(character);
        int graduationValue = dominioDoManaSkill.getGraduation().getGraduationValue();

        int bonus = characterSkillService.getValueForRoll(dominioDoManaSkill, character.getAttributes(), character.getRace());
        bonus += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_BONUS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.SKILL_ROLL_BONUS);
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(DominioDoManaExcellency.class, graduationValue);
        bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.SKILL_ROLL_BONUS);

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(DominioDoManaExcellency.class, graduationValue);
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
     * The Character's own CharacterSkill for Domínio do Mana, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.DOMINIO_DO_MANA);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new DominioDoMana())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
