package org.aventyrs.core.magic.invocation;

import lombok.NonNull;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVOCATION_NOT_SUBORDINATE;
import static org.aventyrs.core.util.TranslatableMessages.INVOCATION_TARGET_REQUIRED;

/** The {@link SpellInvocationService} over the two tree services, each unchanged beneath it. */
public class SpellInvocationServiceImpl implements SpellInvocationService {

    /** What both Subordinado Magias offer: a Cavaleiro or a Torre, and its benefit. */
    static final List<SubordinateBenefit> SUBORDINATE_BENEFITS = List.of(SubordinateBenefit.CAVALEIRO_ATTACK,
            SubordinateBenefit.CAVALEIRO_DAMAGE, SubordinateBenefit.TORRE_DEFESAS, SubordinateBenefit.TORRE_RA);

    private final NatureInvocationService nature;
    private final UndeadInvocationService undead;

    public SpellInvocationServiceImpl() {
        this(new NatureInvocationServiceImpl(), new UndeadInvocationServiceImpl());
    }

    public SpellInvocationServiceImpl(final NatureInvocationService nature, final UndeadInvocationService undead) {
        this.nature = nature;
        this.undead = undead;
    }

    @Override
    public int extraManaCost(@NonNull final SpellInvocation invocation, @NonNull final CombatantSheet caster,
                             final InvocationChoices choices) {
        InvocationChoices counts = choices == null ? InvocationChoices.NONE : choices;
        return switch (invocation) {
            case PREDADOR_REGIONAL -> nature.planAliado(caster, true).extraManaCost();
            case CANCAO_DE_FLORA -> nature.planCancaoDeFlora(caster, counts.additionalAnimals(),
                    counts.strengthenedAnimals(), false).extraManaCost();
            case ACOUGUEIRO -> UndeadInvocationService.BUTCHER_EXTRA_MANA;
            default -> 0;
        };
    }

    @Override
    public InvocationOrder plan(@NonNull final SpellInvocation invocation, @NonNull final InvocationCast cast) {
        CombatantSheet caster = cast.caster();
        return switch (invocation) {
            case CATIVAR_ANIMAL, FALSA_MATILHA, SERVO_CADAVERICO -> {
                requireTarget(cast);
                yield new InvocationOrder.Subordination(SUBORDINATE_BENEFITS);
            }
            case ALIADO -> nature(nature.planAliado(caster, false), cast);
            case PREDADOR_REGIONAL -> nature(nature.planAliado(caster, true), cast);
            case CANCAO_DE_FLORA -> nature(nature.planCancaoDeFlora(caster, cast.choices().additionalAnimals(),
                    cast.choices().strengthenedAnimals(), cast.effectChainTriggered()), cast);
            case EXPERIMENTO -> nature(nature.planExperimento(caster, cast.roller()), cast);
            case ORGULHO -> nature(nature.planOrgulho(caster, cast.roller()), cast);
            case LABORATORIO -> nature(nature.planLaboratorio(caster, cast.roller()), cast);
            case ANCIENTE -> nature(nature.planAnciente(caster), cast);
            case TOTEM_DE_GAEA -> totem(cast);
            case ZUMBI -> undead(undead.planReanimar(caster, requireTarget(cast)), cast);
            case CARNICAL -> undead(undead.planCarnical(caster, requireTarget(cast), false), cast);
            case ACOUGUEIRO -> undead(undead.planCarnical(caster, requireTarget(cast), true), cast);
            case FESTIM, HORDA_FAMINTA -> festim(invocation == SpellInvocation.HORDA_FAMINTA, caster);
        };
    }

    @Override
    public Subordinate subordinate(@NonNull final SpellInvocation invocation, final Scene scene,
                                   @NonNull final CombatantSheet caster, @NonNull final CombatantSheet body,
                                   final CreatureType animalType, @NonNull final SubordinateBenefit benefit,
                                   final boolean effectChainTriggered, final SceneContext sceneContext) {
        return switch (invocation) {
            case CATIVAR_ANIMAL, FALSA_MATILHA -> {
                boolean falsaMatilha = invocation == SpellInvocation.FALSA_MATILHA;
                yield animalType == null
                        ? nature.captivate(caster, body, benefit, falsaMatilha, sceneContext)
                        : nature.captivate(caster, body, animalType, benefit, falsaMatilha, sceneContext);
            }
            case SERVO_CADAVERICO -> undead.servoCadaverico(scene, caster, body, benefit, effectChainTriggered,
                    sceneContext);
            default -> throw new IllegalOperationException(INVOCATION_NOT_SUBORDINATE);
        };
    }

    private static CombatantSheet requireTarget(final InvocationCast cast) {
        if (cast.target() == null) {
            throw new IllegalOperationException(INVOCATION_TARGET_REQUIRED);
        }
        return cast.target();
    }

    /** A nature plan with the caster's Bruxo choices folded in and Potencializar's Rodadas added to its Duração. */
    private InvocationOrder nature(final InvocationPlan authored, final InvocationCast cast) {
        InvocationPlan plan = nature.enhance(authored, cast.caster(), cast.options());
        Integer rounds = plan.rounds() == null ? null : plan.rounds() + cast.extraRounds();
        List<SummonOrder> creatures = plan.creatures().stream()
                .map(creature -> new SummonOrder(creature.getKind().name(), creature.getName(),
                        creature.getConjuradorManaGraduation(), creature.getPowers(), plan.exclusivityGroup(), rounds,
                        plan.concentration(), creature.getEnhancement(), null, null, false))
                .toList();
        return new InvocationOrder.Summons(creatures, plan.enhancement());
    }

    /**
     * An undead plan. Potencializar's Rodadas are not added — a Concentração holds these, and the casting critical
     * never reached them before this service either.
     */
    private static InvocationOrder undead(final UndeadPlan plan, final InvocationCast cast) {
        SizeCategory body = cast.target().getCharacter().getSizeCategory();
        SummonOrder order = new SummonOrder(plan.kind().name(), plan.creature().getName(),
                SummonedMonsterTemplate.manaGraduationOf(cast.caster().getCharacter()), List.of(),
                plan.exclusivityGroup(), plan.rounds(), plan.concentration(), SummonEnhancement.NONE, cast.target(),
                body, plan.butcher());
        return new InvocationOrder.Summons(List.of(order), SummonEnhancement.NONE);
    }

    /** Totem de Gaea: a Predador Regional now and one each Rodada, the totem standing its ROUNDS plus Potencializar's. */
    private InvocationOrder totem(final InvocationCast cast) {
        InvocationPlan plan = nature.enhance(nature.planTotemPredator(cast.caster()), cast.caster(), cast.options());
        NatureSummon predator = plan.creatures().get(0);
        return new InvocationOrder.Spawner(new SpawnerOrder(NatureInvocationService.ROUNDS + cast.extraRounds(),
                predator.getKind().name(), predator.getConjuradorManaGraduation(), List.of(),
                NatureInvocationService.ROUNDS, predator.getEnhancement(), null, null));
    }

    /** Festim dos Mortos: the sarcophagus fills up to the caster's limit, each creature rolled by its rule. */
    private static InvocationOrder festim(final boolean hordaFaminta, final CombatantSheet caster) {
        return new InvocationOrder.Spawner(new SpawnerOrder(FestimDosMortos.ROUNDS, null,
                SummonedMonsterTemplate.manaGraduationOf(caster.getCharacter()), List.of(), null,
                SummonEnhancement.NONE, hordaFaminta ? SpawnerOrder.FESTIM_HORDA : SpawnerOrder.FESTIM,
                SummonLimit.of(caster)));
    }
}
