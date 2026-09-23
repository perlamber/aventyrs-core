package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;

/**
 * Guilhotina — the <b>attacker</b>'s reward: Vantagem on attack and dano rolls for 2 Rodadas (Maior)
 * / 1 (Menor), and "a Margem Crítica Menor dos seus ataques aumenta em +2 / +1 número (efeitos
 * cumulativos)". The Vantagem is three sourced Blessings (both Perícias de Ataque and the dano roll)
 * that a second Guilhotina renews; the margin is a {@code LESSER_CRITICAL_MARGIN} Blessing allowed to
 * stack without limit, read by {@code CriticalServiceImpl}. Nothing lands with no attacker named.
 */
public class Guilhotina extends AbstractCriticalEffect {


    public Guilhotina(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.GUILHOTINA;
    }

    @Override
    protected String majorDescription() {
        return "Receba Vantagem em rolagens de Ataque e Dano por 2 Rodadas, a Margem Crítica Menor dos seus ataques aumenta em +2 números (efeitos cumulativos).";
    }

    @Override
    protected String minorDescription() {
        return "Receba Vantagem em rolagens de Ataque e Dano por 1 Rodada, a Margem Crítica Menor dos seus ataques aumenta em +1 número (efeitos cumulativos).";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        CombatantSheet attacker = attacker();
        if (attacker == null) {
            return InteractionResult.builder();
        }
        int rounds = pick(2, 1);
        String source = getType().name();
        attacker.grantBlessing(new Blessing(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS,
                rounds, TargetScope.SELF, source));
        attacker.grantBlessing(new Blessing(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS,
                rounds, TargetScope.SELF, source));
        attacker.grantBlessing(new Blessing(ModifierType.DAMAGE_ROLL_BONUS, Skill.ADVANTAGE_BONUS,
                rounds, TargetScope.SELF, source));
        attacker.grantBlessing(new Blessing(ModifierType.LESSER_CRITICAL_MARGIN, pick(2, 1), rounds,
                TargetScope.SELF, source, null, Integer.MAX_VALUE));
        return InteractionResult.builder();
    }
}
