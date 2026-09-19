package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The acquired form of {@link DestinoFeat#EXCEPCIONALIDADE} — what it carries, and which Talentos
 * Raciais it offers and accepts. The effect of holding one is in {@code
 * GeneralFeatEffectIntegrationTest}.
 */
class ExcepcionalidadeFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Humano, so every race-gated Talento Racial is out of reach by the ordinary route. */
    private static Character.CharacterBuilder human() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Character titledHuman() {
        return human().primaryTitle(new Santo(List.of(), List.of())).build();
    }

    @Test
    void carriesTheChosenTalentoAndReportsExcepcionalidadeAsItsCatalogEntry() {
        Character holder = titledHuman();

        ExcepcionalidadeFeat feat = ExcepcionalidadeFeat.of(holder, AvianoFeat.CORACAO_ALADO);

        assertSame(DestinoFeat.EXCEPCIONALIDADE, feat.catalogEntry());
        assertSame(AvianoFeat.CORACAO_ALADO, feat.getChosenFeat());
        assertEquals(List.of(AvianoFeat.CORACAO_ALADO), feat.getGrantedFeats(holder));
        assertEquals(FeatCategory.DESTINO, feat.getFeatCategory());
    }

    @Test
    void offersARaceGatedTalentoRacialToACharacterOfAnotherRace() {
        Character holder = titledHuman();

        assertFalse(AvianoFeat.CORACAO_ALADO.isEligible(holder), "a Humano cannot take it directly");
        assertTrue(ExcepcionalidadeFeat.optionsFor(holder).contains(AvianoFeat.CORACAO_ALADO));
    }

    @Test
    void neverOffersAGeneralTalento() {
        List<Feat> options = ExcepcionalidadeFeat.optionsFor(titledHuman());

        assertFalse(options.isEmpty());
        assertTrue(options.stream().allMatch(feat -> feat.getFeatCategory().getType() == FeatCategory.Type.RACIAL));
        assertThrows(IllegalOperationException.class,
                () -> ExcepcionalidadeFeat.of(titledHuman(), DestinoFeat.CORACAO_DE_FERRO_DO_DESTINO));
    }

    /** "que você cumpra todos os demais requisitos" — only the Raça is waived. */
    @Test
    void stillDemandsEveryRequirementOtherThanRace() {
        Character untitled = human().build();

        // CORACAO_ALADO also needs a Título; TERRA_NAS_VEIAS needs Vigor 3.
        assertFalse(ExcepcionalidadeFeat.optionsFor(untitled).contains(AvianoFeat.CORACAO_ALADO));
        assertFalse(ExcepcionalidadeFeat.optionsFor(untitled).contains(OrquicoFeat.TERRA_NAS_VEIAS));
        assertThrows(IllegalOperationException.class,
                () -> ExcepcionalidadeFeat.of(untitled, AvianoFeat.CORACAO_ALADO));
    }

    @Test
    void refusesATalentoAlreadyHeld() {
        Character holder = titledHuman();
        holder.grantFeat(AvianoFeat.CORACAO_ALADO);

        assertFalse(ExcepcionalidadeFeat.optionsFor(holder).contains(AvianoFeat.CORACAO_ALADO));
        assertThrows(IllegalOperationException.class,
                () -> ExcepcionalidadeFeat.of(holder, AvianoFeat.CORACAO_ALADO));
    }

    /** A Talento Racial with a choice of its own must be handed over in its acquired form. */
    @Test
    void refusesAChoiceCarryingTalentoRacialAsItsBareConstant() {
        Character holder = titledHuman();

        assertTrue(ExcepcionalidadeFeat.optionsFor(holder).contains(DraconicoFeat.ARMAMENTO_DRACONICO));
        assertThrows(IllegalOperationException.class,
                () -> ExcepcionalidadeFeat.of(holder, DraconicoFeat.ARMAMENTO_DRACONICO));
    }

    @Test
    void withoutRaceClausesDropsEveryRaceClauseIncludingInsideAnyOf() {
        FeatRequirements requirements = FeatRequirements.builder()
                .requiredRace(Orc.class)
                .forbiddenRace(Human.class)
                .requiredCreatureType(CreatureType.FEERICO)
                .requiredAwakenedTitles(1)
                .alternative(FeatRequirements.builder().requiredRace(Orc.class).requiredAwakenedTitles(2).build())
                .build();

        FeatRequirements stripped = requirements.withoutRaceClauses();

        assertNull(stripped.requiredRace());
        assertNull(stripped.forbiddenRace());
        assertNull(stripped.requiredCreatureType());
        assertEquals(1, stripped.requiredAwakenedTitles());
        assertEquals(1, stripped.anyOf().size());
        assertNull(stripped.anyOf().get(0).requiredRace());
        assertEquals(2, stripped.anyOf().get(0).requiredAwakenedTitles());
    }
}
