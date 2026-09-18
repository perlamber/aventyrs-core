package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.title.PDCost;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.title.AventyrTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbracadoPelaEscuridaoAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void everyAbilityHasADescription() {
        for (AbracadoPelaEscuridaoAbility ability : AbracadoPelaEscuridaoAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void hasTheFourDescribedAbilities() {
        assertEquals(4, AbracadoPelaEscuridaoAbility.values().length);
    }

    @Test
    void onlyFurorDeSylphReportsIsSupremeTrue() {
        assertFalse(AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.isSupreme());
        assertFalse(AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.isSupreme());
        assertFalse(AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.isSupreme());
        assertTrue(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.isSupreme());
    }

    @Test
    void sacrificioYmirianoHasNoFixedPdCostOnlyAVariablePvOne() {
        assertEquals(PDCost.fixed(0), AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.getPDCost());
        assertEquals(ActionCost.ofActionPoints(1), AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.getActionPointCost());
    }

    @Test
    void espinhosDeGaeaHasNoFixedPdCostOnlyAVariablePvOne() {
        assertEquals(PDCost.fixed(0), AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.getPDCost());
        assertEquals(ActionCost.ofActionPoints(2), AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.getActionPointCost());
    }

    @Test
    void placidezDeUndineRancorDeHaloiHasAFixedActivationCost() {
        assertEquals(PDCost.fixed(2), AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.getPDCost());
        assertEquals(ActionCost.ofActionPoints(3), AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.getActionPointCost());
    }

    @Test
    void furorDeSylphIsAFreeActionActivation() {
        assertEquals(PDCost.fixed(2), AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.getPDCost());
        assertEquals(ActionCost.FREE_ACTION, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.getActionPointCost());
    }

    @Test
    void noAbilityIsPassive() {
        // All four have an explicit activation (PA, or Ação Livre for FUROR_DE_SYLPH, which
        // spends no PA but is still player-triggered) — none is "Custo de Ativação: Nenhum,
        // habilidade passiva".
        for (AbracadoPelaEscuridaoAbility ability : AbracadoPelaEscuridaoAbility.values()) {
            assertFalse(ability.isPassive());
        }
    }

    private Character characterWithVigor(final int vigorBase) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
    }

    @Test
    void resolveVigorPvCostMatchesVigorsOwnTotalForSacrificioYmiriano() {
        Character character = characterWithVigor(4);

        assertEquals(4, AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.resolveVigorPvCost(character));
    }

    @Test
    void resolveVigorPvCostMatchesVigorsOwnTotalForFurorDeSylph() {
        Character character = characterWithVigor(3);

        assertEquals(3, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveVigorPvCost(character));
    }

    @Test
    void resolveVigorPvCostDoesNotApplyToTheOtherTwoConstants() {
        Character character = characterWithVigor(5);

        assertEquals(0, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.resolveVigorPvCost(character));
        assertEquals(0, AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.resolveVigorPvCost(character));
    }

    @Test
    void resolveDurationFromPvSpentMatchesEspinhosDeGaeasOwnFormula() {
        assertEquals(1, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.resolveDurationFromPvSpent(1));
        assertEquals(2, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.resolveDurationFromPvSpent(2));
        assertEquals(4, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.resolveDurationFromPvSpent(6));
    }

    @Test
    void resolveDurationFromPvSpentDoesNotApplyToTheOtherThreeConstants() {
        assertEquals(0, AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.resolveDurationFromPvSpent(6));
        assertEquals(0, AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.resolveDurationFromPvSpent(6));
        assertEquals(0, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveDurationFromPvSpent(6));
    }

    @Test
    void resolveEnhancedAttackCountFromPvSpentMatchesFurorDeSylphsOwnFormula() {
        assertEquals(1, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveEnhancedAttackCountFromPvSpent(1));
        assertEquals(2, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveEnhancedAttackCountFromPvSpent(2));
        assertEquals(4, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveEnhancedAttackCountFromPvSpent(6));
    }

    @Test
    void resolveEnhancedAttackCountFromPvSpentDoesNotApplyToTheOtherThreeConstants() {
        assertEquals(0, AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.resolveEnhancedAttackCountFromPvSpent(6));
        assertEquals(0, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.resolveEnhancedAttackCountFromPvSpent(6));
        assertEquals(0, AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.resolveEnhancedAttackCountFromPvSpent(6));
    }

    @Test
    void noAbilityGrantsAbsoluteDamageReductionYet() {
        for (AbracadoPelaEscuridaoAbility ability : AbracadoPelaEscuridaoAbility.values()) {
            assertEquals(0, ability.resolveAbsoluteDamageReduction(null, true));
        }
    }

    @Test
    void everyAbilityRequiresTheAbracadoPelaEscuridaoEspecializacao() {
        for (AbracadoPelaEscuridaoAbility ability : AbracadoPelaEscuridaoAbility.values()) {
            assertEquals(Optional.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), ability.getRequiredSpecialization());
        }
    }

    @Test
    void onlyFurorDeSylphRequiresOtherAbilities() {
        assertEquals(0, AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.getRequiredOtherAbilities());
        assertEquals(0, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.getRequiredOtherAbilities());
        assertEquals(0, AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.getRequiredOtherAbilities());
        assertEquals(2, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.getRequiredOtherAbilities());
    }

    @Test
    void isEligibleRejectsATitleWithoutTheEspecializacao() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        for (AbracadoPelaEscuridaoAbility ability : AbracadoPelaEscuridaoAbility.values()) {
            assertFalse(ability.isEligible(title));
        }
    }

    @Test
    void isEligibleAcceptsSacrificioYmirianoOnceTheEspecializacaoIsHeld() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), List.of());

        assertTrue(AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.isEligible(title));
    }

    @Test
    void isEligibleForFurorDeSylphIgnoresAbilitiesFromASiblingCatalog() {
        // 2 SantoAbility Habilidades held, but none from this same AbracadoPelaEscuridaoAbility
        // catalog — "outras Habilidades de 'Abraçado pela Escuridão'" must not count them.
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS));

        assertFalse(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.isEligible(title));
    }

    @Test
    void isEligibleAcceptsFurorDeSylphOnceEnoughSiblingAbilitiesAreHeld() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO),
                List.of(AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO, AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA));

        assertTrue(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.isEligible(title));
    }

    // Only SACRIFICIO_YMIRIANO is activatable: ESPINHOS_DE_GAEA's whole benefit is retaliation
    // damage, FUROR_DE_SYLPH's +1PA has no Duração in Rodadas to be granted for, and
    // PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI needs "this one delivered attack" scoping.
    @Test
    void onlySacrificioYmirianoReportsAnInteractionClass() {
        assertEquals(Optional.of(SacrificioYmirianoInteraction.class),
                AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.getInteractionClass());
        assertEquals(Optional.empty(), AbracadoPelaEscuridaoAbility.ESPINHOS_DE_GAEA.getInteractionClass());
        assertEquals(Optional.empty(),
                AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI.getInteractionClass());
        assertEquals(Optional.empty(), AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.getInteractionClass());
    }
}
