package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.Definhar;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.effect.Sangramento;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackReceiverTest {

    private final AttackReceiver attackReceiver = new AttackReceiver();
    private final EffectChainService effectChainService = new EffectChainServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** A defender with the given Esquiva e Aparar graduation and Destreza, wearing nothing. */
    private CharacterSheet defender(final int dexterityBase, final int graduation) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        skill.increaseGraduation(graduation);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(dexterityBase).build())
                        .build())
                .skill(SkillType.ESQUIVA_E_APARAR, skill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private IncomingAttack.IncomingAttackBuilder attackOn(final CharacterSheet defender) {
        return IncomingAttack.builder()
                .defender(defender)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL);
    }

    @Test
    void aDefenseTotalEqualToTheThresholdCountsAsDefended() {
        // 3 Destreza + 3 Graduação = 6 bonus; MEDIUM's baseValue is 18, so 12 on the dice ties.
        CharacterSheet defender = defender(3, 3);
        // Graduação 3 also unlocks FOCADO (+1), so the bonus is 7 and 11 on the dice ties.
        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(5, 3, 3)))
                .build());

        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(), result.getRequiredTotal());
        assertEquals(result.getRequiredTotal(), result.getDefenseTotal());
        assertTrue(result.getDefended());
    }

    @Test
    void oneShortOfTheThresholdIsAHit() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(4, 3, 3)))
                .build());

        assertEquals(result.getRequiredTotal() - 1, result.getDefenseTotal());
        assertFalse(result.getDefended());
        assertEquals(1, result.getMargin());
    }

    @Test
    void theAttackBonusRaisesTheThreshold() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .attackBonus(4)
                .defenseRoll(new SkillRoll(List.of(5, 3, 3)))
                .build());

        assertEquals(DifficultyLevel.MEDIUM.getBaseValue() + 4, result.getRequiredTotal());
        assertFalse(result.getDefended());
    }

    /**
     * Graduação 7 unlocks PRODIGIO's -1 Nível, which must make the attack's own GD easier —
     * MEDIUM becomes EASY, a threshold of 14 rather than 18.
     */
    @Test
    void theDefendersDifficultyReductionMakesTheAttacksGdEasier() {
        CharacterSheet defender = defender(3, 7);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .build());

        assertEquals(1, result.getDefenseResult().getDifficultyReduction());
        assertEquals(DifficultyLevel.EASY, result.getEffectiveDifficultyLevel());
        assertEquals(DifficultyLevel.EASY.getBaseValue(), result.getRequiredTotal());
    }

    @Test
    void theEquippedArmorsDefenseColumnRaisesTheDefenseTotal() {
        CharacterSheet bare = defender(3, 3);
        Character armored = bare.getCharacter().toBuilder()
                .equipment(List.of(ArmorItem.ARMADURA_DE_GLADIADOR))
                .build();
        CharacterSheet armoredSheet = CharacterSheet.of(armored, new Player());

        SkillRoll roll = new SkillRoll(List.of(4, 3, 3));
        int bareTotal = attackReceiver.resolve(attackOn(bare).defenseRoll(roll).build()).getDefenseTotal();
        int armoredTotal = attackReceiver.resolve(attackOn(armoredSheet).defenseRoll(roll).build()).getDefenseTotal();

        // ARMADURA_DE_GLADIADOR is Leve, so it costs no Destreza — its DF column is pure gain.
        assertEquals(bareTotal + ArmorItem.ARMADURA_DE_GLADIADOR.getPhysicalDefenseBonus(), armoredTotal);
    }

    @Test
    void dfAndDmAreResolvedAgainstDifferentPools() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(0).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .equipment(List.of(ArmorItem.ROBE_CERIMONIAL))
                .build();
        SkillRoll roll = new SkillRoll(List.of(4, 4, 4));

        int physical = attackReceiver.resolve(IncomingAttack.builder()
                .defender(CharacterSheet.of(character, new Player()))
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(roll)
                .build()).getDefenseTotal();
        int magic = attackReceiver.resolve(IncomingAttack.builder()
                .defender(CharacterSheet.of(character, new Player()))
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.MAGIC)
                .defenseRoll(roll)
                .build()).getDefenseTotal();

        // ROBE_CERIMONIAL is DF 1 / DM 3 — a robe is far better against magic than against steel.
        assertEquals(ArmorItem.ROBE_CERIMONIAL.getMagicDefenseBonus() - ArmorItem.ROBE_CERIMONIAL.getPhysicalDefenseBonus(),
                magic - physical);
    }

    @Test
    void aLandedAttackChainsADamageInteractionEvenWhenItTriggersNoEffect() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertFalse(result.getDefended());
        assertFalse(result.getCriticalEffectTriggered());
        assertInstanceOf(DamageInteraction.class, result.getDefenseResult().getNextInteraction());
    }

    @Test
    void aCriticalFailureOnTheDefenseRollFiresTheAttacksCriticalEffects() {
        CharacterSheet defender = defender(3, 3);
        Sangramento bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);

        // 1+1+2 is a Falha Crítica Menor, and 3+7 bonus falls far short of MEDIUM's 18.
        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .criticalEffect(bleed)
                .build());

        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());

        DamageInteraction head = assertInstanceOf(DamageInteraction.class,
                result.getDefenseResult().getNextInteraction());
        assertEquals(bleed, head.getNextInteraction());
    }

    @Test
    void everyCriticalEffectTheAttackCarriesIsChainedInOrder() {
        CharacterSheet defender = defender(3, 3);
        Sangramento first = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);
        Sangramento second = new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .criticalEffect(first)
                .criticalEffect(second)
                .build());

        DamageInteraction head = assertInstanceOf(DamageInteraction.class,
                result.getDefenseResult().getNextInteraction());
        assertEquals(first, head.getNextInteraction());
        assertEquals(second, first.getNextInteraction());
        assertNull(second.getNextInteraction());
    }

    @Test
    void anAttackClearingTheMarginFiresItsEffectChains() {
        CharacterSheet defender = defender(3, 3);
        Definhar definhar = new Definhar();

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(2, 2, 2)))
                .effectChain(definhar)
                .build());

        // 7 bonus + 6 dice = 13, against MEDIUM's 18 — a margin of 5, exactly the base margin.
        assertEquals(EffectChainService.BASE_REQUIRED_MARGIN, result.getMargin());
        assertTrue(result.getEffectChainTriggered());

        DamageInteraction head = assertInstanceOf(DamageInteraction.class,
                result.getDefenseResult().getNextInteraction());
        assertEquals(definhar, head.getNextInteraction());
    }

    /** The two conditions are independent — this attack clears the margin without critting. */
    @Test
    void anAttackCanFireAnEffectChainWithoutFiringACriticalEffect() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(2, 2, 2)))
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR))
                .effectChain(new Definhar())
                .build());

        assertTrue(result.getEffectChainTriggered());
        assertFalse(result.getCriticalEffectTriggered());
    }

    /** ...and this one crits while falling one short of the margin. */
    @Test
    void anAttackCanFireACriticalEffectWithoutFiringAnEffectChain() {
        CharacterSheet defender = defender(3, 3);

        // 7 bonus + 4 dice = 11 vs MEDIUM's 18 is a margin of 7, so lower the GD to EASY (14)
        // via a negative attackBonus, leaving a margin of 3 — under the base margin of 5.
        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .attackBonus(-4)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR))
                .effectChain(new Definhar())
                .build());

        assertEquals(3, result.getMargin());
        assertTrue(result.getCriticalEffectTriggered());
        assertFalse(result.getEffectChainTriggered());
    }

    @Test
    void resolutoRaisesTheMarginAnEffectChainMustClear() {
        CharacterSheet plain = defender(3, 3);
        Character resoluteCharacter = plain.getCharacter().toBuilder()
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO)
                .build();
        CharacterSheet resolute = CharacterSheet.of(resoluteCharacter, new Player());
        SkillRoll roll = new SkillRoll(List.of(2, 2, 2));

        // A margin of exactly 5: enough against a plain defender, not against RESOLUTO's 7.
        assertTrue(attackReceiver.resolve(attackOn(plain).defenseRoll(roll)
                .effectChain(new Definhar()).build()).getEffectChainTriggered());
        assertFalse(attackReceiver.resolve(attackOn(resolute).defenseRoll(roll)
                .effectChain(new Definhar()).build()).getEffectChainTriggered());
        assertEquals(EffectChainService.RESOLUTO_REQUIRED_MARGIN,
                effectChainService.getRequiredMargin(resoluteCharacter));
    }

    @Test
    void aDefendedAttackChainsNothing() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(6, 6, 6)))
                .build());

        assertTrue(result.getDefended());
        assertNull(result.getDefenseResult().getNextInteraction());
        assertFalse(result.getCriticalEffectTriggered());
        assertFalse(result.getEffectChainTriggered());
    }

    @Test
    void withNoDefenseRollTheOutcomeIsUndeterminedButTheThresholdIsStillReported() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .build());

        assertNull(result.getDefended());
        assertNull(result.getMargin());
        assertNull(result.getCriticalEffectTriggered());
        assertNull(result.getEffectChainTriggered());
        assertNull(result.getCriticalResult());
        assertNull(result.getDefenseResult().getNextInteraction());
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(), result.getRequiredTotal());
        // The bonuses alone: 3 Destreza + 3 Graduação + FOCADO's +1.
        assertEquals(7, result.getDefenseTotal());
    }

    @Test
    void resolveReportsTheDefenseRollsOwnCriticalResult() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 1)))
                .build());

        assertNotNull(result.getCriticalResult());
        assertEquals(result.getDefenseResult().getCriticalResult(), result.getCriticalResult());
    }

    /** Report-only: resolve computes the damage but must never apply any of it. */
    @Test
    void resolveNeverAppliesDamageToTheDefender() {
        CharacterSheet defender = defender(3, 3);

        IncomingAttackResult result = attackReceiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .build());

        assertFalse(result.getDefended());
        assertNotNull(result.getDefenseResult().getNextInteraction());
        assertEquals(0, defender.getDamageTaken());
        assertEquals(0, defender.getShieldPoints());
    }

    /**
     * The defense roll is the one thing resolve does that genuinely changes state (it can grant
     * a temporary Ego point on a critical success — the first-roll-of-Turn check it also runs is
     * non-mutating now), so it must happen exactly once per incoming attack; rolling twice would
     * double-grant that Ego point.
     */
    @Test
    void resolveRollsTheDefenseExactlyOnce() {
        CountingInteraction counting = new CountingInteraction();
        AttackReceiver receiver = new AttackReceiver(counting, effectChainService, new DamageServiceImpl());
        CharacterSheet defender = defender(3, 3);

        receiver.resolve(attackOn(defender)
                .defenseRoll(new SkillRoll(List.of(1, 1, 2)))
                .build());

        assertEquals(1, counting.calls);
    }

    /**
     * Counts on the <em>longest</em> {@code applyTo} overload — the one holding all the logic,
     * which every shorter form delegates down to (CLAUDE.md, "Cascading overloads"). Counting on
     * the four-argument form instead would miss every call {@link AttackReceiver#resolve} makes,
     * since it passes a {@code DamageDescriptor} and so reaches the five-argument form directly.
     */
    private static class CountingInteraction extends EsquivaEApararInteraction {
        private int calls;

        @Override
        public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext,
                                          final SkillRoll skillRoll, final DefenseType defenseType,
                                          final DamageDescriptor damageDescriptor) {
            calls++;
            return super.applyTo(target, sceneContext, skillRoll, defenseType, damageDescriptor);
        }
    }
}
