package org.aventyrs.core.ego;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;

import java.util.Optional;

public interface EgoAdvantage {
    EgoDomain getEgoDomain();
    String getDescription();

    /**
     * A bonus toward a Perícia roll this Vantagem grants right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#IMPETO}'s Vantagem during a Cena de
     * Combate's first two Rounds. Mirrors {@code org.aventyrs.core.skill.SkillCompetencyAbility
     * #resolveConditionalRollBonus}'s shape (same reason: this data isn't reflection-discoverable
     * via a no-arg {@code @Modifier} method), but on the {@code EgoAdvantage} side instead — a
     * Vantagem de Ego is granted once at creation, not tied to any one Perícia, so it's summed
     * generically across every Perícia's own {@code AbstractSkillInteraction#applyTo}, the same
     * additive convention every other {@code skillRollBonus} source already uses. Empty by
     * default; only override on a constant whose rules text grants a bonus scoped to per-roll
     * Scene facts like this.
     */
    default Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext) {
        return Optional.empty();
    }

    /**
     * A bonus toward a dano roll this Vantagem grants right now — mirrors {@code
     * SkillCompetencyAbility#resolveDamageBonus}'s shape, minus the {@code attackTarget}
     * parameter: unlike {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s proximity condition,
     * no {@code EgoAdvantage} granting this needs to know the real attack target, only {@link
     * SceneContext} facts, so this is safe to resolve generically for every attack-skill roll
     * inside {@code AbstractSkillInteraction} itself rather than needing a skill-specific
     * overload with an explicit target. Empty by default; only override on a constant whose
     * rules text grants a dano bonus. Same "only one bonus expected to apply per roll"
     * convention as {@code resolveDamageBonus} — not additive with it.
     */
    default Optional<DamageBonus> resolveDamageBonus(final SceneContext sceneContext) {
        return Optional.empty();
    }
}
