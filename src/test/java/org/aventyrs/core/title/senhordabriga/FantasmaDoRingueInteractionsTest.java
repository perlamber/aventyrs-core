package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.CriticalService;
import org.aventyrs.core.character.services.CriticalServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.ReactionOptionsService;
import org.aventyrs.core.character.services.ReactionOptionsServiceImpl;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleAttackModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cruz de Sangue and Fingir Fraquezas, with and without Malícia de Valentão. */
class FantasmaDoRingueInteractionsTest {

    private static final NaturalWeapon FIST = NaturalWeapon.ATAQUE_DESARMADO;

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final CriticalService criticalService = new CriticalServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final ReactionOptionsService reactionOptionsService = new ReactionOptionsServiceImpl();

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    private static SenhorDaBriga fantasma(final AventyrTitleAbility... abilities) {
        return new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), List.of(abilities));
    }

    private static ReactionContext.ReactionContextBuilder meleeAgainst(final CharacterSheet reactor,
                                                                       final CharacterSheet attacker,
                                                                       final Range distance) {
        return ReactionContext.builder()
                .trigger(ReactionTrigger.SELF_TARGETED_BY_MELEE_ATTACK)
                .reactor(reactor)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .reactorContext(SenhorDaBrigaFixtures.context(0, Map.of(attacker, distance)));
    }

    @Test
    void cruzDeSangueIsOfferedOnlyAsTheSoleTargetOfAMeleeAttackWithinReach() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.CRUZ_DE_SANGUE));
        CharacterSheet attacker = SenhorDaBrigaFixtures.combatant();

        assertEquals(1, reactionOptionsService.getAvailableReactions(
                meleeAgainst(holder, attacker, Range.ADJACENTE).build()).size());
        assertTrue(reactionOptionsService.getAvailableReactions(
                meleeAgainst(holder, attacker, Range.DISTANCIA_CURTA).build()).isEmpty());
        assertTrue(reactionOptionsService.getAvailableReactions(
                meleeAgainst(holder, attacker, Range.ADJACENTE).soleTarget(false).build()).isEmpty());
        assertTrue(reactionOptionsService.getAvailableReactions(
                meleeAgainst(holder, attacker, Range.ADJACENTE).attackSkill(SkillType.ATAQUE_A_DISTANCIA).build())
                .isEmpty());
    }

    @Test
    void cruzDeSangueTradesHalfTheDestrezaInDefesasForWiderMargins() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.CRUZ_DE_SANGUE);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        int defenseBefore = defenseService.getTotalDefense(holder, DefenseType.PHYSICAL);
        int attackMarginBefore = criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO, FIST, null);
        int defenseMarginBefore = criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null, null);

        title.activateAbility(FantasmaDoRingueAbility.CRUZ_DE_SANGUE,
                TitleAbilityActivationRequest.builder().activator(holder).build());

        int penalty = CruzDeSangueInteraction.resolveDefesasPenalty(holder);
        assertEquals(2, penalty);  // Destreza 4
        assertEquals(defenseBefore - penalty, defenseService.getTotalDefense(holder, DefenseType.PHYSICAL));
        assertEquals(attackMarginBefore + 2,
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO, FIST, null));
        assertEquals(defenseMarginBefore + 2,
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null, null));
        assertEquals(1, TitleAttackModifiers.resolve(holder, FIST, null, null).extraDamageDice());
    }

    @Test
    void maliciaDeValentaoAddsRdsTwoToCruzDeSangue() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.CRUZ_DE_SANGUE, FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        int rdBefore = damageService.getTotalDamageReduction(holder, DamageType.FISICO, null);

        title.activateAbility(FantasmaDoRingueAbility.CRUZ_DE_SANGUE,
                TitleAbilityActivationRequest.builder().activator(holder).build());

        assertEquals(rdBefore + 2, damageService.getTotalDamageReduction(holder, DamageType.FISICO, null));
    }

    @Test
    void fingirFraquezasCostsOnePlusTheEnemiesAtMuitoCurta() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet near = SenhorDaBrigaFixtures.combatant();
        CharacterSheet far = SenhorDaBrigaFixtures.combatant();
        SceneContext context = SenhorDaBrigaFixtures.context(0,
                Map.of(near, Range.DISTANCIA_MUITO_CURTA, far, Range.DISTANCIA_MEDIA));

        assertEquals(2, FingirFraquezasInteraction.resolveCost(context));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                        TitleAbilityActivationRequest.builder().activator(holder).sceneContext(context)
                                .determinationPoints(3).build()));
        assertEquals(INVALID_PD_AMOUNT, refused.getMessage());
    }

    @Test
    void fingirFraquezasIsRefusedWithAWeaponDrawn() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        SenhorDaBrigaFixtures.drawSword(holder);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                        TitleAbilityActivationRequest.builder().activator(holder).determinationPoints(1).build()));
        assertEquals(TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY, refused.getMessage());
    }

    @Test
    void fingirFraquezasRaisesDefesasAndBindsEveryEnemyAtMuitoCurtaUntilARest() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet near = SenhorDaBrigaFixtures.combatant();
        CharacterSheet far = SenhorDaBrigaFixtures.combatant();
        SceneContext context = SenhorDaBrigaFixtures.context(0,
                Map.of(near, Range.DISTANCIA_MUITO_CURTA, far, Range.DISTANCIA_MEDIA));
        int defenseBefore = defenseService.getTotalDefense(holder, DefenseType.PHYSICAL);

        title.activateAbility(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                TitleAbilityActivationRequest.builder().activator(holder).sceneContext(context)
                        .determinationPoints(2).build());

        assertEquals(defenseBefore + 2, defenseService.getTotalDefense(holder, DefenseType.PHYSICAL));
        assertTrue(near.getForcedTargeting().isPresent());
        assertEquals(holder, near.getForcedTargeting().get().getEnchanter());
        assertFalse(far.getForcedTargeting().isPresent());
        assertTrue(near.isAffectedUntilRest(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS));

        near.clearRestCooldowns(RestType.MINIMO);
        assertFalse(near.isAffectedUntilRest(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS));
    }

    @Test
    void maliciaDeValentaoGivesVantagemOnAttackAndDanoAgainstAFoeItBound() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS, FantasmaDoRingueAbility.CRUZ_DE_SANGUE,
                FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet bound = SenhorDaBrigaFixtures.combatant();
        CharacterSheet stranger = SenhorDaBrigaFixtures.combatant();
        SceneContext context = SenhorDaBrigaFixtures.context(0, Map.of(bound, Range.ADJACENTE));

        title.activateAbility(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                TitleAbilityActivationRequest.builder().activator(holder).sceneContext(context)
                        .determinationPoints(2).build());

        assertEquals(Skill.ADVANTAGE_BONUS,
                title.resolveAttackRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, FIST, holder, bound, null));
        assertEquals(Skill.ADVANTAGE_BONUS,
                title.resolveDamageRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, FIST, holder, bound, null));
        assertEquals(0, title.resolveAttackRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, FIST, holder, stranger, null));
    }
}
