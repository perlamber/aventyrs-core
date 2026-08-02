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
 * Requests an Esquiva e Aparar Perícia test. Only meaningful for a {@link CharacterSheet}
 * target, since only a Character carries attributes and skills — this Interaction computes
 * the roll bonus itself, looking up the target's own trained Esquiva e Aparar CharacterSkill
 * (or defaulting to untrained, which carries {@link Skill#UNTRAINED_PENALTY}).
 *
 * <p>TODO: the rules text also has the character's equipped armor Categoria reduce or zero
 * out this roll's Destreza contribution (full value with Leve/no armor, half with Média,
 * none with Pesada — see {@link EsquivaEApararCompetencyAbility#ENCOURACADO_E_VELOZ} for how
 * that's lessened). No Equipamento/Armadura tracking exists on {@code Character} yet, so this
 * Interaction always uses the full attribute total via
 * {@link CharacterSkillService#getValueForRoll}, the same as every other {@code
 * <Skill>Interaction} — it doesn't yet know what, if anything, the character has equipped.
 */
public class EsquivaEApararInteraction implements Interaction<CharacterSheet> {

    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;

    public EsquivaEApararInteraction() {
        this(new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    public EsquivaEApararInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        Character character = target.getCharacter();
        CharacterSkill esquivaEApararSkill = findCharacterSkill(character);
        int graduationValue = esquivaEApararSkill.getGraduation().getGraduationValue();

        int bonus = characterSkillService.getValueForRoll(esquivaEApararSkill, character.getAttributes(), character.getRace());
        bonus += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_BONUS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.SKILL_ROLL_BONUS);
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(EsquivaEApararExcellency.class, graduationValue);
        bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.SKILL_ROLL_BONUS);

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(EsquivaEApararExcellency.class, graduationValue);
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
     * The Character's own CharacterSkill for Esquiva e Aparar, or a fresh one carrying
     * {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(SkillType.ESQUIVA_E_APARAR);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(new EsquivaEAparar())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
