package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSheetTest {

    @BeforeEach
    public void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void damageReducesAvailableHitPointsBudget() {
        CharacterSheet sheet = newSheet();
        assertEquals(5, sheet.applyDamage(5));
    }

    @Test
    void shieldAbsorbsDamageBeforeDamageTaken() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(4);
        assertEquals(1, sheet.applyDamage(5));
        assertEquals(0, sheet.getShieldPoints());
    }

    @Test
    void curseDamageBypassesShield() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(10);
        assertEquals(5, sheet.applyCurseDamage(5));
        assertEquals(10, sheet.getShieldPoints());
    }

    @Test
    void healReducesAccumulatedDamageNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        assertEquals(0, sheet.heal(10));
    }

    @Test
    void newlyCreatedCharacterSheetHasNoBleeding() {
        CharacterSheet sheet = newSheet();

        sheet.tickTemporaryEffects();

        assertEquals(0, sheet.getDamageTaken());
    }

    @Test
    void tickTemporaryEffectsAppliesThePerRoundBleedingDamage() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.of(3)));

        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void tickTemporaryEffectsRemovesAFiniteBleedingOnceItsRoundsRunOut() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.of(2)));

        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();
        int damageAfterExpiry = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(2, damageAfterExpiry);
        assertEquals(damageAfterExpiry, sheet.getDamageTaken());
    }

    @Test
    void tickTemporaryEffectsNeverExpiresAnOpenEndedBleeding() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.empty()));

        for (int round = 0; round < 20; round++) {
            sheet.tickTemporaryEffects();
        }

        assertEquals(20, sheet.getDamageTaken());
    }

    @Test
    void healingInterruptsActiveBleedingButKeepsAlreadyDealtDamage() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(2);
        sheet.applyEffect(new Bleeding(1, Optional.of(3)));

        sheet.heal(1);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void healingWithZeroAmountDoesNotInterruptActiveBleeding() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.of(3)));

        sheet.heal(0);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void tickTemporaryEffectsAppliesThePerRoundManaDrainDamage() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new ManaDrain(1, Optional.of(3)));

        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getManaSpent());
    }

    @Test
    void tickTemporaryEffectsRemovesAFiniteManaDrainOnceItsRoundsRunOut() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new ManaDrain(1, Optional.of(2)));

        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();
        int manaSpentAfterExpiry = sheet.getManaSpent();
        sheet.tickTemporaryEffects();

        assertEquals(2, manaSpentAfterExpiry);
        assertEquals(manaSpentAfterExpiry, sheet.getManaSpent());
    }

    @Test
    void tickTemporaryEffectsNeverExpiresAnOpenEndedManaDrain() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new ManaDrain(1, Optional.empty()));

        for (int round = 0; round < 20; round++) {
            sheet.tickTemporaryEffects();
        }

        assertEquals(20, sheet.getManaSpent());
    }

    @Test
    void recoveringMagicPointsInterruptsActiveManaDrainButKeepsAlreadyDrainedMana() {
        CharacterSheet sheet = newSheet();
        sheet.spendMagicPoints(2);
        sheet.applyEffect(new ManaDrain(1, Optional.of(3)));

        sheet.recoverMagicPoints(1);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getManaSpent());
    }

    @Test
    void recoveringMagicPointsWithZeroAmountDoesNotInterruptActiveManaDrain() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new ManaDrain(1, Optional.of(3)));

        sheet.recoverMagicPoints(0);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getManaSpent());
    }

    @Test
    void tickTemporaryEffectsAdvancesBleedingAndManaDrainIndependently() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.of(1)));
        sheet.applyEffect(new ManaDrain(1, Optional.of(1)));

        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
        assertEquals(1, sheet.getManaSpent());
    }

    @Test
    void tickTemporaryEffectsAppliesThePerRoundWitheringDamageAsCurseDamage() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(10);
        sheet.applyEffect(new Withering(1, Optional.of(3)));

        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
        assertEquals(10, sheet.getShieldPoints());
    }

    @Test
    void tickTemporaryEffectsRemovesAFiniteWitheringOnceItsRoundsRunOut() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Withering(1, Optional.of(2)));

        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();
        int damageAfterExpiry = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(2, damageAfterExpiry);
        assertEquals(damageAfterExpiry, sheet.getDamageTaken());
    }

    @Test
    void applyEffectReplacesAnExistingWitheringInsteadOfStackingIt() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Withering(1, Optional.of(1)));
        sheet.applyEffect(new Withering(1, Optional.of(5)));

        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void applyEffectStillLetsCumulativeEffectsLikeBleedingStack() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new Bleeding(1, Optional.of(1)));
        sheet.applyEffect(new Bleeding(1, Optional.of(1)));

        sheet.tickTemporaryEffects();

        assertEquals(2, sheet.getDamageTaken());
    }

    @Test
    void shieldPointsAccumulate() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(3);
        assertEquals(5, sheet.addShield(2));
    }

    @Test
    void magicPointsSpentAreTrackedIndependentlyFromHitPoints() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        assertEquals(3, sheet.spendMagicPoints(3));
        assertEquals(5, sheet.getDamageTaken());
    }

    @Test
    void recoverMagicPointsReducesSpentNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.spendMagicPoints(3);
        assertEquals(0, sheet.recoverMagicPoints(10));
    }

    @Test
    void determinationPointsSpentAreTrackedIndependentlyFromOtherPools() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        sheet.spendMagicPoints(3);
        assertEquals(2, sheet.spendDeterminationPoints(2));
        assertEquals(5, sheet.getDamageTaken());
        assertEquals(3, sheet.getManaSpent());
    }

    @Test
    void recoverDeterminationPointsReducesSpentNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.spendDeterminationPoints(2);
        assertEquals(0, sheet.recoverDeterminationPoints(10));
    }

    /** A fixture Character's Egos are all {@code base 2, variable 0}, so every pool starts at 2. */
    @Test
    void bothEgoPoolsStartFullForEveryDomain() {
        CharacterSheet sheet = newSheet();
        for (EgoDomain domain : EgoDomain.values()) {
            assertEquals(2, sheet.getPermanentEgoPoints(domain));
            assertEquals(2, sheet.getTemporaryEgoPoints(domain));
            assertEquals(4, sheet.getAvailableEgoPoints(domain));
        }
    }

    @Test
    void recoverTemporaryEgoPointsNeverExceedsTheCeiling() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);

        assertEquals(1, sheet.recoverTemporaryEgoPoints(EgoDomain.SORTE, 5));
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void spendEgoPointsReducesOnlyThatDomain() {
        CharacterSheet sheet = newSheet();

        assertEquals(2, sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2).getValue());
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void spendEgoPointsReportsTheDomainAndPoolItDrewFrom() {
        CharacterSheet sheet = newSheet();
        EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 1);

        assertEquals(EgoDomain.SORTE, spend.getDomain());
        assertEquals(EgoPointType.PERMANENT, spend.getType());
        assertEquals(1, spend.getValue());
    }

    @Test
    void spendEgoPointsReportsWhatWasActuallySpentRatherThanWhatWasAsked() {
        CharacterSheet sheet = newSheet();
        assertEquals(2, sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.TEMPORARY, 10).getValue());
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
    }

    /** Spending a permanent point hurts twice — it also lowers the temporary ceiling above it. */
    @Test
    void spendingAPermanentEgoPointAlsoLowersTheTemporaryCeiling() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 1);

        assertEquals(1, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
        assertEquals(1, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(2, sheet.getAvailableEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void grantTemporaryEgoPointBonusDoesNotStackOnTopOfOneAlreadyGrantedByTheSameSource() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryEgoPointBonus(EgoDomain.SORTE, "source", 1);

        assertEquals(3, sheet.grantTemporaryEgoPointBonus(EgoDomain.SORTE, "source", 1));
        assertEquals(3, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void grantTemporaryEgoPointBonusFromADifferentSourceStacksOnTopOfAnother() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryEgoPointBonus(EgoDomain.SORTE, "source-a", 1);

        assertEquals(4, sheet.grantTemporaryEgoPointBonus(EgoDomain.SORTE, "source-b", 1));
        assertEquals(4, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void aTemporaryEgoPenaltyLowersTheCeilingWithoutTouchingPermanentPoints() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new TemporaryEgoPenalty(EgoDomain.SORTE, 1, 1));

        assertEquals(1, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(2, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void aTemporaryEgoPenaltyRestoresTheCeilingOnceItExpires() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new TemporaryEgoPenalty(EgoDomain.SORTE, 2, 1));
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.finishTurn();

        assertEquals(2, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void aTemporaryEgoPenaltyAffectsOnlyItsOwnDomain() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new TemporaryEgoPenalty(EgoDomain.SORTE, 2, 1));

        assertEquals(2, sheet.getMaxTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void famaStartsAtZero() {
        CharacterSheet sheet = newSheet();
        assertEquals(0, sheet.getFamaPositiva());
        assertEquals(0, sheet.getFamaNegativa());
    }

    @Test
    void increaseFamaPositivaAccumulatesIndependentlyFromFamaNegativa() {
        CharacterSheet sheet = newSheet();
        sheet.increaseFamaNegativa(1);

        assertEquals(2, sheet.increaseFamaPositiva(2));
        assertEquals(1, sheet.getFamaNegativa());
    }

    @Test
    void increaseFamaNegativaAccumulates() {
        CharacterSheet sheet = newSheet();
        sheet.increaseFamaNegativa(1);
        assertEquals(4, sheet.increaseFamaNegativa(3));
    }

    @Test
    void useExperienceSubtractsFromUnusedExperience() throws IllegalOperationException {
        CharacterSheet sheet = newSheet();
        sheet.accumulateExperience(BigDecimal.TEN);

        assertEquals(BigDecimal.valueOf(4), sheet.useExperience(BigDecimal.valueOf(6)));
        assertEquals(BigDecimal.valueOf(4), sheet.getUnUsedExperience());
    }

    @Test
    void useExperienceLeavesUnusedExperienceUntouchedWhenRejected() {
        CharacterSheet sheet = newSheet();
        sheet.accumulateExperience(BigDecimal.valueOf(3));

        assertThrows(IllegalOperationException.class, () -> sheet.useExperience(BigDecimal.valueOf(6)));
        assertEquals(BigDecimal.valueOf(3), sheet.getUnUsedExperience());
    }

    @Test
    void accumulateExperienceIncreasesBothTotalAndUnusedExperience() {
        CharacterSheet sheet = newSheet();
        sheet.accumulateExperience(BigDecimal.valueOf(5));

        assertEquals(BigDecimal.valueOf(7), sheet.accumulateExperience(BigDecimal.valueOf(2)));
        assertEquals(BigDecimal.valueOf(7), sheet.getTotalExperience());
        assertEquals(BigDecimal.valueOf(7), sheet.getUnUsedExperience());
    }

    @Test
    void eachCharacterSheetGetsItsOwnDistinctId() {
        CharacterSheet first = newSheet();
        CharacterSheet second = newSheet();

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void ofWithAnExplicitIdReconstructsTheGivenIdentityInsteadOfMintingANewOne() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        UUID persistedId = UUID.randomUUID();

        CharacterSheet sheet = CharacterSheet.of(character, new Player(), persistedId);

        assertEquals(persistedId, sheet.getId());
    }

    @Test
    void ofRejectsANullExplicitId() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertThrows(NullPointerException.class, () -> CharacterSheet.of(character, new Player(), null));
    }

    @Test
    void newlyCreatedCharacterSheetHasNoTemporaryBonuses() {
        CharacterSheet sheet = newSheet();

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void grantTemporaryBonusReturnsTheNewTotalForThatType() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 2, 1);

        assertEquals(5, sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1));
    }

    @Test
    void getTemporaryBonusOnlySumsMatchingModifierType() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);
        sheet.grantTemporaryBonus(ModifierType.DAMAGE_REDUCTION, 5, 1);

        assertEquals(3, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(5, sheet.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
    }

    @Test
    void newlyCreatedCharacterSheetHasNoLifeSteal() {
        CharacterSheet sheet = newSheet();

        assertEquals(0, sheet.getTotalLifeSteal());
    }

    @Test
    void getTotalLifeStealSumsEveryActiveLifeStealEffect() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new LifeSteal(2, Optional.of(3)));

        assertEquals(2, sheet.getTotalLifeSteal());
    }

    @Test
    void applyEffectStillLetsLifeStealStackLikeBleedingDoes() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new LifeSteal(2, Optional.of(1)));
        sheet.applyEffect(new LifeSteal(3, Optional.of(1)));

        assertEquals(5, sheet.getTotalLifeSteal());
    }

    @Test
    void getTotalLifeStealNeverExpiresAnOpenEndedLifeSteal() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new LifeSteal(2, Optional.empty()));

        for (int i = 0; i < 10; i++) {
            sheet.tickTemporaryEffects();
        }

        assertEquals(2, sheet.getTotalLifeSteal());
    }

    @Test
    void tickTemporaryEffectsRemovesAFiniteLifeStealOnceItsRoundsRunOut() {
        CharacterSheet sheet = newSheet();
        sheet.applyEffect(new LifeSteal(2, Optional.of(2)));

        sheet.tickTemporaryEffects();
        assertEquals(2, sheet.getTotalLifeSteal());
        sheet.tickTemporaryEffects();
        assertEquals(0, sheet.getTotalLifeSteal());
    }

    @Test
    void tickTemporaryEffectsCountsDownABonusWithoutExpiringBeforeItsLastRound() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 2);

        sheet.tickTemporaryEffects();

        assertEquals(3, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void tickTemporaryEffectsRemovesABonusOnceItsRoundsRunOut() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 2);

        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void tickTemporaryEffectsOnlyExpiresBonusesWhoseRoundsAreUp() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 1, 1);
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 2, 3);

        sheet.tickTemporaryEffects();

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void tickTemporaryEffectsAdvancesABonusAndABleedingByExactlyOneRoundEach() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 2);
        sheet.applyEffect(new Bleeding(1, Optional.of(2)));

        sheet.tickTemporaryEffects();

        assertEquals(3, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(1, sheet.getDamageTaken());
        sheet.tickTemporaryEffects();
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(2, sheet.getDamageTaken());
    }

    @Test
    void applyEffectAcceptsAnyKindOfTemporaryEffect() {
        CharacterSheet sheet = newSheet();

        sheet.applyEffect(new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1));
        sheet.applyEffect(new Bleeding(1, Optional.of(1)));

        assertEquals(3, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        sheet.tickTemporaryEffects();
        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void removeEffectDropsExactlyThatInstance() {
        CharacterSheet sheet = newSheet();
        TemporaryBonus bonus = new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);
        sheet.applyEffect(bonus);

        sheet.removeEffect(bonus);

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void removeEffectLeavesOtherEffectsOfTheSameModifierTypeUntouched() {
        CharacterSheet sheet = newSheet();
        TemporaryBonus removed = new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);
        TemporaryBonus kept = new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 2, 1);
        sheet.applyEffect(removed);
        sheet.applyEffect(kept);

        sheet.removeEffect(removed);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void removeEffectIsANoOpWhenTheEffectIsNotCurrentlyHeld() {
        CharacterSheet sheet = newSheet();
        TemporaryBonus neverApplied = new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);

        sheet.removeEffect(neverApplied);

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void finishTurnAdvancesTemporaryEffectsByOneRodada() {
        CharacterSheet sheet = newSheet();
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);
        sheet.applyEffect(new Bleeding(1, Optional.of(1)));

        sheet.finishTurn();

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void consumeFirstRollThisTurnReturnsTrueTheFirstTimeForAGivenDomain() {
        CharacterSheet sheet = newSheet();

        assertTrue(sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));
    }

    @Test
    void consumeFirstRollThisTurnReturnsFalseOnASubsequentCallForTheSameDomain() {
        CharacterSheet sheet = newSheet();
        sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY);

        assertFalse(sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));
    }

    @Test
    void consumeFirstRollThisTurnTracksEachDomainIndependently() {
        CharacterSheet sheet = newSheet();
        sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY);

        assertTrue(sheet.consumeFirstRollThisTurn(AttributeDomain.STRENGTH));
    }

    @Test
    void startTurnResetsWhichDomainsHaveAlreadyRolled() {
        CharacterSheet sheet = newSheet();
        sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY);

        sheet.startTurn(1);

        assertTrue(sheet.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));
    }

    // Each of these must spend first: a recovery restores previously-spent points, so against a
    // full pool it is a no-op and the assertion would pass for the wrong reason.

    @Test
    void applyPendingEgoRecoveriesGrantsBackPointsOnceARestOfSufficientTierIsTaken() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        sheet.applyPendingEgoRecoveries(RestType.LONGO);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void applyPendingEgoRecoveriesDoesNothingWhenTheRestTierIsTooLow() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 2, RestType.LONGO));

        sheet.applyPendingEgoRecoveries(RestType.CURTO);

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** Owes back fewer points than were spent, so a re-resolution would be visible as a 2. */
    @Test
    void applyPendingEgoRecoveriesOnlyResolvesEachRecoveryOnce() {
        CharacterSheet sheet = newSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.owePendingEgoRecovery(new PendingEgoRecovery(EgoDomain.SORTE, 1, RestType.MINIMO));

        sheet.applyPendingEgoRecoveries(RestType.MINIMO);
        sheet.applyPendingEgoRecoveries(RestType.MINIMO);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void newlyCreatedCharacterSheetHasAnEmptyInventory() {
        CharacterSheet sheet = newSheet();

        assertTrue(sheet.getInventory().isEmpty());
    }

    @Test
    void addToInventoryAppendsAndRemoveFromInventoryRemovesOneOccurrence() {
        CharacterSheet sheet = newSheet();

        sheet.addToInventory(ArmorItem.COURACA);
        sheet.addToInventory(ArmorItem.COURACA);
        assertEquals(2, sheet.getInventory().size());

        assertTrue(sheet.removeFromInventory(ArmorItem.COURACA));
        assertEquals(List.of(ArmorItem.COURACA), sheet.getInventory());

        assertTrue(sheet.removeFromInventory(ArmorItem.COURACA));
        assertTrue(sheet.getInventory().isEmpty());
    }

    @Test
    void removeFromInventoryReportsFalseForAnItemThatWasNeverInInventory() {
        CharacterSheet sheet = newSheet();

        assertFalse(sheet.removeFromInventory(ArmorItem.COURACA));
    }
}
