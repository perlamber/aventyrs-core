package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * The Vantagem de Autocontrole chosen once at character creation — available only to
 * characters whose Autocontrole base reached {@value
 * org.aventyrs.core.character.services.CharacterCreationService#EGO_ADVANTAGE_MIN_BASE}
 * through the creation-time point distribution (see {@link
 * org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}).
 * Reaching that base any other way (Talentos, Títulos Aventyrs, other Habilidades) never
 * grants access to this choice, and it is never lost if Autocontrole later drops below it.
 */
@Getter
@AllArgsConstructor
public enum AutocontroleAdvantage implements EgoAdvantage {

    // TODO: +1d6 PV/PM/PD recovery (doubled when the point spent was permanent) whenever
    // Autocontrole points are spent for any effect — no Autocontrole-spending system or
    // dice-based recovery integration exists yet.
    DETERMINACAO_HEROICA("Usar pontos de Autocontrole para qualquer efeito adicionalmente " +
            "recupera +1d6PV, PM e PD; se o ponto for permanente, o valor recuperado é " +
            "dobrado."),


    // The Skill -> Damage -> EffectChain -> CriticalEffect pipeline now has real
    // interfaces (org.aventyrs.core.effect.Effect/EffectChain/CriticalEffect), a
    // concrete DamageInteraction, and — for RESOLUTO's own margin math specifically — a
    // real, tested org.aventyrs.core.effect.EffectChainService#getRequiredMargin, which
    // reads this exact field (Character#getAutocontroleAdvantage()) to return 7 instead
    // of 5. See that package's own package-info.

    // TODO: RESOLUTO's margin math is real (EffectChainService), but nothing triggers
    // it yet — no concrete Corrente de Efeitos implementation exists to actually call
    // EffectChainService#hits(...) when one lands.
    RESOLUTO("Correntes de Efeitos, para te afetar, precisam superar suas Defesas em 7, ao " +
            "invés de 5."),

    // TODO: grants +1 additional temporary Autocontrole point recovered per game session —
    // no game-session tracking system exists yet.
    MOTIVACAO_DE_MOSES("Você recupera 1 ponto de Autocontrole temporário adicional por " +
            "sessão de jogo.");

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.AUTOCONTROLE;
    }
}
