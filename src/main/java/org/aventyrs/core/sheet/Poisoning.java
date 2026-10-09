package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;

import java.util.List;

/**
 * An {@link ConditionType#ENVENENADO} that bites every Rodada — "Estado de Fraqueza e sofre Dano Natural
 * contínuo. Quantidade de danos conforme origem do efeito." The amount is the Veneno's own, so it rides
 * the held instance, beside whatever other magnitudes the Veneno states ({@link #getExtraEffects()} —
 * a Multiplicador loss).
 *
 * <p>Dealt in {@link #applyRoundEffect} like a {@link Bleeding}: raw PV, already the Veneno's figure, so
 * no RD/RDS judges it — but a combatant immune to Dano Natural takes none, the one reading of "Dano
 * Natural" that can matter here.
 */
@Getter
public class Poisoning extends Condition {

    /** "Dano Natural" — what an immunity to the damage is judged against. */
    public static final DamageDescriptor NATURAL = new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.NATURAL);

    private final int damagePerRound;

    public Poisoning(final int damagePerRound, final Integer remainingRounds, final CombatantSheet source,
                     final List<ConditionType.ConditionEffect> extraEffects) {
        super(ConditionType.ENVENENADO, remainingRounds, source, extraEffects);
        this.damagePerRound = damagePerRound;
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        if (damagePerRound > 0 && !sheet.isImmuneToDamage(NATURAL.damageType(), NATURAL)) {
            sheet.applyDamage(damagePerRound);
        }
    }

    @Override
    Condition decayed(final ConditionType next, final int rounds) {
        return next == ConditionType.ENVENENADO
                ? new Poisoning(damagePerRound, rounds, getSource(), getExtraEffects())
                : super.decayed(next, rounds);
    }
}
