package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.RelayedHealer;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.bystander;
import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.holder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HealingSource#relay}: the healer's half resolved on the healer's sheet, applied to a target
 * whose client holds no real copy of the healer — the way a Curandeiro heals another player's character.
 */
class CurandeiroRelayTest {

    @BeforeEach
    void setup() {
        CurandeiroFixtures.loadTemplates();
    }

    private static int max(final CharacterSheet sheet) {
        return new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);
    }

    @Test
    void theHealersBonusAndComaBypassTravelWithTheHeal() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS);
        HealingSource relayed = EncantoRegenerativoInteraction.source(curandeiro).relay(false);

        assertNull(relayed.healer());
        assertEquals(new RelayedHealer(Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS, true, null), relayed.relayed());

        CharacterSheet target = bystander();
        int damage = 2 * max(target) - 1;
        target.applyDamage(damage);
        int recovered = EncantoRegenerativoInteraction.healTarget(target, relayed);

        assertTrue(recovered > 1, "Levantar os Caídos lifted the Coma cap on the far side");
    }

    @Test
    void withoutTheBypassTheFarSideStillCapsTheComa() {
        HealingSource relayed = EncantoRegenerativoInteraction.source(holder()).relay(false);
        CharacterSheet target = bystander();
        target.applyDamage(2 * max(target) - 1);

        assertEquals(1, EncantoRegenerativoInteraction.healTarget(target, relayed));
    }

    @Test
    void aRevivalIsPaidForOnTheHealersSideAndJudgedOnTheTargets() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        Curandeiro.heldBy(curandeiro).orElseThrow().activateCurarOsMortos(
                TitleAbilityActivationRequest.builder().activator(curandeiro).build());

        HealingSource relayed = DonsDoCurandeiroInteraction.source(curandeiro).relay(true);

        assertEquals(0, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS));
        assertEquals(CurandeiroFixtures.MEDICINA_E_CURA / 2, relayed.relayed().revivalWindowRounds());

        CharacterSheet target = bystander();
        target.applyDamage(2 * max(target));
        DonsDoCurandeiroInteraction.healTarget(target, relayed, true);

        assertEquals(CharacterStatus.COMMA, new HitPointsServiceImpl().getStatus(target));
    }

    @Test
    void aDeadTargetOutsideTheRelayedWindowStaysDead() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        Curandeiro.heldBy(curandeiro).orElseThrow().activateCurarOsMortos(
                TitleAbilityActivationRequest.builder().activator(curandeiro).build());
        HealingSource relayed = DonsDoCurandeiroInteraction.source(curandeiro).relay(true);

        CharacterSheet target = bystander();
        target.applyDamage(2 * max(target));
        for (int round = 0; round <= CurandeiroFixtures.MEDICINA_E_CURA / 2; round++) {
            target.startNewRound();
        }

        assertEquals(0, DonsDoCurandeiroInteraction.healTarget(target, relayed, true));
    }

    @Test
    void withoutAChargeNoRevivalIsRelayed() {
        HealingSource relayed = DonsDoCurandeiroInteraction.source(
                holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS)).relay(true);

        assertNull(relayed.relayed().revivalWindowRounds());
    }

    @Test
    void curaProtetoraAndTransfersApplyOnTheTargetsOwnSheet() {
        CharacterSheet ally = bystander();
        CuraProtetoraInteraction.protect(ally);
        assertEquals(CuraProtetoraInteraction.DEFESAS_BONUS,
                ally.getTemporaryBonus(org.aventyrs.core.modifier.ModifierType.DEFESAS));

        ally.spendDeterminationPoints(2);
        assertEquals(2, new TransferirDeterminacaoInteraction().receivePoints(ally, 5));
        assertEquals(0, ally.getDeterminationSpent());
    }
}
