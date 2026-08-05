package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.Character;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
 * InteractionResult#getDamageBonus()} set, and one holding {@code
 * AnoesRacialAbility#ABATEDORES_DE_GIGANTES} can get {@link InteractionResult#getSkillRollBonus()}
 * raised — but only via the {@link #applyTo(CharacterSheet, SceneContext, SkillRoll,
 * CharacterSheet)} overload below, since the plain 1-/2-/3-arg {@code applyTo} overloads
 * (inherited unchanged from {@link AbstractSkillInteraction}) have no notion of which {@code
 * CharacterSheet} this particular attack is actually being made against, and both abilities'
 * rules text is scoped to *this* attack's actual target. Both the amount and the condition are
 * resolved by the ability itself — {@link SkillCompetencyAbility#resolveDamageBonus(SceneContext,
 * CharacterSheet)}/{@link SkillCompetencyAbility#resolveAttackRollBonus(CharacterSheet,
 * CharacterSheet)} — this Interaction just scans every {@code SkillCompetencyAbility} the
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

    /**
     * Same as {@link #applyTo(CharacterSheet, SceneContext, SkillRoll)}, but also given
     * attackTarget — the {@code CharacterSheet} this Ataque à Distância is actually being made
     * against — so a held ability like {@code AtaqueADistanciaCompetencyAbility#FRIEZA} or
     * {@code AnoesRacialAbility#ABATEDORES_DE_GIGANTES} can resolve its own bonus against the
     * real target, not merely some generic condition about the encounter at large.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final SkillRoll skillRoll, final CharacterSheet attackTarget) {
        InteractionResult result = super.applyTo(target, sceneContext, skillRoll);
        Character character = target.getCharacter();
        List<SkillCompetencyAbility> abilities = Stream.concat(
                        character.getSkillCompetencyAbilities().stream(),
                        character.getRace().getRacialAbilities().stream())
                .toList();

        Optional<DamageBonus> damageBonus = abilities.stream()
                .map(ability -> ability.resolveDamageBonus(sceneContext, attackTarget))
                .flatMap(Optional::stream)
                .findFirst();
        if (damageBonus.isPresent()) {
            result = result.toBuilder().damageBonus(damageBonus.get()).build();
        }

        int attackRollBonus = abilities.stream()
                .map(ability -> ability.resolveAttackRollBonus(target, attackTarget))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
        if (attackRollBonus != 0) {
            result = result.toBuilder().skillRollBonus(result.getSkillRollBonus() + attackRollBonus).build();
        }

        return result;
    }
}
