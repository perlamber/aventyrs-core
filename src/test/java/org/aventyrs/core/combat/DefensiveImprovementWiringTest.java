package org.aventyrs.core.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.DefensiveEnhancements;
import org.aventyrs.core.item.DefensiveImprovement;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemImprovement;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.ProjectileItem;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.SpellCatalog;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every {@link DefensiveImprovement} clause reaching the attack, the damage and the Magia (core 0.1.6.11). */
class DefensiveImprovementWiringTest {

    private static final SkillRoll ROLL = new SkillRoll(List.of(5, 5, 5));

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final DamageServiceImpl damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    // ---- the Ataque/Danos/Conjuração columns reach every attack and Magia ----------------------------

    @Test
    void aWornHostsColumnsReachEveryAttackAndMagia() {
        Character wearer = characterWearing(armour(DefensiveImprovement.LADRA_DO_AETHER),
                armour(DefensiveImprovement.ENCANTADORA));

        assertEquals(1, DefensiveEnhancements.attackRollBonus(wearer));
        assertEquals(1, DefensiveEnhancements.damageRollBonus(wearer));
        assertEquals(1, DefensiveEnhancements.castingRollBonus(wearer));
    }

    @Test
    void theAttackRollAndTheDanoRollTakeTheWornColumns() {
        AbstractWeapon sword = sword();
        CharacterSheet plain = attacker(List.of(sword));
        CharacterSheet ladra = attacker(List.of(sword, armour(DefensiveImprovement.LADRA_DO_AETHER)));

        DeliveredAttackResult without = attackDelivery.resolve(attackOn(plain, sword).build());
        DeliveredAttackResult with = attackDelivery.resolve(attackOn(ladra, sword).build());

        assertEquals(without.getAttackTotal() + 1, with.getAttackTotal());
        assertEquals(without.getAttackResult().getDamageBonus().getValue() + 1,
                with.getAttackResult().getDamageBonus().getValue());
    }

    @Test
    void aDestroyedOrUnwornHostGrantsNothing() {
        AbstractItem broken = armour(DefensiveImprovement.LADRA_DO_AETHER);
        broken.applyDamage(100);

        assertEquals(0, DefensiveEnhancements.damageRollBonus(characterWearing(broken)));
    }

    // ---- Benção Ymiriana -----------------------------------------------------------------------------

    @Test
    void bencaoYmirianaOpensItsWindowWhenAttackedHitOrMissAndRenewsRatherThanStacking() {
        CharacterSheet wearer = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.BENCAO_YMIRIANA)),
                new Player());

        assertTrue(DefensiveEnhancements.onAttacked(wearer));
        assertTrue(DefensiveEnhancements.onAttacked(wearer));

        assertEquals(1, wearer.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
        assertFalse(DefensiveEnhancements.onAttacked(CharacterSheet.of(characterWearing(), new Player())));
    }

    @Test
    void theYmirianWindowOutlastsTheTurnItOpensOutsideButNotTheWearersNextOne() {
        CharacterSheet wearer = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.BENCAO_YMIRIANA)),
                new Player());

        DefensiveEnhancements.onAttacked(wearer);
        wearer.finishTurn();

        assertEquals(0, wearer.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
    }

    // ---- Espinhosa / Rigidez Refletora ---------------------------------------------------------------

    @Test
    void espinhosaHurtsTheMeleeAttackerAndTheirWeaponAndRigidezDestroysProjectiles() {
        CharacterSheet thorny = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.ESPINHOSA),
                shield(DefensiveImprovement.RIGIDEZ_REFLETORA)), new Player());

        EquipmentRetaliation melee = DefensiveEnhancements.retaliationAgainst(thorny, SkillType.ATAQUE_CORPO_A_CORPO,
                null);
        assertNotNull(melee);
        assertEquals(2, melee.attackerDamage().damage());
        assertEquals(DamageType.FISICO, melee.attackerDamage().descriptor().damageType());
        assertEquals(4, melee.weaponDamage());
        assertFalse(melee.projectileDestroyed());

        EquipmentRetaliation ranged = DefensiveEnhancements.retaliationAgainst(thorny, SkillType.ATAQUE_A_DISTANCIA,
                ProjectileItem.values()[0]);
        assertNotNull(ranged);
        assertNull(ranged.attackerDamage());
        assertEquals(0, ranged.weaponDamage());
        assertTrue(ranged.projectileDestroyed());
    }

    @Test
    void theEquipmentRetaliationDamagesTheAttackingWeaponAndLeavesANaturalOneAlone() {
        AbstractWeapon sword = sword();
        EquipmentRetaliation thorns = new EquipmentRetaliation(null, 2, false);

        assertEquals(2, thorns.damageWeapon(sword));
        assertEquals(2, sword.getDamageTaken());
        assertEquals(0, thorns.damageWeapon(NaturalWeapon.values()[0]));
    }

    @Test
    void anAttackOnAThornyDefenderReportsItsRetaliationHitOrMiss() {
        AbstractWeapon sword = sword();
        CharacterSheet hero = attacker(List.of(sword));
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        CharacterSheet thorny = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.ESPINHOSA)),
                new Player());

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.builder()
                .defender(thorny)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(99)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(sword)
                .attackRoll(ROLL)
                .build());

        assertFalse(result.getHit());
        assertNotNull(result.getEquipmentRetaliation());
        assertEquals(2, result.getEquipmentRetaliation().weaponDamage());
        assertNotNull(foe);
    }

    @Test
    void aDestroyedProjectileIsSpentButNeverJoinsTheRecoveryLedger() {
        CharacterSheet archer = CharacterSheet.of(characterWearing(), new Player());
        archer.startCombat();
        ProjectileItem arrow = java.util.Arrays.stream(ProjectileItem.values())
                .filter(projectile -> projectile.getHead().isRecoverable())
                .findFirst().orElseThrow();

        archer.recordFiredProjectiles(arrow, 3);
        archer.discardFiredProjectiles(arrow, 2);

        assertEquals(1, archer.getFiredProjectilesThisCombat().get(arrow));
        archer.discardFiredProjectiles(arrow, 5);
        assertFalse(archer.getFiredProjectilesThisCombat().containsKey(arrow));
    }

    // ---- Benção Vulcana / Benção Elduriana -----------------------------------------------------------

    @Test
    void bencaoVulcanaCutsElementalAndPhysicalDamageAndElduriananyMagicalOne() {
        CharacterSheet vulcana = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.BENCAO_VULCANA)),
                new Player());
        CharacterSheet elduriana = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.BENCAO_ELDURIANA)),
                new Player());
        DamageDescriptor physical = new DamageDescriptor(DamageType.FISICO);
        DamageDescriptor elemental = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);
        DamageDescriptor magicalElemental = new DamageDescriptor(DamageType.MAGICO, ElementalType.FOGO);
        DamageDescriptor profane = new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO);

        assertEquals(9, hit(vulcana, physical));
        assertEquals(9, hit(vulcana, elemental));
        assertEquals(9, hit(vulcana, magicalElemental));
        assertEquals(10, hit(vulcana, profane));
        assertEquals(10, hit(elduriana, physical));
        assertEquals(9, hit(elduriana, magicalElemental));
        assertEquals(9, hit(elduriana, profane));
    }

    @Test
    void bencaoEldurianaShortensOnlyAnEncantamentoOrMaldicao() {
        Character wearer = characterWearing(armour(DefensiveImprovement.BENCAO_ELDURIANA));
        Spell enchantment = SpellCatalog.ofType(MagicType.ENCANTAMENTO).get(0);
        Spell other = SpellCatalog.all().stream()
                .filter(spell -> spell.getPrimaryType() != MagicType.ENCANTAMENTO
                        && spell.getSecondaryType() != MagicType.ENCANTAMENTO
                        && spell.getPrimaryType() != MagicType.MALDICAO
                        && spell.getSecondaryType() != MagicType.MALDICAO)
                .findFirst().orElseThrow();

        assertEquals(1, DefensiveEnhancements.hostileDurationReduction(wearer, enchantment));
        assertEquals(0, DefensiveEnhancements.hostileDurationReduction(wearer, other));
    }

    @Test
    void bencaoDeProtecaoIsExtendedByARealAttacksDamageToo() {
        AbstractItem protectedItem = armour(DefensiveImprovement.BENCAO_DE_PROTECAO);
        CharacterSheet sheet = CharacterSheet.of(characterWearing(protectedItem), new Player());
        SceneContext round1 = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false, null,
                java.util.UUID.randomUUID());

        new org.aventyrs.core.effect.DamageInteraction().applyTo(sheet, round1, DamageType.FISICO, null, 5, false);

        assertTrue(protectedItem.hasActiveImprovementEffect(round1));
    }

    // ---- Camuflada / Benção Selvagem -----------------------------------------------------------------

    @Test
    void camufladaAddsItsExtraVantagemOnlyInTheChosenTerrain() {
        AbstractItem camouflaged = AbstractItem.builder().name("Armadura").category(ItemCategory.ARMOR).build();
        camouflaged.addImprovement(ItemImprovement.camuflada(TerrainType.FOREST));
        CharacterSheet sheet = CharacterSheet.of(characterWearing(camouflaged), new Player());

        int elsewhere = new FurtividadeInteraction().applyTo(sheet, context(TerrainType.DESERT)).getSkillRollBonus();
        int forest = new FurtividadeInteraction().applyTo(sheet, context(TerrainType.FOREST)).getSkillRollBonus();

        assertEquals(elsewhere + 2, forest);
    }

    @Test
    void bencaoSelvagemWidensTheMarginOfANaturalWeaponOnly() {
        ItemImprovement selvagem = ItemImprovement.of(DefensiveImprovement.BENCAO_SELVAGEM);
        Character wearer = characterWearing();

        assertEquals(1, selvagem.resolveCriticalMarginIncrease(NaturalWeapon.values()[0], wearer));
        assertEquals(0, selvagem.resolveCriticalMarginIncrease(sword(), wearer));
    }

    // ---- Solve-Vidas / Ladra do AEther ---------------------------------------------------------------

    @Test
    void solveVidasKillsAFallenLivingEnemyWithinCurtaAndHealsItsWearerByTheirVigor() {
        CharacterSheet wearer = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.SOLVE_VIDAS)),
                new Player());
        wearer.applyDamage(5);
        CharacterSheet near = CharacterSheet.of(vigorous(), new Player());
        CharacterSheet far = CharacterSheet.of(vigorous(), new Player());
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        near.applyDamage(hitPoints.getMaxHitPoints(near.getCharacter()));
        far.applyDamage(hitPoints.getMaxHitPoints(far.getCharacter()));
        SceneContext context = new SceneContext(List.of(), List.of(near, far),
                Map.of(near, Range.DISTANCIA_CURTA, far, Range.DISTANCIA_LONGA), null);

        List<CombatantSheet> reaped = DefensiveEnhancements.reapSouls(wearer, context);

        assertEquals(List.of(near), reaped);
        assertEquals(CharacterStatus.DEAD, hitPoints.getStatus(near));
        assertTrue(hitPoints.getStatus(far) != CharacterStatus.DEAD);
        assertEquals(5 - 2, wearer.getDamageTaken());
        assertFalse(near.isBeyondRevival());
        assertTrue(DefensiveEnhancements.reapSouls(wearer, context).isEmpty());
    }

    @Test
    void ladraDoAetherRecoversOnAnotherCastersNonSementeMagiaOnly() {
        CharacterSheet thief = CharacterSheet.of(characterWearing(armour(DefensiveImprovement.LADRA_DO_AETHER)),
                new Player());
        CharacterSheet caster = CharacterSheet.of(characterWearing(), new Player());
        thief.spendMagicPoints(3);
        thief.spendDeterminationPoints(3);
        Spell seed = SpellCatalog.at(BranchLevel.SEMENTE).get(0);
        Spell grown = SpellCatalog.all().stream().filter(spell -> spell.getBranchLevel() != BranchLevel.SEMENTE)
                .findFirst().orElseThrow();

        assertEquals(0, DefensiveEnhancements.onAffectedBySpell(thief, seed, caster));
        assertEquals(0, DefensiveEnhancements.onAffectedBySpell(thief, grown, thief));
        assertEquals(1, DefensiveEnhancements.onAffectedBySpell(thief, grown, caster));
        assertEquals(2, thief.recoverMagicPoints(0), "3 spent, 1 recovered");
        assertEquals(2, thief.recoverDeterminationPoints(0), "3 spent, 1 recovered");
    }

    // ---- fixtures ------------------------------------------------------------------------------------

    private int hit(final CharacterSheet target, final DamageDescriptor descriptor) {
        return damageService.calculateFinalDamage(target, null, descriptor, null, 10, false);
    }

    private static SceneContext context(final TerrainType terrain) {
        return new SceneContext(List.of(), List.of(), Map.of(), terrain);
    }

    private static AbstractItem armour(final DefensiveImprovement improvement) {
        AbstractItem item = AbstractItem.builder().name("Armadura").category(ItemCategory.ARMOR).hardness(10).build();
        item.addImprovement(ItemImprovement.of(improvement));
        return item;
    }

    private static AbstractItem shield(final DefensiveImprovement improvement) {
        AbstractItem item = AbstractItem.builder().name("Escudo").category(ItemCategory.SHIELD).hardness(10).build();
        item.addImprovement(ItemImprovement.of(improvement));
        return item;
    }

    private static AbstractWeapon sword() {
        return AbstractWeapon.builder()
                .name("Espada")
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .hardness(10)
                .build();
    }

    private static Character characterWearing(final AbstractItem... items) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.DEXTERITY, 3)))
                .equipment(new ArrayList<>(List.of(items)))
                .build();
    }

    private static Character vigorous() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.VIGOR, 2)))
                .build();
    }

    /** Força 6, Graduação 6 in Ataque Corpo a Corpo, wearing and wielding items. */
    private static CharacterSheet attacker(final List<AbstractItem> items) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(6);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .equipment(new ArrayList<>(items))
                .drawnWeapons(new ArrayList<>(items.stream().filter(AbstractWeapon.class::isInstance).map(org.aventyrs.core.item.Weapon.class::cast).toList()))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static DeliveredAttack.DeliveredAttackBuilder attackOn(final CharacterSheet hero, final AbstractWeapon weapon) {
        return DeliveredAttack.from(GenericMonster.CAPANGA.spawn(new Player()), DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(weapon)
                .attackRoll(ROLL);
    }
}
