package org.aventyrs.core.title;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_AWAKENING_CHOICE_NOT_ELIGIBLE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_AWAKENING_NOT_DUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.CentelhaGranAventyrAntecipadaFeat;
import org.aventyrs.core.feat.DefeitoFeat;
import org.aventyrs.core.feat.DespertarAntecipadoFeat;
import org.aventyrs.core.feat.DestinoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleAwakeningServiceImplTest {

    private final TitleAwakeningService service = new TitleAwakeningServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetWith(final int totalExperience, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        if (totalExperience > 0) {
            sheet.accumulateExperience(BigDecimal.valueOf(totalExperience));
        }
        return sheet;
    }

    private AventyrTitle firstOption(final CharacterSheet sheet, final TitleSlot slot) {
        return service.optionsFor(sheet, slot).get(0);
    }

    @Test
    void nothingIsDueBelowFifteenExperience() {
        assertEquals(Optional.empty(), service.dueSlot(sheetWith(14)));
    }

    @Test
    void thePrimarioIsDueAtFifteenAndTheSecundarioAtThirty() throws IllegalOperationException {
        CharacterSheet sheet = sheetWith(15);
        assertEquals(Optional.of(TitleSlot.PRIMARY), service.dueSlot(sheet));

        AventyrTitle primary = firstOption(sheet, TitleSlot.PRIMARY);
        assertEquals(TitleSlot.PRIMARY, service.awaken(sheet, primary));
        assertSame(primary, sheet.getCharacter().getPrimaryTitle());
        assertEquals(Optional.empty(), service.dueSlot(sheet));

        sheet.accumulateExperience(BigDecimal.valueOf(15));
        assertEquals(Optional.of(TitleSlot.SECONDARY), service.dueSlot(sheet));
    }

    @Test
    void atThirtyWithNothingAwakenedOnlyThePrimarioIsDue() {
        assertEquals(Optional.of(TitleSlot.PRIMARY), service.dueSlot(sheetWith(30)));
    }

    @Test
    void theTerciarioIsNeverOffered() {
        CharacterSheet sheet = sheetWith(100);
        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        sheet.getCharacter().grantTitle(firstOption(sheet, TitleSlot.SECONDARY), TitleSlot.SECONDARY);

        assertEquals(Optional.empty(), service.dueSlot(sheet));
    }

    @Test
    void aFamilyAlreadyHeldIsNotOfferedAgain() {
        CharacterSheet sheet = sheetWith(30);
        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(service.optionsFor(sheet, TitleSlot.SECONDARY).stream()
                .noneMatch(option -> option instanceof Santo));
    }

    @Test
    void awakeningRefusesWhenNothingIsDue() {
        CharacterSheet sheet = sheetWith(10);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.awaken(sheet, new Santo(List.of(), List.of())));

        assertEquals(TITLE_AWAKENING_NOT_DUE, refused.getMessage());
    }

    @Test
    void awakeningRefusesAFamilyAlreadyHeld() {
        CharacterSheet sheet = sheetWith(30);
        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.awaken(sheet, new Santo(List.of(), List.of())));

        assertEquals(TITLE_AWAKENING_CHOICE_NOT_ELIGIBLE, refused.getMessage());
    }

    @Test
    void aSlotATalentoAwakensWithItsOwnPickGetsNoPicker() {
        CharacterSheet primaryPreset = sheetWith(15);
        Character character = primaryPreset.getCharacter();
        character.grantFeat(DespertarAntecipadoFeat.of(character, DespertarAntecipadoFeat.optionsFor(character).get(0)));
        assertEquals(Optional.empty(), service.dueSlot(primaryPreset));

        CharacterSheet secondaryPreset = sheetWith(30);
        Character holder = secondaryPreset.getCharacter();
        holder.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        holder.grantFeat(CentelhaGranAventyrAntecipadaFeat.of(holder,
                CentelhaGranAventyrAntecipadaFeat.optionsFor(holder).get(0)));
        assertEquals(Optional.empty(), service.dueSlot(secondaryPreset));
    }

    @Test
    void centelhaDormenteDelaysThePrimarioToTwentyFiveAndClosesTheSecundario() {
        assertEquals(Optional.empty(), service.dueSlot(sheetWith(15, DefeitoFeat.HERANCA_DE_GILGAMESH_MODERADO)));

        CharacterSheet sheet = sheetWith(25, DefeitoFeat.HERANCA_DE_GILGAMESH_MODERADO);
        assertEquals(Optional.of(TitleSlot.PRIMARY), service.dueSlot(sheet));

        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        sheet.accumulateExperience(BigDecimal.valueOf(10));
        assertEquals(Optional.empty(), service.dueSlot(sheet));
    }

    @Test
    void abdicadorNeverAwakens() {
        CharacterSheet sheet = sheetWith(30, DestinoFeat.ATRASAR_DESPERTAR, DestinoFeat.ABDICADOR);

        assertEquals(Optional.empty(), service.dueSlot(sheet));
        assertFalse(new TitleAcquisitionServiceImpl().isPermitted(sheet.getCharacter(), sheet,
                new Santo(List.of(), List.of()), TitleSlot.PRIMARY));
    }

    @Test
    void aSacrificedCentelhaLeavesASlotUnfillable() {
        CharacterSheet sheet = sheetWith(30);
        sheet.getCharacter().sacrificeCentelhas(2);
        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertEquals(Optional.empty(), service.dueSlot(sheet));
    }

    @Test
    void onlyAtrasarDespertarMayPostpone() {
        assertTrue(service.mayPostpone(sheetWith(15, DestinoFeat.ATRASAR_DESPERTAR).getCharacter()));
        assertFalse(service.mayPostpone(sheetWith(15).getCharacter()));
    }
}
