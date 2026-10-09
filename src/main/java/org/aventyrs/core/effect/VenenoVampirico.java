package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.sheet.VampiricVenom;

/**
 * Veneno Vampírico — "O Multiplicador de PV do alvo é reduzido em -1 e ele sofre 2 pontos de Dano Mágico Profano,
 * então o alvo sofre 2 Pontos de Dano Mágico Profano por Rodada. Este é um efeito de Envenenamento, possui Roubo de
 * Vida 1 e tem por Duração 2 Rodadas." A Corrente de Efeitos (the Serpente Espinhosa's Forma Metamórfica, {@code
 * MetamorfoseDraculeaFeat}).
 *
 * <p>Three parts, all for {@link #ROUNDS} Rodadas:
 * <ul>
 *   <li>the Multiplicador loss <i>is</i> the {@link ConditionType#ENVENENADO} Malefício ("um efeito de
 *       Envenenamento"), whose -1 {@code LIFE_MULTIPLIER} {@code HitPointsService} reads;</li>
 *   <li>{@link #DAMAGE} at once, then {@link #DAMAGE} per Rodada — a {@link VampiricVenom};</li>
 *   <li>Roubo de Vida 1 — ⚠️ read as belonging to each of the Corrente's own hits: the attacker recovers 1PV whenever
 *       its damage lands, at once and on each tick, never more than landed.</li>
 * </ul>
 * ⚠️ Its damage lands after mitigation, as every Corrente stage's does here (see {@link Rugido}): RM does not reach it.
 * "Profano" is a classification nothing reads yet (see {@link ToqueSombrio}).
 */
public class VenenoVampirico extends AbstractEffect implements EffectChain {

    /** "2 pontos de Dano Mágico Profano", at once and per Rodada. */
    public static final int DAMAGE = 2;

    /** "Roubo de Vida 1". */
    public static final int LIFE_STEAL = 1;

    /** "tem por Duração 2 Rodadas". */
    public static final int ROUNDS = 2;

    private final CombatantSheet attacker;

    /** @param attacker who recovers the Roubo de Vida */
    public VenenoVampirico(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "O Multiplicador de PV do alvo é reduzido em -1 e ele sofre 2 pontos de Dano Mágico Profano, então o "
                + "alvo sofre 2 Pontos de Dano Mágico Profano por Rodada. Este é um efeito de Envenenamento, possui "
                + "Roubo de Vida 1 e tem por Duração 2 Rodadas.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.ENVENENADO, ROUNDS, null, java.util.List.of(ConditionType.ConditionEffect.lifeMultiplierLoss(1))));
        int landed = VampiricVenom.strike(target, attacker, DAMAGE, LIFE_STEAL);
        target.applyEffect(new VampiricVenom(attacker, DAMAGE, LIFE_STEAL, ROUNDS));
        return reportChain(InteractionResult.builder()
                .resourceLossValue(landed)
                .resourceLossType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
