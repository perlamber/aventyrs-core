package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Requests an Artes Perícia test. Which of Artes' specializations the roll is for doesn't
 * change the bonus — see {@link ArtesSpecialization} — so it isn't tracked here. See {@link
 * AbstractSkillInteraction} for how the roll bonus/difficultyReduction are actually computed.
 *
 * <p>A character holding {@link ArtesCompetencyAbility#DOM_BARDICO} additionally has this
 * roll set {@code temporaryBonusModifierType} ({@link ModifierType#SKILL_ROLL_BONUS}, since
 * this ability's own rules text is unrestricted — "rolagens de Perícias", not one specific
 * Perícia) and {@code temporaryBonusRounds} (1 Rodada normally, 2 at 5 Graduações in Artes, 3
 * at 10) on the result. {@code temporaryBonusValue} stays {@code null} — that's a lookup by
 * which GD tier this roll reaches, which needs a roll-resolution-vs-{@link DifficultyLevel}
 * engine this core still doesn't have — see the ability's own TODO.
 */
public class ArtesInteraction extends AbstractSkillInteraction {

    public ArtesInteraction() {
        super(SkillType.ARTES);
    }

    public ArtesInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ARTES, characterSkillService, modifierResolver);
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        InteractionResult result = super.applyTo(target);
        Character character = target.getCharacter();
        if (!character.getSkillCompetencyAbilities().contains(ArtesCompetencyAbility.DOM_BARDICO)) {
            return result;
        }
        return result.toBuilder()
                .temporaryBonusModifierType(ModifierType.SKILL_ROLL_BONUS)
                .temporaryBonusRounds(domBardicoRounds(character))
                .build();
    }

    /**
     * DOM_BARDICO's own duration: 1 Rodada normally, 2 once Artes reaches 5 Graduações, 3 at
     * 10. An untrained Artes shouldn't happen — the ability requires training it — but this
     * codebase doesn't enforce that prerequisite (see CLAUDE.md), so it falls back to 1
     * rather than failing.
     */
    private int domBardicoRounds(final Character character) {
        CharacterSkill artesSkill = character.getSkills().get(SkillType.ARTES);
        int graduationValue = artesSkill != null ? artesSkill.getGraduation().getGraduationValue() : Skill.UNTRAINED_PENALTY;
        if (graduationValue >= 10) {
            return 3;
        }
        if (graduationValue >= 5) {
            return 2;
        }
        return 1;
    }
}
