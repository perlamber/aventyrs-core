package org.aventyrs.core.magic;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.Amenizar;
import org.aventyrs.core.effect.Sobrecura;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Judging a cast (core 0.0.93): "Pessoal ou Toque", the DM floor, the Corrente margin and the casting critical. */
class SpellCastJudgementTest {

    private final SpellCastingService service = new SpellCastingServiceImpl();
    private CharacterSheet caster;
    private CharacterSheet ally;
    private MonsterSheet foe;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        caster = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        ally = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        foe = GenericMonster.CAPANGA.spawn(new Player());
        scene = new Scene();
        UUID party = UUID.randomUUID();
        scene.addParticipant(caster, 10, party);
        scene.addParticipant(ally, 9, party);
        scene.addParticipant(foe, 8, UUID.randomUUID());
    }

    private SpellCastingResult cast(final Spell spell, final CombatantSheet target, final boolean alternate) {
        return service.castSpell(SpellCastRequest.builder()
                .caster(caster).spell(spell).scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of()))
                .combatantTarget(target)
                .useAlternateVersion(alternate)
                .build());
    }

    /** "Alcance: Pessoal ou Toque" — on oneself with no target, or on whoever is touched. */
    @Test
    void aPessoalOuToqueMagiaIsCastEitherWay() {
        SpellCastingResult self = cast(VidaSpell.REVIGORAR, null, false);
        SpellCastingResult touched = cast(VidaSpell.REVIGORAR, ally, false);

        assertSame(VidaSpell.REVIGORAR, self.getCastVersion());
        assertTrue(touched.getSpellEffect() != null);
    }

    /** Revigorar: "Médio (16|18) ou DM do alvo (Maior)" — a foe's DM raises the bar; the caster's own never does. */
    @Test
    void theDmFloorRaisesTheTargetValueAgainstAnotherCombatant() {
        int medium = DifficultyLevel.MEDIUM.getBaseValue();
        int foeDm = foe.getDefense(DefenseType.MAGIC);

        assertEquals(Math.max(medium, foeDm), cast(VidaSpell.REVIGORAR, foe, false).getCastingTargetValue());
        assertEquals(medium, cast(VidaSpell.REVIGORAR, null, false).getCastingTargetValue());
        // Benção Bifurcada: "O GD da Conjuração muda pra Médio" — its floor is lifted.
        assertEquals(medium, cast(VidaSpell.REVIGORAR, foe, true).getCastingTargetValue());
    }

    @Test
    void aCastSucceedsOnATieAndItsCorrenteNeedsTheMargin() {
        SpellCastingResult result = cast(VidaSpell.REVIGORAR, ally, false);
        int target = result.getCastingTargetValue();

        assertTrue(service.castSucceeds(result, target));
        assertFalse(service.castSucceeds(result, target - 1));
        assertFalse(service.isEffectChainTriggered(result, ally, target + 4));
        assertTrue(service.isEffectChainTriggered(result, ally, target + 5));
    }

    /** Sobrecura rides Revigorar and Benção Bifurcada; Fonte da Juventude does not inherit Nova Rejuvenescedora's. */
    @Test
    void theCastVersionNamesItsOwnCorrente() {
        Spell bencao = VidaSpell.REVIGORAR.getAlternateVersion().orElseThrow();
        Spell fonte = VidaSpell.NOVA_REJUVENESCEDORA.getAlternateVersion().orElseThrow();

        Sobrecura sobrecura = assertInstanceOf(Sobrecura.class,
                service.resolveEffectChain(VidaSpell.REVIGORAR, caster, DiceRoller.fixed(4)).orElseThrow());
        assertEquals(4, sobrecura.getRolledDice());
        assertTrue(service.resolveEffectChain(bencao, caster, DiceRoller.fixed(4)).isPresent());
        assertTrue(service.resolveEffectChain(fonte, caster, DiceRoller.fixed(4)).isEmpty());
        assertTrue(service.resolveEffectChain(VidaSpell.ALIVIAR_A_DOR, caster, DiceRoller.fixed(4)).isEmpty());
    }

    /** A critical Conjuração puts the Magia's Efeito Crítico — Revigorar's Amenizar — on its target. */
    @Test
    void aCriticalConjuracaoBuildsTheMagiasEfeitoCritico() {
        assertInstanceOf(Amenizar.class, service.resolveCastingCriticalEffect(VidaSpell.REVIGORAR, caster,
                new SkillRoll(List.of(6, 6, 6)), DiceRoller.fixed(3)).orElseThrow());
        assertTrue(service.resolveCastingCriticalEffect(VidaSpell.REVIGORAR, caster,
                new SkillRoll(List.of(3, 3, 3)), DiceRoller.fixed(3)).isEmpty());
    }
}
