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

public class ReactionsServiceImpl implements ReactionsService {

    private final ModifierResolver modifierResolver;

    public ReactionsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public ReactionsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalReactions(final Character character) {
        return Math.max(0, permanentReactions(character));
    }

    @Override
    public int getTotalReactions(final CombatantSheet sheet, final int turnNumber) {
        return getTotalReactions(sheet, turnNumber, null);
    }

    @Override
    public int getTotalReactions(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        Character character = sheet.getCharacter();
        int baseline = permanentReactions(character) + sheet.getTemporaryBonus(ModifierType.REACTIONS);
        return Math.max(0, character.getActionProfile().adjustReactions(baseline, turnNumber, sceneContext));
    }

    /**
     * The fixed counter plus the three-source {@code REACTIONS} scan — unclamped, so the
     * {@link org.aventyrs.core.action.ActionProfile} adjustment and any sheet-level
     * {@code TemporaryBonus} still see a genuine deficit rather than a floored 0.
     */
    private int permanentReactions(final Character character) {
        int total = character.getReactions();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.REACTIONS);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.REACTIONS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.REACTIONS);
        }
        for (Item item : character.getEquipment()) {
            total += item.resolveEnhancementBonus(ModifierType.REACTIONS, null, character);
        }
        return total;
    }
}
