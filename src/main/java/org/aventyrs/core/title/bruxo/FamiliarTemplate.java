package org.aventyrs.core.title.bruxo;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.monster.SkillDifficulty;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Familiar Maior's token — "possui Categoria de Tamanho -4, é considerada uma Invocação, não é capaz de lutar".
 * The rules give it no stat block, so ⚠️ every Atributo is 1, it holds no Perícia and no weapon, and its Defesas are
 * the catalogue floor; what it is worth is its Subordinado benefit, not its body. {@link #isNonCombatant()} is what
 * makes {@code AttackDelivery} refuse it an attack.
 */
public record FamiliarTemplate(@NonNull Familiar familiar) implements SummonedMonsterTemplate {

    /** "Um Familiar Maior possui Categoria de Tamanho -4". */
    public static final SizeCategory SIZE = SizeCategory.MINUS_FOUR;

    @Override
    public String getName() {
        return familiar.name();
    }

    @Override
    public int getConjuradorManaGraduation() {
        return 0;
    }

    /** Nothing about it scales with Domínio do Mana. */
    @Override
    public FamiliarTemplate withConjurador(final int manaGraduation) {
        return this;
    }

    @Override
    public Map<AttributeDomain, Integer> getAttributeBases() {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(AttributeDomain.class);
        for (AttributeDomain domain : AttributeDomain.values()) {
            bases.put(domain, 1);
        }
        return bases;
    }

    @Override
    public Map<SkillType, Integer> getSkillGraduations() {
        return Map.of();
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of();
    }

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of();
    }

    @Override
    public List<Item> getEquipment() {
        return List.of();
    }

    @Override
    public SizeCategory getSizeCategory() {
        return SIZE;
    }

    @Override
    public int getPhysicalDefense() {
        return 0;
    }

    @Override
    public int getMagicDefense() {
        return 0;
    }

    @Override
    public SkillDifficulty getGeneralDifficulty() {
        return SkillDifficulty.of(DifficultyLevel.EASY, 0);
    }

    @Override
    public CreatureType getCreatureType() {
        return familiar.creatureType();
    }

    /** "não é capaz de lutar". */
    @Override
    public boolean isNonCombatant() {
        return true;
    }
}
