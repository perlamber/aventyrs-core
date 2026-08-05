package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;

import java.util.Collection;
import java.util.List;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_COMPETENCY_ABILITY_NOT_HELD;

/**
 * The {@code applyTo}/{@code findCharacterSkill} machinery every {@code <Skill>Interaction}
 * needs, factored out into one place — see CLAUDE.md's "Adding a new Perícia" checklist item
 * 5. Every concrete subclass (e.g. {@link ArtesInteraction}) was, before this class existed,
 * an almost byte-for-byte copy of every other one, varying only by which {@link SkillType} it
 * targeted; that's now the *only* thing a subclass supplies, via its constructor.
 *
 * <p>Computes {@code skillRollBonus} from {@link CharacterSkillService#getValueForRoll} (using
 * whichever Attribute currently governs this Perícia — its own, or a substituted one, see
 * {@link SkillCompetencyAbility#resolveAttributeDomain}; this resolution is safe to run
 * unconditionally, since it's a no-op — falls back to the Perícia's own Attribute — for every
 * skillType with no substituting ability) plus four sources, each summed for *both* {@code
 * ModifierType#SKILL_ROLL_BONUS} (applies to every Perícia's roll) and {@code
 * skillType.getRollBonusType()} (applies only to this one) — see {@link
 * #sumSkillRollBonusModifiers}: {@code attributeAbilities}, {@code skillCompetencyAbilities},
 * unlocked {@link SkillExcellency} tiers, and the target {@link CharacterSheet}'s own {@code
 * TemporaryBonus} pool (see CLAUDE.md's "Temporary bonuses from other Characters" section).
 * Computes {@code difficultyReduction} from unlocked {@code SkillExcellency} tiers plus every
 * {@code skillCompetencyAbility}'s own {@link SkillCompetencyAbility#getDifficultyReduction()}.
 *
 * <p>{@code applyTo} has three overloads, each just delegating down to the next one with
 * {@code null} for the newly-added parameter — {@code applyTo(target)} → {@code
 * applyTo(target, sceneContext)} → {@code applyTo(target, sceneContext, skillRoll)}, the last
 * one holding all the real logic. A subclass with something genuinely skill-specific to add
 * overrides the **longest** overload it needs data from (not a shorter one — {@link
 * ArtesInteraction} overrides the 3-arg one even though it doesn't touch {@code skillRoll}
 * itself, simply because that's where {@code applyTo}'s real logic lives) and calls {@code
 * super.applyTo(...)} first, then layers its own addition on top of the result — most skills
 * need nothing extra (e.g. {@link DominioDoManaInteraction}'s "this is always the second of
 * two rolls" note is documentation, not behavior). Every shorter overload still reaches a
 * subclass's override correctly through ordinary virtual dispatch, so callers using
 * {@code applyTo(target)} keep working unchanged no matter which overload a subclass defines
 * its logic on. {@link ArtesInteraction} is the one that currently overrides: a character
 * holding {@code ArtesCompetencyAbility#DOM_BARDICO} gets {@code temporaryBonusModifierType}/
 * {@code temporaryBonusRounds}/{@code temporaryBonusScope} set on the result — see that class.
 *
 * <p>When {@code skillRoll} names a {@link SkillRoll#getRequestedAbility()}, the 3-arg
 * {@code applyTo} validates the target's Character actually holds it (and that it belongs to
 * this same {@code skillType}) before doing anything else, throwing {@link
 * IllegalOperationException} otherwise — this stops a caller from requesting a maneuver-style
 * {@link SkillCompetencyAbility} the character never acquired. A {@code null} requestedAbility
 * (a plain roll, the common case) skips this check entirely.
 */
public abstract class AbstractSkillInteraction implements Interaction<CharacterSheet> {

    private final SkillType skillType;
    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;

    protected AbstractSkillInteraction(final SkillType skillType) {
        this(skillType, new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    protected AbstractSkillInteraction(final SkillType skillType, final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.skillType = skillType;
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        return applyTo(target, null);
    }

    /**
     * Same as {@link #applyTo(CharacterSheet)}, but also given sceneContext — nearby allies/
     * enemies and their {@code Range} — for a subclass whose bonus is conditioned on
     * proximity (e.g. {@code MedicinaECuraExcellency#FOCADO}'s "se não tiver inimigos
     * próximos") to consult by overriding this method (or the 3-arg one below) instead of the
     * 1-arg one. sceneContext may be {@code null} (the 1-arg {@link #applyTo(CharacterSheet)}
     * always passes {@code null}) when the caller doesn't have one — e.g. no active {@code
     * Scene}, or this roll isn't happening in an encounter.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext) {
        return applyTo(target, sceneContext, null);
    }

    /**
     * Same as {@link #applyTo(CharacterSheet, SceneContext)}, but also given skillRoll — the
     * already-rolled dice behind this Perícia test (this core never rolls dice itself, see
     * the {@code skill} package-info). When non-{@code null}, sets {@link InteractionResult
     * #reachedDifficultyLevel} (via {@link DifficultyLevel#reachedBy} against {@code
     * skillRollBonus + skillRoll.getTotal()}) and {@link InteractionResult#criticalResult}
     * (from {@link SkillRoll#getCriticalResult()}); both stay {@code null} when skillRoll is
     * {@code null} (the 1-/2-arg overloads always pass {@code null}), same as every other
     * not-applicable {@code InteractionResult} field. A subclass whose bonus is conditioned on
     * proximity *and* needs the roll's own result (none currently does) overrides this
     * 3-arg method and calls {@code super.applyTo(target, sceneContext, skillRoll)} first —
     * the same cascading-delegation shape as the 1-/2-arg overloads above, so a subclass only
     * ever needs to override the *one* method with everything it needs; the shorter overloads
     * still reach it correctly through ordinary virtual dispatch (see {@link ArtesInteraction},
     * which overrides this method, not the 2-arg one, even though it doesn't touch skillRoll
     * itself yet).
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final SkillRoll skillRoll) {
        Character character = target.getCharacter();
        if (skillRoll != null) {
            validateRequestedAbility(character, skillRoll.getRequestedAbility());
        }
        CharacterSkill characterSkill = findCharacterSkill(character);
        int graduationValue = characterSkill.getGraduation().getGraduationValue();

        AttributeDomain attributeDomain = SkillCompetencyAbility.resolveAttributeDomain(
                character.getSkillCompetencyAbilities(), skillType, characterSkill.getSkill().getAttributeDomain());

        int bonus = characterSkillService.getValueForRoll(characterSkill, character.getAttributes(), character.getRace(), attributeDomain);
        bonus += sumSkillRollBonusModifiers(character.getAttributeAbilities());
        bonus += sumSkillRollBonusModifiers(character.getSkillCompetencyAbilities());
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(skillType.getExcellencyClass(), graduationValue);
        bonus += sumSkillRollBonusModifiers(unlockedExcellencies);
        bonus += target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS);
        bonus += target.getTemporaryBonus(skillType.getRollBonusType());

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(skillType.getExcellencyClass(), graduationValue);
        difficultyReduction += character.getSkillCompetencyAbilities().stream()
                .mapToInt(SkillCompetencyAbility::getDifficultyReduction)
                .sum();

        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(character.getStatus())
                .skillRollBonus(bonus)
                .difficultyReduction(difficultyReduction);

        if (skillRoll != null) {
            result.reachedDifficultyLevel(DifficultyLevel.reachedBy(bonus + skillRoll.getTotal()).orElse(null))
                    .criticalResult(skillRoll.getCriticalResult());
        }

        return result.build();
    }

    /**
     * A {@code null} requestedAbility (a plain roll) is always fine. A non-{@code null} one
     * must belong to this same {@code skillType} and actually be held by the character —
     * otherwise this roll is trying to invoke a maneuver the character never acquired.
     */
    private void validateRequestedAbility(final Character character, final SkillCompetencyAbility requestedAbility) {
        if (requestedAbility == null) {
            return;
        }
        if (requestedAbility.getSkillType() != skillType || !character.getSkillCompetencyAbilities().contains(requestedAbility)) {
            throw new IllegalOperationException(REQUIRED_COMPETENCY_ABILITY_NOT_HELD);
        }
    }

    /**
     * Sums {@code ModifierType#SKILL_ROLL_BONUS} (every Perícia's roll) *and* {@code
     * skillType.getRollBonusType()} (only this Perícia's) across sources — the fix for a
     * previous bug where {@code AbstractSkillInteraction} only ever summed the generic type,
     * so a bonus meant for one specific Perícia had no way to avoid leaking into every other
     * Perícia's roll too.
     */
    private int sumSkillRollBonusModifiers(final Collection<?> sources) {
        return modifierResolver.sumModifiers(sources, ModifierType.SKILL_ROLL_BONUS)
                + modifierResolver.sumModifiers(sources, skillType.getRollBonusType());
    }

    /**
     * The Character's own CharacterSkill for this Interaction's Perícia, or a fresh one
     * carrying {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(skillType);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(skillType.newSkillInstance())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
