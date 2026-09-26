package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.Dice;

import java.util.UUID;

/**
 * A per-Rodada effect whose size is rolled — "recupera 1d6PV por Rodada", "sofre 3 pontos de Dano
 * Mágico Elemental por Rodada" when it's dice, "recupera 2d6PV por Rodada". Each Rodada it
 * <b>queues</b> a {@link PendingDiceRoll} on its holder instead of acting, because this core never
 * rolls; {@link CombatantSheet#resolveDiceRoll} applies it once the caller supplies the faces.
 *
 * <p>The flat-valued twins already exist and act on their own — {@link Regeneration} for a heal of
 * a stated amount, {@link Bleeding}/{@link Withering} for a stated damage. Reach for this one only
 * when the rules text rolls.
 */
@Getter
public class RecurringDice extends TemporaryEffect {

    public enum Kind {
        /** Healing, as Regeneração ({@code HealingSource.regeneration}-style). */
        HEAL,
        /** Damage, mitigated like any other through {@code DamageService}. */
        DAMAGE
    }

    @NonNull
    private final Kind kind;

    @NonNull
    private final Dice dice;

    private final DamageDescriptor descriptor;

    private final String source;

    /** How many of this source may run at once — a new one past it replaces the oldest ("não cumulativo" is 1). */
    private final int simultaneousLimit;

    public RecurringDice(@NonNull final Kind kind, @NonNull final Dice dice, final DamageDescriptor descriptor,
                         final Integer rounds, final String source) {
        this(kind, dice, descriptor, rounds, source, UNLIMITED_SIMULTANEOUS);
    }

    public RecurringDice(@NonNull final Kind kind, @NonNull final Dice dice, final DamageDescriptor descriptor,
                         final Integer rounds, final String source, final int simultaneousLimit) {
        super(rounds);
        this.kind = kind;
        this.dice = dice;
        this.descriptor = descriptor;
        this.source = source;
        this.simultaneousLimit = simultaneousLimit;
    }

    /** A heal of dice per Rodada, of which at most simultaneousLimit from source run at once. */
    public static RecurringDice healing(@NonNull final Dice dice, final Integer rounds, final String source,
                                       final int simultaneousLimit) {
        return new RecurringDice(Kind.HEAL, dice, null, rounds, source, simultaneousLimit);
    }

    @Override
    int maximumSimultaneous() {
        return simultaneousLimit;
    }

    public static RecurringDice healing(@NonNull final Dice dice, final Integer rounds, final String source) {
        return new RecurringDice(Kind.HEAL, dice, null, rounds, source);
    }

    public static RecurringDice damage(@NonNull final Dice dice, @NonNull final DamageDescriptor descriptor,
                                       final Integer rounds, final String source) {
        return new RecurringDice(Kind.DAMAGE, dice, descriptor, rounds, source);
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        sheet.queuePendingDiceRoll(new PendingDiceRoll(UUID.randomUUID(), kind, dice, descriptor, source));
    }

    @Override
    Object stackingKey() {
        return source;
    }
}
