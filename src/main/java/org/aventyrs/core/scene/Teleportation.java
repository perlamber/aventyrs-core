package org.aventyrs.core.scene;

/**
 * How far an effect teleports its holder, in Unidades de Distância — the <b>reach</b> of a
 * teleportation, which is real authored data even though moving anyone is not this core's job.
 *
 * <p><b>It reports; the caller applies.</b> This core never does geometry: nobody carries a
 * position and a {@link SceneContext}'s distances are a caller-supplied {@code Map<CombatantSheet,
 * Range>} (see {@link Range}'s own javadoc). So a teleportation answers only "is that destination
 * within reach" ({@link #reaches(Range)}), and the client updates the distance map it feeds the
 * next {@code Scene#buildContext}. Same division of labour as {@code Scene#refreshAura}, which is
 * caller-driven after any movement for exactly this reason.
 *
 * <p>A client reads this off the ability to offer the move in the first place — {@code
 * AventyrTitleAbility#resolveTeleportation()}, whose live consumer is {@code
 * SantoAbility#GUARDA_VIDAS} ("se teletransportar para a frente de um aliado em Distância Curta").
 *
 * <p>Deliberately a plain UD amount with no {@code Character} in sight. The Magia side will need
 * more when it is built — {@code TransporteSpell#PISCAR}'s reach is "1d6+Metade do Foco UD",
 * computed per-cast — and that wants its own {@code Character}-taking overload of the hook rather
 * than a guess baked in here now.
 *
 * @param unidadesDeDistancia the reach in UD; at least 1
 */
public record Teleportation(int unidadesDeDistancia) {

    public Teleportation {
        if (unidadesDeDistancia < 1) {
            throw new IllegalArgumentException("A teleportation must reach at least 1UD: " + unidadesDeDistancia);
        }
    }

    /**
     * A teleportation reaching the whole of range — the usual way to author one, since rules text
     * names a band ("em Distância Curta") rather than a number. Refuses {@link
     * Range#AO_ALCANCE_DOS_OLHOS}, which states no maximum UD.
     */
    public static Teleportation of(final Range range) {
        Integer maximum = range.getMaxUnidadesDeDistancia();
        if (maximum == null) {
            throw new IllegalArgumentException("A teleportation needs a bounded Range: " + range);
        }
        return new Teleportation(maximum);
    }

    /** This reach as the {@link Range} band that covers it. */
    public Range asRange() {
        return Range.fromUnidadesDeDistancia(unidadesDeDistancia);
    }

    /**
     * Whether something at distance is within this reach. A {@code null} distance is <b>not</b>
     * reachable: "cannot tell" is a refusal here, the same way a {@code null} {@code SceneContext}
     * yields nobody in {@code MovementReactionService}.
     */
    public boolean reaches(final Range distance) {
        return distance != null && distance.isWithin(asRange());
    }
}
