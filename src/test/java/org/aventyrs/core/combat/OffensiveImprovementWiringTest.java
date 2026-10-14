package org.aventyrs.core.combat;

import org.aventyrs.core.character.Resistance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.BypassedReduction;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.AttackRangeServiceImpl;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.effect.CorrenteAtacante;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.IgnoraResistencias;
import org.aventyrs.core.effect.Inquisicao;
import org.aventyrs.core.effect.RouboDeDeterminacao;
import org.aventyrs.core.effect.RouboDeMana;
import org.aventyrs.core.effect.RouboDeVida;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttachedMeleeAttack;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.HandBudget;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.OffensiveImprovement;
import org.aventyrs.core.item.OffensiveItemImprovement;
import org.aventyrs.core.item.OffensiveMasterpiece;
import org.aventyrs.core.item.WeaponEnhancements;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.IraDeVulcanoSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TimedElementalResistance;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every {@link OffensiveImprovement} clause reaching the attack, the damage and the Magia (core 0.1.6.9). */
class OffensiveImprovementWiringTest {

    /** 5+5+5 = 15 against DF 13 with a +12 attacker: a hit far past the Corrente margin, and no critical. */
    private static final SkillRoll CHAIN_ROLL = new SkillRoll(List.of(5, 5, 5));

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    // --- the weapon-scoped columns --------------------------------------------------------------

    @Test
    void theAtaqueColumnReachesOnlyAnAttackMadeWithItsHost() {
        AbstractWeapon gauntleted = melee(OffensiveImprovement.MANOPLA_DE_SEGURANCA);
        AbstractWeapon plain = melee(null);
        CharacterSheet hero = attacker(gauntleted, plain);

        int withHost = attack(hero, gauntleted).getAttackTotal();
        int withOther = attack(hero, plain).getAttackTotal();

        assertEquals(withOther + 1, withHost);
    }

    @Test
    void theDanosColumnsOfTheHostAndItsObraPrimaJoinTheDanoRoll() {
        AbstractWeapon blessed = melee(OffensiveImprovement.BENCAO_YMIRIANA);
        blessed.setMasterpiece(OffensiveMasterpiece.BRUTAL);   // Danos +1 column too
        AbstractWeapon plain = melee(null);
        CharacterSheet hero = attacker(blessed, plain);

        int withHost = attack(hero, blessed).getAttackResult().getDamageBonus().getValue();
        int withOther = attack(hero, plain).getAttackResult().getDamageBonus().getValue();

        assertEquals(withOther + 2, withHost);
    }

    // --- Roubos ---------------------------------------------------------------------------------

    @Test
    void solveVidasCarriesRouboDeVidaThreeRightBehindTheDamage() {
        AbstractWeapon cursed = melee(OffensiveImprovement.SOLVE_VIDAS);
        DamageInteraction head = head(attack(attacker(cursed), cursed));

        RouboDeVida steal = assertInstanceOf(RouboDeVida.class, head.getNextInteraction());
        assertEquals(3, steal.getAmount());
    }

    @Test
    void ladraDoAetherStealsTwoManaAndTwoDeterminationOnEveryHit() {
        AbstractWeapon cursed = melee(OffensiveImprovement.LADRA_DO_AETHER);
        List<Interaction<CombatantSheet>> stages = stages(head(attack(attacker(cursed), cursed)));

        assertEquals(2, find(stages, RouboDeMana.class).getAmount());
        assertEquals(2, find(stages, RouboDeDeterminacao.class).getAmount());
    }

    @Test
    void rouboDeManaMovesOnlyWhatTheTargetStillHas() {
        CharacterSheet thief = sheet();
        CharacterSheet victim = sheet();
        thief.spendMagicPoints(3);
        int victimMax = new MagicPointsServiceImpl().getCurrentMagicPoints(victim.getCharacter(), victim);
        victim.spendMagicPoints(victimMax - 1);   // one PM left

        new RouboDeMana(thief, 2).applyTo(victim);

        assertEquals(victimMax, victim.getManaSpent());
        assertEquals(2, thief.getManaSpent());
    }

    // --- Correntes ------------------------------------------------------------------------------

    @Test
    void bencaoEldurianaStealsManaAndItsInquisicaoMakesTheTargetsMagiasHarderUntilTheAttackersTurn() {
        AbstractWeapon blessed = melee(OffensiveImprovement.BENCAO_ELDURIANA);
        CharacterSheet hero = attacker(blessed);
        List<Interaction<CombatantSheet>> stages = stages(head(attack(hero, blessed)));

        assertEquals(1, find(stages, RouboDeMana.class).getAmount());
        Inquisicao inquisicao = find(stages, Inquisicao.class);

        CharacterSheet caster = sheet();
        inquisicao.applyTo(caster);
        assertEquals(2, caster.getSpellDifficultyIncrease());
        assertEquals(-2, new SpellCastingServiceImpl()
                .resolveCastingDifficultyReduction(IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR, caster));

        caster.expireMarksAnchoredTo(sheet());   // someone else's Turn — still marked
        assertEquals(2, caster.getSpellDifficultyIncrease());
        caster.expireMarksAnchoredTo(hero);      // the attacker's Turn begins
        assertEquals(0, caster.getSpellDifficultyIncrease());
    }

    @Test
    void aCorrenteOnlyFiresPastTheThreshold() {
        AbstractWeapon blessed = melee(OffensiveImprovement.BENCAO_ELDURIANA);
        CharacterSheet hero = attacker(blessed);
        DeliveredAttackResult grazing = attackDelivery.resolve(attackOn(hero, blessed)
                .attackRoll(new SkillRoll(List.of(1, 1, 1))).build());   // 3 + 12 = 15: margin 2

        assertTrue(grazing.getHit());
        assertFalse(stages(head(grazing)).stream().anyMatch(Inquisicao.class::isInstance));
    }

    @Test
    void correnteComPesoThrowsItsDieAfterMitigationAndNeedsBothHands() {
        AbstractWeapon chained = melee(OffensiveImprovement.CORRENTE_COM_PESO);
        CharacterSheet hero = attacker(chained);
        DeliveredAttackResult result = attackDelivery.resolve(attackOn(hero, chained)
                .attackRoll(CHAIN_ROLL).diceRoller(DiceRoller.fixed(List.of(4))).build());

        CorrenteAtacante chain = find(stages(head(result)), CorrenteAtacante.class);
        CharacterSheet target = sheet();
        chain.applyTo(target);
        assertEquals(4, target.getDamageTaken());

        assertTrue(HandBudget.isTwoHanded(chained));
        assertFalse(HandBudget.isTwoHanded(melee(null)));
    }

    @Test
    void bencaoVulcanaIgnoresRdAlwaysAndReAndRmOnceItsCorrenteFires() {
        AbstractWeapon blessed = melee(OffensiveImprovement.BENCAO_VULCANA);
        CharacterSheet hero = attacker(blessed);

        DamageInteraction chained = head(attack(hero, blessed));
        assertEquals(Set.of(BypassedReduction.RD, BypassedReduction.RE, BypassedReduction.RM), chained.getBypassed());
        assertTrue(stages(chained).stream().anyMatch(IgnoraResistencias.class::isInstance));

        DamageInteraction grazing = head(attackDelivery.resolve(attackOn(hero, blessed)
                .attackRoll(new SkillRoll(List.of(1, 1, 1))).build()));
        assertEquals(Set.of(BypassedReduction.RD), grazing.getBypassed());
    }

    @Test
    void aBypassedStageIsSkippedAndTheOthersStillApply() {
        DamageServiceImpl damage = new DamageServiceImpl();
        CharacterSheet armoured = sheet();
        armoured.grantTemporaryBonus(ModifierType.DAMAGE_REDUCTION, 4, 3);

        assertEquals(6, damage.calculateFinalDamage(armoured, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(), null));
        assertEquals(10, damage.calculateFinalDamage(armoured, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(BypassedReduction.RD), null));
        assertEquals(6, damage.calculateFinalDamage(armoured, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(BypassedReduction.RE, BypassedReduction.RM), null));
    }

    // --- Benção Elemental: fully both types ------------------------------------------------------

    @Test
    void bencaoElementalsElementIsRecordedAndCarriedToTheHead() {
        AbstractWeapon blessed = melee(null);
        blessed.addImprovement(OffensiveItemImprovement.bencaoElemental(ElementalType.FOGO));

        assertEquals(ElementalType.FOGO, head(attack(attacker(blessed), blessed)).getAdditionalElement());
        assertThrows(IllegalArgumentException.class, () -> OffensiveItemImprovement.bencaoElemental(ElementalType.TODOS));
        // A bare fitting keeps the columns and deals no element.
        assertNull(WeaponEnhancements.additionalElement(melee(OffensiveImprovement.BENCAO_ELEMENTAL)));
    }

    @Test
    void theAddedElementMeetsImmunityAndReOnTopOfTheHitsOwnReductions() {
        DamageServiceImpl damage = new DamageServiceImpl();

        CharacterSheet fireproof = sheet();
        fireproof.applyEffect(new DamageScopeEffect(DamageScopeEffect.Kind.IMMUNE, DamageScope.element(ElementalType.FOGO),
                null, "test"));
        assertEquals(0, damage.calculateFinalDamage(fireproof, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(), ElementalType.FOGO));
        assertEquals(10, damage.calculateFinalDamage(fireproof, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(), null));

        CharacterSheet resistant = sheet();
        resistant.grantTemporaryBonus(ModifierType.DAMAGE_REDUCTION, 2, 3);
        resistant.applyEffect(new TimedElementalResistance(ElementalType.FOGO, 1, 3, false));
        assertEquals(10 - 2 - Resistance.RE.instance(), damage.calculateFinalDamage(resistant, null, DamageType.FISICO,
                null, 10, false, false, null, Set.of(), ElementalType.FOGO));   // RD 2 and one RE instance
        assertEquals(8, damage.calculateFinalDamage(resistant, null, DamageType.FISICO, null, 10, false, false, null,
                Set.of(BypassedReduction.RE), ElementalType.FOGO));
    }

    // --- Magias -----------------------------------------------------------------------------------

    @Test
    void aDrawnWeaponsAprimoramentosReachTheMagiasCastWhileItIsDrawn() {
        AbstractWeapon arcane = melee(OffensiveImprovement.ARCANISTA);
        arcane.addImprovement(OffensiveImprovement.BENCAO_ELEMENTAL);
        Character drawn = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new ArrayList<>(List.of(arcane))).drawnWeapons(new ArrayList<>(List.of(arcane))).build();
        Character sheathed = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new ArrayList<>(List.of(arcane))).build();

        // Arcanista's Conjuração column +1; Benção Elemental's +1 only for an elemental Magia.
        assertEquals(2, WeaponEnhancements.castingRollBonus(drawn, IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR));
        assertEquals(1, WeaponEnhancements.castingRollBonus(drawn, VidaSpell.ALIVIAR_A_DOR));
        assertEquals(2, WeaponEnhancements.spellDamageBonus(drawn, IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR));
        assertEquals(1, WeaponEnhancements.spellHealingBonus(drawn, VidaSpell.ALIVIAR_A_DOR));
        assertEquals(0, WeaponEnhancements.castingRollBonus(sheathed, IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR));
    }

    // --- reach, magazine, category ----------------------------------------------------------------

    @Test
    void alcanceEstendidoAddsOneUdAndItsBonusAgainstATargetNotAdjacent() {
        AbstractWeapon extended = melee(OffensiveImprovement.ALCANCE_ESTENDIDO);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        AttackRangeServiceImpl range = new AttackRangeServiceImpl();

        assertEquals(range.getEffectiveRangeInUnidadesDeDistancia(character, melee(null), Set.of()) + 1,
                range.getEffectiveRangeInUnidadesDeDistancia(character, extended, Set.of()));

        CharacterSheet near = sheet();
        CharacterSheet far = sheet();
        SceneContext scene = new SceneContext(List.of(), List.of(near, far),
                Map.of(near, Range.ADJACENTE, far, Range.DISTANCIA_MUITO_CURTA));
        assertEquals(0, WeaponEnhancements.nonAdjacentBonus(extended, near, scene));
        assertEquals(1, WeaponEnhancements.nonAdjacentBonus(extended, far, scene));
        assertEquals(0, WeaponEnhancements.nonAdjacentBonus(extended, far, null));
    }

    @Test
    void penteAlongadoWidensABestasMagazineAndNothingElse() {
        AbstractWeapon besta = ranged(ItemCategory.CROSSBOW, 1);
        besta.addImprovement(OffensiveImprovement.PENTE_ALONGADO);
        AbstractWeapon arco = ranged(ItemCategory.BOW, 0);
        arco.addImprovement(OffensiveImprovement.PENTE_ALONGADO);

        assertEquals(4, besta.getEffectiveMagazineCapacity());
        assertEquals(0, arco.getEffectiveMagazineCapacity());
    }

    @Test
    void aBaionetaMakesTheBowALancaAndLetsItStrikeCorpoACorpo() {
        AbstractWeapon arco = ranged(ItemCategory.BOW, 0);
        arco.addImprovement(OffensiveImprovement.BAIONETA);
        arco.setMasterpiece(OffensiveMasterpiece.BRUTAL);

        assertTrue(arco.countsAs(ItemCategory.SPEAR));
        assertTrue(AttackMethod.SPEAR.matches(arco, null));
        assertTrue(AttackMethod.BOW.matches(arco, null));

        AttachedMeleeAttack bayonet = AttachedMeleeAttack.of(arco);
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, bayonet.getSkillType());
        assertEquals(Range.ADJACENTE, bayonet.getRange());
        assertEquals(ItemCategory.SPEAR, bayonet.getCategory());
        assertNull(bayonet.getAmmunitionKind());
        // The copy's own Brutal still sharpens the swing — the view's host is the bow.
        Character archer = CharacterFixture.blank(CharacterFixture.BLANK).equipment(List.of(arco)).build();
        assertEquals(new DamageBaseServiceImpl().getDamageBase(archer, arco),
                new DamageBaseServiceImpl().getDamageBase(archer, bayonet));

        assertThrows(IllegalArgumentException.class, () -> AttachedMeleeAttack.of(ranged(ItemCategory.BOW, 0)));
        AbstractWeapon machado = ranged(ItemCategory.CROSSBOW, 1);
        machado.addImprovement(OffensiveImprovement.MACHADO_ANEXO);
        assertEquals(ItemCategory.LIGHT_BLADE, AttachedMeleeAttack.of(machado).getCategory());
    }

    // --- helpers -----------------------------------------------------------------------------------

    private AbstractWeapon melee(final OffensiveImprovement improvement) {
        AbstractWeapon weapon = AbstractWeapon.builder()
                .name("Adaga")
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        if (improvement != null) {
            weapon.addImprovement(improvement);
        }
        return weapon;
    }

    private AbstractWeapon ranged(final ItemCategory category, final int magazine) {
        return AbstractWeapon.builder()
                .name(category == ItemCategory.BOW ? "Arco" : "Besta")
                .category(category)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .magazineCapacity(magazine)
                .build();
    }

    private CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /** Força 6, Graduação 6 in Ataque Corpo a Corpo — a +12 attacker — wielding weapons. */
    private CharacterSheet attacker(final AbstractWeapon... weapons) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(6);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .equipment(new ArrayList<>(List.of(weapons)))
                .drawnWeapons(new ArrayList<>(List.of(weapons)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private DeliveredAttack.DeliveredAttackBuilder attackOn(final CharacterSheet hero, final AbstractWeapon weapon) {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());   // DF 13
        return DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(weapon);
    }

    private DeliveredAttackResult attack(final CharacterSheet hero, final AbstractWeapon weapon) {
        DeliveredAttackResult result = attackDelivery.resolve(attackOn(hero, weapon).attackRoll(CHAIN_ROLL).build());
        assertTrue(result.getHit());
        return result;
    }

    private static DamageInteraction head(final DeliveredAttackResult result) {
        return assertInstanceOf(DamageInteraction.class, result.getAttackResult().getNextInteraction());
    }

    private static List<Interaction<CombatantSheet>> stages(final Interaction<CombatantSheet> head) {
        List<Interaction<CombatantSheet>> stages = new ArrayList<>();
        for (Interaction<CombatantSheet> next = head.getNextInteraction(); next != null; next = next.getNextInteraction()) {
            stages.add(next);
        }
        return stages;
    }

    private static <T> T find(final List<Interaction<CombatantSheet>> stages, final Class<T> type) {
        return stages.stream().filter(type::isInstance).map(type::cast).findFirst()
                .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " in " + stages));
    }
}
