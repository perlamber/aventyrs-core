package org.aventyrs.core.title;

/**
 * A held Especialização from one specific Título's own catalog (e.g.
 * {@code SantoSpecialization.ABENCOADO_PELA_LUZ}). Unlike
 * {@code org.aventyrs.core.skill.SkillSpecialization} (purely descriptive — a Perícia's own
 * Especialização carries no bonus of its own, just identity + rules text and a threshold
 * switch on the roll it's requested for), a Título's own Especialização can carry a real
 * activatable effect of its own — Abençoado pela Luz's touch-heal-or-cure clause is the first
 * example.
 *
 * <p>Extends {@link AventyrTitleAbility} to establish that as a general rule, not just a
 * one-off for Abençoado pela Luz: **any Título trait with a real activation cost — a Título's
 * own base effect, an Especialização, or a Habilidade/Suprema — is an Active Ability**,
 * regardless of which catalog it's cataloged under. A Especialização with no activation cost
 * of its own (the common case for a purely descriptive one) needs no override at all —
 * {@code getPDCost()}/{@code getActionPointCost()} default to 0, {@code isSupreme()} defaults
 * to {@code false} (never applicable here), and the inherited {@code isPassive()} formula
 * correctly reports such a specialization as passive. A specialization whose real cost is
 * variable/conditional rather than "none at all" (e.g. {@code SantoSpecialization
 * .ABRACADO_PELA_ESCURIDAO}, whose "Custo de Ativação: Variável" is an entirely PV-based cost,
 * not a PD/PA one) should override {@code isPassive()} explicitly instead of relying on the
 * derived formula, which can't distinguish "genuinely no cost" from "0 PD/PA but a real cost
 * expressed some other way."
 */
public interface AventyrTitleSpecialization extends AventyrTitleAbility {
}
