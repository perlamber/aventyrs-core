package org.aventyrs.core.title.santo;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionOption;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.ReactionOptionsService;
import org.aventyrs.core.character.services.ReactionOptionsServiceImpl;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage granting a real {@link Santo} to a {@link CharacterFixture}-built
 * Character and exercising it through the real scanning service that consumes it
 * ({@link DamageServiceImpl}), mirroring {@code ArtesAprimorarComArteAbilityTest}'s own
 * convention of not calling {@code resolveAbsoluteDamageReduction} directly.
 */
class SantoIntegrationTest {

    private final DamageService damageService = new DamageServiceImpl();
    private final ReactionOptionsService reactionOptionsService = new ReactionOptionsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void grantedTitleRoundTripsThroughCharacter() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo santo = new Santo(List.of(),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        character.grantTitle(santo, TitleSlot.PRIMARY);

        assertEquals(List.of(santo), character.getAllTitles());
        assertEquals(santo, character.getPrimaryTitle());
    }

    @Test
    void ignoreCriticalEffectDurationReflectsTheReallyHeldSpecializationsAndSupremas() {
        Santo santo = new Santo(List.of(),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        // BASTIAO_DOS_NECESSITADOS is a Habilidade (doesn't count), GUARDA_VIDAS is a Suprema
        // (counts) — V19's "1+ número de Especializações e Supremas" is 1 + (0 + 1).
        assertEquals(2, santo.resolveMinorCriticalImmunityRounds());
    }

    @Test
    void damageServiceImplPicksUpBastiaoDosNecessitadosThroughAFullApplyDamageCall() {
        // 3 Habilidades, so Bastião's self-facing RDS is half of "1+ Metade das Habilidades" = 1.
        Character holder = CharacterFixture.blank(CharacterFixture.BLANK).build();
        holder.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS,
                SantoAbility.PROTECAO_UNGIDA, SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());

        Character allyCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet allySheet = CharacterSheet.of(allyCharacter, new Player());
        allySheet.applyDamage(5);

        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        int totalDamageTaken = damageService.applyDamage(holderSheet, sceneContext, 10, false);

        assertEquals(9, totalDamageTaken);
        assertEquals(9, holderSheet.getDamageTaken());
    }

    /**
     * Guarda-Vidas end to end, in the order a client drives it: an ally is named the target of an
     * attack, the Santo is offered the Reação, takes it, and the attack is then <b>built</b> naming
     * the Santo as defender — which is why {@link AttackReceiver} needs to know nothing about any
     * of this and resolves an ordinary attack.
     */
    @Test
    void guardaVidasRedirectsAnAttackFromTheAllyToTheSantoBeforeItIsEverRolled() {
        Character santoCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        santoCharacter.grantTitle(new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS,
                        SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        CharacterSheet santoSheet = CharacterSheet.of(santoCharacter, new Player());
        CharacterSheet allySheet = CharacterSheet.of(
                CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        SceneContext santoContext = new SceneContext(
                List.of(allySheet), List.of(), Map.of(allySheet, Range.DISTANCIA_CURTA));
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(santoCharacter, santoSheet);

        // 1. The foe declares an attack on the ally; the client asks what the Santo can do.
        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(ReactionContext.builder()
                .trigger(ReactionTrigger.ALLY_TARGETED_BY_ATTACK)
                .reactor(santoSheet)
                .reactorContext(santoContext)
                .threatenedAlly(allySheet)
                .build());

        assertEquals(1, options.size());
        assertEquals(SantoAbility.GUARDA_VIDAS, options.get(0).ability());
        assertTrue(options.get(0).affordable());
        // The reach the client needs to draw the teleport before the player commits.
        assertEquals(Teleportation.of(Range.DISTANCIA_CURTA), options.get(0).teleportation());

        // 2. The player takes it — through the held Título, which checks the Habilidade is held.
        InteractionResult reaction = santoCharacter.getPrimaryTitle().activateAbility(
                SantoAbility.GUARDA_VIDAS,
                TitleAbilityActivationRequest.builder()
                        .activator(santoSheet)
                        .target(allySheet)
                        .sceneContext(santoContext)
                        .build());

        assertSame(santoSheet, reaction.getRedirectedAttackTarget());
        assertEquals(pdBefore - 3, determinationPointsService.getCurrentDeterminationPoints(santoCharacter, santoSheet));

        // 3. The attack is built naming the redirected target, and resolved as any other attack —
        // "o ataque ainda deve superar as suas Defesas" holds because these are the Santo's.
        IncomingAttackResult result = new AttackReceiver().resolve(IncomingAttack.builder()
                .defender(reaction.getRedirectedAttackTarget())
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(1, 1, 1)))
                .build());

        assertFalse(result.getDefended());

        // 4. The damage lands on the Santo. The ally, whom the attack was aimed at, is untouched.
        damageService.applyDamage(santoSheet, santoContext, 10, false);

        assertEquals(10, santoSheet.getDamageTaken());
        assertEquals(0, allySheet.getDamageTaken());
    }
}
