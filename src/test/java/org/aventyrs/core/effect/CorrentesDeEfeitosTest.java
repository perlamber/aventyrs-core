package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.ForcedTargeting;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.Withering;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generic Correntes de Efeitos of {@code docs/rules/efeitos-criticos.txt} authored in core 0.0.85, each tested by
 * what it does to a sheet. Table rulings (2026-10-01): Roubo de Determinação moves the PD to the attacker; Ferida
 * Infecciosa's damage yields to any Descanso; Excomungar removes the caster's pick, else the most recent.
 */
class CorrentesDeEfeitosTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /** Força 3 and a trained Ataque Corpo-a-Corpo — a roll the Força Desvantagem reaches. */
    private static CharacterSheet fighter() {
        CharacterSkill attack = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, attack)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static InteractionResult attackRoll(final CharacterSheet roller, final CharacterSheet target) {
        return SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(roller, null, new SkillRoll(List.of(3, 3, 3)), target);
    }

    // ---------- Enrijecer Musculatura ----------

    /** "Perícias Físicas (baseadas em Força e Destreza) … em Desvantagem por 1 Rodada" — never Atenção. */
    @Test
    void enrijecerMusculaturaPutsPhysicalRollsInDesvantagemForOneRodada() {
        CharacterSheet target = fighter();
        CharacterSheet other = sheet();
        int attackBefore = attackRoll(target, other).getSkillRollBonus();
        int attentionBefore = SkillType.ATTENTION.newInteraction().applyTo(target).getSkillRollBonus();

        new EnrijecerMusculatura().applyTo(target);
        new EnrijecerMusculatura().applyTo(target);   // a second hit renews, never -4

        assertEquals(attackBefore + Skill.DISADVANTAGE_MALUS, attackRoll(target, other).getSkillRollBonus());
        assertEquals(attentionBefore, SkillType.ATTENTION.newInteraction().applyTo(target).getSkillRollBonus());
        assertEquals(Skill.DISADVANTAGE_MALUS, target.getTemporaryBonus(ModifierType.PHYSICAL_SKILL_ROLL_BONUS));

        target.tickTemporaryEffects();
        assertEquals(attackBefore, attackRoll(target, other).getSkillRollBonus());
    }

    /** It reaches the roll, never the Atributo: the melee "Metade da Força" and PV stay where they were. */
    @Test
    void enrijecerMusculaturaLeavesTheAtributoAlone() {
        CharacterSheet target = fighter();
        int strength = target.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, target);
        int maxPv = new HitPointsServiceImpl().getMaxHitPoints(target.getCharacter(), target);

        new EnrijecerMusculatura().applyTo(target);

        assertEquals(strength, target.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, target));
        assertEquals(maxPv, new HitPointsServiceImpl().getMaxHitPoints(target.getCharacter(), target));
    }

    // ---------- Escancarar Defesas ----------

    /** "Seu próximo ataque contra este mesmo alvo tem a GD reduzida em -1 Nível" — and only the next. */
    @Test
    void escancararDefesasEasesTheAttackersNextAttackOnThatTargetOnly() {
        CharacterSheet attacker = fighter();
        CharacterSheet target = sheet();
        CharacterSheet bystander = sheet();
        int plain = attackRoll(attacker, target).getDifficultyReduction();

        new EscancararDefesas(attacker).applyTo(target);

        assertEquals(plain + EscancararDefesas.DIFFICULTY_REDUCTION_LEVELS,
                attackRoll(attacker, target).getDifficultyReduction());
        assertEquals(plain, attackRoll(attacker, bystander).getDifficultyReduction());

        attacker.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, null,
                null, 1, null, target.getId(), null));
        assertEquals(plain, attackRoll(attacker, target).getDifficultyReduction());
    }

    @Test
    void escancararDefesasLapsesWithTheCena() {
        CharacterSheet attacker = fighter();
        CharacterSheet target = sheet();
        new EscancararDefesas(attacker).applyTo(target);

        attacker.startNewScene();

        assertFalse(attacker.hasOpenedDefensesOf(target));
    }

    // ---------- Excomungar ----------

    @Test
    void excomungarRemovesTheMostRecentMaldicaoOrEncantamento() {
        CharacterSheet target = sheet();
        CharacterSheet enchanter = sheet();
        target.applyEffect(new Withering(1, Optional.of(3)));
        target.applyEnchantment(new ForcedTargeting(enchanter, 2));
        target.applyCondition(new Condition(ConditionType.CAIDO, 1));

        new Excomungar().applyTo(target);

        assertTrue(target.getForcedTargeting().isEmpty(), "the Encantamento went first");
        assertTrue(target.getRunningEffects().stream().anyMatch(Withering.class::isInstance));
        assertTrue(target.hasCondition(ConditionType.CAIDO, null), "a Malefício that is neither stays");
    }

    @Test
    void excomungarRemovesTheOneTheCasterPicked() {
        CharacterSheet target = sheet();
        Withering curse = new Withering(1, Optional.of(3));
        target.applyEffect(curse);
        target.applyCondition(new Condition(ConditionType.AMALDICOADO, 3));

        new Excomungar(curse).applyTo(target);

        assertFalse(target.getRunningEffects().contains(curse));
        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(List.of(), Excomungar.removableFrom(sheet()));
    }

    // ---------- Explosão Cataclísmica ----------

    @Test
    void explosaoCataclismicaReportsTheExplosionAndAddsCataclismo() {
        InteractionResult result = new ExplosaoCataclismica().applyTo(sheet());

        assertEquals(AreaOfEffect.ATTACK_EXPLOSION, result.getTriggeredAreaOfEffect());
        assertEquals(CriticalEffectType.CATACLISMO, ExplosaoCataclismica.ADDITIONAL_CRITICAL_EFFECT);
    }

    // ---------- Ferida Infecciosa ----------

    /** "3 pontos de danos adicionais … só podem ser recuperados com Descansos" — any Descanso, nothing else. */
    @Test
    void feridaInfecciosasDamageHealsOnlyWithADescanso() {
        CharacterSheet target = sheet();
        target.applyDamage(4);

        InteractionResult result = new FeridaInfecciosa().applyTo(target);

        assertEquals(3, result.getResourceLossValue());
        assertEquals(7, target.getDamageTaken());
        assertEquals(3, target.getRestLockedDamage());

        target.heal(10);
        target.healFromLifeSteal(10);
        assertEquals(3, target.getDamageTaken(), "an ordinary heal and Roubo de Vida stop at the lock");

        target.heal(2, HealingSource.rest(RestType.MINIMO));
        assertEquals(1, target.getDamageTaken());
        assertEquals(1, target.getRestLockedDamage());
    }

    // ---------- Magicae Mortis ----------

    /** "Roubo de Vida igual à 1+ seu número de Títulos Arcanos ou Abençoados" — never more than the hit dealt. */
    @Test
    void magicaeMortisStealsUpToWhatTheHitDealt() {
        CharacterSheet caster = sheet();
        caster.applyDamage(5);
        CharacterSheet target = sheet();
        target.recordDamageReceived(new org.aventyrs.core.sheet.DamageReceipt(4, null, caster));

        MagicaeMortis corrente = new MagicaeMortis(caster);
        InteractionResult result = corrente.applyTo(target);

        assertEquals(1, corrente.lifeSteal(), "no Título held");
        assertEquals(1, result.getResourceGainValue());
        assertEquals(4, caster.getDamageTaken());
    }

    // ---------- Oprimir ----------

    /** Roubo de Determinação 1 (2 on a critical): the target loses it and the attacker recovers it. */
    @Test
    void oprimirMovesDeterminationToTheAttacker() {
        CharacterSheet attacker = sheet();
        attacker.spendDeterminationPoints(3);
        CharacterSheet target = sheet();
        int before = pd(target);

        new Oprimir(attacker, false).applyTo(target);
        new Oprimir(attacker, true).applyTo(target);

        assertEquals(before - 3, pd(target));
        assertEquals(0, attacker.getDeterminationSpent());
    }

    @Test
    void oprimirTakesNoMoreThanTheTargetHas() {
        CharacterSheet attacker = sheet();
        attacker.spendDeterminationPoints(3);
        CharacterSheet target = sheet();
        target.spendDeterminationPoints(pd(target) - 1);

        InteractionResult result = new Oprimir(attacker, true).applyTo(target);

        assertEquals(1, result.getResourceLossValue());
        assertEquals(0, pd(target));
        assertEquals(2, attacker.getDeterminationSpent());
    }

    private static int pd(final CharacterSheet sheet) {
        return new DeterminationPointsServiceImpl().getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    // ---------- Remover Aflição ----------

    @Test
    void removerAflicaoHealsTwo() {
        CharacterSheet target = sheet();
        target.applyDamage(5);

        InteractionResult result = new RemoverAflicao().applyTo(target);

        assertEquals(2, result.getResourceGainValue());
        assertEquals(3, target.getDamageTaken());
    }

    // ---------- Toque Sombrio ----------

    @Test
    void toqueSombrioAddsOneDamage() {
        CharacterSheet target = sheet();

        InteractionResult result = new ToqueSombrio().applyTo(target);

        assertEquals(1, result.getResourceLossValue());
        assertEquals(1, target.getDamageTaken());
    }

    // ---------- Veneno Vampírico ----------

    /**
     * -1 Multiplicador de PV (Envenenado), 2 at once and 2 per Rodada for 2 Rodadas, Roubo de Vida 1 on each hit.
     */
    @Test
    void venenoVampiricoPoisonsDrainsAndFeedsItsSource() {
        CharacterSheet attacker = sheet();
        attacker.applyDamage(5);
        CharacterSheet target = sheet();
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        int multiplier = hitPoints.getLifeMultiplier(target.getCharacter(), target);

        InteractionResult result = new VenenoVampirico(attacker).applyTo(target);

        assertEquals(2, result.getResourceLossValue());
        assertTrue(target.hasCondition(ConditionType.ENVENENADO, null));
        assertEquals(multiplier - 1, hitPoints.getLifeMultiplier(target.getCharacter(), target));
        assertEquals(4, attacker.getDamageTaken());

        target.tickTemporaryEffects();
        target.tickTemporaryEffects();
        target.tickTemporaryEffects();

        assertEquals(6, target.getDamageTaken(), "2 at once, then 2 for each of 2 Rodadas");
        assertEquals(2, attacker.getDamageTaken());
        assertFalse(target.hasCondition(ConditionType.ENVENENADO, null));
    }
}
