package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PendingAcquisition;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleAwakening;
import org.aventyrs.core.title.TitleIdentity;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_ONLY_AT_CREATION;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ACQUISITION_PREVENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DestinoFeat#DESPERTAR_ANTECIPADO} — the Título is picked when the Talento is taken at
 * creation, and awakened by {@link CharacterSheet#applySessionEndAcquisitions()}.
 */
class DespertarAntecipadoFeatTest {

    private final CharacterCreationService creationService = new CharacterCreationServiceImpl();
    private final FeatService featService = new FeatServiceImpl();

    /** Not in TitleCatalog, so never offered. */
    private record UncataloguedTitle() implements AventyrTitle {
        @Override public String getName() { return "Bruxo"; }
        @Override public Optional<TitleIdentity> getIdentity() { return Optional.of(TitleIdentity.BRUXO); }
        @Override public TitleArchetype getArchetype() { return TitleArchetype.ARCANO; }
        @Override public String getBaseEffectDescription() { return ""; }
        @Override public List<AventyrTitleSpecialization> getSpecializations() { return List.of(); }
        @Override public List<AventyrTitleAbility> getAbilities() { return List.of(); }
        @Override public void grantAbility(final AventyrTitleAbility ability) { }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Gigante — exactly two starting slots, both General. */
    private static Character newCharacter() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Gigantes())
                .feats(new ArrayList<>())
                .mimetizedSpells(new ArrayList<>())
                .build();
    }

    private CharacterSheet createdWith(final Character character, final AventyrTitle chosen) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        creationService.grantStartingFeats(character,
                List.of(DespertarAntecipadoFeat.of(character, chosen), DestinoFeat.PRODIGIO), sheet);
        return sheet;
    }

    // ---- the choice ---------------------------------------------------------------------------

    @Test
    void declaresAChoiceOfOneTitulo() {
        List<FeatChoice<?>> choices = DestinoFeat.DESPERTAR_ANTECIPADO.resolveRequiredChoices(newCharacter());

        assertEquals(1, choices.size());
        assertEquals(AventyrTitle.class, choices.get(0).type());
        assertEquals(1, choices.get(0).picks());
        assertTrue(choices.get(0).options().stream().anyMatch(Santo.class::isInstance));
    }

    @Test
    void carriesTheChosenTituloAndReportsDespertarAntecipadoAsItsCatalogEntry() {
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        DespertarAntecipadoFeat feat = DespertarAntecipadoFeat.of(newCharacter(), santo);

        assertSame(DestinoFeat.DESPERTAR_ANTECIPADO, feat.catalogEntry());
        assertSame(santo, feat.getChosenTitle());
        assertEquals(FeatCategory.DESTINO, feat.getFeatCategory());
    }

    @Test
    void refusesATituloItDoesNotOffer() {
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> DespertarAntecipadoFeat.of(newCharacter(), new UncataloguedTitle()));

        assertEquals(DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE, error.getMessage());
    }

    @Test
    void refusesTheBareConstantAtCreation() {
        Character character = newCharacter();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character,
                        List.of(DestinoFeat.DESPERTAR_ANTECIPADO, DestinoFeat.PRODIGIO)));

        assertEquals(FEAT_REQUIRES_CHOICE, error.getMessage());
    }

    // ---- "Apenas personagens recém-criados" --------------------------------------------------

    @Test
    void isOfferedInAStartingSlot() {
        Character character = newCharacter();

        assertTrue(creationService.getStartingFeatOptions(character, creationService.getStartingFeatSlots(character.getRace()).get(0))
                .contains(DestinoFeat.DESPERTAR_ANTECIPADO));
    }

    @Test
    void cannotBeTakenAfterCreation() {
        Character character = newCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet,
                        DespertarAntecipadoFeat.of(character, new Santo(List.of(), List.of()))));

        assertEquals(FEAT_ONLY_AT_CREATION, error.getMessage());
        assertFalse(featService.getAvailableFeats(character, sheet).contains(DestinoFeat.DESPERTAR_ANTECIPADO));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(sheet.getUnUsedExperience()), "no XP spent");
    }

    // ---- awakening at session end -------------------------------------------------------------

    @Test
    void doesNotAwakenTheTituloAtAcquisition() {
        Character character = newCharacter();

        createdWith(character, new Santo(List.of(), List.of()));

        assertNull(character.getPrimaryTitle());
    }

    @Test
    void awakensTheChosenTituloAsPrimarioAtSessionEnd() {
        Character character = newCharacter();
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());
        CharacterSheet sheet = createdWith(character, santo);

        List<PendingAcquisition> granted = sheet.applySessionEndAcquisitions();

        assertSame(santo, character.getPrimaryTitle());
        assertEquals(1, granted.size());
        TitleAwakening awakening = assertInstanceOf(TitleAwakening.class, granted.get(0));
        assertSame(santo, awakening.title());
    }

    @Test
    void aSecondSessionEndAwakensNothingMore() {
        Character character = newCharacter();
        CharacterSheet sheet = createdWith(character, new Santo(List.of(), List.of()));
        sheet.applySessionEndAcquisitions();
        AventyrTitle awakened = character.getPrimaryTitle();

        assertEquals(List.of(), sheet.applySessionEndAcquisitions());
        assertSame(awakened, character.getPrimaryTitle());
    }

    @Test
    void owesNothingOnceThePrimarioSlotIsFilled() {
        Character character = newCharacter();
        CharacterSheet sheet = createdWith(character, new Santo(List.of(), List.of()));
        Santo grantedByNarrador = new Santo(List.of(), List.of());
        character.grantTitle(grantedByNarrador, TitleSlot.PRIMARY);

        assertEquals(List.of(), sheet.applySessionEndAcquisitions());
        assertSame(grantedByNarrador, character.getPrimaryTitle());
    }

    @Test
    void aSheetWithoutSuchTalentosOwesNothing() {
        Character character = newCharacter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertEquals(List.of(), sheet.applySessionEndAcquisitions());
        assertNull(character.getPrimaryTitle());
    }

    /** The prohibition is checked again when the Título awakens, not only when it was picked. */
    @Test
    void aProhibitionTakenAfterThePickStopsTheAwakening() {
        Character character = newCharacter();
        DespertarAntecipadoFeat feat = new DespertarAntecipadoFeat(new UncataloguedTitle());
        character.grantFeat(feat);
        character.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                sheet::applySessionEndAcquisitions);

        assertEquals(TITLE_ACQUISITION_PREVENTED, error.getMessage());
        assertNull(character.getPrimaryTitle());
    }
}
