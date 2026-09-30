package org.aventyrs.core.ego;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sorte's four roll effects (Ego plan Phase 6, {@code docs/rules/ego.txt}): what each costs, what it does to a
 * Perícia roll, and — for the two permanent ones — to an attack.
 */
class SorteSpendingTest {

    private final EgoPointsService egoPointsService = new EgoPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** Sorte 3, Força 4, Ataque Corpo-a-Corpo Graduação 3 — every other Ego at the fixture's 2. */
    private static CharacterSheet lucky() {
        CharacterSkill attack = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        attack.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .egos(CharacterEgos.builder().sorte(EgoValue.builder().base(3).build()).build())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, attack)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static InteractionResult attention(final CharacterSheet sheet, final SkillRoll roll) {
        return new AttentionInteraction().applyTo(sheet, null, roll);
    }

    // ---------- temporary ----------

    /** "Refazer uma rolagem … feitas em Vantagem": new dice, +2, one temporary point. */
    @Test
    void aRerollTakesNewDiceInVantagemForOneTemporaryPoint() {
        CharacterSheet sheet = lucky();
        SkillRoll first = new SkillRoll(List.of(1, 2, 2));

        SkillRoll rerolled = egoPointsService.rerollWithSorte(sheet, first, List.of(4, 4, 4));

        assertEquals(12, rerolled.getTotal());
        assertEquals(attention(sheet, first).getSkillRollBonus() + Skill.ADVANTAGE_BONUS,
                attention(sheet, rerolled).getSkillRollBonus());
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(3, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
    }

    /** "Reduzir o GD de uma rolagem efetuada contra um PdN" — one nível, one temporary point. */
    @Test
    void aDifficultyReductionEasesTheRollByOneNivel() {
        CharacterSheet sheet = lucky();
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        SkillRoll eased = egoPointsService.applySorte(sheet, roll, SorteEffect.DIFFICULTY_REDUCTION);

        assertEquals(attention(sheet, roll).getDifficultyReduction() + 1, attention(sheet, eased).getDifficultyReduction());
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void withNoTemporarySorteNothingIsSpentAndTheRerollIsRefused() {
        CharacterSheet sheet = lucky();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 3);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> egoPointsService.rerollWithSorte(sheet, new SkillRoll(List.of(1, 1, 2)), List.of(6, 6, 6)));

        assertEquals(TranslatableMessages.NOT_ENOUGH_EGO_POINTS, refused.getMessage());
        assertEquals(3, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
    }

    // ---------- permanent ----------

    /** "Escolher ser bem-sucedido … independente do resultado dos dados. É um Acerto Crítico Menor." */
    @Test
    void aChosenSuccessSucceedsAsACriticoMenorForOnePermanentPoint() {
        CharacterSheet sheet = lucky();
        SkillRoll hopeless = SkillRoll.against(List.of(1, 1, 2), DifficultyLevel.values()[DifficultyLevel.values().length - 1]);
        assertFalse(attention(sheet, hopeless).getSucceeded());

        InteractionResult forced = attention(sheet, egoPointsService.applySorte(sheet, hopeless, SorteEffect.FORCED_SUCCESS));

        assertTrue(forced.getSucceeded());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, forced.getCriticalResult());
        assertEquals(2, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
    }

    /** "Não é possível usar um Ponto Temporário para ativar um efeito Permanente." */
    @Test
    void temporaryPointsCannotBuyAPermanentEffect() {
        CharacterSheet sheet = lucky();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 3);
        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "gm", 2);

        assertThrows(IllegalOperationException.class,
                () -> egoPointsService.applySorte(sheet, new SkillRoll(List.of(1, 1, 2)), SorteEffect.FORCED_SUCCESS));
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    // ---------- on an attack ----------

    private static DeliveredAttack.DeliveredAttackBuilder attackOn(final MonsterSheet foe, final CharacterSheet attacker) {
        return DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO);
    }

    /** A chosen success hits whatever the dice, as a Crítico Menor. */
    @Test
    void aChosenSuccessHitsAMissedAttack() {
        CharacterSheet hero = lucky();
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        SkillRoll miss = new SkillRoll(List.of(2, 2, 1));

        DeliveredAttackResult result = new AttackDelivery().resolve(attackOn(capanga, hero)
                .attackRoll(egoPointsService.applySorte(hero, miss, SorteEffect.FORCED_SUCCESS))
                .build());

        assertTrue(result.getHit());
        assertTrue(result.getMargin() < 0);
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());
    }

    /**
     * "Desencadear suas Correntes de Efeitos e Efeitos Críticos Maiores em uma rolagem bem-sucedida": a bare hit
     * (margin 0) fires its Corrente and its Efeito Crítico; the roll itself is reported as rolled.
     */
    @Test
    void unleashingFiresTheCorrenteAndTheCriticalEffectOnABareHit() {
        CharacterSheet hero = lucky();
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        SkillRoll bareHit = new SkillRoll(List.of(2, 2, 2));
        DeliveredAttackResult plain = new AttackDelivery().resolve(attackOn(capanga, hero).attackRoll(bareHit).build());
        assertEquals(0, plain.getMargin());
        assertFalse(plain.getEffectChainTriggered());

        DeliveredAttackResult unleashed = new AttackDelivery().resolve(attackOn(capanga, hero)
                .attackRoll(egoPointsService.applySorte(hero, bareHit, SorteEffect.UNLEASHED_CRITICALS))
                .build());

        assertTrue(unleashed.getHit());
        assertTrue(unleashed.getEffectChainTriggered());
        assertTrue(unleashed.getCriticalEffectTriggered());
        assertEquals(CriticalResult.NONE, unleashed.getCriticalResult());
    }

    /** On a miss there is nothing to unleash — the point is still spent (it was the player's call). */
    @Test
    void unleashingAMissDoesNothing() {
        CharacterSheet hero = lucky();
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = new AttackDelivery().resolve(attackOn(capanga, hero)
                .attackRoll(egoPointsService.applySorte(hero, new SkillRoll(List.of(2, 2, 1)),
                        SorteEffect.UNLEASHED_CRITICALS))
                .build());

        assertFalse(result.getHit());
        assertFalse(result.getEffectChainTriggered());
        assertFalse(result.getCriticalEffectTriggered());
    }
}
