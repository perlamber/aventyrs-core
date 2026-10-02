package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.effect.SpellEffectContext;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellCastRequest;
import org.aventyrs.core.magic.SpellCastingResult;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VIDA, Magia by Magia (core 0.0.94) — {@code docs/plans/spell-casting-audit.md}. */
class VidaCastingTest {

    private final SpellCastingService service = new SpellCastingServiceImpl();
    private CharacterSheet caster;
    private CharacterSheet ally;
    private CharacterSheet other;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        caster = sheet();
        ally = sheet();
        other = sheet();
        scene = new Scene();
        UUID party = UUID.randomUUID();
        scene.addParticipant(caster, 10, party);
        scene.addParticipant(ally, 9, party);
        scene.addParticipant(other, 8, party);
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(org.aventyrs.core.character.CharacterAttributes.builder()
                        .vigor(value(org.aventyrs.core.character.AttributeDomain.VIGOR))
                        .focus(value(org.aventyrs.core.character.AttributeDomain.FOCUS))
                        .instinct(value(org.aventyrs.core.character.AttributeDomain.INSTINCT))
                        .build())
                .build(), new Player());
    }

    private static org.aventyrs.core.character.AttributeValue value(final org.aventyrs.core.character.AttributeDomain domain) {
        return org.aventyrs.core.character.AttributeValue.builder().domain(domain).base(4).build();
    }

    private SpellCastRequest.SpellCastRequestBuilder request(final Spell spell, final CombatantSheet target) {
        return SpellCastRequest.builder().caster(caster).spell(spell).scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of())).combatantTarget(target);
    }

    private static void drain(final CombatantSheet target, final SpellEffect effect) {
        InteractionResult result = target.receiveInteraction(effect);
        while (result.getNextInteraction() != null) {
            result = target.receiveInteraction(result.getNextInteraction());
        }
    }

    /** Toque Curativo: "Efeitos mundanos ou de magias Sementes: Fácil, Brotos: Médio, Mudas: Difícil …". */
    @Test
    void aRungScaledGdFollowsWhatItRemoves() {
        SpellCastingResult muda = service.castSpell(request(VidaSpell.TOQUE_CURATIVO, ally)
                .opposedBranchLevel(BranchLevel.MUDA).build());
        SpellCastingResult mundane = service.castSpell(request(VidaSpell.TOQUE_CURATIVO, ally)
                .opposedBranchLevel(BranchLevel.SEMENTE).build());

        assertEquals(DifficultyLevel.HARD, muda.getCastingDifficultyLevel());
        assertEquals(DifficultyLevel.EASY, mundane.getCastingDifficultyLevel());
    }

    /** Aliviar a Dor: "só afeta o alvo 1 vez, voltando a afetá-lo somente após ele passar por um Descanso Longo". */
    @Test
    void aliviarADorHealsATargetOnceUntilItsDescansoLongo() {
        SpellEffect effect = service.resolveEffect(VidaSpell.ALIVIAR_A_DOR, SpellEffectContext.of(false, caster))
                .orElseThrow();
        ally.applyDamage(8);
        drain(ally, effect);
        int afterFirst = ally.getDamageTaken();
        drain(ally, service.resolveEffect(VidaSpell.ALIVIAR_A_DOR, SpellEffectContext.of(false, caster)).orElseThrow());

        assertTrue(afterFirst < 8);
        assertEquals(afterFirst, ally.getDamageTaken(), "no second heal before a Descanso Longo");
        new RestServiceImpl().applyRest(ally.getCharacter(), ally, RestType.LONGO);
        assertFalse(ally.isAffectedUntilRest("spell-heal:Aliviar a Dor"));
    }

    /** Benção Bifurcada reaches one more; Corrente Abençoada any number, at +3PM each. */
    @Test
    void additionalTargetsAreCappedAndPriced() {
        SpellCastRequest bifurcada = request(VidaSpell.REVIGORAR, ally).useAlternateVersion(true)
                .additionalTarget(other).build();
        assertEquals(1, service.castSpell(bifurcada).getCastVersion().getMaxAdditionalTargets());
        assertThrows(IllegalOperationException.class, () -> service.castSpell(request(VidaSpell.REVIGORAR, ally)
                .useAlternateVersion(true).additionalTarget(other).additionalTarget(caster).build()));

        int alone = service.resolveManaCost(request(VidaSpell.BENCAO_DA_LUZ, ally).useAlternateVersion(true).build());
        int withTwo = service.resolveManaCost(request(VidaSpell.BENCAO_DA_LUZ, ally).useAlternateVersion(true)
                .additionalTarget(other).additionalTarget(caster).build());
        assertEquals(alone + 6, withTwo);
    }

    /** Cura em Massa "você e todos … à até 2m"; Nova Rejuvenescedora heals its caster too; Fonte da Juventude does not. */
    @Test
    void whichAreasReachTheirCaster() {
        Spell curaEmMassa = VidaSpell.REVIGORAR_MAIOR.getAlternateVersion().orElseThrow();
        Spell fonte = VidaSpell.NOVA_REJUVENESCEDORA.getAlternateVersion().orElseThrow();

        assertTrue(curaEmMassa.getTargeting().isCenteredOnCaster());
        assertTrue(curaEmMassa.isCasterAffected());
        assertTrue(VidaSpell.NOVA_REJUVENESCEDORA.isCasterAffected());
        assertFalse(fonte.isCasterAffected());
    }

    /** Fonte da Juventude: PV, PM and PD as a Descanso Curto; hostiles as a Mínimo. */
    @Test
    void fonteDaJuventudeRestoresEveryPool() {
        Spell fonte = VidaSpell.NOVA_REJUVENESCEDORA.getAlternateVersion().orElseThrow();
        ally.applyDamage(30);
        ally.spendMagicPoints(10);
        ally.spendDeterminationPoints(10);
        int pvMinimo = new RestServiceImpl().getRecoveredHitPoints(other.getCharacter(), RestType.MINIMO);
        other.applyDamage(30);

        drain(ally, service.resolveEffect(fonte, SpellEffectContext.of(false, caster)).orElseThrow());
        drain(other, service.resolveEffect(fonte, SpellEffectContext.of(true, caster)).orElseThrow());

        assertTrue(ally.getDamageTaken() < 30);
        assertTrue(ally.getManaSpent() < 10);
        assertTrue(ally.getDeterminationSpent() < 10);
        assertEquals(Math.max(0, 30 - pvMinimo), other.getDamageTaken());
    }

    /** Corpo Fechado: every Malefício removed, none may land while it holds — Concentração, then 2 Rodadas. */
    @Test
    void corpoFechadoWardsAgainstMaleficiosWhileSustained() {
        ally.applyCondition(new Condition(ConditionType.ASSUSTADO, 3));
        drain(ally, service.resolveEffect(VidaSpell.CORPO_FECHADO, SpellEffectContext.of(false, caster)).orElseThrow());

        assertFalse(ally.hasCondition(ConditionType.ASSUSTADO, null));
        ally.applyCondition(new Condition(ConditionType.CAIDO, 2));
        assertFalse(ally.hasCondition(ConditionType.CAIDO, null));

        scene.breakConcentration(caster);
        ally.tickTemporaryEffects();
        assertTrue(ally.isWardedAgainstMaleficios(), "one of its 2 trailing Rodadas left");
        ally.tickTemporaryEffects();
        assertFalse(ally.isWardedAgainstMaleficios());
    }

    /** Procrastinar Ferimento: the next hit's PV come off a Rodada later; Estancar keeps it from bleeding. */
    @Test
    void procrastinarFerimentoPostponesTheNextHitAndEstancarStopsItsBleeding() {
        Spell procrastinar = VidaSpell.ALIVIAR_A_DOR.getAlternateVersion().orElseThrow();
        SpellEffect ward = service.resolveEffect(procrastinar, SpellEffectContext.of(false, caster)).orElseThrow();
        ((org.aventyrs.core.effect.AbstractEffect) ward)
                .chainInto(service.resolveEffectChain(procrastinar, caster, DiceRoller.fixed(1)).orElseThrow());
        drain(ally, ward);
        CharacterSheet attacker = sheet();

        new DamageInteraction().applyTo(ally, null, DamageType.FISICO, attacker, 6, true);
        ally.applyEffect(new Bleeding(2, java.util.Optional.of(3)));

        assertEquals(0, ally.getDamageTaken());
        assertFalse(ally.stopBleeding(), "Estancar refused the Sangramento");
        ally.startNewRound();
        assertEquals(6, ally.getDamageTaken());
        assertTrue(ally.getPostponedWoundWard().isEmpty(), "spent by the hit");
    }

    @Test
    void sobrecuraIsNotOnTheWardedVersions() {
        assertTrue(service.resolveEffectChain(VidaSpell.CORPO_FECHADO, caster, DiceRoller.fixed(1)).isEmpty());
    }
}
