package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Requests an Ataque à Distância Perícia test. The rules text notes this Perícia is compared
 * against a target's DF or DM rather than a fixed GD, but that target-side lookup/conversion
 * is left to a layer above this core. When the roll delivers a Magia rather than a mundane
 * attack, see {@link org.aventyrs.core.magic.SpellCastingService}. If the character has a
 * {@code SkillCompetencyAbility} for this same skill whose {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO}), that Attribute is used in place of
 * Ataque à Distância's normal Destreza — see {@link AbstractSkillInteraction} for how the
 * roll bonus/difficultyReduction are actually computed.
 *
 * <p>A character holding {@code AtaqueADistanciaCompetencyAbility#FRIEZA} can get {@link
 * InteractionResult#getDamageBonus()} set — but only via the {@link #applyTo(CharacterSheet,
 * SceneContext, SkillRoll, CharacterSheet)} overload below, since the plain 1-/2-/3-arg
 * {@code applyTo} overloads (inherited unchanged from {@link AbstractSkillInteraction}) have
 * no notion of which {@code CharacterSheet} this particular attack is actually being made
 * against, and FRIEZA's own rules text is scoped to "alvos em Distância Curta" (*this*
 * attack's target), not "any enemy this close". Both the amount and the condition are resolved
 * by the ability itself — {@link SkillCompetencyAbility#resolveDamageBonus(SceneContext,
 * CharacterSheet)} — this Interaction just scans every {@code SkillCompetencyAbility} the
 * character holds and takes the first non-empty result; it doesn't know or care that FRIEZA is
 * the one constant that currently ever returns one.
 */
public class AtaqueADistanciaInteraction extends AbstractSkillInteraction {

    public AtaqueADistanciaInteraction() {
        super(SkillType.ATAQUE_A_DISTANCIA);
    }

    public AtaqueADistanciaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATAQUE_A_DISTANCIA, characterSkillService, modifierResolver);
    }

    /**
     * Same as {@link #applyTo(CharacterSheet, SceneContext, SkillRoll)}, but also given
     * attackTarget — the {@code CharacterSheet} this Ataque à Distância is actually being made
     * against — so a held ability like {@code AtaqueADistanciaCompetencyAbility#FRIEZA} can
     * resolve its own damage bonus against the real target's distance, not merely whether
     * *some* enemy happens to be close.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final SkillRoll skillRoll, final CharacterSheet attackTarget) {
        InteractionResult result = super.applyTo(target, sceneContext, skillRoll);
        Optional<DamageBonus> damageBonus = target.getCharacter().getSkillCompetencyAbilities().stream()
                .map(ability -> ability.resolveDamageBonus(sceneContext, attackTarget))
                .flatMap(Optional::stream)
                .findFirst();
        return damageBonus.map(bonus -> result.toBuilder().damageBonus(bonus).build()).orElse(result);
    }
}
