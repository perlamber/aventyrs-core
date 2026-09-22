package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.combat.Retaliation;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Espinhos Venenos de Gaea — Vigor in PV buys thorns worth that same Vigor, for 2 Rodadas. */
class EspinhosVenenosDeGaeaInteractionTest {

    private final EspinhosVenenosDeGaeaInteraction interaction = new EspinhosVenenosDeGaeaInteraction();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

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

    private int thorns() {
        return holder.getTemporaryBonus(ModifierType.RETALIATION_DAMAGE);
    }

    @Test
    void itCostsVigorInPvAndArmsThornsWorthThatSameVigor() {
        InteractionResult result = interaction.applyTo(holder);

        assertEquals(3, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(3, holder.getDamageTaken());
        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(3, thorns());
    }

    @Test
    void theThornsLapseAfterTwoRodadas() {
        interaction.applyTo(holder);

        holder.finishTurn();
        assertEquals(3, thorns());
        holder.finishTurn();

        assertEquals(0, thorns());
    }

    @Test
    void anActivationThatWouldBeSelfFatalIsRefused() {
        int toLeaveThreeHitPoints = hitPointsService.getMaxHitPoints(holder.getCharacter()) - 3;
        holder.applyDamage(toLeaveThreeHitPoints);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(NOT_ENOUGH_HIT_POINTS, refused.getMessage());
        assertEquals(0, thorns());
    }

    // --- The Malefício Veneno half, which is a real stat change now -----------------------------

    /** "-1 Multiplicador de Pontos de Vida" — max PV really falls while the poison lasts. */
    @Test
    void envenenadoLowersItsHoldersLifeMultiplierAndSoTheirMaximumPv() {
        CharacterSheet poisoned = holderWithVigor(3);
        int multiplierBefore = hitPointsService.getLifeMultiplier(poisoned.getCharacter(), poisoned);
        int maxBefore = hitPointsService.getMaxHitPoints(poisoned.getCharacter(), poisoned);

        poisoned.applyCondition(new Condition(ConditionType.ENVENENADO, 2, null));

        assertEquals(multiplierBefore - 1, hitPointsService.getLifeMultiplier(poisoned.getCharacter(), poisoned));
        assertEquals(maxBefore - poisoned.getCharacter().getAttributes().getVigor().getTotal(),
                hitPointsService.getMaxHitPoints(poisoned.getCharacter(), poisoned));
    }

    /** The Character-only overload has no sheet to ask, so it cannot see a held Condição. */
    @Test
    void theSheetLessLifeMultiplierOverloadDoesNotSeeThePoison() {
        CharacterSheet poisoned = holderWithVigor(3);
        poisoned.applyCondition(new Condition(ConditionType.ENVENENADO, 2, null));

        assertEquals(poisoned.getCharacter().getLifeMultiplier(),
                hitPointsService.getLifeMultiplier(poisoned.getCharacter()));
    }

    // --- What an attacker actually sees, through the real orchestrator ------------------------

    private DeliveredAttackResult meleeAttackOn(final CharacterSheet defender) {
        return new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(holderWithVigor(1))
                .defender(defender)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());
    }

    @Test
    void aMeleeAttackerIsReportedTheThornsAndTheVenenoTheyRideWith() {
        interaction.applyTo(holder);

        Retaliation retaliation = meleeAttackOn(holder).getRetaliation();

        assertEquals(3, retaliation.damage());
        assertEquals(DamageType.FISICO_ELEMENTAL, retaliation.descriptor().damageType());
        assertEquals(ElementalType.NATURAL, retaliation.descriptor().elementalType());
        assertEquals(ConditionType.ENVENENADO, retaliation.conditionOnDamage());
        assertEquals(2, retaliation.conditionRounds());
    }

    /** "personagens que lhe atacarem Corpo-a-Corpo" — an Ataque à Distância provokes nothing. */
    @Test
    void aRangedAttackerIsReportedNothing() {
        interaction.applyTo(holder);

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(holderWithVigor(1))
                .defender(holder)
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());

        assertNull(result.getRetaliation());
    }

    @Test
    void anUnarmedDefenderReportsNoRetaliationAtAll() {
        assertNull(meleeAttackOn(holder).getRetaliation());
    }
}
