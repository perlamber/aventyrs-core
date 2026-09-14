package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellHealing;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vida's principal branch, rung by rung — the point being that one class serves all five, and
 * only the authored {@link SpellHealing} differs.
 */
class SpellHealingEffectTest {

    private final RestService restService = new RestServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    /** What a Descanso of this tier would restore to this character — the figure the Magia copies. */
    private int restAmount(final CharacterSheet sheet, final RestType restType) {
        return restService.getRecoveredHitPoints(sheet.getCharacter(), restType);
    }

    private InteractionResult cast(final CharacterSheet target, final Spell spell) {
        return target.receiveInteraction(new SpellHealingEffect(spell, spell.getHealing().orElseThrow()));
    }

    @Test
    void revigorarHealsExactlyWhatALongRestWouldWithoutBeingOne() {
        CharacterSheet sheet = newSheet();
        int longRest = restAmount(sheet, RestType.LONGO);
        sheet.applyDamage(longRest + 5);

        InteractionResult result = cast(sheet, VidaSpell.REVIGORAR);

        assertEquals(5, sheet.getDamageTaken());
        assertEquals(longRest, result.getResourceGainValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceGainType());
        // "não substitui Descansos reais" — PM are untouched, which applyRest would have restored.
        assertEquals(0, sheet.getManaSpent());
    }

    @Test
    void revigorarMaiorHealsAsATotalRestAndSoOutdoesRevigorar() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(100);

        int longRest = restAmount(sheet, RestType.LONGO);
        int totalRest = restAmount(sheet, RestType.TOTAL);
        cast(sheet, VidaSpell.REVIGORAR_MAIOR);

        assertEquals(totalRest, 100 - sheet.getDamageTaken());
        assertTrue(totalRest > longRest, "Total should outheal Longo — the branch deepens");
    }

    @Test
    void bencaoDaLuzRestoresEveryLostHitPoint() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(37);

        InteractionResult result = cast(sheet, VidaSpell.BENCAO_DA_LUZ);

        assertEquals(0, sheet.getDamageTaken());
        assertEquals(37, result.getResourceGainValue());
    }

    @Test
    void novaRejuvenescedoraGivesAHostileTargetHalf() {
        CharacterSheet ally = newSheet();
        CharacterSheet enemy = newSheet();
        int longRest = restAmount(ally, RestType.LONGO);
        ally.applyDamage(longRest);
        enemy.applyDamage(longRest);

        SpellHealing healing = VidaSpell.NOVA_REJUVENESCEDORA.getHealing().orElseThrow();
        ally.receiveInteraction(
                new SpellHealingEffect(VidaSpell.NOVA_REJUVENESCEDORA, healing, false));
        enemy.receiveInteraction(
                new SpellHealingEffect(VidaSpell.NOVA_REJUVENESCEDORA, healing, true));

        assertEquals(0, ally.getDamageTaken());
        assertEquals(longRest - longRest / 2, enemy.getDamageTaken());
    }

    @Test
    void aHostileTargetIsOnlyHalvedByAMagiaThatSaysSo() {
        CharacterSheet sheet = newSheet();
        int longRest = restAmount(sheet, RestType.LONGO);
        sheet.applyDamage(longRest);

        SpellHealing healing = VidaSpell.REVIGORAR.getHealing().orElseThrow();
        sheet.receiveInteraction(new SpellHealingEffect(VidaSpell.REVIGORAR, healing, true));

        assertEquals(0, sheet.getDamageTaken());
    }

    @Test
    void aliviarADorHealsATargetThatIsNotBleeding() {
        CharacterSheet sheet = newSheet();
        int minimumRest = restAmount(sheet, RestType.MINIMO);
        sheet.applyDamage(minimumRest);

        InteractionResult result = cast(sheet, VidaSpell.ALIVIAR_A_DOR);

        assertEquals(0, sheet.getDamageTaken());
        assertEquals(minimumRest, result.getResourceGainValue());
    }

    @Test
    void aliviarADorStopsABleedInsteadOfHealingIt() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(10);
        sheet.applyEffect(new Bleeding(2, Optional.of(5)));

        InteractionResult result = cast(sheet, VidaSpell.ALIVIAR_A_DOR);
        sheet.tickTemporaryEffects();

        // "ao invés disso" — the bleeding stopped, and no PV came back.
        assertEquals(10, sheet.getDamageTaken());
        assertNull(result.getResourceGainValue());
    }

    @Test
    void feridasDolorosasRefusesTheRecoveryOutright() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(20);
        sheet.applyCondition(new Condition(ConditionType.FERIDAS_DOLOROSAS, 3));

        InteractionResult result = cast(sheet, VidaSpell.BENCAO_DA_LUZ);

        assertEquals(20, sheet.getDamageTaken());
        assertEquals(0, result.getResourceGainValue(), "reports what landed, not what was offered");
    }

    @Test
    void anUndamagedTargetIsHealedForNothingRatherThanOverhealed() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = cast(sheet, VidaSpell.REVIGORAR);

        assertEquals(0, sheet.getDamageTaken());
        assertEquals(0, result.getResourceGainValue());
    }

    @Test
    void healingInterruptsAnOngoingBleed() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(50);
        sheet.applyEffect(new Bleeding(2, Optional.of(5)));

        cast(sheet, VidaSpell.REVIGORAR);
        int afterHeal = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(afterHeal, sheet.getDamageTaken());
    }

    @Test
    void sobrecuraChainsOntoTheHealAndBothApplyInOnePass() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(100);
        Character caster = newSheet().getCharacter();

        SpellHealing healing = VidaSpell.REVIGORAR.getHealing().orElseThrow();
        Sobrecura sobrecura = new Sobrecura(caster, 4);
        SpellHealingEffect heal = new SpellHealingEffect(VidaSpell.REVIGORAR, healing);
        heal.chainInto(sobrecura);

        InteractionResult result = sheet.receiveInteraction(heal);
        while (result.getNextInteraction() != null) {
            result = sheet.receiveInteraction(result.getNextInteraction());
        }

        int expected = restAmount(sheet, RestType.LONGO) + sobrecura.getRecovery();
        assertEquals(100 - expected, sheet.getDamageTaken());
    }

    @Test
    void theDescriptionIsTheMagiasOwnEfeitoLine() {
        SpellHealingEffect effect = new SpellHealingEffect(VidaSpell.REVIGORAR,
                VidaSpell.REVIGORAR.getHealing().orElseThrow());

        assertEquals(VidaSpell.REVIGORAR.getPrimaryEffectDescription(), effect.getDescription());
    }
}
