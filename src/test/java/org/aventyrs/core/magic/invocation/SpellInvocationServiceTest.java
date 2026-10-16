package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.SpellCastRequest;
import org.aventyrs.core.magic.SpellCastingResult;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.AliadosDaNaturezaSpell;
import org.aventyrs.core.magic.catalog.ReanimarSpell;
import org.aventyrs.core.magic.catalog.SpellCatalog;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.monster.summon.NatureSummonKind;
import org.aventyrs.core.monster.summon.UndeadSummonKind;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.util.DiceRoller;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Magia names what it invokes, and one service turns a cast of it into an order (core 0.1.7.6). */
class SpellInvocationServiceTest {

    private final SpellInvocationService service = new SpellInvocationServiceImpl();
    private final NatureInvocationService nature = new NatureInvocationServiceImpl();
    private final UndeadInvocationService undead = new UndeadInvocationServiceImpl();
    /** Cycles 1–6, so a roll of distinct Lacerto powers always finishes (a constant face never would). */
    private final int[] face = {0};
    private final DiceRoller roller = () -> face[0]++ % 6 + 1;
    private CharacterSheet caster;
    private CharacterSheet ogre;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(6).build())
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .skill(SkillType.DOMINIO_DO_MANA, CharacterSkill.builder()
                        .skill(SkillType.DOMINIO_DO_MANA.newSkillInstance()).specializations(List.of())
                        .graduation(SkillGraduation.builder().graduationValue(4).build()).build())
                .build();
        caster = CharacterSheet.of(character, new Player());
        ogre = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.PLUS_ONE).build(), new Player());
        ogre.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(ogre.getCharacter(), ogre) * 2);
        scene = new Scene();
        scene.addParticipant(caster, 20, UUID.randomUUID());
        scene.addParticipant(ogre, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(caster, scene.next());
    }

    private InvocationCast cast(final CombatantSheet target) {
        return InvocationCast.builder().caster(caster).target(target).roller(roller).build();
    }

    // ---------- the column ----------

    @Test
    void everyAliadosAndReanimarInvocationNamesItsShape() {
        Map<String, SpellInvocation> byName = SpellCatalog.all().stream()
                .filter(spell -> spell.getInvocation().isPresent())
                .collect(Collectors.toMap(spell -> spell.getName(), spell -> spell.getInvocation().orElseThrow()));

        assertEquals(Set.of("Cativar Animal", "Aliados da Natureza", "Canção de Flora", "Experimento de Larcerto",
                "Totem de Gaea", "Orgulho de Lacerto", "Despertar Anciente de Gaea", "Reanimar", "Criar Carniçal",
                "Festim dos Mortos"), byName.keySet());
        assertTrue(ReanimarSpell.INVOCAR_ABANTESMA.getInvocation().isEmpty(), "still open on its own");
    }

    @Test
    void anEfeitoAlternativoNamesItsOwnInvocation() {
        assertEquals(SpellInvocation.FALSA_MATILHA, alternateOf(AliadosDaNaturezaSpell.CATIVAR_ANIMAL));
        assertEquals(SpellInvocation.PREDADOR_REGIONAL, alternateOf(AliadosDaNaturezaSpell.ALIADOS_DA_NATUREZA));
        assertEquals(SpellInvocation.LABORATORIO, alternateOf(AliadosDaNaturezaSpell.ORGULHO_DE_LACERTO));
        assertEquals(SpellInvocation.SERVO_CADAVERICO, alternateOf(ReanimarSpell.REANIMAR));
        assertEquals(SpellInvocation.ACOUGUEIRO, alternateOf(ReanimarSpell.CRIAR_CARNICAL));
        assertEquals(SpellInvocation.HORDA_FAMINTA, alternateOf(ReanimarSpell.FESTIM_DOS_MORTOS));
    }

    private static SpellInvocation alternateOf(final org.aventyrs.core.magic.Spell spell) {
        return spell.getAlternateVersion().orElseThrow().getInvocation().orElseThrow();
    }

    // ---------- plan ----------

    @Test
    void everyInvocationPlansAnOrderOfItsShape() {
        for (SpellInvocation invocation : SpellInvocation.values()) {
            CombatantSheet target = invocation.target() == SpellInvocation.Target.BESIDE_CASTER ? null : ogre;
            InvocationOrder order = service.plan(invocation, cast(target));
            Class<? extends InvocationOrder> expected = switch (invocation.shape()) {
                case SUMMON -> InvocationOrder.Summons.class;
                case SPAWNER -> InvocationOrder.Spawner.class;
                case SUBORDINATE -> InvocationOrder.Subordination.class;
            };
            assertInstanceOf(expected, order, invocation.name());
        }
    }

    @Test
    void anAliadoIsTheNatureTreesPlanWithPotencializarsRodadas() {
        InvocationPlan direct = nature.planAliado(caster, true);

        InvocationOrder.Summons order = (InvocationOrder.Summons) service.plan(SpellInvocation.PREDADOR_REGIONAL,
                InvocationCast.builder().caster(caster).roller(roller).extraRounds(2).build());

        SummonOrder predator = order.creatures().get(0);
        assertEquals(NatureSummonKind.PREDADOR_REGIONAL.name(), predator.kind());
        assertEquals(direct.exclusivityGroup(), predator.exclusivityGroup());
        assertEquals(direct.rounds() + 2, predator.rounds());
        assertEquals(direct.creatures().get(0).getConjuradorManaGraduation(), predator.conjuradorManaGraduation());
        assertNull(predator.corpse());
    }

    @Test
    void cancaoDeFloraSingsEveryAnimalAsked() {
        InvocationOrder.Summons order = (InvocationOrder.Summons) service.plan(SpellInvocation.CANCAO_DE_FLORA,
                InvocationCast.builder().caster(caster).roller(roller)
                        .choices(new InvocationChoices(2, 1)).build());

        assertEquals(nature.planCancaoDeFlora(caster, 2, 1, false).creatures().size(), order.creatures().size());
    }

    @Test
    void reanimarRaisesAZumbiFromTheTouchedBody() {
        UndeadPlan direct = undead.planReanimar(caster, ogre);

        SummonOrder zumbi = ((InvocationOrder.Summons) service.plan(SpellInvocation.ZUMBI,
                InvocationCast.builder().caster(caster).target(ogre).roller(roller).extraRounds(3).build()))
                .creatures().get(0);

        assertEquals(UndeadSummonKind.ZUMBI.name(), zumbi.kind());
        assertSame(ogre, zumbi.corpse());
        assertEquals(SizeCategory.PLUS_ONE, zumbi.bodySize());
        assertEquals(direct.rounds(), zumbi.rounds(), "Potencializar never reached an undead's Duração");
        assertTrue(zumbi.concentration());
        assertFalse(zumbi.butcher());
    }

    @Test
    void anAcougueiroCarnicalHasNoDuracaoAndItsOwnGroup() {
        SummonOrder carnical = ((InvocationOrder.Summons) service.plan(SpellInvocation.ACOUGUEIRO, cast(ogre)))
                .creatures().get(0);

        assertTrue(carnical.butcher());
        assertNull(carnical.rounds());
        assertEquals(UndeadInvocationService.BUTCHER_GROUP, carnical.exclusivityGroup());
    }

    @Test
    void aCorpseInvocationNeedsABody() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.plan(SpellInvocation.CARNICAL, cast(null)));
        assertEquals(TranslatableMessages.INVOCATION_TARGET_REQUIRED, refused.getMessage());
    }

    @Test
    void theTotemStandsASpawnerOfPredadores() {
        SpawnerOrder totem = ((InvocationOrder.Spawner) service.plan(SpellInvocation.TOTEM_DE_GAEA,
                InvocationCast.builder().caster(caster).roller(roller).extraRounds(1).build())).spawner();

        assertEquals(NatureInvocationService.ROUNDS + 1, totem.rounds());
        assertEquals(NatureSummonKind.PREDADOR_REGIONAL.name(), totem.kind());
        assertEquals(NatureInvocationService.ROUNDS, totem.summonRounds());
        assertNull(totem.rule());
    }

    @Test
    void festimFillsUpToTheCastersLimitByItsRule() {
        SpawnerOrder festim = ((InvocationOrder.Spawner) service.plan(SpellInvocation.FESTIM, cast(null))).spawner();
        SpawnerOrder horda = ((InvocationOrder.Spawner) service.plan(SpellInvocation.HORDA_FAMINTA, cast(null)))
                .spawner();

        assertEquals(SpawnerOrder.FESTIM, festim.rule());
        assertEquals(SpawnerOrder.FESTIM_HORDA, horda.rule());
        assertEquals(FestimDosMortos.ROUNDS, festim.rounds());
        assertEquals(SummonLimit.of(caster), festim.summonLimit());
        assertNull(festim.kind());
    }

    // ---------- extra PM ----------

    @Test
    void theExtraManaIsTheTreesOwn() {
        assertEquals(NatureInvocationService.PREDATOR_MANA_COST,
                service.extraManaCost(SpellInvocation.PREDADOR_REGIONAL, caster, null));
        assertEquals(nature.planCancaoDeFlora(caster, 2, 1, false).extraManaCost(),
                service.extraManaCost(SpellInvocation.CANCAO_DE_FLORA, caster, new InvocationChoices(2, 1)));
        assertEquals(UndeadInvocationService.BUTCHER_EXTRA_MANA,
                service.extraManaCost(SpellInvocation.ACOUGUEIRO, caster, null));
        assertEquals(0, service.extraManaCost(SpellInvocation.ZUMBI, caster, null));
    }

    @Test
    void aCastIsPricedWithItsInvocationAndReportsIt() {
        SpellCastingService casting = new SpellCastingServiceImpl();
        SpellCastRequest request = SpellCastRequest.builder()
                .caster(caster)
                .spell(AliadosDaNaturezaSpell.ALIADOS_DA_NATUREZA)
                .useAlternateVersion(true)
                .scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of()))
                .combatantTarget(caster)
                .build();

        SpellCastingResult result = casting.castSpell(request);

        assertEquals(SpellInvocation.PREDADOR_REGIONAL, result.getInvocation());
        assertEquals(AliadosDaNaturezaSpell.ALIADOS_DA_NATUREZA.getManaCost()
                + NatureInvocationService.PREDATOR_MANA_COST, casting.resolveManaCost(request));
    }

    // ---------- subordinate ----------

    @Test
    void cativarAnimalAndServoCadavericoMakeASubordinado() {
        MonsterSheet animal = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(new Player());

        Subordinate captive = service.subordinate(SpellInvocation.CATIVAR_ANIMAL, scene, caster, animal, null,
                SubordinateBenefit.TORRE_DEFESAS, false, null);
        Subordinate servant = service.subordinate(SpellInvocation.SERVO_CADAVERICO, scene, caster, ogre, null,
                SubordinateBenefit.CAVALEIRO_ATTACK, false, null);

        assertEquals(animal.getId(), captive.getCreatureId());
        assertEquals(ogre.getId(), servant.getCreatureId());
        assertEquals(TranslatableMessages.INVOCATION_NOT_SUBORDINATE, assertThrows(IllegalOperationException.class,
                () -> service.subordinate(SpellInvocation.ALIADO, scene, caster, animal, null,
                        SubordinateBenefit.TORRE_DEFESAS, false, null)).getMessage());
    }
}
