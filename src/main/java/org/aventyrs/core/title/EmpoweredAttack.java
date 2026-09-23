package org.aventyrs.core.title;

import org.aventyrs.core.effect.CriticalEffectType;

/**
 * What <b>one</b> attack gains from a Título activation made just before it — reported on {@code
 * org.aventyrs.core.sheet.InteractionResult#getEmpoweredAttack()} for the caller to fold into the
 * attack it is about to build.
 *
 * <p>Three traits produce one today, and between them they cover every stage of an attack, which
 * is why this is one bundle rather than a field per clause: {@code
 * AbracadoPelaEscuridaoAbility#PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI} (Margem Crítica, Roubo de Vida,
 * an extra Efeito Crítico), {@code SantoSpecialization#ABRACADO_PELA_ESCURIDAO}'s Fúria dos Deuses
 * (the attack roll and its GD), and {@code AbracadoPelaEscuridaoAbility#FUROR_DE_SYLPH} (the dano
 * roll and the reach). Every field is inert at 0/{@code null}, so a producer sets only its own.
 *
 * <p><b>Why this is a report and not a grant.</b> The activation happens <em>before</em> the attack
 * exists: the client activates the Habilidade, reads this, and only then builds its {@code
 * DeliveredAttack}/{@code SkillRoll} — so nothing has to interject into a resolution already in
 * flight, and {@code AttackDelivery}/{@code AttackReceiver} need no change at all. That is the same
 * ordering that makes {@code org.aventyrs.core.title.santo.GuardaVidasInteraction}'s interception
 * work, and it is what "this one delivered attack" scoping was previously blocked on: this core's
 * roll machinery ({@code AbstractSkillInteraction}/{@code SkillRoll}) computes bonuses for
 * <em>any</em> roll of a given Perícia, with no notion of "the one attack being made right now as
 * part of activating a different ability".
 *
 * <p>A single value object rather than four parallel fields on {@code InteractionResult}, the same
 * shape {@code org.aventyrs.core.sheet.Blessing} and {@code
 * org.aventyrs.core.character.DamageBonus} already take — and, like {@code
 * InteractionResult#getRedirectedAttackTarget()}, added for one real consumer rather than
 * speculatively.
 *
 * <p><b>Nothing inside this core consumes it.</b> Each field names a figure this core can compute
 * but not apply to a single attack, for a reason of its own; see {@code
 * org.aventyrs.core.title.santo.PlacidezDeUndineRancorDeHaloiInteraction}'s javadoc, which is the
 * only producer today.
 *
 * @param attackRollBonus        a flat bonus on the Perícia de Ataque roll itself — {@code
 *                               Skill#ADVANTAGE_BONUS} where a clause grants Vantagem. Added to
 *                               the roll's own total by the caller, alongside every other source.
 * @param difficultyReduction    how many <i>níveis</i> easier this attack's Grau de Dificuldade
 *                               becomes ("reduz a GD da Rolagem de Perícia de Ataque em -1
 *                               Nível"). A count of tiers, not a flat number — the caller applies
 *                               it with {@code DifficultyLevel#easier}.
 * @param extraDamageDice        further d6 the dano roll gains. <b>This core never rolls</b>, so
 *                               the caller rolls them; a {@code DamageBonus} could not carry a
 *                               die, which is why this is a separate count.
 * @param extraDamageFlat        a flat addition to the dano roll, beside {@link #extraDamageDice}
 *                               — the "+Vigor" half of a "1d6+Vigor" clause.
 * @param rangeIncrease          how many {@code Range} bands this attack's reach widens by.
 * @param criticalMarginIncrease how many <i>números</i> this attack's Margem Crítica Menor widens
 *                               by — a count of steps, so the caller <b>lowers</b> the margin by
 *                               it (lower is wider; see CLAUDE.md's Margem Crítica Menor rule).
 *                               Summed with every other source the attacker holds, then reduced by
 *                               the target's Resistência a Críticos, exactly as {@code
 *                               AbstractSkillInteraction#sumCriticalMarginIncrease} does.
 * @param lifeSteal              Roubo de Vida for this attack alone. Deliberately not a {@code
 *                               org.aventyrs.core.sheet.LifeSteal} effect: that is a {@code
 *                               TemporaryEffect} counting down in Rodadas, so applying it would
 *                               keep stealing for the rest of the Rodada.
 * @param determinationSteal     Roubo de Determinação for this attack alone. No mechanism exists
 *                               for it at all — only Roubo de Vida does ({@code LifeStealService})
 *                               — so this is computed, reported, and applied by nobody.
 * @param additionalCriticalEffect an Efeito Crítico this attack gains <em>on top of</em> whatever
 *                               its weapon already carries ("a Corrente de Efeitos – Rancor de
 *                               Haloi"), or {@code null} when the activation grants none.
 */
public record EmpoweredAttack(int attackRollBonus,
                              int difficultyReduction,
                              int extraDamageDice,
                              int extraDamageFlat,
                              int rangeIncrease,
                              int criticalMarginIncrease,
                              int lifeSteal,
                              int determinationSteal,
                              CriticalEffectType additionalCriticalEffect) {

    /** An otherwise-inert bundle carrying only what a Margem Crítica / Roubo clause grants. */
    public static EmpoweredAttack ofCriticalAndSteal(final int criticalMarginIncrease, final int lifeSteal,
                                                     final int determinationSteal,
                                                     final CriticalEffectType additionalCriticalEffect) {
        return new EmpoweredAttack(0, 0, 0, 0, 0, criticalMarginIncrease, lifeSteal, determinationSteal,
                additionalCriticalEffect);
    }

    /** An otherwise-inert bundle carrying only what a roll-and-GD clause grants. */
    public static EmpoweredAttack ofRoll(final int attackRollBonus, final int difficultyReduction,
                                         final int extraDamageDice, final int extraDamageFlat) {
        return new EmpoweredAttack(attackRollBonus, difficultyReduction, extraDamageDice, extraDamageFlat,
                0, 0, 0, 0, null);
    }
}
