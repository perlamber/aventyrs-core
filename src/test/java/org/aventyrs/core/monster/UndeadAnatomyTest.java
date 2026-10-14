package org.aventyrs.core.monster;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AsphyxiaServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.magic.catalog.ProfanarSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.monster.summon.Zumbi;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Anatomia de Morto-Vivo (core 0.1.6.23), on the spell-raised Zumbi — Vigor 2, so Metade do Vigor is 1. */
class UndeadAnatomyTest {

    private MonsterSheet zumbi;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        zumbi = Zumbi.builder().build().spawn(new Player());
    }

    @Test
    void itsSixEfeitosCriticosAreRefused() {
        assertTrue(zumbi.getCriticalEffectImmunities().containsAll(UndeadAnatomy.CRITICAL_EFFECT_IMMUNITIES));
        assertTrue(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.ATORDOANTE));
        assertTrue(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.OFERENDA_MALDITA));
        assertFalse(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.AMALDICOAR),
                "the printed Menor's list is gone");
    }

    @Test
    void doencasMaldicoesEVenenosDoNotTakeHold() {
        assertTrue(zumbi.isImmuneToCondition(ConditionType.DOENTE));
        assertTrue(zumbi.isImmuneToCondition(ConditionType.AMALDICOADO));
        assertTrue(zumbi.isImmuneToCondition(ConditionType.ENVENENADO));
        assertFalse(zumbi.isImmuneToCondition(ConditionType.CAIDO));
    }

    @Test
    void correntesNeedTwoMore() {
        EffectChainService chains = new EffectChainServiceImpl();
        MonsterSheet living = SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player());

        assertEquals(chains.getRequiredMargin(null, null, living, null) + UndeadAnatomy.EFFECT_CHAIN_RESISTANCE,
                chains.getRequiredMargin(null, null, zumbi, null));
    }

    @Test
    void theDeadDoNotBreathe() {
        new AsphyxiaServiceImpl().stopBreathing(zumbi);
        assertTrue(zumbi.getAsphyxia().isEmpty());
        assertTrue(zumbi.isImmuneToDamage(DamageType.ASFIXIA, null));
    }

    @Test
    void aProfaneMagiaHealsInsteadOfHurting() {
        zumbi.applyDamage(10);

        InteractionResult result = new DamageInteraction(new DamageServiceImpl())
                .fromSpell(ProfanarSpell.LACERAR_A_ALMA)
                .withDiceRoller(() -> 4)
                .applyTo(zumbi, null, DamageType.MAGICO, null, 8, false, null);

        assertEquals(0, result.getResourceLossValue());
        assertEquals(4 + 1, result.getResourceGainValue(), "1d6 + Metade do Vigor");
        assertEquals(10 - 5, zumbi.getDamageTaken());
    }

    @Test
    void aProfaneHitWithNoMagiaHealsToo() {
        zumbi.applyDamage(6);

        new DamageInteraction(new DamageServiceImpl())
                .withSanctity(DamageSanctity.PROFANO)
                .withDiceRoller(() -> 3)
                .applyTo(zumbi, null, DamageType.FISICO, null, 8, false, null);

        assertEquals(6 - 4, zumbi.getDamageTaken());
    }

    @Test
    void aCurativeHealWithDiceDealsTwoD6PlusHalfVigorSagrado() {
        zumbi.heal(5, HealingSource.spell(VidaSpell.REVIGORAR, null).withDice(DiceRoller.fixed(3, 4)));
        assertEquals(3 + 4 + 1, zumbi.getDamageTaken());
    }

    @Test
    void withoutDiceTheHealItselfStandsIn() {
        zumbi.heal(5, HealingSource.spell(VidaSpell.REVIGORAR, null));
        assertEquals(5, zumbi.getDamageTaken());
    }

    @Test
    void aDescansoAndAnUndeadHealerStillHeal() {
        zumbi.applyDamage(8);
        zumbi.heal(3, HealingSource.rest(RestType.CURTO));
        assertEquals(5, zumbi.getDamageTaken());

        MonsterSheet otherDead = Zumbi.builder().build().spawn(new Player());
        zumbi.heal(2, HealingSource.spell(VidaSpell.REVIGORAR, otherDead));
        assertEquals(3, zumbi.getDamageTaken(), "the dead may heal the dead");
    }

    @Test
    void theAnatomyTravelsToTheSheetAClientActsThrough() {
        org.aventyrs.core.sheet.CharacterSheet standIn =
                org.aventyrs.core.sheet.CharacterSheet.of(zumbi.getCharacter(), new Player());
        assertFalse(standIn.hasUndeadAnatomy());

        zumbi.transferAnatomyTo(standIn);

        assertTrue(standIn.hasUndeadAnatomy());
        assertTrue(standIn.getCriticalEffectImmunities().containsAll(UndeadAnatomy.CRITICAL_EFFECT_IMMUNITIES));
        assertTrue(standIn.isImmuneToCondition(ConditionType.DOENTE));
        standIn.heal(5, HealingSource.spell(VidaSpell.REVIGORAR, null).withDice(DiceRoller.fixed(1, 1)));
        assertEquals(1 + 1 + 1, standIn.getDamageTaken(), "the curative heal still burns it");

        MonsterSheet ghost = org.aventyrs.core.monster.summon.Abantesma.summonedBy(0).spawn(new Player());
        org.aventyrs.core.sheet.CharacterSheet ghostStandIn =
                org.aventyrs.core.sheet.CharacterSheet.of(ghost.getCharacter(), new Player());
        ghost.transferAnatomyTo(ghostStandIn);
        assertTrue(ghostStandIn.isDamageShortensDuration());
        assertTrue(ghostStandIn.isImmuneToDamage(DamageType.FISICO, null), "incorpóreo");
    }

    @Test
    void theLivingAreUntouched() {
        MonsterSheet living = SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player());
        assertFalse(living.hasUndeadAnatomy());
        assertFalse(living.isImmuneToDamage(DamageType.FISICO,
                new DamageDescriptor(DamageType.FISICO, null, DamageSanctity.PROFANO)));
    }
}
