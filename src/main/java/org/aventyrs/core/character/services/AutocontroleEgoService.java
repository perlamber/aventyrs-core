package org.aventyrs.core.character.services;

import org.aventyrs.core.ego.AutocontroleDefence;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillRoll;

/**
 * Spending Autocontrole (2.5 Ego › Autocontrole; Ego plan Phase 8, core 0.0.81). Each method pays one point of the
 * type the rules text puts it under — a temporary point never buys a permanent effect — through {@code
 * EgoPointsService#useEgoPointsForEffect} (so Determinação Heroica's recovery follows), and refuses with {@code
 * NOT_ENOUGH_EGO_POINTS}, spending nothing, when that point isn't there. No timing gate.
 */
public interface AutocontroleEgoService {

    /** What rides the RA bought for a Rodada — "receber RD, RM ou RE, à sua escolha". */
    enum Protection {
        RD, RM, RE
    }

    /** "Receber RA por 1 Rodada" — one instance, as are the RD/RM/RE that ride it. */
    int PROTECTION_RODADAS = 1;

    /**
     * Temporary: "reduzir à metade a duração de um malefício" — see {@code CombatantSheet#halveEffectDuration}.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NOT_A_RUNNING_EFFECT} (nothing spent)
     */
    void halveMaleficio(CombatantSheet sheet, TemporaryEffect maleficio);

    /**
     * Temporary: "receber RA por 1 Rodada; adicionalmente, durante este tempo, receber RD, RM ou RE, à sua escolha".
     * One instance each ({@code DamageService#DEFAULT_DAMAGE_REDUCTION}), lasting until the holder's next Turn
     * begins (the "por 1 Rodada" ruling). element names the RE's element, and is required for it.
     *
     * @throws IllegalArgumentException for {@code RE} without an element (nothing spent)
     */
    void grantProtection(CombatantSheet sheet, Protection protection, ElementalType element);

    /** Temporary: "recuperar PV, PM e PD como se passasse por um Descanso Longo". Returns the PV recovered. */
    int recoverAsLongRest(CombatantSheet sheet);

    /**
     * Temporary or permanent, by effect: marks the defence roll so {@code AttackReceiver} keeps a Corrente or a
     * critical's effects from landing (see {@link AutocontroleDefence}). Returns the marked roll.
     */
    SkillRoll applyDefence(CombatantSheet sheet, SkillRoll defenceRoll, AutocontroleDefence effect);

    /**
     * Permanent: "remover completamente um efeito ou malefício, tornando-se imune a ele ao longo da Cena" — removed,
     * and its kind refused for the rest of the Cena ({@code CenaImmunity}; table ruling: "that same kind").
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NOT_A_RUNNING_EFFECT} when it isn't held
     *         (nothing spent)
     */
    void removeMaleficio(CombatantSheet sheet, TemporaryEffect maleficio);

    /**
     * Permanent: "reduzir a zero todo o dano sofrido em uma Rodada" — see {@code
     * CombatantSheet#negateDamageThisRound}. Returns the PV given back.
     */
    int negateDamageThisRound(CombatantSheet sheet);

    /** Permanent: "recuperar PM e PD como se passasse por um Descanso Total, e todos os PV". Returns the PV recovered. */
    int recoverAsTotalRest(CombatantSheet sheet);
}
