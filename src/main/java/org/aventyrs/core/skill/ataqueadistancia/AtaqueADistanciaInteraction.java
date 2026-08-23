package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Ataque à Distância Perícia test. The rules text notes this Perícia is compared
 * against a target's DF or DM rather than a fixed GD; that comparison lives in {@code
 * org.aventyrs.core.combat.AttackDelivery}, which rolls this Interaction and judges the total
 * against the defender's Defesa. This class still only computes the roll. When it delivers a
 * Magia rather than a mundane
 * attack, see {@link org.aventyrs.core.magic.SpellCastingService}. If the character has a
 * {@code SkillCompetencyAbility} for this same skill whose {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO}), that Attribute is used in place of
 * Ataque à Distância's normal Destreza — see {@link AbstractSkillInteraction} for how the
 * roll bonus/difficultyReduction are actually computed.
 *
 * <p>A character holding {@code AtaqueADistanciaCompetencyAbility#FRIEZA} can get {@link
 * InteractionResult#getDamageBonus()} set, and one holding {@code
 * AnoesRacialAbility#ABATEDORES_DE_GIGANTES} can get {@link InteractionResult#getSkillRollBonus()}
 * raised — but only via the {@link #applyTo(CombatantSheet, SceneContext, SkillRoll,
 * CombatantSheet)} overload below, since the plain 1-/2-/3-arg {@code applyTo} overloads
 * (inherited unchanged from {@link AbstractSkillInteraction}) have no notion of which {@code
 * CombatantSheet} this particular attack is actually being made against, and both abilities'
 * rules text is scoped to *this* attack's actual target. Both the amount and the condition are
 * resolved by the ability itself — {@link SkillCompetencyAbility#resolveDamageBonus(SceneContext,
 * CombatantSheet)}/{@link SkillCompetencyAbility#resolveAttackRollBonus(CombatantSheet,
 * CombatantSheet)} — this Interaction just scans every {@code SkillCompetencyAbility} the
 * character holds, from both {@link Character#getSkillCompetencyAbilities()} (acquired) and
 * {@code character.getRace().getRacialAbilities()} (racial, see {@code
 * org.aventyrs.core.race.Race#getRacialAbilities()}) identically — it doesn't know or care
 * which source or which specific constant answers.
 */
public class AtaqueADistanciaInteraction extends AbstractSkillInteraction {

    public AtaqueADistanciaInteraction() {
        super(SkillType.ATAQUE_A_DISTANCIA);
    }

    public AtaqueADistanciaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATAQUE_A_DISTANCIA, characterSkillService, modifierResolver);
    }
}
