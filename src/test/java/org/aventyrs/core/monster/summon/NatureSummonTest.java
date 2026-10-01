package org.aventyrs.core.monster.summon;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.DevorarInteiro;
import org.aventyrs.core.effect.InocularVeneno;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ALIADOS DA NATUREZA creatures (core 0.0.92) — {@code docs/rules/magias.txt}. */
class NatureSummonTest {

    private final HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private int maxHitPoints(final MonsterSheet sheet) {
        return hitPoints.getMaxHitPoints(sheet.getCharacter(), sheet);
    }

    private static Weapon attack(final MonsterSheet sheet) {
        return (Weapon) sheet.getCharacter().getEquipment().get(0);
    }

    // ---------- Aliado da Natureza / Predador Regional ----------

    /** "15PV (Multiplicador de PV x5) e Defesas +3", "1d6+4 (Base 2 + Metade da Força)", an animal. */
    @Test
    void anAliadoUntieredMatchesItsStatBlock() {
        MonsterSheet aliado = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());

        assertEquals(15, maxHitPoints(aliado));
        assertEquals(3, aliado.getDefense(DefenseType.PHYSICAL));
        assertEquals(DamageBase.of(1, 2), attack(aliado).getDamageBase());
        assertEquals(CreatureType.ANIMAL, aliado.getCreatureType());
        assertEquals(4, aliado.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO)
                .getGraduation().getGraduationValue());
    }

    /** Graduação 4: +5PV and Defesas +2; 7: Força and Destreza +2; the attack bonus is the Graduação itself. */
    @Test
    void anAliadoGrowsWithItsConjuradorsGraduacao() {
        MonsterSheet fourth = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(4, new Player());
        MonsterSheet seventh = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(7, new Player());

        assertEquals(20, maxHitPoints(fourth));
        assertEquals(5, fourth.getDefense(DefenseType.PHYSICAL));
        assertEquals(4, fourth.getCharacter().getAttributes().getAttribute(AttributeDomain.STRENGTH).getTotal());
        assertEquals(6, seventh.getCharacter().getAttributes().getAttribute(AttributeDomain.STRENGTH).getTotal());
        assertEquals(5, seventh.getCharacter().getAttributes().getAttribute(AttributeDomain.DEXTERITY).getTotal());
    }

    /** "+2 Graduações em suas Perícias e Vigor +1" — 20PV. */
    @Test
    void aPredadorRegionalIsAStrongerAliado() {
        MonsterSheet predador = NatureSummon.of(NatureSummonKind.PREDADOR_REGIONAL).spawn(new Player());

        assertEquals(20, maxHitPoints(predador));
        assertEquals(6, predador.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO)
                .getGraduation().getGraduationValue());
        assertEquals(4, predador.getCharacter().getSkills().get(SkillType.FURTIVIDADE)
                .getGraduation().getGraduationValue());
    }

    /** Membros Múltiplos: "em Rodadas pares recebem +1PA". */
    @Test
    void anAliadoHasAnExtraPontoDeAcaoInEvenRodadas() {
        MonsterSheet aliado = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();

        assertEquals(3, actionPoints.getMaxActionPoints(aliado, 0));
        assertEquals(4, actionPoints.getMaxActionPoints(aliado, 1));
    }

    // ---------- Experimento / Orgulho de Lacerto ----------

    @Test
    void lacertoPowersAreRolledDistinct() {
        assertEquals(List.of(LacertoPower.BRUTO),
                NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, DiceRoller.fixed(5)).getPowers());
        assertEquals(List.of(LacertoPower.INOCULAR_VENENO, LacertoPower.MEMBROS_MULTIPLOS),
                NatureSummon.invoked(NatureSummonKind.ORGULHO_DE_LACERTO, DiceRoller.fixed(1, 1, 6)).getPowers());
        assertTrue(NatureSummon.invoked(NatureSummonKind.ALIADO_DA_NATUREZA, DiceRoller.fixed(1)).getPowers().isEmpty());
    }

    /** "25PV … DF +12 / DM +8", and Bruto's "Recebe RD e Dano aumenta +1d6". */
    @Test
    void aBrutoExperimentoHitsHarderAndShrugsOffBlows() {
        MonsterSheet plain = NatureSummon.of(NatureSummonKind.EXPERIMENTO_DE_LACERTO).spawn(new Player());
        MonsterSheet bruto = NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, DiceRoller.fixed(5))
                .spawn(new Player());

        assertEquals(25, maxHitPoints(plain));
        assertEquals(12, plain.getDefense(DefenseType.PHYSICAL));
        assertEquals(8, plain.getDefense(DefenseType.MAGIC));
        assertEquals(DamageBase.of(1, 3), attack(plain).getDamageBase());
        assertEquals(DamageBase.of(2, 3), attack(bruto).getDamageBase());
        DamageServiceImpl damage = new DamageServiceImpl();
        assertEquals(damage.getTotalDamageReduction(plain.getCharacter()) + 2,
                damage.getTotalDamageReduction(bruto.getCharacter()));
    }

    /** An Orgulho's Bruto: "Recebe RA …, Vigor +3" — 35PV becomes 50. */
    @Test
    void aBrutoOrgulhoGainsVigorAndRa() {
        MonsterSheet orgulho = NatureSummon.of(NatureSummonKind.ORGULHO_DE_LACERTO).spawn(new Player());
        MonsterSheet bruto = NatureSummon.builder().kind(NatureSummonKind.ORGULHO_DE_LACERTO)
                .powers(List.of(LacertoPower.BRUTO)).build().spawn(new Player());

        assertEquals(35, maxHitPoints(orgulho));
        assertEquals(50, maxHitPoints(bruto));
        assertEquals(DamageBase.of(3, 0), attack(bruto).getDamageBase());
        assertEquals(2, new DamageServiceImpl().getTotalAbsoluteDamageReduction(bruto.getCharacter()));
    }

    /** Inocular Veneno rides every attack it lands past the Corrente margin. */
    @Test
    void anExperimentosVenomRidesItsAttack() {
        MonsterSheet experimento = NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, DiceRoller.fixed(1))
                .spawn(new Player());
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(experimento)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 5)))
                .build());

        assertTrue(result.getEffectChainTriggered());
        DamageInteraction head = assertInstanceOf(DamageInteraction.class, result.getAttackResult().getNextInteraction());
        InocularVeneno venom = assertInstanceOf(InocularVeneno.class, head.getNextInteraction());
        venom.applyTo(capanga);
        assertTrue(capanga.hasCondition(ConditionType.ENVENENADO, null));
    }

    @Test
    void devorarInteiroNamesItsCreatureAsTheCaptor() {
        MonsterSheet orgulho = NatureSummon.builder().kind(NatureSummonKind.ORGULHO_DE_LACERTO)
                .powers(List.of(LacertoPower.DEVORAR_INTEIRO)).build().spawn(new Player());

        DevorarInteiro devour = assertInstanceOf(DevorarInteiro.class, orgulho.getAttackEffectChains().get(0));
        assertEquals(orgulho, devour.getCaptor());
    }

    // ---------- Anciente ----------

    /** "70PV (Multiplicador de PV x6) e DF +18 / DM +21", immune to Menores and harmful Encantamentos. */
    @Test
    void anAncienteMatchesItsStatBlock() {
        MonsterSheet anciente = NatureSummon.of(NatureSummonKind.ANCIENTE).spawn(10, new Player());

        assertEquals(70, maxHitPoints(anciente));
        assertEquals(18, anciente.getDefense(DefenseType.PHYSICAL));
        assertEquals(21, anciente.getDefense(DefenseType.MAGIC));
        assertEquals(DamageBase.of(2, 1), attack(anciente).getDamageBase());
        assertTrue(anciente.ignoresMinorCriticalEffects(null));
        assertTrue(anciente.isWardedAgainstEnchantments());
        assertEquals(2, new NatureSummonAbility(NatureSummonKind.ANCIENTE, List.of(), 10).getDifficultyReduction());
        assertFalse(NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player())
                .isWardedAgainstEnchantments());
    }

    /** Membros Múltiplos: "3PA para realizar 2 ataques com Desvantagem" (core 0.0.97). */
    @Test
    void membrosMultiplosPricesAPairOfAttacksAtThreePa() {
        MonsterSheet aliado = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());
        java.util.Set<org.aventyrs.core.feat.Feat> pair = java.util.Set.of(org.aventyrs.core.feat.CriaturaFeat.MEMBROS_MULTIPLOS);
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        assertTrue(aliado.getCharacter().getFeats().contains(org.aventyrs.core.feat.CriaturaFeat.MEMBROS_MULTIPLOS));
        assertFalse(NatureSummon.of(NatureSummonKind.ANCIENTE).spawn(new Player()).getCharacter().getFeats()
                .contains(org.aventyrs.core.feat.CriaturaFeat.MEMBROS_MULTIPLOS));

        aliado.startTurn(1);
        assertEquals(org.aventyrs.core.sheet.ActionCost.ofActionPoints(3),
                actionPoints.getAttackCost(aliado, SkillType.ATAQUE_CORPO_A_CORPO, attack(aliado), pair, 1));
        aliado.recordAction(new org.aventyrs.core.sheet.CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO,
                AttributeDomain.STRENGTH, attack(aliado), org.aventyrs.core.sheet.ActionCost.ofActionPoints(3), 1,
                null, null, pair));
        assertEquals(org.aventyrs.core.sheet.ActionCost.NONE,
                actionPoints.getAttackCost(aliado, SkillType.ATAQUE_CORPO_A_CORPO, attack(aliado), pair, 1));

        int plain = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(aliado, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
        int paired = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(aliado, null, new SkillRoll(List.of(3, 3, 3), null, null, null, null, pair))
                .getSkillRollBonus();
        assertEquals(plain + org.aventyrs.core.skill.Skill.DISADVANTAGE_MALUS, paired);
    }

    /** Sopro Elemental: Experimento "Cone de 4UD. 2d6+2 (1+ Metade do Vigor) … reduzido em 2 a cada UD" — Natural. */
    @Test
    void anExperimentosBreathIsANaturalConeThatFadesWithDistance() {
        NatureSummon experimento = NatureSummon.builder().kind(NatureSummonKind.EXPERIMENTO_DE_LACERTO)
                .powers(List.of(LacertoPower.SOPRO_ELEMENTAL)).build();
        MonsterSheet sheet = experimento.spawn(new Player());
        MonsterSheet target = GenericMonster.CAPANGA.spawn(new Player());

        ElementalDischarge breath = experimento.breath(sheet.getCharacter()).orElseThrow();

        assertEquals(4, breath.coneLength());
        assertEquals(2, breath.diceCount());
        assertEquals(2, breath.flat(), "1 + half of Vigor 3");
        assertEquals(org.aventyrs.core.magic.ElementalType.NATURAL, breath.descriptor().elementalType());
        assertEquals(9, breath.damageAt(7, 1));
        assertEquals(5, breath.damageAt(7, 3));
        int before = target.getDamageTaken();
        assertTrue(breath.dealTo(target, sheet, 7, 1, null,
                new org.aventyrs.core.character.services.DamageServiceImpl()) >= 0);
        assertTrue(target.getDamageTaken() >= before);
        assertTrue(NatureSummon.of(NatureSummonKind.EXPERIMENTO_DE_LACERTO).breath(sheet.getCharacter()).isEmpty());
    }

    /** Aura Elemental: Experimento "2 de Dano Físico Elemental"; Orgulho "1d6 de Dano Mágico Elemental". */
    @Test
    void anAuraIsAFixedOrRolledNaturalDischarge() {
        ElementalDischarge experimento = NatureSummon.builder().kind(NatureSummonKind.EXPERIMENTO_DE_LACERTO)
                .powers(List.of(LacertoPower.AURA_ELEMENTAL)).build().aura().orElseThrow();
        ElementalDischarge orgulho = NatureSummon.builder().kind(NatureSummonKind.ORGULHO_DE_LACERTO)
                .powers(List.of(LacertoPower.AURA_ELEMENTAL)).build().aura().orElseThrow();

        assertEquals(2, experimento.damageAt(0, 1));
        assertEquals(org.aventyrs.core.character.DamageType.FISICO_ELEMENTAL, experimento.descriptor().damageType());
        assertEquals(1, orgulho.diceCount());
        assertEquals(org.aventyrs.core.character.DamageType.ELEMENTAL, orgulho.descriptor().damageType());
    }

    /** Benção da Regeneração: "1d6+5 (Metade do Vigor)" — Vigor 10. */
    @Test
    void theAncienteRegeneratesHalfItsVigorPlusADie() {
        NatureSummon anciente = NatureSummon.of(NatureSummonKind.ANCIENTE);

        assertEquals(5, anciente.regenerationFlat(anciente.spawn(new Player()).getCharacter()));
        assertEquals(0, NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).regenerationFlat(null));
    }
}
