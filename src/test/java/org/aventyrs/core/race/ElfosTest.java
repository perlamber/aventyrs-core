package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ElfosTest {

    private final Elfo elfos = new Elfo();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(elfos.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasHumanoideCreatureType() {
        assertEquals(CreatureType.HUMANOIDE, elfos.getCreatureType());
    }

    @Test
    void hasAFixedDexterityRacialBonus() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1), elfos.getFixedAttributeBonuses());
    }

    @Test
    void hasOneChoosableBonusPointBetweenFocusAndGnose() {
        assertEquals(1, elfos.getChoosableAttributeBonusPoints());
        assertEquals(Set.of(AttributeDomain.FOCUS, AttributeDomain.GNOSE), elfos.getChoosableAttributes());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, elfos.getNewFeatCost(FeatCategory.METAMAGICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, elfos.getNewSkillCost());
    }

    @Test
    void grantsSentidosAbsolutosAsARacialAbility() {
        assertEquals(List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), elfos.getRacialAbilities());
    }
}
