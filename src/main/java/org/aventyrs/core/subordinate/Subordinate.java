package org.aventyrs.core.subordinate;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.UUID;

/**
 * One Subordinado under a character's command (core 0.0.92) — "Personagens que não executam ações, ao invés disso
 * oferecem benefícios à um Personagem Jogador ou Grupo". Held on the <b>commander's</b> sheet as a running effect, so a
 * Duração ({@code rounds}) or a Descanso ends it like any other; {@code null} rounds is until dismissed.
 *
 * <p>{@link #isProdigious()} makes it reach "todos os Personagens do Grupo de Jogadores" — read as the commander's
 * allies in the Scene ({@link SubordinateBenefits}). {@link #getCreatureId()} names the sheet that <i>is</i> the
 * Subordinado when there is one on the board (Cativar Animal's animal), and is {@code null} for one with no body (a
 * Daemon, Agnação Ancestral's Peão).
 */
@Getter
public class Subordinate extends TemporaryEffect implements org.aventyrs.core.sheet.Sustained {

    /** Stable across the wire (core 0.1.5.6), so a client can dismiss the one another client commands. */
    private final UUID id;
    private final SubordinateGrade grade;
    private final SubordinateBenefit benefit;
    private final boolean prodigious;
    private final String source;
    private final UUID creatureId;
    /** Whoever concentrates on it, for one held by Concentração (core 0.0.98); {@code null} otherwise. */
    private final UUID sustainerId;
    /** The N of "Concentração + N" until the focus breaks; {@code null} otherwise. */
    private Integer trailingRounds;

    public Subordinate(@NonNull final SubordinateBenefit benefit, final boolean prodigious, @NonNull final String source,
                       final UUID creatureId, final Integer rounds) {
        this(null, benefit, prodigious, source, creatureId, rounds);
    }

    /** One restored with the id it was first held under (core 0.1.5.6); a {@code null} id is a new one. */
    public Subordinate(final UUID id, @NonNull final SubordinateBenefit benefit, final boolean prodigious,
                       @NonNull final String source, final UUID creatureId, final Integer rounds) {
        super(rounds);
        this.id = id == null ? UUID.randomUUID() : id;
        this.grade = benefit.getGrade();
        this.benefit = benefit;
        this.prodigious = prodigious;
        this.source = source;
        this.creatureId = creatureId;
        this.sustainerId = null;
        this.trailingRounds = null;
    }

    private Subordinate(final SubordinateBenefit benefit, final boolean prodigious, final String source,
                        final UUID sustainerId, final int trailingRounds, final UUID id) {
        super((Integer) null);
        this.id = id == null ? UUID.randomUUID() : id;
        this.grade = benefit.getGrade();
        this.benefit = benefit;
        this.prodigious = prodigious;
        this.source = source;
        this.creatureId = null;
        this.sustainerId = sustainerId;
        this.trailingRounds = trailingRounds;
    }

    /**
     * One held by sustainerId's Concentração, then trailingRounds more (core 0.0.98) — the Esquecida's sombra
     * conselheira, "por Concentração +1 Rodada".
     */
    public static Subordinate sustained(@NonNull final SubordinateBenefit benefit, final boolean prodigious,
                                        @NonNull final String source, @NonNull final UUID sustainerId,
                                        final int trailingRounds) {
        return sustained(null, benefit, prodigious, source, sustainerId, trailingRounds);
    }

    /** {@link #sustained}, restored with the id it was first held under (core 0.1.5.6). */
    public static Subordinate sustained(final UUID id, @NonNull final SubordinateBenefit benefit,
                                        final boolean prodigious, @NonNull final String source,
                                        @NonNull final UUID sustainerId, final int trailingRounds) {
        return new Subordinate(benefit, prodigious, source, sustainerId, trailingRounds, id);
    }

    @Override
    public boolean release() {
        if (trailingRounds == null) {
            return false;
        }
        int rounds = trailingRounds;
        trailingRounds = null;
        startCountdown(rounds);
        return rounds <= 0;
    }

    /** One with no body on the board and no Duração. */
    public static Subordinate of(final SubordinateBenefit benefit, final boolean prodigious, final String source) {
        return new Subordinate(benefit, prodigious, source, null, null);
    }
}
