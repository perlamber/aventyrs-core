package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.item.ItemType;
import org.aventyrs.core.item.PowerStoneType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.Zumbi;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Profano and Sagrado as a hit's nature (core 0.0.89) — a qualifier on Físico or Mágico, never a type of its own. */
class DamageSanctityTest {

    private static final DamageDescriptor PROFANE_PHYSICAL =
            new DamageDescriptor(DamageType.FISICO, null, DamageSanctity.PROFANO);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void aSanctityScopeMatchesOnlyAHitOfThatNature() {
        DamageScope profane = DamageScope.sanctity(DamageSanctity.PROFANO);

        assertTrue(profane.matches(DamageType.MAGICO, new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO)));
        assertTrue(profane.matches(DamageType.FISICO, PROFANE_PHYSICAL));
        assertFalse(profane.matches(DamageType.FISICO, null));
        assertFalse(profane.matches(DamageType.FISICO, PROFANE_PHYSICAL.withSanctity(DamageSanctity.SAGRADO)));
        assertTrue(DamageScope.PHYSICAL.matches(DamageType.FISICO, PROFANE_PHYSICAL), "a Profano hit is still Físico");
    }

    /** The Zumbi's Anatomia de Morto-Vivo Menor: "imunes a danos Profanos e Naturais". */
    @Test
    void theZumbiIsImmuneToProfanoAndNatural() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());

        assertTrue(zumbi.isImmuneToDamage(DamageType.FISICO, PROFANE_PHYSICAL));
        assertTrue(zumbi.isImmuneToDamage(DamageType.ELEMENTAL,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.NATURAL)));
        assertFalse(zumbi.isImmuneToDamage(DamageType.FISICO, null));
        assertEquals(0, new DamageServiceImpl().calculateFinalDamage(zumbi, null, PROFANE_PHYSICAL, null, 8, false));
    }

    /** Definhar is "Dano Físico Profano": an undead takes nothing from it, a Profano reduction eats it. */
    @Test
    void definharsProfaneTickRespectsImmunityAndReduction() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        new Definhar().applyTo(zumbi);
        zumbi.tickTemporaryEffects();
        assertEquals(0, zumbi.getDamageTaken());

        CharacterSheet warded = sheet();
        warded.grantBlessing(new Blessing(ModifierType.PROFANE_DAMAGE_REDUCTION, 3, 5, TargetScope.SELF, "test"));
        new Definhar().applyTo(warded);
        warded.tickTemporaryEffects();
        assertEquals(0, warded.getDamageTaken());
    }

    /** The two Pedras: Opala's defensive -3 Profano, Turmalina's base -3 Sagrado, and each one's offensive nature. */
    @Test
    void thePedrasReduceAndConferANature() {
        assertEquals(3, PowerStoneType.OPALA_PURIFICADORA.resolveBonus(ModifierType.PROFANE_DAMAGE_REDUCTION,
                ItemType.DEFENSIVE));
        assertEquals(3, PowerStoneType.TURMALINA_OBSCURA.resolveBonus(ModifierType.SACRED_DAMAGE_REDUCTION,
                ItemType.OFFENSIVE));
        assertEquals(DamageSanctity.SAGRADO, PowerStoneType.OPALA_PURIFICADORA.getOffensiveSanctity());
        assertEquals(DamageSanctity.PROFANO, PowerStoneType.TURMALINA_OBSCURA.getOffensiveSanctity());
    }

    @Test
    void aProfaneReductionReachesOnlyProfaneHits() {
        CharacterSheet warded = sheet();
        warded.grantBlessing(new Blessing(ModifierType.PROFANE_DAMAGE_REDUCTION, 3, 5, TargetScope.SELF, "test"));
        DamageServiceImpl damage = new DamageServiceImpl();

        assertEquals(damage.calculateFinalDamage(warded, null, new DamageDescriptor(DamageType.MAGICO), null, 8, false) - 3,
                damage.calculateFinalDamage(warded, null,
                        new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO), null, 8, false));
    }

    /** "O dano causado … é Profano em substituição aos seus tipos" — a triggered Toque Sombrio marks the hit. */
    @Test
    void aTriggeredToqueSombrioMarksTheHitProfane() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(6);
        Character fighter = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build()).build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .build();
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.from(zumbi, DefenseType.PHYSICAL)
                .attacker(CharacterSheet.of(fighter, new Player()))
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .effectChain(new ToqueSombrio())
                .build());
        DamageInteraction head = (DamageInteraction) result.getAttackResult().getNextInteraction();

        assertEquals(DamageSanctity.PROFANO, head.getSanctity());
        head.applyTo(zumbi, 9, false);
        assertEquals(0, zumbi.getDamageTaken(), "the undead is immune to the Profano hit");
    }
}
