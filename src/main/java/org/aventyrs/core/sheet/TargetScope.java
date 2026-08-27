package org.aventyrs.core.sheet;

/**
 * Who a granted {@link Blessing} applies to — e.g. {@code ArtesCompetencyAbility#DOM_BARDICO}'s
 * {@link #ALLIES} ("a eles, mas não a você" — excludes the caster). This is a fixed property of
 * *which ability* granted the bonus, not something this core resolves into an actual recipient
 * list itself — a caller still does that via {@code org.aventyrs.core.scene.Scene#getAllies}/
 * {@code #getEnemies} (for {@link #ALLIES}/{@link #ENEMIES}) or its own target lookup (for
 * {@link #SINGLE_TARGET}).
 *
 * <p>{@link #SELF}/{@link #SELF_AND_ALLIES} cover a genuinely different shape than {@link
 * #ALLIES}: a trait like {@code InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO} or {@code
 * AbencoadoPelaLuzAbility#GRITO_DE_GUERRA_VULCANO} always grants to its own holder, and only
 * *additionally* extends to Scene allies when the ability's rules text says so — unlike {@link
 * #ALLIES}, which specifically excludes the caster. An earlier design modeled this with a plain
 * {@code boolean appliesToAllies} field on a separate {@code InitiativeBlessing} class instead
 * of reusing this enum, precisely to avoid conflating it with {@link #ALLIES}'s
 * excludes-the-caster meaning — these two new constants resolve that without the conflation,
 * once a real caller ({@link Blessing}, used by both the initiative-win and the direct-Interaction-
 * activation grant mechanisms) needed the distinction expressed as data rather than a
 * separate boolean.
 */
public enum TargetScope {
    SINGLE_TARGET,
    ALLIES,
    ENEMIES,
    SELF,
    SELF_AND_ALLIES
}
