package org.aventyrs.core.effect;

/**
 * A Corrente de Efeitos — a secondary effect a hit can trigger beyond its own damage
 * (e.g. {@code org.aventyrs.core.skill.artes.ArtesCompetencyAbility#DISPARO_RICOCHETE},
 * {@code org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility#MILAGREIRO}).
 * Deliberately just a marker on top of {@link Effect} — nothing here should be inferred
 * beyond "this is an Effect that is a Corrente de Efeitos": no trigger condition, no
 * Defesas-comparison threshold (see {@code
 * org.aventyrs.core.ego.AutocontroleAdvantage#RESOLUTO}), no duration/Rodada tracking.
 * Two concrete implementations exist — {@link Definhar} (an ongoing curse drain) and
 * {@link Sobrecura} (the bonus recovery three Vida Magias name). See {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into.
 */
public interface EffectChain extends Effect {
}
