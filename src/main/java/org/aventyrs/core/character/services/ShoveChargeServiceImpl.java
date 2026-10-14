package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.effect.LancarAoChao;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ACTION_PREVENTED_BY_CONDITION;
import static org.aventyrs.core.util.TranslatableMessages.CHARGE_AFTER_REPOSITION;
import static org.aventyrs.core.util.TranslatableMessages.MOVEMENT_PREVENTED;

public class ShoveChargeServiceImpl implements ShoveChargeService {

    private final ChargeService chargeService;
    private final EffectChainService effectChainService;

    public ShoveChargeServiceImpl() {
        this(new ChargeServiceImpl(), new EffectChainServiceImpl());
    }

    public ShoveChargeServiceImpl(final ChargeService chargeService, final EffectChainService effectChainService) {
        this.chargeService = chargeService;
        this.effectChainService = effectChainService;
    }

    @Override
    public ActionCost getCost(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        return chargeService.getActionPointCost(sheet.getCharacter())
                .plusSurcharge(sheet.getActionPointSurcharge(sceneContext));
    }

    @Override
    public Optional<String> refusal(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        if (sheet.isMovementPrevented(sceneContext)) {
            return Optional.of(MOVEMENT_PREVENTED);
        }
        if (sheet.isActionPrevented(ActionKind.ATTACK, sceneContext)) {
            return Optional.of(ACTION_PREVENTED_BY_CONDITION);
        }
        if (sheet.getRepositionsTakenThisRound() > 0) {
            return Optional.of(CHARGE_AFTER_REPOSITION);
        }
        return Optional.empty();
    }

    @Override
    public ShoveResult shove(@NonNull final CombatantSheet attacker, @NonNull final CombatantSheet target,
                             final int targetDefense, final SceneContext sceneContext, @NonNull final SkillRoll roll) {
        refusal(attacker, sceneContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        ActionCost cost = getCost(attacker, sceneContext);
        attacker.consumeMovementThisRound();
        ManoeuvreRolls.Rolled rolled = ManoeuvreRolls.roll(attacker, SkillType.ATAQUE_CORPO_A_CORPO, roll,
                Manoeuvre.ENCONTRAO, cost, sceneContext, target);
        boolean hit = rolled.reaches(targetDefense);
        if (!hit) {
            TemporaryBonus penalty = chargeService.applyOutcome(attacker, false);
            return new ShoveResult(cost, rolled.total(), targetDefense, false, 0, false, penalty, rolled.result());
        }
        int margin = rolled.total() - targetDefense;
        boolean knockedDown = false;
        if (margin >= effectChainService.getRequiredMargin(attacker, null, target, null)) {
            new LancarAoChao(attacker).applyTo(target);
            knockedDown = target.hasCondition(ConditionType.CAIDO, null);
        }
        return new ShoveResult(cost, rolled.total(), targetDefense, true,
                rolled.criticalSuccess() ? CRITICAL_PUSH_UD : PUSH_UD, knockedDown, null, rolled.result());
    }
}
