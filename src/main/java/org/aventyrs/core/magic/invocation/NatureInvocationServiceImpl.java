package org.aventyrs.core.magic.invocation;

import lombok.NonNull;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.monster.summon.NatureSummonKind;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.scene.SummonSpawner;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateGrade;
import org.aventyrs.core.subordinate.SubordinateService;
import org.aventyrs.core.subordinate.SubordinateServiceImpl;
import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.CAPTIVATE_GRADE_NOT_ALLOWED;
import static org.aventyrs.core.util.TranslatableMessages.CAPTIVATE_NEEDS_REST;
import static org.aventyrs.core.util.TranslatableMessages.CAPTIVATE_REQUIRES_ANIMAL;

public class NatureInvocationServiceImpl implements NatureInvocationService {

    /** Cativar Animal's source — on the Subordinado, and the per-animal "until a Descanso" mark. */
    public static final String CATIVAR_ANIMAL = "Cativar Animal";

    /** Totem de Gaea's source, so a second Totem renews its grants rather than stacking them. */
    public static final String TOTEM_DE_GAEA = "Totem de Gaea";

    /** Cativar Animal: "Duração: 3 Rodadas". */
    public static final int CATIVAR_ROUNDS = 3;

    private final SubordinateService subordinateService;

    public NatureInvocationServiceImpl() {
        this(new SubordinateServiceImpl());
    }

    public NatureInvocationServiceImpl(final SubordinateService subordinateService) {
        this.subordinateService = subordinateService;
    }

    // ---------- plans: what each Magia brings ----------

    @Override
    public InvocationPlan planAliado(@NonNull final CombatantSheet caster, final boolean predator) {
        NatureSummonKind kind = predator ? NatureSummonKind.PREDADOR_REGIONAL : NatureSummonKind.ALIADO_DA_NATUREZA;
        return new InvocationPlan(List.of(at(NatureSummon.of(kind), caster)), FAUNA_GROUP, ROUNDS, false,
                predator ? PREDATOR_MANA_COST : 0);
    }

    @Override
    public InvocationPlan planCancaoDeFlora(@NonNull final CombatantSheet caster, final int additionalAnimals,
                                            final int strengthened, final boolean faunaFlora) {
        int extra = Math.max(0, additionalAnimals);
        int aliados = 1 + extra;
        int predators = Math.min(Math.max(0, strengthened), aliados);
        int faunaFloraAnimals = faunaFlora ? faunaFloraAnimals(caster) : 0;
        List<NatureSummon> animals = new ArrayList<>();
        animals.add(at(NatureSummon.of(NatureSummonKind.PREDADOR_REGIONAL), caster));
        for (int i = 0; i < aliados + faunaFloraAnimals; i++) {
            NatureSummonKind kind = i < predators ? NatureSummonKind.PREDADOR_REGIONAL : NatureSummonKind.ALIADO_DA_NATUREZA;
            animals.add(at(NatureSummon.of(kind), caster));
        }
        return new InvocationPlan(animals, FAUNA_GROUP, TRAILING_ROUNDS, true,
                extra * ADDITIONAL_ANIMAL_MANA_COST + predators * STRENGTHENED_ANIMAL_MANA_COST);
    }

    @Override
    public InvocationPlan planExperimento(@NonNull final CombatantSheet caster, @NonNull final DiceRoller roller) {
        return new InvocationPlan(List.of(at(NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, roller),
                caster)), EXPERIMENTO_GROUP, ROUNDS, false, 0);
    }

    @Override
    public InvocationPlan planOrgulho(@NonNull final CombatantSheet caster, @NonNull final DiceRoller roller) {
        return new InvocationPlan(List.of(at(NatureSummon.invoked(NatureSummonKind.ORGULHO_DE_LACERTO, roller),
                caster)), ORGULHO_GROUP, TRAILING_ROUNDS, true, 0);
    }

    @Override
    public InvocationPlan planLaboratorio(@NonNull final CombatantSheet caster, @NonNull final DiceRoller roller) {
        List<NatureSummon> experimentos = new ArrayList<>();
        for (int i = 0; i < LABORATORY_EXPERIMENTS; i++) {
            experimentos.add(at(NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, roller), caster));
        }
        return new InvocationPlan(experimentos, ORGULHO_GROUP, TRAILING_ROUNDS, true, 0);
    }

    @Override
    public InvocationPlan planAnciente(@NonNull final CombatantSheet caster) {
        return new InvocationPlan(List.of(at(NatureSummon.of(NatureSummonKind.ANCIENTE), caster)), null,
                TRAILING_ROUNDS, true, 0);
    }

    @Override
    public InvocationPlan planTotemPredator(@NonNull final CombatantSheet caster) {
        return new InvocationPlan(List.of(at(NatureSummon.of(NatureSummonKind.PREDADOR_REGIONAL), caster)), null,
                ROUNDS, false, 0);
    }

    /** Invocação Maior: "+1PA" and "+2PD"; Invocação Dupla: "+2PA". */
    public static final int LIFE_OPTION_ACTION_POINTS = 1;
    public static final int ATTRIBUTE_OPTION_DETERMINATION = 2;
    public static final int DOUBLED_OPTION_ACTION_POINTS = 2;

    @Override
    public InvocationPlan enhance(@NonNull final InvocationPlan plan, @NonNull final CombatantSheet caster,
                                  @NonNull final InvocationOptions options) {
        SummonEnhancement enhancement = caster.getCharacter().getAllTitles().stream()
                .map(title -> title.resolveSummonEnhancement(caster, options))
                .filter(resolved -> !resolved.isNone())
                .findFirst()
                .orElse(SummonEnhancement.NONE);
        if (!options.equals(InvocationOptions.NONE) && enhancement.isNone()) {
            throw new org.aventyrs.core.sheet.IllegalOperationException(
                    org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD);
        }
        List<NatureSummon> creatures = new ArrayList<>();
        for (NatureSummon creature : plan.creatures()) {
            NatureSummon enhanced = creature.withEnhancement(enhancement);
            creatures.add(enhanced);
            if (options.doubled()) {
                creatures.add(enhanced);
            }
        }
        Integer rounds = options.doubled() && plan.rounds() != null ? Math.max(1, plan.rounds() / 2) : plan.rounds();
        int actionPoints = (options.extraTimeForLife() ? LIFE_OPTION_ACTION_POINTS : 0)
                + (options.doubled() ? DOUBLED_OPTION_ACTION_POINTS : 0);
        int determination = options.boostedAttribute() != null ? ATTRIBUTE_OPTION_DETERMINATION : 0;
        return new InvocationPlan(creatures, plan.exclusivityGroup(), rounds, plan.concentration(),
                plan.extraManaCost(), plan.extraActionPoints() + actionPoints,
                plan.extraDeterminationCost() + determination,
                plan.concentrationUpkeepMultiplier() * (options.doubled() && plan.concentration() ? 2 : 1),
                enhancement);
    }

    private static NatureSummon at(final NatureSummon creature, final CombatantSheet caster) {
        return creature.withConjurador(SummonedMonsterTemplate.manaGraduationOf(caster.getCharacter()));
    }

    private static int faunaFloraAnimals(final CombatantSheet caster) {
        if (!caster.getCharacter().getSpecializations(SkillType.CONHECIMENTOS)
                .contains(ConhecimentosSpecialization.NATUREZA)) {
            return 0;
        }
        return caster.getCharacter().getEffectiveGraduation(SkillType.CONHECIMENTOS) / FAUNA_FLORA_GRADUATIONS_PER_ANIMAL;
    }

    // ---------- placing a plan in a core Scene ----------

    /** Spawns plan's creatures for gm and places them as caster's invocations. */
    public static Invocation place(final Scene scene, final CombatantSheet caster, final InvocationPlan plan,
                                   final Player gm) {
        List<CombatantSheet> sheets = plan.creatures().stream()
                .map(creature -> (CombatantSheet) creature.spawn(gm))
                .toList();
        return new Invocation(scene.addSummons(caster, sheets, plan.exclusivityGroup(), plan.rounds(),
                plan.concentration()), plan.extraManaCost());
    }

    @Override
    public Invocation invokeAliado(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                   final boolean predator, @NonNull final Player gm) {
        return place(scene, caster, planAliado(caster, predator), gm);
    }

    @Override
    public Invocation singCancaoDeFlora(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                        final int additionalAnimals, final int strengthened, final boolean faunaFlora,
                                        @NonNull final Player gm) {
        return place(scene, caster, planCancaoDeFlora(caster, additionalAnimals, strengthened, faunaFlora), gm);
    }

    @Override
    public Invocation invokeExperimento(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                        @NonNull final DiceRoller roller, @NonNull final Player gm) {
        return place(scene, caster, planExperimento(caster, roller), gm);
    }

    @Override
    public Invocation invokeOrgulho(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                    @NonNull final DiceRoller roller, @NonNull final Player gm) {
        return place(scene, caster, planOrgulho(caster, roller), gm);
    }

    @Override
    public Invocation invokeLaboratorio(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                        @NonNull final DiceRoller roller, @NonNull final Player gm) {
        return place(scene, caster, planLaboratorio(caster, roller), gm);
    }

    @Override
    public Invocation awakenAnciente(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                     @NonNull final Player gm) {
        return place(scene, caster, planAnciente(caster), gm);
    }

    @Override
    public SummonSpawner raiseTotem(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                    @NonNull final Player gm) {
        return scene.addSummonSpawner(caster, ROUNDS,
                () -> planTotemPredator(caster).creatures().get(0).spawn(gm), ROUNDS);
    }

    @Override
    public void blessFromTotem(@NonNull final List<? extends CombatantSheet> animals) {
        for (CombatantSheet animal : animals) {
            animal.applyEffect(new TemporaryBonus(ModifierType.ACTION_POINTS, TOTEM_ACTION_POINTS, 1, TOTEM_DE_GAEA));
            animal.applyEffect(new TemporaryBonus(ModifierType.STRENGTH_BONUS, TOTEM_ATTRIBUTE_BONUS, 1, TOTEM_DE_GAEA));
            animal.applyEffect(new TemporaryBonus(ModifierType.VIGOR_BONUS, TOTEM_ATTRIBUTE_BONUS, 1, TOTEM_DE_GAEA));
            animal.applyEffect(new TemporaryBonus(ModifierType.SKILL_DIFFICULTY_REDUCTION, TOTEM_DIFFICULTY_REDUCTION, 1,
                    TOTEM_DE_GAEA));
        }
    }

    @Override
    public Subordinate captivate(@NonNull final CombatantSheet caster, @NonNull final CombatantSheet animal,
                                 @NonNull final SubordinateBenefit benefit, final boolean falsaMatilha,
                                 final SceneContext sceneContext) {
        return captivate(caster, animal, animal.getCreatureType(), benefit, falsaMatilha, sceneContext);
    }

    @Override
    public Subordinate captivate(@NonNull final CombatantSheet caster, @NonNull final CombatantSheet animal,
                                 final CreatureType animalType, @NonNull final SubordinateBenefit benefit,
                                 final boolean falsaMatilha, final SceneContext sceneContext) {
        if (animalType != CreatureType.ANIMAL) {
            throw new IllegalOperationException(CAPTIVATE_REQUIRES_ANIMAL);
        }
        if (benefit.getGrade() != SubordinateGrade.CAVALEIRO && benefit.getGrade() != SubordinateGrade.TORRE) {
            throw new IllegalOperationException(CAPTIVATE_GRADE_NOT_ALLOWED);
        }
        if (animal.isAffectedUntilRest(CATIVAR_ANIMAL)) {
            throw new IllegalOperationException(CAPTIVATE_NEEDS_REST);
        }
        int rounds = falsaMatilha ? (CATIVAR_ROUNDS + 1) / 2 : CATIVAR_ROUNDS;
        Subordinate subordinate = new Subordinate(benefit, falsaMatilha, CATIVAR_ANIMAL, animal.getId(), rounds);
        subordinateService.command(caster, subordinate, sceneContext);
        // "sem que antes passe por um Descanso" — any Descanso.
        animal.markAffectedUntilRest(CATIVAR_ANIMAL, RestType.MINIMO);
        return subordinate;
    }
}
