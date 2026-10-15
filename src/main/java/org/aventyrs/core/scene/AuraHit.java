package org.aventyrs.core.scene;

import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.TemporaryEgoPenalty;

import java.util.List;

/**
 * One target's share of a {@link MonsterAura}'s pulse (core 0.1.7.4), computed by {@code Scene#pulseMonsterAuras} and
 * applied by whoever controls the target ({@link #applyTo}) — the same split as {@code ElementalDischarge#dealTo},
 * since the client owning a sheet is what changes it.
 *
 * @param target           who it lands on
 * @param damage           raw damage before the target's own mitigation
 * @param descriptor       its kind, or {@code null} for none
 * @param heal             PV the target recovers
 * @param conditions       Condições it takes
 * @param conditionRounds  their Duração
 * @param autocontroleLoss temporary Autocontrole lost until a Descanso
 * @param possessIfEmptied whether it is Possuído once its Autocontrole is at zero
 * @param pushUd           how far it is pushed back from the holder (the caller moves the token)
 */
public record AuraHit(@NonNull CombatantSheet target, int damage, DamageDescriptor descriptor, int heal,
                      @NonNull List<ConditionType> conditions, int conditionRounds, int autocontroleLoss,
                      boolean possessIfEmptied, int pushUd) {

    /** Applies everything but the push. Returns the PV the target lost. */
    public int applyTo(final CombatantSheet holder, final SceneContext context, final DamageService damageService) {
        int before = target.getDamageTaken();
        if (damage > 0 && descriptor != null) {
            damageService.applyDamage(target, context, descriptor, holder, damage, false);
        }
        if (heal > 0) {
            target.heal(heal);
        }
        for (ConditionType type : conditions) {
            if (!target.isImmuneToCondition(type)) {
                target.applyCondition(new Condition(type, conditionRounds, holder));
            }
        }
        if (autocontroleLoss > 0) {
            target.applyEffectUntilRest(new TemporaryEgoPenalty(EgoDomain.AUTOCONTROLE, autocontroleLoss, null),
                    RestType.MINIMO);
        }
        if (possessIfEmptied && target.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE) <= 0
                && !target.isImmuneToCondition(ConditionType.POSSESSAO)) {
            target.applyCondition(new Condition(ConditionType.POSSESSAO, conditionRounds, holder));
        }
        return Math.max(0, target.getDamageTaken() - before);
    }
}
