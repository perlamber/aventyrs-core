package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Forma state — {@code CombatantSheet#getCurrentForm()}/{@code enterForm}/{@code isInForm},
 * and the {@code canTakeForm} gate that {@code Feat#resolveFormAccess} feeds.
 *
 * <p>Entering a Forma is a caller's call, like applying a Condição: nothing in this core
 * transforms anybody on its own, so these drive the mutator directly.
 */
class FormStateTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** A Górgona qualified for both Proteções — Força 4 and Carisma 4. */
    private static Character gorgona() {
        return character().race(new Gorgona())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(4).build())
                        .build())
                .build();
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void aSheetStartsInItsOwnShape() {
        CharacterSheet sheet = sheetOf(character().build());

        assertNull(sheet.getCurrentForm(), "no constant spells \"normal\" — it is an absent value");
        assertFalse(sheet.isInForm(FormType.MONSTRUOSA));
    }

    @Test
    void enteringAFormReportsTheOneLeftBehind() {
        CharacterSheet sheet = sheetOf(character().build());

        assertNull(sheet.enterForm(FormType.MONSTRUOSA));
        assertTrue(sheet.isInForm(FormType.MONSTRUOSA));

        assertEquals(FormType.MONSTRUOSA, sheet.enterForm(FormType.FEERICA));
        assertTrue(sheet.isInForm(FormType.FEERICA));

        assertEquals(FormType.FEERICA, sheet.enterForm(null), "null is the way back");
        assertNull(sheet.getCurrentForm());
    }

    /** With no Talento holding an opinion, every shape is open — the state of nearly every character. */
    @Test
    void withNoOpinionatedTalentoEveryFormIsAvailable() {
        CharacterSheet sheet = sheetOf(character().build());

        for (FormType form : FormType.values()) {
            assertTrue(sheet.canTakeForm(form), form + " should be open");
        }
        assertTrue(sheet.canTakeForm(null));
    }

    /** {@code ACOLHIDA_POR_FLORA} — "não pode acessar a forma monstruosa", and nothing else. */
    @Test
    void aForbiddingTalentoRefusesOnlyTheShapeItNames() {
        Character gorgona = gorgona();
        gorgona.grantFeat(GorgonaFeat.ACOLHIDA_POR_FLORA);
        CharacterSheet sheet = sheetOf(gorgona);

        assertFalse(sheet.canTakeForm(FormType.MONSTRUOSA));
        assertTrue(sheet.canTakeForm(FormType.FEERICA));
        assertTrue(sheet.canTakeForm(null));
    }

    /**
     * {@code MARCA_DA_MALDICAO} — "sempre em sua forma monstruosa e incapaz de alternar". A lock
     * refuses every other shape <em>and</em> the holder's own, which is what makes it a lock
     * rather than a preference.
     */
    @Test
    void aLockingTalentoRefusesEveryOtherShapeIncludingTheirOwn() {
        Character gorgona = gorgona();
        gorgona.grantFeat(GorgonaFeat.MARCA_DA_MALDICAO);
        CharacterSheet sheet = sheetOf(gorgona);

        assertTrue(sheet.canTakeForm(FormType.MONSTRUOSA));
        assertFalse(sheet.canTakeForm(FormType.FEERICA));
        assertFalse(sheet.canTakeForm(null), "there is no going back");
    }

    /** The gate is a question, not a guard: the mutator itself validates nothing. */
    @Test
    void enterFormIgnoresTheGateBecauseItIsTheCallersToAsk() {
        Character gorgona = gorgona();
        gorgona.grantFeat(GorgonaFeat.ACOLHIDA_POR_FLORA);
        CharacterSheet sheet = sheetOf(gorgona);

        sheet.enterForm(FormType.MONSTRUOSA);

        assertTrue(sheet.isInForm(FormType.MONSTRUOSA));
        assertFalse(sheet.canTakeForm(FormType.MONSTRUOSA));
    }

    // ---------- What reads the Forma ----------

    /**
     * The second thing a Forma is read for, after the Resistência a Críticos below: which Armas
     * Naturais their holder can strike with. A shape claiming none leaves the answer alone, which
     * is every Forma outside {@code FormaMetamorfica}'s table — the Górgona shapes included.
     *
     * <p>The mechanism itself ({@code Feat#getGrantedNaturalWeapons(Character, CombatantSheet)}
     * and {@code Feat#resolveRacialTraitSuppression}) is exercised in {@code
     * MetamorfoseDraculeaTest} and {@code RacialTraitSuppressionTest}; what belongs here is that
     * the sheet view is Forma-aware at all, and that it stays inert for a shape with no claim
     * on it.
     */
    @Test
    void theNaturalWeaponViewIsFormaAwareButInertForAShapeThatClaimsNothing() {
        Character gorgona = gorgona();
        CharacterSheet sheet = sheetOf(gorgona);
        List<NaturalWeapon> own = sheet.getNaturalWeapons();

        assertEquals(gorgona.getNaturalWeapons(), own,
                "out of any Forma the sheet view is exactly the Character view");

        sheet.enterForm(FormType.MONSTRUOSA);

        assertEquals(own, sheet.getNaturalWeapons(),
                "Forma Monstruosa names no Arma Natural, so it takes and gives nothing");
    }

    /**
     * The third: how much of the holder's race is silenced. Entering a shape no Talento of theirs
     * claims suppresses nothing — the answer for the Górgona shapes, Draconato on a non-holder,
     * and every Forma reached through the bare mutator.
     */
    @Test
    void theRacialSuppressionViewIsInertWithoutATalentoClaimingTheShape() {
        CharacterSheet sheet = sheetOf(gorgona());

        assertEquals(RacialTraitSuppression.NONE, sheet.getRacialTraitSuppression());

        sheet.enterForm(FormType.MONSTRUOSA);

        assertEquals(RacialTraitSuppression.NONE, sheet.getRacialTraitSuppression(),
                "a Górgona's own cursed shape abandons no racial trait");
    }

    /**
     * {@code GorgonaFeat#PROTECAO_DO_DEUS_DOS_MONSTROS}'s "enquanto em sua Forma Monstruosa você
     * recebe Resistência à Críticos" — the first clause to read the Forma, through
     * {@code Feat#resolveCriticalResistance}'s holder-taking overload.
     */
    @Test
    void aFormGatedCriticalResistanceAppliesOnlyInThatForm() {
        Character gorgona = gorgona();
        gorgona.grantFeat(GorgonaFeat.PROTECAO_DO_DEUS_DOS_MONSTROS);
        CharacterSheet sheet = sheetOf(gorgona);

        assertEquals(0, sheet.getTotalCriticalResistance(null));

        sheet.enterForm(FormType.MONSTRUOSA);
        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, sheet.getTotalCriticalResistance(null));

        sheet.enterForm(FormType.FEERICA);
        assertEquals(0, sheet.getTotalCriticalResistance(null), "the wrong shape grants nothing");
    }

    /** Its twin reads the other shape, and the two do not answer for each other. */
    @Test
    void theFeericaTwinGatesOnItsOwnForm() {
        Character gorgona = gorgona();
        gorgona.grantFeat(GorgonaFeat.PROTECAO_DA_RAINHA_DAS_FADAS);
        CharacterSheet sheet = sheetOf(gorgona);

        sheet.enterForm(FormType.MONSTRUOSA);
        assertEquals(0, sheet.getTotalCriticalResistance(null));

        sheet.enterForm(FormType.FEERICA);
        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, sheet.getTotalCriticalResistance(null));
    }
}
