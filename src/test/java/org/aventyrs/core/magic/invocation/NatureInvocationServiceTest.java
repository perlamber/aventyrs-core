package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.monster.summon.NatureSummonKind;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateBenefits;
import org.aventyrs.core.util.DiceRoller;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ALIADOS DA NATUREZA Magias' invocations (core 0.0.92). */
class NatureInvocationServiceTest {

    private final NatureInvocationService service = new NatureInvocationServiceImpl();
    private final Player gm = new Player();
    private CharacterSheet druid;
    private CharacterSheet foe;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        druid = caster(4, 0, false);
        foe = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene = new Scene();
        scene.addParticipant(druid, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(druid, scene.next());
    }

    private static CharacterSheet caster(final int mana, final int knowledge, final boolean natureza) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.DOMINIO_DO_MANA, skill(SkillType.DOMINIO_DO_MANA, mana, List.of()));
        if (knowledge > 0) {
            builder.skill(SkillType.CONHECIMENTOS, skill(SkillType.CONHECIMENTOS, knowledge,
                    natureza ? List.of(ConhecimentosSpecialization.NATUREZA) : List.of()));
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static CharacterSkill skill(final SkillType type, final int graduation,
                                        final List<SkillSpecialization> specializations) {
        return CharacterSkill.builder().skill(type.newSkillInstance()).specializations(specializations)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build()).build();
    }

    private static int maxHitPoints(final CombatantSheet sheet) {
        return new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);
    }

    // ---------- Aliados da Natureza ----------

    /** The Aliado comes at the caster's Graduação (4: 20PV), acts after them, and lasts 3 Rodadas. */
    @Test
    void anAliadoJoinsAfterItsCasterAtTheirGraduacao() {
        Invocation invocation = service.invokeAliado(scene, druid, false, gm);

        SceneSummon aliado = invocation.summons().get(0);
        assertEquals(0, invocation.extraManaCost());
        assertEquals(20, maxHitPoints(aliado.getSummon()));
        assertEquals(3, aliado.getRemainingRounds());
        assertEquals(List.of(druid, aliado.getSummon(), foe), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void aPredadorRegionalCostsOneMorePm() {
        Invocation invocation = service.invokeAliado(scene, druid, true, gm);

        assertEquals(NatureInvocationService.PREDATOR_MANA_COST, invocation.extraManaCost());
        assertEquals("Predador Regional", invocation.summons().get(0).getSummon().getCharacter().getName());
    }

    /** "Apenas 1 Aliado da Natureza pode ser invocado por vez" — the older one leaves as the Turn ends. */
    @Test
    void aSecondAliadoSendsTheFirstAwayAtTheEndOfTheTurn() {
        CombatantSheet first = service.invokeAliado(scene, druid, false, gm).summons().get(0).getSummon();
        CombatantSheet second = service.invokeAliado(scene, druid, false, gm).summons().get(0).getSummon();

        scene.next();

        assertFalse(scene.getAllParticipants().contains(first));
        assertTrue(scene.getAllParticipants().contains(second));
    }

    // ---------- Canção de Flora ----------

    /** "1 Predador Regional e 1 Aliado", +2PM per extra animal, +1PM per strengthened one, held by Concentração. */
    @Test
    void cancaoDeFloraSingsAPackHeldByConcentracao() {
        Invocation invocation = service.singCancaoDeFlora(scene, druid, 2, 1, false, gm);

        assertEquals(4, invocation.summons().size());
        assertEquals(2 * 2 + 1, invocation.extraManaCost());
        assertEquals(2, invocation.summons().stream()
                .filter(summon -> summon.getSummon().getCharacter().getName().equals("Predador Regional")).count());
        assertTrue(invocation.summons().stream().allMatch(SceneSummon::isSustained));
        assertNull(invocation.summons().get(0).getRemainingRounds());

        scene.breakConcentration(druid);
        assertEquals(2, invocation.summons().get(0).getRemainingRounds());
    }

    /** "Não é possível manter invocações de Canção de Flora e Aliados da Natureza simultaneamente." */
    @Test
    void cancaoDeFloraAndAliadosDaNaturezaExcludeEachOther() {
        CombatantSheet aliado = service.invokeAliado(scene, druid, false, gm).summons().get(0).getSummon();
        Invocation pack = service.singCancaoDeFlora(scene, druid, 0, 0, false, gm);

        scene.next();

        assertFalse(scene.getAllParticipants().contains(aliado));
        assertTrue(pack.summons().stream().allMatch(summon -> scene.getAllParticipants().contains(summon.getSummon())),
                "one cast's animals never replace each other");
    }

    /** Fauna Flora: "1 Aliado da Natureza para cada 3 Graduações em Conhecimento: Natureza". */
    @Test
    void faunaFloraAddsAnAliadoPerThreeGraduacoesInNatureza() {
        CharacterSheet naturalist = caster(4, 7, true);
        CharacterSheet scholar = caster(4, 7, false);
        scene.addParticipant(naturalist, 30, UUID.randomUUID());
        scene.addParticipant(scholar, 30, UUID.randomUUID());

        assertEquals(4, service.singCancaoDeFlora(scene, naturalist, 0, 0, true, gm).summons().size());
        assertEquals(2, service.singCancaoDeFlora(scene, scholar, 0, 0, true, gm).summons().size());
    }

    // ---------- Lacerto ----------

    @Test
    void anExperimentoRollsItsPowerAndIsAloneOfItsKind() {
        Invocation first = service.invokeExperimento(scene, druid, DiceRoller.fixed(5), gm);
        service.invokeExperimento(scene, druid, DiceRoller.fixed(6), gm);

        assertEquals(3, first.summons().get(0).getRemainingRounds());
        assertTrue(first.summons().get(0).isReplaced());
    }

    /** Laboratório de Lacerto: two Experimentos at once, "não é cumulativo com a magia Orgulho de Lacerto". */
    @Test
    void theLaboratorioReplacesTheOrgulho() {
        Invocation orgulho = service.invokeOrgulho(scene, druid, DiceRoller.fixed(1, 2), gm);
        Invocation laboratorio = service.invokeLaboratorio(scene, druid, DiceRoller.fixed(3, 4), gm);

        assertEquals(2, laboratorio.summons().size());
        assertTrue(orgulho.summons().get(0).isSustained());
        assertTrue(orgulho.summons().get(0).isReplaced());
        assertFalse(laboratorio.summons().get(0).isReplaced());
    }

    @Test
    void theAncienteIsHeldByConcentracaoAndLeavesTwoRodadasAfterItBreaks() {
        CombatantSheet anciente = service.awakenAnciente(scene, druid, gm).summons().get(0).getSummon();
        scene.breakConcentration(druid);

        scene.next();
        scene.next();
        assertSame(druid, scene.next());   // Rodada 2
        assertTrue(scene.getAllParticipants().contains(anciente));
        scene.next();
        scene.next();
        scene.next();
        assertFalse(scene.getAllParticipants().contains(anciente));
    }

    // ---------- Totem de Gaea ----------

    /** "A cada Rodada um novo animal é criado" — one now, one per Rodada for the Totem's 3 Rodadas. */
    @Test
    void theTotemRaisesAPredadorEachRodada() {
        service.raiseTotem(scene, druid, gm);
        assertEquals(1, scene.getSummonsOf(druid).size());

        nextRodada();
        assertEquals(2, scene.getSummonsOf(druid).size());
        nextRodada();
        assertEquals(3, scene.getSummonsOf(druid).size());
        nextRodada();
        assertTrue(scene.getSummonSpawners().isEmpty(), "spent after its 3 Rodadas");
        assertEquals(2, scene.getSummonsOf(druid).size(), "the first Predador's own 3 Rodadas ran out");
    }

    @Test
    void theTotemBlessesAnimalsInReachForARodada() {
        MonsterSheet animal = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        int before = actionPoints.getMaxActionPoints(animal, 0);

        service.blessFromTotem(List.of(animal));
        service.blessFromTotem(List.of(animal));

        assertEquals(before + 1, actionPoints.getMaxActionPoints(animal, 0), "a second blessing renews, never stacks");
    }

    /** Advances across the next Rodada boundary. */
    private void nextRodada() {
        int round = scene.getCurrentRound();
        while (scene.getCurrentRound() == round) {
            scene.next();
        }
    }

    // ---------- Cativar Animal ----------

    @Test
    void cativarAnimalMakesTheAnimalACavaleiroOrTorre() {
        MonsterSheet animal = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());

        Subordinate subordinate = service.captivate(druid, animal, SubordinateBenefit.TORRE_DEFESAS, false, null);

        assertEquals(3, subordinate.getRemainingRounds());
        assertEquals(animal.getId(), subordinate.getCreatureId());
        assertEquals(1, SubordinateBenefits.of(druid).size());
        IllegalOperationException again = assertThrows(IllegalOperationException.class,
                () -> service.captivate(druid, animal, SubordinateBenefit.CAVALEIRO_ATTACK, false, null));
        assertEquals(TranslatableMessages.CAPTIVATE_NEEDS_REST, again.getMessage());
    }

    /** Falsa Matilha: "A Duração da magia é reduzida à metade. O animal tocado se torna um Subordinado Prodigioso." */
    @Test
    void falsaMatilhaMakesItProdigiosoForHalfTheDuracao() {
        MonsterSheet animal = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());

        Subordinate subordinate = service.captivate(druid, animal, SubordinateBenefit.CAVALEIRO_DAMAGE, true, null);

        assertTrue(subordinate.isProdigious());
        assertEquals(2, subordinate.getRemainingRounds());
    }

    @Test
    void cativarAnimalRefusesANonAnimalOrAnotherGrade() {
        MonsterSheet animal = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());

        assertEquals(TranslatableMessages.CAPTIVATE_REQUIRES_ANIMAL, assertThrows(IllegalOperationException.class,
                () -> service.captivate(druid, foe, SubordinateBenefit.TORRE_RA, false, null)).getMessage());
        assertEquals(TranslatableMessages.CAPTIVATE_GRADE_NOT_ALLOWED, assertThrows(IllegalOperationException.class,
                () -> service.captivate(druid, animal, SubordinateBenefit.REI_SORTE, false, null)).getMessage());
    }
}
