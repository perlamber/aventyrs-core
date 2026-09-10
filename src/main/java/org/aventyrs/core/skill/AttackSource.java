package org.aventyrs.core.skill;

import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;

/**
 * Something an attack can be made <em>with</em> — a {@link Weapon} that gets swung, fired or
 * thrown, or a {@link Spell} that gets cast. Both implement this directly; there is deliberately
 * no wrapper type standing between them and a roll.
 *
 * <p>This exists because plenty of rules text scopes an effect to <em>how</em> an attack is
 * delivered rather than to the Perícia as a whole — {@code
 * AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO}'s "apenas para rolagens de ataques com
 * armas de arremessos e magias" is the first one wired. A no-arg {@code @Modifier} method can't
 * see that, and neither could {@link SkillCompetencyAbility#getSubstituteAttributeDomain()},
 * which is why that hook only ever covered the <em>unconditional</em> case. Handing one of these
 * to {@link AbstractSkillInteraction#applyTo(org.aventyrs.core.sheet.CombatantSheet,
 * org.aventyrs.core.scene.SceneContext, SkillRoll, org.aventyrs.core.sheet.CombatantSheet,
 * AttackSource)} is what makes the scoped case expressible.
 *
 * <p><b>An ability narrows by type, not by asking this interface.</b> There is no {@code
 * isThrown()}/{@code isWeapon()} here, and there shouldn't be: which {@link
 * org.aventyrs.core.item.ItemCategory} values a given clause counts as "arremesso" is that
 * clause's own reading, and the next clause's reading would have to disagree with it. A hook
 * narrows with {@code instanceof} plus whatever column it actually cares about, the same way
 * {@code AtaqueADistanciaCompetencyAbility#FRIEZA} holds both the amount and the {@code Range}
 * condition of its own bonus rather than pushing either onto {@code SceneContext}.
 *
 * <p><b>{@code null} means the caller didn't say</b>, and every hook reads it as "no scope
 * matched" rather than as an error — an attack has no obligation to declare what it was made
 * with. Note {@code null} therefore does <em>not</em> mean "unarmed": an Ataque Desarmado has no
 * representation here, because nothing consumes one yet and inventing a constant for it would
 * pick a Perícia (see {@link #getAttackSkillType()}) that an Ataque Desarmado doesn't have a
 * fixed answer for. See CLAUDE.md's gap catalog.
 *
 * <p>Lives in {@code skill} rather than in {@code combat} because its one member is a {@link
 * SkillType} and both implementors already depend on this package — so it adds no package
 * dependency edge anywhere, where {@code combat} would have added three.
 */
public interface AttackSource {

    /**
     * The Perícia de Ataque that delivers this — Ataque Corpo a Corpo for a machado, Ataque à
     * Distância for an arco or a ranged Magia.
     *
     * <p>This is not a new column: {@link Weapon#getSkillType()} and {@link
     * Spell#getAttackSkillType()} both already carried it, and this interface only names the
     * concept they share, which is what keeps it from being a marker interface. Nothing consumes
     * it yet — {@code DeliveredAttack} still names its own {@code attackSkill}, deliberately, so
     * that a caller supplying both can't be silently overruled by one of them. It's declared
     * because "an attack source knows which Perícia swings it" is the fact that makes a weapon
     * and a Magia the same kind of thing at all.
     */
    SkillType getAttackSkillType();
}
