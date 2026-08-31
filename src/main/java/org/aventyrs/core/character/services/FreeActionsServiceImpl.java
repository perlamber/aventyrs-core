package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.item.Item;

import java.util.List;
import java.util.Map;

public class FreeActionsServiceImpl implements FreeActionsService {

    private final ModifierResolver modifierResolver;

    public FreeActionsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public FreeActionsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalFreeActions(final Character character) {
        return Math.max(0, permanentFreeActions(character));
    }

    @Override
    public int getTotalFreeActions(final CombatantSheet sheet, final int turnNumber) {
        return getTotalFreeActions(sheet, turnNumber, null);
    }

    @Override
    public int getTotalFreeActions(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        Character character = sheet.getCharacter();
        int baseline = permanentFreeActions(character) + sheet.getTemporaryBonus(ModifierType.FREE_ACTIONS);
        return Math.max(0, character.getActionProfile().adjustFreeActions(baseline, turnNumber, sceneContext));
    }

    /**
     * The fixed counter plus the three-source {@code FREE_ACTIONS} scan — unclamped, mirroring
     * {@code ReactionsServiceImpl}'s own private helper, so the single clamp happens after the
     * {@link org.aventyrs.core.action.ActionProfile} has had its say.
     */
    private int permanentFreeActions(final Character character) {
        int total = character.getFreeActions();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.FREE_ACTIONS);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.FREE_ACTIONS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.FREE_ACTIONS);
        }
        for (Item item : character.getEquipment()) {
            total += item.resolveEnhancementBonus(ModifierType.FREE_ACTIONS, null, character);
        }
        return total;
    }
}
