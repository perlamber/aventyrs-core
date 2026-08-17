package org.aventyrs.core.ego;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InitiativeBlessing;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
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

    /**
     * Every {@link InitiativeBlessing} this Vantagem grants the moment its holder wins
     * initiative for their group — e.g. {@link InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s
     * +2UD Movimento Base. Resolved once, at grant-time (not per-roll like {@link
     * #resolveConditionalRollBonus}/{@link #resolveDamageBonus} above, so no {@code
     * SceneContext} parameter — the Round-scoping lives in each returned blessing's own {@code
     * rounds} countdown once granted), by {@code
     * org.aventyrs.core.character.services.InitiativeBlessingService}, which scans this same
     * default-empty hook across {@code EgoAdvantage}, {@code
     * org.aventyrs.core.ability.AttributeAbility}, and {@code
     * org.aventyrs.core.skill.SkillCompetencyAbility} identically — see that service's own
     * javadoc for the full mechanism. Empty by default; only override on a constant whose
     * rules text grants a bonus specifically for winning initiative.
     */
    default List<InitiativeBlessing> resolveInitiativeBlessings() {
        return List.of();
    }

    /**
     * RA (Redução Absoluta) this Vantagem grants right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s RA during a Cena
     * de Combate's first two Rounds. Summed by {@code
     * org.aventyrs.core.character.services.DamageService#getTotalAbsoluteDamageReduction(
     * Character, SceneContext)} across every held {@code EgoAdvantage}, alongside the
     * reflection-based {@code @Modifier(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)} sources that
     * method already scanned — this one exists because a Round/Cena-de-Combate-conditioned
     * bonus isn't reflection-discoverable via a no-arg {@code @Modifier} method, the same
     * reason {@link #resolveConditionalRollBonus} exists instead of a plain {@code @Modifier}
     * for {@code SKILL_ROLL_BONUS}. Zero by default; only override on a constant whose rules
     * text grants RA scoped to per-roll Scene facts like this.
     */
    default int resolveAbsoluteDamageReduction(final SceneContext sceneContext) {
        return 0;
    }

    /**
     * Whether this Vantagem halves damage taken right now, conditioned on {@link
     * SceneContext} — e.g. {@link InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s "dano causado a
     * você é reduzido à metade" once its holder has also won initiative. Mirrors {@link
     * #resolveAbsoluteDamageReduction}'s reasoning, but a plain {@code boolean} rather than a
     * summed value — same shape {@code DamageServiceImpl} already treats {@code
     * ModifierType#HALF_DAMAGE} as (any positive sum means "yes"), not a magnitude. {@code
     * false} by default; only override on a constant whose rules text halves damage taken
     * scoped to per-roll Scene facts like this.
     */
    default boolean resolveHalfDamage(final SceneContext sceneContext) {
        return false;
    }

    /**
     * A bonus toward a Perícia roll this Vantagem grants, but only for named skillType(s) —
     * e.g. {@link ResourcesAdvantage#MORAL_HERDADA}'s "+1 em rolagens de Artes e Persuasão,"
     * not every Perícia. {@link #resolveConditionalRollBonus} can't express this: it's summed
     * identically into every skill's own {@code AbstractSkillInteraction#applyTo} with no
     * {@link SkillType} to condition on at all — using it here would silently over-grant the
     * bonus to every Perícia instead of just the named ones, the same "don't silently narrow or
     * over-grant" restraint this codebase already applies to purpose-scoped Vantagem bonuses.
     * target is the CharacterSheet performing this roll (not a separate attack target — unlike
     * {@code SkillCompetencyAbility#resolveDamageBonus}'s {@code attackTarget}, this Vantagem's
     * own bonus depends on the roller's <em>own</em> state, e.g. its current Fama), passed
     * explicitly for the same reason {@code CharacterSheet} state generally is here: it isn't
     * reflection-discoverable via a no-arg {@code @Modifier} method. Empty by default; only
     * override on a constant whose rules text scopes a roll bonus to specific named skills.
     */
    default Optional<Integer> resolveSkillSpecificRollBonus(final SkillType skillType, final SceneContext sceneContext, final CharacterSheet target) {
        return Optional.empty();
    }

    /**
     * How many extra "números" this Vantagem widens skillType's Margem Crítica Menor by right
     * now, conditioned on {@link SceneContext} — e.g. {@link SorteAdvantage#ACE}'s +1/+3 bonus
     * depending on the roll's attack-skill/combat-scene status. Mirrors {@code
     * org.aventyrs.core.ability.AttributeAbility#resolveCriticalMarginIncrease}/{@code
     * org.aventyrs.core.skill.SkillCompetencyAbility#resolveCriticalMarginIncrease}'s identical
     * shape — see {@code org.aventyrs.core.skill.SkillRoll#getCriticalResult(int)} for how the
     * sum across all three is actually consumed. Zero by default; only override on a constant
     * whose rules text widens Margem Crítica Menor like this.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
        return 0;
    }
}
