package org.aventyrs.core.ego;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.AutocontroleEgoService;
import org.aventyrs.core.character.services.AutocontroleEgoService.Protection;
import org.aventyrs.core.character.services.AutocontroleEgoServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.effect.Definhar;
import org.aventyrs.core.effect.Sangramento;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Autocontrole's Ego spends (Ego plan Phase 8, {@code docs/rules/ego.txt}; table rulings 2026-09-30: the zeroed
 * Rodada is the current one, retroactively; the Cena immunity is to "that same kind").
 */
class AutocontroleSpendingTest {

    private final AutocontroleEgoService service = new AutocontroleEgoServiceImpl();

    private CharacterSheet sheet;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
        CharacterSkill esquiva = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        esquiva.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().autocontrole(EgoValue.builder().base(3).build()).build())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .skill(SkillType.ESQUIVA_E_APARAR, esquiva)
                .build();
        sheet = CharacterSheet.of(character, new Player());
    }

    // ---------- malefícios ----------

    /** "Reduzir à metade a duração de um malefício" — 3 Rodadas to 2 (rounded up). */
    @Test
    void aTemporaryPointHalvesARunningMaleficio() {
        Condition poisoned = new Condition(ConditionType.ENVENENADO, 3);
        sheet.applyCondition(poisoned);

        service.halveMaleficio(sheet, poisoned);

        assertEquals(2, poisoned.getRemainingRounds());
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void anEffectNotRunningIsRefusedWithNothingSpent() {
        assertThrows(IllegalOperationException.class,
                () -> service.halveMaleficio(sheet, new Condition(ConditionType.ENVENENADO, 3)));
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** "Remover completamente … tornando-se imune a ele ao longo da Cena": that kind is refused; another lands. */
    @Test
    void aPermanentPointRemovesAMaleficioAndRefusesItsKindForTheCena() {
        Condition poisoned = new Condition(ConditionType.ENVENENADO, 3);
        sheet.applyCondition(poisoned);

        service.removeMaleficio(sheet, poisoned);
        sheet.applyCondition(new Condition(ConditionType.ENVENENADO, 3));
        sheet.applyCondition(new Condition(ConditionType.CAIDO, 1));

        assertFalse(holds(ConditionType.ENVENENADO));
        assertTrue(holds(ConditionType.CAIDO));
        assertEquals(2, sheet.getPermanentEgoPoints(EgoDomain.AUTOCONTROLE));

        sheet.startNewScene();
        sheet.applyCondition(new Condition(ConditionType.ENVENENADO, 3));
        assertTrue(holds(ConditionType.ENVENENADO));
    }

    private boolean holds(final ConditionType type) {
        return sheet.getRunningEffects().stream()
                .anyMatch(effect -> effect instanceof Condition condition && condition.getType() == type);
    }

    // ---------- protection and recovery ----------

    /** "Receber RA por 1 Rodada; … RD, RM ou RE, à sua escolha" — one instance each. */
    @Test
    void aTemporaryPointBuysRaPlusTheChosenReduction() {
        service.grantProtection(sheet, Protection.RD, null);

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, sheet.getTemporaryBonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, sheet.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
    }

    @Test
    void theReChoiceNamesItsElement() {
        service.grantProtection(sheet, Protection.RE, ElementalType.FOGO);

        assertEquals(1, sheet.getElementalResistanceInstances(ElementalType.FOGO));
        assertEquals(0, sheet.getElementalResistanceInstances(ElementalType.TERRA));
        assertThrows(IllegalArgumentException.class, () -> service.grantProtection(sheet, Protection.RE, null));
    }

    /** "Recuperar PV, PM e PD como se passasse por um Descanso Longo." */
    @Test
    void aTemporaryPointRecoversAsALongRest() {
        sheet.applyDamage(4);   // still standing — a fallen character heals under their own limits
        int longRest = new RestServiceImpl().getRecoveredHitPoints(sheet.getCharacter(), RestType.LONGO);

        assertEquals(Math.min(4, longRest), service.recoverAsLongRest(sheet));
    }

    /** "Recuperar … como se passasse por um Descanso Total, e todos os PV." */
    @Test
    void aPermanentPointRecoversEveryPv() {
        sheet.applyDamage(12);

        assertEquals(12, service.recoverAsTotalRest(sheet));
        assertEquals(0, sheet.getDamageTaken());
    }

    /** "Reduzir a zero todo o dano sofrido em uma Rodada" — the current one: what was lost comes back, no more lands. */
    @Test
    void aPermanentPointZeroesThisRodadasDamage() {
        sheet.applyDamage(5);
        sheet.startNewRound();
        sheet.applyDamage(4);

        assertEquals(4, service.negateDamageThisRound(sheet));
        assertEquals(5, sheet.getDamageTaken());
        sheet.applyDamage(3);
        assertEquals(5, sheet.getDamageTaken());

        sheet.startNewRound();
        sheet.applyDamage(3);
        assertEquals(8, sheet.getDamageTaken());
    }

    // ---------- against an attack ----------

    private IncomingAttack.IncomingAttackBuilder attack(final SkillRoll defenceRoll) {
        return IncomingAttack.builder()
                .defender(sheet)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(defenceRoll);
    }

    /** "Evitar uma Corrente de Efeitos" — a margin that fires it, and it doesn't land. */
    @Test
    void avoidingTheCorrenteKeepsItFromLanding() {
        SkillRoll roll = service.applyDefence(sheet, new SkillRoll(List.of(2, 2, 2)), AutocontroleDefence.AVOID_CHAIN);

        IncomingAttackResult result = new AttackReceiver().resolve(attack(roll).effectChain(new Definhar()).build());

        assertFalse(result.getDefended());
        assertFalse(result.getEffectChainTriggered());
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** "Ignorar os Efeitos de um Acerto Crítico Menor que você tenha sofrido" — a Falha Crítica Menor defence. */
    @Test
    void ignoringAMinorCriticalKeepsItsEffectsOff() {
        SkillRoll fumble = new SkillRoll(List.of(1, 1, 2));
        assertTrue(new AttackReceiver().resolve(attack(fumble)
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR)).build()).getCriticalEffectTriggered());

        SkillRoll ignored = service.applyDefence(sheet, fumble, AutocontroleDefence.IGNORE_MINOR_CRITICAL);

        assertFalse(new AttackReceiver().resolve(attack(ignored)
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR)).build()).getCriticalEffectTriggered());
    }

    /** "Evitar … Efeito Crítico e tornar-se imune ao longo da Cena" — Sangramento is refused for the Cena. */
    @Test
    void avoidingACriticalWithImmunityRefusesItsTypeForTheCena() {
        SkillRoll avoided = service.applyDefence(sheet, new SkillRoll(List.of(1, 1, 2)),
                AutocontroleDefence.AVOID_CRITICAL_WITH_IMMUNITY);

        new AttackReceiver().resolve(attack(avoided)
                .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR)).build());

        assertTrue(sheet.isCenaImmune(CriticalEffectType.SANGRAMENTO));
        assertEquals(2, sheet.getPermanentEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void aTemporaryPointCannotBuyAPermanentDefence() {
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 3);
        sheet.receiveTemporaryEgoPoints(EgoDomain.AUTOCONTROLE, "gm", 2);

        assertThrows(IllegalOperationException.class, () -> service.applyDefence(sheet, new SkillRoll(List.of(1, 1, 2)),
                AutocontroleDefence.AVOID_CRITICAL_WITH_IMMUNITY));
    }
}
