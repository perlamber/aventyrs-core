package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
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
 * Requests an Ataque à Distância Perícia test. Only meaningful for a {@link CharacterSheet}
 * target, since only a Character carries attributes and skills — this Interaction computes
 * the roll bonus itself, looking up the target's own trained Ataque à Distância
 * CharacterSkill (or defaulting to untrained, which carries {@link Skill#UNTRAINED_PENALTY}).
 * The rules text notes this Perícia is compared against a target's DF or DM rather than a
 * fixed GD, but that target-side lookup/conversion is left to a layer above this core — this
 * Interaction still only computes {@code skillRollBonus}/{@code difficultyReduction} exactly
 * like every other {@code <Skill>Interaction}. When the roll delivers a Magia rather than a
 * mundane attack, see {@link org.aventyrs.core.magic.SpellCastingService}. If the character
 * has a {@code SkillCompetencyAbility} for this same skill whose
 * {@link SkillCompetencyAbility#getSubstituteAttributeDomain()} isn't empty (e.g.
 * {@code AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO}), that Attribute is used in place
 * of Ataque à Distância's normal Destreza for {@link CharacterSkillService#getValueForRoll}.
 */
public class AtaqueADistanciaInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;

    public AtaqueADistanciaInteraction() {
        this(new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    public AtaqueADistanciaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character character = target.getCharacter();
        CharacterSkill ataqueADistanciaSkill = findCharacterSkill(character);
        int graduationValue = ataqueADistanciaSkill.getGraduation().getGraduationValue();

        AttributeDomain attributeDomain = SkillCompetencyAbility.resolveAttributeDomain(
                character.getSkillCompetencyAbilities(), SkillType.ATAQUE_A_DISTANCIA,
                ataqueADistanciaSkill.getSkill().getAttributeDomain());

        int bonus = characterSkillService.getValueForRoll(ataqueADistanciaSkill, character.getAttributes(), character.getRace(), attributeDomain);
        bonus += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_BONUS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.SKILL_ROLL_BONUS);
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(AtaqueADistanciaExcellency.class, graduationValue);
        bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.SKILL_ROLL_BONUS);

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(AtaqueADistanciaExcellency.class, graduationValue);
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
     * The Character's own CharacterSkill for Ataque à Distância, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.ATAQUE_A_DISTANCIA);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new AtaqueADistancia())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
