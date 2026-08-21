package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
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
        return sumAbilityModifiers(character, defenseType) + sumEquipment(character, defenseType);
    }

    @Override
    public int getTotalDefense(final CharacterSheet target, final DefenseType defenseType) {
        return getTotalDefense(target.getCharacter(), defenseType)
                + target.getTemporaryBonus(ModifierType.DEFESAS)
                + target.getTemporaryBonus(defenseType.getModifierType());
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
     * Every equipped {@link Item}'s contribution: its flat DF or DM column (unconditional — it
     * applies to anyone carrying the item) plus whichever Defesa-typed {@code ItemBonus}es its
     * {@code ItemFavor} currently grants this character ({@code resolveFavorBonus} already
     * returns 0 for an item with no Favor, or one whose Requisitos aren't met, so there's
     * nothing to null-check here).
     */
    private int sumEquipment(final Character character, final DefenseType defenseType) {
        int total = 0;
        for (Item item : character.getEquipment()) {
            total += defenseType.columnOf(item);
            total += item.resolveFavorBonus(ModifierType.DEFESAS, character);
            total += item.resolveFavorBonus(defenseType.getModifierType(), character);
        }
        return total;
    }

    private int sumBothTypes(final java.util.Collection<?> sources, final DefenseType defenseType) {
        return modifierResolver.sumModifiers(sources, ModifierType.DEFESAS)
                + modifierResolver.sumModifiers(sources, defenseType.getModifierType());
    }
}
