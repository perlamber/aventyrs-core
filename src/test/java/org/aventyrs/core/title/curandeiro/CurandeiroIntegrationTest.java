package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.RealExecution;
import org.aventyrs.core.effect.SpellHealingEffect;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MorteSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.bystander;
import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.holder;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Levantar os Caídos and Curar os Mortos end to end — a Curandeiro casting Vida's Revigorar (a Magia
 * Natural/Divina) on a target in Coma or dead, through the ordinary {@link SpellHealingEffect}.
 */
class CurandeiroIntegrationTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private CharacterSheet target;
    private int max;

    @BeforeEach
    void setup() {
        CurandeiroFixtures.loadTemplates();
        target = bystander();
        max = hitPointsService.getMaxHitPoints(target.getCharacter(), target);
    }

    /** Revigorar cast by caster on the target — heals a Descanso Longo's worth. */
    private void revigorar(final CombatantSheet caster) {
        Spell spell = VidaSpell.REVIGORAR;
        target.receiveInteraction(new SpellHealingEffect(spell, spell.getHealing().orElseThrow(), false,
                new RestServiceImpl(), caster));
    }

    /** What Revigorar heals when a Médico de Guerra casts it — the Magia's figure plus the Especialização's +2. */
    private int revigorarAmount() {
        return new RestServiceImpl().getRecoveredHitPoints(target.getCharacter(),
                VidaSpell.REVIGORAR.getHealing().orElseThrow().restEquivalent())
                + Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS;
    }

    private void curarOsMortos(final CharacterSheet curandeiro) {
        Curandeiro.heldBy(curandeiro).orElseThrow()
                .activateCurarOsMortos(TitleAbilityActivationRequest.builder().activator(curandeiro).build());
    }

    private void intoComa() {
        target.applyDamage(2 * max - 1);
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(target));
    }

    // --- Levantar os Caídos ----------------------------------------------------------------------

    @Test
    void withoutLevantarOsCaidosAComaCapsTheHeal() {
        intoComa();

        revigorar(holder());

        assertEquals(2 * max - 2, target.getDamageTaken());
    }

    @Test
    void levantarOsCaidosHealsAComaBegunThisCenaInFull() {
        intoComa();

        revigorar(holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS));

        assertEquals(2 * max - 1 - revigorarAmount(), target.getDamageTaken());
    }

    @Test
    void levantarOsCaidosDoesNotReachAComaFromAnEarlierCena() {
        intoComa();
        target.startNewScene();

        revigorar(holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS));

        assertEquals(2 * max - 2, target.getDamageTaken());
    }

    @Test
    void levantarOsCaidosIsTheHealersNotTheTargets() {
        target = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS);
        max = hitPointsService.getMaxHitPoints(target.getCharacter(), target);
        intoComa();

        revigorar(bystander());

        assertEquals(2 * max - 2, target.getDamageTaken());
    }

    // --- Curar os Mortos -------------------------------------------------------------------------

    @Test
    void curarOsMortosPaysTwoPdAndBanksOneCharge() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(curandeiro.getCharacter(), curandeiro);

        curarOsMortos(curandeiro);

        assertEquals(pdBefore - 2,
                determinationPointsService.getCurrentDeterminationPoints(curandeiro.getCharacter(), curandeiro));
        assertEquals(1, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS));
    }

    @Test
    void curarOsMortosIsRefusedWithoutThePd() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        int pd = determinationPointsService.getCurrentDeterminationPoints(curandeiro.getCharacter(), curandeiro);
        curandeiro.spendDeterminationPoints(pd - 1);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> curarOsMortos(curandeiro));

        assertEquals(NOT_ENOUGH_DETERMINATION_POINTS, refused.getMessage());
        assertEquals(0, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS));
    }

    @Test
    void holdingCurarOsMortosIsNotEnoughWithoutAnActivation() {
        target.applyDamage(2 * max);

        revigorar(holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS));

        assertEquals(2 * max, target.getDamageTaken());
    }

    @Test
    void anActivatedCharacterRevivesTheRecentlyDeadIntoComaAndSpendsTheCharge() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.applyDamage(2 * max);
        curarOsMortos(curandeiro);

        revigorar(curandeiro);

        // The reviving heal is judged against DEAD, not COMMA, so it is not capped at 1PV.
        assertEquals(2 * max - revigorarAmount(), target.getDamageTaken());
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(target));
        assertEquals(0, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS));
    }

    @Test
    void aHealTooSmallToReviveKeepsItsPvAndASecondActivationFinishesTheJob() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        int damage = 2 * max + revigorarAmount();
        target.applyDamage(damage);
        curarOsMortos(curandeiro);

        revigorar(curandeiro);
        assertEquals(damage - revigorarAmount(), target.getDamageTaken());
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(target));

        curarOsMortos(curandeiro);
        revigorar(curandeiro);
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(target));
    }

    @Test
    void afterTheRevivalLevantarOsCaidosTakesOver() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.applyDamage(2 * max);
        curarOsMortos(curandeiro);
        revigorar(curandeiro);
        int afterRevival = target.getDamageTaken();

        revigorar(curandeiro);

        // Being brought back is a Coma begun in this Cena: no second charge, no 1PV cap.
        assertEquals(afterRevival - revigorarAmount(), target.getDamageTaken());
    }

    @Test
    void theDeadOutsideTheWindowStayDead() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.applyDamage(2 * max);
        for (int round = 0; round <= CurandeiroFixtures.MEDICINA_E_CURA / 2; round++) {
            target.startNewRound();
        }
        curarOsMortos(curandeiro);

        revigorar(curandeiro);

        assertEquals(2 * max, target.getDamageTaken());
        assertEquals(1, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS), "refused, spent nothing");
    }

    @Test
    void theLastRodadaOfTheWindowStillCounts() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.applyDamage(2 * max);
        for (int round = 0; round < CurandeiroFixtures.MEDICINA_E_CURA / 2; round++) {
            target.startNewRound();
        }
        curarOsMortos(curandeiro);

        revigorar(curandeiro);

        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(target));
    }

    @Test
    void onlyAMagiaDivinaOrNaturalReachesTheDead() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.applyDamage(2 * max);
        curarOsMortos(curandeiro);

        target.heal(10, HealingSource.spell(MorteSpell.TOQUE_ANTIVIDA, curandeiro));

        assertEquals(2 * max, target.getDamageTaken());
        assertEquals(1, curandeiro.getCharges(CurandeiroAbility.CURAR_OS_MORTOS));
    }

    @Test
    void aRealExecutionCannotBeUndone() {
        CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS);
        target.receiveInteraction(new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR));
        assertTrue(target.isBeyondRevival());
        int damage = target.getDamageTaken();
        curarOsMortos(curandeiro);

        revigorar(curandeiro);

        assertEquals(damage, target.getDamageTaken());
    }
}
