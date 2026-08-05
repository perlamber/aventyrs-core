package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;

import java.util.Collection;
import java.util.Optional;

public interface SkillCompetencyAbility {
    SkillType getSkillType();
    String getDescription();

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
}
