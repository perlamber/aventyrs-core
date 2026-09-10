package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrollTest {

    private final Troll troll = new Troll();

    @Test
    void generateEmptyCharacterSeedsTheYoungestZeroSizeCategory() {
        Character character = troll.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(troll)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndZeroBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, troll.getCreatureType());
        assertEquals(SizeCategory.ZERO, troll.getBaseSizeCategory());
    }

    @Test
    void hasAFixedTwoPointStrengthRacialBonus() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2), troll.getFixedAttributeBonuses());
    }

    @Test
    void anatomiaVegetalGrantsTheThreeNamedCriticalEffectImmunities() {
        assertEquals(Set.of(CriticalEffectType.ATORDOANTE,
                        CriticalEffectType.FERIDA_PROFUNDA,
                        CriticalEffectType.SANGRAMENTO),
                troll.getCriticalEffectImmunities());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, troll.getChoosableAttributeBonusPoints());
        assertTrue(troll.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, troll.getNewFeatCost(FeatCategory.TROLL));
        assertEquals(Race.BASE_NEW_SKILL_COST, troll.getNewSkillCost());
    }

    @Test
    void isNotMesticoAndGrantsNoRacialAbilities() {
        assertFalse(troll.isMestico());
        assertTrue(troll.getRacialAbilities().isEmpty());
    }
}
