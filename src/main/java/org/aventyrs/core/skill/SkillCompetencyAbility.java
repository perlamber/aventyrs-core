package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InitiativeBlessing;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface SkillCompetencyAbility extends SkillTrait {

    /**
     * How many GD (DifficultyLevel) steps this Habilidade de Competência reduces a Perícia
     * roll's difficulty by — e.g. Atletismo's Atleta Versátil. Mirrors
     * {@link SkillExcellency#getDifficultyReduction()}; the default is 0, only abilities with
     * a concrete difficulty effect override it.
     */
    default int getDifficultyReduction() {
        return 0;
    }

    /**
     * The Attribute this ability unconditionally lets its own Perícia use instead of that
     * Perícia's normal base Attribute — e.g. {@code AtaqueCorpoACorpoCompetencyAbility
     * .ACUIDADE} substituting Destreza for Ataque Corpo-a-Corpo's normal Força. Empty by
     * default; only override on a constant whose rules text grants the substitution
     * unconditionally. A substitution that's scoped to a specific circumstance (e.g.
     * {@code AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO}, only for thrown-weapon/
     * spell attacks) can't be modeled this way yet — this codebase doesn't track what a roll
     * is *for*, the same simplification already applied to scoped Vantagem bonuses (see
     * CLAUDE.md's "Vantagem is a flat +2 bonus" section); document that gap in a TODO on the
     * constant instead of over- or under-granting here.
     */
    default Optional<AttributeDomain> getSubstituteAttributeDomain() {
        return Optional.empty();
    }

    /**
     * A bonus toward a dano roll this ability grants right now — e.g. {@code
     * AtaqueADistanciaCompetencyAbility#FRIEZA}'s Vantagem against a target at Distância
     * Curta or closer. Empty by default; only override on a constant whose rules text grants
     * a dano bonus. Unlike {@link #getDifficultyReduction()}/{@link
     * #getSubstituteAttributeDomain()} (both unconditional once the ability is held), this
     * kind of bonus is typically conditioned on per-roll data — proximity, in FRIEZA's case —
     * that a reflection-based {@code @Modifier} no-arg method has no way to see (same
     * limitation documented in CLAUDE.md's "Acquisition-time ability choices" section for
     * {@code ArtesAprimorarComArteAbility#getBaseDamageBonus(SkillType)}-style branches), so
     * this takes {@code sceneContext}/{@code attackTarget} explicitly instead of relying on
     * {@code ModifierResolver} to discover it.
     */
    default Optional<DamageBonus> resolveDamageBonus(final SceneContext sceneContext, final CharacterSheet attackTarget) {
        return Optional.empty();
    }

    /**
     * A bonus toward this Perícia's own roll ({@code skillRollBonus}) — not a dano roll, see
     * {@link #resolveDamageBonus} for that — conditioned on data about the specific target
     * being attacked, e.g. {@code AnoesRacialAbility#ABATEDORES_DE_GIGANTES}'s Vantagem
     * against a target 2+ Categorias de Tamanho larger than {@code actor}. Empty by default;
     * only override on a constant whose rules text grants a bonus scoped to this kind of
     * per-roll target data. Multiple abilities granting this at once are meant to be summed
     * by the caller, the same additive convention every other {@code skillRollBonus} source
     * already uses — unlike {@link #resolveDamageBonus}, which only ever expects one bonus to
     * apply per roll.
     */
    default Optional<Integer> resolveAttackRollBonus(final CharacterSheet actor, final CharacterSheet attackTarget) {
        return Optional.empty();
    }

    /**
     * A bonus toward this Perícia's own roll ({@code skillRollBonus}), conditioned on data no
     * reflection-based {@code @Modifier} method can see: the current {@link SceneContext} (e.g.
     * its {@code terrainType}) and/or which {@link SkillTrait} this specific roll requested
     * (see {@code SkillRoll#getRequestedAbility()}) — e.g. {@code
     * AnoesRacialAbility#FILHOS_DA_MONTANHA}'s Vantagem on Conhecimentos rolls made under the
     * Natureza Especialização while the Scene's terrain is Mountain or Cave. Empty by default;
     * only override on a constant whose rules text conditions a bonus on these per-roll facts.
     * Unlike {@link #resolveDamageBonus}/{@link #resolveAttackRollBonus} (which need an
     * explicit {@code attackTarget} the generic {@code applyTo} has no parameter for, so each
     * is only wired into one skill-specific Interaction), both {@code sceneContext} and the
     * requested trait are already parameters of {@code AbstractSkillInteraction}'s own {@code
     * applyTo}, so this one is summed generically, for every skill, the same additive
     * convention every other {@code skillRollBonus} source already uses.
     */
    default Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext, final SkillTrait requestedAbility) {
        return Optional.empty();
    }

    /**
     * Every {@link SkillType} this ability still owes the holder a {@code SkillSpecialization}
     * choice for, given character's currently trained Perícias — e.g. {@code
     * ConhecimentosCompetencyAbility#GENERALISTA}'s own "divididas entre até 2 Perícias à sua
     * escolha," mirroring {@code AttributeAbility#resolvePendingSkillTraitChoices}' shape (see
     * that method's own javadoc, and {@code GnoseAbility#DOMINIO_DO_CONHECIMENTO}, for the
     * identical specialization-only case on the AttributeAbility side). Empty by default; only
     * override on a constant whose rules text requires the player to pick one or more Perícias
     * for a new Especialização at acquisition time.
     *
     * <p>Unlike {@code AttributeAbility}'s counterpart, there's no {@code
     * AttributeAbilityService#grantAttributeAbility}-equivalent acquisition service for {@code
     * SkillCompetencyAbility} to report this through yet (see CLAUDE.md's "Adding a new
     * Perícia" section on the missing eligibility service) — a caller resolves each entry
     * directly via {@code AttributeAbilityService#grantSpecializationChoice} once the player
     * picks, the same generic (character, skillType, specialization) mutation
     * DOMINIO_DO_CONHECIMENTO's own entries resolve through; this method itself never mutates
     * anything.
     */
    default List<SkillType> resolvePendingSpecializationChoices(Character character) {
        return List.of();
    }

    /**
     * Every {@link InitiativeBlessing} this Habilidade de Competência grants the moment its
     * holder wins initiative for their group — mirrors {@code
     * org.aventyrs.core.ego.EgoAdvantage#resolveInitiativeBlessings}'s own shape (see that
     * method's javadoc for the full mechanism, and {@code
     * org.aventyrs.core.character.services.InitiativeBlessingService} for how this is scanned
     * alongside {@code EgoAdvantage}/{@code org.aventyrs.core.ability.AttributeAbility}).
     * Empty by default; only override on a constant whose rules text grants a bonus
     * specifically for winning initiative.
     */
    default List<InitiativeBlessing> resolveInitiativeBlessings() {
        return List.of();
    }

    /**
     * How many extra "números" this Habilidade de Competência widens skillType's Margem
     * Crítica Menor by right now, conditioned on {@link SceneContext} — e.g. {@code
     * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}'s "Outras Perícias – Margem
     * Crítica Menor +1" branch (see its own {@code getCriticalMarginReduction(SkillType)},
     * which this delegates to; sceneContext is unused there since that branch isn't
     * Scene-conditioned). Mirrors {@code
     * org.aventyrs.core.ability.AttributeAbility#resolveCriticalMarginIncrease}/{@code
     * org.aventyrs.core.ego.EgoAdvantage#resolveCriticalMarginIncrease}'s identical shape — see
     * {@code SkillRoll#getCriticalResult(int)} for how the sum across all three is actually
     * consumed. Zero by default; only override on a constant (or instance, for a choice-carrying
     * ability) whose rules text widens Margem Crítica Menor like this.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
        return 0;
    }

    /**
     * The Attribute that currently governs skillType's roll/graduation-cap for a character
     * holding skillCompetencyAbilities — defaultDomain, unless one of those abilities
     * targets this same skillType and {@link #getSubstituteAttributeDomain()} isn't empty,
     * in which case the substituted Attribute wins. Shared by every {@code <Skill>Interaction}
     * that supports substitution (see {@code AtaqueCorpoACorpoInteraction}) and by
     * {@code SkillGraduationService}'s max-graduation cap — both need the exact same
     * resolution, so it lives here once rather than duplicated at each call site.
     */
    static AttributeDomain resolveAttributeDomain(final Collection<SkillCompetencyAbility> skillCompetencyAbilities, final SkillType skillType, final AttributeDomain defaultDomain) {
        return skillCompetencyAbilities.stream()
                .filter(ability -> ability.getSkillType() == skillType)
                .map(SkillCompetencyAbility::getSubstituteAttributeDomain)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(defaultDomain);
    }

    /**
     * {@code character.getSkillCompetencyAbilities()} (acquired) plus {@code
     * character.getRace().getRacialAbilities()} (fixed per race) — see CLAUDE.md's "Racial
     * Abilities reuse SkillCompetencyAbility" section. Every caller that needs "every ability
     * of this kind, acquired or racial" (both {@link AbstractSkillInteraction}'s roll/
     * difficulty-reduction computation and {@code SkillGraduationService}'s max-graduation
     * cap, which must resolve {@link #resolveAttributeDomain} against the exact same list —
     * they previously didn't, see CLAUDE.md's Attribute base/Graduação section) shares this
     * one method rather than each concatenating the two lists itself and risking drift.
     */
    static List<SkillCompetencyAbility> allFor(final Character character) {
        return Stream.concat(
                        character.getSkillCompetencyAbilities().stream(),
                        character.getRace().getRacialAbilities().stream())
                .toList();
    }
}
