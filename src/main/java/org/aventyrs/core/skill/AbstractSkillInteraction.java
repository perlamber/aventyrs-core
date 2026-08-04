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
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.Collection;
import java.util.List;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;

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
 * <p>A subclass with something genuinely skill-specific to add overrides {@link
 * #applyTo(CharacterSheet, SceneContext)} (not the 1-arg overload, which just delegates to it
 * with {@code null}) and calls {@code super.applyTo(target, sceneContext)} first, then layers
 * its own addition on top of the result — most skills need nothing extra (e.g. {@link
 * DominioDoManaInteraction}'s "this is always the second of two rolls" note is documentation,
 * not behavior). {@link ArtesInteraction} is the one that currently does: a character holding
 * {@code ArtesCompetencyAbility#DOM_BARDICO} gets {@code temporaryBonusModifierType}/{@code
 * temporaryBonusRounds}/{@code temporaryBonusScope} set on the result — see that class.
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
     * próximos") to consult by overriding this method instead of the 1-arg one. sceneContext
     * may be {@code null} (the 1-arg {@link #applyTo(CharacterSheet)} always passes {@code
     * null}) when the caller doesn't have one — e.g. no active {@code Scene}, or this roll
     * isn't happening in an encounter. Every current skill computes the exact same result
     * whether sceneContext is {@code null} or not, since none has a proximity-conditioned
     * bonus wired yet — this base implementation doesn't consult it at all; it's here purely
     * so subclasses have somewhere to receive it.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext) {
        Character character = target.getCharacter();
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

        return InteractionResult.builder()
                .resultStatus(character.getStatus())
                .skillRollBonus(bonus)
                .difficultyReduction(difficultyReduction)
                .build();
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
