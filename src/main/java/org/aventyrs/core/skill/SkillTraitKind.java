package org.aventyrs.core.skill;

/**
 * Which kind of {@link SkillTrait} an acquisition-time choice actually owes — the one piece of
 * information a pending choice's own {@link SkillType} can't carry, since both traits are keyed
 * by the same Perícia. Reported by {@code
 * org.aventyrs.core.ability.AttributeAbility#resolvePendingSkillTraitKinds()} alongside {@code
 * #resolvePendingSkillTraitChoices}, so a caller (an API/UI layer) resolving an entry knows
 * which of {@code AttributeAbilityService#grantCompetencyAbilityChoice}/{@code
 * #grantSpecializationChoice} to call without hardcoding a per-ability switch of its own — an
 * ability like {@code CharismaAbility#CHARME} owes both per Perícia, one like {@code
 * GnoseAbility#DOMINIO_DO_CONHECIMENTO} owes only a {@link #SPECIALIZATION}.
 */
public enum SkillTraitKind {
    /** A {@link SkillSpecialization} of the pending Perícia. */
    SPECIALIZATION,

    /** A {@link SkillCompetencyAbility} of the pending Perícia. */
    COMPETENCY_ABILITY
}
