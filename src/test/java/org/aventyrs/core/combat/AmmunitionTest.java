package org.aventyrs.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Desestabilizar;
import org.aventyrs.core.effect.DisparoEsmagador;
import org.aventyrs.core.effect.Incendiar;
import org.aventyrs.core.effect.TrespassarLonginquo;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AmmunitionKind;
import org.aventyrs.core.item.BowItem;
import org.aventyrs.core.item.CrossbowItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ProjectileHead;
import org.aventyrs.core.item.ProjectileItem;
import org.aventyrs.core.item.RecoveredProjectiles;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Projéteis, firing, reloading and recovery — table ruling 2026-10-12. */
class AmmunitionTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final AmmunitionService ammunition = new AmmunitionServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** An archer: Destreza 4 + Graduação 3 in Ataque à Distância (7 bonus). */
    private static CharacterSheet archer() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        skill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .build())
                .skill(SkillType.ATAQUE_A_DISTANCIA, skill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static AbstractItem lot(final ProjectileItem projectile) {
        return (AbstractItem) projectile.forge();
    }

    private static AbstractWeapon copyOf(final org.aventyrs.core.item.ItemTemplate weapon) {
        return (AbstractWeapon) AbstractItem.fromTemplate(weapon);
    }

    private DeliveredAttackResult shoot(final CharacterSheet shooter, final MonsterSheet foe, final AbstractWeapon bow,
                                        final ProjectileItem projectile, final List<Integer> dice,
                                        final DiceRoller roller) {
        return attackDelivery.resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(shooter)
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(bow)
                .projectile(projectile)
                .attackRoll(new SkillRoll(dice))
                .diceRoller(roller)
                .build());
    }

    // --- catalog and stacks ------------------------------------------------------------------

    @Test
    void everyProjectileComesAsAFlechaAndAVirote() {
        assertEquals(ProjectileHead.values().length * 2, ProjectileItem.values().length);
        for (ProjectileHead head : ProjectileHead.values()) {
            assertEquals(2, java.util.Arrays.stream(ProjectileItem.values())
                    .filter(projectile -> projectile.getHead() == head).count());
        }
        assertEquals("Flecha Bodkin", ProjectileItem.FLECHA_BODKIN.getName());
        assertEquals(AmmunitionKind.BOLT, ProjectileItem.VIROTE_PORTA_FOGO.getAmmunitionKind());
        assertEquals(AmmunitionKind.ARROW, BowItem.ARCO_CURTO.getAmmunitionKind());
        assertEquals(AmmunitionKind.BOLT, CrossbowItem.BESTA_PESADA.getAmmunitionKind());
    }

    @Test
    void aForgedProjectileIsOneWholeLot() {
        assertEquals(30, lot(ProjectileItem.FLECHA_PONTA_SIMPLES).getQuantity());
        assertEquals(15, lot(ProjectileItem.FLECHA_BODKIN).getQuantity());
        assertEquals(10, lot(ProjectileItem.VIROTE_PORTA_FOGO).getQuantity());
        assertTrue(lot(ProjectileItem.FLECHA_BODKIN).isStackable());
        assertFalse(AbstractItem.fromTemplate(BowItem.ARCO_CURTO).isStackable());
    }

    @Test
    void aSecondLotOfTheSameProjectileMergesIntoTheFirstStack() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_BODKIN));
        hero.addToInventory(lot(ProjectileItem.FLECHA_BODKIN));
        hero.addToInventory(lot(ProjectileItem.VIROTE_BODKIN));

        assertEquals(2, hero.getInventory().size());
        assertEquals(30, hero.getInventory().get(0).getQuantity());
    }

    @Test
    void aProjectileCannotBeAMasterpiece() {
        assertThrows(IllegalArgumentException.class, () -> lot(ProjectileItem.FLECHA_BODKIN)
                .setMasterpiece(org.aventyrs.core.item.OffensiveMasterpiece.values()[0]));
    }

    // --- firing gates -------------------------------------------------------------------------

    @Test
    void aBowCannotShootWithoutAProjectile() {
        CharacterSheet hero = archer();
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        AbstractWeapon bow = copyOf(BowItem.ARCO_CURTO);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> shoot(hero, foe, bow, null, List.of(3, 3, 3), null));
        assertEquals("AMMUNITION_REQUIRED", refused.getMessage());

        IllegalOperationException notCarried = assertThrows(IllegalOperationException.class,
                () -> shoot(hero, foe, bow, ProjectileItem.FLECHA_BODKIN, List.of(3, 3, 3), null));
        assertEquals("AMMUNITION_REQUIRED", notCarried.getMessage());
    }

    @Test
    void aViroteDoesNotFitABow() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.VIROTE_BODKIN));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> shoot(hero, GenericMonster.CAPANGA.spawn(new Player()), copyOf(BowItem.ARCO_CURTO),
                        ProjectileItem.VIROTE_BODKIN, List.of(3, 3, 3), null));
        assertEquals("PROJECTILE_INCOMPATIBLE", refused.getMessage());
    }

    @Test
    void aShotReportsWhatItFiredHitOrMissAndConsumeSpendsIt() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_PONTA_SIMPLES));
        AbstractWeapon bow = copyOf(BowItem.ARCO_CURTO);

        DeliveredAttackResult miss = shoot(hero, GenericMonster.CAPANGA.spawn(new Player()), bow,
                ProjectileItem.FLECHA_PONTA_SIMPLES, List.of(1, 1, 2), null);
        assertFalse(miss.getHit());
        assertEquals(ProjectileItem.FLECHA_PONTA_SIMPLES, miss.getFiredProjectile());
        assertEquals(1, miss.getProjectilesToConsume());

        ammunition.consume(hero, bow, miss.getFiredProjectile(), miss.getProjectilesToConsume());
        assertEquals(29, hero.getInventory().get(0).getQuantity());
    }

    @Test
    void theLastArrowLeavesTheBagAndThenTheBowIsDry() {
        CharacterSheet hero = archer();
        AbstractItem one = lot(ProjectileItem.FLECHA_PONTA_DUPLA);
        one.takeUnits(9);
        hero.addToInventory(one);
        AbstractWeapon bow = copyOf(BowItem.ARCO_CURTO);

        assertEquals(List.of(new AmmunitionService.AmmunitionStack(ProjectileItem.FLECHA_PONTA_DUPLA, 1)),
                ammunition.firableFrom(hero, bow));
        ammunition.consume(hero, bow, ProjectileItem.FLECHA_PONTA_DUPLA, 1);

        assertTrue(hero.getInventory().isEmpty());
        assertTrue(ammunition.firableFrom(hero, bow).isEmpty());
        assertThrows(IllegalOperationException.class,
                () -> ammunition.requireFirable(hero, bow, ProjectileItem.FLECHA_PONTA_DUPLA, 1));
    }

    @Test
    void aShotFiringSeveralProjectilesNeedsThatManyCarried() {
        CharacterSheet hero = archer();
        AbstractItem one = lot(ProjectileItem.FLECHA_BODKIN);
        one.takeUnits(14);
        hero.addToInventory(one);
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> ammunition.requireFirable(hero, copyOf(BowItem.ARCO_CURTO), ProjectileItem.FLECHA_BODKIN, 2));
        assertEquals("NOT_ENOUGH_AMMUNITION", refused.getMessage());
    }

    @Test
    void aFoesStatBlockFiresWithoutABag() {
        // A foe keeps no inventory to draw from: only a character sheet is held to its ammunition.
        assertTrue(ammunition.availableFor(GenericMonster.CAPANGA.spawn(new Player()), BowItem.ARCO_CURTO).isEmpty());
    }

    // --- what the projectile adds to the shot ------------------------------------------------

    @Test
    void aBodkinWidensTheBowsMarginByOne() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_BODKIN));
        hero.addToInventory(lot(ProjectileItem.FLECHA_PONTA_SIMPLES));
        AbstractWeapon bow = copyOf(BowItem.ARCO_CURTO);   // Projétil (17)

        // 6+5+5 = 16: a critical only one número wider than the bow's 17.
        DeliveredAttackResult bodkin = shoot(hero, GenericMonster.CAPANGA.spawn(new Player()), bow,
                ProjectileItem.FLECHA_BODKIN, List.of(6, 5, 5), null);
        DeliveredAttackResult plain = shoot(hero, GenericMonster.CAPANGA.spawn(new Player()), bow,
                ProjectileItem.FLECHA_PONTA_SIMPLES, List.of(6, 5, 5), null);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, bodkin.getCriticalResult());
        assertFalse(plain.getCriticalResult().isCriticalSuccess());
    }

    @Test
    void theBowCritsWithTheProjectilesEfeitoCritico() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_FOLHA_CORTANTE));

        // 6+6+5 = 17, a Menor: Dilacerar Menor picks at random, so with no roller it is reported unapplied.
        DeliveredAttackResult result = shoot(hero, GenericMonster.CAPANGA.spawn(new Player()),
                copyOf(BowItem.ARCO_CURTO), ProjectileItem.FLECHA_FOLHA_CORTANTE, List.of(6, 6, 5), null);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
        assertEquals(List.of(CriticalEffectType.DILACERAR), result.getUnappliedCriticalEffects());
    }

    @Test
    void eachHeadBuildsItsOwnCorrente() {
        assertInstanceOf(DisparoEsmagador.class, ProjectileHead.ESFERICA.createEffectChain(null));
        assertInstanceOf(TrespassarLonginquo.class, ProjectileHead.FOLHA_CORTANTE.createEffectChain(null));
        assertInstanceOf(Desestabilizar.class, ProjectileHead.PONTA_DUPLA.createEffectChain(null));
        assertInstanceOf(Incendiar.class, ProjectileHead.PORTA_FOGO.createEffectChain(null));
        assertNull(ProjectileHead.BODKIN.createEffectChain(null));
        assertNull(ProjectileHead.PONTA_SIMPLES.createEffectChain(null));
    }

    @Test
    void disparoEsmagadorHitsTheTargetAndItsArmour() {
        CharacterSheet target = archer();
        AbstractItem armour = AbstractItem.fromTemplate(org.aventyrs.core.item.ArmorItem.values()[0]);
        target.getCharacter().equip(armour);

        InteractionResult result = new DisparoEsmagador().applyTo(target);

        assertEquals(1, result.getResourceLossValue());
        assertEquals(1, armour.getDamageTaken());
    }

    @Test
    void trespassarLonginquoReportsAShortPenetratingLine() {
        InteractionResult result = new TrespassarLonginquo().applyTo(archer());
        assertEquals(TrespassarLonginquo.AREA, result.getTriggeredAreaOfEffect());
        assertEquals(4, result.getTriggeredAreaOfEffect().unidadesDeDistancia());
    }

    @Test
    void desestabilizarSlowsByOneUdWithoutStacking() {
        CharacterSheet target = archer();
        new Desestabilizar().applyTo(target);
        new Desestabilizar().applyTo(target);
        assertEquals(-1, target.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void incendiarRetypesToFireAndThrowsItsDie() {
        CharacterSheet target = archer();
        InteractionResult rolled = new Incendiar(DiceRoller.fixed(List.of(4))).applyTo(target);
        assertEquals(Incendiar.RETYPED, rolled.getRetypedDamage());
        assertEquals(4, rolled.getResourceLossValue());

        InteractionResult reported = new Incendiar(null).applyTo(archer());
        assertEquals(1, reported.getExtraDamageDice());
    }

    // --- reloading ----------------------------------------------------------------------------

    @Test
    void aBestaFiresOnlyWhatIsLoaded() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.VIROTE_BODKIN));
        AbstractWeapon besta = copyOf(CrossbowItem.BESTA_PESADA);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> ammunition.requireFirable(hero, besta, ProjectileItem.VIROTE_BODKIN, 1));
        assertEquals("WEAPON_NOT_LOADED", refused.getMessage());

        ammunition.reload(hero, besta, ProjectileItem.VIROTE_BODKIN);
        assertEquals(List.of(ProjectileItem.VIROTE_BODKIN), besta.getLoadedProjectiles());
        assertEquals(14, hero.getInventory().get(0).getQuantity());

        ammunition.consume(hero, besta, ProjectileItem.VIROTE_BODKIN, 1);
        assertTrue(besta.getLoadedProjectiles().isEmpty());
        assertEquals(14, hero.getInventory().get(0).getQuantity());
    }

    @Test
    void reloadingIsFreeOutsideCombatAndPricedInIt() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.VIROTE_PONTA_SIMPLES));
        AbstractWeapon pesada = copyOf(CrossbowItem.BESTA_PESADA);
        AbstractWeapon repeticao = copyOf(CrossbowItem.BESTA_DE_REPETICAO);

        assertEquals(ActionCost.NONE, ammunition.reload(hero, pesada, ProjectileItem.VIROTE_PONTA_SIMPLES));

        hero.startCombat();
        // Destreza 4 meets the Repetição's Favor: "Tempo de Ação da recarga reduzido para 1PA".
        assertEquals(ActionCost.ofActionPoints(1), ammunition.getReloadCost(hero, repeticao));
        assertEquals(ActionCost.ofActionPoints(1), ammunition.reload(hero, repeticao,
                ProjectileItem.VIROTE_PONTA_SIMPLES));
        assertEquals(5, repeticao.getLoadedProjectiles().size());
        assertNull(ammunition.getReloadCost(hero, copyOf(BowItem.ARCO_CURTO)));
    }

    @Test
    void theRepeticaoWithoutItsFavorReloadsForTwoPa() {
        CharacterSheet clumsy = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>()).build(), new Player());
        clumsy.startCombat();
        assertEquals(ActionCost.ofActionPoints(2),
                ammunition.getReloadCost(clumsy, copyOf(CrossbowItem.BESTA_DE_REPETICAO)));
    }

    @Test
    void aFullMagazineCannotBeReloadedAndAPartialReloadTakesWhatIsLeft() {
        CharacterSheet hero = archer();
        AbstractItem three = lot(ProjectileItem.VIROTE_PORTA_FOGO);
        three.takeUnits(7);
        hero.addToInventory(three);
        AbstractWeapon repeticao = copyOf(CrossbowItem.BESTA_DE_REPETICAO);

        ammunition.reload(hero, repeticao, ProjectileItem.VIROTE_PORTA_FOGO);
        assertEquals(3, repeticao.getLoadedProjectiles().size());
        assertTrue(hero.getInventory().isEmpty());

        AbstractWeapon mao = copyOf(CrossbowItem.BESTA_DE_MAO);
        hero.addToInventory(lot(ProjectileItem.VIROTE_BODKIN));
        ammunition.reload(hero, mao, ProjectileItem.VIROTE_BODKIN);
        IllegalOperationException full = assertThrows(IllegalOperationException.class,
                () -> ammunition.reload(hero, mao, ProjectileItem.VIROTE_BODKIN));
        assertEquals("RELOAD_NOT_POSSIBLE", full.getMessage());
    }

    // --- recovery -----------------------------------------------------------------------------

    @Test
    void halfOfWhatWasFiredComesBackAtCombatEndButNoBodkin() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_PONTA_SIMPLES));
        hero.addToInventory(lot(ProjectileItem.FLECHA_BODKIN));
        AbstractWeapon bow = copyOf(BowItem.ARCO_CURTO);
        hero.startCombat();

        ammunition.consume(hero, bow, ProjectileItem.FLECHA_PONTA_SIMPLES, 5);
        ammunition.consume(hero, bow, ProjectileItem.FLECHA_BODKIN, 4);

        List<RecoveredProjectiles> recovered = hero.endCombat();

        assertEquals(List.of(new RecoveredProjectiles(ProjectileItem.FLECHA_PONTA_SIMPLES, 2)), recovered);
        Item simples = hero.getInventory().stream()
                .filter(item -> ProjectileItem.of(item) == ProjectileItem.FLECHA_PONTA_SIMPLES).findFirst().orElseThrow();
        assertEquals(27, simples.getQuantity());
        assertTrue(hero.getFiredProjectilesThisCombat().isEmpty());
    }

    @Test
    void shotsOutsideCombatAreNotRecovered() {
        CharacterSheet hero = archer();
        hero.addToInventory(lot(ProjectileItem.FLECHA_PONTA_SIMPLES));
        ammunition.consume(hero, copyOf(BowItem.ARCO_CURTO), ProjectileItem.FLECHA_PONTA_SIMPLES, 4);
        assertTrue(hero.getFiredProjectilesThisCombat().isEmpty());
    }
}
