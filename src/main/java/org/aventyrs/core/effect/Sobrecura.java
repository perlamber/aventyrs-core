package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Sobrecura — the second concrete {@link EffectChain}, and the Corrente de Efeitos three Magias of
 * Vida's principal branch name: "O alvo desta magia adicionalmente recupera +1d6+Metade do Foco
 * PV." Revigorar states it in full; Revigorar Maior and Nova Rejuvenescedora name it with no body,
 * relying on Revigorar's own text, so one class serves all three.
 *
 * <p><b>Chained onto the heal, not folded into it.</b> It is a Corrente rather than part of the
 * {@code Efeito:} line, so it is a separate stage a caller decides applied — assembled with
 * {@link AbstractEffect#chainInto} and walked by the drain loop in this package's {@code
 * package-info}:
 *
 * <pre>{@code
 * SpellHealingEffect heal = new SpellHealingEffect(REVIGORAR, REVIGORAR.getHealing().get());
 * heal.chainInto(new Sobrecura(caster, rolledD6));
 * InteractionResult r = target.receiveInteraction(heal);
 * while (r.getNextInteraction() != null) { r = target.receiveInteraction(r.getNextInteraction()); }
 * }</pre>
 *
 * <p><b>The d6 arrives already rolled.</b> This core never rolls dice, so the caller supplies the
 * face — the same boundary {@code DamageInteraction} keeps for its {@code rawDamage} and {@code
 * SpellDamage} keeps by leaving {@code diceCount} to whoever resolves it. The "Metade do Foco"
 * half is deterministic and computed here, from the <b>caster's</b> Foco: unlike the Descanso
 * tier {@link SpellHealingEffect} reads off the target, a Foco term is the Conjurador's own.
 *
 * <p>Not gated on a {@code CriticalResult}, like {@link Definhar} and unlike every {@link
 * CriticalEffect} here: a Corrente is triggered by the delivery roll clearing the target's DM by
 * a margin, not by a critical. Nothing fires it automatically — {@link EffectChainService#hits}
 * computes that margin, and Sobrecura is the first Corrente whose own text gives a caller a
 * reason to ask.
 */
@Getter
public class Sobrecura extends AbstractEffect implements EffectChain {

    private final Character caster;
    private final int rolledDice;

    /**
     * @param caster     the Conjurador, whose Foco supplies the deterministic half
     * @param rolledDice the already-rolled 1d6 — this core rolls none
     */
    public Sobrecura(final Character caster, final int rolledDice) {
        this.caster = caster;
        this.rolledDice = rolledDice;
    }

    @Override
    public String getDescription() {
        return "Sobrecura: O alvo desta magia adicionalmente recupera +1d6+Metade do Foco PV.";
    }

    /** {@code 1d6 + Metade do Foco}, the Foco half floored the way every half-Atributo term is. */
    public int getRecovery() {
        return rolledDice + caster.getEffectiveAttributeTotal(AttributeDomain.FOCUS) / 2;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int damageBefore = target.getDamageTaken();
        target.heal(getRecovery());

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .resourceGainValue(damageBefore - target.getDamageTaken())
                .resourceGainType(ResourceType.HIT_POINTS))
                .build();
    }
}
