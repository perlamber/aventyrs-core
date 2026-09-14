package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The applicable form of a Magia's {@code Spell#getCleansedConditions()} — lifting the Malefícios
 * it names from its target. The mirror of {@link SpellHealingEffect} on the other ramificação, and
 * parameterized for the same reason: a cleansing branch is one shape whose reach widens rung by
 * rung, not a class per Magia.
 *
 * <pre>
 * Broto        Toque Curativo    Doença, Veneno
 * Muda         Remover Maldição  Maldição
 * Emergente    Exorcizar         Maldição, Doença, Possessão
 * Florescente  Corpo Fechado     every Malefício
 * </pre>
 *
 * <p>Removal goes through {@link CombatantSheet#removeCondition}, which also drops whatever the
 * lifted condition <i>implied</i> — no separate bookkeeping, exactly as removing Caído takes its
 * Desprevenido with it.
 *
 * <p><b>Only what is actually held is reported.</b> {@code removeCondition} is indifferent to a
 * condition the target never had, so the count on the result is computed against {@link
 * CombatantSheet#getActiveConditions} first — a Magia that cleansed nothing is a meaningful
 * outcome for a caller to see, not an error.
 *
 * <p>TODO the immunity half of {@code VidaSpell#CORPO_FECHADO} — "se torna imune à Malefícios
 * enquanto estiver sob efeito de Corpo Fechado" — is not here. Refusing a <i>future</i> condition
 * needs the per-condition immunity CLAUDE.md's Malefício-classification gap names as missing, and
 * it would have to last that Magia's Concentração + 2 Rodadas rather than happening at once.
 */
@Getter
public class ConditionCleansingEffect extends AbstractEffect implements DefensiveEffect {

    private final Spell spell;
    private final Set<ConditionType> cleansedConditions;

    public ConditionCleansingEffect(final Spell spell, final Set<ConditionType> cleansedConditions) {
        this.spell = spell;
        this.cleansedConditions = Set.copyOf(cleansedConditions);
    }

    @Override
    public String getDescription() {
        return spell.getPrimaryEffectDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return applyTo(target, null);
    }

    /**
     * The longest overload, holding all the logic — {@code sceneContext} only reaches {@link
     * CombatantSheet#getActiveConditions}, which needs it to judge a proximity-scoped implication
     * (the fear ladder's "enquanto estiver a até 4UD da origem"). A {@code null} context still
     * lifts every directly-held condition; it can only under-count the implied ones in the report.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext) {
        Set<ConditionType> held = target.getActiveConditions(sceneContext);
        Set<ConditionType> lifted = cleansedConditions.stream()
                .filter(held::contains)
                .collect(Collectors.toUnmodifiableSet());
        cleansedConditions.forEach(target::removeCondition);

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .liftedConditions(lifted))
                .build();
    }
}
