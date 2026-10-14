package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.List;

/**
 * Envenenar — "Alvo recebe a condição Envenenado. Efeitos do Veneno conforme o ataque." A generic Corrente de
 * Efeitos (V19 20261012).
 *
 * <p><b>The poison is the source's</b> (table ruling, 2026-10-12): whatever grants the Corrente supplies the Veneno's
 * own magnitudes ({@link ConditionType.ConditionEffect}s riding the Condição, as {@link InocularVeneno}'s Multiplicador
 * loss does) and the Descanso that ends it. Given nothing, it is a bare {@link ConditionType#ENVENENADO} — the
 * Fraqueza alone — until the target's next Descanso of any tier ({@link #DEFAULT_UNTIL}). Not cumulative: Envenenado
 * is an Estado-implying Condição counted once however often it is held.
 */
@Getter
public class Envenenar extends AbstractEffect implements EffectChain {

    /** With no Duração from the source, any Descanso clears it. */
    public static final RestType DEFAULT_UNTIL = RestType.MINIMO;

    private final List<ConditionType.ConditionEffect> poisonEffects;
    private final RestType until;

    /** The bare Corrente: Envenenado alone, until any Descanso. */
    public Envenenar() {
        this(List.of(), DEFAULT_UNTIL);
    }

    /**
     * @param poisonEffects the Veneno's own effects, from the attack granting the Corrente
     * @param until         the Descanso that ends it
     */
    public Envenenar(@NonNull final List<ConditionType.ConditionEffect> poisonEffects, @NonNull final RestType until) {
        this.poisonEffects = List.copyOf(poisonEffects);
        this.until = until;
    }

    @Override
    public String getDescription() {
        return "Alvo recebe a condição Envenenado. Efeitos do Veneno conforme o ataque.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyEffectUntilRest(new Condition(ConditionType.ENVENENADO, null, null, poisonEffects), until);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
