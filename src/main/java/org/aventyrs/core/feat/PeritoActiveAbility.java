package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.SourcedState;
import org.aventyrs.core.sheet.TemporaryEffect;

/**
 * The two Perito states bought "ao custo de 1PD por Rodada" — {@link PeritoFeat#CRIANCA_DO_MAR}'s
 * breathing underwater and {@link PeritoFeat#REI_DA_MONTANHA}'s clinging to walls and ceilings. Each
 * activation through {@code ActiveAbilityService#activate} spends 1PD and puts a {@link SourcedState}
 * on the holder for one Rodada; keeping it up is activating it again as the next Rodada's PD falls
 * due. The Talento's own hook asks {@link #isActiveOn} whether it is running.
 *
 * <p>An enum because {@code Character#getActiveAbilities()} aggregates held Talentos live and {@code
 * activate} matches by {@code ==} — each constant is the stable singleton that requires.
 *
 * <p>⚠️ <b>The Tempo de Ativação is an inference</b>: both clauses price the state in PD and say
 * nothing about an action, so it is read as an Ação Livre — the same reading this core gives any
 * upkeep a clause states only in a resource.
 */
public enum PeritoActiveAbility implements ActiveAbility {

    /** Criança do Mar — "Você pode respirar na água por um curto período, ao custo de 1PD por Rodada." */
    GUELRAS,

    /** Rei da Montanha — "grudar em paredes e tetos … ao custo de 1PD por Rodada." */
    ADERENCIA;

    /** "ao custo de 1PD por Rodada". */
    private static final int DETERMINATION_POINT_COST = 1;

    /** One Rodada per activation. */
    private static final int DURATION_IN_ROUNDS = 1;

    /** Whether this state is currently running on holder — {@code false} for a {@code null} holder. */
    public boolean isActiveOn(final CombatantSheet holder) {
        return holder != null && holder.hasEffectFrom(source());
    }

    private String source() {
        return "PERITO_" + name();
    }

    @Override
    public String getDescription() {
        // Read lazily: PeritoFeat's constants hold these singletons, so neither enum may reach the
        // other while it is being initialised.
        return (this == GUELRAS ? PeritoFeat.CRIANCA_DO_MAR : PeritoFeat.REI_DA_MONTANHA).getDescription();
    }

    @Override
    public ActionCost getActionPointCost() {
        return ActionCost.FREE_ACTION;
    }

    @Override
    public int getMagicPointCost() {
        return 0;
    }

    @Override
    public int getDeterminationPointCost() {
        return DETERMINATION_POINT_COST;
    }

    @Override
    public int getDurationInRounds() {
        return DURATION_IN_ROUNDS;
    }

    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return new SourcedState(source(), DURATION_IN_ROUNDS);
    }
}
