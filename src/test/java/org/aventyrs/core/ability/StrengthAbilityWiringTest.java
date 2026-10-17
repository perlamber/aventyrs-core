package org.aventyrs.core.ability;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageContributionSource;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.combat.ShieldBreak;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemForgery;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link StrengthAbility#SUBJUGAR} (Agarrar and the Encontrão, which is Empurrar and Derrubar both; Vantagem em
 * Dano against a Caído or Desprevenido target) and {@link StrengthAbility#ESTILHACADOR} (objects, and 2 points to the
 * target's Escudo, else Armadura) — table rulings 2026-10-16.
 */
class StrengthAbilityWiringTest {

    private static final SkillRoll HIGH = new SkillRoll(List.of(6, 6, 5));

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final AttributeAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .equipment(new ArrayList<>());
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private DeliveredAttackResult punch(final CharacterSheet attacker, final CharacterSheet defender) {
        return attackDelivery.resolve(DeliveredAttack.builder()
                .defender(defender)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(NaturalWeapon.ATAQUE_DESARMADO)
                .attackRoll(HIGH)
                .build());
    }

    private static int attributeDamage(final DeliveredAttackResult result) {
        // A net-zero dano bonus reports no breakdown at all.
        var breakdown = result.getAttackResult().getDamageBonusBreakdown();
        return breakdown == null ? 0 : breakdown.valueOf(DamageContributionSource.ATTRIBUTE_ABILITY);
    }

    @Test
    void subjugarFavoursAgarrarAndTheEncontraoButNoOtherManoeuvre() {
        StrengthAbility subjugar = StrengthAbility.SUBJUGAR;

        assertEquals(Skill.ADVANTAGE_BONUS, subjugar.resolveManoeuvreRollBonus(Manoeuvre.AGARRAR, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(Skill.ADVANTAGE_BONUS, subjugar.resolveManoeuvreRollBonus(Manoeuvre.ENCONTRAO, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(0, subjugar.resolveManoeuvreRollBonus(Manoeuvre.INVESTIDA, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(0, subjugar.resolveManoeuvreRollBonus(null, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(0, subjugar.resolveManoeuvreRollBonus(Manoeuvre.AGARRAR, SkillType.ESQUIVA_E_APARAR));
    }

    @Test
    void subjugarGivesVantagemEmDanoOnlyAgainstACaidoOrDesprevenidoTarget() {
        CharacterSheet attacker = sheet(StrengthAbility.SUBJUGAR);
        CharacterSheet standing = sheet();
        CharacterSheet fallen = sheet();
        fallen.applyCondition(new Condition(ConditionType.CAIDO, null));

        assertEquals(0, attributeDamage(punch(attacker, standing)));
        assertEquals(Skill.ADVANTAGE_BONUS, attributeDamage(punch(attacker, fallen)));
        assertEquals(0, attributeDamage(punch(sheet(), fallen)), "only for its holder");
    }

    @Test
    void estilhacadorFavoursAForcaAttackOnAnObjectAndItsDamage() {
        StrengthAbility estilhacador = StrengthAbility.ESTILHACADOR;

        assertEquals(Skill.ADVANTAGE_BONUS, estilhacador.resolveManoeuvreRollBonus(Manoeuvre.ATACAR_OBJETOS,
                SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH));
        assertEquals(0, estilhacador.resolveManoeuvreRollBonus(Manoeuvre.ATACAR_OBJETOS,
                SkillType.ATAQUE_A_DISTANCIA, AttributeDomain.DEXTERITY));
        assertEquals(0, estilhacador.resolveManoeuvreRollBonus(Manoeuvre.ENCONTRAO,
                SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH));
        assertEquals(Skill.ADVANTAGE_BONUS, estilhacador.resolveObjectDamageBonus(AttributeDomain.STRENGTH));
        assertEquals(0, estilhacador.resolveObjectDamageBonus(AttributeDomain.DEXTERITY));
    }

    @Test
    void estilhacadorReportsTwoPointsOnAMeleeHit() {
        assertEquals(ShieldBreak.ESTILHACADOR_DAMAGE,
                punch(sheet(StrengthAbility.ESTILHACADOR), sheet()).getDefensiveEquipmentDamage());
        assertEquals(0, punch(sheet(), sheet()).getDefensiveEquipmentDamage());
    }

    @Test
    void theDamageFallsOnTheEscudoThenTheArmaduraOnceItIsDestroyed() {
        CharacterSheet target = sheet();
        Item shield = ItemForgery.purchased(ItemSpecification.of(ShieldItem.values()[0])).forge();
        Item armour = ItemForgery.purchased(ItemSpecification.of(ArmorItem.values()[0])).forge();
        target.getCharacter().getEquipment().add(armour);
        target.getCharacter().getEquipment().add(shield);

        assertSame(shield, ShieldBreak.against(target, 2).item());
        shield.applyDamage(shield.getEffectiveHardness() + 10);
        assertSame(armour, ShieldBreak.against(target, 2).item());
        assertNull(ShieldBreak.against(sheet(), 2), "nothing worn, nothing struck");
    }
}
