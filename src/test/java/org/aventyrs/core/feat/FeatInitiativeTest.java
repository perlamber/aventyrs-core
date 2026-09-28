package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.InitiativeBlessingService;
import org.aventyrs.core.character.services.InitiativeBlessingServiceImpl;
import org.aventyrs.core.character.services.InitiativeService;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Talentos reaching Iniciativa — {@code Feat#resolveInitiativeBonus}, summed by {@link
 * InitiativeService}, and {@code Feat#resolveInitiativeBlessings}, resolved by {@link
 * InitiativeBlessingService}.
 */
class FeatInitiativeTest {

    private final InitiativeService initiativeService = new InitiativeServiceImpl();
    private final InitiativeBlessingService blessingService = new InitiativeBlessingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character holding(final Map<AttributeDomain, Integer> attributes, final List<Feat> feats,
                                     final Item... equipment) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(attributes))
                .equipment(new ArrayList<>(List.of(equipment)))
                .feats(new ArrayList<>(feats))
                .build();
    }

    private int gain(final Feat feat, final Map<AttributeDomain, Integer> attributes, final Item... equipment) {
        return initiativeService.getTotalInitiative(holding(attributes, List.of(feat), equipment))
                - initiativeService.getTotalInitiative(holding(attributes, List.of(), equipment));
    }

    // ---------- ESCUDO_VELOZ ----------

    @Test
    void aMediumShieldRaisesIniciativaByTwo() {
        assertEquals(2, gain(EscudeiroFeat.ESCUDO_VELOZ, Map.of(), ShieldItem.ESCUDO_MEDIO));
    }

    @Test
    void aHeavyShieldCountsToo() {
        assertEquals(2, gain(EscudeiroFeat.ESCUDO_VELOZ, Map.of(), ShieldItem.ESCUDO_DE_CORPO));
    }

    @Test
    void aLightShieldDoesNot() {
        assertEquals(0, gain(EscudeiroFeat.ESCUDO_VELOZ, Map.of(), ShieldItem.BROQUEL));
    }

    @Test
    void noShieldDoesNot() {
        assertEquals(0, gain(EscudeiroFeat.ESCUDO_VELOZ, Map.of()));
    }

    // ---------- PORTA_ESTANDARTE ----------

    /** "Metade de seu valor de Destreza ou do valor de Carisma, a sua escolha" — the higher one. */
    @Test
    void theStandardBearerAddsHalfTheHigherOfDestrezaAndCarisma() {
        assertEquals(2, gain(MobilidadeFeat.PORTA_ESTANDARTE,
                Map.of(AttributeDomain.DEXTERITY, 5, AttributeDomain.CHARISMA, 2)));
        assertEquals(3, gain(MobilidadeFeat.PORTA_ESTANDARTE,
                Map.of(AttributeDomain.DEXTERITY, 2, AttributeDomain.CHARISMA, 6)));
    }

    // ---------- LIDERAR_O_AVANCO ----------

    @Test
    void winningInitiativeGrantsTwoActionPointsForTwoRodadas() {
        List<Blessing> blessings = blessingService.resolveBlessings(
                holding(Map.of(), List.of(MobilidadeFeat.LIDERAR_O_AVANCO)));

        assertEquals(1, blessings.size());
        Blessing blessing = blessings.get(0);
        assertEquals(ModifierType.ACTION_POINTS, blessing.getModifierType());
        assertEquals(2, blessing.getValue());
        assertEquals(2, blessing.getRounds());
        assertEquals(TargetScope.SELF, blessing.getScope());
    }

    @Test
    void aTalentoGrantingNothingAddsNoBlessing() {
        assertTrue(blessingService.resolveBlessings(holding(Map.of(), List.of(EscudeiroFeat.ESCUDO_VELOZ))).isEmpty());
    }
}
