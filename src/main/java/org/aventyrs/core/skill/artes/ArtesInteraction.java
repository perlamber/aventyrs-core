package org.aventyrs.core.skill.artes;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * Requests an Artes Perícia test. Which of Artes' specializations ({@link ArtesSpecialization})
 * the roll is for doesn't change {@code skillRollBonus}/{@code difficultyReduction} — a held
 * one can still be requested via {@code SkillRoll#getRequestedAbility()}, switching
 * {@code reachedDifficultyLevel} to expert thresholds; see {@link AbstractSkillInteraction} for
 * how that and the roll bonus/difficultyReduction are actually computed.
 *
 * <p>A character holding {@link ArtesCompetencyAbility#DOM_BARDICO} additionally has this roll
 * report one {@link Blessing} on {@link InteractionResult#getBlessings()} once a {@link
 * SkillRoll} reaching at least GD Médio was supplied: {@link ModifierType#SKILL_ROLL_BONUS}
 * (this ability's own rules text is unrestricted — "rolagens de Perícias", not one specific
 * Perícia), {@link TargetScope#ALLIES} ("concedendo... a eles (mas não a você)"), a duration of
 * 1 Rodada normally (2 at 5 Graduações in Artes, 3 at 10), and a value looked up via {@link
 * #domBardicoBonusValue(DifficultyLevel)} from the {@code reachedDifficultyLevel} {@code
 * AbstractSkillInteraction} already resolves: MEDIUM→1, HARD→2, VERY_HARD→3,
 * UNLIKELY/UNIMAGINABLE→4 (UNIMAGINABLE isn't named in the rules text; treated as inheriting
 * UNLIKELY's value until MIRACLE is reached — an inference, not confirmed text), MIRACLE→5.
 * Below MEDIUM (VERY_EASY/EASY), or when no roll was supplied at all, no {@code Blessing} is
 * reported — there's nothing coherent to grant without a resolved value, so {@link
 * InteractionResult#getBlessings()} stays {@code null} in that case, same as when DOM_BARDICO
 * isn't held at all.
 */
public class ArtesInteraction extends AbstractSkillInteraction {

    public ArtesInteraction() {
        super(SkillType.ARTES);
    }

    public ArtesInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ARTES, characterSkillService, modifierResolver);
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final SkillRoll skillRoll) {
        InteractionResult result = super.applyTo(target, sceneContext, skillRoll);
        Character character = target.getCharacter();
        if (!character.getSkillCompetencyAbilities().contains(ArtesCompetencyAbility.DOM_BARDICO)) {
            return result;
        }
        Integer bonusValue = domBardicoBonusValue(result.getReachedDifficultyLevel());
        if (bonusValue == null) {
            return result;
        }
        Blessing domBardicoBlessing = new Blessing(ModifierType.SKILL_ROLL_BONUS, bonusValue, domBardicoRounds(character), TargetScope.ALLIES, ArtesCompetencyAbility.DOM_BARDICO.name());
        return result.toBuilder()
                .blessings(List.of(domBardicoBlessing))
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

    /**
     * DOM_BARDICO's own bonus table: GD Médio +1, Difícil +2, Muito Difícil +3, Improvável
     * +4, Milagre +5. UNIMAGINABLE isn't named in the rules text (it falls between Improvável
     * and Milagre); treated here as inheriting UNLIKELY's +4 until MIRACLE is actually
     * reached — an inference, not confirmed rules text. Below MEDIUM, or when no
     * {@link SkillRoll} was supplied at all ({@code reachedDifficultyLevel} is {@code null}),
     * no bonus applies.
     */
    private static Integer domBardicoBonusValue(final DifficultyLevel reachedDifficultyLevel) {
        if (reachedDifficultyLevel == null) {
            return null;
        }
        return switch (reachedDifficultyLevel) {
            case MEDIUM -> 1;
            case HARD -> 2;
            case VERY_HARD -> 3;
            case UNLIKELY, UNIMAGINABLE -> 4;
            case MIRACLE -> 5;
            default -> null;
        };
    }
}
