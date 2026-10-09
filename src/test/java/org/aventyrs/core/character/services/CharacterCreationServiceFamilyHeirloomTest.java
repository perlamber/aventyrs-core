package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.ResourcesAdvantage;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.HeavyBladeItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.OffensiveImprovement;
import org.aventyrs.core.item.OffensiveMasterpiece;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterCreationServiceFamilyHeirloomTest {

    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    /** Raro — above any Raridade Inicial a Recursos 1 store would carry. */
    private static final HeavyBladeItem RARE_WEAPON = HeavyBladeItem.ESPADA_BASTARDA_OU_KODACHI;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final boolean heir, final int equipmentPoints) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        if (heir) {
            builder.egoAdvantage(EgoDomain.RECURSOS, ResourcesAdvantage.HERANCA_FAMILIAR);
        }
        CharacterSheet sheet = CharacterSheet.of(builder.build(), new Player());
        sheet.grantEquipmentPoints(equipmentPoints);
        return sheet;
    }

    private static ItemSpecification heirloom(final OffensiveMasterpiece masterpiece) {
        return ItemSpecification.builder().base(RARE_WEAPON).masterpiece(masterpiece).build();
    }

    @Test
    void anOffensivePieceOfAnyRarityAsACommonOrUncommonMasterpieceQualifies() {
        assertTrue(creation.isFamilyHeirloom(heirloom(OffensiveMasterpiece.PRECISA)));
        assertTrue(creation.isFamilyHeirloom(heirloom(OffensiveMasterpiece.BRUTAL)));
    }

    @Test
    void theMasterpieceIsRequiredAndCappedAtUncommon() {
        assertFalse(creation.isFamilyHeirloom(ItemSpecification.of(RARE_WEAPON)));
        assertFalse(creation.isFamilyHeirloom(heirloom(OffensiveMasterpiece.PODEROSA)));
    }

    @Test
    void noAprimoramentosNoRegaliaNoDefensivePiece() {
        assertFalse(creation.isFamilyHeirloom(heirloom(OffensiveMasterpiece.PRECISA).toBuilder()
                .improvement(OffensiveImprovement.CRUEL).build()));
        assertFalse(creation.isFamilyHeirloom(heirloom(OffensiveMasterpiece.PRECISA).toBuilder()
                .regaliaGrade(RegaliaGrade.MENOR).build()));
        assertFalse(creation.isFamilyHeirloom(ItemSpecification.of(ArmorItem.ARMADURA_DE_GLADIADOR)));
    }

    @Test
    void theHeirIsGivenTheCopyForNoPe() {
        CharacterSheet sheet = sheet(true, 4);

        Item copy = creation.grantFamilyHeirloom(sheet, heirloom(OffensiveMasterpiece.BRUTAL));

        assertEquals(4, sheet.getEquipmentPoints());
        assertTrue(sheet.getInventory().contains(copy));
        assertEquals(OffensiveMasterpiece.BRUTAL, copy.getMasterpiece());
    }

    @Test
    void aCharacterWithoutTheVantagemIsRefused() {
        CharacterSheet sheet = sheet(false, 0);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> creation.grantFamilyHeirloom(sheet, heirloom(OffensiveMasterpiece.PRECISA)));

        assertEquals(TranslatableMessages.FAMILY_HEIRLOOM_NOT_HELD, refused.getMessage());
        assertTrue(sheet.getInventory().isEmpty());
    }

    @Test
    void anIllegalPieceIsRefused() {
        CharacterSheet sheet = sheet(true, 0);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> creation.grantFamilyHeirloom(sheet, ItemSpecification.of(RARE_WEAPON)));

        assertEquals(TranslatableMessages.INVALID_FAMILY_HEIRLOOM, refused.getMessage());
        assertTrue(sheet.getInventory().isEmpty());
    }
}
