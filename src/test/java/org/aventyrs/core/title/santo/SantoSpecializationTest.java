package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SantoSpecializationTest {

    private final RestService restService = new RestServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    // Every Título Aventyr has exactly two Especializações.
    @Test
    void hasBothDescribedSpecializations() {
        assertEquals(2, SantoSpecialization.values().length);
    }

    @Test
    void everySpecializationHasADescription() {
        for (SantoSpecialization specialization : SantoSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void abencoadoPelaLuzHasTheRightActivationCost() {
        assertEquals(1, SantoSpecialization.ABENCOADO_PELA_LUZ.getPDCost());
        assertEquals(2, SantoSpecialization.ABENCOADO_PELA_LUZ.getActionPointCost());
    }

    // "Custo de Ativação: Variável" refers to Fúria dos Deuses' own PV cost, not a PD/PA cost
    // — genuinely 0/0, not merely unmodeled. See this enum's own comment on the constant.
    @Test
    void abracadoPelaEscuridaoHasNoFixedActivationCost() {
        assertEquals(0, SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getPDCost());
        assertEquals(0, SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getActionPointCost());
    }

    @Test
    void resolveShortRestHealAmountMatchesRestServicesOwnShortRestFormula() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        int expected = restService.getRecoveredHitPoints(character, RestType.CURTO);

        assertEquals(expected, SantoSpecialization.ABENCOADO_PELA_LUZ.resolveShortRestHealAmount(character, restService));
    }

    @Test
    void resolveShortRestHealAmountDoesNotApplyToAbracadoPelaEscuridao() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertEquals(0, SantoSpecialization.ABRACADO_PELA_ESCURIDAO.resolveShortRestHealAmount(character, restService));
    }

    // AventyrTitleSpecialization now extends AventyrTitleAbility — a Título trait with a real
    // activation cost is an Active Ability regardless of which catalog it's in.
    @Test
    void neitherSpecializationIsSupreme() {
        assertFalse(SantoSpecialization.ABENCOADO_PELA_LUZ.isSupreme());
        assertFalse(SantoSpecialization.ABRACADO_PELA_ESCURIDAO.isSupreme());
    }

    @Test
    void abencoadoPelaLuzIsActiveViaTheDerivedFormula() {
        // actionPointCost == 2, so the inherited isPassive() formula correctly reports this
        // as active with no override needed.
        assertFalse(SantoSpecialization.ABENCOADO_PELA_LUZ.isPassive());
    }

    @Test
    void abracadoPelaEscuridaoIsActiveViaItsOwnOverride() {
        // 0 PD/0 PA would make the derived formula say "passive" — its own override corrects
        // that, since its real cost is entirely PV-based, not "no cost at all".
        assertFalse(SantoSpecialization.ABRACADO_PELA_ESCURIDAO.isPassive());
    }

    // ABENCOADO_PELA_LUZ's own touch-heal-or-cure effect is activated via
    // AbencoadoPelaLuzInteraction (see Santo#activateAbencoadoPelaLuz). ABRACADO_PELA_ESCURIDAO's
    // own Fúria dos Deuses effect is still fully TODO'd, so it has no Interaction to point to yet.
    @Test
    void onlyAbencoadoPelaLuzReportsAnInteractionClass() {
        assertEquals(Optional.of(AbencoadoPelaLuzInteraction.class), SantoSpecialization.ABENCOADO_PELA_LUZ.getInteractionClass());
        assertEquals(Optional.empty(), SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getInteractionClass());
    }
}
