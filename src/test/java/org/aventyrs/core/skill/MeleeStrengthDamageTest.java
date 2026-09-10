package org.aventyrs.core.skill;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The base Ataque Corpo-a-Corpo rule: a melee swing adds <b>half the attacker's Força, rounded
 * down</b>, to its own dano roll — and the whole value on the Rodada's first attack for a holder
 * of {@link StrengthAbility#DESTRUIDOR_DE_MUROS} ("O primeiro ataque que realizar a cada Rodada
 * utiliza seu valor de Força integral nas rolagens de dano, ao invés da metade").
 *
 * <p>Exercised through {@code AbstractSkillInteraction} rather than in isolation, like every other
 * dano-bonus source: the term is summed with the rest, which only shows end-to-end.
 */
class MeleeStrengthDamageTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet brawler(final int strength, final AttributeAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(strength).build())
                        .build());
        Stream.of(abilities).forEach(builder::attributeAbility);
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static Integer meleeDamage(final CharacterSheet roller) {
        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(roller, null, null);
        return result.getDamageBonus() == null ? null : result.getDamageBonus().getValue();
    }

    /** An attack already taken this Rodada, which is what spends DESTRUIDOR_DE_MUROS's upgrade. */
    private static void recordAnAttack(final CharacterSheet sheet, final SkillType skill) {
        sheet.recordAction(new CombatantAction(skill, AttributeDomain.STRENGTH, null, null, 0, null));
    }

    // ---------- the base half-Força term ----------

    @Test
    void aMeleeSwingAddsHalfTheAttackersForca() {
        assertEquals(3, meleeDamage(brawler(6)));
    }

    /** Rounded down, the same direction every halving in this ruleset takes. */
    @Test
    void theHalfIsFlooredOnAnOddForca() {
        assertEquals(2, meleeDamage(brawler(5)));
    }

    /**
     * Força 0 contributes nothing at all rather than a bonus of 0 — {@code DamageBonus#total}'s
     * own empty-when-there-is-nothing-to-report contract, which this term must not defeat.
     */
    @Test
    void forcaZeroReportsNoBonusAtAll() {
        assertNull(meleeDamage(brawler(0)));
    }

    /** Untyped, so it takes FISICO when nothing else on the roll is typed. */
    @Test
    void theTermIsUntypedAndReadsAsFisico() {
        assertEquals(DamageType.FISICO, new AtaqueCorpoACorpoInteraction()
                .applyTo(brawler(4), null, null).getDamageBonus().getType());
    }

    @Test
    void itSumsWithTheOtherDanoBonusSources() {
        CharacterSheet roller = brawler(6);
        roller.grantTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS, 2, 1);

        assertEquals(5, meleeDamage(roller));
    }

    // ---------- scope ----------

    /**
     * Melee only. This is exactly why the Arco Composto carries a Favor granting "Metade da Força
     * aos Danos Causados" of its own — a redundant Favor if a ranged attack already had the term.
     */
    @Test
    void aRangedAttackAddsNoForca() {
        assertNull(new AtaqueADistanciaInteraction()
                .applyTo(brawler(6), null, null).getDamageBonus());
    }

    @Test
    void aNonAttackPericiaAddsNoForca() {
        assertNull(new AtletismoInteraction().applyTo(brawler(6), null, null).getDamageBonus());
    }

    /**
     * ACUIDADE substitutes Destreza for the <i>roll</i>; the dano term's own rules text names
     * Força, so a finesse swordsman still adds half their Força and not half their Destreza.
     */
    @Test
    void anAtributoSubstitutionDoesNotChangeWhichAtributoTheDanoTermUses() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE)
                .build();

        assertEquals(3, meleeDamage(CharacterSheet.of(character, new Player())), "half Força, not half Destreza");
    }

    // ---------- DESTRUIDOR_DE_MUROS ----------

    @Test
    void destruidorDeMurosUpgradesTheRodadasFirstAttackToFullForca() {
        assertEquals(6, meleeDamage(brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS)));
    }

    @Test
    void withoutTheAbilityTheFirstAttackStillOnlyGetsHalf() {
        assertEquals(3, meleeDamage(brawler(6)));
    }

    @Test
    void theUpgradeIsSpentOnceAnAttackHasBeenRecordedThisRodada() {
        CharacterSheet brawler = brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS);
        recordAnAttack(brawler, SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(3, meleeDamage(brawler), "back to half");
    }

    /**
     * "O primeiro ataque" is any attack, not the first melee one — opening the Rodada with a bow
     * shot spends the upgrade, and the swing that follows adds only half.
     */
    @Test
    void aRangedAttackAlsoSpendsTheUpgrade() {
        CharacterSheet brawler = brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS);
        recordAnAttack(brawler, SkillType.ATAQUE_A_DISTANCIA);

        assertEquals(3, meleeDamage(brawler));
    }

    /** A non-attack Perícia is not an attack, so it leaves the upgrade unspent. */
    @Test
    void aNonAttackRollDoesNotSpendTheUpgrade() {
        CharacterSheet brawler = brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS);
        recordAnAttack(brawler, SkillType.ATLETISMO);

        assertEquals(6, meleeDamage(brawler));
    }

    /** The window is the Rodada, so the next one restores it. */
    @Test
    void theUpgradeReturnsOnTheNextRodada() {
        CharacterSheet brawler = brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS);
        recordAnAttack(brawler, SkillType.ATAQUE_CORPO_A_CORPO);
        assertEquals(3, meleeDamage(brawler));

        brawler.startNewRound();

        assertEquals(6, meleeDamage(brawler));
    }

    /** Reading the roll never writes the log, so asking twice must not spend the upgrade. */
    @Test
    void resolvingTheRollDoesNotItselfSpendTheUpgrade() {
        CharacterSheet brawler = brawler(6, StrengthAbility.DESTRUIDOR_DE_MUROS);

        assertEquals(6, meleeDamage(brawler));
        assertEquals(6, meleeDamage(brawler));
    }

    // ---------- the hook ----------

    @Test
    void onlyDestruidorDeMurosCarriesTheUpgrade() {
        for (StrengthAbility ability : StrengthAbility.values()) {
            boolean expected = ability == StrengthAbility.DESTRUIDOR_DE_MUROS;
            assertEquals(expected, ability.upgradesFirstMeleeAttackOfRoundStrengthScaling(), ability.name());
        }
    }

    /** It is its own hook, not the Magia one — a Destruidor de Muros is no Magia Poderosa. */
    @Test
    void theUpgradeDoesNotLeakIntoTheMagiaHook() {
        assertTrue(StrengthAbility.DESTRUIDOR_DE_MUROS.upgradesFirstMeleeAttackOfRoundStrengthScaling());
        assertFalse(StrengthAbility.DESTRUIDOR_DE_MUROS.upgradesFirstSpellOfRoundFocusScaling());
    }
}
