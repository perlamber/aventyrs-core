package org.aventyrs.core.item;

/**
 * How heavy an {@link Item} is — the first half of an item's own "(Pesado/Raro)"-style
 * heading, alongside {@link ItemRarity}.
 *
 * <p>This is a plain classification tag today. Carga (a character's carrying capacity, which
 * a weight class would presumably feed into) doesn't exist anywhere in this core — {@code
 * org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility}'s own javadoc already cites
 * Carga as one of the equipment properties this codebase has no home for — so nothing
 * consumes this yet.
 */
public enum ItemWeightClass {
    LIGHT,
    MEDIUM,
    HEAVY
}
