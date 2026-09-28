package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.InitiativeService;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "Você adquire 1 ponto permanente de …" — {@code Feat#resolveEgoBonus}, read through {@code
 * Character#getEffectiveEgoTotal} by the permanent Ego pool and by Iniciativa.
 */
class FeatEgoBonusTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character holding(final Feat... feats) {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>(List.of(feats))).build();
    }

    private static int permanentPointsGain(final EgoDomain domain, final Feat feat) {
        return CharacterSheet.of(holding(feat), new Player()).getPermanentEgoPoints(domain)
                - CharacterSheet.of(holding(), new Player()).getPermanentEgoPoints(domain);
    }

    @Test
    void sorteDeMosesAddsAPermanentSortePoint() {
        assertEquals(1, permanentPointsGain(EgoDomain.SORTE, SobrevivenciaFeat.SORTE_DE_MOSES));
        assertEquals(0, permanentPointsGain(EgoDomain.AUTOCONTROLE, SobrevivenciaFeat.SORTE_DE_MOSES));
    }

    @Test
    void determinacaoDeMosesAddsAPermanentAutocontrolePoint() {
        assertEquals(1, permanentPointsGain(EgoDomain.AUTOCONTROLE, SobrevivenciaFeat.DETERMINACAO_DE_MOSES));
    }

    @Test
    void iniciativaAprimoradaRaisesBothThePoolAndTheTurnOrderFigure() {
        InitiativeService initiativeService = new InitiativeServiceImpl();

        assertEquals(1, permanentPointsGain(EgoDomain.INICIATIVA, MobilidadeFeat.INICIATIVA_APRIMORADA));
        assertEquals(1, initiativeService.getTotalInitiative(holding(MobilidadeFeat.INICIATIVA_APRIMORADA))
                - initiativeService.getTotalInitiative(holding()));
    }

    /** The invested base is untouched: an Ego ceiling Pré-requisito still reads what was bought. */
    @Test
    void theEgoBaseIsNotChanged() {
        Character holder = holding(SobrevivenciaFeat.SORTE_DE_MOSES);

        assertEquals(holding().getEgos().getSorte().getBase(), holder.getEgos().getSorte().getBase());
        assertEquals(holder.getEgos().getSorte().getTotal() + 1, holder.getEffectiveEgoTotal(EgoDomain.SORTE));
    }
}
