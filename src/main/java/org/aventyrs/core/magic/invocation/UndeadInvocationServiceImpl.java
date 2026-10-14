package org.aventyrs.core.magic.invocation;

import lombok.NonNull;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.monster.summon.Carnical;
import org.aventyrs.core.monster.summon.UndeadSummonKind;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.scene.SummonSpawner;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateGrade;
import org.aventyrs.core.subordinate.SubordinateService;
import org.aventyrs.core.subordinate.SubordinateServiceImpl;
import org.aventyrs.core.util.DiceRoller;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.BUTCHER_CARNICAL_EXCLUSIVE;
import static org.aventyrs.core.util.TranslatableMessages.SERVO_CADAVERICO_GRADE_NOT_ALLOWED;

public class UndeadInvocationServiceImpl implements UndeadInvocationService {

    private final RaiseDeadService raiseDeadService;
    private final SubordinateService subordinateService;

    public UndeadInvocationServiceImpl() {
        this(new RaiseDeadServiceImpl(), new SubordinateServiceImpl());
    }

    public UndeadInvocationServiceImpl(final RaiseDeadService raiseDeadService,
                                       final SubordinateService subordinateService) {
        this.raiseDeadService = raiseDeadService;
        this.subordinateService = subordinateService;
    }

    @Override
    public UndeadPlan planReanimar(@NonNull final CombatantSheet caster, @NonNull final CombatantSheet corpse) {
        int graduation = SummonedMonsterTemplate.manaGraduationOf(caster.getCharacter());
        return new UndeadPlan(UndeadSummonKind.ZUMBI,
                UndeadSummonKind.ZUMBI.template(graduation, bodySize(corpse), false),
                null, TRAILING_ROUNDS, true, 0, false);
    }

    @Override
    public UndeadPlan planCarnical(@NonNull final CombatantSheet caster, @NonNull final CombatantSheet corpse,
                                   final boolean butcher) {
        int graduation = SummonedMonsterTemplate.manaGraduationOf(caster.getCharacter());
        SummonedMonsterTemplate carnical = UndeadSummonKind.CARNICAL.template(graduation, bodySize(corpse), butcher);
        return butcher
                // "Seres criados com este método não possuem Duração, permanecendo ativos até terem seus PV reduzidos à 0."
                ? new UndeadPlan(UndeadSummonKind.CARNICAL, carnical, BUTCHER_GROUP, null, false, BUTCHER_EXTRA_MANA, true)
                : new UndeadPlan(UndeadSummonKind.CARNICAL, carnical, null, TRAILING_ROUNDS, true, 0, false);
    }

    /** "Categoria de Tamanho igual à do corpo usado" — the body's own, not a Forma's; the template clamps it. */
    private static SizeCategory bodySize(final CombatantSheet corpse) {
        SizeCategory size = corpse.getCharacter().getSizeCategory();
        return size == null ? SizeCategory.ZERO : size;
    }

    @Override
    public SceneSummon raise(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                             @NonNull final CombatantSheet corpse, @NonNull final UndeadPlan plan,
                             @NonNull final Player gm) {
        raiseDeadService.requireRaisable(scene, corpse);
        if (plan.kind() == UndeadSummonKind.CARNICAL) {
            requireButcherExclusivity(scene, caster, plan.butcher());
        }
        SummonLimit.requireRoom(scene, caster, 1, plan.exclusivityGroup());
        return scene.raise(caster, corpse, plan.creature().spawn(gm), plan.exclusivityGroup(), plan.rounds(),
                plan.concentration());
    }

    /**
     * An Açougueiro Carniçal and any other Carniçal of the caster's exclude each other (⚠️ reading of "não poderá
     * controlar mais nenhum outro Carniçal com esta magia": neither may join while the other kind is held).
     */
    private static void requireButcherExclusivity(final Scene scene, final CombatantSheet caster, final boolean butcher) {
        List<SceneSummon> carnicais = scene.getSummonsOf(caster).stream()
                .filter(held -> !held.isReplaced() && isCarnical(held.getSummon()))
                .toList();
        boolean butcherHeld = carnicais.stream().anyMatch(held -> BUTCHER_GROUP.equals(held.getExclusivityGroup()));
        boolean othersHeld = carnicais.stream().anyMatch(held -> !BUTCHER_GROUP.equals(held.getExclusivityGroup()));
        if (butcher ? othersHeld || butcherHeld : butcherHeld) {
            throw new IllegalOperationException(BUTCHER_CARNICAL_EXCLUSIVE);
        }
    }

    private static boolean isCarnical(final CombatantSheet sheet) {
        return Carnical.builder().build().getName().equals(sheet.getCharacter().getName());
    }

    @Override
    public Subordinate servoCadaverico(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                       @NonNull final CombatantSheet corpse, @NonNull final SubordinateBenefit benefit,
                                       final boolean escudeiroCadaverico, final SceneContext sceneContext) {
        SubordinateGrade grade = benefit.getGrade();
        if (grade != SubordinateGrade.CAVALEIRO && grade != SubordinateGrade.TORRE) {
            throw new IllegalOperationException(SERVO_CADAVERICO_GRADE_NOT_ALLOWED);
        }
        raiseDeadService.requireRaisable(scene, corpse);
        Subordinate servant = Subordinate.sustainedBody(null, benefit, false, SERVO_CADAVERICO, corpse.getId(),
                caster.getId(), TRAILING_ROUNDS);
        if (escudeiroCadaverico) {
            servant.prodigiousThrough(scene.getCurrentRound() + ESCUDEIRO_CADAVERICO_ROUNDS - 1);
        }
        subordinateService.command(caster, servant, sceneContext);
        caster.beginConcentration();
        corpse.setTimesRaised(corpse.getTimesRaised() + 1);
        return servant;
    }

    @Override
    public List<SceneSummon> invokeCarrascos(@NonNull final Scene scene, @NonNull final CombatantSheet monster,
                                             @NonNull final org.aventyrs.core.monster.model.MonsterSummoning summoning,
                                             @NonNull final Player gm) {
        int carisma = monster.getAttributeTotal(org.aventyrs.core.character.AttributeDomain.CHARISMA);
        if (scene.countControlledSummons(monster) + summoning.count() > carisma) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.SUMMON_LIMIT_REACHED);
        }
        List<CombatantSheet> carrascos = new java.util.ArrayList<>();
        for (int i = 0; i < summoning.count(); i++) {
            carrascos.add(org.aventyrs.core.monster.summon.CarrascoMorimbundo.stock(summoning.category()).spawn(gm));
        }
        return scene.addSummons(monster, carrascos, null, summoning.rounds(), summoning.concentration());
    }

    @Override
    public SummonSpawner startPlagueSpitting(@NonNull final Scene scene, @NonNull final CombatantSheet spitter,
                                             @NonNull final Player gm) {
        SummonSpawner.Rule rule = (firing, room) -> {
            if (scene.getCurrentRound() % 2 != 0) {
                return List.of();
            }
            List<SummonSpawner.Spawned> brought = new java.util.ArrayList<>();
            for (int i = 0; i < org.aventyrs.core.monster.model.escolhidodemacaria.EscolhidoDeMacariaAbility
                    .PLAGUE_SPAWN_COUNT; i++) {
                brought.add(new SummonSpawner.Spawned(org.aventyrs.core.monster.summon.CarrascoMorimbundo
                        .stock(org.aventyrs.core.monster.MonsterCategory.DEVIANTE).spawn(gm), false));
            }
            return brought;
        };
        return scene.addSummonSpawner(spitter, null, rule, null, false, true, true);
    }

    @Override
    public SummonSpawner openSarcophagus(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                         final boolean hordaFaminta, @NonNull final DiceRoller roller,
                                         @NonNull final Player gm) {
        int graduation = SummonedMonsterTemplate.manaGraduationOf(caster.getCharacter());
        SummonSpawner.Rule rule = (firing, room) -> {
            List<FestimDosMortos.Spawn> spawns = FestimDosMortos.spawnsFor(roller.rollD6(), hordaFaminta, room);
            int left = room;
            List<SummonSpawner.Spawned> brought = new java.util.ArrayList<>();
            for (FestimDosMortos.Spawn spawn : spawns) {
                // Room is spent creature by creature: a controlled one past the limit rises uncontrolled too.
                boolean uncontrolled = spawn.uncontrolled() || left <= 0;
                if (!uncontrolled) {
                    left--;
                }
                brought.add(new SummonSpawner.Spawned(
                        spawn.kind().template(graduation, null, false).spawn(gm), uncontrolled));
            }
            return brought;
        };
        // The creatures last as long as the sarcophagus ("não podem ter a Duração estendida"), act from the next Rodada.
        return scene.addSummonSpawner(caster, FestimDosMortos.ROUNDS, rule, null, true, true, false);
    }
}
