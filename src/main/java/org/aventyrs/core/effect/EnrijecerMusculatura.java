package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;

/**
 * Enrijecer Musculatura — "O corpo do alvo se torna mais rígido temporariamente, por isso as rolagens de Perícias
 * Físicas (baseadas em Força e Destreza) dele são feitas em Desvantagem por 1 Rodada. Este é um efeito de
 * Envenenamento." A Corrente de Efeitos ({@code GorgonaFeat#MARCA_DA_MALDICAO}'s Presas Longas).
 *
 * <p>The Desvantagem is a {@link ModifierType#PHYSICAL_SKILL_ROLL_BONUS} of {@link Skill#DISADVANTAGE_MALUS} for
 * {@link #ROUNDS} Rodada, granted as a <b>sourced</b> Blessing, so a second hit renews it rather than reaching -4.
 * It reaches only the roll (a foe's matching GD included), never the Atributo. "Efeito de Envenenamento" classifies
 * it and nothing more: unlike Veneno Vampírico it names no Multiplicador loss, so it is not the {@code ENVENENADO}
 * Malefício.
 */
public class EnrijecerMusculatura extends AbstractEffect implements EffectChain {

    /** "por 1 Rodada". */
    public static final int ROUNDS = 1;

    @Override
    public String getDescription() {
        return "O corpo do alvo se torna mais rígido temporariamente, por isso as rolagens de Perícias Físicas "
                + "(baseadas em Força e Destreza) dele são feitas em Desvantagem por 1 Rodada. Este é um efeito de "
                + "Envenenamento.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.grantBlessing(new Blessing(ModifierType.PHYSICAL_SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, ROUNDS,
                TargetScope.SELF, EnrijecerMusculatura.class.getSimpleName()));
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
