package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class DefenseServiceImpl implements DefenseService {

    private final ModifierResolver modifierResolver;

    public DefenseServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DefenseServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalDefense(final Character character, final DefenseType defenseType) {
        return getTotalDefense(character, defenseType, null);
    }

    @Override
    public int getTotalDefense(final Character character, final DefenseType defenseType, final SceneContext sceneContext) {
        return sumAbilityModifiers(character, defenseType) + sumEquipment(character, defenseType, sceneContext)
                + sumFeats(character, defenseType, sceneContext);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType) {
        return getTotalDefense(target, defenseType, null);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType,
                               final SceneContext sceneContext) {
        return getTotalDefense(target, defenseType, sceneContext, null);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType,
                               final SceneContext sceneContext, final DamageDescriptor damageDescriptor) {
        return sumAbilityModifiers(target.getCharacter(), defenseType)
                + sumEquipment(target.getCharacter(), defenseType, sceneContext, damageDescriptor)
                + sumFeats(target.getCharacter(), defenseType, sceneContext, target)
                + target.getTemporaryBonus(ModifierType.DEFESAS)
                + target.getTemporaryBonus(defenseType.getModifierType())
                // Desprevenido's -2 Defesas, and anything conferring it (Caído, Flanqueado,
                // Cego, or the fear ladder while close enough to its origin).
                + target.getConditionBonus(ModifierType.DEFESAS, sceneContext)
                + target.getConditionBonus(defenseType.getModifierType(), sceneContext);
    }

    /**
     * The standard three-source {@code @Modifier} scan (see this service's interface javadoc),
     * each source summed for both {@link ModifierType#DEFESAS} and defenseType's own scoped
     * type. Uses {@link SkillCompetencyAbility#allFor} so a racial ability granting a Defesa
     * counts identically to an acquired one.
     */
    private int sumAbilityModifiers(final Character character, final DefenseType defenseType) {
        int total = sumBothTypes(character.getAttributeAbilities(), defenseType);
        total += sumBothTypes(SkillCompetencyAbility.allFor(character), defenseType);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += sumBothTypes(unlockedExcellencies, defenseType);
        }
        return total;
    }

    /**
     * Every equipped {@link Item}'s contribution: its flat DF or DM column, the item's
     * Masterpiece contribution (which can replace its own base column for a "muda para" Favor),
     * plus whichever Defesa-typed {@code ItemBonus}es its {@code ItemFavor} currently grants.
     */
    private int sumEquipment(final Character character, final DefenseType defenseType,
                             final SceneContext sceneContext) {
        return sumEquipment(character, defenseType, sceneContext, null);
    }

    private int sumEquipment(final Character character, final DefenseType defenseType,
                             final SceneContext sceneContext, final DamageDescriptor damageDescriptor) {
        int total = 0;
        for (Item item : character.getEquipment()) {
            total += item.getEffectiveDefenseBonus(defenseType, character, sceneContext, damageDescriptor);
        }
        return total;
    }

    /**
     * Every held Talento's unconditional contribution to this Defesa — {@code
     * Feat#resolveDefenseBonus}. Feats are deliberately not part of the {@code @Modifier} scan
     * above (nothing else in this codebase scans them reflectively), so they get their own
     * explicit pass, the same way equipment does.
     */
    private int sumFeats(final Character character, final DefenseType defenseType, final SceneContext sceneContext) {
        return sumFeats(character, defenseType, sceneContext, null);
    }

    /**
     * holder is the combatant's own sheet, or {@code null} on the {@code Character}-only entry
     * point — a Talento conditioned on live combat state reads that as "condition not met".
     */
    private int sumFeats(final Character character, final DefenseType defenseType,
                          final SceneContext sceneContext, final CombatantSheet holder) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveDefenseBonus(defenseType, character, sceneContext, holder))
                .sum();
    }

    private int sumBothTypes(final java.util.Collection<?> sources, final DefenseType defenseType) {
        return modifierResolver.sumModifiers(sources, ModifierType.DEFESAS)
                + modifierResolver.sumModifiers(sources, defenseType.getModifierType());
    }
}
