package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;

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

    /**
     * Fully wired: {@link #resolveEgoSpendRecovery} below is resolved and applied by {@code
     * org.aventyrs.core.character.services.EgoPointsService#useEgoPointsForEffect}, which
     * spends and recovers in one step so the recovery can't be forgotten.
     *
     * <p>The 1d6 arrives <strong>already rolled</strong> from the caller — this core never
     * rolls dice — and one roll covers all three pools, per the rules text's single
     * "+1d6PV, PM e PD". "Se o ponto for permanente" reads off {@link
     * EgoPointSpend#getType()}, which is precisely why a spend reports which pool it drew from.
     *
     * <p>It fires only on a <em>deliberate</em> use, never on {@code
     * org.aventyrs.core.effect.Primor} draining a victim — see {@link
     * EgoAdvantage#resolveEgoSpendRecovery}'s own javadoc for why that distinction lives in the
     * service entry point rather than in {@code CombatantSheet#spendEgoPoints}.
     */
    DETERMINACAO_HEROICA("Usar pontos de Autocontrole para qualquer efeito adicionalmente " +
            "recupera +1d6PV, PM e PD; se o ponto for permanente, o valor recuperado é " +
            "dobrado.") {
        @Override
        public int resolveEgoSpendRecovery(final EgoPointSpend spend, final int rolledValue) {
            if (spend.getValue() <= 0) {
                return 0;
            }
            return spend.getType() == EgoPointType.PERMANENT
                    ? rolledValue * PERMANENT_POINT_RECOVERY_MULTIPLIER
                    : rolledValue;
        }
    },


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

    // The amount is real, tested data — resolveExtraSessionEgoRecovery below, read by
    // EgoPointsService#getExtraSessionRecovery and applied by #applySessionRecovery.
    // The trigger is deliberately outside this core: a Narrador ends a session by pressing a
    // button, which the consumer routes to EgoPointsService#applySessionRecovery(Map). No core
    // boundary exists by design — a session ends when the table says so. The identical shape
    // SorteAdvantage#DILETO_DE_TYKHE's own clause has.
    MOTIVACAO_DE_MOSES("Você recupera 1 ponto de Autocontrole temporário adicional por " +
            "sessão de jogo.") {
        @Override
        public int resolveExtraSessionEgoRecovery() {
            return EXTRA_SESSION_EGO_RECOVERY;
        }
    };

    /** MOTIVACAO_DE_MOSES's own extra temporary Autocontrole point per game session. */
    private static final int EXTRA_SESSION_EGO_RECOVERY = 1;

    /** DETERMINACAO_HEROICA's own "se o ponto for permanente, o valor recuperado é dobrado". */
    private static final int PERMANENT_POINT_RECOVERY_MULTIPLIER = 2;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.AUTOCONTROLE;
    }
}
