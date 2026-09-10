package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoblinTest {

    private final Goblin goblin = new Goblin();

    @Test
    void generateEmptyCharacterSeedsMinusOneSizeCategory() {
        Character character = goblin.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(goblin)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndMinusOneBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, goblin.getCreatureType());
        assertEquals(SizeCategory.MINUS_ONE, goblin.getBaseSizeCategory());
    }

    @Test
    void hasAFixedDexterityRacialBonus() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1), goblin.getFixedAttributeBonuses());
    }

    @Test
    void hasOneChoosableBonusPointBetweenFocusAndGnose() {
        assertEquals(1, goblin.getChoosableAttributeBonusPoints());
        assertEquals(Set.of(AttributeDomain.FOCUS, AttributeDomain.GNOSE), goblin.getChoosableAttributes());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, goblin.getNewFeatCost(FeatCategory.GOBLIN));
        assertEquals(Race.BASE_NEW_SKILL_COST, goblin.getNewSkillCost());
    }

    @Test
    void isNotMestico() {
        assertFalse(goblin.isMestico());
    }

    @Test
    void grantsPoderDosNumerosAndAutodesconfiancaAsRacialAbilities() {
        assertEquals(List.of(GoblinsRacialAbility.PODER_DOS_NUMEROS,
                        GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE),
                goblin.getRacialAbilities());
    }

    @Test
    void grantsNoCriticalEffectImmunities() {
        assertTrue(goblin.getCriticalEffectImmunities().isEmpty());
    }
}
