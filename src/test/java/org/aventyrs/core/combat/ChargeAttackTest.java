package org.aventyrs.core.combat;

import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ChargeService;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an Investida is worth once it reaches a real roll — driven through the same {@code
 * AttackDelivery} an ordinary attack takes, because that is the whole design: a charge is an
 * ordinary attack carrying a {@link Manoeuvre} marker, not a second pipeline.
 *
 * <p>Asserted as a <b>difference</b> against the identical roll without the marker rather than
 * against an absolute figure, so the tests stay true whatever else the attacker happens to
 * contribute to the sum.
 */
class ChargeAttackTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet combatant() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .build(), new Player());
    }

    /** A hit: three 4s against a Defesa of 5 clears it without being a critical either way. */
    private static SkillRoll roll(final Manoeuvre manoeuvre) {
        return new SkillRoll(List.of(4, 4, 4), null, null, null, manoeuvre);
    }

    private DeliveredAttackResult melee(final CharacterSheet attacker, final Manoeuvre manoeuvre) {
        return attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(attacker)
                .defender(combatant())
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(roll(manoeuvre))
                .defenseValue(5)
                .defenseType(DefenseType.PHYSICAL)
                .build());
    }

    private static int damageBonusValue(final DeliveredAttackResult result) {
        return result.getAttackResult().getDamageBonus() == null
                ? 0
                : result.getAttackResult().getDamageBonus().getValue();
    }

    // ---------- the dano bonus ----------

    /**
     * "Se acertar, seus danos aumentam em +2" — {@code ChargeService#HIT_DAMAGE_BONUS}, numerically
     * the flat {@code Skill#ADVANTAGE_BONUS} a Vantagem is worth on any dano roll.
     */
    @Test
    void anInvestidaAddsTwoToTheDanoRoll() {
        CharacterSheet attacker = combatant();

        int ordinary = damageBonusValue(melee(attacker, null));
        int charging = damageBonusValue(melee(attacker, Manoeuvre.INVESTIDA));

        assertEquals(ordinary + ChargeService.HIT_DAMAGE_BONUS, charging);
        assertEquals(2, ChargeService.HIT_DAMAGE_BONUS);
    }

    /**
     * The bonus needs no "did it hit" test of its own. A {@code DamageBonus} only ever reaches a
     * dano roll, and {@code AttackDelivery} only builds the chain that carries one inside its own
     * hit branch — so a landed charge has a chain to spend it on and a missed one has none, with
     * no conditional in the bonus maths.
     */
    @Test
    void aMissedInvestidaBuildsNoChainToSpendTheBonusOn() {
        CharacterSheet attacker = combatant();

        DeliveredAttackResult landed = melee(attacker, Manoeuvre.INVESTIDA);
        DeliveredAttackResult missed = attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(attacker)
                .defender(combatant())
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(roll(Manoeuvre.INVESTIDA))
                .defenseValue(40)
                .defenseType(DefenseType.PHYSICAL)
                .build());

        assertTrue(landed.getHit());
        assertNotNull(landed.getAttackResult().getNextInteraction());
        assertTrue(!missed.getHit());
        org.junit.jupiter.api.Assertions.assertNull(missed.getAttackResult().getNextInteraction());
    }

    /** An Investida is a melee manoeuvre — the marker on a ranged roll is worth nothing. */
    @Test
    void theMarkerIsWorthNothingOnARangedAttack() {
        CharacterSheet attacker = combatant();

        InteractionResult ordinary = new AtaqueADistanciaInteraction()
                .applyTo(attacker, null, roll(null));
        InteractionResult charging = new AtaqueADistanciaInteraction()
                .applyTo(attacker, null, roll(Manoeuvre.INVESTIDA));

        assertEquals(ordinary.getDamageBonus(), charging.getDamageBonus());
    }

    /** And an ordinary melee attack picks up nothing from the manoeuvre it isn't. */
    @Test
    void anOrdinaryMeleeAttackGetsNoChargeBonus() {
        CharacterSheet attacker = combatant();

        InteractionResult noRoll = new AtaqueCorpoACorpoInteraction().applyTo(attacker, null, null);
        InteractionResult plain = new AtaqueCorpoACorpoInteraction().applyTo(attacker, null, roll(null));

        assertEquals(noRoll.getDamageBonus(), plain.getDamageBonus());
    }

    // ---------- IMPLACAVEL's Vantagem ----------

    /**
     * {@code DexterityAbility#IMPLACAVEL}'s "Vantagem em suas jogadas de Ataque Corpo-a-Corpo",
     * scoped to its holder's Investidas: the same character rolling an ordinary melee attack gets
     * nothing from it, which is what keeps a manoeuvre clause from becoming a permanent one.
     */
    @Test
    void implacavelGrantsItsVantagemOnlyOnACharge() {
        Character relentless = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributeAbility(DexterityAbility.IMPLACAVEL)
                .build();
        CharacterSheet attacker = CharacterSheet.of(relentless, new Player());

        int ordinary = new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, null, roll(null)).getSkillRollBonus();
        int charging = new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, null, roll(Manoeuvre.INVESTIDA)).getSkillRollBonus();

        assertEquals(ordinary + Skill.ADVANTAGE_BONUS, charging);
    }

    /** A character without the Habilidade rolls a charge at exactly their ordinary bonus. */
    @Test
    void anOrdinaryChargerRollsAtTheirUsualBonus() {
        CharacterSheet attacker = combatant();

        assertEquals(melee(attacker, null).getAttackTotal(),
                melee(attacker, Manoeuvre.INVESTIDA).getAttackTotal());
    }
}
