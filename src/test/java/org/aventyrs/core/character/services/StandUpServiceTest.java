package org.aventyrs.core.character.services;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Levantar-se — how Caído, open-ended since core 0.1.5, ends. */
class StandUpServiceTest {

    private final StandUpService service = new StandUpServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final Feat... feats) {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>(List.of(feats))).build(), new Player());
    }

    @Test
    void standingUpCostsOnePaAndLiftsCaido() {
        CharacterSheet prone = sheet();
        prone.applyCondition(new Condition(ConditionType.CAIDO, null));

        StandUpService.StandUpResult result = service.standUp(prone, null);

        assertEquals(StandUpService.STAND_UP_COST, result.actionCost());
        assertFalse(prone.hasCondition(ConditionType.CAIDO, null));
    }

    @Test
    void onlyACaidoCombatantStandsUp() {
        assertEquals(TranslatableMessages.NOT_PRONE,
                assertThrows(IllegalOperationException.class, () -> service.standUp(sheet(), null)).getMessage());
    }

    /** Desacordado's Caído stays until Desacordado itself has passed. */
    @Test
    void aDesacordadoCombatantCannotStandUp() {
        CharacterSheet out = sheet();
        out.applyCondition(new Condition(ConditionType.DESACORDADO, ConditionType.DESACORDADO_DURATION_IN_ROUNDS));

        assertFalse(service.canStandUp(out, null));
    }

    /** Submissão: "pode se levantar como Ação Livre" — which a Confuso holder may not take. */
    @Test
    void submissaoStandsUpAsAnAcaoLivreUnlessConfuso() {
        CharacterSheet artist = sheet(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO);
        artist.applyCondition(new Condition(ConditionType.CAIDO, null));
        assertEquals(ActionCost.FREE_ACTION, service.getStandUpCost(artist.getCharacter()));

        artist.applyCondition(new Condition(ConditionType.CONFUSO, 2));

        assertFalse(service.canStandUp(artist, null));
    }

    @Test
    void confusoAddsOnePaToStandingUp() {
        CharacterSheet prone = sheet();
        prone.applyCondition(new Condition(ConditionType.CAIDO, null));
        prone.applyCondition(new Condition(ConditionType.CONFUSO, 2));

        assertEquals(2, service.standUp(prone, null).actionCost().actionPoints());
    }
}
