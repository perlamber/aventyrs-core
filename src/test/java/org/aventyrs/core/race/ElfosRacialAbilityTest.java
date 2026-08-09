package org.aventyrs.core.race;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ElfosRacialAbilityTest {

    @Test
    void everyAbilityHasADescription() {
        for (ElfosRacialAbility ability : ElfosRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void sentidosAbsolutosBelongsToAttention() {
        assertEquals(SkillType.ATTENTION, ElfosRacialAbility.SENTIDOS_ABSOLUTOS.getSkillType());
    }

    @Test
    void sentidosAbsolutosGrantsAFlatAttentionRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();

        assertEquals(Skill.ADVANTAGE_BONUS, modifierResolver.sumModifiers(ElfosRacialAbility.SENTIDOS_ABSOLUTOS, ModifierType.ATTENTION_ROLL_BONUS));
    }
}
