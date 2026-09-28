package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.Sobrecura;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The limits on healing the fallen — {@link CombatantSheet#heal(int, HealingSource)}: a character in
 * Coma recovers at most 1PV from each distinct heal effect for as long as the Coma lasts (a real
 * Descanso excepted, capped but repeatable), and a dead one nothing, unless a Título says otherwise
 * (see {@code CurandeiroIntegrationTest} for the Títulos that do).
 */
class FallenHealingTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private CharacterSheet sheet;
    private int max;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        max = hitPointsService.getMaxHitPoints(sheet.getCharacter(), sheet);
    }

    /**
     * Damage leaving the sheet at -max + 1 PV — the bottom of Coma, so a few 1PV heals still leave it
     * there (Coma runs up to -max/2 PV).
     */
    private int comaDamage() {
        return 2 * max - 1;
    }

    private void intoComa() {
        sheet.applyDamage(comaDamage());
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(sheet));
    }

    private void intoDeath() {
        sheet.applyDamage(2 * max);
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(sheet));
    }

    private static HealingSource revigorar() {
        return HealingSource.spell(VidaSpell.REVIGORAR, null);
    }

    @Test
    void aHealInComaRecoversAtMostOnePv() {
        intoComa();

        sheet.heal(10, revigorar());

        assertEquals(comaDamage() - CombatantSheet.COMA_HEAL_CAP, sheet.getDamageTaken());
    }

    @Test
    void eachDistinctEffectGivesOnePvAndTheSameOneNothingMore() {
        intoComa();

        sheet.heal(10, revigorar());
        sheet.heal(10, HealingSource.spell(VidaSpell.BENCAO_DA_LUZ, null));
        sheet.heal(10, revigorar());

        assertEquals(comaDamage() - 2, sheet.getDamageTaken());
    }

    @Test
    void anEffectAlreadyUsedLeavesASangramentoRunning() {
        intoComa();
        sheet.heal(10, revigorar());
        sheet.applyEffect(new Bleeding(1, Optional.of(5)));

        sheet.heal(10, revigorar());
        int afterHeal = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(afterHeal + 1, sheet.getDamageTaken(), "no cure landed to interrupt it");
    }

    @Test
    void aRealRestIsCappedButRepeatable() {
        intoComa();

        sheet.heal(10, HealingSource.rest(RestType.LONGO));
        sheet.heal(10, HealingSource.rest(RestType.LONGO));

        assertEquals(comaDamage() - 2, sheet.getDamageTaken());
    }

    @Test
    void aCorrenteIsOneHealEffectWithItsMagia() {
        intoComa();

        sheet.heal(10, revigorar());
        sheet.heal(10, HealingSource.spellChain(Sobrecura.class, VidaSpell.REVIGORAR, null));

        assertEquals(comaDamage() - 1, sheet.getDamageTaken());
    }

    @Test
    void aRegeneracaoBudgetIsOneEffectInComa() {
        intoComa();
        Regeneration regeneration = new Regeneration(5, 3, null, "test", 1);

        regeneration.applyRoundEffect(sheet);
        regeneration.applyRoundEffect(sheet);

        assertEquals(comaDamage() - 1, sheet.getDamageTaken());
    }

    @Test
    void aRegeneracaoCannotRaiseTheDead() {
        intoDeath();

        new Regeneration(5, 3, null, "test", 1).applyRoundEffect(sheet);

        assertEquals(2 * max, sheet.getDamageTaken());
    }

    @Test
    void leavingComaFreesEveryEffectForTheNextOne() {
        intoComa();
        sheet.heal(10, revigorar());

        sheet.heal(comaDamage());                // an unsourced recovery, out of Coma entirely
        intoComa();
        sheet.heal(10, revigorar());

        assertEquals(comaDamage() - 1, sheet.getDamageTaken());
    }

    @Test
    void theUsedEffectsOutlastANewCena() {
        intoComa();
        sheet.heal(10, revigorar());

        sheet.startNewScene();
        sheet.heal(10, revigorar());

        assertEquals(comaDamage() - 1, sheet.getDamageTaken());
    }

    @Test
    void aComaBegunInThisCenaStopsBeingSoAtTheNext() {
        assertFalse(sheet.hasEnteredComaThisScene());
        intoComa();
        assertTrue(sheet.hasEnteredComaThisScene());

        sheet.startNewScene();

        assertFalse(sheet.hasEnteredComaThisScene());
    }

    @Test
    void theDeadRefuseEverySourcedHeal() {
        intoDeath();

        sheet.heal(10, revigorar());
        sheet.heal(10, HealingSource.rest(RestType.TOTAL));
        sheet.heal(10, HealingSource.titleAbility(org.aventyrs.core.title.santo.SantoSpecialization.ABENCOADO_PELA_LUZ, sheet));

        assertEquals(2 * max, sheet.getDamageTaken());
    }

    @Test
    void theUnsourcedAndLifeStealPathsAreExempt() {
        intoComa();

        sheet.healFromLifeSteal(3);
        assertEquals(comaDamage() - 3, sheet.getDamageTaken());

        sheet.heal(2);
        assertEquals(comaDamage() - 5, sheet.getDamageTaken());
    }

    @Test
    void roundsSinceDeathCountFromZeroAndCloseAtTheNextCena() {
        assertEquals(OptionalInt.empty(), sheet.getRoundsSinceDeath());
        intoDeath();
        assertEquals(OptionalInt.of(0), sheet.getRoundsSinceDeath());

        sheet.startNewRound();
        sheet.startNewRound();
        assertEquals(OptionalInt.of(2), sheet.getRoundsSinceDeath());

        sheet.startNewScene();
        assertEquals(OptionalInt.empty(), sheet.getRoundsSinceDeath());
    }

    @Test
    void revivalChargesStackAndAreSpentOneAtATime() {
        sheet.grantCharge("source");
        sheet.grantCharge("source");

        assertEquals(2, sheet.getCharges("source"));
        assertTrue(sheet.consumeCharge("source"));
        assertTrue(sheet.consumeCharge("source"));
        assertFalse(sheet.consumeCharge("source"));
    }

    @Test
    void anUnspentRevivalChargeIsDroppedAtTheNextCena() {
        sheet.grantCharge("source");

        sheet.startNewScene();

        assertEquals(0, sheet.getCharges("source"));
    }

    @Test
    void beyondRevivalIsPermanent() {
        assertFalse(sheet.isBeyondRevival());

        sheet.markBeyondRevival();
        sheet.startNewScene();

        assertTrue(sheet.isBeyondRevival());
    }

    @Test
    void healingSourceKeysSpellsByNameAndReadsTheirTree() {
        assertEquals(revigorar().key(), HealingSource.spell(VidaSpell.REVIGORAR, sheet).key());
        assertEquals(revigorar().key(), HealingSource.spellChain(Sobrecura.class, VidaSpell.REVIGORAR, null).key());
        assertTrue(HealingSource.rest(RestType.CURTO).repeatableInComa());
        assertFalse(revigorar().repeatableInComa());
        assertTrue(revigorar().isSpellOfType(VidaSpell.REVIGORAR.getTree().getMagicType()));
        assertFalse(HealingSource.rest(RestType.CURTO).isSpellOfType(MagicType.values()));
    }
}
