package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Pele de Pedra — a {@link TemporaryEffect} that <b>negates the first damaging hit outright and
 * then wears away</b>, the shape {@code AbencoadoPelaLuzAbility#CORPO_INDESTRUTIVEL_DE_EPONA}
 * takes: "O primeiro ataque que lhe causaria Danos é reduzido à zero, após isso você recebe RDS 5.
 * A Redução de Danos Sofridos é reduzida em -2 para cada dano sofrido."
 *
 * <p><b>Why this is its own effect rather than a {@link TemporaryBonus}.</b> A bonus holds one
 * figure for its whole Duração; this one holds a different figure after every hit it takes, and
 * before the first it is not a figure at all but an outright negation. Neither the RD scan nor a
 * {@code ModifierType} can express either half, so {@code DamageServiceImpl} consults this
 * directly — the one place it reads a concrete effect type rather than summing a stat.
 *
 * <p><b>It is spent by damage, not by Rodadas.</b> {@link #absorb()} is what advances it, called
 * once per hit that would have dealt damage; the Rodada countdown it inherits is the Duração
 * ("por 1 Rodada"), which ends it early regardless of how much stone is left. Both are real: a
 * held Pele de Pedra ends at whichever runs out first.
 *
 * <p><b>Only a hit that would really have hurt spends it</b> — "o primeiro ataque que lhe
 * <i>causaria</i> Danos". An attack already fully turned aside by RD or RA never reaches this, so
 * a character in stone form does not waste their negation on a blow that was harmless anyway.
 * That is the caller's ordering, and {@code DamageServiceImpl} follows it.
 *
 * <p>Not cumulative: a second casting replaces the first rather than granting two layers of stone,
 * which is what re-entering the same state means.
 */
@Getter
public class PeleDePedra extends TemporaryEffect {

    /** "após isso você recebe RDS 5." */
    public static final int INITIAL_DAMAGE_REDUCTION = 5;

    /** "A Redução de Danos Sofridos é reduzida em -2 para cada dano sofrido." */
    public static final int DECAY_PER_HIT = 2;

    /** Whether the one free negation is still available. */
    private boolean negationAvailable = true;

    private int damageReduction = INITIAL_DAMAGE_REDUCTION;

    public PeleDePedra(final Integer remainingRounds) {
        super(remainingRounds);
    }

    /** One layer of stone per holder — "sua pele é transformada em pedra", not layered onto. */
    @Override
    public boolean isCumulative() {
        return false;
    }

    /**
     * Whether this still negates a hit outright rather than merely reducing it. True exactly until
     * the first damaging hit is absorbed.
     */
    public boolean negatesNextHit() {
        return negationAvailable;
    }

    /**
     * The RDS this currently contributes — 0 while {@link #negatesNextHit()} still holds, since
     * until then the stone is not reducing damage but stopping it.
     */
    public int getEffectiveDamageReduction() {
        return negationAvailable ? 0 : damageReduction;
    }

    /**
     * Spends one hit's worth of stone: the first call consumes the negation, and every later one
     * wears the reduction down by {@link #DECAY_PER_HIT}, floored at 0.
     *
     * <p>Floored rather than allowed to go negative so a spent Pele de Pedra simply stops helping
     * — it must never start <i>adding</i> to incoming damage, which a negative RD would do once
     * summed into the total.
     */
    public void absorb() {
        if (negationAvailable) {
            negationAvailable = false;
            return;
        }
        damageReduction = Math.max(0, damageReduction - DECAY_PER_HIT);
    }
}
