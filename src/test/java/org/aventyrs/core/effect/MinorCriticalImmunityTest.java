package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>severity</b>-keyed half of {@link CriticalEffect#applicableTo} — "Efeitos Críticos
 * Menores", which drops a whole chain at once, as opposed to the type-keyed half (a Zumbi shrugging
 * off Sangramento specifically) that {@code CriticalEffectImmunityTest} covers.
 *
 * <p>Two clauses reach it: {@code MonstruosoFeat#ANATOMIA_UNICA}, unconditional, and Santo's
 * Despertar, for the first Rodadas of a Cena de Combate only.
 */
class MinorCriticalImmunityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private List<CriticalEffect> oneEffect() {
        return List.of(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR));
    }

    private CharacterSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private CharacterSheet anatomiaUnicaSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(MonstruosoFeat.ANATOMIA_UNICA)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** A Santo whose Especialização + Suprema give Despertar a 2-Rodada window. */
    private CharacterSheet santoSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

    /**
     * A combat SceneContext sitting in round. The cursor is placed directly rather than walked
     * there with {@code next()}: this test cares only about which Rodada it is, and replaying turn
     * lifecycle would tick every Round-scoped effect along the way for no reason.
     */
    private SceneContext combatRound(final CombatantSheet holder, final int round) {
        Scene scene = new Scene();
        scene.setCombatScene(true);
        CombatantSheet foe = plainSheet();
        scene.addParticipant(holder, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());
        scene.restoreTurnCursor(round, 0);
        return scene.buildContext(holder, Map.of(foe, Range.ADJACENTE));
    }

    // --- CriticalResult#isMinor ---------------------------------------------------------------

    @Test
    void severityCrossesSuccessAndFailure() {
        assertTrue(CriticalResult.ACERTO_CRITICO_MENOR.isMinor());
        assertTrue(CriticalResult.FALHA_CRITICA_MENOR.isMinor());
        assertFalse(CriticalResult.ACERTO_CRITICO_MAIOR.isMinor());
        assertFalse(CriticalResult.FALHA_CRITICA_MAIOR.isMinor());
    }

    // --- The unconditional source: ANATOMIA_UNICA ---------------------------------------------

    @Test
    void anatomiaUnicaDropsEveryEffectOfAMinorCritical() {
        CombatantSheet immune = anatomiaUnicaSheet();

        assertEquals(List.of(), CriticalEffect.applicableTo(
                immune, oneEffect(), CriticalResult.ACERTO_CRITICO_MENOR, null));
    }

    /** It is severity-keyed, so a Maior critical still lands in full. */
    @Test
    void anatomiaUnicaDoesNotTouchAMaiorCritical() {
        CombatantSheet immune = anatomiaUnicaSheet();
        List<CriticalEffect> effects = oneEffect();

        assertEquals(effects, CriticalEffect.applicableTo(
                immune, effects, CriticalResult.ACERTO_CRITICO_MAIOR, null));
    }

    /** Unconditional means no Scene is needed — a null context does not withhold it. */
    @Test
    void anatomiaUnicaNeedsNoScene() {
        assertTrue(anatomiaUnicaSheet().ignoresMinorCriticalEffects(null));
    }

    /** Its prerequisite is not the same clause: ANATOMIA_INCOMUM needs a per-Cena counter. */
    @Test
    void anatomiaIncomumDoesNotGrantTheImmunity() {
        assertFalse(MonstruosoFeat.ANATOMIA_INCOMUM.ignoresMinorCriticalEffects());
        assertTrue(MonstruosoFeat.ANATOMIA_UNICA.ignoresMinorCriticalEffects());
    }

    @Test
    void aPlainTargetKeepsEveryEffect() {
        List<CriticalEffect> effects = oneEffect();

        assertEquals(effects, CriticalEffect.applicableTo(
                plainSheet(), effects, CriticalResult.ACERTO_CRITICO_MENOR, null));
    }

    // --- The windowed source: Santo's Despertar -----------------------------------------------

    /** "Nas primeiras Rodadas de cada Cena de Combate", 1 Rodada per Especialização and Suprema. */
    @Test
    void despertarIgnoresMinorCriticalsInsideItsWindow() {
        CombatantSheet santo = santoSheet();

        assertTrue(santo.ignoresMinorCriticalEffects(combatRound(santo, 1)));
        assertTrue(santo.ignoresMinorCriticalEffects(combatRound(santo, 2)));
    }

    @Test
    void despertarStopsIgnoringOnceTheWindowHasPassed() {
        CombatantSheet santo = santoSheet();

        assertFalse(santo.ignoresMinorCriticalEffects(combatRound(santo, 3)));
    }

    /**
     * A Santo holding no Especialização or Suprema has a 0-Rodada window and ignores nothing — the
     * clause read literally, not a special case.
     */
    @Test
    void aBareSantoIgnoresNothing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CombatantSheet bareSanto = CharacterSheet.of(character, new Player());

        assertFalse(bareSanto.ignoresMinorCriticalEffects(combatRound(bareSanto, 1)));
    }

    /** A Rodada-windowed immunity cannot confirm it applies without a Scene, so it withholds. */
    @Test
    void despertarWithholdsWithoutASceneContext() {
        assertFalse(santoSheet().ignoresMinorCriticalEffects(null));
    }

    // --- The filter's own contract ------------------------------------------------------------

    /** A caller that names no severity gets the type-only filter — never "assume Menor". */
    @Test
    void aNullCriticalResultAppliesNoSeverityRule() {
        List<CriticalEffect> effects = oneEffect();

        assertEquals(effects, CriticalEffect.applicableTo(anatomiaUnicaSheet(), effects, null, null));
    }

    /** The two-argument form still behaves exactly as it did, delegating with nulls. */
    @Test
    void theShortFormIsUnchanged() {
        List<CriticalEffect> effects = oneEffect();

        assertEquals(effects, CriticalEffect.applicableTo(anatomiaUnicaSheet(), effects));
        assertEquals(List.of(), CriticalEffect.applicableTo(anatomiaUnicaSheet(), List.of()));
    }

    // --- End to end, through the real attack orchestrator --------------------------------------

    /**
     * Despertar in a real exchange: an Acerto Crítico Menor lands on a Santo inside its window and
     * its Efeito Crítico never reaches the chain. The attack still crits — ignoring an Efeito
     * Crítico shortens the chain, it does not un-crit the attack, exactly as a type immunity
     * doesn't.
     */
    @Test
    void despertarDropsAMinorCriticalsEffectFromARealDeliveredAttack() {
        CombatantSheet santo = santoSheet();
        SceneContext insideWindow = combatRound(santo, 1);
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR);

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(plainSheet())
                .defender(santo)
                .defenseValue(5)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                // 6+6+5 = 17 reaches the default Margem Crítica Menor without being three 6s.
                .attackRoll(new SkillRoll(List.of(6, 6, 5)))
                .sceneContext(insideWindow)
                .criticalEffect(bleed)
                .build());

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
        assertTrue(result.getCriticalEffectTriggered());
        assertEquals(List.of(), chainBehind(result.getAttackResult().getNextInteraction()));
    }

    /** Past the window, the same attack lands its Efeito Crítico normally. */
    @Test
    void theSameAttackLandsOnceDespertarsWindowHasPassed() {
        CombatantSheet santo = santoSheet();
        SceneContext pastWindow = combatRound(santo, 3);
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR);

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(plainSheet())
                .defender(santo)
                .defenseValue(5)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 5)))
                .sceneContext(pastWindow)
                .criticalEffect(bleed)
                .build());

        assertEquals(List.of(bleed), chainBehind(result.getAttackResult().getNextInteraction()));
    }

    /** A Maior critical is untouched even inside the window — the clause names Menores only. */
    @Test
    void aMaiorCriticalLandsOnTheSantoInsideTheWindow() {
        CombatantSheet santo = santoSheet();
        SceneContext insideWindow = combatRound(santo, 1);
        CriticalEffect bleed = new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR);

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(plainSheet())
                .defender(santo)
                .defenseValue(5)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .sceneContext(insideWindow)
                .criticalEffect(bleed)
                .build());

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
        assertEquals(List.of(bleed), chainBehind(result.getAttackResult().getNextInteraction()));
    }

    private List<Interaction<CombatantSheet>> chainBehind(final Interaction<CombatantSheet> head) {
        List<Interaction<CombatantSheet>> stages = new ArrayList<>();
        Interaction<CombatantSheet> next = assertInstanceOf(DamageInteraction.class, head).getNextInteraction();
        while (next != null) {
            stages.add(next);
            next = next instanceof AbstractEffect effect ? effect.getNextInteraction() : null;
        }
        return stages;
    }
}
