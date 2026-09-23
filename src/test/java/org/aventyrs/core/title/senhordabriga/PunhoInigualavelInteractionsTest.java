package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.AgarrarEDerrubar;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleAttackModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_OUT_OF_RANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Finalização and the Punho Inigualável activations, down to the attack they shape. */
class PunhoInigualavelInteractionsTest {

    private static final NaturalWeapon FIST = NaturalWeapon.ATAQUE_DESARMADO;

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    private static SenhorDaBriga punho(final AventyrTitleAbility... abilities) {
        return new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), List.of(abilities));
    }

    private static InteractionResult activate(final SenhorDaBriga title, final AventyrTitleAbility ability,
                                              final CombatantSheet activator) {
        return title.activateAbility(ability, TitleAbilityActivationRequest.builder().activator(activator).build());
    }

    private DeliveredAttackResult attack(final CombatantSheet attacker, final MonsterSheet foe) {
        return attackDelivery.resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(FIST)
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());
    }

    @Test
    void finalizacaoOpensAOneRodadaWindow() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.FINALIZACAO));
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);

        assertFalse(title.isFinalizacaoActive(holder));
        InteractionResult result = activate(title, SenhorDaBrigaAbility.FINALIZACAO, holder);

        assertEquals(1, result.getDeterminationPointsSpent());
        assertTrue(title.isFinalizacaoActive(holder));
        holder.finishTurn();
        assertFalse(title.isFinalizacaoActive(holder));
    }

    @Test
    void impactoElementalIsRefusedUntilAnElementIsChosen() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.IMPACTO_ELEMENTAL);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        int pdBefore = holder.getDeterminationSpent();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> activate(title, PunhoInigualavelAbility.IMPACTO_ELEMENTAL, holder));

        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, refused.getMessage());
        assertEquals(pdBefore, holder.getDeterminationSpent());
    }

    @Test
    void impactoElementalRetypesAndEmpowersNaturalAttacksWhileChargesRemain() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.IMPACTO_ELEMENTAL);
        title.chooseImpactoElementalElement(ElementalType.FOGO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        int unempowered = attack(holder, foe).getAttackTotal();

        activate(title, PunhoInigualavelAbility.IMPACTO_ELEMENTAL, holder);

        assertEquals(2, holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL));
        assertEquals(unempowered + Skill.ADVANTAGE_BONUS, attack(holder, foe).getAttackTotal());
        TitleAttackModifiers modifiers = TitleAttackModifiers.resolve(holder, FIST, foe, null);
        assertEquals(new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.FOGO), modifiers.damageDescriptor());
        assertNull(modifiers.defenseType());

        TitleAttackModifiers.consumeCharges(holder, SenhorDaBrigaFixtures.sword());
        assertEquals(2, holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL));
        TitleAttackModifiers.consumeCharges(holder, FIST);
        TitleAttackModifiers.consumeCharges(holder, FIST);
        assertEquals(unempowered, attack(holder, foe).getAttackTotal());
        assertTrue(TitleAttackModifiers.resolve(holder, FIST, foe, null).isNone());
    }

    @Test
    void grandeMestreRollsImpactoAgainstTheDmAsMagicalDamageWithCataclismo() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.IMPACTO_ELEMENTAL,
                PunhoInigualavelAbility.AGARRAR_E_DERRUBAR, PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS);
        title.chooseImpactoElementalElement(ElementalType.GELO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);

        activate(title, PunhoInigualavelAbility.IMPACTO_ELEMENTAL, holder);
        TitleAttackModifiers modifiers = TitleAttackModifiers.resolve(holder, FIST, null, null);

        assertEquals(4, holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL));
        assertEquals(DefenseType.MAGIC, modifiers.defenseType());
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO), modifiers.damageDescriptor());
        assertEquals(CriticalEffectType.CATACLISMO, modifiers.criticalEffectOverride());
    }

    @Test
    void impactoElementalEndsWithTheCombat() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.IMPACTO_ELEMENTAL);
        title.chooseImpactoElementalElement(ElementalType.TERRA);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        Scene scene = new Scene();
        scene.addParticipant(holder, 10);
        scene.startCombat();

        activate(title, PunhoInigualavelAbility.IMPACTO_ELEMENTAL, holder);
        scene.endCombat();

        assertEquals(0, holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL));
    }

    @Test
    void agarrarEDerrubarGivesTheNextNaturalAttackVantagemAndItsCorrente() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        int unempowered = attack(holder, foe).getAttackTotal();

        InteractionResult result = activate(title, PunhoInigualavelAbility.AGARRAR_E_DERRUBAR, holder);

        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(unempowered + Skill.ADVANTAGE_BONUS, attack(holder, foe).getAttackTotal());
        TitleAttackModifiers modifiers = TitleAttackModifiers.resolve(holder, FIST, foe, null);
        assertEquals(1, modifiers.effectChains().size());
        assertInstanceOf(AgarrarEDerrubar.class, modifiers.effectChains().get(0));
        assertTrue(TitleAttackModifiers.resolve(holder, SenhorDaBrigaFixtures.sword(), foe, null).isNone());

        TitleAttackModifiers.consumeCharges(holder, FIST);
        assertEquals(0, holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR));
    }

    @Test
    void theCorrenteKnocksDownATargetUpToTwoCategoriasLarger() {
        CharacterSheet attacker = SenhorDaBrigaFixtures.combatant();
        CharacterSheet sameSize = SenhorDaBrigaFixtures.combatant();

        new AgarrarEDerrubar(attacker).applyTo(sameSize);

        assertTrue(sameSize.hasCondition(ConditionType.CAIDO, null));
    }

    @Test
    void theCorrenteSparesATargetThreeCategoriasLarger() {
        CharacterSheet attacker = SenhorDaBrigaFixtures.combatant();
        CharacterSheet giant = SenhorDaBrigaFixtures.combatant();
        CharacterSizeService sizes = new CharacterSizeService() {
            @Override
            public SizeCategory getEffectiveSizeCategory(final org.aventyrs.core.character.Character character) {
                return SizeCategory.ZERO;
            }

            @Override
            public SizeCategory getEffectiveSizeCategory(final CombatantSheet sheet) {
                return sheet == giant ? SizeCategory.PLUS_THREE : SizeCategory.ZERO;
            }
        };

        new AgarrarEDerrubar(attacker, sizes).applyTo(giant);

        assertFalse(giant.hasCondition(ConditionType.CAIDO, null));
    }

    @Test
    void grandeMestreMayStrikeAFallenTargetAsAnAcaoLivre() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR,
                PunhoInigualavelAbility.ROLAMENTO_OFENSIVO, PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS);
        CharacterSheet attacker = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet target = SenhorDaBrigaFixtures.combatant();

        assertFalse(title.grantsFreeAttackAgainstFallen(target, null));
        new AgarrarEDerrubar(attacker).applyTo(target);
        assertTrue(title.grantsFreeAttackAgainstFallen(target, null));
    }

    @Test
    void rolamentoOfensivoReportsTwoUdTowardAnEnemyAtCurta() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet enemy = SenhorDaBrigaFixtures.combatant();

        InteractionResult result = title.activateAbility(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO,
                TitleAbilityActivationRequest.builder().activator(holder).target(enemy)
                        .sceneContext(SenhorDaBrigaFixtures.context(0, Map.of(enemy, Range.DISTANCIA_CURTA)))
                        .build());

        assertEquals(RolamentoOfensivoInteraction.DISTANCE, result.getMovementTowardTarget());
        assertFalse(holder.hasActivationWindow(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO));
    }

    @Test
    void rolamentoOfensivoRefusesAnEnemyBeyondCurta() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet enemy = SenhorDaBrigaFixtures.combatant();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO,
                        TitleAbilityActivationRequest.builder().activator(holder).target(enemy)
                                .sceneContext(SenhorDaBrigaFixtures.context(0, Map.of(enemy, Range.DISTANCIA_MEDIA)))
                                .build()));

        assertEquals(TITLE_ABILITY_TARGET_OUT_OF_RANGE, refused.getMessage());
    }

    @Test
    void grandeMestreRollsThreeUdAndGrantsARodadaOfVantagem() {
        SenhorDaBriga title = punho(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO,
                PunhoInigualavelAbility.AGARRAR_E_DERRUBAR, PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        int unempowered = attack(holder, foe).getAttackTotal();

        InteractionResult result = title.activateAbility(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO,
                TitleAbilityActivationRequest.builder().activator(holder).target(foe)
                        .sceneContext(SenhorDaBrigaFixtures.context(0, Map.of(foe, Range.ADJACENTE)))
                        .build());

        assertEquals(RolamentoOfensivoInteraction.GRANDE_MESTRE_DISTANCE, result.getMovementTowardTarget());
        assertEquals(unempowered + Skill.ADVANTAGE_BONUS, attack(holder, foe).getAttackTotal());
    }
}
