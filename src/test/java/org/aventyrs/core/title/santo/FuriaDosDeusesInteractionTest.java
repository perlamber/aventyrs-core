package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.EmpoweredAttack;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fúria dos Deuses — three PV prices, each buying more for the one attack about to be made. */
class FuriaDosDeusesInteractionTest {

    private final FuriaDosDeusesInteraction interaction = new FuriaDosDeusesInteraction();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private CharacterSheet holder;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        holder = holderWithVigor(3);
    }

    private CharacterSheet holderWithVigor(final int vigorBase) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private InteractionResult pay(final CharacterSheet sheet, final FuriaDosDeusesInteraction.Tier tier) {
        return interaction.activate(TitleAbilityActivationRequest.builder()
                .activator(sheet)
                .choice(tier)
                .build());
    }

    @Test
    void oneHitPointBuysVantagemOnTheAttackRollAndNothingElse() {
        InteractionResult result = pay(holder, FuriaDosDeusesInteraction.Tier.VANTAGEM);

        EmpoweredAttack empowered = result.getEmpoweredAttack();
        assertEquals(Skill.ADVANTAGE_BONUS, empowered.attackRollBonus());
        assertEquals(0, empowered.difficultyReduction());
        assertEquals(0, empowered.extraDamageDice());
        assertEquals(1, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
    }

    @Test
    void threeHitPointsBuyAGdReductionInsteadOfVantagem() {
        InteractionResult result = pay(holder, FuriaDosDeusesInteraction.Tier.REDUCAO_DE_GD);

        EmpoweredAttack empowered = result.getEmpoweredAttack();
        assertEquals(0, empowered.attackRollBonus());
        assertEquals(1, empowered.difficultyReduction());
        assertEquals(0, empowered.extraDamageDice());
        assertEquals(3, result.getResourceLossValue());
    }

    /** "adicionalmente à redução de GD" — the top tier keeps the middle one's benefit. */
    @Test
    void fiveHitPointsKeepTheGdReductionAndAddOneD6PlusVigor() {
        InteractionResult result = pay(holder, FuriaDosDeusesInteraction.Tier.FURIA_MAIOR);

        EmpoweredAttack empowered = result.getEmpoweredAttack();
        assertEquals(1, empowered.difficultyReduction());
        assertEquals(FuriaDosDeusesInteraction.FURIA_MAIOR_DAMAGE_DICE, empowered.extraDamageDice());
        assertEquals(3, empowered.extraDamageFlat());
        assertEquals(5, result.getResourceLossValue());
    }

    @Test
    void theFlatDamageFollowsTheHoldersOwnVigor() {
        CharacterSheet burly = holderWithVigor(6);

        assertEquals(6, pay(burly, FuriaDosDeusesInteraction.Tier.FURIA_MAIOR)
                .getEmpoweredAttack().extraDamageFlat());
    }

    /** It rides on an attack the holder was making anyway, so it costs neither PD nor PA. */
    @Test
    void itSpendsNoDeterminationPoints() {
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(holder.getCharacter(), holder);

        InteractionResult result = pay(holder, FuriaDosDeusesInteraction.Tier.REDUCAO_DE_GD);

        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(pdBefore, determinationPointsService.getCurrentDeterminationPoints(holder.getCharacter(), holder));
    }

    @Test
    void anActivationNamingNoTierIsRefusedAndCostsNothing() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, refused.getMessage());
        assertEquals(0, holder.getDamageTaken());
    }

    /** The top tier is the one a nearly-dead Santo cannot afford, and the refusal costs nothing. */
    @Test
    void aTierThatWouldBeSelfFatalIsRefused() {
        int toLeaveFiveHitPoints = hitPointsService.getMaxHitPoints(holder.getCharacter()) - 5;
        holder.applyDamage(toLeaveFiveHitPoints);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> pay(holder, FuriaDosDeusesInteraction.Tier.FURIA_MAIOR));

        assertEquals(NOT_ENOUGH_HIT_POINTS, refused.getMessage());
        assertEquals(toLeaveFiveHitPoints, holder.getDamageTaken());
        // The cheaper tier is still payable at the same PV.
        assertNull(pay(holder, FuriaDosDeusesInteraction.Tier.VANTAGEM).getEmpoweredAttack()
                .additionalCriticalEffect());
    }
}
