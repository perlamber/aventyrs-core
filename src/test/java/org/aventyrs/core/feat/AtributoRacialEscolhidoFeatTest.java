package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.Guampo;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MonstruosoFeat's chosen Bônus Racial — ALFA, DUAS_CABECAS and CERBERO's left-over Atributo —
 * judged by {@code Character#getEffectiveAttributeTotal}, the figure every Atributo reader uses.
 */
class AtributoRacialEscolhidoFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character holding(final Feat... feats) {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>(List.of(feats))).build();
    }

    private static int gain(final AttributeDomain domain, final Feat... feats) {
        return holding(feats).getEffectiveAttributeTotal(domain) - holding().getEffectiveAttributeTotal(domain);
    }

    @SuppressWarnings("unchecked")
    private static List<AttributeDomain> options(final Feat feat, final Character holder) {
        List<FeatChoice<?>> choices = feat.resolveRequiredChoices(holder);
        assertEquals(1, choices.size());
        assertEquals(AttributeDomain.class, choices.get(0).type());
        return (List<AttributeDomain>) choices.get(0).options();
    }

    // ---------- DUAS_CABECAS ----------

    @Test
    void twoHeadsOffersGnoseOrInstinto() {
        assertEquals(List.of(AttributeDomain.INSTINCT, AttributeDomain.GNOSE),
                options(MonstruosoFeat.DUAS_CABECAS, holding()));
    }

    @Test
    void theChosenAtributoRisesByOne() {
        Feat twoHeads = AtributoRacialEscolhidoFeat.of(MonstruosoFeat.DUAS_CABECAS, AttributeDomain.GNOSE);

        assertEquals(1, gain(AttributeDomain.GNOSE, twoHeads));
        assertEquals(0, gain(AttributeDomain.INSTINCT, twoHeads));
    }

    /** The acquired form still counts as DUAS_CABECAS for CERBERO's own Pré-requisito. */
    @Test
    void theAcquiredFormIsTheCatalogConstant() {
        assertEquals(MonstruosoFeat.DUAS_CABECAS,
                AtributoRacialEscolhidoFeat.of(MonstruosoFeat.DUAS_CABECAS, AttributeDomain.GNOSE).catalogEntry());
    }

    @Test
    void aBestialQualifiesWithoutATitulo() {
        Character bestial = CharacterFixture.blank(CharacterFixture.BLANK).race(new Bestial())
                .feats(new ArrayList<>()).build();

        assertTrue(MonstruosoFeat.DUAS_CABECAS.isEligible(bestial));
    }

    /** Any other Monstruosa needs a Título Desperto — the general branch of the disjunction. */
    @Test
    void anotherMonstruosoNeedsATitulo() {
        Character guampo = CharacterFixture.blank(CharacterFixture.BLANK).race(new Guampo())
                .feats(new ArrayList<>()).build();

        assertFalse(MonstruosoFeat.DUAS_CABECAS.isEligible(guampo));

        guampo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(MonstruosoFeat.DUAS_CABECAS.isEligible(guampo));
    }

    // ---------- CERBERO ----------

    @Test
    void cerberoGrantsTheAtributoTwoHeadsLeftOut() {
        Feat twoHeads = AtributoRacialEscolhidoFeat.of(MonstruosoFeat.DUAS_CABECAS, AttributeDomain.GNOSE);

        assertEquals(1, gain(AttributeDomain.INSTINCT, twoHeads, MonstruosoFeat.CERBERO));
        assertEquals(1, gain(AttributeDomain.GNOSE, twoHeads, MonstruosoFeat.CERBERO));
    }

    @Test
    void cerberoWithNoRecordedChoiceGrantsNothing() {
        assertEquals(0, gain(AttributeDomain.INSTINCT, MonstruosoFeat.DUAS_CABECAS, MonstruosoFeat.CERBERO));
        assertEquals(0, gain(AttributeDomain.GNOSE, MonstruosoFeat.DUAS_CABECAS, MonstruosoFeat.CERBERO));
    }

    // ---------- ALFA ----------

    /** "um dos atributos cedidos por sua Raça ou no atributo 'Força'". */
    @Test
    void alphaOffersTheRaciallyBoostedAtributosPlusForca() {
        Character holder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).fixedRacialBonus(1).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(2).fixedRacialBonus(1)
                                .build())
                        .build())
                .feats(new ArrayList<>())
                .build();

        assertEquals(List.of(AttributeDomain.VIGOR, AttributeDomain.STRENGTH, AttributeDomain.INSTINCT),
                options(MonstruosoFeat.ALFA, holder));
    }

    @Test
    void alphaWithNoRacialBonusOffersForcaAlone() {
        assertEquals(List.of(AttributeDomain.STRENGTH), options(MonstruosoFeat.ALFA, holding()));
    }

    @Test
    void alphaRaisesItsPick() {
        assertEquals(1, gain(AttributeDomain.STRENGTH,
                AtributoRacialEscolhidoFeat.of(MonstruosoFeat.ALFA, AttributeDomain.STRENGTH)));
    }

    // ---------- HumanoFeat#LIMIAR_DA_EVOLUCAO ----------

    @Test
    void limiarOffersEveryAtributo() {
        assertEquals(List.of(AttributeDomain.values()), options(HumanoFeat.LIMIAR_DA_EVOLUCAO, holding()));
    }

    @Test
    void limiarRaisesItsPick() {
        Feat limiar = AtributoRacialEscolhidoFeat.of(HumanoFeat.LIMIAR_DA_EVOLUCAO, AttributeDomain.CHARISMA);

        assertEquals(1, gain(AttributeDomain.CHARISMA, limiar));
        assertEquals(HumanoFeat.LIMIAR_DA_EVOLUCAO, limiar.catalogEntry());
    }
}
