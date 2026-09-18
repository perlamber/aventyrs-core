package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.ability.ConcentracaoProfundaActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveAbilityServiceImplTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final ActiveAbility ability = new ConcentracaoProfundaActiveAbility();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private Character characterWithFocusBaseHoldingAbility(final int focusBase) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focusBase).build())
                        .build())
                .activeAbility(ability)
                .build();
    }

    @Test
    void activateGrantsTheAbilitysResolvedEffect() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, ability, 0);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void activateSpendsTheAbilitysMagicPointCost() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, ability, 0);

        assertEquals(3, sheet.getManaSpent());
    }

    @Test
    void activateRejectsAnAbilityTheCharacterDoesNotHold() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    @Test
    void activateRejectsWhenNotEnoughActionPointsThisTurn() {
        Character character = characterWithFocusBaseHoldingAbility(5).toBuilder()
                .actionPoints(0)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    @Test
    void activateRejectsWhenNotEnoughCurrentMagicPoints() {
        Character character = characterWithFocusBaseHoldingAbility(5);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int maxMagicPoints = new MagicPointsServiceImpl().getMaxMagicPoints(character);
        sheet.spendMagicPoints(maxMagicPoints - 2);

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));
    }

    // Each Tempo de Ativação is checked against its own counter, not against Pontos de Ação.
    // A Reação costs no PA, so a holder with zero PA can still take one — and a holder with zero
    // Reações cannot, however many PA they have. The two gates are genuinely independent.

    @Test
    void activateChecksAReacaoAgainstTheReactionCounterNotActionPoints() {
        ActiveAbility reaction = costing(ActionCost.REACTION);
        Character character = holding(reaction).toBuilder()
                .actionPoints(0)
                .reactions(1)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, reaction, 0);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void activateRejectsAReacaoFromAHolderEntitledToNone() {
        ActiveAbility reaction = costing(ActionCost.REACTION);
        Character character = holding(reaction).toBuilder().reactions(0).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, reaction, 0));
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void activateChecksAnAcaoLivreAgainstTheFreeActionCounterNotActionPoints() {
        ActiveAbility freeAction = costing(ActionCost.FREE_ACTION);
        Character character = holding(freeAction).toBuilder()
                .actionPoints(0)
                .freeActions(1)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, freeAction, 0);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void activateRejectsAnAcaoLivreFromAHolderEntitledToNone() {
        ActiveAbility freeAction = costing(ActionCost.FREE_ACTION);
        Character character = holding(freeAction).toBuilder().freeActions(0).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, freeAction, 0));
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    // A Variável cost is gated on its floor — the most the player could then choose to spend is
    // the caller's business, not this gate's.
    @Test
    void activateChecksADynamicCostAgainstItsMinimum() {
        ActiveAbility variable = costing(ActionCost.dynamic(2));
        Character affordable = holding(variable).toBuilder().actionPoints(2).build();
        CharacterSheet affordableSheet = CharacterSheet.of(affordable, new Player());

        activeAbilityService.activate(affordable, affordableSheet, variable, 0);

        assertEquals(2, affordableSheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));

        Character tooPoor = holding(variable).toBuilder().actionPoints(1).build();
        CharacterSheet tooPoorSheet = CharacterSheet.of(tooPoor, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(tooPoor, tooPoorSheet, variable, 0));
    }

    // ActionCost.NONE is a passive, with nothing to pay — so nothing to refuse it for either.
    @Test
    void activateChargesAPassiveNothingAtAll() {
        ActiveAbility passive = costing(ActionCost.NONE);
        Character character = holding(passive).toBuilder()
                .actionPoints(0)
                .reactions(0)
                .freeActions(0)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, passive, 0);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    /** A bare ActiveAbility that costs cost and grants a recognisable +2, for the gate tests above. */
    private ActiveAbility costing(final ActionCost cost) {
        return new ActiveAbility() {
            @Override
            public String getDescription() {
                return "Uma habilidade de teste.";
            }

            @Override
            public ActionCost getActionPointCost() {
                return cost;
            }

            @Override
            public int getMagicPointCost() {
                return 0;
            }

            @Override
            public int getDurationInRounds() {
                return 1;
            }

            @Override
            public TemporaryEffect resolveEffect(final Character character) {
                return new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 2, 1);
            }
        };
    }

    private Character holding(final ActiveAbility held) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .activeAbility(held)
                .build();
    }

    @Test
    void activateLeavesCharacterSheetUntouchedWhenRejected() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, ability, 0));

        assertEquals(0, sheet.getManaSpent());
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }
}
