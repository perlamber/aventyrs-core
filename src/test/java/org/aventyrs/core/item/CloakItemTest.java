package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloakItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedCloak() {
        assertEquals(7, CloakItem.values().length);
    }

    @Test
    void everyConstantIsANamedDefensiveCloak() {
        // Capas are not CriticallyDefensiveItem — enforced at compile time.
        Arrays.stream(CloakItem.values()).forEach(cloak -> {
            assertEquals(ItemCategory.CLOAK, cloak.getCategory());
            assertEquals(ItemType.DEFENSIVE, cloak.getType());
            assertFalse(cloak.getName().isBlank());
        });
    }

    @Test
    void everyCatalogedFavorIsDescribedAndCarriesItsRequirements() {
        Arrays.stream(CloakItem.values())
                .map(CloakItem::getFavor)
                .filter(Objects::nonNull)
                .forEach(favor -> {
                    assertFalse(favor.getDescription().isBlank());
                    assertNotNull(favor.getRequirements());
                });
    }

    @Test
    void mantoNortenhoCarriesEveryColumnOfItsRulesText() {
        CloakItem cloak = CloakItem.MANTO_NORTENHO;

        assertEquals("Manto Nortenho", cloak.getName());
        assertEquals(ItemWeightClass.MEDIUM, cloak.getWeightClass());
        assertEquals(ItemRarity.RARE, cloak.getRarity());
        assertEquals(10, cloak.getPrice());
        assertEquals(1, cloak.getPhysicalDefenseBonus());
        assertEquals(0, cloak.getMagicDefenseBonus());
        assertEquals(24, cloak.getHardness());
        assertEquals(0, cloak.getCastingBonus());
    }

    @Test
    void mantoNortenhoFavorGrantsRealDamageReductionOnlyAtVigor3() {
        assertEquals(1, CloakItem.MANTO_NORTENHO
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithBase(AttributeDomain.VIGOR, 3)));
        assertEquals(0, CloakItem.MANTO_NORTENHO
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithBase(AttributeDomain.VIGOR, 2)));
    }

    @Test
    void sobretudoDoInquisidorFavorGrantsRealMagicReductionFromItsEfeitoAdicional() {
        assertEquals(1, CloakItem.SOBRETUDO_DO_INQUISIDOR
                .resolveFavorBonus(ModifierType.MAGIC_REDUCTION, characterWithBase(AttributeDomain.FOCUS, 3)));
        assertEquals(0, CloakItem.SOBRETUDO_DO_INQUISIDOR
                .resolveFavorBonus(ModifierType.MAGIC_REDUCTION, characterWithBase(AttributeDomain.FOCUS, 2)));
    }

    @Test
    void capaEsvoacanteFavorGrantsAPersuasaoVantagem() {
        assertEquals(Skill.ADVANTAGE_BONUS, CloakItem.CAPA_ESVOACANTE
                .resolveFavorBonus(ModifierType.PERSUASAO_ROLL_BONUS, characterWithBase(AttributeDomain.CHARISMA, 3)));
    }

    @Test
    void vesteSombriaFavorGrantsAFurtividadeVantagemFromItsEfeitoAdicionalButNotItsLadiniceLine() {
        Character character = characterWithBase(AttributeDomain.DEXTERITY, 3);

        assertEquals(Skill.ADVANTAGE_BONUS, CloakItem.VESTE_SOMBRIA
                .resolveFavorBonus(ModifierType.FURTIVIDADE_ROLL_BONUS, character));
        assertEquals(1, CloakItem.VESTE_SOMBRIA.getFavor().getBonuses().size());
    }

    /** Resistência Elemental has no ModifierType, so these Favores grant nothing mechanical. */
    @Test
    void grantsNoBonusForItsResistenciaElementalClauses() {
        Character vigor = characterWithBase(AttributeDomain.VIGOR, 3);
        Character dex = characterWithBase(AttributeDomain.DEXTERITY, 3);

        assertTrue(CloakItem.CAPA_DO_VIAJANTE.getFavor().getBonuses().isEmpty());
        assertTrue(CloakItem.PONCHO_DO_AVENTUREIRO.getFavor().getBonuses().isEmpty());
        assertEquals(0, CloakItem.CAPA_DO_VIAJANTE.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, vigor));
        assertEquals(0, CloakItem.PONCHO_DO_AVENTUREIRO.resolveFavorBonus(ModifierType.MAGIC_REDUCTION, dex));
    }

    /** The magic-damage/heal effect bonus scaling has no stat here. */
    @Test
    void gabardinaElficaGrantsNoBonusForItsMagicEffectScalingClauses() {
        assertTrue(CloakItem.GABARDINA_ELFICA.getFavor().getBonuses().isEmpty());
        assertEquals(1, CloakItem.GABARDINA_ELFICA.getCastingBonus());
    }

    private static Character characterWithBase(final AttributeDomain domain, final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(domain, base)))
                .build();
    }
}
