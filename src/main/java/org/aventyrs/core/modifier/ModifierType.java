package org.aventyrs.core.modifier;

/**
 * Registry of the kinds of numeric bonuses abilities, feats, titles or items can grant.
 * Adding a new kind of bonus to the system only requires a new constant here — nothing
 * that resolves or consumes modifiers needs to change.
 *
 * <p>{@code SKILL_ROLL_BONUS} applies broadly, to every Perícia roll — but a bonus that
 * should only apply to *one specific* Perícia (e.g. a temporary buff an ally received for
 * Atletismo specifically, not every roll they make) needs its own constant instead, since
 * {@code @Modifier} is a compile-time-fixed annotation value and {@link
 * org.aventyrs.core.modifier.ModifierResolver} has no way to filter a scan by "which skill is
 * this roll for". Every Perícia gets one of these — see {@code
 * org.aventyrs.core.skill.SkillType#getRollBonusType()} for the per-{@code SkillType} lookup —
 * summed *alongside*, not instead of, {@code SKILL_ROLL_BONUS} by {@code
 * org.aventyrs.core.skill.AbstractSkillInteraction}.
 */
public enum ModifierType {
    LIFE_MULTIPLIER,
    SIZE_CATEGORY,
    MANA_MULTIPLIER,
    DETERMINATION_MULTIPLIER,
    ACTION_POINTS,
    SKILL_ROLL_COST,
    SKILL_ROLL_BONUS,
    REACTIONS,
    FREE_ACTIONS,
    DAMAGE_REDUCTION,
    ABSOLUTE_DAMAGE_REDUCTION,
    ATTENTION_ROLL_BONUS,
    ARTES_ROLL_BONUS,
    ATLETISMO_ROLL_BONUS,
    DIRIGIR_E_CAVALGAR_ROLL_BONUS,
    DOMINIO_DO_MANA_ROLL_BONUS,
    ATAQUE_A_DISTANCIA_ROLL_BONUS,
    ATAQUE_CORPO_A_CORPO_ROLL_BONUS,
    ESQUIVA_E_APARAR_ROLL_BONUS,
    EMPATIA_SELVAGEM_ROLL_BONUS,
    FURTIVIDADE_ROLL_BONUS,
    MEDICINA_E_CURA_ROLL_BONUS,
    PERSUASAO_ROLL_BONUS,
    PROFISSAO_ROLL_BONUS,
    CONHECIMENTOS_ROLL_BONUS
}
