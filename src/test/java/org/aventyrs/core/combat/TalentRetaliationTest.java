package org.aventyrs.core.combat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.DuelistaFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A defender's Talento dealing damage back to whoever landed a melee attack on them — {@code
 * Feat#resolveRetaliation}, reported on {@code getOnHitRetaliations()} by both orchestrators.
 */
class TalentRetaliationTest {

    /** 9, well over the Defesa of 5: a plain hit. */
    private static final List<Integer> HIT = List.of(3, 3, 3);

    /** 17: an Acerto Crítico at the default Margem Crítica Menor. */
    private static final List<Integer> CRITICAL = List.of(6, 6, 5);

    /** 3: well under a Defesa of 5 — and a Falha Crítica, which is fine for a miss. */
    private static final List<Integer> MISS = List.of(1, 1, 1);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet holding(final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static DeliveredAttackResult attack(final CharacterSheet defender, final SkillType skill,
                                                final List<Integer> dice, final AttackSource source) {
        return new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(holding())
                .defender(defender)
                .attackSkill(skill)
                .attackSource(source)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attackRoll(new SkillRoll(dice))
                .build());
    }

    // ---------- CORACAO_DE_FERRO ----------

    @Test
    void aLandedMeleeHitCostsTheAttackerOnePhysicalPoint() {
        List<Retaliation> retaliations = attack(holding(DuelistaFeat.CORACAO_DE_FERRO),
                SkillType.ATAQUE_CORPO_A_CORPO, HIT, null).getOnHitRetaliations();

        assertEquals(1, retaliations.size());
        assertEquals(1, retaliations.get(0).damage());
        assertEquals(DamageType.FISICO, retaliations.get(0).descriptor().damageType());
    }

    /** "se o ataque for um Acerto Crítico ao invés disso ele sofre 3 pontos". */
    @Test
    void aCriticalHitCostsThree() {
        assertEquals(3, attack(holding(DuelistaFeat.CORACAO_DE_FERRO), SkillType.ATAQUE_CORPO_A_CORPO,
                CRITICAL, null).getOnHitRetaliations().get(0).damage());
    }

    @Test
    void aMissCostsNothing() {
        assertTrue(attack(holding(DuelistaFeat.CORACAO_DE_FERRO), SkillType.ATAQUE_CORPO_A_CORPO, MISS, null)
                .getOnHitRetaliations().isEmpty());
    }

    @Test
    void aRangedHitCostsNothing() {
        assertTrue(attack(holding(DuelistaFeat.CORACAO_DE_FERRO), SkillType.ATAQUE_A_DISTANCIA, HIT, null)
                .getOnHitRetaliations().isEmpty());
    }

    @Test
    void aDefenderWithoutTheTalentoRetaliatesWithNothing() {
        assertTrue(attack(holding(), SkillType.ATAQUE_CORPO_A_CORPO, HIT, null).getOnHitRetaliations().isEmpty());
    }

    // ---------- SANGUE_ACIDO ----------

    @Test
    void acidBloodDealsOneNaturalPoint() {
        Retaliation acid = attack(holding(MonstruosoFeat.SANGUE_ACIDO), SkillType.ATAQUE_CORPO_A_CORPO, HIT, null)
                .getOnHitRetaliations().get(0);

        assertEquals(1, acid.damage());
        assertEquals(DamageType.FISICO_ELEMENTAL, acid.descriptor().damageType());
        assertEquals(ElementalType.NATURAL, acid.descriptor().elementalType());
    }

    /** "aumenta em +2 se o atacante tiver utilizado de Armas Naturais". */
    @Test
    void anArmaNaturalAttackerTakesTwoMore() {
        assertEquals(3, attack(holding(MonstruosoFeat.SANGUE_ACIDO), SkillType.ATAQUE_CORPO_A_CORPO, HIT,
                NaturalWeapon.GARRAS_AFIADAS).getOnHitRetaliations().get(0).damage());
    }

    @Test
    void bothTalentosAnswerTheSameHit() {
        assertEquals(2, attack(holding(DuelistaFeat.CORACAO_DE_FERRO, MonstruosoFeat.SANGUE_ACIDO),
                SkillType.ATAQUE_CORPO_A_CORPO, HIT, null).getOnHitRetaliations().size());
    }

    // ---------- the incoming side ----------

    /** A foe's melee attack the defender failed to stop — the same report, from AttackReceiver. */
    @Test
    void aFailedDefenceReportsTheRetaliationToo() {
        IncomingAttackResult result = new AttackReceiver().resolve(IncomingAttack.builder()
                .defender(holding(DuelistaFeat.CORACAO_DE_FERRO))
                .attacker(holding())
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertEquals(1, result.getOnHitRetaliations().size());
        assertEquals(1, result.getOnHitRetaliations().get(0).damage());
    }

    @Test
    void aHeldDefenceReportsNone() {
        IncomingAttackResult result = new AttackReceiver().resolve(IncomingAttack.builder()
                .defender(holding(DuelistaFeat.CORACAO_DE_FERRO))
                .attacker(holding())
                .difficultyLevel(DifficultyLevel.VERY_EASY)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseRoll(new SkillRoll(List.of(6, 6, 4)))
                .build());

        assertTrue(result.getOnHitRetaliations().isEmpty());
    }
}
