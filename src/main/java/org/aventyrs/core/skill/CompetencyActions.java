package org.aventyrs.core.skill;

import lombok.NonNull;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.TrainedCompanion;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;
import org.aventyrs.core.skill.furtividade.FurtividadeCompetencyAbility;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateService;
import org.aventyrs.core.util.TranslatableMessages;

import java.util.List;
import java.util.Set;

/**
 * The Habilidades de Competência that are <b>actions</b> — a roll made for a stated purpose, with an effect on
 * someone — rather than passive bonuses: Estudar Defesas, Esconder Outros, Milagreiro, Medicina Alternativa and
 * Aliado da Natureza, Espalhar Reputação (core 0.0.103). As everywhere in this core the caller throws the dice; each method resolves
 * the roll against the GD its rules text names and applies, or reports, the effect.
 *
 * <p>An effect landing on a sheet the caller does not hold (another player's character) is split: the roll half
 * resolves here on the roller's side and the effect half ({@link #esconderOutros(CombatantSheet, Hidden)}, {@link
 * #applyMilagreiro}, {@link CombatantSheet#owePendingRestBonus}) is what the owner's client runs.
 */
public final class CompetencyActions {

    /** Estudar Defesas: "reduz … o GD … em -1 Nível". */
    public static final int STUDIED_DEFENSES_LEVELS = 1;

    /** Estudar Defesas: "por 2 Rodadas". */
    public static final int STUDIED_DEFENSES_ROUNDS = 2;

    /**
     * The rest-scoped ledger key Milagreiro spends on the patient's sheet until its Descanso Longo — a {@code
     * COMPETENCY:} key, so it persists with the patient's other rest-scoped uses ({@link
     * CompetencyUses#restoreRestScopedUses}).
     */
    public static final String MILAGREIRO_KEY = "COMPETENCY:MEDICINA_E_CURA:MILAGREIRO";

    /** The ledger key Aliado da Natureza's once-per-Cena call is counted under. */
    public static final String ALIADO_DA_NATUREZA_KEY = "COMPETENCY:EMPATIA_SELVAGEM:ALIADO_DA_NATUREZA";

    /** Esconder Outros: "nas Especializações Maestria da Ocultação e Infiltrador". */
    public static final Set<SkillSpecialization> ESCONDER_OUTROS_SPECIALIZATIONS =
            Set.of(FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO, FurtividadeSpecialization.INFILTRADOR);

    /** Espalhar Reputação: "a GD da rolagem pode variar conforme a receptividade local (mínimo Médio)". */
    public static final DifficultyLevel ESPALHAR_REPUTACAO_MINIMUM = DifficultyLevel.MEDIUM;

    private CompetencyActions() {
    }

    // --- Estudar Defesas -----------------------------------------------------------------------

    /**
     * "Efetuar rolagens de Esquiva e Aparar ao invés da Perícia de Ataque contra GD Difícil para analisar o
     * oponente … Se for bem-sucedido você reduz o GD para efetuar ataques contra o alvo em -1 Nível por 2
     * Rodadas." Rolled through the plain Esquiva e Aparar interaction (a stated GD, not a Defesa); on a success
     * {@code holder} studies {@code target}.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability
     */
    public static InteractionResult estudarDefesas(@NonNull final CombatantSheet holder,
                                                   @NonNull final CombatantSheet target,
                                                   final SceneContext sceneContext, @NonNull final List<Integer> dice) {
        requireHeld(holder, EsquivaEApararCompetencyAbility.ESTUDAR_DEFESAS);
        InteractionResult result = new EsquivaEApararInteraction()
                .applyTo(holder, sceneContext, SkillRoll.against(dice, DifficultyLevel.HARD));
        if (Boolean.TRUE.equals(result.getSucceeded())) {
            holder.studyDefensesOf(target, STUDIED_DEFENSES_ROUNDS);
        }
        return result;
    }

    // --- Esconder Outros -----------------------------------------------------------------------

    /**
     * The concealment an Esconder Outros roll gives the ally: the tier furtividadeTotal reached as an
     * Especialista (the roll names Maestria da Ocultação or Infiltrador), made one nível easier to see through —
     * "a GD para esta ação aumenta em +1 Nível" (table ruling), keeping the roll's excess.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability, {@code
     *         COMPETENCY_SPECIALIZATION_REQUIRED} for a roll naming neither Especialização
     */
    public static Hidden esconderOutros(@NonNull final CombatantSheet holder, final int furtividadeTotal,
                                        final SkillSpecialization specialization) {
        requireHeld(holder, FurtividadeCompetencyAbility.ESCONDER_OUTROS);
        if (specialization == null || !ESCONDER_OUTROS_SPECIALIZATIONS.contains(specialization)) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_SPECIALIZATION_REQUIRED);
        }
        Hidden rolled = Hidden.fromRoll(furtividadeTotal, true);
        return new Hidden(rolled.getDifficultyLevel().easier(1), rolled.getBonus());
    }

    /** Hides ally behind concealment — the effect half, on the ally's own sheet. */
    public static Hidden esconderOutros(@NonNull final CombatantSheet ally, @NonNull final Hidden concealment) {
        ally.applyCondition(concealment);
        return concealment;
    }

    // --- Milagreiro ----------------------------------------------------------------------------

    /** Whether target may still be treated with Milagreiro — once per target until its Descanso Longo. */
    public static boolean canAttemptMilagreiro(final CombatantSheet target) {
        return target != null && target.getRestScopedUses(MILAGREIRO_KEY) == 0;
    }

    /**
     * Whether a Milagreiro roll that succeeded also triggered its Corrente — Milagre Maior: its margin reached the
     * base Corrente margin. ⚠️ The Resoluto margin is not applied: the patient is not resisting.
     */
    public static boolean milagreMaior(final InteractionResult result) {
        return result != null && Boolean.TRUE.equals(result.getSucceeded()) && result.getMargin() != null
                && result.getMargin() >= EffectChainService.BASE_REQUIRED_MARGIN;
    }

    /**
     * The Milagreiro roll: Medicina e Cura raised to GD Difícil. The effect is {@link #applyMilagreiro}, on the
     * patient's sheet.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability
     */
    public static InteractionResult rollMilagreiro(@NonNull final CombatantSheet healer, final SceneContext sceneContext,
                                                   @NonNull final List<Integer> dice) {
        requireHeld(healer, MedicinaECuraCompetencyAbility.MILAGREIRO);
        return SkillType.MEDICINA_E_CURA.newInteraction()
                .applyTo(healer, sceneContext, SkillRoll.against(dice, DifficultyLevel.HARD));
    }

    /**
     * Milagreiro on patient: marked until its Descanso Longo whatever the outcome ("mesmo que a primeira tentativa
     * tenha falhado"); on a success it recovers the PV of a Descanso Curto, and with Milagre Maior the PM and PD
     * too. Returns the PV recovered, or -1 when patient was already treated (nothing happens).
     */
    public static int applyMilagreiro(@NonNull final CombatantSheet patient, final boolean succeeded,
                                      final boolean milagreMaior) {
        if (!canAttemptMilagreiro(patient)) {
            return -1;
        }
        patient.spendOrdinaryRestScopedUse(MILAGREIRO_KEY, RestType.LONGO);
        if (!succeeded) {
            return 0;
        }
        RestService rest = new RestServiceImpl();
        int before = patient.getDamageTaken();
        patient.heal(rest.getRecoveredHitPoints(patient.getCharacter(), RestType.CURTO),
                new HealingSource(MILAGREIRO_KEY, false, null, null, null));
        if (milagreMaior) {
            patient.recoverMagicPoints(rest.getRecoveredMagicPoints(patient.getCharacter(), RestType.CURTO));
            patient.recoverDeterminationPoints(rest.getRecoveredDeterminationPoints(patient.getCharacter(), RestType.CURTO));
        }
        return before - patient.getDamageTaken();
    }

    // --- Medicina Alternativa ------------------------------------------------------------------

    /**
     * The Medicina Alternativa roll, against GD Médio. On a success the caller rolls the 1d6 (one die for PV, PM
     * and PD alike — table ruling) and owes it to the patient with {@link CombatantSheet#owePendingRestBonus}.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability
     */
    public static InteractionResult rollMedicinaAlternativa(@NonNull final CombatantSheet healer,
                                                            final SceneContext sceneContext,
                                                            @NonNull final List<Integer> dice) {
        requireHeld(healer, MedicinaECuraCompetencyAbility.MEDICINA_ALTERNATIVA);
        return SkillType.MEDICINA_E_CURA.newInteraction()
                .applyTo(healer, sceneContext, SkillRoll.against(dice, DifficultyLevel.MEDIUM));
    }

    // --- Espalhar Reputação -------------------------------------------------------------------

    /**
     * "Você pode fazer uma rolagem de Artes enquanto se apresenta para uma multidão … a GD da rolagem pode variar
     * conforme a receptividade local (mínimo Médio)." The Narrador names the GD; a lower one is raised to Médio.
     * The effect — listeners "mais favoráveis ou neutros" — is the Narrador's to adjudicate (no disposition
     * system); the result's {@code succeeded} is what they read.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability
     */
    public static InteractionResult espalharReputacao(@NonNull final CombatantSheet performer,
                                                      final SceneContext sceneContext,
                                                      @NonNull final List<Integer> dice,
                                                      final DifficultyLevel difficulty) {
        requireHeld(performer, ArtesCompetencyAbility.ESPALHAR_REPUTACAO);
        DifficultyLevel stated = difficulty == null || difficulty.getBaseValue() < ESPALHAR_REPUTACAO_MINIMUM.getBaseValue()
                ? ESPALHAR_REPUTACAO_MINIMUM
                : difficulty;
        return SkillType.ARTES.newInteraction().applyTo(performer, sceneContext, SkillRoll.against(dice, stated));
    }

    // --- Aliado da Natureza --------------------------------------------------------------------

    /**
     * The training roll: Empatia Selvagem against GD Difícil. On a success the creature is added to the trainer's
     * lasting companions ({@code Character#trainCompanion}).
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NOT_HELD} without the ability
     */
    public static InteractionResult trainCompanion(@NonNull final CombatantSheet trainer, final SceneContext sceneContext,
                                                   @NonNull final List<Integer> dice,
                                                   @NonNull final TrainedCompanion companion) {
        requireHeld(trainer, EmpatiaSelvagemCompetencyAbility.ALIADO_DA_NATUREZA);
        InteractionResult result = SkillType.EMPATIA_SELVAGEM.newInteraction()
                .applyTo(trainer, sceneContext, SkillRoll.against(dice, DifficultyLevel.HARD));
        if (Boolean.TRUE.equals(result.getSucceeded())) {
            trainer.getCharacter().trainCompanion(companion);
        }
        return result;
    }

    /** Whether trainer may still call a trained creature this Cena — "apenas um animal treinado … em cada Cena". */
    public static boolean canCallCompanion(final CombatantSheet trainer) {
        return trainer != null && !trainer.getCharacter().getTrainedCompanions().isEmpty()
                && trainer.getCombatCounter(ALIADO_DA_NATUREZA_KEY) == 0;
    }

    /**
     * Calls companion for this Cena: its benefit applies as a Subordinado, replacing a companion called before.
     * ⚠️ "Cena" is kept per combat, the house reading.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NO_USES_LEFT} when one was already called this
     *         Cena, or {@code SubordinateService#command}'s refusals
     */
    public static Subordinate callCompanion(@NonNull final CombatantSheet trainer, @NonNull final TrainedCompanion companion,
                                            final SceneContext sceneContext, @NonNull final SubordinateService subordinates) {
        if (!canCallCompanion(trainer) || !trainer.getCharacter().getTrainedCompanions().contains(companion)) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_ABILITY_NO_USES_LEFT);
        }
        trainer.getRunningEffects().stream()
                .filter(Subordinate.class::isInstance)
                .map(Subordinate.class::cast)
                .filter(held -> EmpatiaSelvagemCompetencyAbility.ALIADO_DA_NATUREZA.name().equals(held.getSource()))
                .map(Subordinate::getId)
                .toList()
                .forEach(id -> subordinates.dismiss(trainer, id));
        Subordinate called = new Subordinate(companion.benefit(), false,
                EmpatiaSelvagemCompetencyAbility.ALIADO_DA_NATUREZA.name(), null, null);
        subordinates.command(trainer, called, sceneContext);
        trainer.incrementCombatCounter(ALIADO_DA_NATUREZA_KEY);
        return called;
    }

    private static void requireHeld(final CombatantSheet holder, final SkillCompetencyAbility ability) {
        if (!SkillCompetencyAbility.allFor(holder.getCharacter(), holder).contains(ability)) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_ABILITY_NOT_HELD);
        }
    }
}
