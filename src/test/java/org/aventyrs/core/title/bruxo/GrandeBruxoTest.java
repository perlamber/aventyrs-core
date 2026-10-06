package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.MimetizedSpellCastingResult;
import org.aventyrs.core.magic.MimetizedSpellCastingService;
import org.aventyrs.core.magic.MimetizedSpellCastingServiceImpl;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.HIT_POINT_PAYMENT_NOT_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** O Grande Bruxo — the PV price of a Misticismo, through {@code MimetizedSpellCastingService}. */
class GrandeBruxoTest {

    private final MimetizedSpellCastingService service = new MimetizedSpellCastingServiceImpl();
    private final HitPointsService hitPoints = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void aMisticismoCastSpendsItsPd() throws IllegalOperationException {
        CharacterSheet sheet = bruxoIn(TitleSlot.PRIMARY);

        service.cast(sheet, broto(sheet));

        assertEquals(1, sheet.getDeterminationSpent());
    }

    @Test
    void aPrimarioBruxoPaysOnePlusThePdInLockedPvOncePerRodada() throws IllegalOperationException {
        CharacterSheet sheet = bruxoIn(TitleSlot.PRIMARY);
        MimetizedSpell broto = broto(sheet);
        int before = hitPoints.getCurrentHitPoints(sheet.getCharacter(), sheet);
        assertEquals(OptionalInt.of(2), service.resolveHitPointCost(sheet, broto));

        MimetizedSpellCastingResult result = service.cast(sheet, broto, true);

        assertEquals(2, result.getHitPointsPaid());
        assertEquals(0, sheet.getDeterminationSpent());
        assertEquals(before - 2, hitPoints.getCurrentHitPoints(sheet.getCharacter(), sheet));
        assertEquals(2, sheet.getLockedDamage());
        assertEquals(OptionalInt.empty(), service.resolveHitPointCost(sheet, broto));
        assertRefused(() -> service.cast(sheet, broto, true));

        sheet.startNewRound();
        assertEquals(OptionalInt.of(2), service.resolveHitPointCost(sheet, broto));
    }

    @Test
    void aSecundarioBruxoMayNotPayInPv() {
        CharacterSheet sheet = bruxoIn(TitleSlot.SECONDARY);

        assertRefused(() -> service.cast(sheet, broto(sheet), true));
        assertEquals(0, sheet.getDeterminationSpent());
    }

    @Test
    void aMimicryThatIsNoMisticismoMayNotBePaidInPv() {
        CharacterSheet sheet = bruxoIn(TitleSlot.PRIMARY);
        MimetizedSpell other = MimetizedSpell.builder().spell(new TestSpell()).determinationPointCost(1).build();
        sheet.getCharacter().grantMimetizedSpell(other);

        assertRefused(() -> service.cast(sheet, other, true));
    }

    /** A Bruxo knowing Aliados da Natureza with two Habilidades, so its Broto costs 1PD. */
    private static CharacterSheet bruxoIn(final TitleSlot slot) {
        Bruxo bruxo = new Bruxo(List.of(BruxoSpecialization.ILUMINADO),
                List.of(BruxoAbility.FAMILIAR_MAIOR, IluminadoAbility.BENCAO_DO_MAR_DO_SUL));
        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.ALIADOS_DA_NATUREZA);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).mimetizedSpells(new ArrayList<>()).build();
        character.grantTitle(bruxo, slot);
        return CharacterSheet.of(character, new Player());
    }

    private static MimetizedSpell broto(final CharacterSheet sheet) {
        return sheet.getCharacter().getMimetizedSpells().stream()
                .filter(mimetized -> mimetized.getSpell().getBranchLevel() == BranchLevel.BROTO)
                .findFirst().orElseThrow();
    }

    private static void assertRefused(final org.junit.jupiter.api.function.Executable executable) {
        assertEquals(HIT_POINT_PAYMENT_NOT_PERMITTED,
                assertThrows(IllegalOperationException.class, executable).getMessage());
    }
}
