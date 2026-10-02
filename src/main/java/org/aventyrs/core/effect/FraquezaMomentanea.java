package org.aventyrs.core.effect;

import java.util.List;

import lombok.Getter;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;

/**
 * Serra-Pernas's Corrente de Efeitos Alternativa (core 0.1.0): "Em seu próximo Turno o alvo sofre Desvantagem em suas
 * rolagens de Perícias baseadas em Força e Destreza. Adicionalmente o alvo é amaldiçoado durante toda a Duração desta
 * magia." The caster declares it at the cast instead of Espremer (table ruling, 2026-10-02).
 *
 * <ul>
 *   <li>The Desvantagem is {@link ModifierType#PHYSICAL_SKILL_ROLL_BONUS} −2 for one Rodada, the shape {@link
 *       EnrijecerMusculatura} uses: it counts down at the end of the target's own Turn, so it covers the next one. ⚠️
 *       Landed during the target's own Turn (a Reação), it would end with that Turn instead.</li>
 *   <li>The curse is {@link ConditionType#AMALDICOADO} for the Magia's Duração — a marker the catalogue's "considerado
 *       Amaldiçoado" clauses read, carrying no effect of its own.</li>
 * </ul>
 */
@Getter
public class FraquezaMomentanea extends AbstractEffect implements EffectChain {

    /** "Em seu próximo Turno". */
    static final int WEAKNESS_ROUNDS = 1;

    private final Spell spell;
    private final CombatantSheet caster;
    private final int rounds;

    public FraquezaMomentanea(final Spell spell, final CombatantSheet caster, final int rounds) {
        this.spell = spell;
        this.caster = caster;
        this.rounds = rounds;
    }

    @Override
    public String getDescription() {
        return spell.getEffectChainDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Blessing weakness = new Blessing(ModifierType.PHYSICAL_SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS,
                WEAKNESS_ROUNDS, TargetScope.SINGLE_TARGET, BodyChangeGrant.sourceOf(spell));
        target.grantBlessing(weakness);
        if (rounds > 0) {
            target.applyCondition(new Condition(ConditionType.AMALDICOADO, rounds, caster));
        }
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .blessings(List.of(weakness)))
                .build();
    }
}
