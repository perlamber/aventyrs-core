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
public class Subordinate extends TemporaryEffect {

    private final UUID id = UUID.randomUUID();
    private final SubordinateGrade grade;
    private final SubordinateBenefit benefit;
    private final boolean prodigious;
    private final String source;
    private final UUID creatureId;

    public Subordinate(@NonNull final SubordinateBenefit benefit, final boolean prodigious, @NonNull final String source,
                       final UUID creatureId, final Integer rounds) {
        super(rounds);
        this.grade = benefit.getGrade();
        this.benefit = benefit;
        this.prodigious = prodigious;
        this.source = source;
        this.creatureId = creatureId;
    }

    /** One with no body on the board and no Duração. */
    public static Subordinate of(final SubordinateBenefit benefit, final boolean prodigious, final String source) {
        return new Subordinate(benefit, prodigious, source, null, null);
    }
}
