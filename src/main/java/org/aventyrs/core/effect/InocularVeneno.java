package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Inocular Veneno — a Lacerto creature's Corrente de Efeitos ({@code monster.summon.LacertoPower#INOCULAR_VENENO}),
 * in two strengths:
 * <ul>
 *   <li>Experimento de Lacerto: "O alvo adquire o Malefício Veneno, sofrendo redutor de -1 multiplicador de PV (não
 *       cumulativo), recuperados após um Descanso Longo" — {@link ConditionType#ENVENENADO}, until a Descanso
 *       Longo;</li>
 *   <li>Orgulho de Lacerto: "perde 2 multiplicadores de PV (não cumulativo), recupera 1 ao fim da cena e outro após um
 *       Descanso Longo Verdadeiro" — Envenenado until a {@link RestType#TOTAL} Descanso, plus a second -1 that ends
 *       with the combat.</li>
 * </ul>
 * Not cumulative: Envenenado counts once however many times it is held, and the second -1 is keyed by its source, so
 * another bite renews it.
 */
public class InocularVeneno extends AbstractEffect implements EffectChain {

    /** The source key of the Orgulho's second, Cena-long -1. */
    public static final String SOURCE = "Inocular Veneno";

    private final boolean greater;

    /** @param greater the Orgulho de Lacerto's two-multiplier venom rather than the Experimento's */
    public InocularVeneno(final boolean greater) {
        this.greater = greater;
    }

    @Override
    public String getDescription() {
        return greater
                ? "O alvo adquire o Malefício Veneno e perde 2 multiplicadores de PV (não cumulativo), recupera 1 ao fim "
                + "da cena e outro após um Descanso Longo Verdadeiro."
                : "O alvo adquire o Malefício Veneno, sofrendo redutor de -1 multiplicador de PV (não cumulativo), "
                + "recuperados após um Descanso Longo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyEffectUntilRest(new Condition(ConditionType.ENVENENADO, null, null, java.util.List.of(ConditionType.ConditionEffect.lifeMultiplierLoss(1))), greater ? RestType.TOTAL : RestType.LONGO);
        if (greater) {
            target.applyEffectUntilCombatEnds(TemporaryBonus.openEnded(ModifierType.LIFE_MULTIPLIER, -1, SOURCE));
        }
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
