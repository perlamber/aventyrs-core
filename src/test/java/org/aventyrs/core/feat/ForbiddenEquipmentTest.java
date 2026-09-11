package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.CloakItem;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Feat#getForbiddenEquipmentCategories} — a Talento that forbids a whole kind of
 * Equipamento <b>permanently</b>, enforced on the equipment list itself. {@code
 * DraconicoFeat#ASAS_DE_DRAGAO}'s "o impede de usar Equipamentos do tipo Capa" is the only one.
 *
 * <p>Distinct from a Forma's {@code FormEquipmentPolicy}, which is temporary and answers a
 * different question — see {@code MetamorfoseSelvagemTest} for that side.
 */
class ForbiddenEquipmentTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder draconico() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new NascidoDoDragao(new Human(), ElementalType.FOGO))
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>());
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void withoutTheTalentoACapaIsOrdinaryEquipment() {
        CharacterSheet sheet = sheetOf(draconico().build());

        assertTrue(sheet.canEquip(CloakItem.CAPA_DO_VIAJANTE));
        assertDoesNotThrow(() -> sheet.equip(CloakItem.CAPA_DO_VIAJANTE));
    }

    @Test
    void asasDeDragaoRefusesACapa() {
        Character character = draconico().build();
        character.grantFeat(DraconicoFeat.ASAS_DE_DRAGAO);
        CharacterSheet sheet = sheetOf(character);

        assertFalse(sheet.canEquip(CloakItem.CAPA_DO_VIAJANTE));
        assertThrows(IllegalOperationException.class, () -> sheet.equip(CloakItem.CAPA_DO_VIAJANTE));
        assertTrue(character.getEquipment().isEmpty(), "a refused equip leaves the character untouched");
    }

    /** Only the category it names — the wings are in the way of a cloak, not of armour. */
    @Test
    void itForbidsOnlyCapas() {
        Character character = draconico().build();
        character.grantFeat(DraconicoFeat.ASAS_DE_DRAGAO);
        CharacterSheet sheet = sheetOf(character);

        assertTrue(sheet.canEquip(ArmorItem.ARMADURA_COMPLETA));
        assertDoesNotThrow(() -> sheet.equip(ArmorItem.ARMADURA_COMPLETA));
        assertEquals(Set.of(ItemCategory.CLOAK),
                DraconicoFeat.ASAS_DE_DRAGAO.getForbiddenEquipmentCategories(character));
    }

    /**
     * A loadout assembled around the restriction is caught too — a Capa worn before the Talento
     * was taken, or staged through the builder, which bypasses {@code equip} by design.
     */
    @Test
    void aCapaWornBeforeTheTalentoIsCaughtOnValidation() {
        Character character = draconico()
                .equipment(new ArrayList<>(List.of(CloakItem.CAPA_DO_VIAJANTE)))
                .build();
        CharacterSheet sheet = sheetOf(character);

        assertDoesNotThrow(sheet::validateEquipmentLoadout);

        character.grantFeat(DraconicoFeat.ASAS_DE_DRAGAO);

        assertThrows(IllegalOperationException.class, sheet::validateEquipmentLoadout);
    }

    /** Every other Talento forbids nothing — the default, and the answer for the whole catalog but one. */
    @Test
    void noOtherTalentoForbidsAnything() {
        Character character = draconico().build();

        for (Feat feat : FeatCatalog.all()) {
            if (feat == DraconicoFeat.ASAS_DE_DRAGAO) {
                continue;
            }
            assertTrue(feat.getForbiddenEquipmentCategories(character).isEmpty(),
                    feat + " unexpectedly forbids equipment");
        }
    }
}
