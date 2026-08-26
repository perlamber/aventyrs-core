package org.aventyrs.core.action;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class ActionPointsServiceImpl implements ActionPointsService {

    private final ModifierResolver modifierResolver;

    public ActionPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public ActionPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getMaxActionPoints(final Character character, final int turnNumber) {
        return Math.max(0, character.getActionProfile()
                .adjustActionPoints(permanentActionPoints(character), turnNumber));
    }

    @Override
    public int getMaxActionPoints(final CombatantSheet sheet, final int turnNumber) {
        return getMaxActionPoints(sheet, turnNumber, null);
    }

    @Override
    public int getMaxActionPoints(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        Character character = sheet.getCharacter();
        int baseline = permanentActionPoints(character) + sheet.getTemporaryBonus(ModifierType.ACTION_POINTS);
        return Math.max(0, character.getActionProfile().adjustActionPoints(baseline, turnNumber, sceneContext));
    }

    /**
     * Everything below the {@link ActionProfile} adjustment and below any sheet-level
     * {@code TemporaryBonus}: the fixed counter, the three-source {@code ACTION_POINTS} scan,
     * and the character's own plain temporary bonus field. Shared by every overload so the
     * profile is only ever applied once, and always last.
     */
    private int permanentActionPoints(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.ACTION_POINTS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.ACTION_POINTS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.ACTION_POINTS);
        }
        return character.getActionPoints() + bonus + character.getTemporaryActionPointsBonus();
    }

    @Override
    public int getSkillRollCost(final Character character, final int turnNumber) {
        int adjustment = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_COST);
        int adjusted = character.getActionProfile()
                .adjustSkillRollCost(DEFAULT_SKILL_ROLL_COST + adjustment, turnNumber);
        return Math.max(0, adjusted);
    }

    @Override
    public boolean canAffordSkillRoll(final Character character, final int turnNumber) {
        return getMaxActionPoints(character, turnNumber) >= getSkillRollCost(character, turnNumber);
    }

    @Override
    public boolean canAffordSkillRoll(final CombatantSheet sheet, final int turnNumber) {
        return canAffordSkillRoll(sheet, turnNumber, null);
    }

    @Override
    public boolean canAffordSkillRoll(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        return getMaxActionPoints(sheet, turnNumber, sceneContext)
                >= getSkillRollCost(sheet.getCharacter(), turnNumber);
    }
}
