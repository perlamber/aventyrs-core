package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.effect.Desarmar;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import static org.aventyrs.core.util.TranslatableMessages.ITEM_NOT_EQUIPPED;

public class ObjectAttackServiceImpl implements ObjectAttackService {

    private final EffectChainService effectChainService;

    public ObjectAttackServiceImpl() {
        this(new EffectChainServiceImpl());
    }

    public ObjectAttackServiceImpl(final EffectChainService effectChainService) {
        this.effectChainService = effectChainService;
    }

    @Override
    public ObjectAttackResult strike(@NonNull final CombatantSheet attacker, @NonNull final CombatantSheet target,
                                     final int targetDefense, @NonNull final Item item,
                                     @NonNull final SkillType attackSkill, @NonNull final SkillRoll roll,
                                     final int rawDamage, final SceneContext sceneContext) {
        if (target.getCharacter().getEquipment().stream().noneMatch(held -> held == item)) {
            throw new IllegalOperationException(ITEM_NOT_EQUIPPED);
        }
        ActionCost cost = OBJECT_ATTACK_COST.plusSurcharge(attacker.getActionPointSurcharge(sceneContext));
        ManoeuvreRolls.Rolled rolled = ManoeuvreRolls.roll(attacker, attackSkill, roll, Manoeuvre.ATACAR_OBJETOS,
                cost, sceneContext, target);
        int required = targetDefense + LEVEL_STEP;
        if (!rolled.reaches(required)) {
            return new ObjectAttackResult(cost, rolled.total(), required, false, 0, item.isDestroyed(), false,
                    rolled.result());
        }
        // "reduzidos à metade (efeito de Meio-Dano)", then the item's own reductions.
        // Estilhaçador's Vantagem em Danos contra objetos, on a roll Força governed.
        org.aventyrs.core.character.AttributeDomain governing = rolled.result().getGoverningAttributeDomain();
        int objectBonus = attacker.getCharacter().getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveObjectDamageBonus(governing))
                .sum();
        int landed = item.applyDamage(Math.max(0, rawDamage + objectBonus) / 2);
        boolean disarmed = false;
        if (rolled.total() - required >= effectChainService.getRequiredMargin(attacker, null, target, null)
                && !item.isDestroyed()) {
            new Desarmar(item).applyTo(target);
            disarmed = true;
        }
        return new ObjectAttackResult(cost, rolled.total(), required, true, landed, item.isDestroyed(), disarmed,
                rolled.result());
    }
}
