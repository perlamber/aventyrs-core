package org.aventyrs.core.effect;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.util.DiceRoller;

/**
 * Incendiar — the Porta Fogo projectile's Corrente: "Causa +1d6 pontos de danos e o tipo de dano muda para Físico
 * Elemental: Fogo."
 *
 * <p>The retyping is reported on {@link InteractionResult#getRetypedDamage()} — the caller deals the hit as Físico
 * Elemental: Fogo, so the target's RE and immunities judge it. The +1d6 is thrown with the attack's own {@link
 * DiceRoller} and lands as this stage runs (⚠️ after the head's mitigation, like {@link Rugido}'s +2), skipped
 * outright for a target immune to Fogo; with no roller it is reported unrolled on {@link
 * InteractionResult#getExtraDamageDice()} instead.
 */
public class Incendiar extends AbstractEffect implements EffectChain {

    /** "o tipo de dano muda para Elemental: Fogo". */
    public static final DamageDescriptor RETYPED =
            new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);

    /** "+1d6 pontos de danos". */
    public static final int EXTRA_DICE = 1;

    private final DiceRoller roller;

    /** @param roller the attack's own dice roller, or {@code null} to report the die rather than throw it */
    public Incendiar(final DiceRoller roller) {
        this.roller = roller;
    }

    @Override
    public String getDescription() {
        return "Causa +1d6 pontos de danos e o tipo de dano muda para Elemental: Fogo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder().retypedDamage(RETYPED);
        if (roller == null) {
            result.extraDamageDice(EXTRA_DICE);
        } else if (!target.isImmuneToDamage(RETYPED.damageType(), RETYPED)) {
            int before = target.getDamageTaken();
            target.applyDamage(roller.rollD6(EXTRA_DICE));
            result.resourceLossValue(target.getDamageTaken() - before).resourceLossType(ResourceType.HIT_POINTS);
        }
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }
}
