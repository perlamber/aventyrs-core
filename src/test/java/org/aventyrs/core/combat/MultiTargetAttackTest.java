package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end multi-target resolution, driven by the Talento that produces it — {@code
 * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "seus ataques afetam um alvo adicional".
 *
 * <p>The objective throughout is what {@link AttackDelivery} reports and what the assembled chain
 * actually deals, never a hook's own return value: one roll compared against each foe's own
 * Defesa, a Desvantagem on the single dano roll while more than one target is named, and Meio-Dano
 * on the additional target alone.
 */
class MultiTargetAttackTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /**
     * A bare-handed stylist: Ataque Corpo-a-Corpo 5 and one Título Aventyr Desperto (ARTE_FLUIDA's
     * exact ladder), Força 6 and Graduação 6 on top so the attack total clears a Capanga's DF 13
     * comfortably. Nothing equipped, so the Talento's two gates are both open.
     */
    private static CharacterSheet fluidStylist() {
        CharacterSkill melee = CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(6).build())
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .primaryTitle(new Santo(List.of(), List.of()))
                .build();
        character.grantFeat(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA);
        return CharacterSheet.of(character, new Player());
    }

    /** The same character without the Talento — the control every "because of ARTE_FLUIDA" claim needs. */
    private static CharacterSheet plainBrawler() {
        CharacterSkill melee = CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(6).build())
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** A Capanga whose Defesa Física is rewritten, so the two foes need different totals to hit. */
    private static MonsterSheet foeWithPhysicalDefense(final int physicalDefense) {
        MonsterSheet plain = GenericMonster.CAPANGA.spawn(new Player());
        return MonsterSheet.of(plain.getCharacter(), plain.getPlayer(), physicalDefense,
                plain.getMagicDefense(), plain.getAttackDifficulty(), plain.getAttackBonus());
    }

    private DeliveredAttack.DeliveredAttackBuilder attack(final CharacterSheet attacker, final MonsterSheet primary) {
        return DeliveredAttack.from(primary, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO);
    }

    // ---------- how many targets an attack may name ----------

    @Test
    void aCharacterWithoutTheTalentoCannotNameASecondTarget() {
        MonsterSheet primary = foeWithPhysicalDefense(13);
        MonsterSheet second = foeWithPhysicalDefense(13);
        DeliveredAttack attack = attack(plainBrawler(), primary)
                .additionalTarget(AttackTarget.of(second, DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build();

        assertThrows(IllegalOperationException.class, () -> attackDelivery.resolve(attack));
    }

    @Test
    void arteFluidaAllowsExactlyOneExtraTargetAndNoMore() {
        CharacterSheet stylist = fluidStylist();
        MonsterSheet primary = foeWithPhysicalDefense(13);
        SkillRoll roll = new SkillRoll(List.of(2, 2, 2));

        DeliveredAttackResult twoTargets = attackDelivery.resolve(attack(stylist, primary)
                .additionalTarget(AttackTarget.of(foeWithPhysicalDefense(13), DefenseType.PHYSICAL))
                .attackRoll(roll)
                .build());
        assertEquals(1, twoTargets.getAdditionalTargetResults().size());

        DeliveredAttack threeTargets = attack(stylist, primary)
                .additionalTarget(AttackTarget.of(foeWithPhysicalDefense(13), DefenseType.PHYSICAL))
                .additionalTarget(AttackTarget.of(foeWithPhysicalDefense(13), DefenseType.PHYSICAL))
                .attackRoll(roll)
                .build();
        assertThrows(IllegalOperationException.class, () -> attackDelivery.resolve(threeTargets));
    }

    // ---------- one roll, each foe's own Defesa ----------

    @Test
    void theOneAttackTotalIsComparedAgainstEachTargetsOwnDefesa() {
        MonsterSheet weak = foeWithPhysicalDefense(13);
        MonsterSheet tough = foeWithPhysicalDefense(30);

        // 6 Força + 6 Graduação = 12, +6 dice = 18: over the first Defesa, well under the second.
        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), weak)
                .additionalTarget(AttackTarget.of(tough, DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertEquals(18, result.getAttackTotal());
        assertTrue(result.getHit());
        assertEquals(5, result.getMargin());

        DeliveredAttackTargetResult extra = result.getAdditionalTargetResults().get(0);
        assertEquals(tough, extra.getDefender());
        assertEquals(30, extra.getRequiredTotal());
        assertFalse(extra.getHit());
        assertEquals(-12, extra.getMargin());
        assertNull(extra.getNextInteraction(), "a missed target gets no chain");
    }

    @Test
    void anAdditionalTargetCanBeHitWhileThePrimaryOneIsMissed() {
        MonsterSheet tough = foeWithPhysicalDefense(30);
        MonsterSheet weak = foeWithPhysicalDefense(13);

        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), tough)
                .additionalTarget(AttackTarget.of(weak, DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertFalse(result.getHit());
        assertNull(result.getAttackResult().getNextInteraction());
        assertTrue(result.getAdditionalTargetResults().get(0).getHit());
        assertNotNull(result.getAdditionalTargetResults().get(0).getNextInteraction());
    }

    /** The Corrente threshold is judged per target, because both the margin and the foe are. */
    @Test
    void theEffectChainThresholdIsJudgedAgainstEachTargetsOwnMargin() {
        MonsterSheet weak = foeWithPhysicalDefense(13);   // margin 5 — exactly the threshold
        MonsterSheet middling = foeWithPhysicalDefense(17); // margin 1 — a hit, but short of it

        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), weak)
                .additionalTarget(AttackTarget.of(middling, DefenseType.PHYSICAL))
                .effectChain(new org.aventyrs.core.effect.Definhar())
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertTrue(result.getEffectChainTriggered());
        assertTrue(result.getAdditionalTargetResults().get(0).getHit());
        assertFalse(result.getAdditionalTargetResults().get(0).getEffectChainTriggered());
    }

    // ---------- the dano Desvantagem ----------

    @Test
    void namingASecondTargetCostsTheStylistDesvantagemOnTheDanoRoll() {
        CharacterSheet stylist = fluidStylist();
        MonsterSheet primary = foeWithPhysicalDefense(13);
        SkillRoll roll = new SkillRoll(List.of(2, 2, 2));

        DeliveredAttackResult single = attackDelivery.resolve(attack(stylist, primary)
                .attackRoll(roll).build());
        assertNull(single.getAttackResult().getDamageBonus(), "one target, nothing to pay");

        DeliveredAttackResult doubled = attackDelivery.resolve(attack(stylist, primary)
                .additionalTarget(AttackTarget.of(foeWithPhysicalDefense(13), DefenseType.PHYSICAL))
                .attackRoll(roll).build());
        assertNotNull(doubled.getAttackResult().getDamageBonus());
        assertEquals(Skill.DISADVANTAGE_MALUS, doubled.getAttackResult().getDamageBonus().getValue());
    }

    /** Missing an additional target still cost the Desvantagem — the clause is about naming them. */
    @Test
    void theDesvantagemIsChargedEvenWhenTheExtraTargetIsMissed() {
        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), foeWithPhysicalDefense(13))
                .additionalTarget(AttackTarget.of(foeWithPhysicalDefense(30), DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertFalse(result.getAdditionalTargetResults().get(0).getHit());
        assertEquals(Skill.DISADVANTAGE_MALUS, result.getAttackResult().getDamageBonus().getValue());
    }

    // ---------- Meio-Dano on the additional target ----------

    @Test
    void theAdditionalTargetTakesHalfWhatThePrimaryOneTakes() {
        MonsterSheet primary = foeWithPhysicalDefense(13);
        MonsterSheet extra = foeWithPhysicalDefense(13);

        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), primary)
                .additionalTarget(AttackTarget.of(extra, DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        DamageInteraction primaryChain = assertInstanceOf(DamageInteraction.class,
                result.getAttackResult().getNextInteraction());
        DamageInteraction extraChain = assertInstanceOf(DamageInteraction.class,
                result.getAdditionalTargetResults().get(0).getNextInteraction());

        // One attack, one dano roll: the same figure is fed to both chains.
        primaryChain.applyTo(primary, 10, false);
        extraChain.applyTo(extra, 10, false);

        assertEquals(10, primary.getDamageTaken());
        assertEquals(5, extra.getDamageTaken(), "os danos no alvo adicional são reduzidos à metade");
    }

    // ---------- the bonuses-only preview path ----------

    @Test
    void withNoAttackRollEveryTargetsThresholdIsStillReported() {
        MonsterSheet primary = foeWithPhysicalDefense(13);
        MonsterSheet extra = foeWithPhysicalDefense(30);

        DeliveredAttackResult result = attackDelivery.resolve(attack(fluidStylist(), primary)
                .additionalTarget(AttackTarget.of(extra, DefenseType.PHYSICAL))
                .build());

        DeliveredAttackTargetResult extraResult = result.getAdditionalTargetResults().get(0);
        assertEquals(30, extraResult.getRequiredTotal());
        assertNull(extraResult.getHit());
        assertNull(extraResult.getMargin());
        assertNull(extraResult.getNextInteraction());
    }

    @Test
    void resolveNeverAppliesAnythingToAnAdditionalTarget() {
        MonsterSheet primary = foeWithPhysicalDefense(13);
        MonsterSheet extra = foeWithPhysicalDefense(13);

        attackDelivery.resolve(attack(fluidStylist(), primary)
                .additionalTarget(AttackTarget.of(extra, DefenseType.PHYSICAL))
                .attackRoll(new SkillRoll(List.of(2, 2, 2)))
                .build());

        assertEquals(0, extra.getDamageTaken());
    }
}
