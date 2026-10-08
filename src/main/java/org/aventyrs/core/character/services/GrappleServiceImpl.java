package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.HandBudget;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Immobilization;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_ALREADY_HELD;
import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_CAPTOR_INCAPACITATED;
import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_REQUIRES_ARMS;
import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_REQUIRES_FREE_HAND;
import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_TARGET_IMMUNE;
import static org.aventyrs.core.util.TranslatableMessages.GRAPPLE_TARGET_TOO_LARGE;
import static org.aventyrs.core.util.TranslatableMessages.HOLD_NOT_ESCAPABLE;
import static org.aventyrs.core.util.TranslatableMessages.NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.OPPOSED_ROLL_UNSUPPORTED;

public class GrappleServiceImpl implements GrappleService {

    private final CharacterSizeService sizeService;

    public GrappleServiceImpl() {
        this(new CharacterSizeServiceImpl());
    }

    public GrappleServiceImpl(final CharacterSizeService sizeService) {
        this.sizeService = sizeService;
    }

    @Override
    public Optional<String> refusalToGrab(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet target,
                                          final SceneContext sceneContext) {
        if (!captor.canMaintainGrapple()) {
            return Optional.of(GRAPPLE_CAPTOR_INCAPACITATED);
        }
        if (target.isHeldAgarradoBy(captor)) {
            return Optional.of(GRAPPLE_ALREADY_HELD);
        }
        if (target.isImmuneToCondition(ConditionType.AGARRADO)) {
            return Optional.of(GRAPPLE_TARGET_IMMUNE);
        }
        if (hasNoArms(captor)) {
            return Optional.of(GRAPPLE_REQUIRES_ARMS);
        }
        if (handsInUse(captor) >= HandBudget.AVAILABLE_HANDS) {
            return Optional.of(GRAPPLE_REQUIRES_FREE_HAND);
        }
        int difference = sizeService.getEffectiveSizeCategory(target).ordinal()
                - sizeService.getEffectiveSizeCategory(captor).ordinal();
        if (difference > MAXIMUM_SIZE_DIFFERENCE) {
            return Optional.of(GRAPPLE_TARGET_TOO_LARGE);
        }
        return Optional.empty();
    }

    @Override
    public GrappleResult grab(@NonNull final CombatantSheet captor, @NonNull final MonsterSheet target,
                              final SceneContext sceneContext, @NonNull final SkillRoll roll) {
        return resolveGrab(captor, target, target.getDefense(DefenseType.PHYSICAL), sceneContext, roll);
    }

    @Override
    public GrappleResult grab(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet target,
                              final int targetPhysicalDefense, final SceneContext sceneContext,
                              @NonNull final SkillRoll roll) {
        // The authored figure, plus what the foe's own Condições do to it — as MonsterSheet#getDefense adds.
        int defense = targetPhysicalDefense
                + target.getConditionBonus(org.aventyrs.core.modifier.ModifierType.DEFESAS, null)
                + target.getConditionBonus(org.aventyrs.core.modifier.ModifierType.PHYSICAL_DEFENSE, null)
                + target.getConditionBonus(org.aventyrs.core.modifier.ModifierType.SKILL_ROLL_BONUS, null);
        return resolveGrab(captor, target, defense, sceneContext, roll);
    }

    private GrappleResult resolveGrab(final CombatantSheet captor, final CombatantSheet target, final int required,
                                      final SceneContext sceneContext, final SkillRoll roll) {
        refusalToGrab(captor, target, sceneContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        ActionCost cost = GRAB_COST.plusSurcharge(captor.getActionPointSurcharge(sceneContext));
        Rolled rolled = roll(captor, SkillType.ATAQUE_CORPO_A_CORPO, roll, Manoeuvre.AGARRAR, cost, sceneContext, target);
        boolean took = rolled.reaches(required);
        if (took) {
            hold(captor, target);
        }
        return new GrappleResult(cost, rolled.total(), required, took, rolled.result());
    }

    @Override
    public GrappleResult defendGrab(@NonNull final CombatantSheet defender, @NonNull final MonsterSheet captor,
                                    final SceneContext sceneContext, @NonNull final SkillRoll roll) {
        return defendGrab(defender, captor, foeAttackGd(captor), sceneContext, roll);
    }

    @Override
    public GrappleResult defendGrab(@NonNull final CombatantSheet defender, @NonNull final CombatantSheet captor,
                                    final int captorAttackGd, final SceneContext sceneContext,
                                    @NonNull final SkillRoll roll) {
        // A foe's grab is Favorecido against a Caído/Cego/… defender, as any attack of its is.
        int required = captorAttackGd + defender.getAttackerAttackRollBonus(sceneContext);
        // Named Agarrar so a Vantagem "para resistir a ataques de agarrar" can see what it resists.
        Rolled rolled = roll(defender, SkillType.ESQUIVA_E_APARAR, roll, Manoeuvre.AGARRAR, null, sceneContext, null);
        int total = rolled.total()
                + (captor.favoursDefenceBy(defender, sceneContext) ? Skill.ADVANTAGE_BONUS : 0);
        // Imobilizado/Desacordado still defend, succeeding unless it is a Falha Crítica.
        boolean heldDefence = defender.defendsUnlessCriticalFailure(sceneContext) && !rolled.criticalFailure();
        boolean defended = (!rolled.blindFailed() && total >= required) || heldDefence
                // A foe that could not have grabbed at all (too small, no hand free) takes nobody — judged
                // only on a real foe sheet; an identity copy carries nothing to judge.
                || captor instanceof MonsterSheet && refusalToGrab(captor, defender, sceneContext).isPresent();
        if (!defended) {
            hold(captor, defender);
        }
        return new GrappleResult(null, total, required, defended, rolled.result());
    }

    @Override
    public GrappleResult escape(@NonNull final CombatantSheet held, final SceneContext sceneContext,
                                @NonNull final SkillType attackSkill, @NonNull final SkillRoll roll) {
        Condition hold = heldCondition(held, ConditionType.AGARRADO)
                .orElseThrow(() -> new IllegalOperationException(NOT_HELD));
        return escape(held, sceneContext, attackSkill, roll, foeAttackGd(foeCaptor(hold)));
    }

    @Override
    public GrappleResult escape(@NonNull final CombatantSheet held, final SceneContext sceneContext,
                                @NonNull final SkillType attackSkill, @NonNull final SkillRoll roll,
                                final int captorAttackGd) {
        Condition hold = heldCondition(held, ConditionType.AGARRADO)
                .orElseThrow(() -> new IllegalOperationException(NOT_HELD));
        CombatantSheet captor = hold.getSource();
        if (!attackSkill.isAttackSkill()) {
            throw new IllegalOperationException(NOT_HELD);
        }
        ActionCost cost = ESCAPE_COST.plusSurcharge(held.getActionPointSurcharge(sceneContext));
        Rolled rolled = roll(held, attackSkill, roll, Manoeuvre.LIBERTAR_SE_DO_AGARRAO, cost, sceneContext, captor);
        int required = captorAttackGd;
        boolean free = rolled.reaches(required);
        if (free) {
            held.removeCondition(ConditionType.AGARRADO);
            if (captor != null) {
                captor.stopGrappling(held);
            }
        }
        return new GrappleResult(cost, rolled.total(), required, free, rolled.result());
    }

    @Override
    public GrappleResult holdAgainst(@NonNull final CombatantSheet captor, @NonNull final MonsterSheet escapee,
                                     final SceneContext sceneContext, @NonNull final SkillRoll roll) {
        return holdAgainst(captor, escapee, foeAttackGd(escapee), sceneContext, roll);
    }

    @Override
    public GrappleResult holdAgainst(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet escapee,
                                     final int escapeeAttackGd, final SceneContext sceneContext,
                                     @NonNull final SkillRoll roll) {
        if (!escapee.isHeldAgarradoBy(captor)) {
            throw new IllegalOperationException(NOT_HELD);
        }
        Rolled rolled = roll(captor, SkillType.ATAQUE_CORPO_A_CORPO, roll, Manoeuvre.MANTER_AGARRAO, null,
                sceneContext, escapee);
        int required = escapeeAttackGd;
        boolean kept = rolled.reaches(required);
        if (!kept) {
            release(captor, escapee);
        }
        return new GrappleResult(null, rolled.total(), required, kept, rolled.result());
    }

    @Override
    public GrappleResult escapeImmobilization(@NonNull final CombatantSheet held, final SceneContext sceneContext,
                                              @NonNull final SkillRoll roll) {
        return escapeImmobilization(held, sceneContext, roll, null);
    }

    @Override
    public GrappleResult escapeImmobilization(@NonNull final CombatantSheet held, final SceneContext sceneContext,
                                              @NonNull final SkillRoll roll, final int captorAttackGd) {
        return escapeImmobilization(held, sceneContext, roll, Integer.valueOf(captorAttackGd));
    }

    private GrappleResult escapeImmobilization(final CombatantSheet held, final SceneContext sceneContext,
                                               final SkillRoll roll, final Integer captorAttackGd) {
        if (!held.hasCondition(ConditionType.IMOBILIZADO, sceneContext)) {
            throw new IllegalOperationException(NOT_HELD);
        }
        // Torpor's Imobilizado lives on the Ego setback, not on the sheet: it lasts as long as that does.
        Condition hold = heldCondition(held, ConditionType.IMOBILIZADO)
                .orElseThrow(() -> new IllegalOperationException(HOLD_NOT_ESCAPABLE));
        int required = hold instanceof Immobilization immobilization && immobilization.getEscapeDifficulty() != null
                ? immobilization.getEscapeDifficulty()
                : captorAttackGd != null ? captorAttackGd : foeAttackGd(foeCaptor(hold));
        ActionCost cost = ESCAPE_COST.plusSurcharge(held.getActionPointSurcharge(sceneContext));
        Rolled rolled = roll(held, SkillType.FURTIVIDADE, roll, Manoeuvre.LIBERTAR_SE_DA_IMOBILIZACAO, cost,
                sceneContext, null);
        boolean free = rolled.reaches(required);
        if (free) {
            held.removeCondition(ConditionType.IMOBILIZADO);
        }
        return new GrappleResult(cost, rolled.total(), required, free, rolled.result());
    }

    @Override
    public boolean release(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet target) {
        boolean held = target.isHeldAgarradoBy(captor);
        if (held) {
            target.removeCondition(ConditionType.AGARRADO);
        }
        return captor.stopGrappling(target) || held;
    }

    @Override
    public List<CombatantSheet> releaseAllHeldBy(@NonNull final CombatantSheet captor) {
        List<CombatantSheet> freed = new ArrayList<>(captor.getGrappledTargets());
        freed.forEach(target -> release(captor, target));
        return freed;
    }

    // --- internals -----------------------------------------------------------------------------

    private static void hold(final CombatantSheet captor, final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.AGARRADO, null, captor));
        if (target.isHeldAgarradoBy(captor)) {
            captor.startGrappling(target);
        }
    }

    /** "Sem braços" — a Membro Ausente refusing the Perícia a grab is made with, whatever else is held. */
    private static boolean hasNoArms(final CombatantSheet captor) {
        Character character = captor.getCharacter();
        return character != null && character.getFeats().stream()
                .anyMatch(feat -> feat.preventsSkillUse(SkillType.ATAQUE_CORPO_A_CORPO, character));
    }

    /** Hands taken by what is drawn, an equipped Escudo, and every hold already kept. */
    private static int handsInUse(final CombatantSheet captor) {
        Character character = captor.getCharacter();
        int items = character == null ? 0
                : HandBudget.handsUsed(character.getDrawnWeapons())
                + (int) character.getEquipment().stream()
                        .filter((Item item) -> item.getCategory() == ItemCategory.SHIELD)
                        .count();
        return items + captor.getGrappledTargets().size();
    }

    /** The directly held Condição of type, if any — never one an Ego setback derives. */
    private static Optional<Condition> heldCondition(final CombatantSheet sheet, final ConditionType type) {
        return sheet.getHeldConditions().stream()
                .filter(condition -> condition.getType() == type)
                .findFirst();
    }

    /** The foe holding this — a contest with a player captor has no resolution yet. */
    private static MonsterSheet foeCaptor(final Condition hold) {
        if (hold.getSource() instanceof MonsterSheet foe) {
            return foe;
        }
        throw new IllegalOperationException(OPPOSED_ROLL_UNSUPPORTED);
    }

    private static int foeAttackGd(final MonsterSheet foe) {
        return foe.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).getValue();
    }

    private static Rolled roll(final CombatantSheet roller, final SkillType skill, final SkillRoll roll,
                               final Manoeuvre manoeuvre, final ActionCost cost, final SceneContext sceneContext,
                               final CombatantSheet opponent) {
        SkillRoll named = manoeuvre == null ? roll : roll.asManoeuvre(manoeuvre, cost);
        InteractionResult result = SkillInteractionFactory.create(skill)
                .applyTo(roller, sceneContext, named, skill.isAttackSkill() ? opponent : null, null);
        return new Rolled(result, result.getSkillRollBonus() + named.getTotal());
    }

    /** A resolved roll and its total. A tie reaches — the GD convention every roll here keeps. */
    private record Rolled(InteractionResult result, int total) {
        boolean blindFailed() {
            return Boolean.TRUE.equals(result.getBlindCheckFailed());
        }

        boolean criticalFailure() {
            return result.getCriticalResult() != null && result.getCriticalResult().isCriticalFailure();
        }

        boolean reaches(final int required) {
            return !blindFailed() && total >= required;
        }
    }
}
