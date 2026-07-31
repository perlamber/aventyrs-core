package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;

/**
 * Requests an Artes Perícia test. Only meaningful for a {@link CharacterSheet} target, since
 * only a Character carries attributes and skills — this Interaction computes the roll bonus
 * itself, looking up the target's own trained Artes CharacterSkill (or defaulting to
 * untrained, which carries {@link Skill#UNTRAINED_PENALTY}). Which of Artes'
 * specializations the roll is for doesn't change the bonus — see
 * {@link ArtesSpecialization} — so it isn't tracked here.
 */
public class ArtesInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;

    public ArtesInteraction() {
        this(new CharacterSkillServiceImpl());
    }

    public ArtesInteraction(final CharacterSkillService characterSkillService) {
        this.characterSkillService = characterSkillService;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character character = target.getCharacter();
        CharacterSkill artesSkill = findCharacterSkill(character);
        int bonus = characterSkillService.getValueForRoll(artesSkill, character.getAttributes(), character.getRace());
        int graduationValue = artesSkill.getGraduation().getGraduationValue();
        int difficultyReduction = SkillExcellency.totalDifficultyReduction(ArtesExcellency.class, graduationValue);
        return InteractionResult.builder()
                .resultStatus(character.getStatus())
                .skillRollBonus(bonus)
                .difficultyReduction(difficultyReduction)
                .build();
    }

    /**
     * The Character's own CharacterSkill for Artes, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.ARTES);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new Artes())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
