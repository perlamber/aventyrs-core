package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.SpellHealingEffect;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RecoveryOrigin;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The healing riders of {@link VigorAbility#METABOLISMO_RAPIDO} (+2PV) and {@link
 * FocusAbility#CANALIZADOR_DE_MANA} (+1PM) on a heal from a Magia or a Habilidade de Título — and
 * the table ruling (2026-10-16) that a Magia healing "como se passasse por um Descanso" pays the +2,
 * never the Descanso's own +3.
 */
class HealingRiderAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetWith(final AttributeAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .lifeMultiplier(200)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(3).build())
                        .build());
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static void cast(final CharacterSheet target, final Spell spell) {
        target.receiveInteraction(new SpellHealingEffect(spell, spell.getHealing().orElseThrow()));
    }

    private static int longRestEquivalent(final CharacterSheet sheet) {
        return new RestServiceImpl().getRecoveredHitPoints(sheet.getCharacter(), RestType.LONGO,
                RecoveryOrigin.EQUIVALENTE);
    }

    @Test
    void metabolismoAddsTwoToAMagiasHealButNotTheDescansosThree() {
        CharacterSheet plain = sheetWith();
        CharacterSheet fast = sheetWith(VigorAbility.METABOLISMO_RAPIDO);
        int base = longRestEquivalent(plain);
        plain.applyDamage(base + 10);
        fast.applyDamage(base + 10);

        cast(plain, VidaSpell.REVIGORAR);
        cast(fast, VidaSpell.REVIGORAR);

        assertEquals(10, plain.getDamageTaken());
        assertEquals(10 - 2, fast.getDamageTaken());
    }

    @Test
    void metabolismoAddsNothingToAnEgoSpendOrRouboDeVida() {
        CharacterSheet fast = sheetWith(VigorAbility.METABOLISMO_RAPIDO);
        fast.applyDamage(20);

        fast.heal(3, HealingSource.egoSpend(null, fast));
        fast.healFromLifeSteal(3);

        assertEquals(14, fast.getDamageTaken());
    }

    @Test
    void metabolismoStillHealsAtMostOnePvInComa() {
        CharacterSheet fast = sheetWith(VigorAbility.METABOLISMO_RAPIDO);
        int max = new HitPointsServiceImpl().getMaxHitPoints(fast.getCharacter(), fast);
        int comaDamage = 2 * max - 1;
        fast.applyDamage(comaDamage);
        assertEquals(CharacterStatus.COMMA, new HitPointsServiceImpl().getStatus(fast));

        fast.heal(10, HealingSource.spell(VidaSpell.REVIGORAR, null));

        assertEquals(comaDamage - CombatantSheet.COMA_HEAL_CAP, fast.getDamageTaken());
    }

    @Test
    void canalizadorRestoresOnePmWhenAMagiasHealRestoresPv() {
        CharacterSheet channel = sheetWith(FocusAbility.CANALIZADOR_DE_MANA);
        channel.applyDamage(5);
        channel.spendMagicPoints(3);

        channel.heal(2, HealingSource.spell(VidaSpell.REVIGORAR, null));

        assertEquals(3, channel.getDamageTaken());
        assertEquals(2, channel.getManaSpent());
    }

    @Test
    void canalizadorRestoresNothingWhenNoPvCameBackOrTheHealIsNotAMagia() {
        CharacterSheet channel = sheetWith(FocusAbility.CANALIZADOR_DE_MANA);
        channel.spendMagicPoints(3);

        channel.heal(2, HealingSource.spell(VidaSpell.REVIGORAR, null));   // undamaged
        channel.applyDamage(5);
        channel.heal(2, HealingSource.rest(RestType.CURTO));               // a Descanso, not a Magia

        assertEquals(3, channel.getManaSpent());
    }
}
