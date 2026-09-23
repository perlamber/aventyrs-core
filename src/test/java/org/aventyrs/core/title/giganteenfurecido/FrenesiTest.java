package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.combatant;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.enterFrenzy;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.frenesi;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.gigante;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.holder;
import static org.aventyrs.core.util.TranslatableMessages.FRENZY_ALREADY_ACTIVE;
import static org.aventyrs.core.util.TranslatableMessages.FRENZY_CANNOT_END_VOLUNTARILY;
import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Despertar's Frenesi, its Especializações as modes, and the Título-level Habilidades acting on it. */
class FrenesiTest {

    private static final GiganteEnfurecidoSpecialization TITA = GiganteEnfurecidoSpecialization.TITA_ENLOUQUECIDO;
    private static final GiganteEnfurecidoSpecialization BERSERKER = GiganteEnfurecidoSpecialization.BERSERKER;

    @BeforeEach
    void setup() {
        GiganteEnfurecidoFixtures.loadTemplates();
    }

    private static int autocontrole(final CharacterSheet sheet) {
        return sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);
    }

    @Test
    void frenesiCostsOneAutocontroleAndGrantsForcaAndIniciativaForTwoPlusHalfVigorRounds() {
        CharacterSheet sheet = holder(gigante(List.of()));
        int before = autocontrole(sheet);

        enterFrenzy(sheet);

        Frenzy frenzy = sheet.getOwnFrenzy().orElseThrow();
        assertEquals(before - 1, autocontrole(sheet));
        assertEquals(2 + GiganteEnfurecidoFixtures.VIGOR / 2, frenzy.getRemainingRounds());
        assertEquals(GiganteEnfurecidoFixtures.STRENGTH + 2,
                sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet));
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.INITIATIVE));
    }

    @Test
    void asTituloPrimarioTheFrenesiLastsTwoRoundsLonger() {
        CharacterSheet sheet = holder(gigante(List.of()), TitleSlot.PRIMARY);

        enterFrenzy(sheet);

        assertEquals(2 + GiganteEnfurecidoFixtures.VIGOR / 2 + 2, sheet.getOwnFrenzy().orElseThrow().getRemainingRounds());
    }

    @Test
    void frenesiEndsAfterItsRoundsOfTurnStarts() {
        CharacterSheet sheet = holder(gigante(List.of()));
        enterFrenzy(sheet);
        int rounds = sheet.getOwnFrenzy().orElseThrow().getRemainingRounds();

        for (int turn = 1; turn < rounds; turn++) {
            sheet.startTurn(turn);
        }
        assertTrue(sheet.getOwnFrenzy().isPresent());
        sheet.startTurn(rounds);
        assertTrue(sheet.getOwnFrenzy().isEmpty());
    }

    @Test
    void frenesiBlocksConcentrationPericiasAndSpellcastingButNotAttacks() {
        CharacterSheet sheet = holder(gigante(List.of()));
        enterFrenzy(sheet);

        assertTrue(sheet.isSpellCastingPrevented(null));
        assertTrue(sheet.isSkillUsePrevented(SkillType.CONHECIMENTOS, AttributeDomain.GNOSE));
        assertTrue(sheet.isSkillUsePrevented(SkillType.DOMINIO_DO_MANA, AttributeDomain.FOCUS));
        assertFalse(sheet.isSkillUsePrevented(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH));
        assertEquals(0, sheet.getConcentrationActionPointSurcharge());
    }

    @Test
    void aSecondFrenesiWhileOneRunsIsRefused() {
        CharacterSheet sheet = holder(gigante(List.of()));
        enterFrenzy(sheet);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> enterFrenzy(sheet));
        assertEquals(FRENZY_ALREADY_ACTIVE, refused.getMessage());
    }

    @Test
    void withoutAutocontroleTheFrenesiIsRefusedAndCostsNothing() {
        CharacterSheet sheet = combatant(0);
        sheet.getCharacter().grantTitle(gigante(List.of()), TitleSlot.SECONDARY);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> enterFrenzy(sheet));
        assertEquals(NOT_ENOUGH_EGO_POINTS, refused.getMessage());
        assertTrue(sheet.getOwnFrenzy().isEmpty());
    }

    @Test
    void aModeWhoseEspecializacaoIsNotHeldIsRefused() {
        CharacterSheet sheet = holder(gigante(List.of(BERSERKER)));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO));
        assertEquals(TITLE_ABILITY_PREREQUISITE_NOT_MET, refused.getMessage());
    }

    @Test
    void titaEnlouquecidoCostsOneMoreAutocontroleGrowsTheGiganteAndLiftsTheBlock() {
        CharacterSheet sheet = holder(gigante(List.of(TITA)));
        int before = autocontrole(sheet);
        var sizeService = new CharacterSizeServiceImpl();
        var hitPoints = new HitPointsServiceImpl();
        int baseSize = sizeService.getEffectiveSizeCategory(sheet).ordinal();
        int baseMultiplier = hitPoints.getLifeMultiplier(sheet.getCharacter(), sheet);

        enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);

        assertEquals(before - 2, autocontrole(sheet));
        assertEquals(baseSize + 1, sizeService.getEffectiveSizeCategory(sheet).ordinal());
        assertEquals(baseMultiplier + 1, hitPoints.getLifeMultiplier(sheet.getCharacter(), sheet));
        assertFalse(sheet.isSpellCastingPrevented(null));
        assertFalse(sheet.isSkillUsePrevented(SkillType.CONHECIMENTOS, AttributeDomain.GNOSE));
        assertEquals(1, sheet.getConcentrationActionPointSurcharge());
    }

    @Test
    void colossoEnfurecidoAddsAnotherSizeAndTheManaAndDeterminationMultipliersWhileTitaIsActive() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO));
        var sizeService = new CharacterSizeServiceImpl();
        int baseSize = sizeService.getEffectiveSizeCategory(sheet).ordinal();
        int baseMana = new MagicPointsServiceImpl().getManaMultiplier(sheet.getCharacter(), sheet);
        int baseDetermination = new DeterminationPointsServiceImpl().getDeterminationMultiplier(sheet.getCharacter(), sheet);

        enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);

        assertEquals(baseSize + 2, sizeService.getEffectiveSizeCategory(sheet).ordinal());
        assertEquals(baseMana + 1, new MagicPointsServiceImpl().getManaMultiplier(sheet.getCharacter(), sheet));
        assertEquals(baseDetermination + 1,
                new DeterminationPointsServiceImpl().getDeterminationMultiplier(sheet.getCharacter(), sheet));
    }

    @Test
    void colossoIsInertInAFrenesiWithoutTita() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO));
        int baseSize = new CharacterSizeServiceImpl().getEffectiveSizeCategory(sheet).ordinal();

        enterFrenzy(sheet);

        assertEquals(baseSize, new CharacterSizeServiceImpl().getEffectiveSizeCategory(sheet).ordinal());
    }

    @Test
    void berserkerAddsForcaAndMarginAndStacksOncePerAttackerWhenHit() {
        CharacterSheet sheet = holder(gigante(List.of(BERSERKER)));
        GiganteEnfurecido title = enterFrenzy(sheet, FrenzyMode.BERSERKER);
        CharacterSheet ogre = combatant(0);
        CharacterSheet goblin = combatant(0);

        assertEquals(GiganteEnfurecidoFixtures.STRENGTH + 3,
                sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet));
        assertEquals(1, sheet.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));

        assertTrue(title.recordHitTaken(sheet, ogre));
        assertFalse(title.recordHitTaken(sheet, ogre));
        assertTrue(title.recordHitTaken(sheet, goblin));

        assertEquals(-4, sheet.getTemporaryBonus(ModifierType.DEFESAS));
        assertEquals(GiganteEnfurecidoFixtures.STRENGTH + 5,
                sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet));
    }

    @Test
    void berserkerHitsDoNothingOutsideABerserkerFrenesi() {
        CharacterSheet sheet = holder(gigante(List.of(BERSERKER)));
        GiganteEnfurecido title = enterFrenzy(sheet);

        assertFalse(title.recordHitTaken(sheet, combatant(0)));
    }

    @Test
    void atZeroAutocontroleTheGiganteIsCompelledToAttackTheNearest() {
        CharacterSheet sheet = combatant(1);
        sheet.getCharacter().grantTitle(gigante(List.of()), TitleSlot.SECONDARY);
        assertFalse(sheet.isCompelledToAttackNearest());

        enterFrenzy(sheet);

        assertEquals(0, autocontrole(sheet));
        assertTrue(sheet.isCompelledToAttackNearest());
        assertTrue(sheet.treatsEveryoneAsEnemy());
    }

    @Test
    void autocontroleSpentInFrenesiComesBackOneEveryTwoHoursOutsideIt() {
        CharacterSheet sheet = holder(gigante(List.of(TITA)));
        int before = autocontrole(sheet);
        enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);
        assertEquals(2, sheet.getOwedHourlyEgoRecovery(EgoDomain.AUTOCONTROLE));

        sheet.passHours(1);
        assertTrue(sheet.getOwnFrenzy().isEmpty());
        assertEquals(before - 2, autocontrole(sheet));

        sheet.passHours(1);
        assertEquals(before - 1, autocontrole(sheet));
        sheet.passHours(2);
        assertEquals(before, autocontrole(sheet));
        assertEquals(0, sheet.getOwedHourlyEgoRecovery(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void unoComAIraReturnsTheFirstTwoPointsAfterTwoRoundsWhileTheFrenesiRuns() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA));
        int before = autocontrole(sheet);
        enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);

        sheet.startNewRound();
        assertEquals(before - 2, autocontrole(sheet));
        sheet.startNewRound();
        assertEquals(before, autocontrole(sheet));
        assertEquals(0, sheet.getOwedHourlyEgoRecovery(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void unoComAIraReturnsNothingOnceTheFrenesiHasEnded() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA));
        int before = autocontrole(sheet);
        GiganteEnfurecido title = enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);

        title.endFrenzyVoluntarily(sheet);
        sheet.startNewRound();
        sheet.startNewRound();

        assertEquals(before - 2, autocontrole(sheet));
        assertEquals(2, sheet.getOwedHourlyEgoRecovery(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void withoutUnoComAIraTheFrenesiCannotBeEndedVoluntarily() {
        CharacterSheet sheet = holder(gigante(List.of()));
        GiganteEnfurecido title = enterFrenzy(sheet);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.endFrenzyVoluntarily(sheet));
        assertEquals(FRENZY_CANNOT_END_VOLUNTARILY, refused.getMessage());
        assertTrue(sheet.getOwnFrenzy().isPresent());
    }

    @Test
    void endingEarlyExhaustsUntilATrueShortRest() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA));
        GiganteEnfurecido title = enterFrenzy(sheet);
        var rest = new RestServiceImpl();

        title.endFrenzyVoluntarily(sheet);

        assertTrue(sheet.getOwnFrenzy().isEmpty());
        assertEquals(-2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(-2, sheet.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
        rest.applyRest(sheet.getCharacter(), sheet, RestType.LONGO, false);
        rest.applyRest(sheet.getCharacter(), sheet, RestType.MINIMO, true);
        sheet.passHours(8);
        assertEquals(-2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        rest.applyRest(sheet.getCharacter(), sheet, RestType.CURTO, true);
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void endingWithNoFrenesiRunningIsRefused() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA));
        GiganteEnfurecido title = GiganteEnfurecido.heldBy(sheet).orElseThrow();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.endFrenzyVoluntarily(sheet));
        assertEquals(FRENZY_REQUIRED, refused.getMessage());
    }

    @Test
    void prolongarDescontroleAddsTwoRoundsButOnlyDuringAFrenesi() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE));
        GiganteEnfurecido title = GiganteEnfurecido.heldBy(sheet).orElseThrow();
        TitleAbilityActivationRequest request = TitleAbilityActivationRequest.builder().activator(sheet).build();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE, request));
        assertEquals(FRENZY_REQUIRED, refused.getMessage());

        enterFrenzy(sheet);
        int rounds = sheet.getOwnFrenzy().orElseThrow().getRemainingRounds();
        title.activateAbility(GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE, request);
        assertEquals(rounds + 2, sheet.getOwnFrenzy().orElseThrow().getRemainingRounds());
    }

    @Test
    void frenesiEsmeraldaDeepensTheFrenesiOnce() {
        CharacterSheet sheet = holder(gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA, GiganteEnfurecidoAbility.FRENESI_ESMERALDA));
        GiganteEnfurecido title = enterFrenzy(sheet);
        TitleAbilityActivationRequest request = TitleAbilityActivationRequest.builder().activator(sheet).build();

        title.activateAbility(GiganteEnfurecidoAbility.FRENESI_ESMERALDA, request);

        assertEquals(GiganteEnfurecidoFixtures.STRENGTH + 4,
                sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet));
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.ATTENTION_ROLL_BONUS));
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
        assertEquals(-4, sheet.getTemporaryBonus(ModifierType.DEFESAS));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(GiganteEnfurecidoAbility.FRENESI_ESMERALDA, request));
        assertEquals(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED, refused.getMessage());
    }

    @Test
    void desprezarDanosGivesRaInFrenesiAndHalvesDamageAndHealingAtZeroHitPoints() {
        CharacterSheet sheet = holder(gigante(List.of(BERSERKER), BerserkerAbility.DESPREZAR_DANOS));
        enterFrenzy(sheet);
        var damage = new DamageServiceImpl();
        int max = new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);

        assertEquals(2, damage.getTotalAbsoluteDamageReduction(sheet, null));
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.HALF_DAMAGE));

        sheet.applyDamage(max + 4);
        assertTrue(sheet.isAtOrBelowZeroHitPoints());
        assertEquals(1, sheet.getTemporaryBonus(ModifierType.HALF_DAMAGE));
        int taken = sheet.getDamageTaken();
        sheet.heal(4);
        assertEquals(taken - 2, sheet.getDamageTaken());
        sheet.healFromLifeSteal(2);
        assertEquals(taken - 4, sheet.getDamageTaken());
    }
}
