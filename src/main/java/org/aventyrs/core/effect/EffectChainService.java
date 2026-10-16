package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Optional;

/**
 * Whether a Corrente de Efeitos actually hits/affects a target — the roll that
 * triggered it must surpass a required challenge number by a margin: {@link
 * #BASE_REQUIRED_MARGIN} normally, {@link #RESOLUTO_REQUIRED_MARGIN} if the target
 * holds {@link AutocontroleAdvantage#RESOLUTO}. Every path that fires a Corrente reads the full four-argument
 * {@link #getRequiredMargin(CombatantSheet, InitiativePosition, CombatantSheet, InitiativePosition)} —
 * {@code AttackDelivery}, {@code AttackReceiver} and {@code SpellCastingService#isEffectChainTriggered} — comparing
 * the roll's margin directly; {@link #hits} stays for a caller holding a bare challenge number, and has none yet.
 */
public interface EffectChainService {
    /** Resistência à Corrente de Efeitos' "valor padrão é 5" — {@link org.aventyrs.core.character.Resistance#EFFECT_CHAIN_BASE}. */
    int BASE_REQUIRED_MARGIN = org.aventyrs.core.character.Resistance.EFFECT_CHAIN_BASE;

    /** Resoluto's enumerated +2 on top of the base. */
    int RESOLUTO_REQUIRED_MARGIN = BASE_REQUIRED_MARGIN + 2;

    /**
     * {@link #RESOLUTO_REQUIRED_MARGIN} when {@code target} holds {@link
     * AutocontroleAdvantage#RESOLUTO}, {@link #BASE_REQUIRED_MARGIN} otherwise.
     */
    int getRequiredMargin(Character target);

    /**
     * The margin an attack by attacker must clear defender's Defesa by for a Corrente to land —
     * {@link #getRequiredMargin(Character)} raised by the defender's own Talentos and lowered by the
     * attacker's ({@code Feat#resolveEffectChainResistanceIncrease}/{@code
     * #resolveTargetEffectChainResistanceReduction}), each read against where its holder stands in
     * the order of play ({@code PeritoFeat#GRANDE_ANALISTA_TATICO}'s "Enquanto você for o último a
     * agir"). A position is {@link InitiativePosition#UNKNOWN} when no live Scene says. Never below 0.
     * Either sheet {@code null} contributes nothing.
     */
    default int getRequiredMargin(final CombatantSheet attacker, final InitiativePosition attackerPosition,
                                  final CombatantSheet defender, final InitiativePosition defenderPosition) {
        // Sorte a Zero's Atrair a Morte and Autocontrole a Zero's Vontade Fraca: every Corrente that beats its Defesas
        // lands, "sem a margem de 5" (core 0.0.82).
        if (defender.hasEgoSetback(org.aventyrs.core.ego.EgoSetback.ATRAIR_A_MORTE)
                || defender.hasEgoSetback(org.aventyrs.core.ego.EgoSetback.VONTADE_FRACA)) {
            return 0;
        }
        Character target = defender.getCharacter();
        int margin = getRequiredMargin(target);
        margin += target.getFeats().stream()
                .mapToInt(feat -> feat.resolveEffectChainResistanceIncrease(target, defenderPosition))
                .sum();
        // Anatomia de Morto-Vivo: "Resistência a Correntes de Efeitos +2" (core 0.1.6.23).
        if (defender.hasUndeadAnatomy()) {
            margin += org.aventyrs.core.monster.UndeadAnatomy.EFFECT_CHAIN_RESISTANCE;
        }
        // Habilidades Monstruosas: Fisiologia Estranha's +2, Forma Verdadeira's +3 (core 0.1.7.2).
        margin += org.aventyrs.core.monster.MonstrousAbilityGrant.effectChainResistanceIncrease(defender);
        if (attacker != null) {
            Character source = attacker.getCharacter();
            margin -= source.getFeats().stream()
                    .mapToInt(feat -> feat.resolveTargetEffectChainResistanceReduction(source, attackerPosition))
                    .sum();
            margin -= org.aventyrs.core.monster.MonstrousAbilityGrant.targetEffectChainResistanceReduction(attacker);
        }
        return Math.max(0, margin);
    }

    /**
     * {@code challengeLevel}, if present, is shifted easier by {@code
     * difficultyReduction} steps and its {@link DifficultyLevel#getBaseValue()} taken;
     * {@code variableBonus} is then added on top of that (already-reduced) value.
     * {@code challengeLevel} absent means the required challenge number is just {@code
     * variableBonus} alone — {@link DifficultyLevel}'s 8 tiers are coarse, so this is
     * what lets a caller land on an exact target number rather than being stuck on one
     * of those 8 values.
     */
    int getRequiredChallengeNumber(Optional<DifficultyLevel> challengeLevel, int difficultyReduction, int variableBonus);

    /**
     * Whether {@code attackRollTotal} clears {@link #getRequiredChallengeNumber} by at
     * least {@link #getRequiredMargin(Character)} — reaching the margin exactly counts
     * as surpassing it.
     */
    boolean hits(int attackRollTotal, Character target, Optional<DifficultyLevel> challengeLevel,
                 int difficultyReduction, int variableBonus);
}
