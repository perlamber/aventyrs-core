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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmetItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedHeadgear() {
        assertEquals(7, HelmetItem.values().length);
    }

    @Test
    void everyConstantIsANamedDefensiveHelmet() {
        // Protetores de Cabeça are not CriticallyDefensiveItem — enforced at compile time.
        Arrays.stream(HelmetItem.values()).forEach(helmet -> {
            assertEquals(ItemCategory.HELMET, helmet.getCategory());
            assertEquals(ItemType.DEFENSIVE, helmet.getType());
            assertFalse(helmet.getName().isBlank());
        });
    }

    @Test
    void everyCatalogedFavorIsDescribed() {
        Arrays.stream(HelmetItem.values())
                .map(HelmetItem::getFavor)
                .filter(Objects::nonNull)
                .forEach(favor -> assertFalse(favor.getDescription().isBlank()));
    }

    @Test
    void capacetesEElmosFechadosCarriesEveryColumnOfItsRulesText() {
        HelmetItem helmet = HelmetItem.CAPACETES_E_ELMOS_FECHADOS;

        assertEquals("Capacetes e Elmos Fechados", helmet.getName());
        assertEquals(ItemWeightClass.MEDIUM, helmet.getWeightClass());
        assertEquals(ItemRarity.COMMON, helmet.getRarity());
        assertEquals(7, helmet.getPrice());
        assertEquals(1, helmet.getPhysicalDefenseBonus());
        assertEquals(1, helmet.getMagicDefenseBonus());
        assertEquals(20, helmet.getHardness());
        assertEquals(0, helmet.getCastingBonus());
    }

    @Test
    void mascaraPrimalFavorGrantsAnEmpatiaSelvagemVantagemFromItsEfeitoAdicional() {
        assertEquals(Skill.ADVANTAGE_BONUS, HelmetItem.MASCARA_PRIMAL
                .resolveFavorBonus(ModifierType.EMPATIA_SELVAGEM_ROLL_BONUS,
                        characterWithBase(AttributeDomain.CHARISMA, 3)));
    }

    /** "Car 3/Gno 3" — either Atributo satisfies the requirement. */
    @Test
    void charmeDoArtesaoRequirementIsMetByEitherCharismaOrGnose() {
        assertTrue(HelmetItem.CHARME_DO_ARTESAO.grantsFavorTo(characterWithBase(AttributeDomain.CHARISMA, 3)));
        assertTrue(HelmetItem.CHARME_DO_ARTESAO.grantsFavorTo(characterWithBase(AttributeDomain.GNOSE, 3)));
        assertFalse(HelmetItem.CHARME_DO_ARTESAO.grantsFavorTo(characterWithBase(AttributeDomain.CHARISMA, 2)));
    }

    /** Ladinice/Diplomacia/target-scoped Vantagem and Encantamento duration are all unmodelled. */
    @Test
    void grantsNoBonusForItsBlockedFavorClauses() {
        assertTrue(HelmetItem.CAPACETES_E_ELMOS_FECHADOS.getFavor().getBonuses().isEmpty());
        assertTrue(HelmetItem.CHAPEU_GALANTE.getFavor().getBonuses().isEmpty());
        assertTrue(HelmetItem.FACE_DO_BUFAO.getFavor().getBonuses().isEmpty());
        assertTrue(HelmetItem.GORRO_DO_MAGO.getFavor().getBonuses().isEmpty());
    }

    /**
     * Tiaras e Bandanas has "Favor: Nenhum" but an Efeito Adicional, so its {@link ItemFavor}
     * exists with {@code null} requirements to carry that line unconditionally.
     */
    @Test
    void tiarasEBandanasCarriesAnUnconditionalEfeitoAdicionalWithNoRequirement() {
        HelmetItem helmet = HelmetItem.TIARAS_E_BANDANAS;

        assertNotNull(helmet.getFavor());
        assertNull(helmet.getFavor().getRequirements());
        assertTrue(helmet.getFavor().getBonuses().isEmpty());
        assertTrue(helmet.getFavor().hasAdditionalEffects());
        assertTrue(helmet.grantsFavorTo(characterWithBase(AttributeDomain.CHARISMA, 1)));
    }

    private static Character characterWithBase(final AttributeDomain domain, final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(domain, base)))
                .build();
    }
}
