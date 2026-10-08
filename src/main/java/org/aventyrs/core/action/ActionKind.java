package org.aventyrs.core.action;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

/**
 * What kind of thing a combatant is trying to do — the question a held Condição's refusal is asked
 * against ({@code CombatantSheet#refusalForAction}). One vocabulary for every gate, so a Condição
 * that forbids "Ações" (Imobilizado, Desacordado), "Ações de Movimento" (Agarrado) or "Ações Livres
 * e Reações" (Confuso) is stated once, on the {@code ConditionType}, rather than in each service.
 *
 * <p><b>{@link #DEFENCE} is never refused by a Condição.</b> Defending is not an Ação: a
 * defender under Imobilizado or Desacordado still rolls, and succeeds unless it is a Falha
 * Crítica (table ruling, 2026-10-07).
 */
public enum ActionKind {
    /** Andar, correr, investir, reposicionar — any Ação de Movimento. */
    MOVEMENT,
    /** An Ataque Corpo-a-Corpo or à Distância. */
    ATTACK,
    /** Any other Perícia roll. */
    SKILL_ROLL,
    /** An Esquiva e Aparar roll against an attack. */
    DEFENCE,
    /** Activating a Habilidade (de Aventyr, de Monstro, a Talento's activated ability). */
    ABILITY_ACTIVATION,
    /** Conjurar uma Magia. */
    SPELL_CAST,
    /** Drawing or taking up a weapon. */
    ARMING,
    /** Anything bought as an Ação Livre. */
    FREE_ACTION,
    /** Anything bought as a Reação. */
    REACTION,
    /** Levantar-se — leaving Caído. */
    STAND_UP,
    /** Libertar-se do Agarrão. */
    ESCAPE_GRAPPLE,
    /** Libertar-se da Imobilização — the one action Imobilizado allows. */
    ESCAPE_IMMOBILIZATION,
    /** Libertar-se da Predação. */
    ESCAPE_DEVOURED,
    /** A captor letting go. */
    RELEASE_GRAPPLE;

    /** Whether this is one of the three escapes. */
    public boolean isEscape() {
        return this == ESCAPE_GRAPPLE || this == ESCAPE_IMMOBILIZATION || this == ESCAPE_DEVOURED;
    }

    /**
     * The kind a Perícia roll is: an escape manoeuvre's own kind, an Esquiva e Aparar's {@link
     * #DEFENCE}, an attack Perícia's {@link #ATTACK}, otherwise {@link #SKILL_ROLL}.
     */
    public static ActionKind of(final SkillType skillType, final SkillRoll skillRoll) {
        Manoeuvre manoeuvre = skillRoll == null ? null : skillRoll.getManoeuvre();
        if (manoeuvre != null && manoeuvre.getActionKind() != null) {
            return manoeuvre.getActionKind();
        }
        if (skillType == SkillType.ESQUIVA_E_APARAR) {
            return DEFENCE;
        }
        return skillType.isAttackSkill() ? ATTACK : SKILL_ROLL;
    }

    /** {@link #FREE_ACTION} or {@link #REACTION} when cost is priced as one, else {@code null}. */
    public static ActionKind ofCost(final ActionCost cost) {
        if (cost == null) {
            return null;
        }
        return switch (cost.kind()) {
            case FREE_ACTION -> FREE_ACTION;
            case REACTION -> REACTION;
            default -> null;
        };
    }
}
