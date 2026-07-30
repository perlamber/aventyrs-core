package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interactable;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;

/**
 * Requests an Attention Perícia test. Only meaningful for a {@link CharacterSheet} target,
 * since only a Character carries attributes and skills — this Interaction computes the
 * roll bonus itself, looking up the target's own trained Attention CharacterSkill (or
 * defaulting to untrained, which carries {@link Skill#UNTRAINED_PENALTY}).
 */
public class AttentionInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;

    public AttentionInteraction() {
        this(new CharacterSkillServiceImpl());
    }

    public AttentionInteraction(final CharacterSkillService characterSkillService) {
        this.characterSkillService = characterSkillService;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        
        Character character = target.getCharacter();
        CharacterSkill attentionSkill = findCharacterSkill(character);
        int bonus = characterSkillService.getValueForRoll(attentionSkill, character.getAttributes(), character.getRace());
        return InteractionResult.builder()
                .resultStatus(character.getStatus())
                .skillRollBonus(bonus)
                .build();
    }

    /**
     * The Character's own CharacterSkill for Attention, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.ATTENTION);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new Attention())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
