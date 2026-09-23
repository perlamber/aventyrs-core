package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.PDCost;
import org.junit.jupiter.api.BeforeEach;
import org.aventyrs.core.sheet.ActionCost;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SantoSpecializationTest {


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
        assertEquals(PDCost.fixed(0), SantoSpecialization.ABENCOADO_PELA_LUZ.getPDCost());
        assertEquals(ActionCost.ofActionPoints(2), SantoSpecialization.ABENCOADO_PELA_LUZ.getActionPointCost());
    }

    // "Custo de Ativação: Variável" refers to Fúria dos Deuses' own PV cost, not a PD/PA cost
    // — genuinely none of either, not merely unmodeled. See this enum's own comment on the
    // constant, and its isPassive() override, which is why ActionCost.NONE doesn't make it passive.
    @Test
    void abracadoPelaEscuridaoHasNoFixedActivationCost() {
        assertEquals(PDCost.fixed(0), SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getPDCost());
        assertEquals(ActionCost.NONE, SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getActionPointCost());
    }

    /** "recupere 3+ Quantidade de Habilidades de Abençoado pela Luz PV" — 3 with none held. */
    @Test
    void resolveTouchHealAmountIsTheBaseAloneWithNoGatedHabilidadesHeld() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL,
                SantoSpecialization.ABENCOADO_PELA_LUZ.resolveTouchHealAmount(title));
    }

    @Test
    void resolveTouchHealAmountCountsEveryHabilidadeGatedOnThisEspecializacao() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO,
                        AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO));

        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL + 2,
                SantoSpecialization.ABENCOADO_PELA_LUZ.resolveTouchHealAmount(title));
    }

    /** "de Abençoado pela Luz" — a Habilidade from Santo's own catalog is not one of them. */
    @Test
    void resolveTouchHealAmountIgnoresHabilidadesFromASiblingCatalog() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA));

        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL,
                SantoSpecialization.ABENCOADO_PELA_LUZ.resolveTouchHealAmount(title));
    }

    @Test
    void resolveTouchHealAmountDoesNotApplyToAbracadoPelaEscuridao() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), List.of());

        assertEquals(0, SantoSpecialization.ABRACADO_PELA_ESCURIDAO.resolveTouchHealAmount(title));
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
    void bothEspecializacoesReportAnInteractionClass() {
        assertEquals(Optional.of(AbencoadoPelaLuzInteraction.class), SantoSpecialization.ABENCOADO_PELA_LUZ.getInteractionClass());
        assertEquals(Optional.of(FuriaDosDeusesInteraction.class),
                SantoSpecialization.ABRACADO_PELA_ESCURIDAO.getInteractionClass());
    }
}
